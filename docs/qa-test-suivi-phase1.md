# Fichier de suivi — PHASE 1 (fixtures, bornes précises) — chapitre 3 du prompt

> Périmètre : validation navigateur du socle `qa-socle` via **fixtures HTML locales** (`file://`).
> Source : `docs/qa-test-prompt.md`, chapitre 3 (PHASE 1). Objectif : **aucun trou dans la raquette** —
> chaque test du document figure ici avec son statut. Un test non exécuté ou en échec est **notifié**.

## Périmètre exact des tests de la PHASE 1

Le chapitre 3 comporte **deux tableaux**. Les identifiants absents du document (T3, T4, T12, T13, T21)
ne sont **pas** des tests : ils n'existent pas dans les tableaux et ne sont donc pas inventés ici.
- Tableau « Tests aux limites » : **L1, L2, L3, L4, L5, L6** → 6 tests.
- Tableau « Comportements précis » : **T1, T2, T5, T6, T7, T8, T9, T10, T11, T14, T15, T16, T17, T18,
  T19, T20, T22, T23, T24** → 19 tests.
- **Total attendu PHASE 1 = 25 tests.**

## État des prérequis

| Prérequis | État | Détail |
|---|---|---|
| Socle publié dans `~/.m2` | ✅ Prêt | `qa-parent` + `qa-socle` 1.0.0-SNAPSHOT présents |
| POM (`qa-parent` parent, `qa-socle` sans version) | ✅ Prêt | conforme, inchangé |
| `serenity.conf` (environment=qatest, implicit 3s, soft=false, page-load normal, reporting off) | ✅ Prêt | aligné sur le prompt |
| Fixtures HTML (10) sous `src/test/resources/fixtures/` | ✅ Prêt | appear, disappear, rerender, hidden, disabled, overlay, offscreen, slowload, form, nested |
| Utilitaire `Fixtures.url(...)` (résolution `file://`) | ✅ Prêt | `org.automation.support.Fixtures` |
| Chrome / Firefox / Maven | ✅ Prêt | Chrome 150, Firefox 152, Maven 3.9.12 |
| Tests JUnit L1–L6 / T* | ✅ Écrits + exécutés (Chrome) | 25 tests couverts ; Firefox restant |

> ⚠️ Point d'attention environnement : JDK par défaut de `mvn` = Java 25 ; JDK 17 disponible
> (`ms-17.0.19`). Tous les runs ci-dessous ont été forcés en **Java 17** via `JAVA_HOME`
> (cible du socle). Compilation et exécution OK en Java 17.

## Tableau de résultats — Tests aux limites

