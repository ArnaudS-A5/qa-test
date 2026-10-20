# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`qa-test` est un **projet consommateur** : il ne contient **aucune source de production**, seulement des
tests qui **valident au navigateur réel** le socle `qa-socle` (dépôt
[`ArnaudS-A5/qa-template`](https://github.com/ArnaudS-A5/qa-template)), consommé comme dépendance Maven.
C'est aussi un **harnais de non-régression** pérenne : on **ne supprime aucun test existant**.

## Prérequis d'environnement

- **Java 17** obligatoire (cible du socle ; au-delà, le hook `assertThrows` de Serenity ne s'installe pas).
  Le JDK se règle **dans l'IDE**, pas dans les commandes (README « Lancer les tests » §1 ; ici
  `.vscode/settings.json` : `terminal.integrated.env.osx` et `java.configuration.runtimes`). Les commandes
  `mvn` s'écrivent **nues**, sans préfixe `JAVA_HOME`. Vérification : `mvn -v` affiche `Java version: 17…`.
- Le socle doit être présent dans `~/.m2` (`com.example.qa:qa-parent` + `qa-socle` `1.0.0-SNAPSHOT`),
  publié depuis le dépôt du socle (`./mvnw clean install`). **Rien à installer depuis `qa-test`.**
  Socle consommé : branche `feature/package-by-feature` (packages par fonctionnalité :
  `com.example.qa.sync` — `WebSync`, `SyncTimeout`, `SyncException` —, `.secret`, `.page`, `.failure`…).
- Pas de Maven Wrapper ici (contrairement au socle) : on utilise le `mvn` du poste.
- Navigateurs : **Chrome et Firefox** doivent passer. Le driver est provisionné par Selenium Manager
  (`webdriver.autodownload = true`), aucun binaire à installer.

## Commandes courantes

```bash
# Campagne complète : UNE commande par navigateur (profil `campagne` du pom, cf. ci-dessous)
mvn clean verify                                 # Chrome (défaut de serenity.conf)
mvn clean verify -Dwebdriver.driver=firefox      # Firefox

# Suivre une campagne en direct : sortie redirigée dans logs/ (ignoré par Git), suivie dans un terminal
mkdir -p logs && mvn clean verify > logs/campagne-chrome.log 2>&1
tail -n +1 -F logs/campagne-chrome.log           # dans un autre terminal (VS Code)

# Une seule classe / une seule méthode (-Dtest désactive le profil `campagne` : aucune clé posée)
mvn test -Dtest=ImplicitWaitDefaultTest
mvn test -Dtest='ProfileMediumConfiguredTest#L4_medium_high' -Dqa.sync.timeout.medium=25s

# Chrome PUIS Firefox en une commande, résultats conservés dans target-chrome/ et target-firefox/ :
# README « Lancer les tests » §3 (jamais & entre deux campagnes : même target/). CI Jenkins : §5.

# Build rouge au 1er échec (CI) : par défaut le pom IGNORE les échecs pour collecter 100 % des résultats
mvn clean verify -Dmaven.test.failure.ignore=false
```

### Groupes de configuration (profil `campagne` du pom)

Les timeouts du socle se règlent par clés `qa.*` (précédence `-D` > `serenity.conf`, cf. T24). Un même
profil ne pouvant pas être à la fois « surchargé » et « par défaut » dans une JVM, le profil `campagne`
(actif sauf si `-Dtest` est passé) découpe la campagne en **exécutions Surefire sélectionnées par
package** (par nom de classe pour `soft`), chacune avec ses propres propriétés et ses JVM en parallèle
(`forkCount`) :

| Exécution | Sélection | Propriétés posées |
|---|---|---|
| `defaults` | tout ce qui n'est pas dans les autres groupes (un nouveau test n'est jamais oublié) | **aucune** : défauts du socle (SHORT 5 s, MEDIUM 15 s, LONG 60 s, EXTRA_LONG 120 s, pageLoad 2 min) |
| `configured` | package `org.automation.config.configured` | `short=10s`, `medium=25s`, `long=30s`, `extraLong=40s`, `pageLoadTimeout=10s` |
| `precedence` | package `org.automation.config.precedence` | `environment=override-D` (T24) |
| `polling` | package `org.automation.config.polling` | `pollingInterval=2000ms` (T78 ; défaut socle 250 ms) |
| `soft` | classes `**/websync/assertion/AssertionSoft*Test` (le package `assertion` contient aussi les assertions dures, qui restent dans `defaults`) | `qa.assertions.soft=true` (partout ailleurs, `should*` durs), `short=1s` (un `should*` en échec attend le timeout du profil). Tests : T17 et T82, `@Disabled`, lancés à la main par cette exécution |

- **Valeurs écrites à un seul endroit (le pom)** : les tests aux limites les **lisent** via
  `support/Timeouts` (clé, ou `SyncTimeout.defaultValue()`) et en déduisent leurs délais. Garde : un test
  `configured` lancé sans sa clé, ou un test `defaults` lancé avec une clé, échoue avec un message explicite.
- **Chaque clé du groupe `configured` diffère du défaut**, sinon rien ne prouverait qu'elle est lue.
- **Règle aux limites ± 5 s** : cas passant = cible à `timeout − 5 s` ; cas non passant = cible à
  `timeout + 5 s`, exception attendue **entre** le timeout et l'arrivée de la cible (chronométrée). Marge
  réduite à **2 s** quand le timeout est trop court (SHORT par défaut 5 s, implicit wait 3 s).
- `maven.test.failure.ignore=true` (propriété du pom, toutes exécutions) : un échec ne stoppe pas la
  campagne. Surchargeable par `-Dmaven.test.failure.ignore=false`.
- Autres clés utiles en manuel : `-Dqa.sync.pollingInterval=250ms` (sondage `WebSync`),
  `-Dqa.assertions.soft=true` (T17), `-Denvironment=<valeur>` (`{ENV}` des artefacts `KO__`). Format des
  durées : `Nms`, `Ns` ou `N` (secondes). ⚠️ La 4ᵉ clé de profil est `extraLong` (camelCase).

### Tests `@Disabled` (producteurs d'échec volontaire)

Les tests qui échouent **à dessein** (producteurs d'artefacts, soft-assert) sont `@Disabled` : hors
campagne. Les réactiver explicitement (les échecs n'arrêtent pas le build, cf. ci-dessus) :

```bash
mvn test -Dtest=ArtifactProducerTest -Djunit.jupiter.conditions.deactivate='*DisabledCondition'
```

T17 et T82 (soft-assert, `AssertionSoftTest`) se lancent par l'exécution `soft` du pom, qui porte leurs clés
(`-Dtest` les perdrait) :
`mvn clean test-compile surefire:test@soft serenity:aggregate -Djunit.jupiter.conditions.deactivate='*DisabledCondition'`
(+ `-Dwebdriver.driver=firefox`). Détail : README « Lancer les tests » §6.

## Architecture

```
pom.xml                              # parent qa-parent ; UNE dépendance : qa-socle ; profil `campagne`
src/test/java/org/automation/
  config/                            # Configuration et horloge du moteur de synchro, rangées par GROUPE du pom
    defaults/                        #   exécution « defaults » (aucune clé)
      Profile{Short,Medium}DefaultTest  #   bornes ± marge aux défauts du socle (L5 : SHORT 5 s, MEDIUM 15 s)
      ProfileViewDefaultTest         #     T77 : web(SyncTimeout) rend une vue distincte, web reste MEDIUM
      ImplicitWaitDefaultTest        #     L1/L2 (implicit wait ± 2 s), L6 (sondage WebSync vs implicit wait)
    configured/                      #   exécution « configured » (clés posées par le pom)
      Profile{Short,Medium,Long,ExtraLong}ConfiguredTest  # bornes ± 5 s par clé (L3/L4, L5)
      PageLoadConfiguredTest         #     T6 / T6bis (pageLoadTimeout ± 5 s)
      PageLoadThrottledTest          #     T83 : throttle 2G réel (Chrome only ; « Aborted » sur Firefox : assumeTrue)
    precedence/
      PrecedenceConfigTest           #   T24 (précédence -D > serenity.conf ; préalables -D et conf vérifiés)
    polling/
      PollingIntervalTest            #   T78 (qa.sync.pollingInterval : clic au 3e sondage)
  websync/                           # Surface de l'API WebSync (groupe « defaults »), un sous-package par domaine ;
                                     # étapes @Step dans steps/websync/ (cf. « Étapes @Step »)
    action/                          #   ActionKeyboardTest (T25-T26), ActionJavaScriptTest (T27-T30),
                                     #   ActionPointerTest (T31-T32), ActionWindowTest (T75-T76, fenêtres mère/fille),
                                     #   ActionFormTest (T84 formulaire complet, T22 Secret)
    within/                          #   WithinNestedTest (T11 sur 2 niveaux ; T79-T81 sur 5 niveaux + leurre)
    select/                          #   SelectListTest (T33-T36)
    read/                            #   ReadDomTest (T37-T41), ReadCollectionTest (T42-T44)
    state/                           #   StatePredicateTest (T14, T45-T47)
    assertion/                       #   AssertionHardTest (T16, T48-T58) ; AssertionSoftTest (T17, T82 : groupe
                                     #   « soft », @Disabled, rouges à dessein)
    wait/                            #   WaitStateTest (T15, T59-T66 ± 5 s), WaitDurationTest (T67-T70),
                                     #   WaitFluentTest (T1 élément tardif, T5 stale)
    diagnostic/                      #   DiagnosticCodeTest (T2, T7-T10bis : codes), DiagnosticCauseTest (T71-T74 :
                                     #   cause, suppressed)
  failure/                           # Capture d'échec du socle (KO__), @Disabled : ArtifactProducerTest /
                                     # ArtifactVerifierTest (T18-T23), RawDomProducerTest / RawDomVerifierTest (T19)
  site/                              # PHASE 2, site réel : LoginTest (S5 connexion, S4 capture KO @Disabled),
                                     # DynamicWaitTest (S3 : profil SyncTimeout via PageObject, SHORT KO / MEDIUM OK)
  demo/                              # RobotMouseDemoTest : curseur OS réel (java.awt.Robot), bloc commenté, hors socle
  pages/                             # PageObjects (étendent QaPageObject du socle : `web` hérité)
  steps/                             # Steps des PageObjects (Login, Checkboxes, DelayedButton)
    websync/                         #   étapes de la surface WebSync : Action, Read, State, Select, Within
                                     #   (base WebSyncSteps : open une fixture, avec ou sans paramètres ;
                                     #   withProfile). ActionSteps.type existe aussi pour un Secret (titre masqué)
  support/                           # outillage partagé, sans test : Fixtures (URL file://), Timeouts (clés/défauts
                                     # + garde), ProfileBounds (cible à timeout ± marge), Transitions (scénarios)
src/test/resources/
  fixtures/                          # HTML locaux (file://), rangés comme les tests : une fixture vit dans le
                                     # dossier le plus profond que partagent tous ses utilisateurs
    appear, form, offscreen, states, transitions .html   # partagées entre plusieurs domaines
    config/configured/               #   slowload.html
    websync/action/                  #   keyboard, javascript, pointer, window-parent, window-child .html
    websync/within/                  #   nested.html (2 niveaux), nested-deep.html (5 niveaux + leurre)
    websync/select/  websync/read/   #   select.html ; attributes.html, collection.html
    websync/assertion/               #   soft.html (T82, éléments dans l'ordre des vérifications)
    websync/wait/  websync/diagnostic/  # disappear, rerender .html ; hidden, disabled, overlay .html
  serenity.conf                      # config Serenity (fenêtre 800x600 Chrome + Firefox) + clés qa.* du socle
  junit-platform.properties          # parallélisme JUnit (same_thread par défaut ; CONCURRENT ciblé)
  logback-test.xml
logs/                                # sorties de campagne suivies en direct (ignoré par Git)
docs/                                # prompt de validation, suivi d'exécution, suggestions et notes pour le socle
```

### Les trois phases (cf. `docs/qa-test-prompt.md`)

1. **PHASE 1 — fixtures locales** (`file://`, aucun serveur) : contrôle exact des délais/états pour valider
   les **bornes** des mécanismes d'attente. Chaque fixture lit ses paramètres (`?delay=N`, `?ms=N`…).
2. **PHASE 2 — sites réels** : navigation réaliste + surface d'interaction sur widgets réels (référence
   `the-internet.herokuapp.com`). **Réutiliser** les PageObjects existants (non-régression S5).
3. **PHASE 3 — parallélisme** : après tout le séquentiel, activer l'exécution parallèle et vérifier que
   chaque test KO produit **son** DOM (pas de fuite `ThreadLocal`).

### Principes de test

- **Identifiants** (`docs/qa-test-prompt.md`) : **L** = test aux **limites** (bornes ± 5 s), **T** = test de
  comportement, **S** = **site** réel (PHASE 2). **C** n'est pas un identifiant de test : c'est un **point
  critique** de la revue de code du socle (ex. C8 « une seule horloge »). Un nouveau test prend le **T
  suivant** le plus grand existant (T84 au 2026-10-20), préfixe repris dans le nom de méthode et le
  `@DisplayName`.
- **Convention de nommage** (décision du 2026-10-20) :
  1. **le code en anglais** : packages, classes, méthodes (tests, étapes, pages, support), constantes, variables
     et paramètres (étapes comprises), identifiants des exécutions du pom ; les valeurs qui viennent d'une
     fixture restent telles quelles (`HIDDEN = By.id("cache")`, paramètre `scenario=apparition`) ;
  2. **en français ce qui se lit** : commentaires et Javadoc, messages d'assertion et logs, titres d'étape `@Step`
     (l'application est en français), `@DisplayName` ;
  3. **ordre des mots** : le nom de la famille d'abord, la variante ensuite, pour que les membres se suivent
     (`AssertionHardTest` / `AssertionSoftTest`, `ProfileShortDefaultTest` / `ProfileShortConfiguredTest`,
     `PageLoadConfiguredTest` / `PageLoadThrottledTest`) ;
  4. **méthode de test** : `<ID>_<sujet>[_<précision>]`, le sujet étant la méthode du socle visée écrite comme
     dans l'API (`T59_waitUntilPresent`) ou quelques mots anglais courts (`T37_visible_text`) ; un test aux
     limites finit par `_low` (borne basse) ou `_high` (borne haute) ; jamais de « ok/ko » dans le nom ;
  5. **`@DisplayName`** : `<ID> - <PASS | NON PASS | PASS + NON PASS> - <description>`, description en français
     **sans accents** ; `PASS` / `NON PASS` se lit sur l'appel au socle (il réussit ou doit échouer), pas sur la
     couleur du test ; `PASS + NON PASS` pour les 13 tests qui jouent un appel qui réussit puis un appel qui doit
     échouer, non scindés (T11, T34, T48 à T58) ;
  6. **packages** : un par domaine ; deux variantes d'un même domaine se distinguent par le nom de la classe ;
  7. **étapes** : une étape porte le nom de la méthode du socle qu'elle appelle (`StateSteps.shouldBeVisible`) ;
     deux surcharges d'une étape avec le même nombre de paramètres portent le **même titre** `@Step` : Serenity
     retrouve le titre par le nom et le nombre de paramètres, et confondrait sinon les deux
     (`ActionSteps.type(By, String)` / `type(By, Secret)`, constaté le 2026-10-20) ;
  8. **fixtures** : noms anglais, rangées comme les tests (cf. « Architecture »).
- **Une classe = un domaine** (plusieurs `@Test` indépendants, un navigateur neuf chacun) ; **un `@Test` =
  un comportement**, éventuellement plusieurs actions, chacune suivie de son assertion. Pas de test
  fourre-tout : un échec arrête le reste de sa méthode.
- **Fixtures** : ne jamais modifier le contenu d'une fixture existante (régression des tests qui l'utilisent) ;
  un besoin nouveau = une fixture nouvelle et dédiée. Exceptions admises (2026-10-20) : un renommage qui casse
  un lien entre fixtures (`window-parent.html` ouvre `window-child.html`), et la tenue à jour des seuls
  **commentaires** HTML (tests, classes et packages cités), contrôlée par comparaison des fixtures commentaires
  retirés.
- **Étapes `@Step`** (décision du 2026-10-18) : comme chez un vrai consommateur, les actions et vérifications
  **passantes** des tests `websync/` passent par des étapes (`steps/websync/`, une étape = un appel au socle),
  rapportées avec leurs paramètres et une capture (`serenity.take.screenshots = AFTER_EACH_STEP`) : le rapport
  permet de contrôler à l'œil faux positifs et faux négatifs, et un échec dans une étape produit le dump
  `{step}.html` des artefacts `KO__`. Avant un cas non passant, une étape de **constat** montre le mauvais état.
  Restent des **appels directs**, hors étape :
  1. l'appel dont on **attend l'échec** (`assertThrows`) : une étape avale l'exception (Serenity ne la relance que
     par un hook ByteBuddy, inopérant hors Java 17, cf. `DynamicWaitTest`) et une étape n'attrape jamais
     d'exception (convention 1) ;
  2. tout appel **chronométré** (bornes ± 5 s / ± 2 s, « immédiat ») : l'instrumentation de l'étape et sa capture
     s'ajouteraient à la mesure ;
  3. d'où des classes **entièrement sans étape** : `config/` (bornes des profils, implicit wait, précédence,
     polling), `websync/wait/` (bornes ± 5 s, `fluentWait`), `websync/diagnostic/` (codes et structure de
     l'exception) et `failure/` (producteurs et vérificateurs d'artefacts). Les tests venus de l'ancien `phase1/`
     suivent la règle depuis le 2026-10-20 : T22 et T84 entièrement en étapes, T11 et T16 avec leur constat en
     étape, T14 avec sa seule ouverture en étape (ses prédicats sont chronométrés).

  **Mode soft** (`qa.assertions.soft=true`) : un `should*` en échec ne lève rien, il est collecté ; **dans une
  étape**, le socle le signale aussi à Serenity (sous-étape rouge portant le message, capture, `{step}.html`)
  sans faire sauter les étapes suivantes ; **hors étape**, il est seulement collecté. En fin de test,
  l'extension du socle lève l'agrégat de tous les échecs. T82 joue les deux formes (bloc hors étape, puis
  étapes).
  **Piège Serenity 4.2.22** : `assertThrows(X.class, …)` déclare `X` exception « attendue » du test ; si le test
  finit en échec sur `X`, Serenity **repeint en vert** toutes les étapes en échec par `X`
  (`SerenityTestExecutionListener` → `BaseStepListener.exceptionExpected` →
  `TestOutcome.resetFailingStepsCausedBy`). Dans un test qui finit rouge à dessein avec des étapes rouges,
  capturer l'exception par `try/catch` (T82). Sans effet sur un test qui finit vert (T48-T58).
  Les étapes tiennent directement un `WebSync` : sur la surface du socle, un PageObject ne ferait que relayer
  l'appel (le chaînage PageObject → étape est couvert par la PHASE 2 et T77).
