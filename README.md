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
`~/.m2` une fois. Ce projet consomme la branche **`feature/package-by-feature`** du socle (packages
par fonctionnalité : `com.example.qa.sync`, `.secret`, `.page`, `.failure`… — la branche `main` est un
squelette) :

```bash
git clone https://github.com/ArnaudS-A5/qa-template
cd qa-template
git checkout feature/package-by-feature
./mvnw clean install         # publie qa-parent + qa-socle dans ~/.m2 (Maven Wrapper obligatoire côté socle)
```

Le build se fait en **Java 17** (cf. « Lancer les tests », §1).

En CI, ces artefacts viennent d'Artifactory (cf. `distributionManagement` du parent).

## Lancer les tests

### 1. Java 17

Le build et les tests exigent **Java 17**. `mvn` utilise le JDK désigné par `JAVA_HOME` : il se règle une fois
pour toutes dans l'IDE (ou le profil du terminal), pas dans les commandes. Vérification : `mvn -v` doit
afficher `Java version: 17…`. Avec un JDK plus récent (25 par exemple), le hook `assertThrows` de Serenity
4.2.22 ne s'installe pas et `DynamicWaitTest` sort « broken ».

Exemple VS Code : `terminal.integrated.env.<osx|linux|windows>` → `JAVA_HOME` pour les terminaux intégrés,
et `java.configuration.runtimes` pour l'IDE (compilation, Test Explorer).

### 2. Poste local : un navigateur

```bash
mvn clean verify                               # Chrome  → target/site/serenity/index.html
mvn clean verify -Dwebdriver.driver=firefox    # Firefox → même dossier : écrase la campagne précédente
```

### 3. Poste local : Chrome puis Firefox, en une seule commande

Les deux campagnes s'enchaînent seules (Firefox démarre à la fin de Chrome), et chacune garde ses résultats
dans son propre dossier, sans écrasement. Shell POSIX (macOS, Linux, Git Bash sous Windows) :

```bash
mkdir -p logs
for b in chrome firefox; do mvn clean verify -Dwebdriver.driver=$b > logs/campagne-$b.log 2>&1; rm -rf target-$b 2>/dev/null || mv target-$b target-$b.$RANDOM; mv target target-$b; done
```

Si l'ancien `target-<navigateur>/` ne se laisse pas supprimer (dossier ouvert dans le Finder, qui y recrée ses
`.DS_Store`), il est renommé `target-<navigateur>.<nombre>` au lieu de bloquer : ce reste est ignoré par Git et
peut être supprimé à la main.

| Résultat | Chrome | Firefox |
|---|---|---|
| Rapport Serenity | `target-chrome/site/serenity/index.html` | `target-firefox/site/serenity/index.html` |
| Rapports Surefire | `target-chrome/surefire-reports/` | `target-firefox/surefire-reports/` |
| Artefacts des tests KO | `target-chrome/qa-results/` | `target-firefox/qa-results/` |
| Sortie console | `logs/campagne-chrome.log` | `logs/campagne-firefox.log` |

Suivi en direct dans un 2ᵉ terminal : `tail -n +1 -F logs/campagne-chrome.log` (puis `campagne-firefox.log`).

> ⚠️ **Jamais `&` entre deux campagnes** (`mvn … & mvn …`) : `&` les lance **en même temps** dans le même
> `target/`, et la dernière à finir écrase les rapports de l'autre. Deux terminaux : oui, mais l'un **après**
> l'autre.

### 4. Une seule classe ou méthode

```bash
# -Dtest désactive le profil campagne : aucune clé n'est posée, à passer à la main si le test en dépend.
mvn test -Dtest=ImplicitWaitDefaultTest
mvn test -Dtest='ProfileMediumConfiguredTest#L4_medium_high' -Dqa.sync.timeout.medium=25s
```

### 5. Intégration continue (Jenkins)

