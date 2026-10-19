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
mvn test -Dtest=BornesLimitesTest
mvn test -Dtest='ProfilMediumCleTest#L4_profil_borne_haute' -Dqa.sync.timeout.medium=25s
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

### 6. Soft-assert : T82, lancé à la main

T82 (`src/test/java/org/automation/websync/etat/soft/SoftAssertionsTest.java`) est **rouge à dessein** :
`@Disabled`, hors campagne et hors CI. On le lance par l'exécution `soft` du pom, qui porte ses clés
(`qa.assertions.soft=true`, SHORT à 1 s), et **pas** par `-Dtest` (qui désactive le profil `campagne`, donc ces
clés). Une commande par navigateur :

```bash
mvn clean test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
mvn clean test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition' -Dwebdriver.driver=firefox
```

- Résultats : rapport `target/site/serenity/index.html`, artefacts `target/qa-results/KO__QATEST__T82_…/`.
  `clean` vide `target/` : les campagnes du §3 (`target-chrome/`, `target-firefox/`) ne sont pas touchées.
- Ce qu'on contrôle (détail : Javadoc de la classe T82). T82 joue 16 `should*` (13 KO, 3 OK, le dernier OK),
  deux fois, sur la fixture `websync/soft.html` :
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
> `-Djunit.jupiter.conditions.deactivate='*DisabledCondition'` (T82 : §6).

## La campagne : cinq groupes de configuration

Les timeouts du socle se règlent par clés `qa.*`. Un même profil ne peut pas être à la fois « surchargé »
et « par défaut » dans une JVM : le profil Maven **`campagne`** (actif sauf si `-Dtest` est passé) découpe
donc la campagne en **cinq exécutions Surefire, sélectionnées par package**, chacune avec ses propriétés
et ses JVM en parallèle (`forkCount`) :

| Exécution | Package | Propriétés posées par le pom |
|---|---|---|
| `defaut` | tout le reste (un nouveau test n'est jamais oublié) | **aucune** : SHORT 5 s, MEDIUM 15 s, LONG 60 s, EXTRA_LONG 120 s, pageLoad 2 min |
| `cle` | `org.automation.sync.cle` | `short=10s`, `medium=25s`, `long=30s`, `extraLong=40s`, `pageLoadTimeout=10s` |
| `precedence` | `org.automation.sync.precedence` | `environment=override-D` (T24) |
| `polling` | `org.automation.sync.polling` | `pollingInterval=2000ms` (T78 ; défaut socle 250 ms) |
| `soft` | `org.automation.websync.etat.soft` | `qa.assertions.soft=true` (partout ailleurs, `should*` durs), `short=1s` (un `should*` en échec attend le timeout du profil). Seul test : T82, `@Disabled`, lancé à la main par cette exécution (§6) |

**Règle des tests aux limites (± 5 s).** Cas passant : la cible apparaît à `timeout − 5 s`, le clic
aboutit après l'avoir réellement attendue. Cas non passant : la cible apparaît à `timeout + 5 s`, la
`SyncException [ABSENT]` doit tomber **entre** le timeout et l'arrivée de la cible (chronométré : preuve
que la borne appliquée est la bonne). Marge réduite à **2 s** quand le timeout est trop court (SHORT par
défaut 5 s, implicit wait 3 s).

**Valeurs écrites à un seul endroit.** Les tests **lisent** la clé (ou le défaut du socle) via
`support/Timeouts` et en déduisent leurs délais. Chaque clé du groupe `cle` **diffère du défaut**, sinon
rien ne prouverait qu'elle est lue. Garde : un test `cle` lancé sans sa clé (ou un test `defaut` avec une
clé) échoue avec un message explicite.

**Navigateurs.** Fenêtre **800x600** identique pour Chrome et Firefox (`serenity.conf`) : même géométrie
de viewport. `ThrottledLoadTest` (throttling CDP) n'existe que sur Chrome : sur Firefox, son
`assumeTrue` l'interrompt volontairement → **« Aborted »** chez Serenity, « Skipped » chez Surefire.

### Artefacts KO (tests `@Disabled`, hors campagne)

