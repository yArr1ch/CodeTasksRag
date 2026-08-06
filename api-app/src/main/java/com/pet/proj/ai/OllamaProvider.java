package com.pet.proj.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "ollama", matchIfMissing = true)
public class OllamaProvider implements AiProvider {
    private final ChatClient.Builder client;
    private final ObjectMapper json;

    public <T> T generate(String prompt, Class<T> responseType) {
        try {
            var converter = new BeanOutputConverter<>(responseType, json);
            return client.build().prompt().user(prompt)
                    .options(OllamaChatOptions.builder()
                            .outputSchema(converter.getJsonSchema())
                            .build())
                    .call()
                    .entity(converter);
        } catch (Exception e) {
            var reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new IllegalStateException("Ollama returned an invalid structured response: " + reason, e);
        }
    }
}
