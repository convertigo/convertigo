# FS Parity Matrix (Iframe -> Native)

## Objectif

Suivre la parite **feature / look / URL** entre:

- mode iframe: `/admin/fullsync/` + URL interne Fauxton dans l'iframe
- mode natif: `/admin/fs/...`

Mise a jour iterative: **une page a 100% avant de passer a la suivante**.

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

## Matrice de correspondance

| ID     | Page / Ecran                         | URL host iframe    | URL interne iframe (reference)                                              | URL native FS cible                                | URL parity       | Look parity            | Feature parity     | Statut      | Sorties vers pages non traitees | Notes                                                                                                |
| ------ | ------------------------------------ | ------------------ | --------------------------------------------------------------------------- | -------------------------------------------------- | ---------------- | ---------------------- | ------------------ | ----------- | ------------------------------- | ---------------------------------------------------------------------------------------------------- |
| FS-001 | Liste des bases                      | `/admin/fullsync/` | `/convertigo/admin_/_utils/#`                                               | `/admin/fs/`                                       | OK               | DELTA (intentionnelle) | OK (scope convenu) | DONE        | FS-002                          | Deltas valides: pas de colonne Partitioned, pas d'action lock/permissions, ajout Search + Size + Seq |
| FS-002 | Base: All Docs (`{db}`)              | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_all_docs`                        | `/admin/fs/{db}`                                   | OK (segment URL) | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-003, FS-005, FS-007, FS-008  | Audit en cours: alignement sidebar/toolbar/options/selection                                         |
| FS-003 | Mango Query (`_find`)                | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_find`                            | `/admin/fs/{db}/_find`                             | OK               | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-004, FS-008                  | Run/Explain + history + modes de rendu                                                               |
| FS-004 | Mango Indexes (`_index`)             | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_index`                           | `/admin/fs/{db}/_index`                            | OK               | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-003, FS-008                  | Create index + selection + delete                                                                    |
| FS-005 | Query d'une vue (`_design/_view`)    | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/_design/{ddoc}/_view/{view}`     | `/admin/fs/{db}/_design/{ddoc}/_view/{view}`       | OK               | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-006, FS-007, FS-008          | URL segmentee obligatoire; robustesse ajoutee sur 401/403 initiaux (retry auth)                      |
| FS-006 | Edition d'une vue                    | `/admin/fullsync/` | `/convertigo/admin_/_utils/#database/{db}/_design/{ddoc}/_view/{view}/edit` | `/admin/fs/{db}/_design/{ddoc}/_view/{view}/_edit` | DELTA (segment)  | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-005                          | Route iframe confirmee; `Cancel` revient bien a la page de view                                      |
| FS-007 | Edition document (`{docid}`)         | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/{docid}`                         | `/admin/fs/{db}/{docid}`                           | OK               | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-009, FS-010, FS-011          | Ecran sans sidebar (comme convenu), actions doc presentes                                            |
| FS-008 | Nouveau document (`_new`)            | `/admin/fullsync/` | `/convertigo/admin_/_utils/#/database/{db}/_new`                            | `/admin/fs/{db}/_new`                              | OK               | OK                     | OK                 | DONE        | FS-007                          | Route dediee + titre `New Document` + bouton `Create Document`                                       |
| FS-009 | Dialogue Upload Attachment           | `/admin/fullsync/` | Depuis ecran doc (`Upload Attachment`)                                      | Depuis ecran doc (`Upload Attachment`)             | N/A              | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | -                               | Ouverture du dialogue OK; upload reel encore a valider sur bout en bout                              |
| FS-010 | Action Clone Document                | `/admin/fullsync/` | Depuis ecran doc (`Clone Document`)                                         | Depuis ecran doc (`Clone Document`)                | N/A              | IN_PROGRESS            | IN_PROGRESS        | IN_PROGRESS | FS-007                          | Dialogue clone + ID auto presents; valider parcours complet apres confirmation                       |
| FS-011 | View Attachments (liste + ouverture) | `/admin/fullsync/` | Depuis ecran doc (`View Attachments`)                                       | Depuis ecran doc (`View attachments`)              | N/A              | IN_PROGRESS            | OK (scope courant) | IN_PROGRESS | -                               | Liste visible + clic piece jointe ouvre bien un nouvel onglet                                        |

## Validation detaillee FS-001 (DONE)

- Navigation row click: `offchat_fullsync` ouvre `http://localhost:5173/admin/fs/offchat_fullsync/`.
- Navigation via recherche: valeur `alerts` + action Open ouvre `http://localhost:5173/admin/fs/alerts/`.
- Pagination: page 2 OK (`Showing 21–29 of 29 databases`), next/previous operants.
- Delete row: dialogue Yes/No present; `No` annule sans suppression.
- Liens header: `JSON` pointe `.../convertigo/fullsync/_all_dbs` (new tab), doc pointe la doc CouchDB (new tab).
- Tri de colonnes (Docs/Size/Seq): tri interactif fonctionnel.

