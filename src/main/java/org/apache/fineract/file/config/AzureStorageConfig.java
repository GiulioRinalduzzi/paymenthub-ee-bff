package org.apache.fineract.file.config;

import com.azure.storage.blob.BlobClientBuilder;
import org.apache.fineract.config.properties.CloudProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfig {

    private final CloudProperties properties;

    public AzureStorageConfig(CloudProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            value="cloud.azure.enabled",
            havingValue = "true")
    public BlobClientBuilder getClient() {
        BlobClientBuilder client = new BlobClientBuilder();
        client.connectionString(properties.azure().blob().connectionString());
        return client;
    }

}
