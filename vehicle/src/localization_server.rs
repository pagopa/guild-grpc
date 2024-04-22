use tonic::{transport::Server, Request, Response, Status, Streaming};

use localization::localization_service_server::{LocalizationService, LocalizationServiceServer};
use localization::VehicleLocationRequest;
use common::AckResponse;

pub mod localization {
    tonic::include_proto!("localization");
}
pub mod common {
    tonic::include_proto!("common");
}


#[derive(Debug, Default)]
pub struct LocalizationServerImpl {}

#[tonic::async_trait]
impl LocalizationService for LocalizationServerImpl {

    async fn send_location(&self, request: Request<Streaming<VehicleLocationRequest>>) -> Result<Response<AckResponse>, Status> {

        println!("Received request on open channel for localization: {:?}", request);
        
        let mut location_stream = request.into_inner();
        while let Some(location) = location_stream.message().await? {
            println!("Location for vehicle {} = ({:?})", location.vehicle_id, location.location);
        }

        Ok(Response::new(AckResponse { success: true, message: String::from("OK") }))
    }
}


#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {

    println!("Starting execution of gRPC stub for Localization server responder...");
    Server::builder()
        .add_service(LocalizationServiceServer::new(LocalizationServerImpl::default()))
        .serve("[::1]:50052".parse()?)
        .await?;
    println!("Execution of gRPC stub for Localization server responder ended.");
    Ok(())
}