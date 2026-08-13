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
public final class OllamaProvider implements AiProvider {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper json;

    @Override
    public <T> T generate(String prompt, Class<T> responseType, double temperature) {
        var converter = new BeanOutputConverter<>(responseType, json);
        var options = OllamaChatOptions.builder()
                .outputSchema(converter.getJsonSchema())
                .temperature(temperature)
                .build();

        var request = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .options(options);

        try {
            var response = request.call();
            return response.entity(converter);
        } catch (Exception e) {
            throw new AiGenerationException("AI generation failed: " + reason(e), e);
        }
    }

    private String reason(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
