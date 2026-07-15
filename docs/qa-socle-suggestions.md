# Suggestions pour `qa-socle` — capture d'échec (`TestFailureManager`)

> Cible : dépôt **`ArnaudS-A5/qa-template`**, branche **`develop`**, fichier
> `qa-socle/src/main/java/com/example/qa/internal/failure/TestFailureManager.java`.
>
> Observées en consommant le socle depuis le projet `qa-test` (un test KO produit
> `target/qa-results/KO__{ENV}__{Test}__{ts}/` avec `ERROR.log`, `FAIL.log`, `{step}.html`).
> Trois corrections, indépendantes et toutes côté socle.

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

## Tests à compléter côté socle (`TestFailureArtefactsTest`)
- `sanitize("Vérifier qu'on a réussi")` → `Verifier_quon_a_reussi` (accents translittérés, apostrophe ôtée).
- `folderName(...)` / nom de fichier : vérifier l'absence de `_` parasite en place d'accent.
- `failingStep(...)` avec une hiérarchie imbriquée (parent KO + feuille KO) → renvoie la **feuille**.
- Dump HTML : si DOM brut indisponible, repli documenté ; sinon, contenu = `<html>…` non échappé.
