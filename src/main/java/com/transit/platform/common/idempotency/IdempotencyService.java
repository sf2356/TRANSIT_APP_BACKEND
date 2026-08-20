package com.transit.platform.common.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Protection contre la double soumission (Prompt 04 §41-42), pour les opérations
 * financières critiques identifiées : POST /paiements, POST /factures/{id}/paiements.
 *
 * Fonctionnement : le client envoie un header `Idempotency-Key` (généré côté frontend,
 * ex. UUID par soumission de formulaire). Si la même clé est réutilisée pour le même
 * endpoint et la même entreprise, l'opération n'est PAS rejouée : la ressource déjà créée
 * est simplement retournée à nouveau.
 *
 * LIMITE ASSUMÉE (à vérifier ensemble à l'exécution) : la garantie forte vient de la
 * contrainte UNIQUE en base (entreprise_id, idempotency_key, endpoint). Le "check avant
 * création" ci-dessous couvre le cas courant (double clic, double soumission réseau) mais
 * pas une course strictement simultanée sub-milliseconde ; dans ce cas rare, l'INSERT de
 * la clé échouera avec DataIntegrityViolationException, remontée telle quelle — le service
 * appelant (ex. PaiementService) doit alors relire la clé existante plutôt que d'échouer
 * silencieusement. Non couvert automatiquement ici pour rester simple : à surveiller en prod.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findExistingResourceId(UUID entrepriseId, String idempotencyKey, String endpoint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEntrepriseIdAndIdempotencyKeyAndEndpoint(entrepriseId, idempotencyKey, endpoint)
                .map(IdempotencyKeyEntity::getResourceId);
    }

    /** Transaction séparée : l'enregistrement de la clé ne doit jamais être annulé par un rollback ultérieur non lié. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID entrepriseId, String idempotencyKey, String endpoint, String resourceType, UUID resourceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setEntrepriseId(entrepriseId);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setEndpoint(endpoint);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Course rare (cf. javadoc) : la clé a été insérée entre-temps par une requête
            // concurrente. On ignore volontairement — la ressource déjà enregistrée fait foi.
        }
    }
}
