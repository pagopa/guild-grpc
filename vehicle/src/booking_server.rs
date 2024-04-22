use tonic::{transport::Server, Request, Response, Status};

use booking::vehicle_service_server::{VehicleService, VehicleServiceServer};
use booking::BookConfirmationRequest;
use common::AckResponse;

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
        let response = common::AckResponse{
            message: String::from("OK"),
            success: true
        };
        Ok(Response::new(response))
    }
} 

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {

    println!("Starting execution of gRPC stub for Booking server responder...");
    Server::builder()
        .add_service(VehicleServiceServer::new(BookingServerImpl::default()))
        .serve("[::1]:50051".parse()?)
        .await?;
    println!("Execution of gRPC stub for Booking server responder ended.");
    Ok(())
}