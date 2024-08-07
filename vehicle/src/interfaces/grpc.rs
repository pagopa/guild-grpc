use std::sync::{Arc, Mutex};
use std::env;

use tonic::{transport::Server, Request, Response, Status};

use booking::booking_service_server::BookingService;
use booking::BookingRequest;

use crate::data::database::VehicleDatabase;
use crate::interfaces::grpc::booking::booking_service_server::BookingServiceServer;
use crate::service::booking::BookingServerImpl;

pub mod booking {
    tonic::include_proto!("it.pagopa.guild.grpc.booking");
}
pub mod localization {
    tonic::include_proto!("localization");
}

#[tonic::async_trait]
impl BookingService for BookingServerImpl {

    async fn book(&self, request: Request<BookingRequest>) -> Result<Response<booking::AckResponse>, Status> {
        println!("[Booking Receiver] [gRPC] >>>>> Starting communication through gRPC stream.");
        let book_confirmation_request = request.get_ref();
        let (message, success) = self.book_vehicle(&book_confirmation_request.user_id, &book_confirmation_request.vehicle_id);
        let response = booking::AckResponse { message, success };
        println!("[Booking Receiver] [gRPC] <<<<< Ending communication through gRPC stream.");
        Ok(Response::new(response))
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
        .add_service(BookingServiceServer::new(BookingServerImpl::new(database)))
        .serve(format!("0.0.0.0:{}", port).parse().unwrap())
        .await
        .unwrap();
    println!("[Booking Receiver] Execution of Vehicle booking microservice ended.");
}