| # | Mécanisme | Attendu | Statut | Chrome | Firefox | Écart / Note |
|---|---|---|---|---|---|---|
| L1 | implicit wait 3s — borne basse (`appear?delay=2800`, findElement brut) | trouvé (< 3 s) | ✅ OK | ✅ trouvé en **2856 ms** | ⬜ non exécuté | Implicit wait natif 3 s opérationnel sur appel Selenium brut. |
| L2 | implicit wait 3s — borne haute (`appear?delay=3200`, findElement brut) | `NoSuchElement` (> 3 s) | ✅ OK | ✅ `NoSuchElement` en **3056 ms** | ⬜ non exécuté | Borne haute de l'implicit wait respectée. |
| L3 | profil — borne basse (`appear?delay=T-200`, `WebSync.click`) | succès | ✅ OK | ✅ click en **1826 ms** | ⬜ non exécuté | Passe, mais voir **ÉCART-1** : la valeur du profil n'est pas déterminante (l'attente effective est le défaut Serenity ~5 s, pas `medium`). Le succès tient au fait que la cible (1800 ms) < ~5 s. |
| L4 | profil — borne haute (`appear?delay=T+200`, `WebSync.click`) | `SyncException [ABSENT]` | ❌ KO | ❌ **aucune exception** (le click réussit) | ⬜ non exécuté | **ÉCART-1** : `-Dqa.sync.timeout.medium=2s` ignoré → attente effective ~5 s > cible 2200 ms → pas de timeout, pas de `SyncException`. Profils `SyncTimeout` non appliqués. |
| L5 | répéter L3/L4 pour SHORT/MEDIUM/LONG/EXTRA_LONG (dont vraies valeurs MEDIUM) | bornes respectées | ❌ KO (partiel) | LONG borne basse ✅ (click 2576 ms) ; SHORT borne haute ❌ (aucune exception) ; MEDIUM vraies valeurs (14 s) ❌ `ElementShouldBeEnabledException` à **~4 s** | ⬜ non exécuté | **ÉCART-1** : SHORT=1s et MEDIUM=15s non appliqués. L'attente plafonne à ~5 s (défaut Serenity) → SHORT ne timeoute pas (cible 1500 ms), MEDIUM abandonne à ~4 s au lieu d'attendre 14 s. EXTRA_LONG non testé isolément (même moteur). |
| L6 | polling fin sous WebSync malgré implicit 3s | WebSync tôt (~250ms) vs brut ~3s | ✅ OK | ✅ WebSync **2615 ms** vs brut **2547 ms** (assertion `msWeb ≤ msRaw+500` tenue) | ⬜ non exécuté | Les deux attentes ne s'additionnent pas ; le toggle d'implicit wait fonctionne. Cible à 2500 ms → l'avantage du polling fin n'est pas spectaculaire ici (les deux rendent la main vers ~2,5 s). |

## Tableau de résultats — Comportements précis

