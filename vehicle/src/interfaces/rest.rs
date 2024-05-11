use std::{env, sync::{Arc, Mutex}};

use warp::Filter;

use crate::data::database::VehicleDatabase;
use crate::interfaces::AckResponseRest;
use crate::service::booking::BookingServerImpl;

use super::BookConfirmationRequestRest;




fn json_body() -> impl Filter<Extract = (BookConfirmationRequestRest,), Error = warp::Rejection> + Clone {
    warp::body::content_length_limit(1024 * 16).and(warp::body::json())
}


async fn health_check() -> Result<impl warp::Reply, warp::Rejection> {
    //let r = store.grocery_list.read();
    Ok(warp::reply::json(&String::from("OK")))
}

async fn change_batch_type(database: Arc<Mutex<VehicleDatabase>>) -> Result<impl warp::Reply, warp::Rejection> {
    let mut db = database.lock().unwrap();
    db.change_ping_send_mode();
    drop(db);

    let mut db = database.lock().unwrap();
    let x = db.is_stream_ping_active();
    drop(db);

    let batch_type = match x {
        true => "gRPC Stream",
        false => "REST",
    };

    Ok(warp::reply::json(&String::from(format!("Changed communication type for batch to [{}]!", batch_type))))
}

async fn book_vehicle(request: BookConfirmationRequestRest, database: Arc<Mutex<VehicleDatabase>>, ) -> Result<impl warp::Reply, warp::Rejection> {
    println!("[Booking Receiver] [REST] >>>>> Starting communication through REST API.");
    let booking_server_impl = BookingServerImpl::new(database);
    let (message, success) = booking_server_impl.book_vehicle(&request.user_id, &request.vehicle_id);
    let response = AckResponseRest { message, success };
    println!("[Booking Receiver] [REST] <<<<< Ending communication through REST API.");
    Ok(warp::reply::json(&response))
}



#[tokio::main]
pub async fn start_rest_server(database: Arc<Mutex<VehicleDatabase>>) {

    let database_filter = warp::any().map(move || Arc::clone(&database));

    let health = warp::get()
        .and(warp::path("info"))
        .and(warp::path::end())
        .and_then(health_check);

    let change_batch_type = warp::post()
        .and(warp::path("change-batch-type"))
        .and(warp::path::end())
        .and(database_filter.clone())
        .and_then(change_batch_type);


    let booking = warp::post()
        .and(warp::path("booking"))
        .and(warp::path::end())
        .and(json_body())
        .and(database_filter.clone())
        .and_then(book_vehicle);

    let routes = health.or(change_batch_type).or(booking);

    let port = env::var("REST_EXPOSED_PORT").unwrap_or(String::from("8080")).parse::<u16>().unwrap();
    warp::serve(routes)
        .run(([127, 0, 0, 1], port))
        .await;

}