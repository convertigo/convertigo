# FS Parity Matrix (Iframe -> Native)

## Objectif

Suivre la parite **feature / look / URL** entre:

- mode iframe: `/admin/fullsync/` + URL interne Fauxton dans l'iframe
- mode natif: `/admin/fs/...`

Mise a jour iterative: **une page a 100% avant de passer a la suivante**.

### Scope de reference (iframe patchee)

La reference est le mode iframe **avec script d'injection** (`/src/routes/(app)/admin/fullsync/+page.svelte`), pas Fauxton upstream brut.

Effets de cette injection:

- suppression des entrees `Permissions` et `Changes` dans la sidebar DB
- suppression des liens `Metadata` dans les design docs
- suppression de la colonne `Size` sur la table legacy des bases

## Regles d'iteration

1. Ouvrir la page iframe de reference.
2. Noter l'URL **host** (`/admin/fullsync/...`) et l'URL **interne iframe** (`/convertigo/admin_/_utils/#...`).
3. Ouvrir la page native correspondante (`/admin/fs/...`).
4. Valider 3 axes sur la page:
   - URL: format, segments, navigation (back/forward, refresh, liens internes)
   - Look: structure, densite, tailles, alignements, etats (hover/active/disabled)
   - Feature: boutons, actions, wizards, dialogues, effets reseau
5. Si un bouton ouvre une page non traitee:
   - ajouter une ligne dans la matrice (statut `TODO`)
   - revenir finir la page courante
   - traiter la nouvelle page ensuite

## Definition "100%"

Une page est `DONE` quand:

- URL equivalente validee (incluant deep-link direct)
- tous les controles visibles testes (clic, etat, action)
- dialogues/wizards de la page verifies
- ecarts look documentes/corriges
- aucune regression de navigation detectee sur la page

## Statuts

- `TODO`: pas commence
- `IN_PROGRESS`: en cours de verification/correction
- `DONE`: parite validee sur la page
- `BLOCKED`: bloque par dependance (backend, composant, decision UX)
- `N/A`: hors perimetre (absent dans la reference iframe patchee)

## Matrice de correspondance

| ID     | Page / Ecran                         | URL host iframe    | URL interne iframe (reference)                                              | URL native FS cible                               | URL parity       | Look parity            | Feature parity     | Statut | Sorties vers pages non traitees | Notes                                                                                                                                                      |
| ------ | ------------------------------------ | ------------------ | --------------------------------------------------------------------------- | ------------------------------------------------- | ---------------- | ---------------------- | ------------------ | ------ | ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FS-001 | Liste des bases                      | `/admin/fullsync/` | `/convertigo/admin_/_utils/#`                                               | `/admin/fs/`                                      | OK               | DELTA (intentionnelle) | OK (scope convenu) | DONE   | FS-002                          | Deltas valides: pas de colonne Partitioned, pas d'action lock/permissions, ajout Search + Size + Seq; fallback `Size = N/A` si info absente (mode PouchDB) |
| FS-002 | Base: All Docs (`{db}`)              | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_all_docs`                        | `/admin/fs/{db}`                                  | OK (segment URL) | OK                     | OK                 | DONE   | FS-003, FS-005, FS-007, FS-008  | Parite validee: sidebar/toolbar/options/selection + pagination (avec/sans limit) + ouverture doc (ligne/toolbar)                                           |
| FS-003 | Mango Query (`_find`)                | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_find`                            | `/admin/fs/{db}/_find`                            | OK               | OK                     | OK                 | DONE   | FS-004, FS-008                  | Run/Explain + history + modes de rendu + navigation `Manage Indexes` validees                                                                              |
| FS-004 | Mango Indexes (`_index`)             | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_index`                           | `/admin/fs/{db}/_index`                           | OK               | OK                     | OK                 | DONE   | FS-003, FS-008                  | Create index + selection + delete valides; pagination alignee iframe (sans option `None`)                                                                  |
| FS-005 | Query d'une vue (`_design/_view`)    | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/_design/{ddoc}/_view/{view}`     | `/admin/fs/{db}/_design/{ddoc}/_view/{view}`      | OK               | OK                     | OK                 | DONE   | FS-006, FS-007, FS-008          | URL segmentee obligatoire; robustesse ajoutee sur 401/403 initiaux (retry auth)                                                                            |
| FS-006 | Edition d'une vue                    | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_design/{ddoc}/_view/{view}/edit` | `/admin/fs/{db}/_design/{ddoc}/_view/{view}/edit` | OK               | OK                     | OK                 | DONE   | FS-005                          | Deep-link `/edit` valide; `Cancel` + `Save` redirigent vers la view query                                                                                  |
| FS-007 | Edition document (`{docid}`)         | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/{docid}`                         | `/admin/fs/{db}/{docid}`                          | OK               | OK                     | OK                 | DONE   | -                               | Validation UI complete en session admin: save persistant + delete (No/Yes) + suppression effective (404 apres delete)                                      |
| FS-008 | Nouveau document (`_new`)            | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/_new`                            | `/admin/fs/{db}/_new`                             | OK               | OK                     | OK                 | DONE   | FS-007                          | Route dediee + titre `New Document` + bouton `Create Document`                                                                                             |
| FS-009 | Dialogue Upload Attachment           | `/admin/fullsync/` | Depuis ecran doc (`Upload Attachment`)                                      | Depuis ecran doc (`Upload Attachment`)            | N/A              | OK                     | OK                 | DONE   | -                               | Dialogue valide (`Esc`, clic externe), upload API valide, acceptation fichier 0 octet alignee Fauxton                                                      |
| FS-010 | Action Clone Document                | `/admin/fullsync/` | Depuis ecran doc (`Clone Document`)                                         | Depuis ecran doc (`Clone Document`)               | N/A              | OK                     | OK                 | DONE   | FS-007                          | Flux UI valide en session admin (`modal -> clone -> redirection clone -> save`); comportement `missing_stub` aligne Fauxton                                |
| FS-011 | View Attachments (liste + ouverture) | `/admin/fullsync/` | Depuis ecran doc (`View Attachments`)                                       | Depuis ecran doc (`View attachments`)             | N/A              | OK                     | OK                 | DONE   | -                               | Menu visible si pieces jointes, masque sinon; fermeture menu `Esc`/clic externe; ouverture nouvel onglet OK                                                |
| FS-012 | Permissions DB                       | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_security`                        | `/admin/fs/{db}/_security`                        | N/A              | N/A                    | N/A                | N/A    | -                               | Hors scope: lien retire par l'injection iframe (`.nav-list li a` contenant `Permissions`)                                                                  |
| FS-013 | Changes feed                         | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_changes`                         | `/admin/fs/{db}/_changes`                         | N/A              | N/A                    | N/A                | N/A    | -                               | Hors scope: lien retire par l'injection iframe (`.nav-list li a` contenant `Changes`)                                                                      |
| FS-014 | Design Doc Metadata (`_info`)        | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_design/{ddoc}/_info`             | `/admin/fs/{db}/_design/{ddoc}/_info`             | N/A              | N/A                    | N/A                | N/A    | -                               | Hors scope: lien retire par l'injection iframe (`.design-doc-body li a` contenant `Metadata`)                                                              |

## Validation detaillee FS-001 (DONE)

- Navigation row click: `offchat_fullsync` ouvre `http://localhost:5173/admin/fs/offchat_fullsync/`.
- Navigation via recherche: valeur `alerts` + action Open ouvre `http://localhost:5173/admin/fs/alerts/`.
- Pagination: page 2 OK (`Showing 21–29 of 29 databases`), next/previous operants.
- Delete row: dialogue Yes/No present; `No` annule sans suppression.
- Liens header: `JSON` pointe `.../convertigo/fullsync/_all_dbs` (new tab), doc pointe la doc CouchDB (new tab).
- Tri de colonnes (Docs/Size/Seq): tri interactif fonctionnel.

