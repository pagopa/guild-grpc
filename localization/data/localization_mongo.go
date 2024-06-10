package data

import (
	"context"
	"fmt"
	"log"
	"os"
	"sync"

	"github.com/joho/godotenv"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type LocationModel struct {
	Type        string    `bson:"type"`
	Coordinates []float64 `bson:"coordinates"`
}

type VehicleLocationModel struct {
	ID       string        `json:"id"`
	Status   string        `json:"status"`
	Location LocationModel `json:"location"`
}

var (
	mongoClient *mongo.Client
	vehicles    *mongo.Collection
	once        sync.Once
)

func init() {
	// Initialize MongoDB client once
	once.Do(func() {
		var err error
		mongoClient, err = GetMongoClient()
		if err != nil {
			log.Fatalf("Could not connect to MongoDB: %v", err)
		}
		vehicles = mongoClient.Database("guild-grpc-db").Collection("vehicles")
		createGeospatialIndex(vehicles)
	})
}

func QueryProximity(latitude float64, longitude float64) []VehicleLocationModel {
	filter := bson.M{
		"location": bson.M{
			"$near": bson.M{
				"$geometry": bson.M{
					"type":        "Point",
					"coordinates": []float64{latitude, longitude},
				},
				"$maxDistance": 100000,
			},
		},
		"status": "AVAILABLE",
	}

	cur, err := vehicles.Find(context.TODO(), filter)
	var locations []VehicleLocationModel
	if err != nil {
		log.Fatalf("fatal: %v", err)
	}
	defer cur.Close(context.TODO())

	for cur.Next(context.TODO()) {
		var loc VehicleLocationModel
		err := cur.Decode(&loc)
		log.Println(loc)
		if err != nil {
			log.Fatalf("fatal: %v", err)
		}
		locations = append(locations, loc)
	}
	log.Println(locations)

	return locations
}

func UpsertVehicleLocation(id string, latitude float64, longitude float64) {
	filter := bson.M{"id": id}

	toUpdate := bson.M{
		"$set": bson.D{
			{Key: "_id", Value: id},
			{Key: "id", Value: id},
			{Key: "type", Value: "CAR"},
			{Key: "status", Value: "AVAILABLE"},
			{Key: "location", Value: bson.D{
				{Key: "type", Value: "Point"},
				{Key: "coordinates", Value: []float64{latitude, longitude}},
			}},
		},
	}

	opts := options.Update().SetUpsert(true)
	result, err := vehicles.UpdateOne(context.Background(), filter, toUpdate, opts)
	if err != nil {
		log.Fatalf("fatal: %v", err)
	}

	fmt.Printf("Document upserted with ID: %s\n", result.UpsertedID)
}

func createGeospatialIndex(collection *mongo.Collection) error {
	indexModel := mongo.IndexModel{
		Keys: bson.M{
			"location": "2dsphere",
		},
	}
	_, err := collection.Indexes().CreateOne(context.TODO(), indexModel)
	return err
}

func GetMongoClient() (*mongo.Client, error) {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found")
	}

	uri := os.Getenv("MONGODB_URI")
	if uri == "" {
		log.Fatal("Set your 'MONGODB_URI' environment variable. " +
			"See: " +
			"www.mongodb.com/docs/drivers/go/current/usage-examples/#environment-variable")
	}
	client, err := mongo.Connect(context.TODO(), options.Client().
		ApplyURI(uri))

	log.Print("Mongo connected!")

	return client, err
}
