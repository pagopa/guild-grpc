use std::{collections::HashMap, env};

use rand::{distributions::{Distribution, Standard}, Rng};

use crate::service::booking::common::Location;

const BASE_LATITUDE: f32 = 41.90;
const BASE_LONGITUDE: f32 = 12.45;

pub struct Vehicle {
    pub id: String,
    pub location: Coordinates,
    pub state: VehicleState,
    pub fuel: f32,
    pub booked_user: Option<String>,
    pub direction: Direction,
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

pub enum Direction {
    NorthWest,
    NorthEast,
    SouthWest,
    SouthEast,
}

impl Distribution<Direction> for Standard {
    fn sample<R: Rng + ?Sized>(&self, rng: &mut R) -> Direction {
        match rng.gen_range(0..=3) {
            0 => Direction::NorthWest,
            1 => Direction::NorthEast,
            2 => Direction::SouthWest,
            _ => Direction::SouthEast,
        }
    }
}

impl Direction {

    pub fn get_coordinate_movement(direction: &Direction) -> (f32, f32) {
        let result = match direction {
            Direction::NorthWest => (-1.0, 1.0),
            Direction::NorthEast => (-1.0, -1.0),
            Direction::SouthWest => (1.0, 1.0),
            Direction::SouthEast => (1.0, -1.0),
        };
        result
    }
}

pub struct VehicleDatabase {
    pub vehicles:  HashMap<String, Vehicle>
}

impl VehicleDatabase {

    pub fn new() -> Self {
        Self { vehicles: HashMap::new() }
    }

    pub fn add_default_vehicles(&mut self, default_ids: String) {
        let ids = default_ids.split(',');
        for id in ids {
            self.add_vehicle(id);
        }
    }

    pub fn add_vehicle(&mut self, id: &str) {
        self.vehicles.insert(id.to_string(), Vehicle { 
            id: String::from(id), 
            location: Coordinates { 
                latitude: BASE_LATITUDE, 
                longitude: BASE_LONGITUDE 
            }, 
            direction: Direction::NorthWest,
            state: VehicleState::Free, 
            fuel: 100.0,
            booked_user: None,
            estimated_time_in_state: -1
        });
    }

    pub fn update_vehicle(&mut self, id: &str) {
        let result = self.vehicles.get_mut(id);
        if let Some(vehicle) = result {
            match &mut vehicle.state {
                VehicleState::Booked => {
                    if vehicle.estimated_time_in_state > 0 {
                        vehicle.estimated_time_in_state -= 1;
                        vehicle.fuel -= env::var("FUEL_CONSUME_PER_TURN").unwrap_or(String::from("1.0")).parse::<f32>().unwrap() as f32;
                        // updating movement
                        let movement = Direction::get_coordinate_movement(&vehicle.direction);
                        vehicle.location.latitude += movement.0 * 0.001;
                        vehicle.location.longitude += movement.1 * 0.001;
                    } else {
                        vehicle.state = VehicleState::Free;
                        vehicle.booked_user = None;
                        vehicle.estimated_time_in_state = 0;
                    }
                },
                VehicleState::Free => {
                    if vehicle.fuel <= 40.0 {
                        vehicle.state = VehicleState::ReturningToBase;
                    } 
                },
                VehicleState::ReturningToBase => {
                    if vehicle.estimated_time_in_state > 0 {
                        vehicle.estimated_time_in_state -= 1;
                    } else {
                        vehicle.state = VehicleState::Free;
                        vehicle.fuel = 100.0;
                        vehicle.location = VehicleDatabase::get_base_coordinates();
                    }
                },
            }
        } else {
            println!("[Database] Error! No valid vehicle found with id [{}]", id);
        }
    }

    pub fn get_vehicle_ids(&mut self) -> Vec<String> {
        let mut ids = vec![];
        for id in self.vehicles.keys() {
            ids.push(id.to_owned());
        }
        ids
    }

    pub fn get_vehicle(&mut self, id: &str) -> Option<&Vehicle> {
        let result = self.vehicles.get(id);
        result
    }

    pub fn set_to_booked(&mut self, id: &str, booked_user: Option<String>, estimated_time_in_state: i32) {
        let result = self.vehicles.get_mut(id);
        match result {
            Some(vehicle) => {
                vehicle.state = VehicleState::Booked;
                vehicle.booked_user = booked_user;
                vehicle.estimated_time_in_state = estimated_time_in_state;
                vehicle.direction = rand::random();
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

    pub fn get_base_coordinates() -> Coordinates {
        Coordinates { latitude: BASE_LATITUDE, longitude: BASE_LONGITUDE }
    }

}