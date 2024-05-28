package it.pagopa.guild.grpc.booking;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableFeignClients
public class BookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingApplication.class, args);
	}

	@Bean
	public OpenAPI defineOpenApi(@Value("${server.port}") String restServerPort, @Value("${project.version}") String projectVersion) {
		Server server = new Server();
		server.setUrl("http://localhost:" + restServerPort);
		server.setDescription("Development Server");

		Contact myContact = new Contact();
		myContact.setName("Enrico Monte");
		myContact.setEmail("enrico.monte@pagopa.it");

		Info information = new Info()
				.title("Booking service API")
				.version(projectVersion)
				.description("Java SpringBoot REST API to book vehicles for car sharing services.")
				.contact(myContact);

		ExternalDocumentation yamlApiDoc = new ExternalDocumentation().url("v3/api-docs.yaml").description("YAML api doc");

		return new OpenAPI().info(information).servers(List.of(server)).externalDocs(yamlApiDoc);
	}

}
