db.createUser(
    {
        user: process.env.MONGO_GUILD_USER,
        pwd: process.env.MONGO_GUILD_PW,
        roles: [
            {
                role: "readWrite",
                db: "guild-grpc-db"
            }
        ]
    }
);

db.vehicles.drop();
db.vehicles.insertMany([
  {
    _id: ObjectId("60c9e1152a61292d843b7412"),
    type: "CAR",
    status: "BOOKED",
    bookings: [
        {
            date: "2023-04-16T08:00:00Z",
            user_id: "user123",
            location: {
                latitude: 40.7128,
                longitude: -74.006
            }
        },
        {
            date: "2023-04-20T10:30:00Z",
            user_id: "user456",
            location: {
                latitude: 34.0522,
                longitude: -118.2437
            }
        }
    ]
  },
  {
    _id: ObjectId("60c9e1152a61292d843b7413"),
    type: "MOTORCYCLE",
    status: "AVAILABLE",
    bookings: [
        {
            date: "2023-03-16T08:00:00Z",
            user_id: "user789",
            location: {
                latitude: 40.7128,
                longitude: -74.006
            }
        },
        {
            date: "2023-01-20T10:30:00Z",
            user_id: "user012",
            location: {
                latitude: 34.0522,
                longitude: -118.2437
            }
        }
    ]
  }
])