Les producteurs d'échec **volontaire** génèrent les dossiers `target/qa-results/KO__…`, ensuite relus
par les vérificateurs. **Nettoyer `target/qa-results` d'abord** (des artefacts périmés fausseraient les
vérificateurs). *À venir : faire des producteurs des **prérequis** de leurs vérificateurs.*

```bash
rm -rf target/qa-results   # indispensable avant toute campagne producteur/vérificateur
DIS="-Djunit.jupiter.conditions.deactivate=*DisabledCondition"

# 1) RawDomCapture : 2 échecs volontaires (LOGIN + CHECKBOXES, en parallèle) → artefacts + dump DOM.
mvn test -Dtest='RawDomCaptureProducerTest' "$DIS"
# 2) Valide : DOM réel <html>, masquage password, un DOM par test (no-leak ThreadLocal). Doit être VERT.
mvn test -Dtest='RawDomCaptureVerificationTest' "$DIS"

# 3) Artefacts : échec volontaire HORS @Step → ERROR.log + FAIL.log (pas de {step}.html).
#    -Dqa.sync.timeout.long=3s : simple accélération (l'élément est absent, inutile d'attendre 60 s).
mvn test -Dtest='ArtefactsProducerTest' -Dqa.sync.timeout.long=3s "$DIS"
# 4) ArtefactsVerification : ROUGE ATTENDU (échec du producteur hors @Step → pas de {step}.html).
mvn test -Dtest='ArtefactsVerificationTest' "$DIS"

# 5) SoftAssert (T17) : soft-assert agrégé (soft=true) → échec EN FIN, produit son artefact KO__.
mvn test -Dtest='SoftAssertTest' -Dqa.assertions.soft=true "$DIS"
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

- **`DynamicWaitTest`** (groupe `defaut`) injecte un bouton `#delayed` après **10 s** (JS, page réelle),
  profil choisi dans le PageObject (`web(SyncTimeout)`) : `SHORT` (5 s) échoue → `SyncException`,
  `MEDIUM` (15 s) réussit — soit exactement la règle ± 5 s aux valeurs par défaut. **Cross-browser.**
- **`ThrottledLoadTest`** (groupe `cle`, `pageLoadTimeout=10s`) applique un vrai **throttle 2G** (CDP) :
  la navigation `/login` (~45 s) dépasse le plafond → `TimeoutException` à ~10 s (navigation bornée).
  **Chrome uniquement** (« Aborted » sur Firefox, cf. plus haut).

## Ce que démontre la suite

