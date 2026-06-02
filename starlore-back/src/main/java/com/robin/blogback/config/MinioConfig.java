package com.robin.blogback.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // 设置全局超时（连接、读取、写入各 30 秒）
        client.setTimeout(
            java.time.Duration.ofSeconds(30).toMillis(),
            java.time.Duration.ofSeconds(30).toMillis(),
            java.time.Duration.ofSeconds(30).toMillis()
        );

        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO bucket check failed: {}", e.getMessage());
        }

        return client;
    }

    public String getBucket() { return bucket; }
    public String getEndpoint() { return endpoint; }
}
