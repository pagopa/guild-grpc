package it.pagopa.guild.grpc.hello;

import it.pagopa.guild.grpc.demo.HelloReply;
import it.pagopa.guild.grpc.demo.HelloRequest;
import it.pagopa.guild.grpc.demo.MyServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    @GrpcClient("myclient")
    private MyServiceGrpc.MyServiceBlockingStub greeterStub;

    @Scheduled(fixedDelayString = "1000")
    void post() {
        HelloRequest request = HelloRequest.newBuilder().setName("MyName").build();
        HelloReply reply = greeterStub.sayHello(request);
        System.out.println(reply);
    }
}
