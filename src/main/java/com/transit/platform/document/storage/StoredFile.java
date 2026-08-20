package com.transit.platform.document.storage;

/** Résultat d'un stockage : la clé technique (chemin/objet) à conserver en base, jamais le fichier lui-même. */
public record StoredFile(String storageKey, long size) {}
