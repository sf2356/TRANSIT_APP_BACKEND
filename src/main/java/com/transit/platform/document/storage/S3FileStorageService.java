package com.transit.platform.document.storage;

import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation S3-compatible : fonctionne indifféremment avec AWS S3 (endpoint vide,
 * région AWS standard), Cloudflare R2 ou MinIO (endpoint personnalisé + path-style activé).
 * Le choix du provider concret est purement une question de configuration, jamais de code.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorageService(@Value("${app.storage.s3.endpoint}") String endpoint,
                                 @Value("${app.storage.s3.region}") String region,
                                 @Value("${app.storage.s3.bucket}") String bucket,
                                 @Value("${app.storage.s3.access-key}") String accessKey,
                                 @Value("${app.storage.s3.secret-key}") String secretKey) {
        this.bucket = bucket;
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));

        var clientBuilder = S3Client.builder()
                .region(Region.of(region.isBlank() ? "us-east-1" : region))
                .credentialsProvider(credentials);
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(region.isBlank() ? "us-east-1" : region))
                .credentialsProvider(credentials);

        // endpoint renseigné => MinIO ou Cloudflare R2 ; laissé vide => AWS S3 standard.
        if (endpoint != null && !endpoint.isBlank()) {
            clientBuilder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = clientBuilder.build();
        this.s3Presigner = presignerBuilder.build();
    }

    @Override
    public StoredFile store(String suggestedKeyPrefix, String originalFilename, String contentType,
                             InputStream content, long contentLength) {
        String extension = extractExtension(originalFilename);
        String key = suggestedKeyPrefix + "/" + UUID.randomUUID() + extension;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromInputStream(content, contentLength));
            return new StoredFile(key, contentLength);
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Échec de l'envoi du fichier vers le stockage objet",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
        } catch (NoSuchKeyException e) {
            throw BusinessException.notFound(ErrorCode.DOCUMENT_NOT_FOUND, "Fichier introuvable sur le stockage objet");
        }
    }

    @Override
    public Optional<String> generatePresignedDownloadUrl(String storageKey, Duration ttl) {
        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getRequest)
                .build();
        return Optional.of(s3Presigner.presignGetObject(presignRequest).url().toString());
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Échec de la suppression sur le stockage objet",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}
