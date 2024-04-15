package main

import (
	"flag"
	"fmt"
	pb "localization/protofiles/localization"
	"log"
	"net"

	"google.golang.org/grpc"
)

var (
	port = flag.Int("port", 50051, "The server port")
)

// server is used to implement localization.LocalizationServer.
type server struct {
	pb.UnimplementedLocalizationServer
}

// SendLocation implements localization.LocalizationServer
func (s *server) SendLocation(stream pb.Localization_SendLocationServer) error {
	for {
		req, err := stream.Recv()
		if err != nil {
			return err
		}

		// Handle vehicle location request
		log.Printf("Received vehicle location request: VehicleID %s, Latitude %f, Longitude %f\n",
			req.VehicleId, req.Location.Latitude, req.Location.Longitude)

		// Send acknowledgement
		if err := stream.SendAndClose(&pb.AckResponse{Success: true, Message: "Vehicle location received"}); err != nil {
			return err
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
