package it.pagopa.guild.grpc.hello;

import io.grpc.stub.StreamObserver;
import it.pagopa.guild.grpc.demo.HelloReply;
import it.pagopa.guild.grpc.demo.HelloRequest;
import it.pagopa.guild.grpc.demo.MyServiceGrpc;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ServerService extends MyServiceGrpc.MyServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