| # | Quoi | Attendu | Statut | Chrome | Firefox | Écart / Note |
|---|---|---|---|---|---|---|
| T1 | `fluentWait` attend un élément tardif | action réussit | ✅ OK | ✅ click sur cible à 1500 ms | ⬜ non exécuté | Attente d'un élément tardif honorée. |
| T2 | jamais présent (locator bidon) | `SyncException [ABSENT]` | ❌ KO | ❌ `ElementShouldBeEnabledException` (pas `SyncException`) | ⬜ non exécuté | **ÉCART-2** : sur locator absent, l'exception Serenity brute remonte ; pas de `SyncException [ABSENT]`. Le code `[ABSENT]` n'est pas produit. |
| T5 | stale/re-render absorbé (`rerender`) | ré-résolution transparente | ✅ OK | ✅ lectures v0→v2 sans `StaleElement` | ⬜ non exécuté | Ré-résolution du `By` transparente, transitoires absorbés. |
| T6 | `pageLoadTimeout` posé auto (borne, `slowload?ms=8000`) | échec borné ~5 s | ❌ KO | ❌ navigation **8714 ms** (blocage complet), pas de timeout | ⬜ non exécuté | **ÉCART-3** : `-Dqa.sync.pageLoadTimeout=5s` non honoré ; la navigation n'est pas bornée (attend les 8 s complets). |
| T7 | `HIDDEN` (`hidden`) | `[HIDDEN]` | ✅ OK | ✅ `SyncException [HIDDEN]` | ⬜ non exécuté | Diagnostic actif opérationnel pour HIDDEN. |
| T8 | `DISABLED` (`disabled`) | `[DISABLED]` | ❌ KO | ❌ `ElementShouldBeEnabledException` (pas `SyncException`) | ⬜ non exécuté | **ÉCART-2** : élément visible mais `disabled` → exception Serenity brute, pas `SyncException [DISABLED]`. |
| T9 | `INTERCEPTED` (`overlay`) | `[INTERCEPTED]` | ✅ OK | ✅ `SyncException [INTERCEPTED]` | ⬜ non exécuté | Diagnostic actif opérationnel pour INTERCEPTED (hit-test). |
| T10 | `OUT_OF_BOUNDS` (`offscreen`) | `[OUT_OF_BOUNDS]` | ❌ KO | ❌ **aucune exception** (le click aboutit) | ⬜ non exécuté | **ÉCART-4** : élément à `top:5000px` → Selenium scrolle et clique ; le code `[OUT_OF_BOUNDS]` n'est jamais déclenché. |
| T11 | `within` container vs child absent (`nested`) | `cause` = bon maillon | ✅ OK | ✅ succès bon container/child | ⬜ non exécuté | Résolution deux-temps `within` fonctionnelle. |
| T14 | `is*`/`contains*` : absent → `false`, instantané | `false` immédiat (pas de 3 s) | ❌ KO | ❌ `containsText` fuit `NoSuchElement` sur absent | ⬜ non exécuté | **ÉCART-5** : `isVisible/isPresent/isEnabled` OK (false), mais `containsText(By, …)` sur locator absent **lève** `NoSuchElement` au lieu de rendre `false`. Contrat « ne lève jamais » violé pour `containsText`. |
| T15 | `waitUntil*` positifs/négatifs (`appear`/`disappear`) | attentes honorées | ✅ OK | ✅ `waitUntilVisible` puis `waitUntilNotVisible` honorés | ⬜ non exécuté | Attentes explicites positives et négatives OK. |
| T16 | `should*` dur (`soft=false`, `form`) | échoue à la 1re | ❌ KO | ❌ `AssertionError` (pas `SyncException` avec code) | ⬜ non exécuté | **ÉCART-6** : `shouldBeVisible` sur absent lève une `AssertionError` Serenity sans code diagnostic `[ABSENT]`/`[HIDDEN]`. L'échec « dur » à la 1re assertion a bien lieu, mais pas sous forme de `SyncException` diagnostiquée. |
| T17 | `should*` soft (`soft=true`) | agrégé, échec en fin (extension auto) | ✅ OK | ✅ 2 `should*` KO **collectés**, échec agrégé **en fin** | ⬜ non exécuté | Mode soft opérationnel : pas d'arrêt à la 1re, agrégation « 2 assertion(s) souple(s) en échec » via extension auto-détectée (aucune annotation côté test). |
| T18 | KO → dossier `KO__…/` + 3 fichiers | dossier + ERROR.log + FAIL.log + {step}.html | ✅ OK | ✅ dossier `KO__QATEST__…` + ERROR.log + FAIL.log + `{step}.html` (via `@Steps`) | ⬜ non exécuté | Validé par `RawDomCaptureProducer/Verifier` (site réel, PageObjects `@Step`). **Note** : le producteur phase-1 `ArtefactsProducerTest` appelle `WebSync` **hors `@Step`** → seul `TestFailureManager` s'active (ERROR.log + FAIL.log) mais **pas** de `{step}.html` (dump lié au `stepFailed`, conforme doc). |
| T19 | dump = DOM réel à l'échec | page source du moment | ✅ OK | ✅ vrai DOM `<html>` de la page à l'échec (pas la vue Prism échappée) | ⬜ non exécuté | Validé via `RawDomCaptureVerifier` (4 tests verts). |
| T20 | masquage `input[type=password]` (zéro clair, `form`) | `value` blanchie dans le `.html` | ✅ OK | ✅ mot de passe clair **absent** du dump (0 occurrence), `value` blanchie | ⬜ non exécuté | Validé via `RawDomCaptureVerifier` (login `Secret.of("mauvais-mot-de-passe")` → 0 occurrence en clair). |
| T22 | `type(By, Secret)` (`form`) | saisie OK + masquage | ✅ OK | ✅ `value` DOM = `SuperSecretPassword!` (saisie effective) | ⬜ non exécuté | Saisie via `Secret` fonctionnelle ; masquage à l'échec couvert par T20. |
| T23 | 3 services actifs par présence du jar | listeners actifs (aucun `@ExtendWith` custom) | ✅ OK | ✅ 3 services actifs sans `@ExtendWith` custom | ⬜ non exécuté | `TestFailureManager` (dossiers/logs) + `RawDomCaptureListener` (dump DOM au `stepFailed`) + `SoftAssertionExtension` (agrégation T17) confirmés par simple présence du jar. |
| T24 | précédence `-D` > `serenity.conf` | `-D` gagne | ✅ OK | ✅ `environment` résolu = `override-D` (conf = `qatest`) | ⬜ non exécuté | Précédence `-D` > `serenity.conf` confirmée. |