## Validation detaillee FS-002 (DONE)

- URL map validee:
  - iframe interne: `#/database/offchat_fullsync/_all_docs`
  - natif: `/admin/fs/offchat_fullsync/`
- Sidebar:
  - clic sur design doc ouvre/ferme bien l'accordion.
  - liens de vues presents (`.../_design/{ddoc}/_view/{view}`) + actions edit/clone/delete visibles.
- Toolbar/table:
  - input document + boutons Options/JSON/Docs visibles.
  - modes `Table / Metadata / JSON` presents.
  - selection ligne + copy id + pagination presentes.
- Validation finale:
  - interactions toolbar/options alignees iframe.
  - requetes `_all_docs` alignees selon le mode (all docs / view / include docs / tri / skip / limit).
  - verification desktop + mobile validee.

### Avancement FS-002 (iteration en cours)

- Corrige: separation `Limit` (Query Options) vs `Documents per page` (footer).
  - `Query Options > Limit` par defaut = `None`.
  - `Documents per page` reste a `20` par defaut.
- Corrige: requete deep-link view route alignee Fauxton.
  - natif: `/_design/{ddoc}/_view/{view}?include_docs=true&limit=21&skip=0&conflicts=true&reduce=false`
  - reference iframe observee: meme pattern.
- Corrige: listage design docs aligne.
  - natif: `/_all_docs?include_docs=true&limit=501&startkey=%22_design%2F%22&endkey=%22_design0%22`
- Corrige: filtrage des design docs Mango aligne Fauxton.
  - les design docs avec `language: "query"` sont exclus de la sidebar (comme dans Fauxton `sidebar/reducers.js`).
- Corrige: ordre des design docs stabilise par ID (`_design/...`) pour rester coherent avec la navigation iframe.
- Corrige: all docs par defaut aligne.
  - natif: `/_all_docs?limit=21&skip=0`

### Reste FS-002 avant DONE

- Aucun: FS-002 est valide et cloture.

## Avancement FS-003 (iteration en cours)

- Corrige: `Query Options` (dialogue all docs) select `Update`/`Limit` elargis pour eviter la troncature de la fleche.
- Corrige: panneau Mango query.
  - `Run Query` / `Explain` restent directement sous l'editeur (plus colles au bas du panneau).
  - `manage indexes` remplace par un bouton `Manage Indexes` (style admin).
- Corrige: editeurs Monaco Mango (`_find` et `_index`) passent en theme dynamique light/dark (alignes sur le reste de l'admin).
- Corrige: execution `_find` alignee sur le comportement Fauxton pour la pagination.
  - payload envoi `execution_stats=true` + `limit=documentsPerPage+1` + `skip`.
  - `next/previous` recharge par requete serveur (plus de pagination locale decalee).
  - detection `hasNext` basee sur la ligne supplementaire.
- Verifie: mode Explain garde les controles de gauche visibles (run/explain/manage indexes), comme l'iframe.
- Verifie: mode JSON rend une liste par document (selection + ouverture doc), avec pagination et suppression multi-doc.
- Corrige: structure du header/resultats alignee Fauxton.
  - `Create Document` deplace dans le header de page (`_find`) au lieu de la toolbar de resultats.
  - toolbar de resultats masquee quand il n'y a aucun resultat (plus de "header distendu" dans l'etat vide).
  - `Manage Indexes` reste un lien visible sous l'editeur de query.

