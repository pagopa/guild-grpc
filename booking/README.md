
# Booking Service

### Introduction
**Booking Service** is a Java Spring Boot application that exposes a **gRPC** API to booking vehicles for car sharing services. It interacts with a MongoDB database to store bookings and update vehicle status, and in order to confirm bookings it interacts also with the Vehicle Service (which exposes a gRPC API for booking confirmations).
The service offers the same functionalities/APIs also in the **REST** mode (associating also with the REST APIs exposed by the Vehicle Service) to make a comparison about gRPC vs REST performance.

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

This will start the Booking Service container on port 8082 (http) and 9090 (gRPC), and the MongoDB container on port 27017.

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

### REST APIs
You can use the same functionalities of the service also in REST mode by invoking the following endpoint:
| Functionality    | Endpoint    | HTTP Method |
| -------- | -------- | ------- |
|book a vehicle | http://localhost:8082/booking  | POST    |

**Example of request:**
```json
{
  "vehicle_id": "b5b23d1b-ad4a-4a74",
  "location": {
    "latitude": 36.07,
    "longitude": -95.9
  },
  "user_id": "user-99a2"
}
```

**Example of response:**
```json
{
  "success": true,
  "message": "Vehicle booked successfully"
}
```

<br/>

After you have started the service you can:
- Open the OpenAPI definitions in [JSON format](http://localhost:8082/api-docs.yaml) and in [YAML format](http://localhost:8082/api-docs.yaml).
  (If you need offline api doc, go [here](https://github.com/pagopa/guild-grpc/blob/main/booking/openapi/docs/openapi-booking.yaml))
- Open [Swagger UI](http://localhost:8082/swagger-ui/index.html) to interact with the REST APIs

### Testing Scenarios (valid for gRPC and REST):

1. **Successful Booking**:
   Use the vehicle ID 60c9e1152a61292d843b7413 to test a successful booking. This vehicle is available for booking, and the booking should be recorded in the database.

2. **Vehicle Not Found**:
   Use the vehicle ID 60c9e1152a61292d843b7467 to test a "vehicle not found" scenario. This vehicle does not exist in the database, and the booking request should result in an error response.

3. **Vehicle Already Booked**:
   Use the vehicle ID 60c9e1152a61292d843b7412 to test a "vehicle already booked" scenario. This vehicle is currently booked by another user, and the booking request should result in an error response indicating a booking conflict.

### Error handling:
| Scenario    | gRPC error    | REST error |
| -------- | -------- | ------- |
|vehicle not found | NOT_FOUND - 5 | NOT_FOUND - 404    |
|vehicle already booked | FAILED_PRECONDITION - 9 | CONFLICT - 409    |
|vehicle service error (confirmation) | INTERNAL - 13 | BAD_GATEWAY - 502    |
|invalid request | INVALID_ARGUMENT - 3 | BAD_REQUEST - 400    |
|generic error | UNKNOWN - 2 | INTERNAL_SERVER_ERROR - 500    |