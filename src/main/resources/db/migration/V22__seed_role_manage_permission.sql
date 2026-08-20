-- Prompt 04 §32 : gestion des rôles/permissions via API. Permission dédiée et volontairement
-- réservée au DIRECTEUR par défaut — "un utilisateur normal ne doit pas pouvoir s'attribuer
-- lui-même des permissions" (Prompt 04 §32).
INSERT INTO permissions (id, code, module, description) VALUES
    (gen_random_uuid(), 'ROLE_MANAGE', 'ROLE', 'Créer des rôles personnalisés et gérer leurs permissions');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'DIRECTEUR' AND p.code = 'ROLE_MANAGE'
ON CONFLICT DO NOTHING;
