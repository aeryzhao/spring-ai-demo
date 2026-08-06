package org.aeryzhao.rag.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer(JsonMapper objectMapper) {
        return builder -> builder.messageConverters(converters -> customizeJacksonConverters(converters, objectMapper));
    }

    private void customizeJacksonConverters(List<HttpMessageConverter<?>> converters, JsonMapper objectMapper) {
        List<HttpMessageConverter<?>> updated = new ArrayList<>();
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof JacksonJsonHttpMessageConverter) {
                updated.add(new JacksonJsonHttpMessageConverter(objectMapper));
            } else {
                updated.add(converter);
            }
        }
        converters.clear();
        converters.addAll(updated);
    }
}
