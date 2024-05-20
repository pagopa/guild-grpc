package main

import (
	"fmt"
	grpc "localization/localization_grpc"
	rest "localization/localization_rest"
)

func main() {
	fmt.Println("gRPC guild localization µ-service has been launched!")
	// Start GIN server in a Goroutine
	go rest.StartGINServer()
	// Start gRPC server in the main Goroutine
	grpc.StartGRPCServer()
}
