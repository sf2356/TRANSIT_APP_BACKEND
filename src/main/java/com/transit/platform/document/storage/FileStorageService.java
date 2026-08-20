package com.transit.platform.document.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction de stockage de fichiers (Prompt 01 §17, Prompt 03 §23).
 *
 * PostgreSQL ne stocke JAMAIS le contenu binaire — uniquement les métadonnées et la clé
 * renvoyée par store(). Deux implémentations : LocalFileStorageService (dev/self-hosted)
 * et S3FileStorageService (AWS S3, Cloudflare R2, MinIO — tous compatibles S3). Basculer
 * de l'une à l'autre est un simple changement de propriété (app.storage.provider),
 * transparent pour DocumentService et pour le reste de l'application.
 */
public interface FileStorageService {

    /** Stocke le flux sous une clé unique (généralement préfixée par entrepriseId/dossierId) et retourne cette clé. */
    StoredFile store(String suggestedKeyPrefix, String originalFilename, String contentType, InputStream content, long contentLength);

    /** Flux de lecture du fichier — utilisé si aucune URL directe n'est disponible (mode local). */
    InputStream retrieve(String storageKey);

    /**
     * URL de téléchargement direct et temporaire (ex. URL S3 pré-signée), si le provider le
     * permet. Absent en mode local : le contrôleur doit alors streamer via retrieve().
     */
    Optional<String> generatePresignedDownloadUrl(String storageKey, Duration ttl);

    void delete(String storageKey);
}
