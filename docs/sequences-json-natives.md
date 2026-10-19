# Séquences nativement JSON : conception et maquette

Statut : note de conception et maquette locale. Moteur : branche `perf/json-native-steps` partie de
`perf/engine-quick-wins` (652f0c40c étapes, 9f07004e8 vue XML à la demande). lib_BaseRow : branche
`perf/aggregate-plugin` (ef69576 `formssource_GetTableDataNative`, acd0836 `formssource_GetTableDataMixed`). Rien
n'est poussé.

## 1. Pourquoi

Le travail d'optimisation de lib_BaseRow (connecteur Baserow de C8oForms) a ramené le temps à -64 % et la mémoire
allouée à -65 % par rapport à la production : raw_json, sortie JSON native du moteur, lean_cells, plugin
d'agrégation Baserow, correctifs du moteur. Dans le dernier profil d'allocation (lib perf/memory, moteur perf), les
conversions XML ne pèsent plus que 3 %. Ce qui reste :

| Poste | Part des allocations |
|---|---|
| JavaScript de la lib (ComputeData ~28 % dont formatBaserowCell 14 %, JSON.parse 8 %) | ~40 % |
| Coût fixe par requête du moteur (classloader, contexte, clone, scope JS, include) | ~15 % |
| Sérialisation JSON de la réponse | ~14 % |
| Appel HTTP vers Baserow | ~13 % |
| Tomcat et filtres | ~11 % |

Le JavaScript métier traite les lignes une par une, avec des fonctions définies à chaque requête (`include` de
utils.js et getFilter.js, ~800 lignes de fonctions dans ComputeData). Une séquence dont les données restent du JSON
et dont les traitements sont des étapes natives du Studio remplacerait ce code par des éléments déclaratifs, plus
lisibles, testables, et plus rapides **à condition que chaque étape traite un tableau entier** : une étape par
cellule coûterait plus cher que le JavaScript (copie de l'étape, évaluation des expressions, événements à chaque
exécution).

## 2. Modèle d'exécution

### Ce qui existe déjà (branche perf/engine-quick-wins)

- `Sequence.jsonNativeOutput` (f9537d672) : une étape JSON to XML en mode JS placée directement sous la séquence garde
  son JSON en texte pour la réponse `.json`, sans construire son XML. Repli en XML si une clé est en double, pour une
  erreur, ou si le projet ne sort pas du JSON typé (`useType`, `docChildNodes`). Pas de cache, pas de source XPath
  possible sur ces sorties.
- 8f1398972 : pas de clé `text` vide quand toutes les sorties sont natives.
- 1deae91b8 : `RhinoUtils.jsonStringifyData` écrit les données JavaScript (objets, tableaux, textes, nombres) dans un
  seul tampon, et la réponse ajoute ce JSON sans le recopier dans un JSONObject.
- Maquette (652f0c40c) : `writeJsonData` écrit aussi les `Map` et `List` Java, ce qui permet aux étapes natives de
  produire des données que la sortie écrit sans conversion.

### Cible

