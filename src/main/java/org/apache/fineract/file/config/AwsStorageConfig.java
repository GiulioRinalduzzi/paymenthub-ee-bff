package org.apache.fineract.file.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.fineract.config.properties.CloudProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsStorageConfig {

    private final CloudProperties properties;

    public AwsStorageConfig(CloudProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            value="cloud.aws.enabled",
            havingValue = "true")
    public AmazonS3 s3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(properties.aws().credentials().accessKey(),
                properties.aws().credentials().secretKey());
        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(properties.aws().s3BaseUrl(), properties.aws().region().staticRegion()))
                .build();
    }

}