### Reste FS-003 avant DONE

- Aucun: FS-003 est valide et cloture.

### Validation FS-003 complementaire (session courante)

- Validation comparee iframe vs natif sur `offchat_fullsync`:
  - iframe: `#/database/offchat_fullsync/_find`
  - natif: `/admin/fs/offchat_fullsync/_find/`
- Desktop (`http://localhost:18080/convertigo/admin/fs/offchat_fullsync/_find/`):
  - `Run Query` -> affichage table, colonnes selectionnables, pagination et selection multi-docs OK.
  - bascule `Table`/`JSON` validee apres run; cards JSON avec action `Open document` OK.
  - `Explain` affiche le JSON explain et masque la toolbar/result footer de resultats (alignement iframe).
- Navigation:
  - bouton `Manage Indexes` -> `/admin/fs/offchat_fullsync/_index/` OK.
  - retour `Edit Query` depuis `_index` -> `/admin/fs/offchat_fullsync/_find/` OK.
- Mobile (viewport 390px):
  - header/actions/editor/toolbar/resultats restent utilisables sans blocage.
  - controle `Documents per page` + navigation pagination restent accessibles.

## Avancement FS-004 (iteration en cours)

- URL/Navigation verifiees:
  - iframe: `#/database/{db}/_index`
  - natif: `/admin/fs/{db}/_index`
  - `Edit Query` retourne bien vers `/admin/fs/{db}/_find`.
- Fonctionnel verifie:
  - `Create Index` cree bien un index json.
  - listage indexes recharge apres creation.
  - selection index + `Delete selected...` fonctionne avec confirmation.
  - suppression index rafraichit la liste.
  - `special: _id` non supprimable en bulk (comportement attendu).
  - `Create Document` deplace dans le header de page (`_index`) pour alignement avec Fauxton.
- Nettoyage effectue pendant test:
  - index de test cree puis supprime, retour a l'etat initial.

### Reste FS-004 avant DONE

- Aucun: FS-004 est valide et cloture.

### Validation FS-004 complementaire (session courante)

- Validation comparee iframe vs natif sur `offchat_fullsync`:
  - iframe: `#/database/offchat_fullsync/_index`
  - natif: `/admin/fs/offchat_fullsync/_index/`
- Desktop:
  - `Create Index` avec exemple `message index` cree bien un index `json: message-idx`.
  - selection de l'index creee + action `Delete selected...` + confirmation `Yes` fonctionnent.
  - suppression effective validee (retour a `special: _id` uniquement).
- Parite pagination `_index`:
  - select `Documents per page` aligne iframe (`5, 10, 20, 30, 50, 100`), sans `None`.
- Mobile (viewport 390px):
  - panneau editeur + actions (`Create Index`, `Edit Query`) + liste indexes + footer pagination accessibles.

## Avancement FS-005 (iteration en cours)

- URL verifiee au clic sidebar:
  - iframe: `#/database/offchat_fullsync/_design/OffChat/_view/getChat`
  - natif: `/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/`
