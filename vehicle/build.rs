fn main() -> Result<(), Box<dyn std::error::Error>> {

    // build and configure server for booking requests
    tonic_build::configure()
        .build_client(false)
        .build_server(true)
        .compile(&["./proto/service/booking.proto"], &["./proto"])
        .unwrap_or_else(|e| panic!("protobuf compilation for 'vehicle' service failed! Error: {}", e));

        // build and configure server for booking requests
        tonic_build::configure()
            .build_server(false)
            .build_client(true)
            .compile(&["./proto/service/localization.proto"], &["./proto"])
            .unwrap_or_else(|e| panic!("protobuf compilation for 'vehicle' service failed! Error: {}", e));

    Ok(())
}