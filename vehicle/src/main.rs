use std::sync::{Arc, Mutex};
use std::thread;

use dotenv::dotenv;

use data::database::VehicleDatabase;
use crate::interfaces::batch::start_ping;
use crate::interfaces::grpc::start_server;
use crate::interfaces::rest::start_rest_server;

mod data;
mod service;
mod interfaces;

#[tokio::main]
async fn main() {

    // load .env file and all its variables
    dotenv().ok();

    // initialize data base
    let database = Arc::new(Mutex::new(VehicleDatabase::new()));

    // start thread: gRPC booking service
    let db_ref1 = Arc::clone(&database);
    let thread1 = thread::spawn(move || start_server(db_ref1));

    // start thread: gRPC localization service
    let db_ref2 = Arc::clone(&database);
    let thread2 = thread::spawn(move || start_ping(db_ref2));

    let db_ref3 = Arc::clone(&database);
    let thread3 = thread::spawn(move || start_rest_server(db_ref3));

    // joining threads and close main application
    thread1.join().unwrap();
    thread2.join().unwrap();
    thread3.join().unwrap();
}
