package localization_rest

import (
	"flag"
	"fmt"
	data "localization/localization_mongo"
	"log"
	"net/http"

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

// vehicles slice to seed record vehicle data.
var vehicles = []VehicleLocation{
	{ID: "1", Location: Location{Latitude: 45.97, Longitude: 12.33}},
	{ID: "2", Location: Location{Latitude: 45.98, Longitude: 12.34}},
	{ID: "3", Location: Location{Latitude: 45.99, Longitude: 12.35}},
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
	latitude := c.DefaultQuery("latitude", "45.97")
	longitude := c.DefaultQuery("longitude", "12.22")
	log.Printf("Get near vehicles from location (%v,%v)", latitude, longitude)
	c.IndentedJSON(http.StatusOK, vehicles)
}

// postVehicle adds a vehicle from JSON received in the request body.
func postVehicle(c *gin.Context) {
	var vl VehicleLocation

	// Call BindJSON to bind the received JSON to vehicle_location.
	if err := c.BindJSON(&vl); err != nil {
		log.Println(err.Error())
		return
	}

	// Add the new vehicle_location to the slice.
	vehicles = append(vehicles, vl)
	data.UpsertVehicleLocation(vl.ID, vl.Location.Latitude, vl.Location.Longitude)
	c.IndentedJSON(http.StatusCreated, vl)
}
