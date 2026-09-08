# Xaam-Xaam+ — Backend

Backend Spring Boot du projet **Xaam-Xaam+**, plateforme d'accompagnement scolaire par IA guidée
(méthode socratique) via l'API Anthropic Claude.

## Stack

- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL + Flyway (migrations versionnées)
- MapStruct (mapping entité ↔ DTO), Lombok
- springdoc-openapi (Swagger UI)

## Prérequis

- JDK 17+
- Maven 3.9+ (le projet n'embarque pas le Maven Wrapper — installe Maven localement,
  ou génère le wrapper toi-même avec `mvn -N io.takari:maven:wrapper` une fois le
  projet ouvert dans ton IDE)
- Docker (pour lancer PostgreSQL localement via `docker-compose`) — ou une instance
  PostgreSQL déjà disponible (ex. Supabase/Neon comme prévu dans le cahier des charges)
- Une clé API Anthropic valide

## Démarrage rapide

### 1. Lancer la base de données locale

```bash
docker compose up -d
```

Cela démarre un PostgreSQL sur `localhost:5432` avec les identifiants définis dans
`docker-compose.yml` (base `xaamxaam`, utilisateur `xaamxaam`).

### 2. Configurer les variables d'environnement

Copie `.env.example` et adapte les valeurs, en particulier `ANTHROPIC_API_KEY` :

```bash
cp .env.example .env
```

Exporte-les dans ton shell avant de lancer l'application (ou configure-les dans
ton IDE / lance avec `export $(cat .env | xargs) && mvn spring-boot:run`).

### 3. Lancer l'application

```bash
mvn spring-boot:run
```

Au démarrage, Flyway applique automatiquement la migration `V1__init_schema.sql`
et crée tout le schéma (tables `utilisateurs`, `eleves`, `exercices`, etc.).

L'application démarre sur **http://localhost:8080**.

## Tester l'API via Swagger

Une fois l'application lancée, ouvre :

**http://localhost:8080/swagger-ui.html**

Toute la documentation interactive de l'API y est disponible, organisée par rôle
(Authentification, Élève, Parent, Enseignant, Administrateur établissement, Superadmin).

### Flux de test recommandé

1. **Créer un compte établissement de test** : passe d'abord par le superadmin pour
   créer un établissement (`POST /api/admin/etablissements`) — nécessite un premier
   compte Superadmin (voir note ci-dessous).
2. **Créer un compte Élève** : `POST /api/auth/register` avec `"role": "ELEVE"`.
   Les comptes Élève et Parent sont activés immédiatement.
3. **Se connecter** : `POST /api/auth/login` → récupère le `token` JWT retourné.
4. Dans Swagger UI, clique sur **Authorize** (cadenas en haut à droite) et colle le
   token (sans le préfixe `Bearer `).
5. **Soumettre un exercice** : `POST /api/eleve/exercices`.
6. **Soumettre une tentative** : `POST /api/eleve/exercices/{id}/tentatives` →
   observe la relance socratique générée par l'IA (jamais la réponse finale).
7. **Débloquer un indice** : `POST /api/eleve/exercices/{id}/indices/suivant`.
8. **Clôturer par la reformulation** : `POST /api/eleve/exercices/{id}/reformulation`
   → l'IA retourne un score de compréhension sur 100.

### Créer le tout premier compte Superadmin

Le endpoint `/api/auth/register` avec `"role": "SUPERADMIN"` crée un compte **inactif**
par défaut (cohérent avec la validation manuelle décrite en section 12 du cahier des
charges). Pour le tout premier superadmin, il faut donc l'activer manuellement en base :

```sql
UPDATE utilisateurs SET actif = true WHERE email = 'ton-email@exemple.com';
```

Les superadmins suivants pourront être validés directement depuis l'API par un
superadmin déjà actif.

## Points à compléter avant la mise en production

- **Parsing JSON de l'évaluation IA** (`SocratiqueService.parserReformulation`) :
  parsing volontairement simplifié pour le MVP, à remplacer par un vrai
  `ObjectMapper.readValue(...)` une fois le format de réponse stabilisé et testé.
