package localization_mongo

import (
	"context"
	"fmt"
	"log"
	"os"

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
	Location LocationModel `json:"location"`
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

func QueryProximity(latitude float64, longitude float64) []VehicleLocationModel {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found")
	}

	uri := os.Getenv("MONGODB_URI")
	if uri == "" {
		log.Fatal("Set your 'MONGODB_URI' environment variable.")
	}
	client, err := mongo.Connect(context.TODO(), options.Client().
		ApplyURI(uri))

	if err != nil {
		panic(err)
	}

	defer func() {
		if err = client.Disconnect(context.TODO()); err != nil {
			log.Fatalf("fatal: %v", err)
		}
	}()

	coll := client.Database("guild-grpc-db").Collection("localizations")

	createGeospatialIndex(coll)

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
	}

	cur, err := coll.Find(context.TODO(), filter)
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
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found")
	}

	uri := os.Getenv("MONGODB_URI")
	if uri == "" {
		log.Fatal("Set your 'MONGODB_URI' environment variable.")
	}
	client, err := mongo.Connect(context.TODO(), options.Client().
		ApplyURI(uri))

	if err != nil {
		panic(err)
	}

	defer func() {
		if err = client.Disconnect(context.TODO()); err != nil {
			panic(err)
		}
	}()

	coll := client.Database("guild-grpc-db").Collection("localizations")

	filter := bson.M{"id": id}

	update := bson.M{
		"$set": bson.D{
			{Key: "id", Value: id},
			{Key: "location", Value: bson.D{
				{Key: "type", Value: "Point"},
				{Key: "coordinates", Value: []float64{latitude, longitude}},
			}},
		},
	}

	opts := options.Update().SetUpsert(true)
	result, err := coll.UpdateOne(context.Background(), filter, update, opts)
	if err != nil {
		log.Fatalf("fatal: %v", err)
	}

	fmt.Printf("Document upserted with ID: %s\n", result.UpsertedID)
}

func GetCollection() *mongo.Collection {
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

	if err != nil {
		panic(err)
	}

	defer func() {
		if err = client.Disconnect(context.TODO()); err != nil {
			panic(err)
		}
	}()

	coll := client.Database("guild-grpc-db").Collection("localizations")

	return coll
}
