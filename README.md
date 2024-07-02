# gRPC guild
<description placeholder>

## How to run

### Run the micro-services ecosystem with docker compose
```
cd docker
docker compose build
docker compose up
```

### Run performance test with k6 as user micro-service

```
cd user
# Read user/README.md
yarn run-tests-locally-grpc
yarn run-tests-locally-rest
```

