package main

import (
	"flag"
	"fmt"
	"localization/protofiles/localization"
	pb "localization/protofiles/localization"
	"log"
	"net"
	"time"

	"google.golang.org/grpc"
)

var (
	port = flag.Int("port", 50051, "The server port")
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

func main() {
	fmt.Println("gRPC guild localization µ-service has been launched!")

	flag.Parse()
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterLocalizationServer(s, &server{})

	log.Printf("server listening at %v", lis.Addr())
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
