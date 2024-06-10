package server

import (
	"flag"
	"fmt"
	data "localization/data"
	"localization/protofiles/localization"
	pb "localization/protofiles/localization"
	"log"
	"net"
	"time"

	"google.golang.org/grpc"
)

var (
	gRPC_port = flag.Int("grpc_port", 50052, "The gRPC server port")
)

// server is used to implement localization.LocalizationServer.
type server struct {
	pb.UnimplementedLocalizationServer
}

func StartGRPCServer() {
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

func mapVehicleLocationModelsToVehicles(models []data.VehicleLocationModel) []*pb.Vehicle {
	var vehicles []*pb.Vehicle

	for _, model := range models {
		vehicle := &pb.Vehicle{
			VehicleId: model.ID,
			Location: &pb.Location{
				Latitude:  model.Location.Coordinates[0],
				Longitude: model.Location.Coordinates[1],
			},
		}
		vehicles = append(vehicles, vehicle)
	}

	return vehicles
}

func (s *server) GetNearVehicles(req *pb.UserLocationRequest, stream pb.Localization_GetNearVehiclesServer) error {
	// Handle vehicle location request
	log.Printf("Received user location request: UserID %s, Latitude %f, Longitude %f\n",
		req.UserId, req.Location.Latitude, req.Location.Longitude)

	var vl []data.VehicleLocationModel = data.QueryProximity(req.Location.Latitude, req.Location.Longitude)
	vehicles := mapVehicleLocationModelsToVehicles(vl)

	// Stream each vehicle to the client
	if err := stream.Send(&pb.NearVehicleResponse{Vehicle: vehicles}); err != nil {
		return err
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

			data.UpsertVehicleLocation(req.VehicleId, req.Location.Latitude, req.Location.Longitude)

			// Reset the timer
			if !timeout.Stop() {
				// read from the timer's channel timeout.C to drain any remaining signal
				<-timeout.C
			}
			timeout.Reset(10 * time.Second)

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