## Audit FS-002 en cours

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
- A finaliser pour passer `DONE`:
  - verif exhaustive des interactions toolbar/options (comportement identique a l'iframe).
  - verif reseau `_all_docs` (parametres startkey/endkey/include_docs/limit/skip/descending selon mode).
  - verif look pixel-level (densite/lignes/alignements) desktop + mobile.

### Avancement FS-002 (iteration en cours)

- Corrige: separation `Limit` (Query Options) vs `Documents per page` (footer).
  - `Query Options > Limit` par defaut = `None`.
  - `Documents per page` reste a `20` par defaut.
- Corrige: requete deep-link view route alignee Fauxton.
  - natif: `/_design/{ddoc}/_view/{view}?include_docs=true&limit=21&skip=0&conflicts=true&reduce=false`
  - reference iframe observee: meme pattern.
- Corrige: listage design docs aligne.
  - natif: `/_all_docs?include_docs=true&limit=501&startkey=%22_design%2F%22&endkey=%22_design0%22`
- Corrige: all docs par defaut aligne.
  - natif: `/_all_docs?limit=21&skip=0`

### Reste FS-002 avant DONE

- Validation manuelle exhaustive desktop/mobile des etats toolbar (Options ouvert/ferme, Include docs on/off, Descending, Skip).
- Validation pagination (next/prev) avec et sans `Query Options > Limit`.

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

- Validation visuelle et comportementale complete desktop/mobile vs iframe sur:
  - table/json/explain,
  - toolbar resultat en mode explain,
  - navigation bouton `Manage Indexes` vers FS-004.
  - finition look des selects d'en-tete colonnes (hauteur/alignement fin).

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

- Validation visuelle fine desktop/mobile:
  - densite verticale cartes JSON indexes,
  - alignement toolbar (select-all, delete, create document),
  - coherence tailles boutons vs references iframe.

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
  - colonnes visibles initiales `key` / `value` (2 sur 3).
  - colonne copy reintroduite en mode table.
  - selection de lignes desactivee tant que `Include Docs` est off (comme attendu sans `_rev`).
- Correctif transversal applique pendant l'audit:
  - logo topbar passe en `convertigo:logo` (plus de chemin relatif casse sur routes profondes).

### Reste FS-005 avant DONE

- Verif exhaustive toolbar Query Options sur une view:
  - comportement `reduce`, `include_docs`, `descending`, `skip`, `limit`.
- Verif actions lignes:
  - copy/open sur resultats table par defaut.
  - select/delete multi uniquement apres activation `Include Docs`.

### Validation FS-005 complementaire (session courante)

- Comparee au rendu iframe `#/database/offchat_fullsync/_design/OffChat/_view/getChat`:
  - `Table` affiche la selection de colonnes (par defaut `key` + `value`).
  - `Metadata` affiche les colonnes fixes `id / key / value`.
  - bascule `Table / Metadata / JSON` debloquee sur route `_view` (plus de reset force sur `Table`).
  - clic design doc dans la sidebar ouvre bien l'accordion cible.
- Limite de validation locale:
  - sur ma session native actuelle, les routes `_view` retournent `HTTP 403` (alors que l'iframe reste alimentee),
    ce qui bloque la validation E2E de certaines actions sur les resultats de view (selection/suppression sur data reelle).

### Validation FS-002 complementaire (session courante)

- `all_docs` mode `JSON` aligne sur la presentation Mango JSON:
  - cartes JSON par document,
  - switch de selection par ligne,
  - action d'ouverture du document dans l'entete de carte.

## Avancement FS-006 (iteration en cours)

- URL edit capturee en iframe au clic sur l'action view (wrench):
  - `#/database/offchat_fullsync/_design/OffChat/_view/checkUsername/edit`
- URL edit native correspondante:
  - `/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/_edit/`
- Comportement compare:
  - le formulaire `Design Document / Index name / Map / Reduce` est present des 2 cotes.
  - `Cancel` retourne bien sur la page de query de la view dans les deux versions.

### Reste FS-006 avant DONE

- Aligner la convention URL (`edit` vs `_edit`) ou documenter le delta final.
- Verifier visuellement la densite/hauteur Monaco (cas de hauteur compressee deja signales).
- Valider `Save` sur une view de test (sans impact metier) puis retour automatique.

### Validation FS-006 complementaire (session courante)

- Correctif applique sur le composant Monaco partage pour eviter les rendus "editor ecrase" lors de transitions de page/layout.
- Recontrole visuel sur `/admin/fs/offchat_fullsync/_design/c8o/_view/hash/_edit/` et
  `/admin/fs/offchat_fullsync/_design/c8o/_view/checkAcl/_edit/`: hauteur editeur stable.

## Avancement FS-007 (iteration en cours)

- URL de doc existant verifiee:
  - iframe: `#/database/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy`
  - natif: `/admin/fs/offchat_fullsync/56226c43-f189-4979-82fd-aef13f6de8c5_copy/`
- Controles presents et compares:
  - `Save Changes`, `Cancel`, `View attachments`, `Upload Attachment`, `Clone Document`, `Delete`.
  - editeur JSON Monaco present.

### Reste FS-007 avant DONE

- Valider `Save Changes` (modif + persistance + retour attendu).
- Valider `Delete` (dialogue + annulation + confirmation) sur doc de test seulement.
- Verifier details look header (tailles police + largeur utile avant ellipsis).

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

- FS-009: valider upload effectif (selection fichier + requete + apparition dans menu attachments).
- FS-010: valider flux complet clone (confirmation -> ouverture nouveau doc -> save).
- FS-011: verifier etat vide (aucune attachment) + fermeture menu/dialogue clavier/souris.

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
- Native view edit (exemple): `http://localhost:5173/admin/fs/offchat_fullsync/_design/OffChat/_view/getChat/_edit/`
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