- Chaque PageObject détient un `WebSync` et agit **par `By`** (jamais de `WebElement` retourné : contrat
  no-leak du socle). Les noms de méthodes du socle **sont** ceux de Serenity/Selenium (`isVisible`, pas
  `isDisplayed`).
- Les artefacts `KO__{ENV}__{NomDuTest}__{timestamp}/` (dossier + `ERROR.log` + `FAIL.log` +
  `{step}.html`) ne sont produits que pour un test **KO**. Un dump `{step}.html` est écrit **par step en
  échec** (le `RawDomCaptureListener` capture au `stepFailed`) : un échec appelé directement dans une méthode
  de test ne génère que les logs.
- **Le code du socle fait foi** : toute divergence doc/comportement est un **écart à signaler** (backlog
  socle), pas à corriger côté test.

## Git flow

Branches : `main` (stable) → `develop` (intégration) → `feature/*` / `fix/*` (PR vers `develop`). Les PR
vers `main` sont des releases (squash au besoin).

Convention de commit (uniforme, sans exception) : `<type>(<scope>): <résumé impératif court>` en français.
- Types : `feat|fix|test|refactor|core|docs|ci`. `core` = build, configuration, outillage (jamais `chore`).
- La portée est **toujours** présente, y compris pour la documentation : `docs(readme): …`, jamais `docs: …`.
- Scopes usuels : `build`, `config`, `fixtures`, `pages`, `sync`, `diagnostic`, `secret`, `assertions`,
  `failure` ; pour `docs`, le nom du document : `readme`, `claude`, `prompt`, `suivi`, `socle`.
