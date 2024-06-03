use std::{env, sync::{Arc, Mutex}, thread, time::Duration};

use crate::{data::database::VehicleDatabase, service::localization::{send_location_via_rest, update_locations}};
use crate::service::localization::send_location_via_grpc;
use crate::interfaces::grpc::localization::localization_client::LocalizationClient;

/**
 * This tokio task permits to start the execution of the ping towards Localization server
 */
#[tokio::main]
pub async fn start_ping(database: Arc<Mutex<VehicleDatabase>>) { 

    let retry: u64 = env::var("LOCALIZATION_PING_INTERVAL").unwrap_or(String::from("10")).parse().unwrap();
    loop {
        
        let mut db = database.lock().unwrap();
        let is_stream_ping_active = db.is_stream_ping_active();
        drop(db);

        if is_stream_ping_active {
            
            let url = env::var("LOCALIZATION_SERVER_GRPC_URL").unwrap_or(String::from("http://[::]:50052"));
            println!("[Localization Sender] [gRPC] >>>>> Starting connection to Localization service. Pointing to URL [{}].", url);
            match LocalizationClient::connect(url).await {
                Err(_) => {
                    update_locations(&database).await;
                    println!("[Localization Sender] [gRPC] <<<<< Stream with Localization service currently closed. Trying again in [{}] seconds", retry);
                },
                Ok(client) => {
                    send_location_via_grpc(&database, client).await;
                }
            }
        } else {
            
            let client = reqwest::Client::new();
            send_location_via_rest(&database, client).await;
        }
        thread::sleep(Duration::from_secs(retry));
    }
}