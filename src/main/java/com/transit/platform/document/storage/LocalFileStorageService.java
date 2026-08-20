package com.transit.platform.document.storage;

import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Implémentation locale (profil dev / installations self-hosted sans S3). Écrit sous
 * storage.local.base-path avec un nom de fichier physique aléatoire — le nom d'origine
 * reste en base (documents.nom_fichier), jamais utilisé comme chemin sur disque.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path basePath;

    public LocalFileStorageService(@Value("${app.storage.local.base-path}") String basePath) {
        this.basePath = Path.of(basePath);
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Impossible d'initialiser le stockage local",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StoredFile store(String suggestedKeyPrefix, String originalFilename, String contentType,
                             InputStream content, long contentLength) {
        String extension = extractExtension(originalFilename);
        String key = suggestedKeyPrefix + "/" + UUID.randomUUID() + extension;
        Path target = basePath.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(key, Files.size(target));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Échec de l'enregistrement du fichier",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return Files.newInputStream(basePath.resolve(storageKey));
        } catch (IOException e) {
            throw BusinessException.notFound(ErrorCode.DOCUMENT_NOT_FOUND, "Fichier introuvable sur le stockage local");
        }
    }

    @Override
    public Optional<String> generatePresignedDownloadUrl(String storageKey, Duration ttl) {
        // Pas d'URL directe en local : le contrôleur streame via retrieve().
        return Optional.empty();
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(basePath.resolve(storageKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Échec de la suppression du fichier",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}
