# Backend — Plateforme Transit, Logistique, Import-Export & Douane

Implémentation Spring Boot 3 / Java 21, conforme à l'architecture (Prompt 01) et au modèle
de données (Prompt 02) validés précédemment.

## État d'avancement (respect strict de l'ordre du Prompt 03 §59)

Conformément à la consigne explicite *« NE PAS essayer de coder tout le backend en une
seule étape »* (§59), cette livraison couvre les **étapes 1 à 9** :

| Étape | Contenu | Statut |
|---|---|---|
| 1 | Configuration projet (Maven, profils, `.env.example`) | ✅ |
| 2 | Base PostgreSQL + Flyway (V1 → V10) | ✅ |
| 3 | Entreprise + Paramètres | ✅ |
| 4 | Utilisateur | ✅ |
| 5 | Rôles + Permissions (RBAC, seed du référentiel) | ✅ |
| 6 | Authentification JWT (register/login/refresh/logout/me) | ✅ |
| 7 | Multi-tenant (`TenantContext`, filtrage systématique par `entreprise_id`) | ✅ |
| 8 | Tiers (clients/fournisseurs) | ✅ |
| 9 | Dossiers (entité pivot, historique, actions contextuelles) | ✅ |
| 10 | Marchandises (rattachées au dossier, sans `entreprise_id` propre — tenant dérivé via `DossierService`) | ✅ |
| 11 | Documents + `FileStorageService` (implémentations Local et S3/MinIO/R2, upload multipart contextualisé, téléchargement par redirection signée ou streaming) | ✅ |
| 12 | Cotations + lignes (montants **toujours** recalculés côté backend, jamais acceptés du client) | ✅ |
| 13 | Factures + lignes (statuts EN_ATTENTE/PARTIELLEMENT_PAYEE/PAYEE calculés exclusivement par `PaiementService`, jamais assignables manuellement) | ✅ |
| 14 | Paiements (séquence transactionnelle stricte §52 : vérif. facture → client → montant → enregistrement → recalcul facture → mouvement caisse → audit) | ✅ |
| 15 | Charges (rattachées au dossier, fournisseur optionnel) | ✅ |
| 16 | Caisse (solde **toujours recalculé**, jamais stocké ; mouvement auto-généré uniquement pour les paiements ESPECES) | ✅ |
| 17 | Recouvrement / Relances (client et facture toujours cohérents ; `GET /factures/{id}/relances` enfin câblé) | ✅ |
| 18 | Validations (relation polymorphe `entite_id` sans FK physique — intégrité garantie exclusivement par `ValidationRequestService`) | ✅ |
| 19 | Audit — service posé dès l'étape 9 ; consultation ajoutée (`GET /api/v1/audit`, permission `AUDIT_READ`) | ✅ |
| 20 | Dashboard (global/direction/opérations/facturation/recouvrement/finance) + rentabilité par dossier (enfin résolue, différée depuis l'étape 9) + comptabilité opérationnelle (§33) | ✅ |
| 21 | Tests — cas critiques additionnels (validation polymorphe, rentabilité avec encaissement > facturé) | ✅ partiel |
| 22 | Swagger/OpenAPI — chaque contrôleur documenté (`@Tag`/`@Operation`), à date pas de relecture globale dédiée | ✅ partiel |
| 23 | Optimisation | ⚠️ voir point 13 ci-dessous, non traité |

**Prompt 04 — API REST, OpenAPI, contrat d'intégration :**

| Élément | Statut |
|---|---|
| Rôles/Permissions en API (`GET/POST/PUT /roles`, `/roles/{id}/permissions`, `GET /permissions`) | ✅ |
| Namespace `/api/v1/parametres` (général, entreprise, numérotation, signature) | ✅ |
| Référentiels statiques (`/api/v1/referentiels/...`) pour les listes déroulantes | ✅ |
| `GET /dossiers/{id}/resume` enrichi (compteurs + rentabilité agrégés en un seul appel) | ✅ |
| Contrat imbriqué `CreateDossierRequest` (`ordreTransit`/`douane`/`trajet`) conforme au §65 | ✅ |
| Idempotency-Key sur `POST /paiements` et `POST /factures/{id}/paiements` | ✅ |
| Correlation-ID (`X-Request-ID`) + logging API structuré | ✅ (ordre de filtre à vérifier, point 21) |
| Rate limiting `/auth/*` (scaffold mémoire, sans dépendance externe) | ✅ |
| PDF `GET /factures/{id}/pdf` et `GET /cotations/{id}/pdf` | ✅ |
| Tests MockMvc bout-en-bout (auth, dossier, idempotence paiement) | ✅ partiel |
| Export Excel/CSV (§60) | ❌ non implémenté (architecture jugée compatible, pas de code) |
| Tests de contrat JSON stricts (§63) | ❌ non implémenté |

## Points signalés (conformément au Prompt 03 §63 : « ne pas modifier silencieusement »)

1. **`GET /api/v1/dossiers/{id}/rentabilite` non implémenté à ce stade.** Le calcul dépend
   des modules Cotation/Facture/Paiement/Charge (étapes 13 à 16), pas encore livrés. Plutôt
   que de renvoyer des données fictives ou une dépendance prématurée vers des tables
   inexistantes, l'endpoint est explicitement différé — voir le commentaire dans
   `DossierController`. Le DTO `DossierRentabiliteResponse` est déjà posé pour la suite.

2. **`POST /api/v1/auth/register` crée une nouvelle entreprise (tenant)**, pas un simple
   utilisateur. Le Prompt 03 §11 ne précisait pas explicitement ce comportement ; c'est la
   seule interprétation cohérente avec le modèle SaaS multi-tenant du Prompt 01/02 (il faut
   bien un point d'entrée pour la toute première inscription). L'ajout d'utilisateurs dans
   une entreprise **existante** passe par `POST /api/v1/utilisateurs` (authentifié).

3. **Rôles système vs rôles personnalisés** : le modèle (Prompt 02 §44) laissait ce point
   ouvert. Choix retenu ici : les 5 rôles système (`DIRECTEUR`, etc.) sont partagés
   (`entreprise_id IS NULL`, seedés une fois en V10) et affectables dans toute entreprise ;
   la création de rôles personnalisés par entreprise n'est pas encore exposée via API (la
   colonne `entreprise_id` de `roles` le permet déjà côté modèle) — à confirmer avec le
   métier avant de l'exposer.

4. **Permissions par défaut par rôle** (V10) sont une proposition de démarrage, pas une
   spécification figée — cohérent avec Prompt 03 §15 (*« Nous réaliserons une validation
   métier avec l'entreprise avant de figer les permissions finales »*). Modifiable sans
   changement de code via `role_permissions`.

## Points signalés — étapes 10 à 12

5. **`documents.facture_id`** existe déjà en base (V12) mais n'est pas encore exposé via
   l'API : le module Facture n'étant pas livré, l'associer maintenant créerait une
   dépendance vers une ressource qui n'existe pas encore côté service. La contrainte FK
   correspondante sera ajoutée dans la migration qui créera `factures` (étape 13).

6. **Stockage : mode par défaut = local.** Le provider bascule uniquement via
   `app.storage.provider=s3` (+ variables `STORAGE_S3_*`) — aucun changement de code requis
   pour passer à AWS S3, Cloudflare R2 ou MinIO (`S3FileStorageService` utilise
   `endpointOverride` + `forcePathStyle` pour les deux derniers). À tester avec un vrai
   bucket avant mise en production, non vérifiable dans cet environnement.

7. **Cotation modifiable uniquement au statut `BROUILLON`** (ajout/modif/suppression de
   lignes, suppression de la cotation) : interprétation ajoutée pour éviter qu'une cotation
   déjà envoyée au client change silencieusement de montant — à confirmer avec le métier,
   cohérent avec l'esprit du Prompt 02 §44 point 2 (brouillons supprimables).

## Points signalés — étapes 13 à 16 (partie financière)

8. **Dépassement de paiement → REFUS strict.** Le Prompt 03 §26/§32 laissait le choix entre
   "REFUSER ou traiter selon règle métier définie". Décision retenue ici : un paiement dont
   le montant dépasse le reste à payer d'une facture est **rejeté** (`INVALID_AMOUNT`,
   422), plutôt qu'accepté en excédent/avoir. C'est le choix le plus sûr par défaut, mais il
   empêche par exemple un client qui paie volontairement un léger surplus. Si le métier
   souhaite gérer les avoirs/notes de crédit, cela nécessitera une nouvelle entité dédiée —
   à ne pas improviser en modifiant silencieusement cette règle.

9. **Statuts de facture EN_ATTENTE/PARTIELLEMENT_PAYEE/PAYEE = calculés, jamais assignables
   manuellement.** `FactureService.changeStatut` rejette explicitement toute tentative de
   les positionner à la main (seuls BROUILLON/EMISE/ANNULEE le sont) — ils appartiennent à
   `PaiementService.recalculerApresPaiement`, seul point d'écriture de ces trois champs.

10. **Règle Paiement ↔ Caisse (Prompt 03 §53) :** seul un paiement en mode `ESPECES` génère
    automatiquement un mouvement de caisse. Les autres modes (virement, chèque, mobile
    money, carte) sont considérés comme ne transitant pas par la caisse physique et ne
    créent aucun mouvement. **Décision à valider avec le métier** — notamment le cas du
    mobile money, qui peut selon les entreprises être assimilé à de la caisse. Le point
    d'extension unique est `CaisseService.creerDepuisPaiement`, donc modifiable sans
    toucher à `PaiementService`.

11. **Annulation d'un paiement** repasse la facture par un recalcul complet à partir des
    paiements `VALIDE` restants (jamais une simple soustraction en mémoire) — élimine tout
    risque de désynchronisation cumulative sur des annulations répétées.

12. **`GET /factures/{id}/relances`** (Prompt 03 §25) n'est pas encore exposé : dépend du
    module Recouvrement (étape 17), non livré à ce stade.

## Points signalés — étapes 17 à 20

13. **Performance du dashboard (`/dashboard/direction`) — N+1 assumé.** Le classement des
    dossiers rentables/à risque interroge individuellement jusqu'à 50 dossiers récents
    (une requête facture + une requête charge par dossier) plutôt qu'une requête SQL
    agrégée unique. Documenté dans `DashboardService` comme point à traiter à l'étape 23
    si la volumétrie le justifie (vue matérialisée ou requête agrégée `GROUP BY dossier_id`
    sur `factures`/`charges` jointes) — **non corrigé ici**, à faire vérifier par vous.

14. **Marge estimée du dashboard = encaissé − charges** (et non facturé − charges), pour
    rester cohérent avec `DossierRentabiliteService` et l'exemple du Prompt 03 §32. Si le
    métier attend plutôt une marge "facturée" (accrual) indépendante des encaissements,
    c'est une constante à changer à un seul endroit (`DossierRentabiliteService.calculer`
    et `DashboardService.calculerRentabiliteEchantillon`), pas une réécriture.

