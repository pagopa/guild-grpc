use std::{env, sync::{Arc, Mutex}, time::Duration};

use tokio::time;
use tonic::Request;

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

    let url = env::var("LOCALIZATION_SERVER_URL").unwrap_or(String::from("http://[::1]:50052"));
    println!("[Localization Sender] Starting connection to Localization service. Pointing to URL [{}]", url);
    let mut client = LocalizationServiceClient::connect(url).await.unwrap();
    println!("[Localization Sender] Connected to Localization service!");
    
    let interval_size: u64 = env::var("LOCALIZATION_PING_INTERVAL").unwrap_or(String::from("5")).parse().unwrap();
    println!("[Localization Sender] Set sending interval to [{}] seconds.", interval_size);
    let mut interval = time::interval(Duration::from_millis(interval_size * 1000));

    loop {
        let mut locations = vec![];
        interval.tick().await;

        // first lock: get the vehicles identifiers
        let vehicle_ids = database.lock().unwrap().get_vehicle_ids();

        // update vehicle status and send updated location
        for vehicle_id in vehicle_ids {
            
            // second lock: update vehicle state
            println!("[Localization Sender] Updating state for vehicle with id [{}].", vehicle_id);
            let mut db_guard = database.lock().unwrap();
            db_guard.update_vehicle(&vehicle_id);
            drop(db_guard);

            // third lock: add location in stream
            println!("[Localization Sender] Generating location for vehicle with id [{}] to be sent to Localization service.", vehicle_id);
            let mut db_guard = database.lock().unwrap();
            let vehicle = db_guard.get_vehicle(&vehicle_id).unwrap();
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

        // sending updated locations in stream
        println!("[Localization Sender] Sending vehicle location to Localization Server...");
        let request_stream = futures::stream::iter(locations);
        let response = client.send_location(Request::new(request_stream))
            .await
            .unwrap()
            .into_inner();
        println!("[Localization Sender] Vehicle location sent to server. Response: [{}]", response.message);
    }
      
}

