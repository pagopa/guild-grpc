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

        let location = match book_confirmation_request.location.to_owned() {
            Some(loc) => loc,
            None => VehicleDatabase::get_base_location(),
        };

        // first database lock: get nearest vehicle identifier
        let mut db_guard = self.database.lock().unwrap();
        let nearest_vehicle = db_guard.get_nearest(location.latitude, location.longitude);
        drop(db_guard);
        
        let mut success = false;
        let message;
        if let Some(vehicle_id) = nearest_vehicle {
            println!("[Booking Receiver] Found a free vehicle. Vehicle id: [{}]", vehicle_id);
            success = true;
            message = String::from("Booked!");

            println!("[Booking Receiver] Setting vehicle with id [{}] as Booked", vehicle_id);
            
            // second database lock: set vehicle as Booked
            let mut db_guard = self.database.lock().unwrap();
            db_guard.set_to_booked(vehicle_id.as_str(), Some(user_id.to_string()), rng.gen_range(10..50));
            drop(db_guard);
            
            println!("[Booking Receiver] Vehicle id [{}] correctly booked!", vehicle_id);
        } else {
            println!("[Booking Receiver] No free vehicle found for user [{}]", user_id);
            message = String::from("No free vehicle found!");
        }
        let response = AckResponse { message, success };
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