| Ensemble | Rôle |
|---|---|
| `sync/` (L*, T6, T24, T77, T78) | **Bornes** des mécanismes d'attente du socle : chaque profil `SyncTimeout`, aux défauts **et** par clé, `pageLoadTimeout`, précédence de configuration, intervalle de sondage. Chronométré, règle ± 5 s. |
| `websync/` (T25 à T82) | **Surface complète de l'API `WebSync`** : chaque méthode publique de synchronisation est appelée par au moins un test (100 %, 78/78), sur des fixtures dédiées (`fixtures/websync/`), avec étapes `@Step` et captures. |
| `phase1/` (L1, L2, L6, T1 à T23) | Harnais historique de la PHASE 1 : bornes de l'implicit wait, diagnostic actif (`[ABSENT]`, `[HIDDEN]`…), formulaire et `Secret`, soft-assert T17 et artefacts `KO__` (`@Disabled`). |
| `AuthenticationTest`, `DynamicWaitTest`, `ThrottledLoadTest` | PHASE 2 sur site réel : smoke de la chaîne (le mot de passe est un `Secret`, **masqué** dans les rapports), passage du profil par PageObject, throttle 2G réel (Chrome). |
| `AuthenticationTest#invalid_login_is_captured_by_qa_socle`, `RawDomCapture*Test` | `@Disabled` (échec volontaire) : démonstration de la **capture d'échec** du socle (ci-dessous). |

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
mvn test -Dtest='AuthenticationTest#invalid_login_is_captured_by_qa_socle' \
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
├── sync/                         # tests aux limites des timeouts, rangés par GROUPE de configuration
│   ├── defaut/                   #   Profil{Short,Medium}DefautTest — défauts du socle (aucune clé),
│   │                             #   ProfilVuePageObjectDefautTest (T77 : web(SyncTimeout) sans effet de bord)
│   ├── cle/                      #   Profil{Short,Medium,Long,ExtraLong}CleTest, PageLoadTest (T6/T6bis),
│   │                             #   ThrottledLoadTest (2G réel, Chrome) — clés posées par le pom
│   ├── precedence/               #   PrecedenceConfigTest (T24)
│   └── polling/                  #   PollingIntervalTest (T78) — qa.sync.pollingInterval posé par le pom
├── websync/                      # surface de l'API WebSync, rangée comme le socle range ses méthodes
│   ├── action/                   #   FormulaireClavierTest (T25-T26), ActionsJavaScriptTest (T27-T30),
│   │                             #   PointeurTest (T31-T32), FenetreTest (T75-T76, fenêtres mère/fille)
│   ├── within/                   #   ImbricationWithinTest (T79-T81) — within sur 5 niveaux
│   ├── select/                   #   ListeDeroulanteTest (T33-T36)
│   ├── lecture/                  #   LecturesDomTest (T37-T41), CollectionsTest (T42-T44)
│   ├── etat/                     #   PredicatsTest (T45-T47), AssertionsDuresTest (T48-T58)
│   │   └── soft/                 #     SoftAssertionsTest (T82, @Disabled : 16 should* soft en bloc puis en
│   │                             #     étapes ; clés posées par l'exécution « soft » du pom, cf. §6)
│   ├── attente/                  #   AttentesEtatTest (T59-T66), AttentesDureeExpliciteTest (T67-T70)
│   └── exception/                #   ExceptionSyncTest (T71-T74)
├── phase1/                       # PHASE 1 (fixtures) : BornesLimitesTest, DiagnosticTest, FormEtSecretTest,
│                                 #   SoftAssertTest et Artefacts*Test (@Disabled), RobotMouseDemoTest (démo
│                                 #   java.awt.Robot, curseur OS — bloc commenté, hors socle)
├── support/                      # Fixtures (file://), Timeouts (lecture clés/défauts + garde), BornesProfil
├── pages/                        # PageObjects — étendent QaPageObject du socle (champ WebSync `web` hérité)
│   ├── LoginPage.java            #   login réel ; type(By, Secret), click, waitUntilVisible
│   ├── CheckboxesPage.java       #   2e page (spike DOM), marqueur #checkboxes
│   └── DelayedButtonPage.java    #   injecte #delayed après un délai ; web(SyncTimeout) par profil
├── steps/                        # Steps @Step (orchestration ; écrites « attendu passant »)
│   ├── LoginSteps.java           #   le Secret transite jusqu'à WebSync.type (aucun clair)
│   ├── CheckboxesSteps.java
│   ├── DelayedButtonSteps.java
│   └── websync/                  #   étapes de la surface WebSync (Action, Lecture, Etat, Select, Within)
├── AuthenticationTest.java       # PHASE 2 smoke (login réel) + capture KO @Disabled
├── DynamicWaitTest.java          # PHASE 2 : passage du profil SyncTimeout (SHORT échoue / MEDIUM réussit)
└── RawDomCapture{Producer,Verification}Test.java  # spike capture DOM brut (@Disabled)
```

> **Étapes `@Step` : où, et pourquoi pas partout.** Les actions et vérifications passantes des tests `websync/`
> passent par des étapes (`steps/websync/`) : le rapport Serenity les montre une à une, avec une capture après
> chacune (`take.screenshots = AFTER_EACH_STEP`), pour contrôler à l'œil ce que vérifie le test. Restent hors
> étape : l'appel dont on **attend l'échec** (une étape avalerait l'exception), tout appel **chronométré**
> (l'étape et sa capture fausseraient la mesure), donc `sync/`, `websync/attente/`, `websync/exception/` et
> `phase1/` en entier. En **mode soft**, un `should*` en échec ne lève rien : dans une étape, il la rend
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