15. **`ValidationRequest` vérifie l'existence ET l'appartenance au tenant** de l'entité
    ciblée avant toute création (`verifierExistenceEtTenant`), en s'appuyant sur les
    repositories/services des modules concernés plutôt que sur une FK — c'est le seul
    rempart d'intégrité pour cette relation polymorphe assumée au Prompt 02 §44. Un type
    d'entité non listé dans `ENTITE_TYPES_SUPPORTEES` (DOSSIER/FACTURE/COTATION/PAIEMENT/
    CHARGE) est rejeté explicitement plutôt que silencieusement accepté.

16. **Permissions `AUDIT_READ`, `VALIDATION_CREATE`, `VALIDATION_DECIDE`, `DASHBOARD_READ`
    ajoutées après coup** (migration V20, absentes du seed initial V10 qui ne couvrait que
    les permissions explicitement listées au Prompt 03 §14). Attribution par défaut
    proposée : `AUDIT_READ` au DIRECTEUR uniquement ; `VALIDATION_DECIDE` au DIRECTEUR et
    au RESPONSABLE_LOGISTIQUE ; `VALIDATION_CREATE`/`DASHBOARD_READ` à tous les rôles
    opérationnels. **À valider avec le métier**, comme toute la matrice de permissions
    (cf. point déjà signalé à l'étape 1-9 du README).

17. **`DashboardController` protège TOUTE la classe** avec `@PreAuthorize("hasAuthority('DASHBOARD_READ')")`
    au niveau du contrôleur plutôt que méthode par méthode — cohérent puisque les 6 vues
    partagent la même permission, mais à changer si une vue (ex. `/finance`) doit un jour
    être restreinte plus finement (ex. réservée au COMPTABLE/DIRECTEUR).

## Points signalés — Prompt 04 (à corriger ensemble à l'exécution)

18. **Bug corrigé pendant ce prompt : `/api/v1/auth/register` n'était pas dans la liste des
    endpoints publics de `SecurityConfig`** (Prompt 03). Sans ce correctif, aucune première
    inscription n'aurait jamais été possible — le endpoint aurait renvoyé 401 avant même
    d'atteindre le contrôleur. Corrigé ici ; **à confirmer que rien d'autre ne dépendait de
    ce comportement** (peu probable vu qu'il empêchait strictement tout usage).

19. **Limite architecturale des rôles système révélée par le Prompt 04 §32.** Les rôles
    système (DIRECTEUR, etc.) sont des lignes **partagées entre toutes les entreprises**
    (`entreprise_id IS NULL`, décision du Prompt 02/03). `PUT /roles/{id}/permissions`
    modifierait donc le comportement de **toutes les entreprises** si on l'autorisait sur un
    rôle système — c'est un défaut de conception, pas une fonctionnalité. `RoleService`
    **refuse explicitement** (422) toute modification d'un rôle système et oriente vers la
    création d'un rôle personnalisé. **Ce point mérite une vraie décision produit** : soit
    dupliquer les rôles système par entreprise à la création du tenant (migration de
    données requise), soit assumer que les rôles système sont volontairement non
    personnalisables et que toute personnalisation passe par un rôle custom dès le départ.

20. **`CreateDossierRequest` restructuré en JSON imbriqué** (`ordreTransit`/`douane`/`trajet`)
    pour coller à l'exemple du Prompt 04 §65 — **breaking change** par rapport au contrat
    plat du Prompt 03. `UpdateDossierRequest` reste volontairement plat (non demandé
    explicitement) : **incohérence assumée à trancher ensemble** — faut-il aussi imbriquer
    la mise à jour pour la cohérence, ou est-ce disproportionné pour une mise à jour ?

21. **Ordre des filtres `ApiLoggingFilter`/`CorrelationIdFilter`/`RateLimitFilter` non
    vérifié à l'exécution.** Le raisonnement (documenté dans le code) est que
    `@Order(LOWEST_PRECEDENCE)` place `ApiLoggingFilter` juste avant la servlet, donc après
    l'authentification JWT interne à la chaîne Spring Security — permettant de logger
    l'utilisateur authentifié. **C'est le tout premier point à vérifier au premier
    démarrage** : si les logs affichent systématiquement `user=anonyme` alors qu'une
    requête authentifiée a réussi, l'ordre des filtres doit être ajusté explicitement (ex.
    via `FilterRegistrationBean` avec un ordre numérique explicite plutôt que `@Order`).

22. **Rate limiting en mémoire, mono-instance.** Documenté comme scaffold volontairement
    sans dépendance externe (pas de Redis/Bucket4j) — protection réduite si l'application
    est déployée en plusieurs instances derrière un load balancer. Acceptable pour une V1
    mono-instance ; **à migrer vers un backend partagé si une architecture multi-instance
    est planifiée**.

23. **Idempotence : la garantie forte vient de la contrainte UNIQUE en base**
    (`idempotency_keys`), pas du contrôle applicatif "vérifier avant créer" qui est
    seulement une optimisation pour le cas courant. Une course strictement simultanée
    (sub-milliseconde) sur la même clé lèverait une `DataIntegrityViolationException` côté
    `IdempotencyService.record()`, actuellement **avalée silencieusement** plutôt que de
    relire et retourner la ressource existante — **limite connue, non corrigée**, car ce
    cas est extrêmement rare pour un double-clic humain (contrairement à un vrai retry
    automatique agressif côté client).

24. **`FacturePdfService`/`CotationPdfService` ne rendent pas le logo comme image**, juste
    comme texte ("Logo : chemin"). Rendre l'image réelle nécessiterait de retélécharger le
    fichier depuis `FileStorageService` à chaque génération de PDF (coût I/O par appel) —
    reporté volontairement, **à discuter si l'affichage du logo est un prérequis bloquant**
    pour la mise en production.

25. **Deux namespaces coexistent pour les informations d'entreprise** :
    `/api/v1/entreprise` (Prompt 03) et `/api/v1/parametres/entreprise` (Prompt 04 §31,
    explicitement demandé). Les deux pointent vers le même service, aucune duplication de
    logique, mais **c'est une redondance d'API à trancher** : garder les deux, ou déprécier
    l'un des deux pour Angular/Flutter (Prompt 05/06) ?

26. **Tests de contrat JSON stricts (§63) non implémentés** — seule la structure est
    respectée par construction (records Java + Jackson), mais aucun test n'échouerait
    automatiquement si un champ était renommé par erreur. À ajouter si la stabilité du
    contrat devient critique (ex. avant que Angular/Flutter ne soient développés en
    parallèle du backend).

27. **Export Excel/CSV (§60) non implémenté**, seulement jugé compatible avec
    l'architecture actuelle (pagination + filtres déjà en place, il suffirait d'ajouter un
    endpoint `/export` par module avec un `Content-Type` adapté). Non prioritaire tant
    qu'aucun besoin réel n'est confirmé, conformément à la consigne du §60.

