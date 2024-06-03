import { check, fail } from "k6";
import http from "k6/http";
import { getConfigOrThrow } from "../utils/config";

/**
 * Test constants
 */
const config = getConfigOrThrow();

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
    "http_req_duration{name:localization-get-near-vehicles}": ["p(95)<=250"],
    "http_req_duration{name:booking-create-book}": ["p(95)<=250"],
  },
};

const headersParams = {
  headers: {
      'Content-Type': 'application/json'
  }
};

export default function () {
  const localization = {
    latitude: 43.90,
    longitude: 43.90
  }
  const userId = "123";//TODO deve essere randomico? può essere fisso?
  const localizationUrl = config.LOCALIZATION_REST_HOST + `/vehicles?latitude=${localization.latitude}&longitude=${localization.longitude}`;
  const bookingUrl = config.BOOKING_REST_HOST + "/booking";
  const localizationResponse = http.get(localizationUrl, {
    tags: { name: "localization-get-near-vehicles" }
  });
  check(
    localizationResponse,
    { "Response status from GET vehicles is 200 OK": (r) => r.status == 200 },
    { name: "localization-get-near-vehicles" }
  );
  const vehicles = localizationResponse.json() as unknown as Array<any>;
  if (!vehicles || vehicles.length == 0) {
    fail("No vehicle information received from localization service");
  }
  const vehicle = vehicles[Math.floor(Math.random() * (vehicles.length))];
  const bookingRequest = {
    vehicle_id: vehicle.id,
    location: {
      latitude: vehicle.location.latitute,
      longitude: vehicle.location.longitude
    },
    user_id: userId
  };
  const bookingResponse = http.post(bookingUrl, JSON.stringify(bookingRequest), {
    ...headersParams,
    tags: { name: "booking-create-book" }
  });
  check(
    bookingResponse,
    { "Response status from POST booking is 200 OK": (r) => r.status == 200 },
    { name: "booking-create-book" }
  );

}

