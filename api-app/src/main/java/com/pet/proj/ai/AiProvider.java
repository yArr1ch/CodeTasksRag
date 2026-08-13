package com.pet.proj.ai;

public sealed interface AiProvider permits OllamaProvider {
    <T> T generate(String prompt, Class<T> responseType, double temperature);
}