## Démarrage local

```bash
cp .env.example .env        # puis éditer les valeurs
docker compose up -d        # démarre PostgreSQL
export $(cat .env | xargs)  # ou configurer les variables autrement
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI : `http://localhost:8080/swagger-ui.html`
Health check : `http://localhost:8080/actuator/health`

Nouveautés Prompt 04 à tester en priorité :
- `POST /api/v1/paiements` avec un header `Idempotency-Key: <uuid>` envoyé deux fois de
  suite → un seul paiement doit être créé (même `id` retourné les deux fois).
- 11 requêtes rapprochées sur `POST /api/v1/auth/login` avec des identifiants invalides →
  la 11e doit renvoyer `429 TOO_MANY_REQUESTS`.
- Toute réponse doit porter un header `X-Request-ID` (généré si absent de la requête).
- `GET /api/v1/factures/{id}/pdf` et `GET /api/v1/cotations/{id}/pdf` doivent renvoyer un
  PDF valide (`Content-Type: application/pdf`).

Comptes de démonstration (profil `dev`, mot de passe `Demo1234!`) :
`admin@transit-demo.test` (DIRECTEUR), `comptable@transit-demo.test`,
`agent@transit-demo.test`, `commercial@transit-demo.test`, `logistique@transit-demo.test`.

