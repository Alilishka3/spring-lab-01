package kz.iitu.springlab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Spring Lab Library API")
                .version("1.0")
                .description("RWPSF 3305 — REST API of the book catalogue (labs 5–7)")
                .contact(new Contact().name("Altai Ali, IT1-2409IS")));
    }
}
