# Suggestions pour `qa-socle` — retours du projet consommateur `qa-test`

> Cible : dépôt **`ArnaudS-A5/qa-template`**, module `qa-socle`. §1 à §3 ont été observées sur la branche
> `develop` (alors `com/example/qa/internal/failure/TestFailureManager.java`) ; §4 à §7 sur la branche
> `feature/package-by-feature` (packages par fonctionnalité : `com/example/qa/failure/`, `com/example/qa/sync/`).
>
> Observées en consommant le socle depuis le projet `qa-test` (un test KO produit
> `target/qa-results/KO__{ENV}__{Test}__{ts}/` avec `ERROR.log`, `FAIL.log`, `{step}.html`).
> Corrections indépendantes, toutes côté socle.

## État au 2026-10-19

| § | Sujet | État | Vérifié côté `qa-test` par |
|---|---|---|---|
| 1 | dump HTML rendable (DOM brut, option A) | ✅ appliqué | `RawDomCaptureVerificationTest` |
| 2 | nom du fichier = step feuille en échec | ✅ appliqué | `RawDomCaptureVerificationTest` |
| 3 | translittération des accents (`sanitize`) | ✅ appliqué | noms des dossiers `KO__` et des `{step}.html` |
| 4 | `[OUT_OF_BOUNDS]` à tort sur `display:none` | ⬜ **ouvert** | T7 (vert : ne vérifie que `[HIDDEN]`) |
| 5 | `count` / `getTexts` soumis à l'implicit wait | ✅ corrigé (`8736508`) | T43, Chrome et Firefox |
| 6 | `setWindowFocus(By)` sans résolution du locator | ✅ corrigé (`1dda2d7`, option 1) | T76, Chrome et Firefox |
| 7 | échec soft non signalé à Serenity | ✅ corrigé (`81f66d5`, `da57c34`) | T82, Chrome et Firefox (cf. §7, « Suite ») |

---

## 1. Le dump HTML n'est pas une page rendable (vue « code » Prism, échappée)

### Constat
Le fichier `{step}.html` produit n'est pas un instantané du DOM mais la **vue source de
Serenity** : le markup réel est **HTML‑échappé** dans un `<pre><code class='language-html'>`,
destiné à être colorisé par **Prism.js** :

```html
<html lang='en'><head><link href='prism/prism.css' rel='stylesheet' /></head>
<body><script src='prism/prism.js'></script>
  <pre><code class='language-html'>&lt;html class="no-js" lang="en"&gt; ... </code></pre>
```

