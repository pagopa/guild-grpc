use std::sync::{Arc, Mutex};

use crate::data::database::VehicleDatabase;



pub async fn update_locations(database: &Arc<Mutex<VehicleDatabase>>) -> Vec<(String, f64, f64)> {
    
    let mut locations = vec![];

    // first lock: get the vehicles identifiers
    let vehicle_ids = database.lock().unwrap().get_vehicle_ids();

    // update vehicle status and send updated location
    println!("[Localization Sender] Updating state for all vehicles.");
    for vehicle_id in vehicle_ids {
                        
        // second lock: update vehicle state
        let mut db_guard = database.lock().unwrap();
        db_guard.update_vehicle(&vehicle_id);
        drop(db_guard);

        // third lock: add location in stream
        let mut db_guard = database.lock().unwrap();
        let vehicle = db_guard.get_vehicle(&vehicle_id).unwrap();
        if vehicle.booked_user == None {
            
            let location = (vehicle_id, vehicle.location.latitude as f64, vehicle.location.latitude as f64); 
            drop(db_guard);
            locations.push(location);
        }
    }

    locations
}