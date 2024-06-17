use std::env;
use std::sync::{Arc, Mutex};
use std::time::Duration;

use reqwest::Client;
use tokio::time;
use tonic::transport::Channel;
use tonic::Request;

use crate::data::database::VehicleDatabase;
use crate::interfaces::grpc::localization::localization_client::LocalizationClient;
use crate::interfaces::grpc::localization::VehicleLocationRequest;
use crate::interfaces::{AckResponseRest, LocationRest, VehicleLocationRequestRest};


pub async fn update_locations(database: &Arc<Mutex<VehicleDatabase>>) -> Vec<(String, f64, f64)> {
    
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
            
            let location = (vehicle_id, vehicle.location.latitude as f64, vehicle.location.latitude as f64); 
            drop(db_guard);
            locations.push(location);
        }
    }

    locations
}


pub async fn send_location_via_grpc(database: &Arc<Mutex<VehicleDatabase>>, mut client: LocalizationClient<Channel>) {
    println!("[Localization Sender] [gRPC] >>>>> Connected to Localization service!");
        
    let interval_size: u64 = env::var("LOCALIZATION_PING_INTERVAL").unwrap_or(String::from("10")).parse().unwrap();
    println!("[Localization Sender] [gRPC] Set sending interval to [{}] seconds.", interval_size);
    let mut interval = time::interval(Duration::from_millis(interval_size * 1000));

    loop {

        let mut db = database.lock().unwrap();
        let is_stream_send_enabled = db.is_stream_ping_active();
        drop(db);
        if !is_stream_send_enabled {
            break;
        }
        
        // wait until interval is completed
        interval.tick().await;

        let locations = update_locations(database).await;
        let locations: Vec<VehicleLocationRequest> = locations.iter().map(|loc| VehicleLocationRequest {
            vehicle_id: loc.0.to_owned(),
            location: Some(crate::interfaces::grpc::localization::Location {
                latitude: loc.1.to_owned(),
                longitude: loc.2.to_owned()
            }),
        }).collect();
   
        // sending updated locations in stream, if stream channel is open
        let stream_size = locations.len();
        let request_stream = futures::stream::iter(locations);
        match client.send_location(Request::new(request_stream)).await {
            Err(e) => println!("[Localization Sender] [gRPC] <<<<< Connection stream with Localization service closed. Impossible to send updated position to remote service. {}", e),
            Ok(response) => println!("[Localization Sender] [gRPC] Sent [{}] vehicle locations to Localization server. Remote server response: [{}]", stream_size, response.into_inner().message),
        }
    }
}


pub async fn send_location_via_rest(database: &Arc<Mutex<VehicleDatabase>>, client: Client) {
        
    let url = env::var("LOCALIZATION_SERVER_REST_URL").unwrap_or(String::from("http://localhost:8081/localization"));
    let interval_size: u64 = env::var("LOCALIZATION_PING_INTERVAL").unwrap_or(String::from("10")).parse().unwrap();
    println!("[Localization Sender] Set sending interval to [{}] seconds.", interval_size);
    let mut interval = time::interval(Duration::from_millis(interval_size * 1000));

    loop {

        let mut db = database.lock().unwrap();
        let is_stream_send_enabled = db.is_stream_ping_active();
        drop(db);
        if is_stream_send_enabled {
            break;
        }
        
        // wait until interval is completed
        interval.tick().await;

        let locations = update_locations(database).await;
        let locations: Vec<VehicleLocationRequestRest> = locations.iter().map(|loc| VehicleLocationRequestRest {
            vehicle_id: loc.0.to_owned(),
            location: Some(LocationRest {
                latitude: loc.1.to_owned(),
                longitude: loc.2.to_owned()
            }),
        }).collect();
   
        // sending updated locations
        println!("[Localization Sender] [REST] >>>>> Starting connection to Localization service. Pointing to URL [{}].", url);
        let client_request = client.post(&url).json(&locations);
        match client_request.send().await {
            Err(_) => println!("[Localization Sender] [REST] <<<<< Connection with Localization service invalid. Impossible to send updated position to remote service."),
            Ok(response) => {
                let ack_response = response.json::<AckResponseRest>().await;
                match ack_response {
                    Ok(data) => println!("[Localization Sender] [REST] <<<<< Sent [{}] vehicle locations to Localization server. Remote server response: [{:?}]", locations.len(), data),
                    Err(error) => println!("[Localization Sender] [REST] <<<<< Error during sending [{}] vehicle locations to Localization server. Remote server response: [{:?}]", locations.len(), error),
                }
            },
        }
    }
}