## Légende statut

- ⬜ non exécuté
- ✅ OK (attendu observé)
- ❌ KO (attendu non observé — à notifier + écart)
- ⚠️ partiel / à confirmer

## Synthèse Chrome (2026-07-15)

- **25 tests couverts. Chrome : 17 ✅ / 8 ❌.** Firefox : ⬜ non encore exécuté (les 8 écarts sont
  de niveau **socle**, donc attendus identiques en Firefox ; à confirmer par un run).
- ✅ (17) : L1, L2, L3, L5(LONG), L6, T1, T5, T7, T9, T11, T15, T17, T18, T19, T20, T22, T23, T24.
  *(L3 et L5-LONG passent, mais sans valider réellement la borne du profil — cf. ÉCART-1.)*
- ❌ (8) : **L4, L5(SHORT+MEDIUM), T2, T6, T8, T10, T14, T16**.

> Les parties **réellement implémentées** du socle (capture d'artefacts `KO__`, dump DOM au `stepFailed`,
> masquage `password`, soft-assert, activation par ServiceLoader, diagnostic HIDDEN/INTERCEPTED, `within`,
> stale-absorb, waits explicites, toggle implicit-wait) **fonctionnent**. Les KO se concentrent sur le
> **moteur de timeout par profils** et sur **une partie du diagnostic** (ABSENT/DISABLED/OUT_OF_BOUNDS) et
> des `should*`.

## Écarts socle à signaler (backlog socle — ne pas corriger côté test)

> Conformément au chapitre 5 du prompt (« le code du socle fait foi ; toute divergence doc/comportement
> est un écart à signaler »). Cause racine cohérente avec l'état connu du socle (moteur SYNC en partie
> squelette ; capture d'échec + masquage réels).

- **ÉCART-1 — Profils `SyncTimeout` non appliqués (impacte L3, L4, L5).** Les valeurs `SHORT/MEDIUM/LONG/
  EXTRA_LONG` (défauts 5/15/60/120 s) et surtout les overrides `-Dqa.sync.timeout.<profil>` **n'ont aucun
  effet** sur l'attente réelle de `WebSync`. L'attente effective plafonne au défaut Serenity (~5 s).
  Preuves : `L3` avec `-Dqa.sync.timeout.medium=1s` attend quand même 1852 ms (cible 1800 ms) ; `L4`
  (cible 2200 ms, `medium=2s`) réussit au lieu de timeouter ; `L5-MEDIUM` (cible 14 s) abandonne à ~4 s.
  Le mécanisme `-D` lui-même fonctionne (T24 ✅) → c'est bien le socle qui n'exploite pas la clé.
- **ÉCART-2 — Diagnostic ABSENT/DISABLED non produit (T2, T8).** Sur élément **absent** ou **disabled**,
  l'exception Serenity brute (`ElementShouldBeEnabledException`) remonte au lieu d'une `SyncException`
  portant `[ABSENT]` / `[DISABLED]`. (HIDDEN et INTERCEPTED, eux, sont bien diagnostiqués — T7/T9 ✅.)
- **ÉCART-3 — `pageLoadTimeout` non borné (T6).** `-Dqa.sync.pageLoadTimeout=5s` n'est pas honoré ;
  `slowload?ms=8000` bloque 8714 ms sans échec de navigation borné.
- **ÉCART-4 — `OUT_OF_BOUNDS` non détecté (T10).** Élément hors viewport → Selenium scrolle et clique ;
  aucun `SyncException [OUT_OF_BOUNDS]`.
- **ÉCART-5 — `containsText` viole le contrat « ne lève jamais » (T14).** Sur locator absent,
  `containsText(By, …)` propage `NoSuchElement` au lieu de rendre `false`. `isVisible/isPresent/isEnabled`
  respectent le contrat.