En CI, la durée importe peu : les deux navigateurs passent l'un après l'autre, chacun dans son étape, et ses
résultats sont publiés **avant** que l'étape suivante ne fasse son `clean`. Exemple de `Jenkinsfile` (noms
d'outils à adapter à la configuration Jenkins ; plugins JUnit et HTML Publisher requis) :

```groovy
pipeline {
    agent any
    tools {
        jdk 'jdk-17'          // Java 17 obligatoire
        maven 'maven-3.9'
    }
    stages {
        stage('Campagne Chrome')  { steps { script { campagne('chrome') } } }
        stage('Campagne Firefox') { steps { script { campagne('firefox') } } }
    }
}

def campagne(String navigateur) {
    try {
        // -B : logs sans couleurs ; headless : agent sans écran ; suffixe : Chrome et Firefox distincts dans JUnit.
        sh "mvn -B clean verify -Dwebdriver.driver=${navigateur} -Dheadless.mode=true -Dsurefire.reportNameSuffix=${navigateur}"
    } finally {
        junit testResults: 'target/surefire-reports/*.xml'                 // tests KO → build UNSTABLE
        archiveArtifacts artifacts: 'target/qa-results/**', allowEmptyArchive: true
        publishHTML(target: [reportName: "Serenity ${navigateur}", reportDir: 'target/site/serenity',
                             reportFiles: 'index.html', keepAll: true, alwaysLinkToLastBuild: true, allowMissing: true])
    }
}
```

- `maven.test.failure.ignore=true` (pom) : un test KO ne casse pas `mvn`, la campagne Firefox tourne quand même ;
  c'est l'étape `junit` qui passe le build en **UNSTABLE**. Une vraie erreur de build le met en **FAILURE**.
- Le socle vient d'Artifactory (cf. « Pré-requis »), et non du `~/.m2` local.
- ⚠️ Pipeline non encore exécuté sur un agent : à valider (mode headless, fenêtre 800x600, charge de l'agent
  pour les tests chronométrés).

### 6. Soft-assert : T17 et T82, lancés à la main

T17 et T82 (`src/test/java/org/automation/websync/assertion/AssertionSoftTest.java`) sont **rouges à dessein** :
`@Disabled`, hors campagne et hors CI. On les lance par l'exécution `soft` du pom, qui porte leurs clés
(`qa.assertions.soft=true`, SHORT à 1 s), et **pas** par `-Dtest` (qui désactive le profil `campagne`, donc ces
clés). Une commande par navigateur :

```bash
mvn clean test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
mvn clean test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dwebdriver.driver=firefox
```

- Résultats : rapport `target/site/serenity/index.html`, artefacts `target/qa-results/KO__QATEST__T17_…/` et
  `KO__QATEST__T82_…/`.
  `clean` vide `target/` : les campagnes du §3 (`target-chrome/`, `target-firefox/`) ne sont pas touchées.
