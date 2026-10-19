# Pipeline Jenkins QA

Job : `http://grid.home.arpa:8080/job/qa-ci-develop/`.

Le job récupère `qa-template` et `qa-test` depuis GitHub, sur `develop` par défaut.
Le `Jenkinsfile` et son exécuteur Python sont versionnés dans ce dépôt. Maven est
toujours lancé avec le Wrapper 3.9.9 du socle, y compris pour le projet consommateur.

## Étapes

1. Récupération des deux sources et enregistrement de leurs commits dans les rapports.
2. Sélection de `jdk17` par défaut (`jdk21` et `jdk25` sont aussi proposés).
3. Compilation, tests du socle, couverture JaCoCo et installation Maven dans un dépôt
   local propre au workspace. Les accès WebDriver des listeners Serenity utilisent le Grid.
4. Campagne navigateur normale en cinq invocations, avec les paramètres distincts
   documentés dans le README : fixtures, vrai timeout MEDIUM, précédence `-D`,
   site de démonstration et throttling. Aucun test d'échec volontaire `@Disabled`
   n'est réactivé. Les rapports JUnit de chaque invocation sont conservés séparément.
5. Rapport JaCoCo du socle combinant ses tests et les appels depuis le harnais.
6. Analyse des deux projets SonarQube existants. Le scanner utilise JDK 21 ; cela
   ne change pas le JDK de compilation ni la cible Java 17.
7. Attente du traitement SonarQube et vérification des résultats exacts des deux analyses.
   La gate `CI - Couverture globale 80%` ajoute une couverture globale minimale de 80 %
   et une duplication globale maximale de 3 % aux conditions de Sonar way. L'exception
   des petits changements est désactivée pour ces deux projets. Le socle doit avoir
   une condition de couverture effectivement évaluée. Le harnais, sans source de
   production, garde une couverture non applicable.
8. Publication Maven des deux projets uniquement après succès des tests et des gates.
   `qa-test` publie aussi le classifier `tests`, contenant le harnais et ses ressources.

Les analyses sont conservées même si la campagne navigateur échoue : le build
reste rouge et la publication est alors interdite. Une gate rouge fait également
échouer le pipeline et empêche la publication. Le pipeline ne
modifie ni les seuils ni les exclusions pour obtenir artificiellement un résultat vert.

## Paramètres

| Paramètre | Défaut |
|---|---|
| `TEMPLATE_BRANCH`, `TEST_BRANCH` | `develop` |
| `JDK_VERSION` | `17` ; choix `21`, `25` |
| `BROWSER` | `chrome` ; choix `firefox`, `edge` |
| `BASE_URL` | `https://the-internet.herokuapp.com` |
| `PUBLISH_ARTIFACTS` | `true` |
| `ALLOW_RELEASE` | `false` |

Le déclenchement est manuel. Un seul build peut tourner à la fois, sur l'agent
`grid.home.arpa`. Le timeout est de 30 minutes ; 20 builds et les artefacts des
10 derniers sont conservés. Les releases sont refusées tant que `ALLOW_RELEASE`
n'est pas explicitement activé. Le Maven Deploy Plugin choisit le dépôt correspondant
à la version : snapshots ou releases. Aucun changement de version n'est automatisé.

## Services et secrets

- Grid : `http://grid.home.arpa:4444` (URL WebDriver, sans `/ui/`).
- SonarQube : `http://sonarqube.home.arpa:9000` (sans `/projects`).
- Maven snapshots : `http://artifactory.home.arpa:8082/artifactory/libs-snapshot-local`.
- Maven releases : `http://artifactory.home.arpa:8082/artifactory/libs-release-local`.

Credentials Jenkins nécessaires :

- `sonarqube-ci` : secret text, token autorisant l'analyse et la lecture des gates des deux projets ;
- `artifactory-ci` : username/password, utilisateur Artifactory et token comme password.

Le `settings.xml` Maven est temporaire, hors des sources et des artefacts archivés,
avec des permissions 0600 ; il est supprimé après chaque invocation. Aucun token
n'est placé dans les POM, les URLs GitHub ou les arguments Maven.

Les snapshots sont horodatés. Le dépôt Artifactory doit conserver
`snapshotVersionBehavior=UNIQUE` et `maxUniqueSnapshots=0` pour garder l'historique
sans limite automatique. La publication d'une release suit le dépôt releases et
les règles de versionnement de l'équipe.

## Fixtures distantes

Les navigateurs Grid n'ont pas accès aux fichiers `file://` du workspace Jenkins.
Le runner sert les fixtures pendant la campagne sur `192.168.1.50:8765`, puis arrête
le serveur. Le pare-feu Debian autorise ce port uniquement depuis le réseau de
sortie des conteneurs Grid (`docker_gwbridge`, `172.18.0.0/16`). Le port n'est pas
publié sur Internet. En local, sans `qa.fixtures.baseUrl`, le fonctionnement
`file://` existant est conservé.

Le test de throttling conserve son hypothèse existante `HasNetworkConditions` : il
peut être ignoré quand le driver distant n'expose pas cette capacité Chromium.

## Mise en service avant fusion des PR

Le job charge sa définition depuis `ci/jenkins-grid-pipeline`, tandis que les deux
branches de sources restent `develop`. Les patches de ce dossier appliquent au
workspace les seules adaptations CI proposées dans les PR (POM et URL des fixtures).
Ils ne sont appliqués que si nécessaire ; après leur fusion dans `develop`, ils
sont reconnus comme déjà présents. Une divergence incompatible fait échouer le
build explicitement. Les commits et empreintes des patches sont archivés dans
`reports/source-revisions.json` pour identifier exactement le code analysé.
