use std::{env, sync::{Arc, Mutex}, thread, time::Duration};

use tokio::time;
use tonic::{transport::Channel, Request};

use localization::{VehicleLocationRequest, localization_service_client::LocalizationServiceClient};
use common::Location;

use crate::data::database::VehicleDatabase;


pub mod localization {
    tonic::include_proto!("localization");
}
pub mod common {
    tonic::include_proto!("common");
}


#[tokio::main]
pub async fn start_ping(database: Arc<Mutex<VehicleDatabase>>) { 

    loop {
        let url = env::var("LOCALIZATION_SERVER_URL").unwrap_or(String::from("http://[::1]:50052"));
        println!("[Localization Sender] Starting connection to Localization service. Pointing to URL [{}]", url);

        match LocalizationServiceClient::connect(url).await {
            Err(_) => {
                let retry = 10;
                println!("[Localization Sender] Stream with Localization service currently closed. Trying again in [{}] seconds", retry);
                thread::sleep(Duration::from_secs(retry));
            },
            Ok(client) => {
                update_and_send_location(&database, client).await;
            }
        }
    }
}

async fn update_and_send_location(database: &Arc<Mutex<VehicleDatabase>>, mut client: LocalizationServiceClient<Channel>) {
    println!("[Localization Sender] Connected to Localization service!");
        
    let interval_size: u64 = env::var("LOCALIZATION_PING_INTERVAL").unwrap_or(String::from("5")).parse().unwrap();
    println!("[Localization Sender] Set sending interval to [{}] seconds.", interval_size);
    let mut interval = time::interval(Duration::from_millis(interval_size * 1000));

    loop {
        
        // wait until interval is completed
        interval.tick().await;

        let mut locations = vec![];

        // first lock: get the vehicles identifiers
        let vehicle_ids = database.lock().unwrap().get_vehicle_ids();

        // update vehicle status and send updated location
        println!("[Localization Sender] Updating state for all vehicles.");
        for vehicle_id in vehicle_ids {
                        
            // second lock: update vehicle state
            let mut db_guard = database.lock().unwrap();
            db_guard.update_vehicle(&vehicle_id);
            drop(db_guard);

            // third lock: add location in stream
            let mut db_guard = database.lock().unwrap();
            let vehicle = db_guard.get_vehicle(&vehicle_id).unwrap();
            if vehicle.booked_user == None {
                let location = VehicleLocationRequest {
                    vehicle_id,
                    location: Some(Location { 
                        latitude: vehicle.location.latitude as f64, 
                        longitude: vehicle.location.longitude as f64 
                    }),
                };
                drop(db_guard);
                
                // add updated location in the final stream
                locations.push(location);
            }
        }

        // sending updated locations in stream, if stream channel is open
        let stream_size = locations.len();
        let request_stream = futures::stream::iter(locations);
        match client.send_location(Request::new(request_stream)).await {
            Err(_) => println!("[Localization Sender] Connection stream with Localization service closed. Impossible to send updated position to remote service."),
            Ok(response) => println!("[Localization Sender] Sent [{}] vehicle locations to Localization server. Remote server response: [{}]", stream_size, response.into_inner().message),
        }
    }
}