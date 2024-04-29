use tonic::{transport::Server, Request, Response, Status};

use booking::vehicle_service_server::VehicleService;
use booking::BookConfirmationRequest;
use common::AckResponse;

use crate::service::booking::booking::vehicle_service_server::VehicleServiceServer;

pub mod booking {
    tonic::include_proto!("booking");
}
pub mod common {
    tonic::include_proto!("common");
}


#[derive(Debug, Default)]
pub struct BookingServerImpl {} 

#[tonic::async_trait]
impl VehicleService for BookingServerImpl {

    async fn book_confirmation(&self, request: Request<BookConfirmationRequest>) -> Result<Response<AckResponse>, Status> {

        println!("Received request for booking confirmation: {:?}", request);
        let response = AckResponse{
            message: String::from("OK"),
            success: true
        };
        Ok(Response::new(response))
    }
}

#[tokio::main]
pub async fn start_server() -> Result<(), Box<dyn std::error::Error>> {
    
    println!("[Booking Receiver] Starting execution of Vehicle booking microservice.");
    Server::builder()
        .add_service(VehicleServiceServer::new(BookingServerImpl::default()))
        .serve("0.0.0.0:50051".parse().unwrap())
        .await?;
    println!("[Booking Receiver] Execution of Vehicle booking microservice ended.");
    Ok(())
}