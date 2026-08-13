package com.pet.proj.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaProviderTest {
    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec request;
    @Mock
    private ChatClient.CallResponseSpec response;

    private OllamaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OllamaProvider(chatClientBuilder, new ObjectMapper());
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.options(any(OllamaChatOptions.class))).thenReturn(request);
    }

    @Test
    void generate_validStructuredResponse_usesSuppliedTemperature() {
        when(request.call()).thenReturn(response);
        when(response.entity(any(StructuredOutputConverter.class))).thenReturn(new GeneratedResponse("ready"));

        var result = provider.generate("prompt", GeneratedResponse.class, 0.25);

        assertThat(result.value()).isEqualTo("ready");
        var options = ArgumentCaptor.forClass(OllamaChatOptions.class);
        verify(request).options(options.capture());
        assertThat(options.getValue().getTemperature()).isEqualTo(0.25);
    }

    @Test
    void generate_requestFails_wrapsAsRequestFailure() {
        when(request.call()).thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> provider.generate("prompt", GeneratedResponse.class, 0.0))
                .isInstanceOf(AiGenerationException.class)
                .hasMessageContaining("AI generation failed")
                .hasMessageContaining("connection refused")
                .hasRootCauseMessage("connection refused");
    }

    @Test
    void generate_malformedStructuredResponse_wrapsAsStructuredResponseFailure() {
        when(request.call()).thenReturn(response);
        when(response.entity(any(StructuredOutputConverter.class)))
                .thenThrow(new IllegalArgumentException("unexpected end of input"));

        assertThatThrownBy(() -> provider.generate("prompt", GeneratedResponse.class, 0.0))
                .isInstanceOf(AiGenerationException.class)
                .hasMessageContaining("AI generation failed")
                .hasMessageContaining("unexpected end of input")
                .hasRootCauseMessage("unexpected end of input");
    }

    private record GeneratedResponse(String value) {
    }
}