- Corps facultatif : 2 à 4 puces d'une ligne, pas de pavé.

## Statut du projet

> Les entrées antérieures au 2026-10-20 citent les anciens noms de packages, de classes et de fixtures ;
> correspondance : `docs/qa-test-suivi-phase1.md`, section « Mise à jour 2026-10-20 ».

- **Étapes pour les tests venus de `phase1/`, noms internes en anglais, `PASS` / `NON PASS` (2026-10-20,
  suite).** T22 et T84 entièrement en étapes, T11 et T16 avec leur constat en étape, T14 avec son ouverture en
  étape ; constantes, variables et paramètres traduits (portée vérifiée : privés à leur classe ou méthode) ;
  `@DisplayName` en `PASS` / `NON PASS` / `PASS + NON PASS` ; commentaires HTML des fixtures à jour (eux seuls).
  Campagne complète (Java 17) : **104 tests, 0 KO** ; Chrome 93 OK, 11 ignorés, 3 min 37 ; Firefox 92 OK, 12
  ignorés, 3 min 47. Détail et contrôles du rapport : `docs/qa-test-suivi-phase1.md`, section la plus récente.
- **Convention de nommage appliquée (2026-10-20).** Code en anglais, `@DisplayName` en français sans accents
  avec le cas (passant / non passant, depuis `PASS` / `NON PASS`), `sync/` devenu `config/`, `phase1/` réparti dans les domaines, fixtures
  en anglais rangées comme les tests, étapes au nom de la méthode du socle appelée. Nouveaux ID : T83, T84, S3,
  S4, S5 (et T17 à T19 portés par les noms de méthodes). Campagne complète après renommage (Java 17) :
  **104 tests, 0 KO** sur les deux navigateurs ; Chrome 93 OK, 11 ignorés (`@Disabled`), 3 min 38 ; Firefox
  92 OK, 12 ignorés (dont T83, « Aborted » hors Chrome), 3 min 43. Les mêmes tests passent qu'avant le
  renommage.