- T17 : les deux échecs soft figurent dans `FAIL.log` (agrégés par le vrai vote de fin de JUnit).
- T82, ce qu'on contrôle (détail : Javadoc de `AssertionSoftTest`). T82 joue 16 `should*` (13 KO, 3 OK, le
  dernier OK), deux fois, sur la fixture `websync/assertion/soft.html` :
  1. **le bloc**, hors étape : vérifié **dans le code** (un seul agrégat, exactement 13 échecs, dans l'ordre) ;
     la console l'atteste par `[T82] RESULTAT bloc … => bloc confirme` ;
  2. **les étapes**, une par `should*` : dans le rapport, 13 étapes **rouges** et 3 **vertes** (n° 8, 14 et 16),
     aucune sautée ; dans `KO__…`, un `{step}.html` par étape en échec (13) ;
  3. **la fin** : le test est **FAILED**, avec les 13 échecs des étapes.
- `BUILD SUCCESS` est normal : `maven.test.failure.ignore=true` (pom) ; le test, lui, est **FAILED** à dessein.

> **Un échec ne stoppe pas la campagne** : `maven.test.failure.ignore=true` est une propriété du pom
> (toutes exécutions) → 100 % des résultats à chaque passe. Pour un build rouge au 1ᵉʳ échec (CI) :
> `-Dmaven.test.failure.ignore=false`.

> **Garde-fou = annotation `@Disabled`** : les tests qui échouent à dessein (démo capture KO, spike DOM
> brut, producteurs d'artefacts, soft-assert T17 et T82) sont `@Disabled`. On les réactive à la demande avec
> `-Djunit.jupiter.conditions.deactivate='*DisabledCondition'` (T17 et T82 : §6).

## La campagne : cinq groupes de configuration

Les timeouts du socle se règlent par clés `qa.*`. Un même profil ne peut pas être à la fois « surchargé »
et « par défaut » dans une JVM : le profil Maven **`campagne`** (actif sauf si `-Dtest` est passé) découpe
donc la campagne en **cinq exécutions Surefire, sélectionnées par package** (par nom de classe pour `soft`),
chacune avec ses propriétés et ses JVM en parallèle (`forkCount`) :

| Exécution | Sélection | Propriétés posées par le pom |
|---|---|---|
| `defaults` | tout le reste (un nouveau test n'est jamais oublié) | **aucune** : SHORT 5 s, MEDIUM 15 s, LONG 60 s, EXTRA_LONG 120 s, pageLoad 2 min |
| `configured` | package `org.automation.config.configured` | `short=10s`, `medium=25s`, `long=30s`, `extraLong=40s`, `pageLoadTimeout=10s` |
| `precedence` | package `org.automation.config.precedence` | `environment=override-D` (T24) |
| `polling` | package `org.automation.config.polling` | `pollingInterval=2000ms` (T78 ; défaut socle 250 ms) |
| `soft` | classes `**/websync/assertion/AssertionSoft*Test` (les assertions dures du même package restent dans `defaults`) | `qa.assertions.soft=true` (partout ailleurs, `should*` durs), `short=1s` (un `should*` en échec attend le timeout du profil). Tests : T17 et T82, `@Disabled`, lancés à la main par cette exécution (§6) |

**Règle des tests aux limites (± 5 s).** Cas passant : la cible apparaît à `timeout − 5 s`, le clic
aboutit après l'avoir réellement attendue. Cas non passant : la cible apparaît à `timeout + 5 s`, la
`SyncException [ABSENT]` doit tomber **entre** le timeout et l'arrivée de la cible (chronométré : preuve
que la borne appliquée est la bonne). Marge réduite à **2 s** quand le timeout est trop court (SHORT par
défaut 5 s, implicit wait 3 s).

**Valeurs écrites à un seul endroit.** Les tests **lisent** la clé (ou le défaut du socle) via
`support/Timeouts` et en déduisent leurs délais. Chaque clé du groupe `configured` **diffère du défaut**, sinon
rien ne prouverait qu'elle est lue. Garde : un test `configured` lancé sans sa clé (ou un test `defaults` avec une
clé) échoue avec un message explicite.

**Navigateurs.** Fenêtre **800x600** identique pour Chrome et Firefox (`serenity.conf`) : même géométrie
de viewport. `PageLoadThrottledTest` (throttling CDP) n'existe que sur Chrome : sur Firefox, son
`assumeTrue` l'interrompt volontairement → **« Aborted »** chez Serenity, « Skipped » chez Surefire.

### Artefacts KO (tests `@Disabled`, hors campagne)

Les producteurs d'échec **volontaire** génèrent les dossiers `target/qa-results/KO__…`, ensuite relus
par les vérificateurs. **Nettoyer `target/qa-results` d'abord** (des artefacts périmés fausseraient les
vérificateurs). *À venir : faire des producteurs des **prérequis** de leurs vérificateurs.*

