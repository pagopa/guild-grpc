package server

import (
	"flag"
	"fmt"
	data "localization/data"
	"log"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

var (
	gin_port = flag.Int("gin_port", 8081, "The GIN server port")
)

// VehicleLocation represents data about a record VehicleLocation.
type VehicleLocation struct {
	ID       string   `json:"id"`
	Location Location `json:"location"`
}

// Location represents data about a record Location.
type Location struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

func StartGINServer() error {
	router := gin.Default()

	router.GET("/vehicles", getNearVehicles)
	router.POST("/vehicles", postVehicle)

	router.Run(fmt.Sprintf("localhost:%d", *gin_port))
	log.Printf("GIN Server listening at %d", *gin_port)

	return nil
}

// getNearVehicles responds with the list of all vehicles as JSON.
func getNearVehicles(c *gin.Context) {
	var latitude, _ = strconv.ParseFloat(c.DefaultQuery("latitude", "45.97"), 64)
	var longitude, _ = strconv.ParseFloat(c.DefaultQuery("longitude", "12.22"), 64)
	var page, _ = strconv.Atoi(c.DefaultQuery("page", "0"))
	var size, _ = strconv.Atoi(c.DefaultQuery("size", "1"))
	var vl []data.VehicleLocationModel = data.QueryProximity(latitude, longitude, int64(page), int64(size))
	if vl == nil {
		c.IndentedJSON(http.StatusOK, "No nearby vehicles within the defined radius")
	} else {
		c.IndentedJSON(http.StatusOK, vl)
	}
}

// postVehicle adds a vehicle from JSON received in the request body.
func postVehicle(c *gin.Context) {
	var vl VehicleLocation

	// Call BindJSON to bind the received JSON to vehicle_location.
	if err := c.BindJSON(&vl); err != nil {
		log.Println(err.Error())
		return
	}

	// Add or udpate the Vehicle Location
	data.UpsertVehicleLocation(vl.ID, vl.Location.Latitude, vl.Location.Longitude)
	c.IndentedJSON(http.StatusCreated, vl)
}
