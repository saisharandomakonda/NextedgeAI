package com.syncari.core.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.syncari.core.event.store.BigQueryEventStore;
import com.syncari.core.event.store.EventStore;
import com.syncari.core.file.GCSFileManager;
import com.syncari.core.service.secrets.SecretManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class GoogleConfig {
    @Autowired
    AppConfig appConfig;
    @Autowired
    SecretManager secretManager;

    @Bean("bigQuery")
    public BigQuery bigQueryService() throws Exception {
        Collection<String> scopes = List.of("https://www.googleapis.com/auth/bigquery",
                "https://www.googleapis.com/auth/bigquery.insertdata");
        Credentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(appConfig.gcpCredentialsKey)))
                .createScoped(scopes);
        return BigQueryOptions.newBuilder()
                .setProjectId(appConfig.getGcpProjectId())
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean("storageService")
    public Storage storageService() throws Exception {
    	Credentials credentials = GoogleCredentials
    			.fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(appConfig.getGcpCredentialsKey())));
    	
    	return StorageOptions.newBuilder()
                .setProjectId(appConfig.getGcpProjectId())
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean
    public BigQueryWriteClient bigQueryWriteClient(@Qualifier("bigQueryWriteSettings") BigQueryWriteSettings bigQueryWriteSettings) {
        try {
            return BigQueryWriteClient.create(bigQueryWriteSettings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create BigQueryWriteClient", e);
        }
    }

    @Bean("bigQueryWriteSettings")
    public BigQueryWriteSettings bigQueryWriteSettings() {
        Collection<String> scopes = List.of(
                "https://www.googleapis.com/auth/bigquery",
                "https://www.googleapis.com/auth/bigquery.insertdata"
        );
        try (InputStream credentialsStream = new ByteArrayInputStream(Base64.getDecoder().decode(appConfig.gcpCredentialsKey))) {
            Credentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(scopes);
            CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            BigQueryWriteSettings settings = BigQueryWriteSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
            log.info("BigQueryWriteSettings created with CredentialsProvider: {}", settings.getCredentialsProvider() != null);
            return settings;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create BigQueryWriteSettings from credentials", e);
        }
    }

    @Bean("gcsCfFileManager")
    public GCSFileManager gcsCfFileManager() throws Exception {
        Credentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(appConfig.getCfDeployerCredentialsKey())));

        Storage s = StorageOptions.newBuilder()
                .setProjectId(appConfig.getGcpProjectId())
                .setCredentials(credentials)
                .build()
                .getService();
        return new GCSFileManager(s, appConfig);
    }

    @Bean("gcsImportedFilesFileManager")
    public GCSFileManager gcsImportedFilesFileManager() throws Exception {
        String importedFilesCredentials = "local".equalsIgnoreCase(appConfig.getEnvName())
                ? appConfig.getGcpCredentialsKey()
                : secretManager.getSecret("imported_files_gcp_credentials_encoded_key");
        Credentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(
                Base64.getDecoder().decode(importedFilesCredentials)));

        Storage s = StorageOptions.newBuilder()
                .setProjectId(appConfig.getGcpProjectId())
                .setCredentials(credentials)
                .build()
                .getService();
        return new GCSFileManager(s, appConfig);
    }

    @Bean
    @DependsOn({"bigQuery"})
    public EventStore eventStore() {
        return new BigQueryEventStore();
    }
}
