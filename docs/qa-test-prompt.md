# Prompt — Validation navigateur du socle QA au projet consommateur `qa-test`

Tu interviens dans une **conversation neuve**, dans un **projet de test consommateur** nommé `qa-test`
qui **existe déjà** (un site de test y a déjà été utilisé, avec des PageObjects). Ta mission : **valider
au navigateur réel** le comportement d'une librairie interne (« le socle ») consommée comme dépendance
Maven. On procède en **deux phases** : **(1) fixtures locales** pour des **tests aux limites** précis des
mécanismes d'attente/horloge, puis **(2) sites réels** pour la navigation réaliste. Document **autoportant**.

> Ce projet est aussi un **harnais de non-régression** pérenne : on **ne supprime aucun test existant**.
> Il revalide le socle à chaque modification, en particulier en **intégration / livraison continue (CI/CD)**.

## 1. Contexte — c'est quoi le socle (`qa-socle`) ?

`qa-socle` est une librairie Java (jar) qui outille **Selenium 4.25 / Serenity BDD 4.2.22 / JUnit 5.11.4 /
Maven (Java 17)**, consommée par ~17 projets de tests. Cœur de valeur : **synchronisation robuste** des
interactions web (attentes, ré-résolution, diagnostic d'échec) + **artefacts d'échec** exploitables.

- parent : `com.example.qa:qa-parent:1.0.0-SNAPSHOT` (POM parent + agrégateur, centralise TOUTES les versions)
- lib : `com.example.qa:qa-socle` (le jar consommé)

Le base package `com.example.qa` est un **placeholder assumé** — tu le laisses tel quel.

### API publique (`com.example.qa.api.*`)

- **`api.sync.WebSync`** : façade web. Toutes les méthodes prennent un `org.openqa.selenium.By` et **ne
  rendent jamais** de `WebElement` vivant (no-leak). ~78 méthodes **nommées exactement** comme la méthode
  Serenity/Selenium sous-jacente (wrapper de synchro) :
    - actions : `click`, `type(By, CharSequence...)`, `type(By, Secret)`, `typeAndEnter`, `clear`,
      `submit`, `doubleClick`, `contextClick`, `typeAndTab`, `setWindowFocus`
    - `<select>` : `selectByVisibleText/Value/Index`, `deselect*`, `getSelectOptions`,
      `getFirstSelectedOptionVisibleText`, `getSelectedVisibleTexts`…
    - lectures : `getText`, `getValue`, `getAttribute`, `getDomAttribute`, `getDomProperty`, `getTagName`,
      `getCssValue`…
    - états (**ne lèvent jamais**, absent → `false`) : `isVisible`, `isCurrentlyVisible`, `isPresent`,
      `isEnabled`, `isDisabled`, `isClickable`, `isSelected`, `containsText`, `hasClass`…
    - assertions (avec attente) : `shouldBeVisible`, `shouldNotBeVisible`, `shouldBePresent`,
      `shouldBeEnabled`, `shouldBeSelected`, `shouldContainText`, `shouldContainSelectedOption`…
    - attentes explicites : `waitUntilVisible/NotVisible/Present/Clickable/Enabled/Disabled`,
      `waitForElement(By, Duration)`, `waitForElementToDisappear(By, Duration)`
    - collections : `count(By)`, `getTexts(By)` ; imbriqué : `within(By container)` → `WebSync` borné
- **`api.sync.SyncTimeout`** : profils `SHORT/MEDIUM/LONG/EXTRA_LONG` (défauts **5/15/60/120 s**), choisis
  à la construction (`new WebSync()` = `MEDIUM`, `new WebSync(SyncTimeout.LONG)`). Valeurs pilotées par
  `qa.sync.timeout.<profil>`. **Jamais de durée en dur**.
- **`api.secret.Secret`** / `SecretManagers` ; **`api.exception.SyncException`** (unchecked).

### Mécanismes internes (package `internal.*`, non importé)

- **`fluentWait`** : ré-résout le `By` en boucle (polling ~250 ms, clé `qa.sync.pollingInterval`) jusqu'à
  succès ou timeout du profil, en **absorbant** les transitoires (`NoSuchElement`, `StaleElement`,
  `ElementNotInteractable`, `ElementClickIntercepted`). Au timeout → `SyncException` + **diagnostic actif**.
- **Diagnostic actif (`SyncCondition`)** : sonde l'état réel dans l'ordre
  `ABSENT → STALE → HIDDEN → DISABLED → OUT_OF_BOUNDS → INTERCEPTED` (interception via hit-test JS), remonte
  **toutes** les conditions : message avec **codes** stables `[HIDDEN]`… , `cause` = dernière exception,
  latentes en `suppressed`.
- **Une seule horloge (C8) + toggle implicit wait** : `fluentWait` = horloge **principale**. L'implicit wait
  natif (recommandé **3 s**, `webdriver.timeouts.implicitlywait`) = **filet aux appels Selenium bruts** hors
  `WebSync` ; le socle le **neutralise à 0** pendant son polling puis le **restaure** → les deux attentes
  **ne s'additionnent jamais** (point clé, T4).
- **`within`** : résolution deux-temps (container puis child) → localise le maillon fautif.
- **`pageLoadTimeout`** : plafond (clé `qa.sync.pageLoadTimeout`, défaut 120 s) posé **auto** au 1ᵉʳ contact driver.
- **Soft-assert (`qa.assertions.soft`)** : `false` = échec 1ʳᵉ assertion ; `true` = les `should*` collectent,
  échec **en fin** via extension JUnit **auto-détectée** (aucune annotation côté test).
- **Activation native (ServiceLoader)** — 3 services par **présence du jar** : `TestFailureManager`
  (artefacts KO), `RawDomCaptureListener` (DOM brut au `stepFailed`), `SoftAssertionExtension` (drain soft).
- **Artefacts d'échec** (par test KO) : `target/qa-results/KO__{ENV}__{NomDuTest}__{yyyy-MM-dd_HH-mm-ss}/`
  = `ERROR.log` + `FAIL.log` + `{stepEnEchec}.html` (**DOM brut**, `value` des `input[type=password]`
  **blanchie**). `{ENV}` = clé Serenity `environment`.

### Implémenté (à tester) vs hors périmètre

- **Implémenté** : tout **SYNC web** (moteur, profils, toggle horloge, diagnostic, `within`, surface,
  `should*`, waits, `pageLoadTimeout`), **FAILURE** (artefacts + DOM + masquage), **soft-assert**, activation.
- **HORS périmètre** (ne pas tester) : **sentinelle JS async** (emplacement vide), **CyberArk**, **reporting
  AUTO**, **DataFileManager** (JDD **en dur** temporaire). Mobile : différé.

## 2. Prérequis, mise en place, fixtures

### Prérequis (fait côté socle — PAS depuis `qa-test`)

`./mvnw clean install` est lancé **dans le dépôt du socle** → `qa-parent` + `qa-socle` `1.0.0-SNAPSHOT`
dans le `~/.m2`. **Rien à installer depuis `qa-test`.** *(En CI : Artifactory.)*

### Dans `qa-test`

1. **POM** : `qa-parent` en **parent**, `qa-socle` en **dépendance SANS version**. Rien d'autre.
2. **`src/test/resources/serenity.conf`** :
   ```
   environment = qatest
   webdriver.driver = chrome               # rejouer aussi en firefox
   webdriver.autodownload = true
   qa.assertions.soft = false              # true pour T17
   webdriver.timeouts.implicitlywait = 3000
   serenity.driver.page.load.strategy = normal
   qa.failure.artefacts.outputDir = target/qa-results
   qa.reporting.enabled = false
   ```
3. **Navigateurs** : valider **Chrome ET Firefox** (`mvn clean verify` puis
   `mvn clean verify -Dwebdriver.driver=firefox`). Les deux doivent passer.

### Fixtures (PHASE 1) — pages HTML locales sous `src/test/resources/fixtures/`

Ouvertes en `file://` (`getDriver().get("file://"+chemin)`), **aucun serveur**. But : **contrôle exact**
des délais/états pour les **tests aux limites**. Fixtures à créer (JS inline) :

- **`appear.html`** — lit `?delay=N` (ms) et insère `#target` après `setTimeout(N)`. **Fixture pivot des
  bornes** : une seule page, tous les seuils.
- **`disappear.html?delay=N`** — retire un élément après N ms (waits négatifs).
- **`rerender.html?interval=N`** — remplace le nœud `#target` toutes les N ms (stale/re-render).
- **`hidden.html`** — `#target` présent en `display:none`.
- **`disabled.html`** — champ/bouton `disabled` (option : réactivé après N ms).
- **`overlay.html`** — overlay `position:fixed` couvrant `#target` (interception).
- **`offscreen.html`** — `#target` à `top:5000px` (hors viewport).
- **`slowload.html?ms=N`** — bloque le chargement N ms (`<script>` occupé) pour `pageLoadTimeout`.
- **`form.html`** — inputs, `<select>`, checkbox, `input[type=password]` (actions, masquage, Secret).
- **`nested.html`** — container `#box` contenant `#child` (pour `within`).

### Sites réels (PHASE 2)

**Réutiliser** le site et les PageObjects **déjà présents** ; **compléter** avec de nouveaux PageObjects
exerçant la dizaine de méthodes sur des widgets réels. Référence : `https://the-internet.herokuapp.com/`
(*Dynamic Loading, Dynamic Controls, Checkboxes, Dropdown, Context Menu, Add/Remove, Login, Challenging
DOM*). Secours : `demoqa.com`, `saucedemo.com`. Chaque PageObject détient un `WebSync` et agit par `By`.

## 3. Tests — PHASE 1 (fixtures, bornes précises)

**Tests aux limites (le cœur).** Pour **chaque** mécanisme d'attente : cible qui apparaît **juste avant /
pile / juste après** le seuil. Pour rester rapide, **abaisser le profil** via `-Dqa.sync.timeout.*` (ex.
`medium=2s`) et garder le schéma. Mesurer les **timings**.

| # | Mécanisme | Manip (fixture) | Attendu |
|---|---|---|---|
| L1 | implicit wait 3 s — **borne basse** | `appear.html?delay=2800`, `getDriver().findElement` **brut** | trouvé (< 3 s) |
| L2 | implicit wait 3 s — **borne haute** | `appear.html?delay=3200`, `findElement` **brut** | `NoSuchElement` (> 3 s) |
| L3 | profil (ex. MEDIUM) — **borne basse** | profil réglé à T (via `-D`), `appear.html?delay=T-200`, `WebSync.click` | succès |
| L4 | profil — **borne haute** | `appear.html?delay=T+200`, `WebSync.click` | `SyncException` `[ABSENT]` au timeout |
| L5 | **répéter L3/L4 pour SHORT / MEDIUM / LONG / EXTRA_LONG** (au moins un run aux **vraies** valeurs 14/15/16 s pour MEDIUM) | idem, profils variés | bornes respectées par profil |
| L6 | polling **fin** sous WebSync malgré implicit 3 s (T4) | `appear.html?delay=2500` : `WebSync` (profil 10 s) **vs** `findElement` brut | WebSync réussit tôt (polling ~250 ms) ; brut attend jusqu'à ~3 s |

**Comportements précis (fixtures)** :

| # | Quoi | Fixture | Attendu |
|---|---|---|---|
| T1 | `fluentWait` attend un élément tardif | `appear.html?delay=…` | action réussit |
| T2 | jamais présent | locator bidon | `SyncException` `[ABSENT]` |
| T5 | stale/re-render absorbé | `rerender.html` | ré-résolution transparente, pas de `StaleElement` |
| T6 | `pageLoadTimeout` posé auto (borne) | plafond abaissé (`-Dqa.sync.pageLoadTimeout=5s`) + `slowload.html?ms=8000` | échec de navigation borné ~5 s |
| T7 | `HIDDEN` | `hidden.html` | `[HIDDEN]` |
| T8 | `DISABLED` | `disabled.html` | `[DISABLED]` |
| T9 | `INTERCEPTED` | `overlay.html` | `[INTERCEPTED]` |
| T10 | `OUT_OF_BOUNDS` | `offscreen.html` | `[OUT_OF_BOUNDS]` |
| T11 | `within` container vs child absent | `nested.html` (child ou box bidon) | `cause` = bon maillon |
| T14 | `is*`/`contains*` : absent → `false`, **instantané** | locator bidon | `false` immédiat (pas de 3 s) |
| T15 | `waitUntil*` positifs/négatifs | `appear.html` / `disappear.html` | attentes honorées |
| T16 | `should*` **dur** (`soft=false`) | `form.html` | échoue à la 1ʳᵉ |
| T17 | `should*` **soft** (`soft=true`) | plusieurs `should*` KO | agrégé, échec en fin (extension auto) |
| T18 | KO → dossier `KO__…/` + 3 fichiers | provoquer un KO | dossier + `ERROR.log`+`FAIL.log`+`{step}.html` |
| T19 | dump = DOM réel à l'échec | — | page source du moment |
| T20 | masquage `input[type=password]` (**zéro clair**) | `form.html` password rempli | `value` blanchie dans le `.html` |
| T22 | `type(By, Secret)` | `form.html` | saisie OK + masquage |
| T23 | 3 services actifs par présence du jar | test KO, aucun `@ExtendWith` custom | listeners actifs |
| T24 | précédence `-D` > `serenity.conf` | override une clé | `-D` gagne |

## 4. Tests — PHASE 2 (sites réels)

Navigation réaliste + couverture de la **surface d'interaction** sur widgets réels, et **régression**.

| # | Quoi | Site / page réelle | Attendu |
|---|---|---|---|
| S1 | click / `<select>` / checkbox / clic droit / double-clic / saisie / submit | *the-internet* : Checkboxes, Dropdown, Context Menu, Inputs ; demoqa Buttons | effet réel + synchro |
| S2 | lectures getText/getValue/getAttribute/getDom* | pages réelles | valeurs correctes |
| S3 | `fluentWait` / `waitUntil*` en conditions réelles | *the-internet* : Dynamic Loading, Dynamic Controls | attentes honorées |
| S4 | password réel + masquage à l'échec | *the-internet* : Login (`/login`) | saisie OK, `value` blanchie si KO |
| S5 | reprise des PageObjects existants (non-régression) | site déjà en place | tout reste vert |

## 5. Méthode & phasage

1. **Séquentiel d'abord** (priorité), un test après l'autre. **Phase 1 (fixtures)** en premier — c'est là
   que les **bornes** se valident proprement — puis **Phase 2 (sites réels)**.
2. Premier jalon : **un test vert** minimal (fixture `appear.html` + `click` synchronisé).
3. Pour chaque test : **nommer**, **isoler**, **vérifier** (résultat, **codes** de la `SyncException`,
   **présence/contenu** des artefacts `KO__`, ou **timings** chronométrés). Consigner un **tableau de
   résultats** (id, OK/KO, écart).
4. Rejouer en **Firefox**. Les deux navigateurs doivent passer.
5. **PHASE 3 (après tout le séquentiel)** : **parallélisme** — activer Surefire/JUnit parallèle et vérifier
   que chaque test KO produit **son** DOM (pas de fuite `ThreadLocal`).

Le **code du socle fait foi** : toute divergence doc/comportement est un écart à **signaler** (backlog
socle), pas à corriger en douce côté test.

## 6. Rappels / pièges

- Les `is*`/`contains*` ne lèvent **jamais** et sont **instantanés** (un `isVisible` sur absent à 3 s = le
  toggle d'implicit wait est en cause → L6/T14).
- Le nom de méthode du socle **est** celui de Serenity/Selenium appelé (`isVisible`, pas `isDisplayed`).
- Artefacts `KO__` créés **uniquement** pour un test **KO** ; délimiteur `__` (double) volontaire.
- `{ENV}` = clé `environment`. **On ne supprime aucun test existant** (harnais CI/CD).
- Bornes : jouer sur `?delay=` des fixtures **et** l'abaissement des profils via `-D` pour rester rapide,
  avec au moins un contrôle aux **vraies** valeurs.
- Docs de référence du socle (si accès) :
  `qa-socle/docs/{decisions.md,sfd.md,revue-socle.md,serenity.conf.example,points-attention.md}`.