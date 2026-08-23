-- Nouvelle matrice de permissions par rôle (demande utilisateur, matrice complète fournie).
-- Deux permissions manquantes ajoutées (les écrans existaient déjà mais empruntaient une
-- permission d'un autre module faute de permission dédiée) :
--   - COMPTABILITE_READ (l'écran comptabilité utilisait DASHBOARD_READ jusqu'ici)
--   - VALIDATION_READ (la liste des validations utilisait VALIDATION_CREATE jusqu'ici,
--     ce qui donnait par erreur le droit de consulter à quiconque pouvait aussi créer)
INSERT INTO permissions (id, code, module, description) VALUES
                                                            (gen_random_uuid(), 'COMPTABILITE_READ', 'COMPTABILITE', 'Consulter la comptabilité opérationnelle'),
                                                            (gen_random_uuid(), 'VALIDATION_READ', 'VALIDATION', 'Consulter les demandes de validation')
    ON CONFLICT DO NOTHING;

-- Repart d'une table rase pour les 5 rôles système : plus sûr que d'empiler des correctifs
-- incrémentaux sur les affectations déjà en place depuis V10/V20/V22, avec le risque
-- d'oublier une combinaison — la matrice complète ci-dessous est la seule source de vérité.
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code IN
                                             ('DIRECTEUR', 'RESPONSABLE_LOGISTIQUE', 'AGENT_TRANSIT', 'COMMERCIAL', 'COMPTABLE'));

-- DIRECTEUR : toutes les permissions existantes, sans exception (règle inchangée).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'DIRECTEUR';

-- RESPONSABLE_LOGISTIQUE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'RESPONSABLE_LOGISTIQUE'
  AND p.code IN (
                 'DASHBOARD_READ',
                 'DOSSIER_CREATE','DOSSIER_READ','DOSSIER_UPDATE','DOSSIER_DELETE','DOSSIER_CLOSE',
                 'DOCUMENT_CREATE','DOCUMENT_READ','DOCUMENT_DELETE',
                 'MARCHANDISE_CREATE','MARCHANDISE_READ','MARCHANDISE_UPDATE',
                 'TIERS_READ',
                 'COTATION_READ',
                 'FACTURE_READ',
                 'CHARGE_CREATE','CHARGE_READ',
                 'COMPTABILITE_READ',
                 'VALIDATION_READ','VALIDATION_CREATE','VALIDATION_DECIDE',
                 'AUDIT_READ'
    );

-- AGENT_TRANSIT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'AGENT_TRANSIT'
  AND p.code IN (
                 'DASHBOARD_READ',
                 'DOSSIER_CREATE','DOSSIER_READ','DOSSIER_UPDATE','DOSSIER_DELETE',
                 'DOCUMENT_CREATE','DOCUMENT_READ','DOCUMENT_DELETE',
                 'MARCHANDISE_CREATE','MARCHANDISE_READ','MARCHANDISE_UPDATE',
                 'TIERS_READ',
                 'CHARGE_READ',
                 'VALIDATION_READ'
    );

-- COMMERCIAL
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'COMMERCIAL'
  AND p.code IN (
                 'DASHBOARD_READ',
                 'DOSSIER_READ',
                 'DOCUMENT_READ',
                 'MARCHANDISE_READ',
                 'TIERS_CREATE','TIERS_READ','TIERS_UPDATE',
                 'COTATION_CREATE','COTATION_READ','COTATION_UPDATE',
                 'FACTURE_READ',
                 'RECOUVREMENT_READ',
                 'COMPTABILITE_READ',
                 'VALIDATION_READ'
    );

-- COMPTABLE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'COMPTABLE'
  AND p.code IN (
                 'DASHBOARD_READ',
                 'DOSSIER_READ',
                 'DOCUMENT_READ',
                 'MARCHANDISE_READ',
                 'TIERS_READ',
                 'COTATION_READ',
                 'FACTURE_CREATE','FACTURE_READ','FACTURE_UPDATE','FACTURE_VALIDATE',
                 'PAIEMENT_CREATE','PAIEMENT_READ',
                 'RECOUVREMENT_CREATE','RECOUVREMENT_READ',
                 'CHARGE_CREATE','CHARGE_READ',
                 'CAISSE_CREATE','CAISSE_READ',
                 'COMPTABILITE_READ',
                 'VALIDATION_READ',
                 'AUDIT_READ'
    );