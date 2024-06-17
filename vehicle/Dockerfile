# Generating folders 
FROM rust:1.76 as build


RUN apt-get update && apt-get install -y protobuf-compiler

# create a new empty shell project
RUN USER=root cargo new --bin app
WORKDIR /app
COPY ./Cargo.lock ./Cargo.lock
COPY ./Cargo.toml ./Cargo.toml
COPY ./build.rs ./build.rs
COPY ./proto ./proto

# Compiling dependencies
RUN cargo build --release
RUN rm src/*.rs
COPY ./src ./src
RUN rm ./target/release/deps/*

# Compiling binary
RUN cargo build --release


# Copy binary file, built in previous steps, in a new image
FROM rust:1.76
COPY --from=build /app/target/release/vehicle .

# Run binary file
EXPOSE 50051
CMD ["./vehicle"]