| | Séquence XML (aujourd'hui) | Séquence JSON native |
|---|---|---|
| Entrées | variables texte + document XML d'entrée construit à chaque requête | objet JSON `input` (types JSON, tableaux, objets) ; document XML seulement si une étape XML le lit |
| Données entre étapes | DOM XML des étapes + variables Rhino | arbre JSON Java (`Map`, `List`, `String`, `Number`, `Boolean`, `null`), lu par les étapes natives sans conversion ; vu du JavaScript à travers une enveloppe |
| Transactions | document XML (ou texte brut `_raw` + `JSON.parse` en JS, comme raw_json) | la transaction HTTP rend l'arbre JSON de la réponse (analyse en flux, une seule fois) |
| Sous-séquences | requête interne : nouveau contexte, document XML rendu, copie | appel direct avec un objet JSON en entrée, objet JSON rendu, même fil |
| Sortie | XML → JSON (XmlToJson) ou jsonNativeOutput | l'étape de sortie écrit l'arbre en flux dans la réponse ; XML seulement pour un appel `.xml`/SOAP (conversion à la fin, `JsonNativeOutput.toElement` existe) |

Règle d'or : **une étape = une exécution par tableau**. Ses propriétés sont des expressions JavaScript évaluées une
seule fois par exécution (la source, les colonnes, les options) ; le travail par ligne est du Java. Une expression
par ligne (« colonne calculée ») reste possible mais doit être signalée comme le chemin lent.

## 3. Tous les beans existants dans une séquence JSON

Exigence : une séquence JSON native accepte **tous** les beans actuels (étapes et requestables XML), pas seulement
les nouvelles étapes JSON. Le principe qui garde le gain : **le DOM n'est construit que pour l'étape XML qui en a
besoin, au moment où elle le demande**. Une séquence faite seulement d'étapes JSON n'en construit jamais.

### Le point d'entrée existe

Toutes les sources des étapes (aujourd'hui `StepSource` = priorité de l'étape + XPath) passent par
`Step.getContextValues(xpath, loop)` → `Step.getContextNode(loop)` → `Sequence.findStepElement(executeTimeID)`.
Une étape JSON n'a pas d'élément dans le DOM de travail ; elle redéfinit `getContextNode` et rend une **vue XML de sa
valeur**, construite à la première lecture par les règles de JSON to XML (`JsonNativeOutput.toElement` : types,
`item` pour les tableaux, `originalKeyName`), puis gardée pour les lectures suivantes de la même exécution. C'est ce
que fait la maquette (`JsonTransformStep.getContextNode`) : aucune vue tant que personne ne lit l'étape par XPath.

### Inventaire par famille

« Tel quel » : le bean n'a pas de source XPath ni de sortie XML utile, il marche dans une séquence JSON sans
changement. « Adaptation simple » : il a un équivalent JSON direct (même résultat, sans DOM), à écrire une fois dans
le moteur. « Pont » : il reste XML et reçoit une vue XML à la demande ; sa sortie XML est convertie à la fin.

| Famille | Beans | Dans une séquence JSON |
|---|---|---|
| Contrôle (conditions JS) | IfStep, IfThenElseStep, WhileStep, DoWhileStep, SimpleIteratorStep, ReturnStep, BreakStep, FunctionStep, SerialStep, ParallelStep | tel quel (expressions JS sur le scope, qui voit les valeurs JSON par l'enveloppe) |
| Contrôle sur source XPath | IfExistStep, IfExistThenElseStep, IsInStep, IsInThenElseStep, IteratorStep, IfFileExistStep* | pont ; adaptation simple avec une source en chemin JSON (existe = chemin non vide ; IteratorStep itère un tableau JSON sans DOM) |
| JavaScript | SimpleStep, ExceptionStep | tel quel ; un script qui manipule `dom` (le document de sortie) force la construction du DOM complet |
| JavaScript sur source | SimpleSourceStep, JsonSourceStep | pont ; sur une étape JSON, simple référence à la valeur avec un chemin JSON (pas de conversion) |
| JSON | JsonToXmlStep | adaptation simple : en séquence JSON il pose la valeur dans la sortie (c'est déjà `jsonNativeOutput`) |
| JSON typé | JsonObjectStep, JsonArrayStep, JsonFieldStep | adaptation simple : construire les `Map`/`List` qui donnent le même JSON que leur XML typé |
| XML structurant | XMLElementStep, ElementStep, XMLAttributeStep, AttributeStep, XMLComplexStep | pont : ils produisent des nœuds, convertis à la sortie ; attributs = clés `attr` comme aujourd'hui |
| XML de traitement | XMLCopyStep, XMLConcatStep, XMLCountStep, XMLSortStep, XMLSplitStep, XMLDateTimeStep, XMLGenerateDatesStep | pont ; Copier, Concaténer, Compter, Trier, Découper ont des équivalents JSON ensemblistes naturels (candidats au jeu d'étapes) |
| XSLT | XMLTransformStep | pont obligatoire (la vue XML est l'entrée de la feuille) |
| Erreur | XMLErrorStep | adaptation simple : même objet `error` que la conversion actuelle |
| Appels | TransactionStep, SequenceStep (+ variables d'étape sourcées) | pont pour un requestable XML (document rendu converti à la demande) ; objet JSON direct pour une séquence JSON native, une transaction HTTP en `_raw`/JSON, CouchDB/FullSync ; les variables d'étape acceptent un chemin JSON |
| Fichiers | ReadJSONStep, ReadCSVStep | adaptation simple : la valeur JSON / les lignes directement (ReadJSON fabrique aujourd'hui du XML à partir de JSON) |
| Fichiers | ReadXMLStep, WriteXMLStep | pont |
| Fichiers | WriteJSONStep, WriteCSVStep, WriteFileStep, WriteBase64Step | pont, ou chemin JSON pour la source |
| Fichiers | CopyStep, DuplicateStep, MoveStep, RenameStep, DeleteStep, CreateDirectoryStep | tel quel |
| Fichiers | ListDirStep | pont (ou liste JSON en adaptation simple) |
| Session, HTTP | SessionGet/Set/Remove(Object)Step, Get/Set/RemoveAuthenticatedUserStep, LDAPAuthenticationStep, GetRequestHeaderStep, SetResponseHeaderStep, SetResponseStatusStep | tel quel (SmartType JS) ; ceux qui produisent un nœud (SessionGet, GetRequestHeader) : pont ou valeur JSON en adaptation simple |
| Autres | InputVariablesStep | adaptation simple : les entrées en objet JSON |
| Autres | SmtpStep, PdfFormStep, PushNotificationStep | pont pour leurs sources XPath |
| Autres | SleepStep, RemoveContextStep, RemoveSessionStep, ProcessExecStep, LogStep, GenerateHashCodeStep | tel quel |
| Requestables | transactions HTTP/JsonHttp/XmlHttp, SQL, SAP, CICS, Javelin, SiteClipper, CouchDB/FullSync | appelés depuis une séquence JSON : document XML converti à la demande (même conversion que la réponse `.json`), sauf HTTP `_raw`/JSON et CouchDB qui rendent l'objet |

\* IfFileExistStep n'a qu'un chemin de fichier en expression : tel quel.

Toute propriété SmartType en mode SOURCE est une source comme les autres : même pont.

### Sources : XPath et chemin JSON

- Nouvelle forme de source : (priorité, chemin) où un chemin qui commence par `$` est un **chemin JSON** évalué sur
  la valeur de l'étape, sans DOM. Une source XPath existante reste valide : elle lit la vue XML.
- Traduction des XPath simples (le cas courant des séquences) : `.` → `$` ; `./a/b` → `$.a.b` ; `./a/item` → `$.a[*]` ;
  `./a/item[2]` → `$.a[1]` ; `./a/text()` → `$.a` ; `//x` → `$..x`. Restent en XPath (donc en pont) : axes, fonctions,
  prédicats sur valeurs, attributs (`@type`, `@originalKeyName` n'existent pas en JSON), XPath absolus sur
  `//document` qui mélangent les sorties de plusieurs étapes.
- Le Studio propose la traduction quand elle est exacte (vérifiée en exécutant les deux sur le test case) et marque
  les sources restées en XPath.

### Sortie : nœuds XML et objets JSON mêlés

Aujourd'hui le JSON d'une séquence est `XmlToJson(document)` (types, éléments répétés en tableau, `attr`, `text`), et
`jsonNativeOutput` y ajoute les valeurs gardées en JSON, avec repli en XML quand une clé est déjà dans le XML. Cible :
la sortie est une liste ordonnée d'entrées (clé, nœud XML | valeur JSON). L'écriture convertit chaque nœud XML par
les **mêmes règles** qu'aujourd'hui et écrit les valeurs JSON telles quelles ; deux entrées de même clé, quelle que
soit leur nature, deviennent un tableau comme deux éléments frères le deviennent dans `XmlToJson`. Le JSON obtenu
est celui d'aujourd'hui par construction ; le repli en XML n'est plus nécessaire.

### Migration d'une séquence existante

- Passer une séquence XML en mode JSON **sans la réécrire** est possible : chaque étape XML garde son comportement
  par le pont, la sortie suit les mêmes règles de conversion, donc même JSON. Le gain n'arrive qu'étape par étape,
  quand une étape XML est remplacée par son équivalent JSON ou qu'une source XPath devient un chemin JSON.
- Cas qui imposent le DOM complet (le mode JSON les garde en XML, la séquence le signale) : un script qui lit ou
  modifie `dom`, une source XPath absolue sur `//document` qui traverse les sorties de plusieurs étapes, un
  XMLCopyStep qui copie la sortie d'étapes précédentes, des espaces de noms XML.
- Le Studio affiche sur chaque étape un badge « pont XML » (vue construite pour elle) ou « DOM complet », et sur la
  séquence « construit un DOM : non / pour N étapes / complet ». Une action « Vérifier le mode JSON » exécute les
  test cases dans les deux modes et compare les JSON (comme `jsonsteps_dump.py` sur le banc).

## 4. Jeu minimal d'étapes ensemblistes

| Étape | Entrée → sortie | Propriétés | Maquette |
|---|---|---|---|
| **Lire par chemin JSON** | texte JSON, réponse de transaction ou variable → valeur | source, chemin (`results`, `a.b[0]`, `items[*].id`), valeur par défaut | non (le `JSON.parse` de ParseRawFirstPage reste en JS) |
| **Sélectionner les colonnes** (transformer les colonnes) | lignes → lignes | colonnes en tête, colonnes gardées (ordre), ordre de repli + colonnes restantes | **oui** `JsonSelectColumnsStep` |
| **Formater selon le type de colonne** | lignes de valeurs → lignes de cellules | schéma (champs), colonnes exposées, colonnes masquées, valeurs seules, fuseau par défaut, format nommé, variable des options de colonnes | **oui** `JsonFormatCellsStep`, format `baserow` |
| **Dédoublonner** | lignes → lignes | colonnes, clé de la valeur dans la cellule | **oui** `JsonDistinctStep` |
| **Regrouper et agréger** | lignes → une ligne par groupe | colonnes de groupe, agrégations `{type, column, key, label}` (count, sum, avg, min, max), schéma (contrôle, décimales), variable d'erreur, format des cellules d'agrégat | **oui** `JsonGroupStep` |
| **Écrire la sortie** | variable → réponse | clé, variable | existant : JSON to XML + jsonNativeOutput (lit les `Map`/`List`) ; cible : étape dédiée qui écrit l'arbre en flux |
| Ajouter / concaténer | tableau + tableau → tableau, avec limite | source, cible, limite | non (pagination de FetchNextPages) |
| Filtrer, trier, limiter | lignes → lignes | condition déclarative, colonnes, ordre | non |

Choix de la maquette :

- Les étapes héritent de `JsonTransformStep` (propriétés `rows` et `variableName`, expressions évaluées dans le scope
  de la séquence, résultat mis dans une variable). Elles ne produisent pas de XML (`xml = false`).
- Elles lisent indifféremment des objets Rhino (`JSON.parse`, JavaScript) et des `Map`/`List` Java (`JsonData`), avec
  la sémantique exacte du JavaScript qu'elles remplacent : clé absente = undefined, `==` lâche, `"" + x`, `toFixed`,
  `parseInt`, `parseFloat`, `trim`, `Math.round`, ordre des clés de `for in`.
- Le format `baserow` (`BaserowCellFormat`) est un **profil nommé** : HTML des cellules, valeurs, valeurs lean et
  options de colonnes, cellules d'agrégat, arrondis. C'est le seul code métier ; les étapes sont génériques.
  Dans un produit, ces profils seraient enregistrés par le projet (classe Java fournie par la lib) ou décrits par des
  gabarits par type de colonne.
- Ordre de la chaîne dans `formssource_GetTableDataNative` : Formater → Dédoublonner → Regrouper → Sélectionner. Le
  regroupement lit les colonnes agrégées avant que la sélection ne les retire, comme le JavaScript lisait `obj` avant
  `jReorderedRow`.
- ComputeData reste dans la séquence pour ce que la maquette ne prend pas (modèle de grille, groupes déjà calculés
  par le plugin, table vide qui produit un `colDef`) ; le symbole `lib_baserow.native_steps=false` y envoie tout.

```yaml
↓cells [steps.JsonFormatCellsStep-1990000000078]:
  rows: results
  schema: nativeStructure
  columns: displayColumns
  hiddenColumns: nativeHidden
  valuesOnly: leanCells
  defaultTimezone: '"${lib_BaseRow.dateTimezone=UTC}"'
  variableName: cells
  fieldsVariableName: leanFields
↓distinct [steps.JsonDistinctStep-1990000000079]:
  rows: cells
  columns: distinctColumns
  variableName: cells
↓groups [steps.JsonGroupStep-1990000000080]:
  rows: cells
  groupColumns: groupByColumns
  aggregations: aggregationSpecs
  schema: nativeStructure
  errorVariableName: aggregationError
  variableName: cells
↓columns [steps.JsonSelectColumnsStep-1990000000081]:
  rows: cells
  firstColumns: '["id", "order"]'
  columns: nativeColumns
  columnsOrder: nativeOrder
  variableName: data
```

836 lignes de JavaScript (ComputeData) deviennent 4 étapes et une étape JavaScript de 9 lignes qui prépare leurs
paramètres (colonnes masquées, colonnes de sortie) : la configuration par requête peut rester en JavaScript, c'est
le travail par ligne qui doit être natif.

## 5. Coût fixe par requête

Mesuré dans le profil final : ~15 % des allocations hors travail utile, dont `DirClassLoader.loadClass` 3,5 %,
`HttpServletRequestTwsWrapper` 2,7 %, en-têtes du CompressionFilter 2,0 %, `initContext` 1,8 %, clone des
requestables 1,7 % + 0,5 %, `newScope` 1,6 %, `include` 1,1 %, `insertObjectsInScope` 1,0 %. Les sous-séquences
coûtent en plus ~13 % (3,8 % hors runCore côté appelé, 9,4 % dans `InternalRequester.processRequest` côté appelant).

Ce qu'une séquence JSON native permet de supprimer :

1. **Ne plus copier la séquence.** `Context.loadSequence` clone la séquence (`cloneKeepParent`) à chaque requête, et
   `Sequence.getStepCopyToExecute` / `StepWithExpressions.getStepCopy` copient chaque étape à chaque exécution (à
   chaque tour de boucle). La copie existe parce que les étapes gardent un état d'exécution dans leurs champs
   (`evaluated`, `executeTimeID`, `jsonSource`, `sequence`...). Cible : des étapes sans état (la définition) et un
   **cadre d'exécution** par requête (état par identifiant d'étape, variables, sortie). Les étapes de la maquette
   n'ont d'autre état que `Step.evaluated` : elles pourraient s'exécuter sans copie.
2. **Ne plus reconstruire le contexte JavaScript.** Chaque requestable fait `initStandardObjects` (des centaines
   d'objets Rhino), insère context/log/dom/steps, puis `include("js/utils.js")` réexécute les scripts (définitions de
   fonctions, gros littéraux comme `_c8oBRtypes`). Cible : un scope parent scellé par version de projet (objets
   standard + includes exécutés une fois, comme `use()` le fait déjà avec `UseResult`), et un scope de requête léger
   qui en hérite ; pas de scope du tout pour une séquence sans étape JavaScript.
3. **Sous-séquence appelée directement**, sans requête interne ni document XML : entrée JSON, sortie JSON, même
   contexte enfant léger (gain plafonné à ~13 % mesuré).
4. **Pas de document XML d'entrée** (`GenericRequester` crée `context.inputDocument` à chaque requête) tant
   qu'aucune étape XML ne le lit.

## 6. Studio

- **Création** : plutôt qu'une nouvelle classe de séquence, un mode « JSON » sur `GenericSequence` (évolution de
  `jsonNativeOutput`), pour garder les tests, les variables, l'exposition et le YAML existants. La palette met en
  avant les étapes JSON et signale celles qui forcent une conversion XML.
- **Édition** : les propriétés des étapes sont des expressions JavaScript (déjà marquées `SCRIPTABLE` dans les
  BeanInfo de la maquette) avec complétion des variables de la séquence. Les listes (colonnes, agrégations)
  mériteraient un éditeur de tableau qui écrit une liste YAML plutôt qu'une expression.
- **Affichage JSON** : chaque exécution de test garde la valeur JSON de la variable produite par chaque étape ; la
  vue de la séquence l'affiche en arbre/texte à la place de l'arbre XML de sortie, et la réponse du test case est
  montrée en JSON. Le débogueur pas à pas affiche les variables JSON du cadre.
- **Schéma** : les séquences XML décrivent leur sortie en XSD ; une séquence JSON a besoin d'un schéma JSON
  (déduit d'un échantillon, comme `jsonSample`, ou déclaré) pour l'OpenAPI et l'éditeur de sources des pages NGX.
- **YAML** : les étapes s'écrivent comme les autres beans (exemple ci-dessus) ; les valeurs par défaut sont omises
  grâce à `database_objects_default.xml` (généré par `updateBeansDefaultValues`). Lisible, comparable en revue.
- **Compatibilité et mélange** : la maquette montre que les étapes natives vivent dans une séquence XML ordinaire,
  à côté de SimpleStep, IfStep, SequenceStep, JSON to XML. Dans l'autre sens, une étape XML dans une séquence JSON
  doit déclencher une conversion explicite et visible (source XPath sur une valeur JSON = conversion JSON → XML de
  cette valeur seulement). Vu du JavaScript, une `List` Java n'est pas un tableau (`Array.isArray` faux,
  `JSON.stringify` inutilisable) : la frontière doit convertir ou envelopper proprement (enveloppe qui se comporte
  comme un tableau/objet JS), sinon des bugs subtils apparaîtront dans les séquences mixtes.

## 7. Mesures de la maquette

Banc bench-acoss (Convertigo 8.4.6 et Baserow 1.34.5 dans Docker, 46 appels de sources capturés sur 6 pages).
Moteur `perf/json-native-steps` + `javascript.shared_class_cache=true` + `cems.Context` à WARN ; lib
`perf/aggregate-plugin` (raw_json, colonnes utiles), Baserow sans plugin (les groupes sont calculés par la séquence,
donc par l'étape Regrouper). Seule la séquence appelée change : `formssource_GetTableData` (JavaScript),
`formssource_GetTableDataNative` (étapes natives), `formssource_GetTableDataMixed` (étapes natives + une étape XML
existante au milieu). Outils : `tools/jsonsteps_all.sh`, `jsonsteps_r3.sh`, `jsonsteps_dump.py`, `jsonsteps_run.py`,
`jsonsteps_table.py`, `jsonsteps_alloc_groups.py`.

### Sorties

| Comparaison avec formssource_GetTableData | sans lean_cells | avec lean_cells |
|---|---|---|
| Native : valeurs / ordre des clés / texte | 46/46, 46/46, 46/46 | 46/46, 46/46, 46/46 |
| Mixte : valeurs / ordre des clés / texte | 46/46, 46/46, 46/46 | 46/46, 46/46, 46/46 |

28 appels passent par les étapes natives (vérifié en inversant `id`/`order` dans une copie déployée : 28 réponses
changent d'ordre, aucune de valeur) ; 14 tables vides et le modèle restent à ComputeData ; 3 appels sont
`formssource_GetSelectData` ; une erreur `ERROR_ORDER_BY_FIELD_NOT_FOUND` préexistante est identique partout. Le test
différentiel hors banc (vrai JavaScript de la lib contre le Java, 47 schémas de colonnes (dont aucun) × 90 valeurs, soit 4 196 cellules, en HTML et en lean,
options de colonnes, 600 cellules d'agrégat, 3000 nombres aléatoires pour `toFixed`, `Math.round`, `parseInt`,
`parseFloat`) donne 25 234 vérifications et 0 écart.

### Temps et mémoire (total des 6 pages, médiane de 3 tours ; mixte : 1 tour)

| Configuration | 1 util. (ms) | 5 util. (ms) | Mo alloués (GC, 5 util.) |
|---|---|---|---|
| JavaScript, cellules HTML | 3048 | 9636 | 7752 |
| Étapes natives, cellules HTML | 2766 (-9 %) | 9379 (-3 %) | 7621 (-2 %) |
| Mixte (vue XML + JsonSourceStep), cellules HTML | 3536 (+16 %) | 9711 (+1 %) | 10693 (+38 %) |
| JavaScript, lean_cells | 2752 | 8681 | 7523 |
| Étapes natives, lean_cells | 2456 (-11 %) | 9510 (+10 %) | 7544 (0 %) |
| Mixte, lean_cells | 3045 (+11 %) | 9444 (+9 %) | 9223 (+23 %) |

Par tour (1 util. ms / 5 util. ms / Mo) : JavaScript HTML 3637/17782/7752 (tour 1 perturbé par une compilation sur
la même machine), 3048/9636/7835, 2904/9547/7734 ; natif HTML 3164/9379/7621, 2766/8777/7697, 2653/9884/7611 ;
JavaScript lean 2752/8681/7523, 2670/8477/7457, 3107/12727/7581 ; natif lean 2437/9510/7598, 2456/10590/7544,
2553/8165/7487. Le temps à 5 utilisateurs varie de ±20 % d'un tour à l'autre : seul le temps à 1 utilisateur et la
mémoire, stables, permettent de conclure.

Profil d'allocation JFR (1 utilisateur, 3 tours, échantillonné ; il sous-estime le total car il perd les fils de
requestables courts, il sert à la répartition) :

| Poste | JavaScript | Natif |
|---|---|---|
| ComputeData et autres scripts de la séquence | 724 Mo | 78 Mo |
| formatBaserowCell et co. | 137 Mo | — |
| getResolvedFieldType / getBrFilterType | 74 Mo | 3 Mo |
| Étapes natives (cellules, groupes, colonnes, `JsonData`) | — | 374 Mo |
| JSON.parse des pages Baserow (inchangé) | 249 Mo | 250 Mo |
| Total échantillonné | 2,85 Go | 2,34 Go |

### Lecture

- **Exactitude** : les étapes ensemblistes reproduisent le JavaScript octet pour octet, y compris dans un mode mixte.
- **Temps** : -9 à -11 % sur le temps de réponse à 1 utilisateur, toutes requêtes comprises (le travail remplacé
  n'est qu'une partie de chaque requête : appel Baserow, `JSON.parse`, coût fixe, sérialisation restent).
- **Mémoire** : le travail remplacé alloue deux fois moins (~850 → 374 Mo échantillonnés), mais le total mesuré par
  le GC ne bouge presque pas (-2 %, 0 %). Les étapes de la maquette allouent encore beaucoup : une `LinkedHashMap`
  par cellule, une copie de chaque ligne par l'étape Sélectionner, un `StringBuilder` par cellule, les clés de
  chaque ligne recopiées. Pistes : cellules compactes (objet à champs fixes écrit directement en JSON), étapes qui
  modifient en place les lignes qu'elles possèdent, un seul tampon HTML réutilisé, chaîne Formater → Sélectionner
  fusionnée quand rien ne s'intercale. Le plus gros reste ailleurs : `JSON.parse` des pages (à remplacer par l'étape
  Lire en analyse en flux), sérialisation de la réponse, coût fixe par requête.
- **Pont** : une étape XML qui lit par XPath le tableau entier des cellules (~23 000 cellules) coûte plus que tout le
  ComputeData JavaScript : +28 % de temps et +40 % de mémoire par rapport aux étapes natives sans pont (+3 Go
  alloués). Ce pont-ci passe par du texte (JSON → JSONObject → DOM → XmlToJson → `JSON.parse`) ; une vue construite
  directement depuis les `Map`/`List`, et surtout des sources en chemin JSON, en enlèveraient l'essentiel. Le mode
  mixte est donc une **compatibilité**, pas un régime de croisière : le Studio doit montrer où le pont s'applique et
  sur quel volume.
- **Conclusion** : des séquences natives JSON en étapes ensemblistes sont faisables à l'identique et plus rapides,
  mais le gain mesurable sur ce cas (~10 % du temps) ne vient qu'en remplaçant le JavaScript le plus lourd ; le gain
  de mémoire demande des étapes plus économes que la maquette, et les plus gros postes restants sont la lecture des
  pages Baserow, la sérialisation de la réponse et le coût fixe par requête (section 5).

Limites de la maquette : le format est choisi par une propriété (`baserow`) sans contrôle des valeurs inconnues ;
les étapes ne sont pas encore utilisables sans JavaScript pour préparer leurs paramètres ; la vue XML passe par du
texte ; la table vide (`colDef`) et le modèle de grille restent en JavaScript ; mesures sans le plugin d'agrégation
(avec lui, l'étape Regrouper n'a presque rien à faire).

## 8. Risques et effort

Risques :

- **Exactitude octet par octet** : la sémantique du JavaScript (`==` lâche, `0 == ""` vrai donc un nombre 0 vide sa
  cellule, `toFixed`, dates, `trim` Unicode, ordre des clés) doit être reproduite exactement. La maquette passe un
  test différentiel qui exécute le vrai JavaScript de la lib et le Java sur les mêmes valeurs (25 234 vérifications,
  0 écart) en plus des 46 appels du banc. Ce test doit accompagner tout profil de format.
- **Double maintenance** : le format existe en JavaScript dans la lib (et dans le front pour lean_cells) et en Java
  dans le moteur. Il faut une seule source (le profil Java, la lib ne gardant que le repli) sinon ils divergeront.
- **Code métier dans le moteur** : le profil `baserow` n'a rien à faire dans le moteur générique ; il faut un
  mécanisme de profils fournis par les projets (jar dans la lib) ou déclaratifs.
- **Frontière Rhino / Java** (ci-dessus) et **état des étapes** : supprimer la copie des étapes demande de sortir
  l'état d'exécution de toutes les étapes, y compris les anciennes ; le faire seulement pour le mode JSON limite le
  risque.
- **Scope JavaScript partagé** : scellé, sinon une requête pourrait modifier les objets d'une autre ; les includes
  qui ont des effets de bord (variables globales modifiées) ne peuvent pas être partagés tels quels.
- **Effort Studio** important (palette, éditeurs, vues JSON, schéma, débogueur).

Effort estimé (jours de développement, hors revue) :

| Lot | Effort |
|---|---|
| Modèle de données JSON, enveloppe JavaScript, étapes Lire / Ajouter / Filtrer / Trier en plus des 4 de la maquette, tests | 6 à 9 j |
| Profils de format fournis par les projets, profil Baserow sorti du moteur, test différentiel outillé | 3 à 4 j |
| Étape de sortie en flux, entrée JSON, pas de document d'entrée | 3 à 4 j |
| Tous les beans : vue XML à la demande pour toute valeur JSON, sources en chemin JSON (et traduction des XPath simples), sortie mixte nœuds XML + JSON aux règles de XmlToJson, DOM de travail créé seulement au premier besoin | 6 à 9 j |
| Adaptations simples (IteratorStep, IfExist, JsonSource, JsonObject/Array/Field, ReadJSON/CSV, XMLError, InputVariables, appels de séquences JSON) | 5 à 8 j |
| Scope parent scellé par version de projet, includes exécutés une fois | 3 à 5 j |
| Étapes sans état et cadre d'exécution (mode JSON), plus de clone de séquence | 6 à 10 j |
| Appel direct des sous-séquences JSON | 5 à 8 j |
| Studio : mode JSON, palette, éditeurs de listes, vues JSON des étapes et des tests, schéma JSON | 12 à 20 j |
| lib_BaseRow : ComputeData, pagination et lecture des pages en étapes natives, repli conservé | 2 à 3 j |

Ordre conseillé : (1) étapes ensemblistes + profils dans les séquences actuelles, mesurées comme ici ;
(2) scope partagé et includes, qui profitent aussi aux séquences XML ; (3) mode JSON avec pont pour tous les beans,
sans copie ni DOM tant qu'aucune étape XML n'en demande, et appel direct des sous-séquences ; (4) Studio.
