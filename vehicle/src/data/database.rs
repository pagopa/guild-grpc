use std::{collections::HashMap, env};

use rand::{distributions::{Distribution, Standard}, Rng};

const BASE_LATITUDE: f32 = 41.90;
const BASE_LONGITUDE: f32 = 12.45;

pub struct Vehicle {
    pub id: String,
    pub r#type: VehicleType,
    pub location: Coordinates,
    pub state: VehicleState,
    pub fuel: f32,
    pub booked_user: Option<String>,
    pub direction: Direction,
    pub estimated_time_in_state: i32
}

pub enum VehicleType {
    Car,
    Bicycle,
    Motorcycle,
}

pub struct Coordinates {
    pub latitude: f32,
    pub longitude: f32
}

#[derive(PartialEq)]
pub enum VehicleState {
    Available,
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
        let mut self_instance = Self { vehicles: HashMap::new() };
        let car_fleet_size = env::var("CAR_FLEET_SIZE").unwrap_or(String::from("5")).parse::<i32>().unwrap();
        for idx in 1..car_fleet_size + 1 {
            self_instance.add_vehicle(format!("CAR{:0fill$}", idx, fill=8).as_str(), VehicleType::Car);
        }
        let bicycle_fleet_size = env::var("BICYCLE_FLEET_SIZE").unwrap_or(String::from("3")).parse::<i32>().unwrap();
        for idx in 1..bicycle_fleet_size + 1 {
            self_instance.add_vehicle(format!("BIC{:0fill$}", idx, fill=8).as_str(), VehicleType::Bicycle);
        }
        let motorcycle_fleet_size = env::var("MOTORCYCLE_FLEET_SIZE").unwrap_or(String::from("2")).parse::<i32>().unwrap();
        for idx in 1..motorcycle_fleet_size + 1 {
            self_instance.add_vehicle(format!("MOT{:0fill$}", idx, fill=8).as_str(), VehicleType::Motorcycle);
        }
        self_instance
    }

    pub fn add_vehicle(&mut self, id: &str, vehicle_type: VehicleType) {
        self.vehicles.insert(id.to_string(), Vehicle { 
            id: String::from(id), 
            location: Coordinates { 
                latitude: BASE_LATITUDE, 
                longitude: BASE_LONGITUDE 
            }, 
            r#type: vehicle_type,
            direction: Direction::NorthWest,
            state: VehicleState::Available, 
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
                        vehicle.state = VehicleState::Available;
                        vehicle.booked_user = None;
                        vehicle.estimated_time_in_state = 0;
                    }
                },
                VehicleState::Available => {
                    if vehicle.fuel <= 40.0 {
                        vehicle.state = VehicleState::ReturningToBase;
                    } 
                },
                VehicleState::ReturningToBase => {
                    if vehicle.estimated_time_in_state > 0 {
                        vehicle.estimated_time_in_state -= 1;
                    } else {
                        vehicle.state = VehicleState::Available;
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

    pub fn get_base_coordinates() -> Coordinates {
        Coordinates { latitude: BASE_LATITUDE, longitude: BASE_LONGITUDE }
    }

}