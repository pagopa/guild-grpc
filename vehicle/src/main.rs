use std::sync::{Arc, Mutex};
use std::thread;

use data::database::VehicleDatabase;

use crate::service::localization::start_ping;
use crate::service::booking::start_server;

mod data;
mod service;

#[tokio::main]
async fn main() {

    let database = Arc::new(Mutex::new(VehicleDatabase::new()));
    let mut db_instance = database.lock().unwrap();
    db_instance.add_vehicle("vehicle-01");
    drop(db_instance);

    let db_ref1 = Arc::clone(&database);
    let thread1 = thread::spawn(move || start_server(db_ref1));

    let db_ref2 = Arc::clone(&database);
    let thread2 = thread::spawn(move || start_ping(db_ref2));

    thread1.join().unwrap();
    thread2.join().unwrap();
}