```bash
rm -rf target/qa-results   # indispensable avant toute campagne producteur/vérificateur
DIS="-Djunit.jupiter.conditions.deactivate=*DisabledCondition"

# 1) RawDomProducer : 2 échecs volontaires (LOGIN + CHECKBOXES, en parallèle) → artefacts + dump DOM.
mvn test -Dtest='RawDomProducerTest' "$DIS"
# 2) Valide : DOM réel <html>, masquage password, un DOM par test (no-leak ThreadLocal). Doit être VERT.
mvn test -Dtest='RawDomVerifierTest' "$DIS"

# 3) Artefacts : échec volontaire HORS @Step → ERROR.log + FAIL.log (pas de {step}.html).
#    -Dqa.sync.timeout.long=3s : simple accélération (l'élément est absent, inutile d'attendre 60 s).
mvn test -Dtest='ArtifactProducerTest' -Dqa.sync.timeout.long=3s "$DIS"
# 4) ArtifactVerifier : ROUGE ATTENDU (échec du producteur hors @Step → pas de {step}.html).
mvn test -Dtest='ArtifactVerifierTest' "$DIS"

# 5) T17 : soft-assert agrégé (soft=true) → échec EN FIN, produit son artefact KO__ (ou par §6, avec T82).
mvn test -Dtest='AssertionSoftTest#T17_should_aggregated' -Dqa.assertions.soft=true "$DIS"
```

> **Résultats attendus** : 2) **vert** (4/4) ; 1), 3), 5) **rouges volontaires** (producteurs) ; 4)
> **rouge documenté** (`{step}.html` hors `@Step`). Les 5 dossiers `KO__` produits sont la preuve
> visible de la capture d'échec du socle.

### La clé `environment` (ce que teste T24)

`environment` est une propriété Serenity (valeur `qatest` dans `serenity.conf`). Elle sert de token
`{ENV}` dans le nommage des artefacts d'échec : `target/qa-results/KO__{ENV}__{Test}__{timestamp}/`.
T24 (exécution `precedence` du pom) la surcharge (`environment=override-D`) et **vérifie que la
propriété système l'emporte sur `serenity.conf`** — c'est un test de **précédence de configuration**,
rien de plus. Concrètement,
ça ne « remplace » qu'une **valeur** : les dossiers `KO__` seraient préfixés `override-D` au lieu de
`qatest`. L'URL de base n'est pas affectée (héritée du bloc `environments.default`).

### PHASE 2 : forcer un vrai timeout socle sur site réel

Depuis la migration des PageObjects vers `WebSync` (cf. « Architecture »), la PHASE 2 **passe par le
moteur de synchro du socle** (`qa.sync.*` la gouverne). Mais **abaisser les profils ne suffit pas** sur
un site rapide : chaque interaction trouve sa cible **déjà présente** (page chargée avant le 1ᵉʳ sondage
de `WebSync`), donc le budget de timeout n'est jamais consommé. Pour éprouver réellement les bornes
contre le site dédié, il faut une **cible qui met du temps à apparaître** :

- **`DynamicWaitTest`** (S3, groupe `defaults`) injecte un bouton `#delayed` après **10 s** (JS, page réelle),
  profil choisi dans le PageObject (`web(SyncTimeout)`) : `SHORT` (5 s) échoue → `SyncException`,
  `MEDIUM` (15 s) réussit — soit exactement la règle ± 5 s aux valeurs par défaut. **Cross-browser.**
- **`PageLoadThrottledTest`** (T83, groupe `configured`, `pageLoadTimeout=10s`) applique un vrai **throttle 2G** (CDP) :
  la navigation `/login` (~45 s) dépasse le plafond → `TimeoutException` à ~10 s (navigation bornée).
  **Chrome uniquement** (« Aborted » sur Firefox, cf. plus haut).

## Ce que démontre la suite

