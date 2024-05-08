use std::sync::{Arc, Mutex};
use std::time::Duration;
use std::{env, thread};

use tokio::time;
use tonic::transport::Channel;
use tonic::{transport::Server, Request, Response, Status};

use booking::vehicle_service_server::VehicleService;
use booking::BookConfirmationRequest;
use common::AckResponse;

use crate::data::database::VehicleDatabase;
use crate::interfaces::grpc::booking::vehicle_service_server::VehicleServiceServer;
use crate::interfaces::grpc::common::Location;
use crate::interfaces::grpc::localization::localization_service_client::LocalizationServiceClient;
use crate::interfaces::grpc::localization::VehicleLocationRequest;
use crate::service::booking::BookingServerImpl;
use crate::service::localization::update_locations;

pub mod booking {
    tonic::include_proto!("booking");
}
pub mod localization {
    tonic::include_proto!("localization");
}
pub mod common {
    tonic::include_proto!("common");
}

#[tonic::async_trait]
impl VehicleService for BookingServerImpl {

    async fn book_confirmation(&self, request: Request<BookConfirmationRequest>) -> Result<Response<AckResponse>, Status> {
        let book_confirmation_request = request.get_ref();
        let (message, success) = self.book_vehicle(&book_confirmation_request.user_id, &book_confirmation_request.vehicle_id);
        let response = AckResponse { message, success };
        Ok(Response::new(response))
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

        let locations = update_locations(database).await;
        let x: Vec<VehicleLocationRequest> = locations.iter().map(|loc| VehicleLocationRequest {
             vehicle_id: loc.0.to_owned(),
             location: Some(Location {
                latitude: loc.1.to_owned(),
                longitude: loc.2.to_owned()
             }),
        }).collect();

        // sending updated locations in stream, if stream channel is open
        let stream_size = locations.len();
        let request_stream = futures::stream::iter(x);
        match client.send_location(Request::new(request_stream)).await {
            Err(_) => println!("[Localization Sender] Connection stream with Localization service closed. Impossible to send updated position to remote service."),
            Ok(response) => println!("[Localization Sender] Sent [{}] vehicle locations to Localization server. Remote server response: [{}]", stream_size, response.into_inner().message),
        }
    }
}

/**
 * This tokio task permits to start the execution of the ping towards Localization server
 */
#[tokio::main]
pub async fn start_ping(database: Arc<Mutex<VehicleDatabase>>) { 

    loop {
        let url = env::var("LOCALIZATION_SERVER_URL").unwrap_or(String::from("http://[::1]:50052"));
        println!("[Localization Sender] Starting connection to Localization service. Pointing to URL [{}].", url);

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

/**
 * This tokio task permits to start the execution of the server that receives the booking
 */
#[tokio::main]
pub async fn start_server(database: Arc<Mutex<VehicleDatabase>>) { 
    
    let port = env::var("BOOKING_SERVICE_EXPOSED_PORT").unwrap_or(String::from("50051"));
    println!("[Booking Receiver] Starting execution of Vehicle booking microservice on port [{}].", port);
    Server::builder()
        .add_service(VehicleServiceServer::new(BookingServerImpl::new(database)))
        .serve(format!("0.0.0.0:{}", port).parse().unwrap())
        .await
        .unwrap();
    println!("[Booking Receiver] Execution of Vehicle booking microservice ended.");
}