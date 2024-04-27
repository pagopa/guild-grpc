# Booking Service

### Introduction
**Booking Service** is a Java Spring Boot application that exposes a gRPC API for booking vehicles for car sharing services. It interacts with a MongoDB database to store bookings and update vehicle status, and in order to confirm bookings it interacts also with the Vehicle Service (which exposes a gRPC API for booking confirmations).

### Prerequisites
- Java 21
- Maven 3.9.6
- MongoDB 7.0.8

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

This will start the Booking Service container on port 8080 (http) and 9090 (gRPC), and the MongoDB container on port 27017.

### Invoking gRPC book method
The book gRPC API can be invoked using various tools, including Postman or other gRPC client software. You'll need to import the **booking.proto** file, which defines the service definition and message types.

Using Postman:
- Create a new collection and import the booking.proto file. 
- Select the Book method from the list of services
- Choose **localhost:9090** as URL
- In the "Message" tab you can choose "Example Message" to autobuild a request message like this:
```json
{
    "location": {
        "latitude": -37412929.48437618,
        "longitude": -74393901.02844512
    },
    "user_id": "0001",
    "vehicle_id": "60c9e1152a61292d843b7413"
}
```
- Populate the vehicle_id, user_id, and location fields with appropriate values.
- Send the request and check the response:

### Testing Scenarios:

1. Successful Booking:
Use the vehicle ID 60c9e1152a61292d843b7413 to test a successful booking. This vehicle is available for booking, and the booking should be recorded in the database.

2. Vehicle Not Found:
Use the vehicle ID 60c9e1152a61292d843b7467 to test a "vehicle not found" scenario. This vehicle does not exist in the database, and the booking request should result in an error response.

3. Vehicle Already Booked:
Use the vehicle ID 60c9e1152a61292d843b7412 to test a "vehicle already booked" scenario. This vehicle is currently booked by another user, and the booking request should result in an error response indicating a booking conflict.

