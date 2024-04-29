package main

import (
	"flag"
	"fmt"
	"localization/protofiles/localization"
	pb "localization/protofiles/localization"
	"log"
	"net"
	"net/http"
	"time"

	"google.golang.org/grpc"

	"github.com/gin-gonic/gin"
)

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++gRPC API START+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

var (
	gRPC_port = flag.Int("grpc_port", 50051, "The gRPC server port")
	gin_port  = flag.Int("gin_port", 8080, "The GIN server port")
)

// server is used to implement localization.LocalizationServer.
type server struct {
	pb.UnimplementedLocalizationServer
}

func (s *server) GetNearVehicles(req *pb.UserLocationRequest, stream pb.Localization_GetNearVehiclesServer) error {
	// Simulate some nearby vehicles
	vehicles := []*pb.Vehicle{
		{
			VehicleId: "1",
			Location: &pb.Location{
				Latitude:  37.7749,
				Longitude: -122.4194,
			},
			Fuel: 80.0,
		},
		{
			VehicleId: "2",
			Location: &pb.Location{
				Latitude:  37.774,
				Longitude: -122.42,
			},
			Fuel: 70.0,
		},
	}

	// Handle vehicle location request
	log.Printf("Received user location request: UserID %s, Latitude %f, Longitude %f\n",
		req.UserId, req.Location.Latitude, req.Location.Longitude)

	// Stream each vehicle to the client
	for i := 1; i <= 10; i++ {
		if err := stream.Send(&pb.NearVehicleResponse{Vehicle: vehicles}); err != nil {
			return err
		}

		// Sleep for demonstration purposes
		time.Sleep(1 * time.Second)
	}

	response := &pb.NearVehicleResponse{
		Vehicle: vehicles,
	}

	if err := stream.Send(response); err != nil {
		return err
	}

	return nil
}

func (s *server) SendLocation(stream pb.Localization_SendLocationServer) error {
	requestCh := make(chan *localization.VehicleLocationRequest)
	defer close(requestCh)

	// Goroutine to receive and process requests
	go func() {
		for {
			req, err := stream.Recv()
			if err != nil {
				return
			}
			requestCh <- req
		}
	}()

	// Timer for timeout
	timeout := time.NewTimer(3 * time.Second)
	defer timeout.Stop()

	for {
		select {
		case req := <-requestCh:
			// Handle vehicle location request
			log.Printf("Received vehicle location request: VehicleID %s, Latitude %f, Longitude %f\n",
				req.VehicleId, req.Location.Latitude, req.Location.Longitude)

			// Reset the timer
			if !timeout.Stop() {
				// read from the timer's channel timeout.C to drain any remaining signal
				<-timeout.C
			}
			timeout.Reset(3 * time.Second)

		case <-timeout.C:
			// If no new request received within 3 seconds, send acknowledgment and close the stream
			log.Println("Client is too slow, closing the stream")
			if err := stream.SendAndClose(&localization.AckResponse{Success: true, Message: "Vehicle location received"}); err != nil {
				log.Printf("Error sending response: %v\n", err)
			}
			return nil

		case <-stream.Context().Done():
			// Client disconnected, return without sending acknowledgment
			log.Println("Client disconnected")
			return nil
		}
	}
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++gRPC API END+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++RESTful API START+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

// vehicle_location represents data about a record vehicle_location.
type vehicle_location struct {
	ID       string   `json:"id"`
	Location location `json:"location"`
	Fuel     float64  `json:"fuel"`
}

// location represents data about a record location.
type location struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

// vehicles slice to seed record album data.
var vehicles = []vehicle_location{
	{ID: "1", Location: location{Latitude: 45.97, Longitude: 12.33}, Fuel: 26.25},
	{ID: "2", Location: location{Latitude: 45.98, Longitude: 12.34}, Fuel: 50.25},
	{ID: "3", Location: location{Latitude: 45.99, Longitude: 12.35}, Fuel: 96.25},
}

// getNearVehicles responds with the list of all vehicles as JSON.
func getNearVehicles(c *gin.Context) {
	latitude := c.DefaultQuery("latitude", "45.97")
	longitude := c.DefaultQuery("longitude", "12.22")
	log.Printf("Get near vehicles from location (%v,%v)", latitude, longitude)
	c.IndentedJSON(http.StatusOK, vehicles)
}

// postVehicle adds an album from JSON received in the request body.
func postVehicle(c *gin.Context) {
	var vehicle_location vehicle_location

	// Call BindJSON to bind the received JSON to vehicle_location.
	if err := c.BindJSON(&vehicle_location); err != nil {
		log.Println(err.Error())
		return
	}

	// Add the new vehicle_location to the slice.
	vehicles = append(vehicles, vehicle_location)
	c.IndentedJSON(http.StatusCreated, vehicle_location)
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++RESTful API END+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

func main() {
	fmt.Println("gRPC guild localization µ-service has been launched!")

	// Start gRPC server in a Goroutine
	go startGINServer()

	// Start GIN server in the main Goroutine
	startGRPCServer()
}

func startGRPCServer() {
	flag.Parse()
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *gRPC_port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterLocalizationServer(grpcServer, &server{})

	log.Printf("gRPC server listening at %v", lis.Addr())
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}

func startGINServer() {
	router := gin.Default()

	router.GET("/vehicles", getNearVehicles)
	router.POST("/vehicles", postVehicle)

	router.Run(fmt.Sprintf("localhost:%d", *gin_port))
	log.Printf("GIN Server listening at %d", *gin_port)
}
