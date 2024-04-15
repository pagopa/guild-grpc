# Performance tests for gRPC guild

This is a set of [k6](https://k6.io) load tests related to the gRPC guild.

All tests require a set of parameters: **rate**, **duration**, **preAllocatedVUs** and **maxVUs**. 

These parameters are necessary to set the test target to a given number of iteration per second (**rate**) in a given time (**duration**), using a certain number of VUs (**preAllocatedVUs** and **maxVUs**).

An example test configuration file can be found into .env.example file

## Test execution

Tests can be executed using both k6 locally installed on the machine or using grafana/k6 docker image using the following command:

### Local k6 installation test execution

```sh
yarn run-tests-locally
```

### Docker k6 image installation test execution

```sh
yarn run-tests-with-docker
```

Both commands will execute all configured tests and expect a `.env` file to be present (see .env.example) for an example configuration file.

To run a single tests you can run the following commands:

### Local k6 installation test execution

```sh
export SET NODE_OPTIONS=--openssl-legacy-provider && shx rm -rf dist && webpack && cp -r definitions dist/definitions
export $(cat .env | xargs) && k6 run $(pwd)/dist/<<test file name>>.js 
```

### Docker k6 image installation test execution

```sh
export SET NODE_OPTIONS=--openssl-legacy-provider && shx rm -rf dist && webpack && cp -r definitions dist/definitions
docker run --env-file .env -i --rm -v $(pwd)/dist:/dist grafana/k6 run /dist/<<test file name>>.js 
```

changing the `<<test file name>>` with the typescript test file name

Local tests can be useful to perform tests against a locally deployed service instead of a remote ones

# K6's Test executor

The chosen test executor is `ramping-arrival-rate`.
Each test execution is configurable by the following configuration parameters:
- rate : the target req/s to be reach
- rampingDuration: duration of the up/down ramp
- duration: target req/s constant rate duration
- preAllocatedVUs: pre allocated VU
- maxVUs: max allocable VUs

Configuring a test execution with the following parameters:
```
rate=50
duration=1m
preAllocatedVUs=1
maxVUs=50
rampingDuration=10s
```
will result in a test with the following stages:

| stage | duration   | req/s                            |
|-------|------------|----------------------------------|
| 1     | 10 seconds | from 0 to 50 linearly increasing |
| 2     | 60 seconds | constant at 50                   |
| 3     | 10 seconds | from 50 to 0 linearly decreasing |

K6 will use up to 50 VUs to support the stage target req/s starting from 1 pre-allocated VU.
The test total duration will be 10+60+10 = 80 seconds 