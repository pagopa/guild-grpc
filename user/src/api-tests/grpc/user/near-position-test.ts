import { check } from "k6";
import { getConfigOrThrow } from "../utils/config";
import grpc from 'k6/net/grpc';


/**
 * Test constants
 */
const config = getConfigOrThrow();
const client = new grpc.Client();

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
    "grpc_req_duration{name:get-vehicle-position-test}": ["p(95)<1500"],
  },
};

/**
 * Configure gRpc client
 */
client.load(['definitions'], 'localization.proto');


let initialized = false;

function initializeClient() {
  if (!initialized) {
    const grpcServerHost = config.GRPC_SERVER_HOST;
    client.connect(grpcServerHost, {
      plaintext: true //here set to true to allow unsecure communication for test purpose
    });
    initialized = true;
  }
}

export default function () {
  initializeClient();
  const requestUrl = "/localization.Localization/GetNearVehicles";
  const request = {
    user_id: "123",
    location: {
      latitude: 0,
      longitude: 0
    },
    vehicle_level: 1
  };
  const response = client.invoke(
    requestUrl,
    request,
    { tags: { name: "get-vehicle-position-test" } }
  );
  check(response, { 'status is OK': (r) => r && r.status === grpc.StatusOK },
    { name: "get-vehicle-position-test" });
}
