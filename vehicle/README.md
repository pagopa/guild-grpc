# Vehicle Service

### Introduction
...  

### Prerequisites
 - Rust (via [rustup](https://doc.rust-lang.org/book/ch01-01-installation.html))   

_You can also replicate the environment using **Docker**, without installing any additional dependencies:_
- Docker

### Setup and run
1. Install Docker:
Follow the official Docker installation instructions for your operating system: https://docs.docker.com/get-docker/
2. Clone the Repository:
`git clone <repository_url>`
3. Navigate to docker folder (at root level)
4. Run the docker compose to spin up booking service and mongo db:
`docker-compose up -d`

This will start the Vehicle Service container on port 8082 (http) and 50051 (gRPC), and the MongoDB container on port 27017.

### Invoking gRPC methods
The vehicle booking gRPC API can be invoked using various tools, including Postman or other gRPC client software. You'll need to import the **vehicle.proto** file, which defines the service definition and message types.  

Using Postman:
- Create a new collection and import the vehicle.proto file. 
- Select the BookingConfirmation method from the list of services
- Choose **localhost:50051** as URL
- In the "Message" tab you can choose "Example Message" to autobuild a request message like this:
```json
{
    {
        "user_id": "0001",
        "vehicle_id": "CAR00000001",
        "location": {
            "latitude": 41.900001525878906,
            "longitude": 12.449999809265137
        }
    }
}
```
- Populate the vehicle_id, user_id, and location fields with appropriate values.
- Send the request and check the response:


### Invoking REST methods
...  

### Testing Scenarios:

1. Successful Booking:
Use the vehicle ID CAR00000001 to test a successful booking. This vehicle is available for booking, and its status will be updated.

2. Vehicle Not Found:
Use the vehicle ID UNK00000001 to test a "vehicle not found" scenario. This vehicle does not exist and the booking request should result in an error response.

3. Vehicle Already Booked:
Use the vehicle ID CAR00000001 to test a "vehicle already booked" scenario. This vehicle is currently booked by a user in the previous scenario, and the booking request should result in an error response indicating a booking conflict.
