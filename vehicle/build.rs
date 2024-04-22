fn main() -> Result<(), Box<dyn std::error::Error>> {

    // build and configure server for booking requests
    tonic_build::configure()
        .build_client(false)
        .build_server(true)
        .compile(&["./proto/common/common.proto", "./proto/service/vehicle.proto"], &["./proto"])
        .unwrap_or_else(|e| panic!("protobuf compilation for 'vehicle' server failed! Error: {}", e));


    // build and configure server for localization requests
    tonic_build::configure()
        .build_client(true)
        .build_server(true)
        .compile(&["./proto/common/common.proto", "./proto/service/localization.proto"], &["./proto"])
        .unwrap_or_else(|e| panic!("protobuf compilation for 'localization' stream server failed! Error: {}", e));

    Ok(())
}