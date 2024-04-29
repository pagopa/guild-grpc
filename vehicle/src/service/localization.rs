use std::time::Duration;

use tokio::time;
use tonic::Request;
use rand::Rng;

use localization::{VehicleLocationRequest, localization_service_client::LocalizationServiceClient};
use common::Location;


pub mod localization {
    tonic::include_proto!("localization");
}
pub mod common {
    tonic::include_proto!("common");
}


#[tokio::main]
pub async fn start_ping() -> Result<(), Box<dyn std::error::Error>> {

    println!("[Localization Sender] Starting connection to Localization service...");
    let mut client = LocalizationServiceClient::connect("http://[::1]:50052").await?;
    println!("[Localization Sender] Connected to Localization service!");
    
    let mut rng = rand::thread_rng();
    let mut interval = time::interval(Duration::from_millis(2000));

    loop {
        interval.tick().await;
        let location = VehicleLocationRequest {
            location: Some(Location {
                latitude: rng.gen_range(41.85000..=41.95999),
                longitude: rng.gen_range(12.43000..=12.49999),
            }),
            vehicle_id: String::from("car_1")
        };

        println!("[Localization Sender] Sending vehicle location to Localization Server...");
        let request_stream = futures::stream::once(async { location });
        let response = client.send_location(Request::new(request_stream))
            .await?
            .into_inner();
        println!("Response: {:?}", response);
    }
      
}