Ouvert seul dans un navigateur, les assets `prism/*` (relatifs) manquent → on voit le markup
**comme du texte**, pas la page. (Indépendant de l'OS : identique macOS/Windows.)

### Cause
`failingStepHtml(...)` lit `ScreenshotAndHtmlSource.getHtmlSource()`, qui pointe sur cette vue
« report » d'ores et déjà échappée/wrappée, et non sur le DOM brut.

### Correctif proposé
Capter le **DOM brut** via le WebDriver, qui se rend directement dans un navigateur. Le DOM
n'est plus disponible de façon fiable dans `executionFinished` (le driver peut être fermé) :
deux niveaux possibles.

**Option B — minimale (dans la couche d'extraction), avec repli :**
```java
private static String failingStepHtml(TestOutcome outcome) {
    // 1) DOM BRUT via le driver encore actif → se rend tel quel dans un navigateur.
    String raw = rawPageSourceFromLiveDriver();
    if (raw != null) {
        return raw;
    }
    // 2) Repli : ancienne source Serenity (vue "code" Prism, échappée — non rendue telle quelle).
    return failingStepHtmlFromSerenity(outcome);
}

private static String rawPageSourceFromLiveDriver() {
    try {
        // NB: classe utilitaire Serenity exacte à confirmer dans la version utilisée
        // (ThucydidesWebDriverSupport / net.serenitybdd.core.Serenity#getDriver()).
        if (ThucydidesWebDriverSupport.isDriverInstantiated()) {
            WebDriver driver = ThucydidesWebDriverSupport.getDriver();
            if (driver != null) {
                return driver.getPageSource();
            }
        }
    } catch (RuntimeException e) {
        LOGGER.debug("DOM brut indisponible au moment de la capture", e);
    }
    return null;
}
```
(`failingStepHtmlFromSerenity` = l'actuel corps de `failingStepHtml`, conservé comme repli.)

**Option A — robuste (recommandée) :** capter le DOM **au moment de l'échec de step**, tant que le
driver est vivant, via un `net.thucydides.core.steps.StepListener#stepFailed(...)` (ou une classe
compagnon) qui stocke `driver.getPageSource()` dans un `ThreadLocal<String>` ; `writeArtefacts`
consomme ensuite cette valeur. Sûr en parallèle (un ThreadLocal par thread de test).

> ⚠️ À valider côté socle : disponibilité du driver dans `executionFinished` (probablement déjà
> fermé → l'Option B retombe sur la vue Prism). L'Option A garantit un DOM brut.

---

## 2. Le nom du fichier porte le test, pas la step en échec

### Constat
Le nom attendu est `{description de la step en échec}.html`. En présence de **steps imbriquées**,
on obtient la **description du test** (step parent) au lieu de la step réellement fautive → non
conforme et imprécis.

### Cause
```java
private static Optional<TestStep> failingStep(TestOutcome outcome) {
    return outcome.getFlattenedTestSteps().stream()
            .filter(TestFailureManager::isFailing)
            .findFirst();          // ← première step KO = la step PARENT (≈ nom du test)
}
```
L'échec d'une step feuille se propage à ses parents ; `findFirst()` sur l'ordre pré‑fixe renvoie
donc le **groupe parent** avant la feuille.

### Correctif proposé
Prendre la step en échec **la plus profonde réellement exécutée** (dernière step KO de l'ordre
aplati), ou la feuille en échec :
```java
private static Optional<TestStep> failingStep(TestOutcome outcome) {
    return outcome.getFlattenedTestSteps().stream()
            .filter(TestFailureManager::isFailing)
            .reduce((first, last) -> last);   // dernière KO = la plus profonde (l'action fautive)
    // Variante équivalente, plus explicite :
    //   .filter(step -> !step.hasChildren())   // ne garder que les feuilles
    //   .reduce((first, last) -> last);
}
```
Bonus : `failingStepHtml` (qui réutilise `failingStep`) prendra alors la capture de la **feuille**
en échec, plus pertinente que celle du groupe.

---

## 3. `sanitize` détruit les accents au lieu de les translittérer

### Constat
`KO__RECETTE__Un_chec_de_connexion_est_captur_en_artefacts...` et
`V_rifier_que_la_connexion_a_r_ussi.html` : chaque caractère accentué (`é`, `è`, `à`, `ù`, `ç`…)
est remplacé par `_`, ce qui ampute les mots (`Vérifier` → `V_rifier`) et nuit à la lisibilité.

### Cause
`sanitize` ne considère « sûrs » que `[A-Za-z0-9.-]` ; tout le reste (donc les lettres accentuées)
devient `_`.

### Correctif proposé
**Translittérer** d'abord vers l'ASCII de base (décomposition Unicode NFD + suppression des
diacritiques), retirer les apostrophes/quotes, puis appliquer la logique `_` existante sur le
résidu :
```java
import java.text.Normalizer;

static String sanitize(String raw) {
    if (raw == null || raw.isBlank()) {
        return "unknown";
    }
    // 1) Translittération : é->e, à->a, ù->u, ç->c, ñ->n... (NFD puis suppression des marques).
    String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD)
                             .replaceAll("\\p{M}+", "");
    // 2) Apostrophes / guillemets : supprimés (collage). Ex. "l'écran" -> "lecran".
    ascii = ascii.replaceAll("['’‘`\"]", "");
    // 3) Résidu : caractères non sûrs -> '_', runs réduits, bords nettoyés (logique inchangée).
    String cleaned = trimUnderscores(collapseToUnderscores(ascii));
    return cleaned.isEmpty() ? "unknown" : cleaned;
}
```

### Résultat attendu
| Avant | Après |
|---|---|
| `V_rifier_que_la_connexion_a_r_ussi` | `Verifier_que_la_connexion_a_reussi` |
| `Un_chec_de_connexion_est_captur...` | `Un_echec_de_connexion_est_capture...` |
| `KO__RECETTE__...` | inchangé (déjà ASCII) |

S'applique partout où `sanitize` est utilisé : nom de dossier (`ENV`, nom du test) **et** nom du
fichier HTML.

---

## 4. `[OUT_OF_BOUNDS]` remonté à tort pour un élément `display:none`

### Constat
`DiagnosticTest#T7_hidden` (élément présent mais `display:none`) obtient, au timeout :
`Synchronisation impossible sur By.id: target après 1 s [HIDDEN] [OUT_OF_BOUNDS] : présent mais non
visible ; hors de la zone atteignable`. Seul `[HIDDEN]` est attendu : l'élément n'est pas « hors
zone », il n'a simplement pas de rendu. Le test ne vérifie que `[HIDDEN]`, il reste donc vert.

### Cause
`AbstractSyncManager.inViewport` s'appuie sur `getBoundingClientRect()`. Un élément `display:none` n'a
pas de boîte : le rectangle vaut `0,0,0,0`, donc `r.bottom>0` est faux et la sonde conclut « hors
viewport ». Or la Javadoc de la méthode vise « Doute → `true` (pas de faux positif) ».

À ne pas confondre avec le cas d'un élément **réellement** hors viewport : il est scrollé
automatiquement avant le clic, donc l'erreur est inatteignable en pratique (cf. T10 reclassé, T10bis).

### Correctif proposé
Ne pas juger la position d'un élément sans géométrie : soit ne sonder `OUT_OF_BOUNDS` que si l'élément
est affiché (comme `INTERCEPTED`, qui présuppose le reste OK), soit traiter le rectangle nul comme un
doute dans le script :
```java
"var r=arguments[0].getBoundingClientRect();"
+ "if (r.width===0 && r.height===0) return true;"   // pas de boîte : pas de jugement
+ "return r.bottom>0 && r.right>0 && ..."
```

### Résultat attendu
| Cas | Avant | Après |
|---|---|---|
| `display:none` | `[HIDDEN] [OUT_OF_BOUNDS]` | `[HIDDEN]` |
| visible, dans le viewport | aucun code de position | inchangé |

---

## 5. `count` / `getTexts` / `within` attendent l'implicit wait sur un élément absent

### Constat
`CollectionsTest#T43_absent_sans_exception` (2026-10-18, Chrome et Firefox) : `count(By.id("absent"))`
rend bien `0`, mais en **3 071 ms** au lieu d'être immédiat. La Javadoc annonce pourtant « état courant,
**sans attente** ». Les prédicats (`is*`, `contains*`), eux, rendent `false` en quelques ms (T14, T47).

### Cause
`AbstractSyncManager.findAll` (utilisé par `count` et `getTexts`) appelle
`resolveScope(driver).findElements(locator)` **hors** de `withoutImplicitWait`, contrairement à `query`.
Le driver porte l'implicit wait de `serenity.conf` (`webdriver.timeouts.implicitlywait = 3000`) :
`findElements` attend donc jusqu'à 3 s qu'au moins un élément apparaisse. Même effet sur
`within(absent).count(…)` : `resolveScope` fait un `findElement(container)` soumis à l'implicit wait
avant que l'absence soit rattrapée en liste vide. Sur des éléments **présents**, aucun impact (T42).

### Correctif proposé
Exécuter `findAll` sous la même neutralisation que les prédicats :
```java
private List<WebElement> findAll(By locator) {
    return withoutImplicitWait(() -> {
        try {
            return resolveScope(driver()).findElements(locator);
        } catch (NoSuchElementException | StaleElementReferenceException containerGone) {
            return List.of();
        }
    });
}
```

### Résultat attendu
| Appel sur absent | Avant | Après |
|---|---|---|
| `count`, `getTexts` | `0` / `[]` en ~3 s (implicit wait) | `0` / `[]` en quelques ms |
| `within(absent).count` | `0` en ~3 s | `0` en quelques ms |

---

## 6. `setWindowFocus(By)` ne résout jamais son locator : faux succès sur absent

### Constat
`FenetreTest#T76_set_window_focus_absent` (2026-10-18, Chrome et Firefox) :
`setWindowFocus(By.id("absent"))` **rend la main sans exception**. Attendu : attente du profil puis
`SyncException [ABSENT]`, comme toute méthode qui prend un `By`. T75 confirme par ailleurs que
`window.focus()` est bien exécuté sur un élément présent.

### Cause
`WebSync.setWindowFocus` passe par `fluentWait(locator, element -> element.setWindowFocus())`. Le
`WebElementFacade` résolu est **paresseux**, et `WebElementFacadeImpl.setWindowFocus` (bytecode Serenity
4.2.22) se limite à `executeScript("window.focus()")` : il ne touche **jamais** l'élément. Aucune
`NoSuchElementException` n'est levée, donc ni boucle ni diagnostic. Le paramètre `By` est ignoré. C'est
le « faux succès » que la Javadoc de `requireEnabled` juge « plus nuisible qu'un échec franc ».

### Correctif proposé
Au choix :
- **Garder la signature** et forcer la résolution avant le focus, pour bénéficier de l'attente et du
  diagnostic : par exemple résoudre l'élément brut (`resolveElement(locator)`) dans la boucle, puis
  exécuter `window.focus()`.
- **Ou assumer la sémantique réelle** (focus de la fenêtre, indépendant de l'élément) : exposer
  `setWindowFocus()` sans `By`, et déprécier la variante actuelle.

### Résultat attendu
| Cas | Avant | Après (1re option) |
|---|---|---|
| élément présent | `window.focus()` appelé | inchangé |
| élément absent | aucun effet visible, aucune exception | `SyncException [ABSENT]` au timeout, `window.focus()` non appelé |

---

## 7. Soft-assert : un `should*` en échec n'est pas signalé à Serenity

Note détaillée pour l'agent du socle : `qa-test/docs/note-agent-socle-soft-assert.md`.

### Constat
Sonde du 2026-10-18 (Chrome, Java 17) : 4 `should*` alternés (OK, KO, OK, KO), chacun dans une `@Step`, avec
`qa.assertions.soft=true`. Le test finit bien FAILED avec la liste des 2 échecs, **mais** dans le rapport
Serenity l'étape en échec « #cache visible » est **verte**, et l'échec agrégé est rattaché à la **dernière**
étape (rouge même si elle avait réussi). Aucun dump `{step}.html` n'est produit pour les échecs soft.

### Cause
`SoftAssertions.check` (mode soft) intercepte l'`AssertionError` et la range dans le collecteur sans rien dire
à Serenity : l'étape se termine normalement. L'agrégat levé par `SoftAssertionExtension` en fin de test est
ensuite imputé par Serenity à la dernière étape exécutée.

### Correctif attendu
Signaler chaque échec soft à Serenity sur l'étape en cours (rouge, capture, `{step}.html`), sans interrompre
l'exécution ni sauter les étapes suivantes, et sans imputer l'agrégat final à la dernière étape. Le mode soft
natif de Serenity (`StepEventBus.enableSoftAsserts()`) **ne convient pas** : mesuré, il fait disparaître les
échecs (test vert).

### Résultat attendu
| Cas (mode soft) | Avant | Après |
|---|---|---|
| étape dont le `should*` échoue | verte | **rouge**, avec capture |
| dernière étape, si elle a réussi | rouge (porte l'agrégat) | **verte** |
| verdict du test | FAILED, liste des échecs | inchangé |
| artefacts `KO__` | `ERROR.log`, `FAIL.log` | + un `{step}.html` par étape en échec |

### Suite (2026-10-19)
Corrigé par le socle : `81f66d5` (chaque échec soft dans une étape devient une sous-étape rouge, les étapes
suivantes ne sont ni sautées ni privées de WebDriver ; l'extension rattache l'échec final au test) et
`da57c34` (un dump `{step}.html` par step en échec). Vérifié par T82
(`qa-test/src/test/java/org/automation/websync/etat/soft/SoftAssertionsTest.java`, refondu à cette occasion
au lieu de créer le T83 prévu par la note), Chrome et Firefox : 13 étapes en échec **rouges**, 3 vertes dont la
dernière, aucune sautée, 13 `{step}.html`, test FAILED. Mode dur inchangé (T16, T34, T48 à T58 verts dans la
campagne du 2026-10-18, socle corrigé).

Deux constats en marge :
1. **Côté test, pas côté socle** : sous Serenity 4.2.22, un `assertThrows(AssertionError.class, …)` dans un test
   qui finit en échec sur `AssertionError` fait repeindre en vert, en fin de test, toutes les étapes en échec
   par ce type (`SerenityTestExecutionListener` → `BaseStepListener.exceptionExpected` →
   `TestOutcome.resetFailingStepsCausedBy`). T82 capture donc son agrégat par `try/catch`.
2. Le message final de Serenity annonce « The following 14 assertions failed » : les 13 échecs des étapes, puis
   l'agrégat du socle qui les reprend. Sans gravité, laissé en l'état.

---

## Tests à compléter côté socle
État constaté dans `qa-template/qa-socle/src/test/java/com/example/qa/` le 2026-10-19 (✅ présent, ⬜ à faire) :
- ✅ `sanitize("Vérifier qu'on a réussi")` → `Verifier_quon_a_reussi` (`failure/TestFailureArtefactsTest`).
- `folderName(...)` / nom de fichier : vérifier l'absence de `_` parasite en place d'accent.
- `failingStep(...)` avec une hiérarchie imbriquée (parent KO + feuille KO) → renvoie la **feuille**.
- Dump HTML : si DOM brut indisponible, repli documenté ; sinon, contenu = `<html>…` non échappé.
- ⬜ Diagnostic (package `sync`) : élément `display:none` → le message porte `[HIDDEN]` **sans**
  `[OUT_OF_BOUNDS]` (§4, ouvert).
- ✅ Collections (package `sync`) : `count` / `getTexts` sur absent, avec un implicit wait non nul sur le
  driver → résultat vide **sans** attendre l'implicit wait (§5, `sync/CollectionsTest`).
- ✅ `setWindowFocus(By)` sur absent → `SyncException [ABSENT]` au timeout, pas de retour silencieux (§6,
  `sync/SetWindowFocusTest`).
- ✅ Soft-assert : signalement à Serenity dans une étape, hors étape inchangé, mode dur inchangé (§7,
  `sync/SoftAssertionsTest`).
