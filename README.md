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
├── pages/LoginPage.java     # Page Object (interactions Serenity natives)
├── steps/LoginSteps.java    # Steps @Step (orchestration ; le secret transite en Secret du socle)
└── AuthenticationTest.java  # Tests JUnit 5 + SerenityJUnit5Extension
```

> Limite actuelle : `WebSync`/`MobileSync`, `DataFileManager`, `SecretManager`, `ReportingManager`
> du socle restent des squelettes (no-op, « corps réels = étape 8 »). Dès leur implémentation, les
> interactions de `LoginPage` pourront passer par `WebSync` (locators `By`, synchro maison) plutôt
> que par l'API Serenity native.

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
