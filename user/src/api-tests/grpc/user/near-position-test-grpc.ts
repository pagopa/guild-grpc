import { check, fail, sleep } from "k6";
import { getConfigOrThrow } from "../utils/config";
import grpc from 'k6/net/grpc';

/**
 * Test constants
 */
const config = getConfigOrThrow();
const localizationClient = new grpc.Client();
const bookingClient = new grpc.Client();

/**
 * Configure test options
 */
export let options = {
  scenarios: {
    contacts: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: config.preAllocatedVUs,
      maxVUs: config.maxVUs,
      stages: [
        { target: config.rate, duration: config.rampingDuration },
        { target: config.rate, duration: config.duration },
        { target: 0, duration: config.rampingDuration },
      ],
    },
  },

  thresholds: {
    grpc_req_duration: ["p(99)<1500"], // 99% of requests must complete below 1.5s
    checks: ['rate>0.9'], // 90% of the request must be completed
    "grpc_req_duration{name:get-vehicle-position-test-localization}": ["p(95)<1000"],
    "grpc_req_duration{name:get-vehicle-position-test-booking}": ["p(95)<1000"],
  },
};

/**
 * Configure gRpc client
 */
localizationClient.load(['definitions'], 'localization.proto', 'booking.proto', 'common.proto');
bookingClient.load(['definitions'], 'booking.proto', 'common.proto');


let initialized = false;

function initializeClients() {
  try {
    if (!initialized) {
      const bookingGrpcHost = config.BOOKING_GRPC_SERVER_HOST;
      const localizationGrpcHost = config.LOCALIZATION_GRPC_SERVER_HOST;
      console.log(`Connecting localzation client to endpoint: ${localizationGrpcHost}`);
      localizationClient.connect(localizationGrpcHost, {
        plaintext: true //here set to true to allow unsecure communication for test purpose
      });
      console.log(`Localization client connected successfully: ${localizationClient}`);
      console.log(`Connecting booking client to endpoint: ${bookingGrpcHost}`);
      bookingClient.connect(bookingGrpcHost, {
        plaintext: true //here set to true to allow unsecure communication for test purpose
      });
      console.log(`Booking client connected successfully: ${bookingClient}`);
      initialized = true;
    }
  } catch (error) {
    console.log(`Error initializing clients. Error: ${error}`);
    fail(`Cannot perform test, error initializing clients`);
  }
}

export default function () {
  initializeClients();
  const userId = "123";//TODO deve essere randomico? può essere fisso?
  const localizationUrl = "/localization.Localization/GetNearVehicles";
  const userLocation = {
    latitude: 43.90,
    longitude: 43.90
  };
  const localizationRequest = {
    user_id: userId,
    location: userLocation,
    vehicle_level: 1
  };
  const bookingUrl = "/it.pagopa.guild.grpc.booking.BookingService/Book";
  const localizationStreaming = new grpc.Stream(localizationClient, localizationUrl, { tags: { name: "get-vehicle-position-test-localization" } });
  localizationStreaming.on("data", response => {
    const vehicles = (response as any).vehicle;
    check(vehicles, { 'Response have vehicles': (r) => r && r?.length > 0 },
      { name: "get-vehicle-position-test-localization" });
    if (!vehicles || vehicles.length == 0) {
      fail("No vehicle information received from localization service");
    }
    const vehicle = vehicles[Math.floor(Math.random() * (vehicles.length))];
    const bookingRequest = {
      location: userLocation,
      user_id: userId,
      vehicle_id: vehicle.vehicleId
    };
    const bookingResponse = bookingClient.invoke(
      bookingUrl,
      bookingRequest,
      { tags: { name: "get-vehicle-position-test-booking" } }
    );

    check(bookingResponse, { 'status is OK': (r) => r && (r.status === grpc.StatusOK || r.status === grpc.StatusFailedPrecondition)},
      { name: "get-vehicle-position-test-booking" });
  });
  localizationStreaming.on("error", error => {
    fail(`Error retrieving near vehicle, localization status code: [${error}]`);
  });
  localizationStreaming.on("end", _ => {
  });
  //send request to localization service
  localizationStreaming.write(localizationRequest);
}

