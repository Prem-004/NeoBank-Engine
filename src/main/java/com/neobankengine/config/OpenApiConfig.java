package com.neobankengine.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class OpenApiConfig
{
    @Bean
    public OpenAPI neoBankEngineAPI()
    {
        return new OpenAPI().info(new Info().title("NeoBank Engine API")
                .description("API documentation for NeoBank Engine").version("1.0.0"));
    }
}
/*creating an open API object and attching dthe meta data [new Info().]
and the title shown in SWAGGER UI and a short description about it.*/