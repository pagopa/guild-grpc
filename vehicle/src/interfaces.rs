use serde::{Deserialize, Serialize};

pub mod grpc;
pub mod rest;
pub mod batch;


#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct LocationRest {
    pub latitude: f64,
    pub longitude: f64
}

#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct BookConfirmationRequestRest {
    pub user_id: String,
    pub vehicle_id: String,
    pub location: LocationRest
}

#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct VehicleLocationRequestRest {
    pub vehicle_id: String,
    pub location: Option<LocationRest>
}


#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct AckResponseRest {
    pub success: bool,
    pub message: String
}