use std::sync::{Arc, Mutex};

use rand::Rng;


use crate::data::database::VehicleDatabase;


pub struct BookingServerImpl {
    database: Arc<Mutex<VehicleDatabase>>
} 

impl BookingServerImpl {
    
    pub fn new(database: Arc<Mutex<VehicleDatabase>>) -> Self {
        Self { database }
    }

    pub fn book_vehicle(&self, user_id: &String, vehicle_id: &String) -> (String, bool) {
        let mut rng = rand::thread_rng();

        //let book_confirmation_request = request.get_ref();
        println!("[Booking Receiver] Received request for booking confirmation for user [{}]", user_id);

        let vehicle_id = vehicle_id.to_owned();

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
        (message, can_be_booked)

    }
}
