use std::sync::{Arc, Mutex};

use tonic::{transport::Server, Request, Response, Status};

use rand::Rng;

use booking::vehicle_service_server::VehicleService;
use booking::BookConfirmationRequest;
use common::AckResponse;

use crate::data::database::VehicleDatabase;
use crate::service::booking::booking::vehicle_service_server::VehicleServiceServer;

pub mod booking {
    tonic::include_proto!("booking");
}
pub mod common {
    tonic::include_proto!("common");
}


pub struct BookingServerImpl {
    database: Arc<Mutex<VehicleDatabase>>
} 

impl BookingServerImpl {
    
    pub fn new(database: Arc<Mutex<VehicleDatabase>>) -> Self {
        Self { database }
    }
}

#[tonic::async_trait]
impl VehicleService for BookingServerImpl {

    async fn book_confirmation(&self, request: Request<BookConfirmationRequest>) -> Result<Response<AckResponse>, Status> {

        let mut rng = rand::thread_rng();

        let book_confirmation_request = request.get_ref();
        let user_id = &book_confirmation_request.user_id;
        println!("[Booking Receiver] Received request for booking confirmation for user [{}]", user_id);

        let vehicle_id = book_confirmation_request.vehicle_id.to_owned();

        // first database lock: get nearest vehicle identifier
        let mut db_guard = self.database.lock().unwrap();
        let mut can_be_booked = false;
        if let Some(vehicle) = db_guard.get_vehicle(vehicle_id.as_str()) {
            can_be_booked = vehicle.booked_user == None;
        }
        drop(db_guard);
        
        let message;
        if can_be_booked {
            println!("[Booking Receiver] Found a free vehicle. Vehicle id: [{}]", vehicle_id);
            message = String::from("Booked!");

            println!("[Booking Receiver] Setting vehicle with id [{}] as Booked", vehicle_id);
            
            // second database lock: set vehicle as Booked
            let mut db_guard = self.database.lock().unwrap();
            db_guard.set_to_booked(vehicle_id.as_str(), Some(user_id.to_string()), rng.gen_range(10..75));
            drop(db_guard);
            
            println!("[Booking Receiver] Vehicle id [{}] correctly booked!", vehicle_id);
        } else {
            println!("[Booking Receiver] No vehicle with id [{}] found for user [{}].", vehicle_id, user_id);
            message = String::from("No free vehicle found!");
        }
        let response = AckResponse { message, success: can_be_booked };
        Ok(Response::new(response))
    }
}

#[tokio::main]
pub async fn start_server(database: Arc<Mutex<VehicleDatabase>>) { 
    
    println!("[Booking Receiver] Starting execution of Vehicle booking microservice.");
    Server::builder()
        .add_service(VehicleServiceServer::new(BookingServerImpl::new(database)))
        .serve("0.0.0.0:50051".parse().unwrap())
        .await
        .unwrap();
    println!("[Booking Receiver] Execution of Vehicle booking microservice ended.");
}