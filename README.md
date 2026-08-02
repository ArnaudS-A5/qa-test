# qa-test — tests automatisés Selenium / Serenity, consommateur du socle `qa-socle`

Projet de tests **Selenium Java / Serenity BDD (sans Cucumber) / JUnit 5 / Maven**, en
**Page Object Model**. Il **consomme le socle QA** (`qa-socle`, du dépôt
[`ArnaudS-A5/qa-template`](https://github.com/ArnaudS-A5/qa-template)) : c'est lui qui fournit
les dépendances (Selenium, Serenity, JUnit 5, POI, log) **et** les comportements maison.

## Principe de consommation (décision D6-bis du socle)

- `qa-test` **hérite** du parent `com.example.qa:qa-parent` → toutes les versions (dépendances
  **et** plugins) sont pilotées par le socle. Ce projet n'épingle **aucune** version.
- Il déclare **une seule dépendance** : `qa-socle`. Elle **réexporte** (scope `compile`) Selenium,
  Serenity, JUnit 5, Apache POI et le log. Aucune de ces libs n'est redéclarée ici — elles sont
  **transitives**.

```
qa-parent (parent)  ──►  qa-socle (unique dépendance)
                           ├─ selenium-java
                           ├─ serenity-core / serenity-junit5
                           ├─ junit-jupiter
                           ├─ poi / poi-ooxml
                           └─ slf4j + logback
```

## Pré-requis : publier le socle en local

Le socle est en `-SNAPSHOT` et n'est pas sur un dépôt distant local → il faut le bâtir dans
`~/.m2` une fois. **Les comportements runtime implémentés vivent sur la branche `develop`** (la
branche `main` est un squelette) :

```bash
git clone https://github.com/ArnaudS-A5/qa-template
cd qa-template
git checkout develop
mvn install -DskipTests      # publie qa-parent + qa-socle dans ~/.m2
```

En CI, ces artefacts viennent d'Artifactory (cf. `distributionManagement` du parent).

## Lancer les tests

```bash
# Suite normale : seuls les tests "verts" tournent (les @Disabled sont ignorés).
mvn test

# TOUS les tests, y compris ceux @Disabled (qui échouent à dessein → failure.ignore pour finir).
mvn test -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true

# Rapport Serenity agrégé (target/site/serenity/index.html).
mvn verify
```

> **Garde-fou = annotation `@Disabled`** (pas de config dans le pom, qui reste minimal) : les tests
> qui échouent à dessein (démo capture KO, spike DOM brut) sont `@Disabled`. On les réactive à la
> demande avec `-Djunit.jupiter.conditions.deactivate='*DisabledCondition'`.

## Lancer une campagne complète (PHASE 1 + PHASE 2)

Certains tests exigent des valeurs de configuration **contradictoires** (ex. le profil `MEDIUM` :
`2s` pour L3/L4, mais `15s` réel pour `L5_medium_vraies_valeurs`). Ils **ne peuvent pas** tourner
sous un même jeu de `-D` → la campagne se fait en **4 invocations**.
`-Dmaven.test.failure.ignore=true` empêche l'arrêt au 1ᵉʳ échec : on **collecte tous** les résultats
malgré d'éventuels cas non passants.

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"

# Run 1 — PHASE 1 (fixtures locales) : profils du socle abaissés pour rester rapide.
#   short=1s (L5-SHORT, T2, T11, T16, DiagnosticTest) · medium=2s (L3, L4)
#   long=3s (L5-LONG, L6, T1, T5, T15) · pageLoadTimeout=5s (T6)
#   L1/L2 s'appuient sur l'implicit wait natif (3s, serenity.conf) — volontairement non surchargé.
#   On EXCLUT L5_medium_vraies_valeurs (elle veut medium=15s, cf. Run 2) via le "!".
mvn clean test \
  -Dqa.sync.timeout.short=1s -Dqa.sync.timeout.medium=2s -Dqa.sync.timeout.long=3s \
  -Dqa.sync.pageLoadTimeout=5s \
  -Dtest='BornesLimitesTest,DiagnosticTest,FormEtSecretTest,PageLoadEtConfigTest#T6_page_load_timeout,!BornesLimitesTest#L5_medium_vraies_valeurs' \
  -Dmaven.test.failure.ignore=true

# Run 2 — L5 MEDIUM aux VRAIES valeurs : medium=15s (défaut, aucun -D de timeout). L'élément
#   apparaît à 14s → le socle doit attendre ~14s puis réussir. (Pas de 'clean' : on accumule.)
mvn test -Dtest='BornesLimitesTest#L5_medium_vraies_valeurs' \
  -Dmaven.test.failure.ignore=true

# Run 3 — T24 : précédence -D > serenity.conf. On passe -Denvironment=override-D et le test
#   vérifie que la valeur résolue est bien 'override-D' (et non 'qatest' du serenity.conf).
#   Isolé pour que le reste de la campagne garde {ENV}=qatest dans le nommage des artefacts KO__.
mvn test -Dtest='PageLoadEtConfigTest#T24_precedence_D' -Denvironment=override-D \
  -Dmaven.test.failure.ignore=true

# Run 4 — PHASE 2 (site réel, cross-browser) : AuthenticationTest (smoke) + DynamicWaitTest.
#   DynamicWaitTest valide le PASSAGE du profil SyncTimeout : #delayed apparaît à 10s → SHORT (1s)
#   échoue (SyncException), MEDIUM (15s) réussit. short=1s rend SHORT trop court ; le login
#   (éléments déjà présents) n'en pâtit pas.
mvn test -Dtest='AuthenticationTest,DynamicWaitTest' -Dqa.sync.timeout.short=1s \
  -Dmaven.test.failure.ignore=true

# Run 5 — PHASE 2 throttling 2G RÉEL (Chrome uniquement ; s'ignore sur Firefox via assumeTrue).
#   Sous 2G, la navigation /login dépasse le pageLoadTimeout du socle → WebDriverException (bornée).
mvn test -Dtest='ThrottledLoadTest' -Dqa.sync.pageLoadTimeout=2s \
  -Dmaven.test.failure.ignore=true
```

Pour **Firefox** : ajouter `-Dwebdriver.driver=firefox` à **chacune** des 5 commandes. Le Run 5
(`ThrottledLoadTest`) donne alors **1 skip** (throttling CDP indisponible hors Chromium) → un test de
moins sur Firefox, attendu.

### Campagne complète : artefacts KO (tests `@Disabled`)

Les producteurs d'échec **volontaire** (`@Disabled` par défaut) génèrent les dossiers
`target/qa-results/KO__…`, ensuite relus par les vérificateurs. **Nettoyer `target/qa-results` d'abord**
(des artefacts périmés fausseraient les vérificateurs). Ces cas échouent **à dessein** : `failure.ignore`
sert justement à poursuivre la campagne.

```bash
rm -rf target/qa-results   # indispensable avant toute campagne producteur/vérificateur

# Run 6 — RawDomCapture : 2 échecs volontaires (LOGIN + CHECKBOXES, en parallèle) → artefacts + dump DOM.
mvn test -Dtest='RawDomCaptureProducerTest' \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true
# Run 7 — valide : DOM réel <html>, masquage password, un DOM par test (no-leak ThreadLocal). Doit être VERT.
mvn test -Dtest='RawDomCaptureVerificationTest' \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition'

# Run 8 — Artefacts : échec volontaire HORS @Step → ERROR.log + FAIL.log (pas de {step}.html).
#   long=3s évite d'attendre 60s (profil LONG par défaut) sur l'élément absent.
mvn test -Dtest='ArtefactsProducerTest' -Dqa.sync.timeout.long=3s \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true
# Run 9 — ArtefactsVerification : ROUGE ATTENDU. L'échec du producteur étant hors @Step, il n'y a pas de
#   {step}.html → le vérificateur échoue. T18-T20 sont validés EN VERT par RawDomCapture (Run 6/7).
mvn test -Dtest='ArtefactsVerificationTest' \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true

# Run 10 — SoftAssert (T17) : soft-assert agrégé (soft=true) → échec EN FIN, produit son artefact KO__.
mvn test -Dtest='SoftAssertTest' -Dqa.assertions.soft=true \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dmaven.test.failure.ignore=true
```

> **Résultats attendus** de cette sous-campagne : Run 7 **vert** (4/4) ; Runs 6/8/10 **rouges volontaires**
> (producteurs) ; Run 9 **rouge documenté** (écart `{step}.html` hors `@Step`). Les 5 dossiers `KO__`
> produits sont la preuve visible de la capture d'échec du socle.

### La clé `environment` (ce que teste T24)

`environment` est une propriété Serenity (valeur `qatest` dans `serenity.conf`). Elle sert de token
`{ENV}` dans le nommage des artefacts d'échec : `target/qa-results/KO__{ENV}__{Test}__{timestamp}/`.
T24 la surcharge en ligne de commande (`-Denvironment=override-D`) et **vérifie que le `-D` l'emporte
sur `serenity.conf`** — c'est un test de **précédence de configuration**, rien de plus. Concrètement,
ça ne « remplace » qu'une **valeur** : les dossiers `KO__` seraient préfixés `override-D` au lieu de
`qatest`. L'URL de base n'est pas affectée (héritée du bloc `environments.default`).

### PHASE 2 : forcer un vrai timeout socle sur site réel

Depuis la migration des PageObjects vers `WebSync` (cf. « Architecture »), la PHASE 2 **passe par le
moteur de synchro du socle** (`qa.sync.*` la gouverne). Mais **abaisser les profils ne suffit pas** sur
un site rapide : chaque interaction trouve sa cible **déjà présente** (page chargée avant le 1ᵉʳ sondage
de `WebSync`), donc le budget de timeout n'est jamais consommé. Pour éprouver réellement les bornes
contre le site dédié, il faut une **cible qui met du temps à apparaître** :

- **`DynamicWaitTest`** (Run 4) injecte un bouton `#delayed` après **10 s** (JS, page réelle) :
  `SHORT` (1 s) échoue → `SyncException`, `MEDIUM` (15 s) réussit → valide le passage du profil
  `SyncTimeout`. **Cross-browser.**
- **`ThrottledLoadTest`** (Run 5) applique un vrai **throttle 2G** (CDP) : la navigation `/login`
  dépasse le `pageLoadTimeout` → `WebDriverException` (navigation bornée). **Chrome uniquement**
  (throttling CDP indisponible sur Firefox → le test s'auto-ignore).

## Ce que démontre la suite

| Test | Rôle |
|---|---|
| `AuthenticationTest#valid_user_can_log_in` | **Vert.** Smoke de la chaîne Selenium/Serenity/JUnit 5 (toutes transitives via `qa-socle`). Le mot de passe est un `com.example.qa.api.secret.Secret` du socle → **masqué** dans les rapports. |
| `AuthenticationTest#invalid_login_is_captured_by_qa_socle` | **`@Disabled` par défaut** (il échoue volontairement). Démontre le **seul comportement runtime implémenté du socle** : `TestFailureManager`. |

### La consommation réelle du socle : la capture d'échec

`TestFailureManager` est un `TestExecutionListener` JUnit **auto-enregistré** via
`META-INF/services/...` (activation par simple présence du jar — D16). Sur **tout test KO**, il
produit, *sans une ligne de code dans ce projet* :

```
target/qa-results/KO__{ENV}__{NomDuTest}__{yyyy-MM-dd_HH-mm-ss}/
├── ERROR.log    # cause directe
├── FAIL.log     # stacktrace + historique des steps (le Secret y apparaît masqué)
└── {step}.html  # dump du DOM de la step en échec
```

`{ENV}` provient de la propriété Serenity `environment`. Ce test est `@Disabled` (il échoue à
dessein). Pour rejouer la démo et produire les artefacts :

```bash
mvn test -Dtest='AuthenticationTest#invalid_login_is_captured_by_qa_socle' \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
  -Dmaven.test.failure.ignore=true -Denvironment=recette
```

> Retirer le socle de `pom.xml` ⇒ plus de dossier `KO__`, plus de masquage : la consommation
> n'est **pas** transparente.

## Configuration

- **`src/test/resources/serenity.conf`** — driver Chrome (via Selenium Manager), URL de base par
  environnement, capture d'écran/HTML `FOR_FAILURES`, sortie console Serenity lisible
  (`serenity.logging = VERBOSE`, `console.colors = true`), et clés du socle
  (`qa.failure.artefacts.*`).
- **`src/test/resources/logback-test.xml`** — sans ce fichier, `logback-classic` (fourni par le
  socle, qui n'embarque pas encore de config par défaut) retombe sur sa config intégrée
  (`root=DEBUG`, nom de package complet, sans couleur). Ce fichier reprend la main : console
  propre, niveaux relevés, le déroulé des steps étant rendu par la sortie native Serenity.

## Architecture (Page Object Model)

```
src/test/java/org/automation/
├── pages/                        # PageObjects — étendent QaPageObject du socle (champ WebSync `web` hérité)
│   ├── LoginPage.java            #   login réel ; type(By, Secret), click, waitUntilVisible
│   ├── CheckboxesPage.java       #   2e page (spike DOM), marqueur #checkboxes
│   └── DelayedButtonPage.java    #   injecte #delayed après un délai ; web(SyncTimeout) par profil
├── steps/                        # Steps @Step (orchestration ; écrites « attendu passant »)
│   ├── LoginSteps.java           #   le Secret transite jusqu'à WebSync.type (aucun clair)
│   ├── CheckboxesSteps.java
│   └── DelayedButtonSteps.java
├── AuthenticationTest.java       # PHASE 2 smoke (login réel) + capture KO @Disabled
├── DynamicWaitTest.java          # PHASE 2 : passage du profil SyncTimeout (SHORT échoue / MEDIUM réussit)
├── ThrottledLoadTest.java        # PHASE 2 : throttle 2G réel (Chrome), pageLoadTimeout borné
├── RobotMouseDemoTest.java       # démo java.awt.Robot (curseur OS) — bloc commenté, hors socle
└── RawDomCapture{Producer,Verification}Test.java  # spike capture DOM brut (@Disabled)
```

> **Convention de logs** (tous les tests) — pour un cas non passant :
> `[TAG] RESULTAT : exception attendue=<Type> | levee=OUI | type reel=<classe> | message="…" => cas non passant confirme` ;
> pour un cas passant : `[TAG] RESULTAT : <succès> en <ms> ms | aucune exception | <critère> => cas passant confirme`.
> Les Steps sont écrites comme du code **attendu passant** ; c'est le test (`assertThrows`) qui vérifie
> qu'un timeout trop court lève bien l'exception.

> **Migration effectuée** : `WebSync` étant désormais implémenté côté socle, les PageObjects
> (`LoginPage`, `CheckboxesPage`) ont été **migrés de l'API Serenity native (`WebElementFacade`) vers
> `WebSync`** (locators `By`, synchro maison, diagnostic, contrat no-leak). Leurs interactions sont donc
> gouvernées par `qa.sync.*`. La navigation (`open()`) reste fournie par le `PageObject` Serenity
> (`@DefaultUrl` + `webdriver.base.url`). Le mot de passe transite en `Secret` **jusqu'à**
> `WebSync.type(By, Secret)` : plus aucune déballe du clair dans ce projet.

## Spike — validation de la capture DOM brut (option A du socle)

Le socle capte désormais le **DOM brut** (`driver.getPageSource()`) au `stepFailed` via un
`StepListener` + `ThreadLocal`, avec repli sur la source Serenity. Deux tests valident le mécanisme
de bout en bout (tous deux `@Disabled` → réactivés à la demande) :

- `RawDomCaptureProducerTest` — provoque 2 échecs sur 2 pages distinctes (LOGIN / CHECKBOXES) **en
  parallèle** (`@Execution(CONCURRENT)`) pour stresser le pont `ThreadLocal`.
- `RawDomCaptureVerificationTest` — relit les artefacts et vérifie : **(1)** DOM non vide,
  **(2)** DOM brut/rendable (vrai `<html>`, pas la vue Prism échappée du repli), **(3)** chaque
  artefact porte le DOM de SA page (aucune fuite entre threads). **Strict** : un artefact attendu
  manquant fait ÉCHOUER le test (jamais de skip silencieux).

```bash
# 1) Produire les artefacts (vrai navigateur, en parallèle)
rm -rf target/qa-results
mvn test -Dtest=RawDomCaptureProducerTest \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
  -Dmaven.test.failure.ignore=true -Denvironment=recette
# 2) Valider les points 1, 2, 3
mvn test -Dtest=RawDomCaptureVerificationTest \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
```

## Suggestions remontées au socle

Voir [`docs/qa-socle-suggestions.md`](docs/qa-socle-suggestions.md). **Appliquées sur `develop`** et
validées par le spike ci-dessus : dump HTML brut/rendable (option A), nom de fichier = **step
feuille** en échec, **translittération** des accents dans `sanitize` (`Échec`→`Echec`,
`réussi`→`reussi`).
