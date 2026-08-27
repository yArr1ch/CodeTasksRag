package com.pet.proj.task.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@Profile("messaging-sns-sqs")
class AwsMessagingConfig {

    @Bean(destroyMethod = "close")
    SnsAsyncClient snsAsyncClient(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.endpoint:}") String endpoint,
            @Value("${app.aws.access-key-id:test}") String accessKeyId,
            @Value("${app.aws.secret-access-key:test}") String secretAccessKey) {
        var builder = SnsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(endpoint, accessKeyId, secretAccessKey));
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    SqsAsyncClient sqsAsyncClient(
            @Value("${app.aws.region}") String region,
            @Value("${app.aws.endpoint:}") String endpoint,
            @Value("${app.aws.access-key-id:test}") String accessKeyId,
            @Value("${app.aws.secret-access-key:test}") String secretAccessKey) {
        var builder = SqsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(endpoint, accessKeyId, secretAccessKey));
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private AwsCredentialsProvider credentials(String endpoint, String accessKeyId, String secretAccessKey) {
        if (!StringUtils.hasText(endpoint)) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}