Le seed crée un cycle complet illustrant les étapes 9 à 16 : dossier DOS-0001 → marchandise
→ cotation COT-0001 → facture FAC-0001 (statut `PARTIELLEMENT_PAYEE`) → paiement PAY-0001
(150 000 XOF sur 295 000 XOF) → charge "Droit de douane". Le seed ne couvre pas encore les
étapes 17-20 (relances, validations) — à enrichir si des données de démo y sont utiles.

## Prochaine itération suggérée

- Traiter les points signalés ci-dessus (13 à 17), en particulier la performance du
  dashboard direction si un test de charge le justifie.
- Étoffer la couverture de tests (étape 21) : le brief demande des tests unitaires sur les
  services, la génération de numéros, les calculs financiers, les permissions et la
  validation — seule une partie représentative des cas critiques du §47 est couverte ici.
- Relecture Swagger globale (étape 22) : vérifier la cohérence des exemples, codes
  d'erreur et descriptions sur l'ensemble des ~40 endpoints avant de les considérer
  "prêts pour intégration frontend/mobile" (Prompt 04).

> Note d'environnement : la construction (`mvn compile`/`mvn test`) nécessite un accès
> réseau à Maven Central, non disponible dans cet environnement d'exécution. Le code n'a
> donc pas pu être compilé/exécuté ici — à valider dans un environnement de développement
> standard avant intégration.

## Sécurité multi-tenant — règle non négociable

Chaque service métier résout l'entreprise courante **uniquement** via `TenantContext`
(lui-même alimenté par le JWT vérifié dans `JwtAuthenticationFilter`). Aucun DTO de requête
n'accepte de champ `entrepriseId` : toute tentative d'accès à une ressource d'une autre
entreprise renvoie `404 DOSSIER_NOT_FOUND` / `TIERS_NOT_FOUND` / etc., jamais les données.
Voir les tests `DossierMultiTenantIsolationIT` et `ReferenceGeneratorServiceIT` qui couvrent
respectivement les cas critiques n°1 et n°2 du Prompt 03 §47.

## Prochaine itération suggérée

Étapes 10 à 12 (Marchandises, Documents avec `FileStorageService` abstrait S3-compatible,
Cotations + lignes avec calcul backend des montants), en réutilisant strictement les mêmes
patrons que le module Dossier ci-dessus.
