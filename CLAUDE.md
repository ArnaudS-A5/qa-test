# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`qa-test` est un **projet consommateur** : il ne contient **aucune source de production**, seulement des
tests qui **valident au navigateur réel** le socle `qa-socle` (dépôt
[`ArnaudS-A5/qa-template`](https://github.com/ArnaudS-A5/qa-template)), consommé comme dépendance Maven.
C'est aussi un **harnais de non-régression** pérenne : on **ne supprime aucun test existant**.

## Prérequis d'environnement

- **Java 17** obligatoire (cible du socle). `mvn` peut résoudre un JDK plus récent par défaut : forcer
  systématiquement l'exécution en 17.
  ```bash
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  ```
- Le socle doit être présent dans `~/.m2` (`com.example.qa:qa-parent` + `qa-socle` `1.0.0-SNAPSHOT`),
  publié depuis le dépôt du socle (`./mvnw clean install`). **Rien à installer depuis `qa-test`.**
- Pas de Maven Wrapper ici (contrairement au socle) : on utilise le `mvn` du poste.
- Navigateurs : **Chrome et Firefox** doivent passer. Le driver est provisionné par Selenium Manager
  (`webdriver.autodownload = true`), aucun binaire à installer.

## Commandes courantes

```bash
# Toute la suite en Chrome (défaut de serenity.conf)
mvn clean verify

# Toute la suite en Firefox
mvn clean verify -Dwebdriver.driver=firefox

# Une seule classe / une seule méthode
mvn test -Dtest=BornesLimitesTest
mvn test -Dtest='BornesLimitesTest#L4_profil_borne_haute'

# Collecter tous les résultats sans arrêter le build au 1er échec
mvn test -Dtest=DiagnosticTest -Dmaven.test.failure.ignore=true
```

### Piloter les mécanismes du socle par `-D`

Les tests aux limites règlent le comportement du socle via system properties (précédence `-D` >
`serenity.conf`, cf. T24). Clés utiles :

- `-Dqa.sync.timeout.<short|medium|long|extraLong>=2s` — abaisse un profil `SyncTimeout` (défauts
  5/15/60/120 s) pour rester rapide. Format accepté : `Nms`, `Ns`, ou `N` (secondes). ⚠️ La 4ᵉ clé est
  `extraLong` (camelCase), pas `extra_long` — vérifié dans le bytecode du socle.
- `-Dqa.sync.pageLoadTimeout=5s` — plafond de chargement de page (défaut socle 2 min ; T6, ThrottledLoadTest).
- `-Dqa.sync.pollingInterval=250ms` — intervalle de sondage du poll `WebSync` (défaut 250 ms).
- `-Dqa.assertions.soft=true` — bascule en soft-assert (T17).
- `-Denvironment=<valeur>` — pilote le `{ENV}` du nommage des artefacts `KO__` (défaut `qatest`).

> ⚠️ Certains tests exigent des valeurs de profil **contradictoires** (ex. L4 veut `medium=2s`,
> L5 « vraies valeurs » veut `medium=15s`). Ils doivent donc tourner dans des **invocations Maven
> séparées** — ne pas grouper toute la classe sous un seul jeu de `-D`.

### Tests `@Disabled` (producteurs d'échec volontaire)

Les tests qui échouent **à dessein** (producteurs d'artefacts, soft-assert) sont `@Disabled` pour rester
hors du build normal. Les réactiver explicitement :

```bash
mvn test -Dtest=ArtefactsProducerTest \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
  -Dmaven.test.failure.ignore=true
```

## Architecture

```
pom.xml                              # parent qa-parent ; UNE dépendance : qa-socle (sans version)
src/test/java/org/automation/
  phase1/                            # PHASE 1 — tests aux limites sur fixtures locales
    BornesLimitesTest                #   L1–L6, T1/T2/T5/T11/T14/T15 (bornes attente/horloge)
    DiagnosticTest                   #   T7–T10 + T10bis (diagnostic actif ; equivalence pointeur WebSync/Actions)
    PageLoadEtConfigTest             #   T6 (pageLoadTimeout), T24 (précédence -D)
    FormEtSecretTest                 #   surface formulaire, T22 (Secret), T16 (should* dur)
    SoftAssertTest                   #   T17 (soft-assert agrégé) — @Disabled
    Artefacts{Producer,Verification}Test  # T18–T23 via fixture — @Disabled
  pages/  steps/                     # PHASE 2 — PageObjects (étendent QaPageObject du socle : `web` hérité) + Steps
    AuthenticationTest               #   smoke login réel + capture KO (@Disabled)
    DynamicWaitTest                  #   passage du profil SyncTimeout (#delayed à 10s : SHORT KO / MEDIUM OK)
    ThrottledLoadTest                #   throttle 2G réel (Chrome only) → pageLoadTimeout borné
    RobotMouseDemoTest               #   démo Robot (curseur OS réel) — bloc commenté, hors socle
  RawDomCapture{Producer,Verification}Test # capture DOM brut + masquage sur @Step (site réel)
  support/Fixtures.java              # résolution des fixtures en URL file://
src/test/resources/
  fixtures/*.html                    # 10 fixtures JS inline (délais/états contrôlés)
  serenity.conf                      # config Serenity + clés qa.* du socle
  junit-platform.properties          # parallélisme JUnit (same_thread par défaut ; CONCURRENT ciblé)
  logback-test.xml
docs/                                # prompt de validation, suivi PHASE 1, suggestions socle
```

### Les trois phases (cf. `docs/qa-test-prompt.md`)

1. **PHASE 1 — fixtures locales** (`file://`, aucun serveur) : contrôle exact des délais/états pour valider
   les **bornes** des mécanismes d'attente. Chaque fixture lit ses paramètres (`?delay=N`, `?ms=N`…).
2. **PHASE 2 — sites réels** : navigation réaliste + surface d'interaction sur widgets réels (référence
   `the-internet.herokuapp.com`). **Réutiliser** les PageObjects existants (non-régression S5).
3. **PHASE 3 — parallélisme** : après tout le séquentiel, activer l'exécution parallèle et vérifier que
   chaque test KO produit **son** DOM (pas de fuite `ThreadLocal`).

### Principes de test

- Chaque PageObject détient un `WebSync` et agit **par `By`** (jamais de `WebElement` retourné : contrat
  no-leak du socle). Les noms de méthodes du socle **sont** ceux de Serenity/Selenium (`isVisible`, pas
  `isDisplayed`).
- Les artefacts `KO__{ENV}__{NomDuTest}__{timestamp}/` (dossier + `ERROR.log` + `FAIL.log` +
  `{step}.html`) ne sont produits que pour un test **KO**. Le dump `{step}.html` n'apparaît que si l'échec
  survient dans un **`@Step`** (le `RawDomCaptureListener` capture au `stepFailed`) : un échec appelé
  directement dans une méthode de test ne génère que les logs.
- **Le code du socle fait foi** : toute divergence doc/comportement est un **écart à signaler** (backlog
  socle), pas à corriger côté test.

## Git flow

Branches : `main` (stable) → `develop` (intégration) → `feature/*` / `fix/*` (PR vers `develop`). Les PR
vers `main` sont des releases (squash au besoin).

Convention de commit (alignée sur `qa-template`) : `<type>(<scope>): <résumé impératif court>` en
français, types `feat|fix|test|refactor|chore|docs|ci`. Scopes usuels ici : `build`, `config`, `fixtures`,
`pages`, `sync`, `diagnostic`, `secret`, `assertions`, `failure`, `docs`, `readme`.

## Statut du projet

- **Socle corrigé → PHASE 1 tout-vert (Chrome, 2026-08-02).** Le moteur SYNC a été implémenté côté socle
  (`develop`) : les profils `SyncTimeout` / overrides `-Dqa.sync.timeout.*` sont désormais appliqués, et le
  diagnostic `[ABSENT]/[DISABLED]/[OUT_OF_BOUNDS]` produit. Avec les **bons `-D` par test** (cf. README
  « Lancer une campagne complète »), les anciens 8 KO passent (L4, L5-SHORT, T2, T8, T14 par le correctif
  socle ; **T6, T10, T16 par correctif côté test**). `docs/qa-test-suivi-phase1.md` détaille l'historique
  (campagne pré-correctif du 2026-07-15) et la mise à jour du 2026-08-02.
- **Corrections côté test appliquées** (cf. `docs/qa-test-briefing-erreurs-phase1.md`) : T6 (contact
  `WebSync` avant navigation → `pageLoadTimeout` armé), T10 **reclassé** (scroll-into-view ; ajout de
  **T10bis**, preuve d'équivalence `WebSync.click` ≡ `Actions`), T16 (attend `AssertionError`), et **A9**
  (secret : plus aucune sortie du clair). Démo `RobotMouseDemoTest` (OS-level, commentée, hors socle).
- **PHASE 2 enrichie et basée sur `QaPageObject`.** `LoginPage`/`CheckboxesPage`/`DelayedButtonPage`
  étendent `QaPageObject` du socle (champ `web` hérité + `web(SyncTimeout)` par profil) — plus d'API
  Serenity native (`WebElementFacade`) ; le mot de passe transite en `Secret` jusqu'à
  `WebSync.type(By, Secret)`. Deux validations réelles ajoutées : **`DynamicWaitTest`** (passage du profil
  `SyncTimeout` : `#delayed` à 10 s, SHORT échoue / MEDIUM réussit, **cross-browser**) et
  **`ThrottledLoadTest`** (throttle **2G réel** via CDP, **Chrome only** — s'ignore sur Firefox → un test
  de moins).
- **Conventions (tout le projet).** (1) Steps/PageObjects écrits « attendu passant » : aucune Step ne
  renvoie/capture une exception pour signaler un échec, c'est le test (`assertThrows`) qui le vérifie.
  (2) Logs uniformes — cas non passant : `RESULTAT : exception attendue=… | levee=OUI | type reel=… |
  message="…" => cas non passant confirme` ; cas passant : `RESULTAT : … | aucune exception => cas passant
  confirme`.
- **Reste à faire** : re-passe **Firefox** de la PHASE 1 avec le socle corrigé (rafraîchir les timings du
  suivi), puis **PHASE 3** (parallélisme — un DOM par test KO, pas de fuite `ThreadLocal`).

## Documentation de référence

- `docs/qa-test-prompt.md` — mission de validation navigateur, tableaux des tests L*/T*/S* des trois phases
- `docs/qa-test-suivi-phase1.md` — suivi d'exécution PHASE 1 (statuts, timings, écarts socle, journal)
- `docs/qa-socle-suggestions.md` — retours/suggestions remontés vers le socle
- Contexte socle : `CLAUDE.md` et `qa-socle/docs/*` du dépôt `ArnaudS-A5/qa-template`
