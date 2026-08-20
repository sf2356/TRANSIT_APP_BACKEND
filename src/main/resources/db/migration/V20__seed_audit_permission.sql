-- Permission ajoutée après coup (étape 19) pour la consultation transverse de l'audit —
-- absente du seed initial V10 qui ne couvrait que les permissions déjà identifiées au
-- Prompt 03 §14. Réservée au DIRECTEUR par défaut, ajustable comme toute permission.
INSERT INTO permissions (id, code, module, description) VALUES
    (gen_random_uuid(), 'AUDIT_READ', 'AUDIT', 'Consulter le journal d''audit'),
    (gen_random_uuid(), 'VALIDATION_CREATE', 'VALIDATION', 'Demander une validation'),
    (gen_random_uuid(), 'VALIDATION_DECIDE', 'VALIDATION', 'Approuver ou rejeter une validation'),
    (gen_random_uuid(), 'DASHBOARD_READ', 'DASHBOARD', 'Consulter les tableaux de bord');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'DIRECTEUR' AND p.code IN ('AUDIT_READ', 'VALIDATION_CREATE', 'VALIDATION_DECIDE', 'DASHBOARD_READ')
ON CONFLICT DO NOTHING;

-- Tous les rôles opérationnels peuvent demander une validation et lire les dashboards de leur périmètre.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('RESPONSABLE_LOGISTIQUE','AGENT_TRANSIT','COMMERCIAL','COMPTABLE')
  AND p.code IN ('VALIDATION_CREATE','DASHBOARD_READ')
ON CONFLICT DO NOTHING;

-- Décision de validation réservée à l'encadrement.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'RESPONSABLE_LOGISTIQUE' AND p.code = 'VALIDATION_DECIDE'
ON CONFLICT DO NOTHING;