- **ÉCART-6 — `should*` sans code diagnostic (T16).** `shouldBeVisible` sur absent lève une `AssertionError`
  Serenity, pas une `SyncException` avec code `[ABSENT]`/`[HIDDEN]`. L'échec « dur » a lieu, mais non
  diagnostiqué.

## Journal d'exécution

**Environnement.** `JAVA_HOME` forcé sur Java 17 (`ms-17.0.19`) ; Maven 3.9.12 ; Chrome 150 /
chromedriver 150 (Selenium Manager) ; socle `qa-socle 1.0.0-SNAPSHOT` (jar reconstruit le 2026-07-15 02:05).
Tous les runs : `-Dmaven.test.failure.ignore=true` pour collecter tous les résultats.

**Runs Chrome (2026-07-15, ~03:24–03:45).** Groupés par valeurs de profil compatibles (contrainte :
L4 exige `medium=2s`, L5-MEDIUM exige `medium=15s` réel → runs séparés) :

- **A** — `BornesLimitesTest` (sauf L5-MEDIUM) `-Dqa.sync.timeout.medium=2s -Dqa.sync.timeout.short=1s
  -Dqa.sync.timeout.long=10s` → 13 tests, 3 échecs (L4, L5-SHORT, T2) + 1 erreur (T14). L1=2856 ms,
  L2=3056 ms, L3=1826 ms, L5-LONG=2576 ms, L6=2615/2547 ms.
- **B** — `BornesLimitesTest#L5_medium_vraies_valeurs` (aucun override, `medium`=15 s réel) →
  1 erreur `ElementShouldBeEnabledException` à ~4 s (au lieu d'attendre 14 s).
- **C** — `DiagnosticTest` `-Dqa.sync.timeout.short=1s` → 4 tests, 2 échecs : T8 (DISABLED),
  T10 (OUT_OF_BOUNDS). T7 (HIDDEN) et T9 (INTERCEPTED) ✅.
- **D** — `FormEtSecretTest` `-Dqa.sync.timeout.short=1s` → 3 tests, 1 échec : T16. Surface (type/select/
  checkbox/submit + lectures) ✅, T22 (`Secret`) ✅.
- **E** — `PageLoadEtConfigTest` `-Dqa.sync.pageLoadTimeout=5s -Denvironment=override-D` → 2 tests,
  1 échec : T6 (nav 8714 ms). T24 ✅ (`environment=override-D`).
- **F/G** — `ArtefactsProducerTest` puis `ArtefactsVerificationTest`
  (`-Djunit.jupiter.conditions.deactivate=*DisabledCondition`) : producteur KO volontaire → dossier
  `KO__QATEST__…` avec ERROR.log + FAIL.log **mais sans `{step}.html`** (échec hors `@Step`) → vérificateur
  KO (« dump {step}.html manquant »). ⇒ T18/T19/T20 revalidés par les tests `@Steps` ci-dessous.
- **H** — `SoftAssertTest` `-Dqa.assertions.soft=true` + deactivate → T17 ✅ : 2 `should*` collectés,
  échec agrégé « 2 assertion(s) souple(s) en échec » en fin.
- **RawDomCapture** (site réel, PageObjects `@Step`) — `RawDomCaptureProducerTest` (2 KO volontaires
  LOGIN/CHECKBOXES) puis `RawDomCaptureVerificationTest` (**4 ✅**) : dossiers avec `{step}.html`, DOM réel
  `<html>` (T19), mot de passe clair `mauvais-mot-de-passe` **absent** du dump = masquage (T20), 3 services
  actifs (T23). ⇒ **T18/T19/T20/T23 validés**.

**Reste à faire.** Passe **Firefox** (`-Dwebdriver.driver=firefox`) sur l'ensemble (mêmes groupes/`-D`) pour
remplir la colonne Firefox ; puis PHASE 2 (sites réels) et PHASE 3 (parallélisme) selon le prompt.

