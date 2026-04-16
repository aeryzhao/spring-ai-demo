package org.aeryzhao.rag.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer(ObjectMapper objectMapper) {
        return builder -> builder.messageConverters(converters -> customizeJacksonConverters(converters, objectMapper));
    }

    private void customizeJacksonConverters(List<HttpMessageConverter<?>> converters, ObjectMapper objectMapper) {
        ObjectMapper safeMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setObjectMapper(safeMapper);
            }
        }
    }
}
