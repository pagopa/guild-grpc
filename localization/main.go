package main

import (
	"fmt"
	"localization/server"
)

func main() {
	fmt.Println("gRPC guild localization µ-service has been launched!")
	// Start GIN server in a Goroutine
	go server.StartGINServer()
	// Start gRPC server in the main Goroutine
	server.StartGRPCServer()
}
