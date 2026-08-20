-- Référentiel global des permissions (module par module).
INSERT INTO permissions (id, code, module, description) VALUES
    (gen_random_uuid(), 'DOSSIER_CREATE', 'DOSSIER', 'Créer un dossier'),
    (gen_random_uuid(), 'DOSSIER_READ', 'DOSSIER', 'Consulter un dossier'),
    (gen_random_uuid(), 'DOSSIER_UPDATE', 'DOSSIER', 'Modifier un dossier'),
    (gen_random_uuid(), 'DOSSIER_DELETE', 'DOSSIER', 'Supprimer un dossier'),
    (gen_random_uuid(), 'DOSSIER_CLOSE', 'DOSSIER', 'Clôturer un dossier'),
    (gen_random_uuid(), 'MARCHANDISE_CREATE', 'MARCHANDISE', 'Créer une marchandise'),
    (gen_random_uuid(), 'MARCHANDISE_READ', 'MARCHANDISE', 'Consulter une marchandise'),
    (gen_random_uuid(), 'MARCHANDISE_UPDATE', 'MARCHANDISE', 'Modifier une marchandise'),
    (gen_random_uuid(), 'DOCUMENT_CREATE', 'DOCUMENT', 'Ajouter un document'),
    (gen_random_uuid(), 'DOCUMENT_READ', 'DOCUMENT', 'Consulter un document'),
    (gen_random_uuid(), 'DOCUMENT_DELETE', 'DOCUMENT', 'Supprimer un document'),
    (gen_random_uuid(), 'COTATION_CREATE', 'COTATION', 'Créer une cotation'),
    (gen_random_uuid(), 'COTATION_READ', 'COTATION', 'Consulter une cotation'),
    (gen_random_uuid(), 'COTATION_UPDATE', 'COTATION', 'Modifier une cotation'),
    (gen_random_uuid(), 'FACTURE_CREATE', 'FACTURE', 'Créer une facture'),
    (gen_random_uuid(), 'FACTURE_READ', 'FACTURE', 'Consulter une facture'),
    (gen_random_uuid(), 'FACTURE_UPDATE', 'FACTURE', 'Modifier une facture'),
    (gen_random_uuid(), 'FACTURE_VALIDATE', 'FACTURE', 'Valider une facture'),
    (gen_random_uuid(), 'PAIEMENT_CREATE', 'PAIEMENT', 'Enregistrer un paiement'),
    (gen_random_uuid(), 'PAIEMENT_READ', 'PAIEMENT', 'Consulter un paiement'),
    (gen_random_uuid(), 'CHARGE_CREATE', 'CHARGE', 'Créer une charge'),
    (gen_random_uuid(), 'CHARGE_READ', 'CHARGE', 'Consulter une charge'),
    (gen_random_uuid(), 'CAISSE_CREATE', 'CAISSE', 'Créer un mouvement de caisse'),
    (gen_random_uuid(), 'CAISSE_READ', 'CAISSE', 'Consulter la caisse'),
    (gen_random_uuid(), 'RECOUVREMENT_CREATE', 'RECOUVREMENT', 'Créer une relance'),
    (gen_random_uuid(), 'RECOUVREMENT_READ', 'RECOUVREMENT', 'Consulter le recouvrement'),
    (gen_random_uuid(), 'UTILISATEUR_CREATE', 'UTILISATEUR', 'Créer un utilisateur'),
    (gen_random_uuid(), 'UTILISATEUR_UPDATE', 'UTILISATEUR', 'Modifier un utilisateur'),
    (gen_random_uuid(), 'PARAMETRE_UPDATE', 'PARAMETRE', 'Modifier les paramètres entreprise'),
    (gen_random_uuid(), 'TIERS_CREATE', 'TIERS', 'Créer un tiers'),
    (gen_random_uuid(), 'TIERS_READ', 'TIERS', 'Consulter un tiers'),
    (gen_random_uuid(), 'TIERS_UPDATE', 'TIERS', 'Modifier un tiers');

-- Rôles système partagés (entreprise_id NULL = catalogue global, affectable dans toute entreprise)
INSERT INTO roles (id, entreprise_id, code, libelle, est_systeme) VALUES
    (gen_random_uuid(), NULL, 'DIRECTEUR', 'Directeur', true),
    (gen_random_uuid(), NULL, 'RESPONSABLE_LOGISTIQUE', 'Responsable logistique', true),
    (gen_random_uuid(), NULL, 'AGENT_TRANSIT', 'Agent de transit', true),
    (gen_random_uuid(), NULL, 'COMMERCIAL', 'Commercial', true),
    (gen_random_uuid(), NULL, 'COMPTABLE', 'Comptable', true);

-- DIRECTEUR : toutes les permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'DIRECTEUR';

-- AGENT_TRANSIT : opérationnel (dossiers, marchandises, documents)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'AGENT_TRANSIT'
  AND p.code IN ('DOSSIER_CREATE','DOSSIER_READ','DOSSIER_UPDATE','MARCHANDISE_CREATE','MARCHANDISE_READ',
                 'MARCHANDISE_UPDATE','DOCUMENT_CREATE','DOCUMENT_READ','TIERS_READ');

-- COMMERCIAL : clients, cotations
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'COMMERCIAL'
  AND p.code IN ('DOSSIER_READ','DOSSIER_CREATE','TIERS_CREATE','TIERS_READ','TIERS_UPDATE',
                 'COTATION_CREATE','COTATION_READ','COTATION_UPDATE');

-- COMPTABLE : facturation, paiements, caisse, recouvrement
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'COMPTABLE'
  AND p.code IN ('DOSSIER_READ','FACTURE_CREATE','FACTURE_READ','FACTURE_UPDATE','FACTURE_VALIDATE',
                 'PAIEMENT_CREATE','PAIEMENT_READ','CHARGE_CREATE','CHARGE_READ','CAISSE_CREATE',
                 'CAISSE_READ','RECOUVREMENT_CREATE','RECOUVREMENT_READ');

-- RESPONSABLE_LOGISTIQUE : supervision opérationnelle
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'RESPONSABLE_LOGISTIQUE'
  AND p.code IN ('DOSSIER_CREATE','DOSSIER_READ','DOSSIER_UPDATE','DOSSIER_CLOSE','MARCHANDISE_CREATE',
                 'MARCHANDISE_READ','MARCHANDISE_UPDATE','DOCUMENT_CREATE','DOCUMENT_READ','TIERS_READ',
                 'UTILISATEUR_UPDATE');
