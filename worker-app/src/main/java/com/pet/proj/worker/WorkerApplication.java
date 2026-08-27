package com.pet.proj.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.submission.persistence.SubmissionEntity;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackageClasses = {SubmissionEntity.class, TaskEntity.class})
@EnableJpaRepositories(basePackageClasses = {SubmissionRepository.class, TaskRepository.class})
public class WorkerApplication {
    static void main(String[] args) { SpringApplication.run(WorkerApplication.class, args); }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
