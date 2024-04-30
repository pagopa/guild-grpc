use std::thread;

use crate::service::localization::start_ping;
use crate::service::booking::start_server;


mod service;

#[tokio::main]
async fn main() {

    let thread1 = thread::spawn(|| {
        let _ = start_server();
    });
    let thread2 = thread::spawn(|| {
        let _ = start_ping();
    });
    thread1.join().unwrap();
    thread2.join().unwrap();
}
