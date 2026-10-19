# Prompt — Validation navigateur du socle QA au projet consommateur `qa-test`

Tu interviens dans une **conversation neuve**, dans un **projet de test consommateur** nommé `qa-test`
qui **existe déjà** (un site de test y a déjà été utilisé, avec des PageObjects). Ta mission : **valider
au navigateur réel** le comportement d'une librairie interne (« le socle ») consommée comme dépendance
Maven. On procède en **trois phases** : **(1) fixtures locales** pour des **tests aux limites** précis des
mécanismes d'attente/horloge, puis **(2) sites réels** pour la navigation réaliste, enfin **(3) parallélisme**
(§5). Document **autoportant**.

> Ce projet est aussi un **harnais de non-régression** pérenne : on **ne supprime aucun test existant**.
> Il revalide le socle à chaque modification, en particulier en **intégration / livraison continue (CI/CD)**.

## 1. Contexte — c'est quoi le socle (`qa-socle`) ?

`qa-socle` est une librairie Java (jar) qui outille **Selenium 4.25 / Serenity BDD 4.2.22 / JUnit 5.11.4 /
Maven (Java 17)**, consommée par ~17 projets de tests. Cœur de valeur : **synchronisation robuste** des
interactions web (attentes, ré-résolution, diagnostic d'échec) + **artefacts d'échec** exploitables.

- parent : `com.example.qa:qa-parent:1.0.0-SNAPSHOT` (POM parent + agrégateur, centralise TOUTES les versions)
- lib : `com.example.qa:qa-socle` (le jar consommé)

Le base package `com.example.qa` est un **placeholder assumé** — tu le laisses tel quel.

### API publique (`com.example.qa.*`, packages par fonctionnalité)

- **`sync.WebSync`** : façade web. Toutes les méthodes prennent un `org.openqa.selenium.By` et **ne
  rendent jamais** de `WebElement` vivant (no-leak). ~78 méthodes **nommées exactement** comme la méthode
  Serenity/Selenium sous-jacente (wrapper de synchro) :
    - actions : `click`, `type(By, CharSequence...)`, `type(By, Secret)`, `typeAndEnter`, `clear`,
      `submit`, `doubleClick`, `contextClick`, `typeAndTab`, `setWindowFocus`, `clickViaJS`, `typeViaJS`
    - `<select>` : `selectByVisibleText/Value/Index`, `deselect*`, `getSelectOptions`,
      `getFirstSelectedOptionVisibleText`, `getSelectedVisibleTexts`…
    - lectures : `getText`, `getValue`, `getAttribute`, `getDomAttribute`, `getDomProperty`, `getTagName`,
      `getCssValue`…
    - états (**ne lèvent jamais**, absent → `false`) : `isVisible`, `isCurrentlyVisible`, `isPresent`,
      `isEnabled`, `isDisabled`, `isClickable`, `isSelected`, `containsText`, `hasClass`…
    - assertions (avec attente jusqu'au timeout du profil, sauf `shouldBeCurrentlyVisible`, immédiate) :
      `shouldBeVisible`, `shouldNotBeVisible`, `shouldBePresent`, `shouldBeEnabled`, `shouldBeSelected`,
      `shouldContainText`, `shouldContainSelectedOption`…
    - attentes explicites : `waitUntilVisible/NotVisible/Present/Clickable/Enabled/Disabled`,
      `waitForElement(By, Duration)`, `waitForElementToDisappear(By, Duration)`
    - collections : `count(By)`, `getTexts(By)` ; imbriqué : `within(By container)` → `WebSync` borné
- **`sync.SyncTimeout`** : profils `SHORT/MEDIUM/LONG/EXTRA_LONG` (défauts **5/15/60/120 s**), choisis
  à la construction (`new WebSync()` = `MEDIUM`, `new WebSync(SyncTimeout.LONG)`). Valeurs pilotées par
  `qa.sync.timeout.<profil>`. **Jamais de durée en dur**.
- **`secret.Secret`** / `SecretManagers` ; **`sync.SyncException`** (unchecked) ; **`page.QaPageObject`**.

### Mécanismes internes (package-private dans leur package, non importés)

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
  échec **en fin** via extension JUnit **auto-détectée** (aucune annotation côté test). Un échec soft survenu
  **dans une `@Step`** est aussi signalé à Serenity : l'étape est **rouge** (sous-étape portant le message),
  les suivantes s'exécutent.
- **Activation native (ServiceLoader)** — 3 services par **présence du jar** : `TestFailureManager`
  (artefacts KO), `RawDomCaptureListener` (DOM brut au `stepFailed`), `SoftAssertionExtension` (drain soft).
- **Artefacts d'échec** (par test KO) : `target/qa-results/KO__{ENV}__{NomDuTest}__{yyyy-MM-dd_HH-mm-ss}/`
  = `ERROR.log` + `FAIL.log` + un `{stepEnEchec}.html` **par step en échec** (**DOM brut**, `value` des
  `input[type=password]` **blanchie** ; aucun dump si l'échec survient hors `@Step`). `{ENV}` = clé Serenity
  `environment`.

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
   qa.assertions.soft = false              # true pour T17 (-D) et T82 (exécution « soft » du pom)
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

Fixtures **dédiées à la surface `WebSync`** (sous `fixtures/websync/`). Une fixture existante n'est jamais
modifiée : un besoin nouveau = une fixture nouvelle, pour ne pas faire régresser les tests en place.

- **`saisie.html`** — `#nom` (prérempli « ancien ») → `#prenom` → `#ville` → `#envoyer` dans l'ordre de
  tabulation ; chaque soumission écrit `nom|prenom|ville` dans `#recap` et compte dans `#nb-submit`.
- **`javascript.html`** — fixture **unique** des actions JS : `#recouvert` sous `#voile`, `#desactive`
  (disabled), `#champ-js` / `#zone-js` avec journal d'événements, `#non-champ` (div).
- **`pointeur.html`** — `#cible` : `#statut` = dernier événement (`dblclick`, `contextmenu bouton=N`),
  `#nb-clics` = nombre de `click`.
- **`select.html`** — `#simple` (Option A/B/C, valeurs a/b/c), `#multiple` (Rouge/Vert/Bleu/Jaune, valeurs
  r/v/b/j, rien de sélectionné).
- **`attributs.html`** — texte visible + masqué, `input value="initial"`, classes et couleur, ARIA,
  `#boite` en position absolue (500,150) 150×40.
- **`collection.html`** — `#liste` (Un/Deux/Trois), `#conteneur` (2 `.item`), `#vide`.
- **`etats.html`** — un élément par état figé : `#visible`, `#cache`, `#desactive`, `#recouvert`,
  `#cache-desactive`, `#coche` / `#non-coche`, `#texte`, `#champ` ; `#absent` n'existe pas.
- **`transitions.html?scenario=…&delai=N`** — `#cible` change d'état après N ms : `apparition`,
  `apparition-cachee`, `activation`, `desactivation`, `disparition` (outillage `support/Transitions`).
- **`fenetre-mere.html`** / **`fenetre-fille.html`** — la mère ouvre la fille (`#ouvrir-fille`, fenêtre
  séparée) ; chacune publie son état de focus et espionne `window.focus()` (`#nb-appels` compte les appels).
- **`imbrication.html`** — deux branches `#branche-x > .niveau-2 > … > .niveau-5 > .cible/.action`, plus un
  **leurre** dans la branche B avant le vrai chemin : seule la chaîne des 5 `within` atteint « cible B ».
- **`soft.html`** — récapitulatif de commande statique (T82), éléments rangés dans l'ordre où T82 les vérifie :
  `#erreur` caché, `#confirmation` visible (« Commande n° 1234 validée »), `#code-promo` absent, `#annuler`
  désactivé, `#payer` actif, `#newsletter` non cochée, `#cgv` cochée, `#livraison` (3 options) ; à côté de
  chaque élément, son état réel et les numéros des vérifications qui le visent, avec le résultat attendu.

### Sites réels (PHASE 2)

**Réutiliser** le site et les PageObjects **déjà présents** ; **compléter** avec de nouveaux PageObjects
exerçant la dizaine de méthodes sur des widgets réels. Référence : `https://the-internet.herokuapp.com/`
(*Dynamic Loading, Dynamic Controls, Checkboxes, Dropdown, Context Menu, Add/Remove, Login, Challenging
DOM*). Secours : `demoqa.com`, `saucedemo.com`. Chaque PageObject détient un `WebSync` et agit par `By`.

## 3. Tests — PHASE 1 (fixtures, bornes précises)

**Tests aux limites (le cœur).** Pour **chaque** mécanisme d'attente : cible qui apparaît **avant /
après** le seuil, selon la règle **± 5 s** (marge réduite à **2 s** quand le timeout est trop court :
SHORT par défaut 5 s, implicit wait 3 s). Couvrir le timeout **par défaut** du socle (aucune clé) **et**
le timeout **configuré par clé** `qa.sync.timeout.*` — chaque clé **différente du défaut**, sinon rien ne
prouve qu'elle est lue. Les valeurs vivent dans le `pom.xml` (groupes `defaut` / `cle`) ; les tests les
**lisent** et en déduisent leurs délais. **Chronométrer** chaque cas.

| # | Mécanisme | Manip (fixture) | Attendu |
|---|---|---|---|
| L1 | implicit wait (3 s) — **borne basse** | `appear.html?delay=implicit−2s`, `getDriver().findElement` **brut** | trouvé avant l'implicit wait |
| L2 | implicit wait (3 s) — **borne haute** | `appear.html?delay=implicit+2s`, `findElement` **brut** | `NoSuchElement` entre 3 s et 5 s |
| L3 | profil MEDIUM (clé 25 s) — **borne basse** | `appear.html?delay=T−5s`, `WebSync.click` | clic abouti avant T |
| L4 | profil MEDIUM (clé 25 s) — **borne haute** | `appear.html?delay=T+5s`, `WebSync.click` | `SyncException` `[ABSENT]` entre T et T+5 s |
| L5 | **L3/L4 pour chaque profil** : SHORT / MEDIUM aux **défauts** (5 s / 15 s) ; SHORT / MEDIUM / LONG / EXTRA_LONG **par clé** (10 / 25 / 30 / 40 s) | idem, un cas passant **et** un cas non passant par profil | bornes respectées par profil |
| L6 | polling **fin** sous WebSync malgré implicit 3 s (T4) | `appear.html?delay=2500` : `WebSync` (profil LONG) **vs** `findElement` brut | WebSync réussit tôt (polling ~250 ms) ; brut attend jusqu'à ~3 s |

**Comportements précis (fixtures)** :

| # | Quoi | Fixture | Attendu |
|---|---|---|---|
| T1 | `fluentWait` attend un élément tardif | `appear.html?delay=…` | action réussit |
| T2 | jamais présent | locator bidon | `SyncException` `[ABSENT]` |
| T5 | stale/re-render absorbé | `rerender.html` | ré-résolution transparente, pas de `StaleElement` |
| T6 | `pageLoadTimeout` posé auto (borne) | plafond par clé (`qa.sync.pageLoadTimeout=10s`) + `slowload.html?ms=plafond+5s` (T6) / `ms=plafond−5s` (T6bis) | T6 : navigation bornée au plafond ; T6bis : page chargée |
| T7 | `HIDDEN` | `hidden.html` | `[HIDDEN]` |
| T8 | `DISABLED` | `disabled.html` | `[DISABLED]` |
| T9 | `INTERCEPTED` | `overlay.html` | `[INTERCEPTED]` |
| T10 | `OUT_OF_BOUNDS` | `offscreen.html` | *reclassé* : inatteignable via `WebSync` (scroll-into-view) → clic abouti ; T10bis : équivalence de trace avec `Actions` (après défilement explicite) |
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

**Surface de l'API `WebSync` (fixtures dédiées `fixtures/websync/`)** — un sous-package de
`org.automation.websync` par domaine du socle ; chaque test vérifie l'effet de **chaque** action :

| # | Quoi | Fixture | Attendu |
|---|---|---|---|
| T25 | `typeAndTab` ×2 puis `typeAndEnter`, `hasFocus` | `saisie.html` | valeur remplacée, focus `#nom`→`#prenom`→`#ville`, 1 soumission `Dupont\|Jean\|Lyon` |
| T26 | `clear` puis `submit`, `containsValue` | `saisie.html` | champ vidé, 1 soumission `Martin\|\|Paris` |
| T27 | `clickViaJS` sur élément recouvert | `javascript.html` | préalable : clic natif `[INTERCEPTED]` ; clic JS → gestionnaire déclenché |
| T28 | `clickViaJS` sur élément `disabled` | `javascript.html` | `SyncException` `[DISABLED]`, aucun effet |
| T29 | `typeViaJS` sur `input` et `textarea` | `javascript.html` | valeur remplacée, événements `input` puis `change` |
| T30 | `typeViaJS` sur un `div` | `javascript.html` | `JavascriptException` **immédiate** (non transitoire, pas d'attente du profil) |
| T31 | `doubleClick` | `pointeur.html` | `dblclick` reçu, précédé de 2 `click` |
| T32 | `contextClick` | `pointeur.html` | `contextmenu` bouton 2, aucun `click` |
| T33 | `selectByVisibleText`, `selectByIndex`, `getFirstSelectedOptionValue` | `select.html` | option lue par valeur et libellé |
| T34 | `getSelectOptions`, `getSelectOptionValues`, `containsSelectOption`, `shouldContainSelectedOption` | `select.html` | lectures exactes ; `should…` passe sur option existante, `AssertionError` sinon |
| T35 | `deselectByVisibleText/Value/Index`, `deselectAll`, `getSelectedVisibleTexts`, `getSelectedValues` | `select.html` | chaque (dé)sélection reflétée |
| T36 | `deselectByVisibleText` sur liste simple | `select.html` | `UnsupportedOperationException` **immédiate**, sélection inchangée |
| T37 | `getText` / `getTextContent` / `getTextValue` | `attributs.html` | visible seul / texte masqué inclus / valeur ou texte selon l'élément |
| T38 | `getDomAttribute` / `getDomProperty` / `getAttribute`, `getTagName` | `attributs.html` | attribut initial / propriété courante / valeur résolue |
| T39 | `getCssValue`, `hasClass` | `attributs.html` | couleur normalisée (rgba Chrome, rgb Firefox) ; `hasClass` faux sans exception sur absent |
| T40 | `getAriaLabel`, `getAriaRole`, `getAccessibleName` | `attributs.html` | valeurs ARIA exactes (Chrome et Firefox) |
| T41 | `getLocation`, `getSize`, `getRect`, `getScreenshotAs` | `attributs.html` | géométrie exacte ; PNG aux proportions de l'élément |
| T42 | `count`, `getTexts`, à la racine et dans `within` | `collection.html` | 3 / [Un, Deux, Trois] |
| T43 | `count`, `getTexts`, `within(absent).count` sur absent | `collection.html` | 0 / liste vide, **immédiats** (< 1 s) — a révélé l'écart socle §5 (implicit wait 3 s), corrigé par le socle (`8736508`) |
| T44 | `containsElements` | `collection.html` | vrai / faux sur vide / faux sans exception sur absent |
| T45 | `isDisabled`, `isClickable`, `isCurrentlyVisible` | `etats.html` | vrai et faux selon l'état |
| T46 | `containsOnlyText`, `containsValue` | `etats.html` | texte exact vs partiel ; valeur contenue ou non |
| T47 | 9 prédicats sur absent (complète T14) | `etats.html` | `false`, chacun < 1 s |
| T48–T58 | les 11 `should*` restants (un test chacun, complète T16) | `etats.html` (+ `transitions.html` pour T49) | passe sur le bon état ; `AssertionError` citant le locator au timeout SHORT, < timeout + 2 s (une horloge, C8) ; T48 `shouldBeCurrentlyVisible` : échec immédiat ; T49 : attend une disparition à 2 s |
| T59–T66 | `waitUntilPresent/Enabled/Clickable/Disabled` (passant / non passant) | `transitions.html` | ± 5 s autour de MEDIUM (15 s) ; T59 : présent mais pas visible |
| T67–T70 | `waitForElement(By, Duration)`, `waitForElementToDisappear(By, Duration)` | `transitions.html` | ± 5 s autour de la durée (8 s) ; exception avant le profil MEDIUM → la durée passée prime |
| T71–T74 | structure de la `SyncException` | `etats.html` | `cause` = exception Selenium typée ; un `suppressed` par code ; T74 : `[HIDDEN]` puis `[DISABLED]`, cause = garde d'activation |
| T75 | `setWindowFocus` en alternance mère/fille/mère/fille | `fenetre-mere.html` + `fenetre-fille.html` | chaque fenêtre reçoit **ses** appels à `window.focus()` et aucun de l'autre. Le gain de focus lui-même n'est pas vérifié : `switchTo().window()` le donne déjà, et le navigateur filtre `window.focus()` (sonde du 2026-10-18) |
| T76 | `setWindowFocus` sur absent | `fenetre-mere.html` | `SyncException [ABSENT]`, aucun appel — a révélé l'écart socle §6 (faux succès), corrigé par le socle (`1dda2d7`) |
| T77 | `QaPageObject.web(SyncTimeout)` (groupe `defaut`) | `etats.html` | vue distincte expirant à SHORT ; `web` expire ensuite à MEDIUM |
| T78 | clé `qa.sync.pollingInterval` (groupe `polling`, 2000 ms) | `transitions.html` | cible à 1,5 intervalle → clic au 3e sondage (≥ 2 intervalles) |
| T79 | `within` ×5, lecture | `imbrication.html` | « cible B » au 5e niveau ; racine = 3 cibles ; chaîne courte = leurre |
| T80 | `within` ×5, action | `imbrication.html` | le clic atteint le bouton B (ni A, ni le leurre) |
| T81 | `within` ×5 avec un maillon absent | `imbrication.html` | `SyncException [ABSENT]`, cause désignant le maillon manquant |
| T82 | soft-assert (groupe `soft`, SHORT 1 s), **rouge à dessein, `@Disabled`**, lancé à la main (README « Lancer les tests » §6) : 16 `should*` (13 KO, 3 OK, le dernier OK), joués en bloc hors étape puis en étapes | `soft.html` | **bloc** : vote de fin = un seul agrégat de 13 échecs dans l'ordre, collecteur purgé (vérifié dans le code, capturé par `try/catch` et non `assertThrows`) ; **étapes** : 13 rouges et 3 vertes, aucune sautée, un `{step}.html` par échec ; **fin** : test FAILED |

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
- Bornes : jouer sur `?delay=` des fixtures (timeout ± 5 s) avec des timeouts **réalistes** : défauts du
  socle **et** clés distinctes du défaut. Jamais de valeur abaissée « pour aller vite » dans un test aux
  limites ; la vitesse vient du parallélisme (`forkCount`).
- Chaque test porte **ses assertions** dans sa méthode, et n'écrit son `RESULTAT` qu'**après** elles.
- Sous Serenity 4.2.22, un `assertThrows(X.class, …)` déclare `X` « attendue » : si le test finit en échec sur
  `X`, Serenity repeint en vert les étapes en échec par `X`. Test rouge à dessein avec étapes rouges → capturer
  par `try/catch` (T82).
- Docs de référence du socle (si accès) :
  `qa-socle/docs/{decisions.md,sfd.md,revue-socle.md,serenity.conf.example,points-attention.md}`.