| Ensemble | Rôle |
|---|---|
| `config/` (L1 à L6, T6, T24, T77, T78, T83) | **Configuration et horloge** du moteur de synchro : chaque profil `SyncTimeout`, aux défauts **et** par clé, implicit wait, `pageLoadTimeout`, précédence de configuration, intervalle de sondage. Chronométré, règle ± 5 s. |
| `websync/` (T1, T2, T5, T7 à T17, T22, T25 à T82, T84) | **Surface complète de l'API `WebSync`**, par domaine : chaque méthode publique de synchronisation est appelée par au moins un test (100 %, 78/78), sur des fixtures locales, avec étapes `@Step` et captures. |
| `failure/` (T18 à T23) | `@Disabled` (échec volontaire) : la **capture d'échec** du socle, produite puis relue (ci-dessous). |
| `site/` (S3, S4, S5) | PHASE 2 sur site réel : connexion (le mot de passe est un `Secret`, **masqué** dans les rapports), capture KO (`@Disabled`), passage du profil par PageObject. |
| `demo/` | Démo `java.awt.Robot` (curseur OS réel), hors socle. |

### La consommation réelle du socle : la capture d'échec

`TestFailureManager` est un `TestExecutionListener` JUnit **auto-enregistré** via
`META-INF/services/...` (activation par simple présence du jar — D16). Sur **tout test KO**, il
produit, *sans une ligne de code dans ce projet* :

```
target/qa-results/KO__{ENV}__{NomDuTest}__{yyyy-MM-dd_HH-mm-ss}/
├── ERROR.log    # cause directe
├── FAIL.log     # stacktrace + historique des steps (le Secret y apparaît masqué)
└── {step}.html  # dump du DOM, un fichier par step en échec (aucun si l'échec survient hors @Step)
```

`{ENV}` provient de la propriété Serenity `environment`. Ce test est `@Disabled` (il échoue à
dessein). Pour rejouer la démo et produire les artefacts :

```bash
mvn test -Dtest='LoginTest#S4_login_failure_captured' \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
  -Dmaven.test.failure.ignore=true -Denvironment=recette
```

> Retirer le socle de `pom.xml` ⇒ plus de dossier `KO__`, plus de masquage : la consommation
> n'est **pas** transparente.

## Configuration

- **`src/test/resources/serenity.conf`** — driver Chrome (via Selenium Manager), fenêtre **800x600**
  pour Chrome **et** Firefox, URL de base par
  environnement, capture d'écran **après chaque step** (`serenity.take.screenshots = AFTER_EACH_STEP` : le
  rapport montre l'état de la page à chaque étape), sortie console Serenity lisible
  (`serenity.logging = VERBOSE`, `console.colors = true`), et clés du socle
  (`qa.failure.artefacts.*`, `qa.assertions.soft = false`, implicit wait 3 s).
