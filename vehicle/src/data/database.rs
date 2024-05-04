use std::collections::HashMap;

use crate::service::booking::common::Location;

const BASE_LATITUDE: f32 = 41.90;
const BASE_LONGITUDE: f32 = 12.45;

pub struct Vehicle {
    pub id: String,
    pub location: Coordinates,
    pub state: VehicleState,
    pub fuel: f32,
    pub booked_user: Option<String>,
    pub estimated_time_in_state: i32
}

pub struct Coordinates {
    pub latitude: f32,
    pub longitude: f32
}

#[derive(PartialEq)]
pub enum VehicleState {
    Free,
    Booked,
    ReturningToBase,
}


pub struct VehicleDatabase {
    pub vehicles:  HashMap<String, Vehicle>
}

impl VehicleDatabase {

    pub fn new() -> Self {
        Self { vehicles: HashMap::new() }
    }

    pub fn add_vehicle(&mut self, id: &str) {
        self.vehicles.insert(id.to_string(), Vehicle { 
            id: String::from(id), 
            location: Coordinates { 
                latitude: BASE_LATITUDE, 
                longitude: BASE_LONGITUDE 
            }, 
            state: VehicleState::Free, 
            fuel: 100.0,
            booked_user: None,
            estimated_time_in_state: -1
        });
    }

    pub fn get_vehicle(&mut self, id: &str) -> Option<&Vehicle> {
        let result = self.vehicles.get(id);
        result
    }

    pub fn set_to_booked(&mut self, id: &str, booked_user: Option<String>, estimated_time_in_state: i32) {
        self.change_state(id, VehicleState::Booked, booked_user, estimated_time_in_state);
    }

    pub fn set_to_free(&mut self, id: &str) {
        self.change_state(id, VehicleState::Free, None, -1);
    }

    pub fn set_to_returning(&mut self, id: &str, estimated_time_in_state: i32) {
        self.change_state(id, VehicleState::ReturningToBase, None, estimated_time_in_state);
    }

    fn change_state(&mut self, id: &str, state: VehicleState, booked_user: Option<String>, estimated_time_in_state: i32) {
        let result = self.vehicles.get_mut(id);
        match result {
            Some(vehicle) => {
                vehicle.state = state;
                vehicle.booked_user = booked_user;
                vehicle.estimated_time_in_state = estimated_time_in_state;
            },
            None => {
                println!("[Database] Error! No valid vehicle found with id [{}]", id)
            }
        }
    }

    /**
     * Get the identifier of the nearest vehicle based on latitude and longitude
     */
    pub fn get_nearest(&mut self, latitude: f64, longitude: f64) -> Option<String> {
        let mut vehicle_by_distance = vec![];
        for (_, vehicle) in self.vehicles.iter() {
            if vehicle.state == VehicleState::Free {
                let delta_lat = vehicle.location.latitude as f64 - latitude;
                let delta_lon = vehicle.location.longitude as f64 - longitude;
                let quadratic_distance = delta_lat.powi(2) + delta_lon.powi(2);
                let distance = quadratic_distance.sqrt();
                vehicle_by_distance.push((distance, vehicle));
            }
        }
        vehicle_by_distance.sort_by(|a, b| a.0.partial_cmp(&b.0).unwrap());
        let mut result = None;
        if let Some(&(_, vehicle)) = vehicle_by_distance.first() {
            result = Some(vehicle.id.to_owned());
        }
        result
    }

    pub fn get_base_location() -> Location {
        Location { latitude: BASE_LATITUDE as f64, longitude: BASE_LONGITUDE as f64 }
    }

    pub fn set_position(&mut self, id: &String, latitude: f32, longitude: f32) {
        let result = self.vehicles.get_mut(id);
        if let Some(vehicle) = result {
            vehicle.location = Coordinates { latitude, longitude }
        } else {
            println!("[Database] Error! No valid vehicle found with id [{}]", id);
        }
    }

}