- Navigation sidebar verifiee:
  - clic design doc ouvre l'accordion.
  - liens de vues naviguent bien vers URL segmentee (sans query string d'etat).
- Requete reseau verifiee sur view:
  - natif: `/_design/OffChat/_view/getChat?limit=21&skip=0&reduce=false`
  - iframe: `/_design/OffChat/_view/getChat?skip=0&limit=21&reduce=false`
  - pattern aligne (hors ordre des query params).
- Corrige: layout par defaut en route view.
  - natif demarre maintenant en mode `Table` (plus `Metadata`).
- Corrige: table view par defaut alignee Fauxton.
  - schema de colonnes derive de `row.doc` (comme all_docs) + champs de `row.value` quand presents.
  - colonne copy reintroduite en mode table.
  - `include_docs` active par defaut a l'ouverture d'une route `_view`.
- Correctif transversal applique pendant l'audit:
  - logo topbar passe en `convertigo:logo` (plus de chemin relatif casse sur routes profondes).

### Reste FS-005 avant DONE

- Aucun: FS-005 est valide et cloture.

### Validation FS-005 complementaire (session courante)

- Comparee au rendu iframe `#/database/offchat_fullsync/_design/OffChat/_view/getChat`:
  - `Table` affiche la selection de colonnes basee sur les champs documents (`include_docs` actif par defaut sur une route `_view`).
  - les champs issus de `row.value` sont injectes dans le schema de colonnes table (avec des labels alignes Fauxton).
  - colonne finale "attachements" ajoutee en bout de tableau (compteur).
  - `Metadata` affiche les colonnes fixes `id / key / value`.
  - valeurs `null` en metadata conservees comme `null` (plus converties en `{}`).
  - bascule `Table / Metadata / JSON` debloquee sur route `_view` (plus de reset force sur `Table`).
  - clic design doc dans la sidebar ouvre bien l'accordion cible.
  - ordre des views trie alphabetiquement dans chaque design doc.
  - footer table: label + select `Documents per page` elargis (plus de wrap/troncature).
- Validation reseau post-correctif (session courante):
  - `include_docs=false` conserve l'etat apres `Run Query` avec une seule requete `_view` (`?limit=21&skip=0&reduce=false`).
  - `skip=1` conserve l'etat apres `Run Query` avec une seule requete `_view` (`?limit=21&skip=1&reduce=false`).
  - `descending=true` + `limit=5` envoie une seule requete `_view` attendue
    (`?include_docs=true&limit=5&skip=0&descending=true&conflicts=true&reduce=false`).
  - `stable=true` + `update=lazy` envoie `...&stable=true&update=lazy...`.
  - `stable=true` + `update=false` envoie `...&stable=true&update=false...`.
  - mode `By Key(s)` avec `Key=userList` envoie `...&key=%22userList%22`.
  - mode `Between Keys` avec `Start key=a` / `End key=z` envoie `...&startkey=%22a%22&endkey=%22z%22`.
  - `reduce=true` sur une map-only view retourne bien `400` (`?limit=21&skip=1&reduce=true&group=true`) avec message UI
    `Reduce is invalid for map-only views.`.
  - les `403` observes precedemment sur `_view` ne sont pas reproduits dans cette session de validation.
- Validation interactions (session courante):
  - clic sur une ligne resultats ouvre bien `/admin/fs/{db}/{docid}/` puis retour navigateur OK vers la route `_view`.
  - avec `Include Docs=false`, la selection globale est desactivee et l'action
    `Delete {n} selected document(s)` disparait.
  - `Copy row` affiche bien la notification `Row copied`.

### Validation FS-002 complementaire (session courante)

- `all_docs` mode `JSON` aligne sur la presentation Mango JSON:
  - cartes JSON par document,
  - switch de selection par ligne,
  - action d'ouverture du document dans l'entete de carte.
- Pagination sans `Query Options > Limit`:
  - avec `Documents per page=5`, navigation `Next/Previous` validee;
  - requetes observees: `/_all_docs?limit=6&skip=0` puis `/_all_docs?limit=6&skip=5`.
- Pagination avec `Query Options > Limit=10` + `Documents per page=5`:
  - alignee Fauxton: page 1 `Showing document 1 - 5`, page 2 `Showing document 6 - 10`, `Next` desactive.
  - correctif applique: suppression de la ligne sentinelle `docsPerPage+1` quand la limite de query est atteinte.
- Ouverture document validee:
  - clic sur ligne -> ouverture `/admin/fs/{db}/{docid}/`;
  - toolbar `Document ID` + bouton `Open document` -> meme redirection.
- Check mobile rapide (viewport 390px):
  - sidebar/tables/actions restent accessibles, sans rupture bloquante.

## Validation detaillee FS-006 (DONE)

- Corrige: URL native d'edition alignee sur l'iframe (`.../_view/{view}/edit`).
- URL edit capturee en iframe au clic sur l'action view (wrench):
  - `#/database/offchat_fullsync/_design/OffChat/_view/checkUsername/edit`
- URL edit native correspondante:
  - `/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/edit/`
- Comportement compare:
  - le formulaire `Design Document / Index name / Map / Reduce` est present des 2 cotes.
  - `Cancel` retourne bien sur la page de query de la view dans les deux versions.

### Validation FS-006 complementaire (session courante)

- Correctif applique sur le composant Monaco partage pour eviter les rendus "editor ecrase" lors de transitions de page/layout.
- Recontrole visuel sur `/admin/fs/offchat_fullsync/_design/c8o/_view/hash/edit/` et
  `/admin/fs/offchat_fullsync/_design/c8o/_view/checkAcl/edit/`: hauteur editeur stable.
- Validation fonctionnelle finale en session admin (base `alerts`):
  - deep-link `/admin/fs/alerts/_design/codex_fs006_1771415892887/_view/v1/edit/`:
    `Cancel` renvoie vers `/admin/fs/alerts/_design/codex_fs006_1771415892887/_view/v1/`.
  - `Save Document and then Build Index` depuis `/edit`: `PUT .../_design/codex_fs006_1771415892887` retourne `201`,
    toast `View saved`, puis redirection sur la route query de la view.

## Validation detaillee FS-007 (DONE)

- URL de doc existant verifiee:
  - iframe: `#/database/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy`
  - natif: `/admin/fs/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy/`
- Controles presents et compares:
  - `Save Changes`, `Cancel`, `View attachments`, `Upload Attachment`, `Clone Document`, `Delete`.
  - editeur JSON Monaco present.
  - validation alignee Fauxton: `_id` non modifiable pour un document existant (message d'erreur avec consigne de cloner).
- Validation completee en session admin:
  - `Save Changes`: modification JSON + toast `saved` + `_rev` incremente.
  - `Delete`: dialogue confirme (`No` annule, `Yes` supprime), redirection liste + toast `deleted`.
  - verification backend post-delete: `GET /convertigo/fullsync/{db}/{docid}` retourne `404` (`not_found/deleted`).
- Correctif d'implementation:
  - `removeDocument()` aligne sur une suppression fiable via `_bulk_docs` (retour `201`) au lieu d'un `DELETE` pouvant renvoyer `200` vide sans suppression effective.

### Reste FS-007 avant DONE

- Aucun: FS-007 est valide et cloture.

## Validation detaillee FS-008 (DONE)

- Iframe `_new` valide:
  - URL: `#/database/offchat_fullsync/_new`
  - titre: `New Document`
  - payload initial avec `_id` auto-genere (32 hex), bouton `Create Document`.
- Native corrige:
  - route dediee: `/admin/fs/[database]/_new/+page.svelte`.
  - URL chargee: `/admin/fs/offchat_fullsync/_new/`.
  - header aligne: `offchat_fullsync > New Document`.
  - action principale alignee: bouton `Create Document`.
  - actions non pertinentes masquees en mode creation (`View attachments`, `Upload`, `Clone`, `Delete`).
  - generation `_id` initial via `_uuids` avec fallback local hex 32 caracteres.
- Validation E2E completee:
  - `Create Document` cree bien le document et redirige vers `/admin/fs/{db}/{docid}/`.
  - `Cancel` depuis `/_new` retourne bien vers `/admin/fs/{db}/`.
  - nettoyage de test effectue (doc cree puis supprime dans `alerts`).

## Avancement FS-009 / FS-010 / FS-011 (iteration en cours)

- FS-011 (View attachments):
  - natif: menu des pieces jointes visible; clic ouvre bien un nouvel onglet
    - ex: `/convertigo/fullsync/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy/img.jpeg`
  - iframe: comportement equivalent valide (menu + nouvel onglet).
- FS-010 (Clone):
  - iframe et natif affichent un dialogue de clone avec avertissement et ID genere.
  - exemples observes:
    - iframe: `33e7ab4aa0ee52aaeea07698d2001243`
    - natif: `33e7ab4aa0ee52aaeea07698d200389a`
- FS-009 (Upload):
  - bouton/dialogue presents dans les deux versions.

### Reste FS-009 / FS-010 / FS-011 avant DONE

- Aucun: FS-009, FS-010 et FS-011 sont valides et clotures.

### Validation API complementaire (session courante)

- Base testee: `http://localhost:5173/convertigo/fullsync` (DB `alerts`).
- FS-009 (upload attachment):
  - create doc: OK
  - upload `file.txt`: OK
  - relecture doc: `_attachments` present (`count=1`)
  - ouverture direct attachment (`/{db}/{docid}/file.txt`): OK
- FS-010 (clone):
  - clone d'un doc sans attachment: OK
  - clone d'un doc avec attachment stub: `412 missing_stub` (`Invalid attachment stub ...`)
  - comportement coherent avec la logique Fauxton `doc.copy()` (copie brute JSON + suppression `_rev`).
- FS-004/FS-003 (mango):
  - create index `_index`: OK
  - delete bulk `_index/_bulk_delete`: OK
  - `_find` avec `execution_stats=true`: OK (`execution_stats` present dans la reponse)

## Verification URL deja observee (session courante)

- Host iframe (admin legacy): `http://localhost:5173/admin/fullsync/`
- Iframe root: `http://localhost:5173/convertigo/admin_/_utils/`
- Iframe all docs: `http://localhost:5173/convertigo/admin_/_utils/#database/offchat_fullsync/_all_docs`
- Iframe mango: `http://localhost:5173/convertigo/admin_/_utils/#database/offchat_fullsync/_find`
- Iframe indexes: `http://localhost:5173/convertigo/admin_/_utils/#database/offchat_fullsync/_index`
- Iframe view link (exemple): `http://localhost:5173/convertigo/admin_/_utils/#/database/offchat_fullsync/_design/c8o/_view/checkAcl`
- Iframe view link (exemple 2): `http://localhost:5173/convertigo/admin_/_utils/#/database/offchat_fullsync/_design/OffChat/_view/getChat`
- Iframe view edit (exemple): `http://localhost:5173/convertigo/admin_/_utils/#database/offchat_fullsync/_design/OffChat/_view/checkUsername/edit`
- Iframe doc link (exemple): `http://localhost:5173/convertigo/admin_/_utils/#/database/offchat_fullsync/13608b61-7ca0-40a6-beb8-dcac14d5b958`
- Iframe new doc: `http://localhost:5173/convertigo/admin_/_utils/#/database/offchat_fullsync/_new`
- Native view (exemple): `http://localhost:5173/admin/fs/offchat_fullsync/_design/c8o/_view/checkAcl/`
- Native view (exemple 2): `http://localhost:5173/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/`
- Native view edit (exemple): `http://localhost:5173/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/edit/`
- Native doc link (exemple): `http://localhost:5173/admin/fs/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy/`
- Native new doc: `http://localhost:5173/admin/fs/offchat_fullsync/_new/`

## Refactor interne KISS (session courante)

- Objectif: reduire la duplication sans changer le comportement visible.
- Fichier: `FullSyncDatabasePage.svelte`
  - extraction de helpers generiques:
    - `toggleSelectionMap`
    - `selectAllFromRows`
    - `setColumnSelection`
  - extraction du rendu repetitif `all_docs`/`mango` dans un composant dedie:
    - `FullSyncRowsPanel.svelte`
  - le composant centralise:
    - switch de selection par ligne
    - copy/open
    - rendu `json`, `table`, `metadata`
    - selecteurs de colonnes
- Verification compilation:
  - `npm run check:admin`: OK
  - `npm run build`: OK

## Session update (2026-02-17)

- Verification technique relancee apres les derniers ajustements:
  - `npm run check:admin`: OK (`0 errors, 0 warnings`)
  - `npm run build`: OK (warnings non bloquants hors scope FS)
- Validation live reexecutee avec DevTools:
  - URL native `_view` testee: `/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/`
  - URL native `_view` testee: `/admin/fs/offchat_fullsync/_design/c8o/_view/checkAcl/`
  - requetes observees:
    - `GET /convertigo/fullsync/offchat_fullsync/_design/OffChat/_view/getChat?limit=21&skip=0&reduce=false`
    - `GET /convertigo/fullsync/offchat_fullsync/_design/c8o/_view/checkAcl?limit=21&skip=0&reduce=false`
  - comportement observe: `403` intermittent au premier chargement (corps vide), puis `200` apres reload.
- Correctif applique suite a l'observation:
  - API FS native (`fullsync-api.js`) renforcee avec un retry unique sur `GET` en cas de `401/403`
    apres `engine.CheckAuthentication`.
  - objectif: eviter les `HTTP 403` intermittents au premier chargement de routes `_view`.
- Impact matrice:
  - FS-005 repasse `IN_PROGRESS` (plus `BLOCKED`) avec mitigations en place.
  - validation finale reste a faire sur parcours long (navigation croisee + actions de ligne) pour passer `DONE`.

## Session update (2026-02-17, passe Mango)

- Verification live (DevTools) relancee sur:
  - `/admin/fs/offchat_fullsync/_find`
  - `/admin/fs/offchat_fullsync/_index`
  - `/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/`
- Resultats:
  - `_find` et `_index` ne presentent plus d'ecran "vide" intermittent apres navigation.
  - `_find` sans resultats: panneau central sans toolbar resultat parasite + footer conserve.
  - `_find` avec resultats: toolbar `select-all + Table/JSON` visible, `Create Document` reste en header.
  - `_find` explain: boutons gauche (`Run Query`, `Explain`, `Manage Indexes`) restent visibles.
  - `_index`: `Create Document` visible en header + `Create Index` / `Edit Query` valides.
  - `_view`: bascule `Table/Metadata/JSON` validee.
- Requetes reseau reconfirmees:
  - all docs: `/_all_docs?limit=21&skip=0`
  - design docs: `/_all_docs?include_docs=true&limit=501&startkey=%22_design%2F%22&endkey=%22_design0%22`
  - view query: `/_design/OffChat/_view/getChat?limit=21&skip=0&reduce=false`

## Session update (2026-02-17, hygiene tooling)

- Verification relancee sur le cas signale:
  - `npm run format` puis `npm run check:admin` -> OK (`0 errors, 0 warnings`).
- Build complet:
  - `npm run build` -> OK (warnings connus hors scope FS).

## Session update (2026-02-17, alignement fonctions Fauxton)

- Alignement table "dernier Fauxton" sur les ecrans FS natifs (sans toucher le scope masque iframe):
  - colonne de fin "attachements" visible en mode `Table` sur `all_docs`, `_view` et `_find` (header vide comme Fauxton).
  - compteur de pieces jointes alimente depuis `_attachments` quand disponible.
- Alignement edition document:
  - blocage de la modification `_id` sur document existant, avec message explicite recommandant `Clone Document` (comme Fauxton).
- Scope iframe patchee conserve sans changement:
  - `Permissions`, `Changes`, `Metadata` design doc restent hors perimetre (`N/A`).

## Session update (2026-02-17, alignement doc-editor + table trailing column)

- Alignement table `Table` avec le Fauxton recent:
  - colonne de fin enrichie avec l'indicateur de conflits (`_conflicts`) en plus des pieces jointes (`_attachments`).
  - affichage applique sur `all_docs`, `_view` (quand `include_docs`) et `_find`.
- Alignement ecran document:
  - `View Attachments` masque quand le document n'a aucune piece jointe (comme `AttachmentsPanelButton` Fauxton).
  - dialogue `Upload Attachment`: bouton `Upload Attachment` desactive tant qu'aucun fichier n'est selectionne.
  - dialogue `Clone Document`: texte d'aide complete sur l'ID genere + focus automatique du champ ID.
- Scope masque iframe conserve:
  - aucun ajout sur `Permissions`, `Changes`, `Metadata` design doc (`N/A` inchanges).

## Session update (2026-02-17, view query reduce/group level)

- Alignement Query Options sur les routes `_view`:
  - ajout du toggle `Reduce` (comme Fauxton), avec exclusion mutuelle `Include Docs` <-> `Reduce`.
  - ajout de `Group Level` (`Exact`, `1..9`) actif quand `Reduce` est coche.
- Alignement requetes `_view`:
  - en mode reduce: envoi `reduce=true` + `group=true` (Exact) ou `group_level=n`.
  - en mode include docs: envoi `include_docs=true` + `reduce=false`.
- Alignement layout:
  - quand `Reduce` est actif, le layout est force sur `Metadata` (pas de `Table/JSON`, comme Fauxton).
  - le schema table de view conserve aussi `key` quand disponible pour les cas non-`include_docs`.
- Scope iframe patchee conserve:
  - aucun ajout sur `Permissions`, `Changes`, `Metadata` design doc (`N/A` inchanges).

## Session update (2026-02-17, layout/query-options parity)

- Alignement comportement header/layout comme Fauxton:
  - changement de layout `Table/Metadata/JSON` synchronise maintenant `include_docs`.
  - `Metadata` force `include_docs=false`; `Table/JSON` force `include_docs=true` (hors mode `reduce`).
- Alignement `Group Level` sur `_view`:
  - ajout de l'option `None` (`0`) en plus de `1..9` et `Exact`.
  - en mode `reduce`, la requete peut envoyer `group_level=0` (parite Fauxton).
- Correctif rendu metadata:
  - en resultats `reduce/group` sans `id`, la cellule `id` n'est plus un lien vide cliquable.

## Session update (2026-02-17, doc upload parity)

- Alignement `Upload Attachment` sur Fauxton:
  - un fichier selectionne est maintenant accepte meme si sa taille est `0` octet.
  - le bouton `Upload Attachment` se base sur la presence d'un nom de fichier, pas sur `size > 0`.

## Session update (2026-02-17, doc delete parity)

- Alignement `Delete` sur le comportement Fauxton:
  - la suppression utilise la version sauvegardee du document (`_id` / `_rev` d'origine),
    pas le brouillon JSON en cours d'edition.
  - impact: une edition locale non sauvegardee ne peut plus casser la suppression.

## Session update (2026-02-18, clone E2E)

- Validation E2E API `Clone Document` (base `alerts`) :
  - creation doc source -> clone -> ouverture clone -> sauvegarde clone : OK.
  - verification post-save: champs modifies persistes (`count=2`, `saved_after_clone=true`).
- Validation clone avec attachment:
  - clone d'un doc contenant un attachment stub retourne `412 missing_stub` (`Invalid attachment stub ...`),
    comportement coherent avec la logique Fauxton `doc.copy()`.
- Alignement UX clone:
  - au clic `Clone Document`, le dialogue se ferme immediatement avant execution (comme Fauxton).
  - en echec, une notification globale est affichee (`Could not duplicate document, reason: ...`).
- Limite de validation UI automatisee dans cette session:
  - Playwright est redirige vers `/login/?redirect=...` sans session admin partagee; le parcours UI click-by-click
    reste a confirmer en session authentifiee pour passer FS-010 en `DONE`.

## Session update (2026-02-18, cancel navigation parity)

- Alignement bouton `Cancel` sur l'editeur document:
  - retour direct vers `/admin/fs/{db}` (liste all docs), au lieu d'un `history.back()` contextuel.
  - comportement aligne avec le flux Fauxton (retour deterministic vers la liste).

## Session update (2026-02-18, clone UI final)

- Validation UI complete FS-010 en session admin (`admin/admin`) :
  - ouverture modal `Clone Document`,
  - edition ID cible,
  - soumission clone,
  - redirection automatique vers `/admin/fs/{db}/{newId}/`,
  - sauvegarde du clone via `Save Changes` (nouvelle revision visible).
- Nettoyage effectue:
  - suppression des documents de test `codex_clone_src_*` et `codex_clone_ui_*` dans `alerts`.

## Session update (2026-02-18, upload/attachments UI final)

- Validation UI complete FS-009 (`Upload Attachment`) en session admin:
  - ouverture dialogue,
  - fermeture par `Esc`,
  - fermeture par clic exterieur (backdrop),
  - bouton upload active uniquement quand un fichier est selectionne.
- Validation UI complete FS-011 (`View Attachments`) en session admin:
  - bouton visible seulement si `_attachments` present, masque sinon,
  - ouverture du menu, fermeture `Esc` et clic exterieur,
  - clic sur la piece jointe ouvre bien un nouvel onglet (`/convertigo/fullsync/{db}/{doc}/{file}`).

## Session update (2026-02-18, FS-007 final)

- Validation UI complete FS-007 en session admin:
  - creation doc de test depuis `/_new`,
  - edition JSON + `Save Changes` (rev passe a `2-*`),
  - test suppression `Delete` avec annulation puis confirmation.
- Validation reseau post-correctif:
  - suppression via `POST /convertigo/fullsync/alerts/_bulk_docs` (`201`),
  - verification immediate: `GET /convertigo/fullsync/alerts/{docid}` retourne `404`.

## Session update (2026-02-18, PouchDB size fallback)

- Ajustement FS-001 pour compatibilite CouchDB/PouchDB via servlet Convertigo:
  - quand `sizes.active` est absent (ou invalide), la colonne `Size` affiche maintenant `N/A`
    au lieu d'un placeholder visuel.
- Impact:
  - le rendu reste stable sans masquer dynamiquement la colonne,
  - la vue garde la meme structure en CouchDB et PouchDB.

## Session update (2026-02-18, prerender static Tomcat)

- Suppression de `src/routes/(app)/admin/fs/+layout.js` (`prerender = false`) pour ne plus exclure FS du build statique.
- Ajout de `entries()` sur les routes dynamiques FS avec segments sentinelles `_`
  (meme pattern que dashboard) pour generer les pages statiques placeholders:
  - `[database]`, `[database]/[docid]`, `[database]/_new`, `[database]/_find`, `[database]/_index`,
    `[database]/_design/[design]/_view/[view]`, `.../edit`.
- Alignement decoding param route:
  - `decodeRouteParam('_')` retourne maintenant `''` pour traiter le placeholder de prerender.
- Validation build:
  - `npm run build` OK; generation des chemins statiques FS:
    `admin/fs/_/`, `admin/fs/_/_/`, `admin/fs/_/_find/`, `admin/fs/_/_index/`,
    `admin/fs/_/_new/`, `admin/fs/_/_design/_/_view/_/(index|edit)`.

## Session update (2026-02-18, FS-006 final)

- Validation UI deep-link en session admin:
  - `/edit` -> `Cancel` revient sur la query view cible.
  - `/edit` -> `Save Document and then Build Index` persiste (`PUT` `201`) puis redirige vers la query view.
- Observation historique pendant cette passe:
  - des `HTTP 403` intermittents ont ete observes sur `_view` (deja suivi en FS-005) sans bloquer la parite route/UI FS-006.
- Nettoyage test:
  - suppression hard du design doc de test non appliquee en mode courant (`DELETE 200` sans effet, `_bulk_docs 201` sans effet),
    la view de test a ete retiree du design doc (`views = {}`) pour limiter l'impact visuel.

## Session update (2026-02-18, FS-005 query options stabilisees)

- Verification live relancee sur `/admin/fs/offchat_fullsync/_design/c8o/_view/hash/` apres correctif `queryScope`.
- Requetes observees:
  - `GET .../_view/hash?limit=21&skip=0&reduce=false` apres desactivation `Include Docs` (pas de requete de reset).
  - `GET .../_view/hash?limit=21&skip=1&reduce=false` apres `Skip=1` (pas de requete de reset vers `skip=0`).
  - `GET .../_view/hash?limit=21&skip=1&reduce=true&group=true` retourne `400` sur map-only view.
- UI:
  - message attendu affiche: `Reduce is invalid for map-only views.`
  - les `403` `_view` ne sont pas reproduits dans cette session de verification.

## Session update (2026-02-18, FS-006 edit overflow fix)

- Correctif UI applique sur l'ecran `Edit View` pour supprimer le debordement horizontal:
  - conteneurs du formulaire (`panel/form/fieldset/labels`) bornes en largeur (`min-width: 0`, `max-width: 100%`),
  - confinement horizontal du panel (`overflow-x: hidden`),
  - garde-fou largeur sur les wrappers Monaco.
- Validation runtime:
  - URL testee: `/admin/fs/offchat_fullsync/_design/Design_document/_view/alertView/edit/`
  - resultat: `scrollWidth == clientWidth` (pas de scroll horizontal global).

## Session update (2026-02-18, FS factorisation helpers feedback)

- Factorisation transversale FS:
  - nouveau module partage `src/routes/(app)/admin/fs/fullsync-feedback.js`.
  - centralise `fullSyncErrorMessage`, `showFullSyncError`, `showFullSyncSuccess`.
- Pages branchees sur ce helper:
  - `FullSyncDatabasesPage.svelte`,
  - `FullSyncDatabasePage.svelte`,
  - `FullSyncDocumentPage.svelte`.
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.

## Session update (2026-02-18, FS factorisation helpers JSON)

- Factorisation transversale FS:
  - nouveau module partage `src/routes/(app)/admin/fs/fullsync-json.js`.
  - centralise `parseFullSyncJson`, `parseFullSyncJsonSilent`, `fullSyncPretty`.
- Pages branchees sur ce helper:
  - `FullSyncDatabasePage.svelte`,
  - `FullSyncDocumentPage.svelte`.
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.

## Session update (2026-02-18, FS factorisation helpers liens externes)

- Factorisation transversale FS:
  - nouveau module partage `src/routes/(app)/admin/fs/fullsync-links.js`.
  - centralise:
    - URLs documentation CouchDB (`document`, `mango`, `_all_dbs`),
    - ouverture externe securisee (`openFullSyncLink`),
    - ouverture d'un payload JSON (`openFullSyncJsonPayload`).
- Pages branchees sur ce helper:
  - `FullSyncDatabasePage.svelte`,
  - `FullSyncDocumentPage.svelte`,
  - `FullSyncDatabasesPage.svelte` (URL doc `_all_dbs`).
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.

## Session update (2026-02-18, FS factorisation modal confirmation)

- Factorisation transversale FS:
  - nouveau module partage `src/routes/(app)/admin/fs/fullsync-modal.js`.
  - centralise:
    - recuperation optionnelle du context `modalYesNo` (`getFullSyncConfirmModal`),
    - confirmation unifiee (`openFullSyncConfirmation`) avec fallback `window.confirm`.
- Pages branchees sur ce helper:
  - `FullSyncDatabasePage.svelte`,
  - `FullSyncDatabasesPage.svelte`,
  - `FullSyncDocumentPage.svelte`.
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.

## Session update (2026-02-18, FS factorisation path encoders)

- Factorisation transversale FS:
  - encodeurs de chemins exportes depuis `src/routes/(app)/admin/fs/fullsync-api.js`:
    - `encodeFullSyncDocPath`,
    - `encodeFullSyncDesignDocPath`.
- Pages recablees:
  - `FullSyncDocumentPage.svelte` reutilise `encodeFullSyncDocPath` (suppression helper local),
  - `FullSyncDatabasePage.svelte` reutilise `encodeFullSyncDesignDocPath` (suppression helper local).
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.

## Session update (2026-02-18, FS factorisation feedback hooks + guard SSR)

- Factorisation supplementaire:
  - `fullsync-feedback.js` expose maintenant `createFullSyncFeedback(setLastError)`.
  - suppression des wrappers locaux `showError/showSuccess` dans:
    - `FullSyncDatabasesPage.svelte`,
    - `FullSyncDatabasePage.svelte`,
    - `FullSyncDocumentPage.svelte`.
- Correctif robustesse SSR/prerender:
  - deplacement des helpers JSON (`parseJsonSilent`, `pretty`, `parseJson`) avant leur premiere utilisation runtime,
  - evite l'erreur `ReferenceError: Cannot access 'parseJsonSilent' before initialization` observee au build sur:
    - `/admin/fs/_/_new/`,
    - `/admin/fs/_/_/`.
- Validation:
  - `npm run check:admin` OK (0 error, 0 warning).
  - `npm run build` OK (plus d'erreur `parseJsonSilent` en prerender).

## Session update (2026-02-18, FS nettoyage routes legacy)

- Suppression de la route legacy d'edition view `.../_edit` (compat iframe historique retiree).
- Route supportee desormais:
  - `.../_design/{ddoc}/_view/{view}/edit`.
- Nettoyage associe:
  - suppression des fichiers route `_edit/+page.js` et `_edit/+page.svelte`,
  - matrice mise a jour pour ne plus annoncer `/_edit`.
- Validation:
  - `npm run check:admin` OK.
  - `npm run build` OK.