- **`src/test/resources/logback-test.xml`** — sans ce fichier, `logback-classic` (fourni par le
  socle, qui n'embarque pas encore de config par défaut) retombe sur sa config intégrée
  (`root=DEBUG`, nom de package complet, sans couleur). Ce fichier reprend la main : console
  propre, niveaux relevés, le déroulé des steps étant rendu par la sortie native Serenity.

## Architecture (Page Object Model)

```
src/test/java/org/automation/
├── config/                       # configuration et horloge du moteur de synchro, rangées par GROUPE du pom
│   ├── defaults/                 #   Profile{Short,Medium}DefaultTest (L5), ProfileViewDefaultTest (T77),
│   │                             #   ImplicitWaitDefaultTest (L1, L2, L6) — aucune clé
│   ├── configured/               #   Profile{Short,Medium,Long,ExtraLong}ConfiguredTest (L3-L5),
│   │                             #   PageLoadConfiguredTest (T6/T6bis), PageLoadThrottledTest (T83, 2G réel,
│   │                             #   Chrome) — clés posées par le pom
│   ├── precedence/               #   PrecedenceConfigTest (T24)
│   └── polling/                  #   PollingIntervalTest (T78) — qa.sync.pollingInterval posé par le pom
├── websync/                      # surface de l'API WebSync, un sous-package par domaine
│   ├── action/                   #   ActionKeyboardTest (T25-T26), ActionJavaScriptTest (T27-T30),
│   │                             #   ActionPointerTest (T31-T32), ActionWindowTest (T75-T76, mère/fille),
│   │                             #   ActionFormTest (T84, T22)
│   ├── within/                   #   WithinNestedTest (T11 sur 2 niveaux, T79-T81 sur 5 niveaux)
│   ├── select/                   #   SelectListTest (T33-T36)
│   ├── read/                     #   ReadDomTest (T37-T41), ReadCollectionTest (T42-T44)
│   ├── state/                    #   StatePredicateTest (T14, T45-T47)
│   ├── assertion/                #   AssertionHardTest (T16, T48-T58), AssertionSoftTest (T17, T82 :
│   │                             #   @Disabled, clés posées par l'exécution « soft » du pom, cf. §6)
│   ├── wait/                     #   WaitStateTest (T15, T59-T66), WaitDurationTest (T67-T70),
│   │                             #   WaitFluentTest (T1, T5)
│   └── diagnostic/               #   DiagnosticCodeTest (T2, T7-T10bis), DiagnosticCauseTest (T71-T74)
├── failure/                      # capture d'échec (@Disabled) : Artifact{Producer,Verifier}Test (T18-T23),
│                                 #   RawDom{Producer,Verifier}Test (T19, site réel, en parallèle)
├── site/                         # PHASE 2, site réel : LoginTest (S5, S4 @Disabled), DynamicWaitTest (S3)
├── demo/                         # RobotMouseDemoTest : curseur OS réel (java.awt.Robot), bloc commenté, hors socle
├── support/                      # outillage partagé, sans test : Fixtures (file://), Timeouts (clés/défauts +
│                                 #   garde), ProfileBounds (cible à timeout ± marge), Transitions (scénarios)
├── pages/                        # PageObjects — étendent QaPageObject du socle (champ WebSync `web` hérité)
│   ├── LoginPage.java            #   login réel ; type(By, Secret), click, waitUntilVisible
│   ├── CheckboxesPage.java       #   2e page (spike DOM), marqueur #checkboxes
│   └── DelayedButtonPage.java    #   injecte #delayed après un délai ; web(SyncTimeout) par profil
└── steps/                        # Steps @Step (orchestration ; écrites « attendu passant »)
    ├── LoginSteps.java           #   le Secret transite jusqu'à WebSync.type (aucun clair)
    ├── CheckboxesSteps.java
    ├── DelayedButtonSteps.java
    └── websync/                  #   étapes de la surface WebSync (Action, Read, State, Select, Within ; base
                                  #   WebSyncSteps), chacune au nom de la méthode du socle qu'elle appelle
src/test/resources/fixtures/      # HTML locaux (file://), rangés comme les tests : une fixture vit dans le
├── appear, form, offscreen,      #   dossier le plus profond que partagent tous ses utilisateurs ;
│   states, transitions .html     #   celles-ci servent à plusieurs domaines
├── config/configured/            #   slowload.html
└── websync/                      #   action/ (keyboard, javascript, pointer, window-parent, window-child),
                                  #   within/ (nested, nested-deep), select/, read/ (attributes, collection),
                                  #   assertion/ (soft), wait/ (disappear, rerender),
                                  #   diagnostic/ (hidden, disabled, overlay)
```

> **Convention de nommage** : le code en anglais (packages, classes, méthodes, constantes, variables,
> paramètres), en français ce qui se lit (commentaires, messages, titres d'étape, `@DisplayName` sans accents) ;
> méthode de test `<ID>_<sujet>[_<précision>]` (`T59_waitUntilPresent`, `L5_short_low`) ; `@DisplayName`
> `<ID> - <PASS | NON PASS | PASS + NON PASS> - <description>` (`PASS + NON PASS` : T11, T34, T48 à T58, qui
> jouent un appel qui réussit puis un appel qui doit échouer). Détail : CLAUDE.md, « Principes de test »,
> « Convention de nommage ».

> **Étapes `@Step` : où, et pourquoi pas partout.** Les actions et vérifications passantes des tests `websync/`
> passent par des étapes (`steps/websync/`) : le rapport Serenity les montre une à une, avec une capture après
> chacune (`take.screenshots = AFTER_EACH_STEP`), pour contrôler à l'œil ce que vérifie le test. Restent hors
> étape : l'appel dont on **attend l'échec** (une étape avalerait l'exception), tout appel **chronométré**
> (l'étape et sa capture fausseraient la mesure), donc `config/`, `websync/wait/`, `websync/diagnostic/` et
> `failure/` en entier. En **mode soft**, un `should*` en échec ne lève rien : dans une étape, il la rend
> **rouge** (sous-étape portant le message) sans arrêter le test ; T82 le joue hors étape **et** en étapes.
> Piège Serenity : dans un test qui finit rouge, un `assertThrows(AssertionError.class, …)` fait **repeindre en
> vert**, en fin de test, toutes les étapes en échec par `AssertionError` ; T82 capture donc son agrégat par
> `try/catch`. Détail : CLAUDE.md, « Étapes `@Step` ».

> **Convention de logs** (tous les tests) — pour un cas non passant :
> `[TAG] RESULTAT : exception attendue=<Type> | levee=OUI | type reel=<classe> | message="…" => cas non passant confirme` ;
> pour un cas passant : `[TAG] RESULTAT : <succès> en <ms> ms | aucune exception | <critère> => cas passant confirme`.
> Les Steps sont écrites comme du code **attendu passant** ; c'est le test (`assertThrows`) qui vérifie
> qu'un timeout trop court lève bien l'exception (seule exception : T82, cf. encadré ci-dessus). Chaque test
> porte **ses assertions dans sa méthode**,
> et le `RESULTAT` n'est écrit **qu'après** elles : il n'annonce jamais « confirmé » avant vérification.

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

- `RawDomProducerTest` — provoque 2 échecs sur 2 pages distinctes (LOGIN / CHECKBOXES) **en
  parallèle** (`@Execution(CONCURRENT)`) pour stresser le pont `ThreadLocal`.
- `RawDomVerifierTest` — relit les artefacts et vérifie : **(1)** DOM non vide,
  **(2)** DOM brut/rendable (vrai `<html>`, pas la vue Prism échappée du repli), **(3)** chaque
  artefact porte le DOM de SA page (aucune fuite entre threads). **Strict** : un artefact attendu
  manquant fait ÉCHOUER le test (jamais de skip silencieux).

```bash
# 1) Produire les artefacts (vrai navigateur, en parallèle)
rm -rf target/qa-results
mvn test -Dtest=RawDomProducerTest \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition' \
  -Dmaven.test.failure.ignore=true -Denvironment=recette
# 2) Valider les points 1, 2, 3
mvn test -Dtest=RawDomVerifierTest \
  -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
```

## Suggestions remontées au socle

Voir [`docs/qa-socle-suggestions.md`](docs/qa-socle-suggestions.md), dont l'en-tête donne l'état de chaque
suggestion. **Appliquées** par le socle et vérifiées ici :
1. §1 à §3 : dump HTML brut/rendable (option A, validé par le spike ci-dessus), nom de fichier = **step
   feuille** en échec, **translittération** des accents dans `sanitize` (`Échec`→`Echec`, `réussi`→`reussi`) ;
2. §5 : `count` / `getTexts` immédiats sur absent (T43 vert) ;
3. §6 : `setWindowFocus(By)` résout son locator (T76 vert) ;
4. §7 : un échec soft dans une étape la rend rouge, un `{step}.html` par étape en échec (T82).

Reste ouverte : §4, `[OUT_OF_BOUNDS]` remonté à tort sur un élément `display:none` (T7 reste vert : il ne
vérifie que `[HIDDEN]`). Notes transmises à l'agent du socle : `docs/note-agent-socle-T43-T76.md`,
`docs/note-agent-socle-soft-assert.md`.
