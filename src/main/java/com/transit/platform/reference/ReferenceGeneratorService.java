package com.transit.platform.reference;

import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Génération atomique et concurrent-safe des numéros fonctionnels (DOS-0001, FAC-0001...).
 *
 * Stratégie retenue (voir Prompt 02 §9 et Prompt 03 §39) : une table compteur
 * sequences_numerotation(entreprise_id, type_document, dernier_numero), incrémentée via
 * une seule instruction `UPDATE ... RETURNING`. PostgreSQL sérialise automatiquement les
 * écritures concurrentes sur la même ligne (verrou de ligne implicite le temps de la
 * transaction) : deux créations simultanées dans la même entreprise ne peuvent jamais
 * recevoir le même numéro, sans verrou applicatif explicite.
 *
 * REQUIRES_NEW : la génération du numéro est validée immédiatement et definitivement,
 * même si la transaction métier appelante échoue plus tard et est annulée — on préfère
 * "sauter" un numéro (ex. DOS-0007 jamais utilisé après un rollback) plutôt que de risquer
 * un verrou de ligne prolongé pendant toute la durée d'une transaction métier plus large.
 */
@Service
public class ReferenceGeneratorService {

    private final JdbcTemplate jdbcTemplate;
    private final ParametreEntrepriseRepository parametreRepository;

    public ReferenceGeneratorService(JdbcTemplate jdbcTemplate, ParametreEntrepriseRepository parametreRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.parametreRepository = parametreRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(UUID entrepriseId, ReferenceType type) {
        jdbcTemplate.update(
                "INSERT INTO sequences_numerotation (entreprise_id, type_document, dernier_numero) " +
                "VALUES (?, ?, 0) ON CONFLICT (entreprise_id, type_document) DO NOTHING",
                entrepriseId, type.name());

        Long dernierNumero = jdbcTemplate.queryForObject(
                "UPDATE sequences_numerotation SET dernier_numero = dernier_numero + 1 " +
                "WHERE entreprise_id = ? AND type_document = ? RETURNING dernier_numero",
                Long.class, entrepriseId, type.name());

        String prefixe = resolvePrefixe(entrepriseId, type);
        return prefixe + "-" + String.format("%04d", dernierNumero);
    }

    private String resolvePrefixe(UUID entrepriseId, ReferenceType type) {
        ParametreEntreprise parametres = parametreRepository.findByEntrepriseId(entrepriseId)
                .orElseThrow(() -> new IllegalStateException("Paramètres entreprise introuvables pour " + entrepriseId));
        return switch (type) {
            case DOSSIER -> parametres.getPrefixeDossier();
            case COTATION -> parametres.getPrefixeCotation();
            case FACTURE -> parametres.getPrefixeFacture();
            case PAIEMENT -> parametres.getPrefixePaiement();
        };
    }
}