- **Endpoint "messages globaux"** du Superadmin (diffusion d'annonces) : le canal
  (email, notification in-app...) n'est pas encore précisé dans le cahier des charges.
- **Clé OCR.space par défaut** (`helloworld`) : c'est la clé de démo publique du
  fournisseur, très limitée en volume. Crée ton propre compte gratuit sur
  [ocr.space/ocrapi/freekey](https://ocr.space/ocrapi/freekey) avant tout usage réel.
- **Intégration de paiement réelle** (Wave, Orange Money, carte) : l'entité
  `Abonnement` existe (statuts, montants) mais rien ne débite réellement un parent
  ou un établissement aujourd'hui.
- **Déploiement** : pas de `Dockerfile` ni de profil `application-prod.yml` pour
  l'instant — à préparer avant la démo d'octobre (cible Railway/Render, section 6
  du cahier des charges).
- **Vérification d'email non bloquante** : voir la note dans `EmailVerificationService`
  si tu veux la rendre obligatoire pour se connecter.
- **Étape de reformulation "obligatoire"** : rien n'empêche aujourd'hui un élève de
  laisser un exercice sans reformulation ; à décider si ça doit être contraint côté
  backend (ex. bloquer un nouvel exercice tant que le précédent n'est pas clôturé)
  ou géré uniquement côté UX frontend.

## OCR — extraction de texte depuis une photo

`POST /api/eleve/ocr` (multipart, champ `image`) appelle l'API externe **OCR.space**
et retourne le texte détecté. Le flux complet pour un élève :

1. Prend une photo de son exercice.
2. `POST /api/eleve/ocr` avec l'image → récupère `texteExtrait`.
3. `POST /api/eleve/exercices` en utilisant ce texte comme `enonce` (et en renseignant
   `sourceImageUrl` si tu stockes l'image quelque part pour traçabilité).

Le service (`OcrService`) est isolé : pour changer de fournisseur (Google Cloud Vision,
etc.), seule cette classe est à réécrire.

## Quota de requêtes IA par élève

Chaque tentative, déblocage d'indice ou reformulation déclenche un appel à l'API
Claude et consomme donc une requête du quota quotidien (`QuotaIaService`), fixé par
défaut à **30 requêtes/jour/élève** (`QUOTA_REQUETES_JOUR` dans `.env`).

- Le quota est vérifié et incrémenté **avant** l'appel à Claude, dans une transaction
  séparée avec verrou pessimiste (`QuotaIaRepository.findForUpdateByEleveIdAndDate`)
  pour rester fiable même en cas de requêtes concurrentes.
- Une fois le quota atteint, l'API répond **HTTP 429** avec un message explicite
  (`QuotaDepasseException`, gérée par `GlobalExceptionHandler`).
- `GET /api/eleve/quota-ia-restant` permet au frontend d'afficher le nombre de
  requêtes restantes pour la journée.

## Gestion des comptes par l'administrateur d'établissement

`AdminEtablissementController` expose maintenant la création/désactivation des comptes
élèves et enseignants, entièrement **scopée à l'établissement de l'admin connecté** :

- `POST /api/admin-etablissement/eleves` / `GET /api/admin-etablissement/eleves`
- `POST /api/admin-etablissement/eleves/{id}/desactiver` / `.../reactiver`
- `POST /api/admin-etablissement/enseignants` / `GET /api/admin-etablissement/enseignants`
- `POST /api/admin-etablissement/enseignants/{id}/desactiver` / `.../reactiver`

Point de sécurité important (voir `GestionComptesEtablissementService`) : l'établissement
n'est **jamais** pris depuis le corps de la requête envoyée par le client, mais toujours
déduit du compte `AdminEtablissement` authentifié. Un admin ne peut donc pas, même en
modifiant un ID dans sa requête, créer ou modifier un compte pour un autre établissement
que le sien — vérifié par les tests `GestionComptesEtablissementServiceTest`.

## Auto-activation des comptes en développement local

En plus du seed de données de test, un second mécanisme — totalement indépendant —
active **automatiquement tous les comptes et établissements en attente à chaque
démarrage**, sans avoir besoin de taper une requête SQL manuelle à chaque fois que tu
crées un nouveau compte pendant tes tests (via `/api/auth/register` ou l'inscription
sur le frontend).

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou dans IntelliJ (**une seule fois** — ça reste actif à chaque redémarrage ensuite) :
**Run → Edit Configurations → Environment variables** :
```
SPRING_PROFILES_ACTIVE=dev
```

À chaque démarrage, tous les comptes `ADMIN_ETABLISSEMENT`/`ENSEIGNANT`/`SUPERADMIN`
créés depuis le dernier redémarrage (et qui étaient donc inactifs) sont activés
automatiquement, ainsi que tout établissement en attente de validation. Le résultat est
loggé au démarrage (`[dev] Auto-activation : X compte(s)...`).

**Réservé au développement local** : ce comportement contournerait complètement le
contrôle manuel des inscriptions voulu en section 12 du cahier des charges — le profil
`dev` ne doit jamais être actif en production.

Tu peux combiner les deux profils (`dev,seed`) pour avoir à la fois le jeu de données
de test complet **et** l'auto-activation de tout nouveau compte créé par la suite :
```
SPRING_PROFILES_ACTIVE=dev,seed
```

## Données de test (seed)

Un jeu de données complet et cohérent peut être injecté automatiquement au démarrage,
via le profil Spring `seed` — **jamais actif par défaut**, pour éviter tout risque en
production.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

Ou avec Docker / variable d'environnement :
```bash
SPRING_PROFILES_ACTIVE=seed
```

**Idempotent** : si un superadmin `superadmin@xaamxaam.sn` existe déjà, le seed est
ignoré (log d'info) — sans danger de relancer l'app plusieurs fois avec ce profil.

**Ne nécessite PAS `ANTHROPIC_API_KEY`** : les exercices, tentatives et reformulations
sont insérés directement en base avec un contenu factice mais réaliste, sans jamais
appeler l'API Claude.

### Comptes créés (mot de passe commun : `Password123!`)

| Rôle | Emails |
|---|---|
| Superadmin | `superadmin@xaamxaam.sn` |
| Admin établissement | `admin.lycee@xaamxaam.sn`, `admin.college@xaamxaam.sn` |
| Enseignant | `prof.maths@xaamxaam.sn`, `prof.physique@xaamxaam.sn`, `prof.francais@xaamxaam.sn` |
| Élève | `eleve.seydou@xaamxaam.sn` (3ème), `eleve.awa@xaamxaam.sn` (2nde), `eleve.moussa@xaamxaam.sn` (Terminale), `eleve.fatou@xaamxaam.sn` (1ère), `eleve.ibrahima@xaamxaam.sn` (4ème), `eleve.mariama@xaamxaam.sn` (Licence 1, sans établissement) |
| Parent | `parent.diop@xaamxaam.sn` (rattaché à Seydou et Ibrahima), `parent.ndiaye@xaamxaam.sn` (rattaché à Awa) |

Tous les comptes ci-dessus sont **actifs immédiatement**, y compris Enseignant et Admin
établissement (contrairement au flux normal qui nécessite validation superadmin).

### Données incluses

- 2 établissements (Lycée + Collège), licence active, déjà validés
- 3 exercices types dans la bibliothèque (un par enseignant)
- Une quinzaine d'exercices répartis entre les élèves : certains **terminés** (avec
  reformulation et score), d'autres **en cours** (avec une tentative)
- **Badges attribués automatiquement** via la vraie logique de `BadgeService` (pas de
  données inventées) — Moussa a 5 exercices terminés avec un score moyen élevé pour
  débloquer `APPRENTI` et `MAITRE_REFORMULATION`
- Consentements parentaux (variés : certains avec statistiques nominatives autorisées,
  d'autres non — pour tester le filtrage différencié)
- 2 abonnements (un établissement, un parent)
- 1 signalement de contournement de démonstration (pour tester le dashboard Superadmin)

## Tests

```bash
mvn test
```

- **Tests unitaires** (Mockito, sans base de données) : logique de quota IA
  (`QuotaIaServiceTest`), vérification de propriété des exercices et orchestration du
  parcours guidé (`ExerciceServiceTest`), construction des prompts et parsing du score
  de reformulation (`SocratiqueServiceTest`), scoping par établissement
  (`GestionComptesEtablissementServiceTest`), activation des comptes selon le rôle
  (`AuthServiceTest`), génération/validation JWT (`JwtServiceTest`).
- **Test d'intégration** (`XaamXaamApplicationTests`) : démarrage du contexte Spring
  avec une vraie instance PostgreSQL éphémère (Testcontainers) — nécessite Docker
  disponible sur la machine qui exécute les tests.

En écrivant ces tests, deux bugs réels ont été corrigés dans le code livré :
- `SocratiqueService.extraireScore` retournait toujours 0 à cause d'une regex mal
  construite (`replaceAll` en chaîne qui effaçait toute la chaîne dès le premier
  caractère non numérique) — remplacé par un `Pattern`/`Matcher` correct.
- `JwtService`/`JwtAuthFilter` laissaient remonter une `ExpiredJwtException` non
  interceptée sur un token expiré, ce qui aurait provoqué une erreur 500 au lieu
  d'un rejet propre de la requête — désormais capturée et traitée comme un token
  invalide.

## Fonctionnalités V2 implémentées

Toutes les fonctionnalités suivantes, listées en V2 dans le cahier des charges, ont
été implémentées dans ce backend (le frontend doit encore les exposer) :

- **Historique et progression par matière** : `GET /api/eleve/progression`
  (`TableauDeBordService.construireProgressionParMatiere`).
- **Bibliothèque d'exercices types** : `POST/GET /api/enseignant/bibliotheque`,
  `GET /api/eleve/bibliotheque`, et `POST /api/eleve/exercices/depuis-bibliotheque/{id}`
  pour démarrer un exercice à partir d'un modèle.
- **Badges / gamification** : `BadgeService` évalue automatiquement les seuils après
  chaque reformulation soumise ; `GET /api/eleve/badges` liste les badges obtenus.
  Catalogue initial (4 badges) inséré par la migration `V7__badges_gamification.sql`.
- **Détection de contournement** : `ContournementDetectionService` génère des
  signalements (copie d'énoncé, réponse anormalement longue, rapidité suspecte)
  **sans jamais bloquer l'élève** — consultables par le superadmin via
  `GET /api/admin/signalements-contournement`.
- **Statistiques nominatives vs anonymisées selon consentement** : la vue agrégée
  (`GET /api/enseignant/statistiques`) reste toujours anonyme. La vue nominative
  (`GET /api/enseignant/statistiques/nominatives`) n'inclut QUE les élèves dont le
  parent a explicitement consenti via `PUT /api/parent/enfants/{id}/consentement`.
- **Rapports hebdomadaires parent** : `RapportHebdomadaireService` tourne chaque
  lundi 8h (`@Scheduled`) et envoie un résumé par email à chaque parent ayant au
  moins un enfant. Nécessite un SMTP configuré (voir section Email ci-dessous).

## Conformité et sécurité — implémenté

- **Consentement parental** : entité `ConsentementParental`, endpoints
  `GET/PUT /api/parent/enfants/{id}/consentement`. Deux consentements distincts
  (traitement des données, statistiques nominatives), chacun horodaté séparément.
- **Droit d'accès / export des données** : `GET /api/eleve/mes-donnees` et
  `GET /api/parent/enfants/{id}/export-donnees`.
- **Droit à l'oubli** : `DELETE /api/eleve/mon-compte` — suppression irréversible,
  Hibernate gère la suppression en cascade sur toute la hiérarchie JOINED
  (`Utilisateur` → `Eleve`) ainsi que sur toutes les données liées (exercices,
  tentatives, badges, consentement...) grâce aux contraintes `ON DELETE CASCADE`.
- **Réinitialisation de mot de passe** : `POST /api/auth/mot-de-passe-oublie` puis
  `POST /api/auth/reinitialiser-mot-de-passe`. Ne révèle jamais si un email existe
  en base (réponse `204` identique dans tous les cas).
- **Vérification d'email** : envoyée automatiquement à l'inscription
  (`POST /api/auth/verifier-email`). **Ne bloque pas le login** dans ce MVP — le
  flag `emailVerifie` est juste exposé dans `AuthResponse` pour que le frontend
  affiche un bandeau ; voir `EmailVerificationService` pour resserrer ce choix plus tard.
- **Révocation de token / logout** : `POST /api/auth/logout` ajoute le `jti` du
  token courant à une liste noire (`TokenRevoque`), vérifiée par `JwtAuthFilter`
  à chaque requête. Purge automatique quotidienne des entrées expirées.
- **Protection brute-force** : `LoginAttemptService` bloque temporairement les
  connexions après `MAX_TENTATIVES_CONNEXION` échecs (5 par défaut) sur une fenêtre
  de `FENETRE_BLOCAGE_MINUTES` minutes (15 par défaut). Basé sur une table SQL —
  suffisant au volume de lancement ; envisager Redis si le trafic grossit beaucoup.
- **CORS restreint** : `SecurityConfig` ne whitelist plus `*` mais uniquement les
  origines listées dans `CORS_ALLOWED_ORIGINS` (défaut `http://localhost:3000`).
  **Pense à mettre le vrai domaine du frontend en production.**
- **Actuator** : dépendance ajoutée, `GET /actuator/health` exposé publiquement
  (utile pour le health check Railway/Render), aucun autre endpoint actuator exposé.

## Email (SMTP)

`EmailService` utilise `spring-boot-starter-mail`. Configure ces variables
d'environnement avant de lancer l'app pour que les emails partent réellement
(sinon, `EmailService` logue l'erreur et continue sans bloquer l'action métier) :

```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=ton-compte@gmail.com
SMTP_PASSWORD=mot-de-passe-application
MAIL_EXPEDITEUR=no-reply@xaamxaam.sn
FRONTEND_URL=https://app.xaamxaam.sn
```

## Déploiement (Render)

Le projet inclut un `Dockerfile` multi-stage (build Maven puis image JRE Alpine
minimale, utilisateur non-root, health check intégré) et un `render.yaml` prêts à
l'emploi pour Render, comme prévu en section 6 du cahier des charges.

### Build et test en local

```bash
docker build -t xaamxaam-backend .
docker run -p 8080:8080 --env-file .env xaamxaam-backend
```

### Déploiement sur Render

1. Pousse le repo sur GitHub (Render se connecte à un repo Git, pas à un zip).
2. Sur [render.com](https://render.com) → **New > Blueprint** → sélectionne le repo.
3. Render lit `render.yaml` automatiquement et crée **deux ressources d'un coup** :
   - le service web `xaamxaam-backend` (build via `Dockerfile`)
   - la base PostgreSQL managée `xaamxaam-db`
4. **Aucune manipulation d'identifiants de base de données à faire** : `render.yaml`
   injecte automatiquement `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
   `DB_PASSWORD` depuis `xaamxaam-db` vers le service web (bloc `fromDatabase`), et
   `application.yml` compose l'URL JDBC à partir de ces morceaux.
5. Dans le dashboard Render, renseigne les variables restantes (marquées
   `sync: false` dans `render.yaml`, donc pas dans le repo) :
   - `JWT_SECRET` — génère une vraie valeur forte : `openssl rand -base64 32`
   - `ANTHROPIC_API_KEY`
   - `OCR_API_KEY` (ton compte OCR.space, pas la clé de démo `helloworld`)
   - `CORS_ALLOWED_ORIGINS` — le vrai domaine du frontend une fois déployé (Vercel)
   - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_EXPEDITEUR`
   - `FRONTEND_URL` — utilisé dans les liens des emails (vérification, reset password)
6. `SPRING_PROFILES_ACTIVE=prod` est déjà défini dans `render.yaml`, ce qui active
   `application-prod.yml` (logs moins verbeux, `ddl-auto: validate` strict).
7. Render redéploie automatiquement à chaque push sur la branche connectée.

Le plan `free` est utilisé par défaut dans `render.yaml` pour démarrer sans coût
(cohérent avec la phase de lancement gratuite, section 7 du cahier des charges) —
passe à un plan payant avant la démo d'octobre si tu veux éviter la mise en veille
du service gratuit après inactivité (voir point "cold start" ci-dessous).

### Points d'attention pour la mise en production

- **`JWT_SECRET`** : ne jamais garder la valeur par défaut du `.env.example`.
- **`CORS_ALLOWED_ORIGINS`** : jamais `*` ni `localhost` en production.
- **`ddl-auto: validate`** (profil `prod`) : le schéma est piloté uniquement par
  Flyway — si une migration manque, l'application refuse de démarrer plutôt que de
  modifier le schéma automatiquement. C'est voulu.
- **Cold start / mise en veille** : le plan gratuit Render met le service en veille
  après une période d'inactivité, avec un redémarrage à froid de quelques dizaines
  de secondes au prochain appel — problématique en plein milieu d'une démo live.
  Prévoir soit un plan payant avant octobre, soit un ping périodique (cron externe)
  pour garder le service éveillé pendant la présentation.
- **Health check** : `/actuator/health` est volontairement le seul endpoint Actuator
  exposé publiquement (voir `SecurityConfig` et `application.yml` :
  `management.endpoints.web.exposure.include: health`) — aucune donnée sensible
  n'y transite.
- **Région** : `render.yaml` utilise `frankfurt` par défaut (latence raisonnable
  depuis le Sénégal parmi les régions disponibles sur Render) — ajustable si besoin.

## Structure du projet

```
src/main/java/com/xaamxaam/
├── config/           SecurityConfig, OpenApiConfig (Swagger), ClaudeApiProperties
├── domain/           Entités JPA (user, etablissement, exercice, abonnement)
├── repository/       Interfaces Spring Data JPA
├── service/          Logique métier (auth, exercice, suivi, admin...)
├── controller/       Endpoints REST, un contrôleur par rôle
├── dto/               Objets d'échange request/response
├── mapper/           Mapping entité <-> DTO (MapStruct)
├── security/         JWT (génération, filtre, UserDetailsService)
├── client/           Client HTTP vers l'API Anthropic Claude
└── exception/        Gestion globale des exceptions
```

## Notes sur les choix techniques

Le cahier des charges initial recommandait Node.js/FastAPI ; ce backend est en
Spring Boot, choix justifié par la compétence de l'équipe. Voir la discussion
complète sur ce point dans les échanges de conception du projet.
