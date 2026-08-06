package com.pet.proj.ai;

public interface AiProvider {
    <T> T generate(String prompt, Class<T> responseType);
}