- **Soft-assert visible dans le rapport, T82 refondu (2026-10-19).** Le socle signale chaque échec soft
  survenu dans une étape (sous-étape rouge, un `{step}.html` par étape en échec ; commits socle `81f66d5`,
  `da57c34`, écart `docs/qa-socle-suggestions.md` §7). T82 est désormais **rouge à dessein et `@Disabled`** :
  16 `should*` (13 KO, 3 OK, le dernier OK) sur `fixtures/websync/soft.html`, joués en bloc hors étape (agrégat
  de 13 vérifié dans le code), puis en étapes (13 rouges, 3 vertes), lancé à la main (README « Lancer les
  tests » §6). Vérifié sur Chrome et Firefox. Le groupe `soft` pose aussi SHORT à 1 s. Le message final de
  Serenity compte 14 échecs (les 13 des étapes, plus l'agrégat du socle qui les reprend) : laissé en l'état.
- **Étapes `@Step` dans `websync/` (2026-10-18).** Actions et vérifications passantes en étapes
  (`steps/websync/`), capture après chaque étape (`AFTER_EACH_STEP`) ; règle et exceptions : « Principes de
  test » ci-dessous.
- **Couverture de l'API `WebSync` complète (2026-10-18).** T25 à T82 sur des fixtures dédiées
  (`fixtures/websync/`). Couverture fonctionnelle (méthode publique de synchro appelée par au moins un test
  de la campagne, et non lignes/branches) : **100 % (78/78)**, contre **20 % (16/78)** avant. Nouveaux groupes
  **`polling`** (T78) et **`soft`** (T82) ; `within` sur 5 niveaux (T79-T81) ; `setWindowFocus` en alternance
  mère/fille (T75). Les 2 écarts socle révélés, **T43** (`count`/`getTexts` sur absent attendaient l'implicit
  wait, §5) et **T76** (`setWindowFocus` sur absent : faux succès, §6), sont **corrigés par le socle**
  (commits `8736508`, `1dda2d7`). Dernière campagne complète (2026-10-18, Java 17, socle corrigé) : Chrome
  104 tests, 93 OK, 10 ignorés ; Firefox 92 OK, 11 ignorés (`ThrottledLoadTest` interrompu, attendu). Seul KO
  sur les deux : l'**ancien** T82, depuis refondu et `@Disabled` (campagne non relancée depuis).
- **Socle package-by-feature → campagne unique, Chrome + Firefox tout-verts (2026-10-18).** Imports migrés
  vers les packages par fonctionnalité du socle. La campagne tient en **une commande par navigateur**
  (groupes `defaut` / `cle` / `precedence`, cf. « Commandes courantes »). Tests aux limites réécrits selon la
  règle **± 5 s** (16 cas, passants **et** non passants, chronométrés), fenêtre **800x600** sur les deux
  navigateurs, T10bis avec défilement explicite. Chaque test porte **ses assertions dans sa méthode** et
  n'écrit son `RESULTAT` qu'**après** elles. Détail et timings : `docs/qa-test-suivi-phase1.md`.
- **Socle corrigé → PHASE 1 tout-vert (Chrome, 2026-08-02).** Le moteur SYNC a été implémenté côté socle
  (`develop`) : les profils `SyncTimeout` / overrides `-Dqa.sync.timeout.*` sont désormais appliqués, et le
  diagnostic `[ABSENT]/[DISABLED]/[OUT_OF_BOUNDS]` produit. Avec les **bons `-D` par test** (campagne de
  l'époque en 5 invocations, remplacée le 2026-10-18 par le profil `campagne`), les anciens 8 KO passent (L4, L5-SHORT, T2, T8, T14 par le correctif
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
  renvoie/capture une exception pour signaler un échec, c'est le test (`assertThrows`) qui le vérifie (sauf
  T82 : `try/catch`, cf. « Piège Serenity » ci-dessus).
  (2) Logs uniformes — cas non passant : `RESULTAT : exception attendue=… | levee=OUI | type reel=… |
  message="…" => cas non passant confirme` ; cas passant : `RESULTAT : … | aucune exception => cas passant
  confirme`. Le `RESULTAT` est écrit **après** les assertions : il n'annonce jamais « confirmé » avant
  vérification. (3) **Assertions visibles** dans chaque méthode de test (pas cachées dans un helper ; seule
  exception : les `should*` des Steps Serenity, ex. `shouldSeeLoginSuccess`).
- **Reste à faire** :
  1. producteurs d'artefacts `@Disabled` à transformer en **prérequis** de leurs vérificateurs (T18-T23) ;
  2. écart socle encore ouvert : `[OUT_OF_BOUNDS]` faux positif sur `display:none` (T7, suggestions §4) ;
  3. **PHASE 2** : tests S1 et S2 sur sites réels ;
  4. **PHASE 3** : parallélisme (un DOM par test KO, pas de fuite `ThreadLocal`) ;
  5. `Jenkinsfile` réel (README « Lancer les tests » §5, pipeline non encore exécuté).

## Documentation de référence

- `docs/qa-test-prompt.md` — mission de validation navigateur, tableaux des tests L*/T*/S* des trois phases
- `docs/qa-test-suivi-phase1.md` — suivi d'exécution (statuts, timings, écarts socle, journal), section la plus
  récente en tête
- `docs/qa-socle-suggestions.md` — retours/suggestions remontés vers le socle, état de chacun en en-tête
- `docs/note-agent-socle-T43-T76.md`, `docs/note-agent-socle-soft-assert.md` — notes transmises à l'agent du
  socle (écarts §5-§6 et §7), avec leur suite
- `docs/qa-test-briefing-erreurs-phase1.md`, `docs/qa-test-rapport-run-complet-2026-08-02.md` — traces
  historiques datées (non réécrites)
- Contexte socle : `CLAUDE.md` et `qa-socle/docs/*` du dépôt `ArnaudS-A5/qa-template`
