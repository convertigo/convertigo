# Changelog

## 8.3.11

#### Improvements:

- [#998](https://github.com/convertigo/convertigo/issues/998) [Engine] The usage monitor log now includes the current session count for better runtime visibility
- [#1006](https://github.com/convertigo/convertigo/issues/1006) [Studio] Naming a component now correctly initializes its main property value for all affected Convertigo components

#### Bug Fixes:

- [#898](https://github.com/convertigo/convertigo/issues/898) [Security] Fixed, expression injection vectors have been removed and inputs are now properly sanitized
- [#993](https://github.com/convertigo/convertigo/issues/993) [Security] Fixed, the “Add Symbol” feature is now protected against XSS attacks
- [#994](https://github.com/convertigo/convertigo/issues/994) [Studio] Fixed, adding an HTTP Connector via drag-and-drop from the Palette no longer triggers an unexpected popup message
- [#995](https://github.com/convertigo/convertigo/issues/995) [Studio] Fixed, clearing the Palette search field on macOS now works with a single click
- [#1000](https://github.com/convertigo/convertigo/issues/1000) [Engine] Fixed, deprecated Xalan-XSLTC usage has been removed to prevent warnings and future incompatibilities
- [#1001](https://github.com/convertigo/convertigo/issues/1001) [Engine] Fixed, the HttpConnector now works correctly again when used with XML templates (regression resolved)
- [#1003](https://github.com/convertigo/convertigo/issues/1003) [Studio] Fixed, renaming an empty Sequence_JS step no longer inserts the step name on the first line of code
- [#1004](https://github.com/convertigo/convertigo/issues/1004) [Documentation] Fixed, the documentation for the “SharedAction Variable” Palette component has been corrected
- [#1007](https://github.com/convertigo/convertigo/issues/1007) [NGX] Fixed, the `ion-range` component now correctly accepts a `pinFormatter` function

---

## 8.3.10

#### Improvements:

- [#986](https://github.com/convertigo/convertigo/issues/986) [NGX] A usage hint has been added to the Select container (back-ported to all templates)
- [#987](https://github.com/convertigo/convertigo/issues/987) [Studio] The CRUD SQLTransaction generator now also produces the corresponding SQLTransaction schema

#### Bug Fixes:

- [#984](https://github.com/convertigo/convertigo/issues/984) [Studio] Fixed, the CRUD SQLTransaction generator now properly quotes PostgreSQL column names, avoiding issues with reserved words and casing
- [#988](https://github.com/convertigo/convertigo/issues/988) [Engine] Fixed, the "Namespaces removed:" message now uses the standard truncation helper (even in DEBUG) to prevent oversized log entries
- [#989](https://github.com/convertigo/convertigo/issues/989) [Engine] Fixed, log message truncation is configurable via settings, allowing you to control maximum logged payload size
- [#990](https://github.com/convertigo/convertigo/issues/990) [Studio] Fixed, revealing nodes preserves the Project Explorer expansion state instead of collapsing the tree

---

## 8.3.9

#### Improvements:

- [#968](https://github.com/convertigo/convertigo/issues/968) [MobileBuilder] Logs now trace the full UI Action Stack (including nested actions) so you can follow UI execution step-by-step
- [#969](https://github.com/convertigo/convertigo/issues/969) [NGX] The NgxApp Split Pane `breakpoint` property now accepts TypeScript expressions/bindings (e.g. `[breakpoint]="isAdmin ? 'xl' : 'md'"`)

#### Bug Fixes:

- [#966](https://github.com/convertigo/convertigo/issues/966) [NoCode DB] Fixed, filters built from filter groups now generate the correct `filter` content in the sequenceJS step, producing proper queries
- [#967](https://github.com/convertigo/convertigo/issues/967) [LDAPStep] Fixed, LDAP searches now work even when the LDAP URL includes an explicit port; the port in the URL no longer breaks the search
- [#970](https://github.com/convertigo/convertigo/issues/970) [Studio] Fixed, the Reference view **Used by** section now lists all usages of a component instead of missing some entries
- [#971](https://github.com/convertigo/convertigo/issues/971) [SSL] Fixed, client certificate aliases can now be retrieved from USB tokens, restoring mutual-TLS selection
- [#974](https://github.com/convertigo/convertigo/issues/974) [CI] Fixed, `generateMobileBuilder` no longer throws an NPE when the project has no Mobile App; the task now exits gracefully

---

## 8.3.8

#### Improvements:

- [#963](https://github.com/convertigo/convertigo/issues/963) [Studio] The SQL Connector editor now assists in creating CRUD SQL Transactions and Sequences (compatible with AgGrid_CRUD from lib_ExtendedComponents_ui_ngx)
- [#965](https://github.com/convertigo/convertigo/issues/965) [Studio] The Reference View now handles NGX objects and references (shared actions/components, Call_Sequence, FullSync, etc.)

#### Bug Fixes:

- [#960](https://github.com/convertigo/convertigo/issues/960) [Studio] Fixed, dragging items from the Project tree on macOS no longer raises exceptions
- [#961](https://github.com/convertigo/convertigo/issues/961) [Studio] Fixed, renaming a Sequence with a case-only change now correctly updates the file name on Windows
- [#962](https://github.com/convertigo/convertigo/issues/962) [Studio] Fixed, deployment now succeeds even when the target HTTPS server uses an untrusted certificate

---

## 8.3.7

#### Improvements:

- [#957](https://github.com/convertigo/convertigo/issues/957) [Studio] Mobile preview screencast is no longer shown for the Frontend debug

#### Bug Fixes:

- [#955](https://github.com/convertigo/convertigo/issues/955) [FullSync] Fixed, GetAttachment transaction now correctly downloads application/json content-type files as expected
- [#956](https://github.com/convertigo/convertigo/issues/956) [Studio] Fixed, NGX Frontend debug now displays devtool debugger content instead of a blank content
- [#958](https://github.com/convertigo/convertigo/issues/958) [Studio] Fixed, Palette is now correctly populated even if the Studio starts with the registration wizard

---

## 8.3.6

#### Improvements:

- [#949](https://github.com/convertigo/convertigo/issues/949) [Studio] A new Administration View now embeds the Administration console directly inside Studio, so it no longer opens in an external browser

#### Bug Fixes:

- [#688](https://github.com/convertigo/convertigo/issues/688) [Studio] Fixed, you can now select the target server on small screens—the advertising banner no longer blocks the list
- [#946](https://github.com/convertigo/convertigo/issues/946) [HTTP Connector] Fixed, multipart file names are now correctly encoded in the `Content-Disposition` header
- [#947](https://github.com/convertigo/convertigo/issues/947) [Engine] Fixed, parallel requestables initializing the same HTTP session no longer create ghost sessions, session handling is now thread-safe
- [#950](https://github.com/convertigo/convertigo/issues/950) [Admin] Fixed, the Administration UI now uses local fonts, eliminating slowdowns when the browser has no Internet access
- [#951](https://github.com/convertigo/convertigo/issues/951) [Admin] Fixed, the log viewer now refresh correctly in Microsoft Edge
- [#953](https://github.com/convertigo/convertigo/issues/953) [Admin] Fixed, the Symbols page no longer throws an error when a project fails to load (for example, if it was created with a newer Convertigo version)
- [#954](https://github.com/convertigo/convertigo/issues/954) [NGX] Fixed, using a ‘$’ sign in Smart type “TS” properties and class code no longer breaks the code generator


## 8.3.6

#### Improvements:

- [#949](https://github.com/convertigo/convertigo/issues/949) [Studio] Add an AdminView with integrated admin instead of opening in the external browser

#### Bug Fixes:

- [#688](https://github.com/convertigo/convertigo/issues/688) Deploy: advertisin prevents to select the server in the list on small screens
- [#946](https://github.com/convertigo/convertigo/issues/946) [HTTP Connector] Multipart filename not well encoded in Content-Disposition
- [#947](https://github.com/convertigo/convertigo/issues/947) [Engine] Ghost session can occur if parallel requestables initialize the same HTTP session
- [#950](https://github.com/convertigo/convertigo/issues/950) [Admin] Reference external fonts can slowdown administration loading if browser hasn't Internet access
- [#951](https://github.com/convertigo/convertigo/issues/951) [Admin] Log viewer can fail on Edge
- [#953](https://github.com/convertigo/convertigo/issues/953) [Admin] NPE error on Symbols page if a project cannot load (caused by a superior version for example)

---

## 8.3.5

#### Improvements:

- [#939](https://github.com/convertigo/convertigo/issues/939) [Studio] SQL Transaction queries now open directly in the editor when you double-click the transaction in the treeview

#### Bug Fixes:

- [#859](https://github.com/convertigo/convertigo/issues/859) [NGX] Fixed, binding a Form control value to a Call_Sequence variable now preserves safe operators and generates correct code
- [#933](https://github.com/convertigo/convertigo/issues/933) [Studio] Fixed, exporting a project as a `.car` on macOS now proposes the correct file name (single `.car` extension)
- [#936](https://github.com/convertigo/convertigo/issues/936) [Studio] Fixed, saving code in the MobileBuilder editor now regenerates the code when the parent tree object has changed
- [#937](https://github.com/convertigo/convertigo/issues/937) [Studio] Fixed, drag-and-drop from the Source picker onto beans with multiple JsonField properties works again (regression introduced in 8.3.4)
- [#938](https://github.com/convertigo/convertigo/issues/938) [FullSync] Fixed, fail-safe startup now renews the `fsclient` after a port change, restoring PouchDB connectivity
- [#940](https://github.com/convertigo/convertigo/issues/940) [Admin] Fixed, column sorting is now preserved after data refresh
- [#942](https://github.com/convertigo/convertigo/issues/942) [Studio] Fixed, closing the “Manage Modules” window with the X button no longer triggers an unnecessary rebuild
- [#943](https://github.com/convertigo/convertigo/issues/943) [Studio] Fixed, focusing a design document from search results no longer breaks the treeview

---

## 8.3.4

#### Improvements:

- [#924](https://github.com/convertigo/convertigo/issues/924) [Studio] The Convertigo object search is now integrated with the Eclipse search wizard and view
- [#926](https://github.com/convertigo/convertigo/issues/926) [HTTP Connector] The "no Pool" mode now automatically adds a "Connection: close" header
- [#929](https://github.com/convertigo/convertigo/issues/929) [Studio] Memory usage has been optimized for better performance
- [#931](https://github.com/convertigo/convertigo/issues/931) [Studio] The NGXComponent Source picker now sorts Local and Global variables by name

#### Bug Fixes:

- [#921](https://github.com/convertigo/convertigo/issues/921) [Studio] Fixed, adding a Mobile Application to a Convertigo project no longer triggers errors
- [#922](https://github.com/convertigo/convertigo/issues/922) [Studio] Fixed, Page Hints are now applied when adding a new Page via the Contextual Menu
- [#925](https://github.com/convertigo/convertigo/issues/925) [Admin] Fixed, the Logs page now displays correctly after refreshing the page
- [#928](https://github.com/convertigo/convertigo/issues/928) Fixed, copying elements in the Treeview no longer triggers an application rebuild
- [#930](https://github.com/convertigo/convertigo/issues/930) [Engine] Fixed, the server no longer performs excessive GIT fetch/reset operations on startup
- [#932](https://github.com/convertigo/convertigo/issues/932) Fixed, Convertigo now returns a proper error structure when a file variable exceeds the maximum permitted size

---

## 8.3.3

#### Improvements:

- [#870](https://github.com/convertigo/convertigo/issues/870) [Studio] The Palette documentation pane has been restyled
- [#885](https://github.com/convertigo/convertigo/issues/885) [Studio] FlatLaf swing theme restored for the Rhino JS Backend debugger
- [#886](https://github.com/convertigo/convertigo/issues/886) [Studio] Multiple optimizations have been made to improve responsiveness
- [#888](https://github.com/convertigo/convertigo/issues/888) [Docker] Admin user/password set in `engine.properties` now take priority over environment variables
- [#895](https://github.com/convertigo/convertigo/issues/895) [Studio] NPE no longer occurs when building multiple schemas for a project
- [#901](https://github.com/convertigo/convertigo/issues/901) [Studio] Removed the warning message logs when opening the NoCode Databases view
- [#905](https://github.com/convertigo/convertigo/issues/905) [Studio] Stub from Baserow now generates symbols for tables' IDs
- [#912](https://github.com/convertigo/convertigo/issues/912) [Studio] Importing variables from a target sequence in an NGX Call_Sequence action component now also imports comments
- [#913](https://github.com/convertigo/convertigo/issues/913) [Studio] Stub from Baserow now generates JSON filters from views and supports composed filters

#### Bug Fixes:

- [#871](https://github.com/convertigo/convertigo/issues/871) [HTTP] Fixed, `__header_` variables now properly use `HttpName` to override the real header name
- [#874](https://github.com/convertigo/convertigo/issues/874) [Engine] Fixed, the requestable timeout is no longer doubled on fast machines
- [#875](https://github.com/convertigo/convertigo/issues/875) [SQL] Fixed, retrying a SQL query now respects the autocommit policy
- [#876](https://github.com/convertigo/convertigo/issues/876) [Docker] Fixed, `gosu` setup no longer adds critical CVEs
- [#887](https://github.com/convertigo/convertigo/issues/887) [Engine] Fixed, random deadlock on project migration no longer occurs
- [#889](https://github.com/convertigo/convertigo/issues/889) [Studio] Fixed, the default Dark theme is now automatically adapted for Light Windows environment
- [#890](https://github.com/convertigo/convertigo/issues/890) [Studio] Fixed, no more NPE for scheduled FS sequences when the Connector Editor Part is opened
- [#892](https://github.com/convertigo/convertigo/issues/892) [Baserow lib] Fixed, the filter generator now properly checks for variables that can be null
- [#896](https://github.com/convertigo/convertigo/issues/896) [Studio] Fixed, renaming a project containing “-” no longer leads to unexpected behavior
- [#897](https://github.com/convertigo/convertigo/issues/897) [Studio] Fixed, “Widget is disposed” error no longer occurs when executing sequences
- [#900](https://github.com/convertigo/convertigo/issues/900) [Studio] Fixed, renaming a project now updates the listen Directive Source name
- [#903](https://github.com/convertigo/convertigo/issues/903) [Studio] Fixed, the Baserow CRUD Delete sequence now correctly returns `success : false`
- [#904](https://github.com/convertigo/convertigo/issues/904) [Studio] Fixed, renaming a shared component’s variable no longer updates all inputs sharing the same substring in source mode
- [#906](https://github.com/convertigo/convertigo/issues/906) [Engine] Fixed, the Reference Manager now reports a 404 error for missing resources
- [#908](https://github.com/convertigo/convertigo/issues/908) [Studio] Fixed, a warning is now displayed if the project saving fails
- [#909](https://github.com/convertigo/convertigo/issues/909) [Studio] Fixed, in the NGX Source wizard, the schema is now correct in the Iterator Source for a Sequence using a Read CSV step
- [#916](https://github.com/convertigo/convertigo/issues/916) [Studio] Fixed, the "Convertigo Server Endpoint" wizard now manage starting Studio port

---

## 8.3.2

#### Improvements:

- [#860](https://github.com/convertigo/convertigo/issues/860) [NGX] Handle the Zoneless template
- [#861](https://github.com/convertigo/convertigo/issues/861) [Studio] Add a template for GitLab CI/CD

#### Bug Fixes:

- [#856](https://github.com/convertigo/convertigo/issues/856) [NGX] Fixed, Toast color property works properly
- [#858](https://github.com/convertigo/convertigo/issues/858) [Admin] Fixed, allow to kill sessions with roles
- [#862](https://github.com/convertigo/convertigo/issues/862) [CI] Fixed, "include" parameters handled for the "car" task
- [#864](https://github.com/convertigo/convertigo/issues/864) [CI] Fixed, Gradle can now build projects with ngx tpl < 8.3
- [#865](https://github.com/convertigo/convertigo/issues/865) [Engine] Fixed, restored handling of requests with __nolog
- [#869](https://github.com/convertigo/convertigo/issues/869) [NGX] Fixed, mobile picker can now be used with array data

---

## 8.3.1

#### Improvements:

- [#841](https://github.com/convertigo/convertigo/issues/841) [NGX] Can now use safe operators when generating binding sources on Array

#### Bug Fixes:

- [#840](https://github.com/convertigo/convertigo/issues/840) [Studio] Fixed, The Baserow View now generates stub schema of "lookup" type as an array
- [#844](https://github.com/convertigo/convertigo/issues/844) [NGX] Fixed, Shared components files are now copied to main project when using multiple nested shared components from another project
- [#845](https://github.com/convertigo/convertigo/issues/845) [Admin] Fixed, Prevent broken password properties if a property is set by JAVA_OPTS and changed in the Admin console 
- [#846](https://github.com/convertigo/convertigo/issues/846) [Studio] Fixed, Can now rename a project imported from Git
- [#848](https://github.com/convertigo/convertigo/issues/848) [NGX] Fixed, The "Include in auto menu" Page property now hide entry in AutoMenu
- [#849](https://github.com/convertigo/convertigo/issues/849) [Studio] Fixed, Tutorial now detects success steps
- [#853](https://github.com/convertigo/convertigo/issues/853) [Studio] Fixed, Renaming a project now update the Page property of ion-tab-button
- [#855](https://github.com/convertigo/convertigo/issues/855) [Studio] Fixed, node based subprocess can be terminated on a fresh Windows 11 (MB builder + PouchDB)

---

## 8.3.0

#### New Features:

- [#132](https://github.com/convertigo/convertigo/issues/132) [Studio] Add an 'Update Mobile Builder Templates' menu for Project Reference beans
- [#794](https://github.com/convertigo/convertigo/issues/794) [NGX] Include a HTML style editor inside the Mobile Builder editor
- [#796](https://github.com/convertigo/convertigo/issues/796) [Studio] Add a debugger interface for backend scripts (Sequences / Transactions)
- [#800](https://github.com/convertigo/convertigo/issues/800) [NGX] TPL 8.3.0 with Ionic 7
- [#814](https://github.com/convertigo/convertigo/issues/814) [Studio] Ship Studio with the Eclipse MarketPlace Plugin, ready for Copilot
- [#816](https://github.com/convertigo/convertigo/issues/816) [NGX] Add an Angular virtual scroll bean

#### Improvements:

- [#292](https://github.com/convertigo/convertigo/issues/292) Differentiate App/Page needed imports/modules/providers/packages
- [#749](https://github.com/convertigo/convertigo/issues/749) [Mobile] Application have icon name and color theme properties to change manifest.json
- [#750](https://github.com/convertigo/convertigo/issues/750) Update bases (Eclipse 2023-12 / Tomcat 9.0.89 / JVM 21.0.3 / Node ...)
- [#773](https://github.com/convertigo/convertigo/issues/773) [NGX] SearchBar should be bindable
- [#820](https://github.com/convertigo/convertigo/issues/820) [NGX] Source Picker for Sequence is a collapsable treeview
- [#829](https://github.com/convertigo/convertigo/issues/829) [Documentation] Generate the online documentation for NGX components
- [#830](https://github.com/convertigo/convertigo/issues/830) [Studio] Sort Palette categories by alphanumeric order
- [#831](https://github.com/convertigo/convertigo/issues/831) [Android] Choose for package APK or bundle AAB for Android release build
- [#835](https://github.com/convertigo/convertigo/issues/835) [HTTP Download Transaction] Property to get filename from the Content-Disposition header
- [#836](https://github.com/convertigo/convertigo/issues/836) [NGX] Added hints for Image and LazyImage components
- [#838](https://github.com/convertigo/convertigo/issues/838) [NGX] Added hints for PageComponent

#### Bug Fixes:

- [#808](https://github.com/convertigo/convertigo/issues/808) [Mobile] Fixed, template the endpoint in cordova config.xml
- [#813](https://github.com/convertigo/convertigo/issues/813) [NGX] Fixed, tooltips component can be used in ng-template
- [#815](https://github.com/convertigo/convertigo/issues/815) [NGX] Fixed, segment /:param will not fails if value is 0
- [#817](https://github.com/convertigo/convertigo/issues/817) [Studio] Fixed, DnD Sequence on a Form component keeps 'Control name' property
- [#818](https://github.com/convertigo/convertigo/issues/818) [NGX] Fixed, Ngx source picker gives available options when selecting iterator sources
- [#819](https://github.com/convertigo/convertigo/issues/819) [NGX] Fixed, copying and pasting keeps source picker binds
- [#821](https://github.com/convertigo/convertigo/issues/821) [Studio] Fixed, no more Invalid Thread Access when importing a project from GIT with <project>=<git url>
- [#822](https://github.com/convertigo/convertigo/issues/822) [NGX] Fixed, TreeviewComponent works anymore with Angular 16
- [#824](https://github.com/convertigo/convertigo/issues/824) [Studio] Fixed, generating Schema from connector data success even if documentation of imported REST Swagger contains illegal characters
- [#825](https://github.com/convertigo/convertigo/issues/825) [NGX] Fixed, UseShared class attribute and its Style content are added in the scss file
- [#826](https://github.com/convertigo/convertigo/issues/826) [Engine] Fixed, enable "SSL debug" now print SSL debug in stdout after a JVM reboot
- [#827](https://github.com/convertigo/convertigo/issues/827) [Engine] Fixed, variable __header_ log visibility now respected for the input document log (like other variables)
- [#828](https://github.com/convertigo/convertigo/issues/828) [NGX] Fixed, Agm core components are now deprecated since Angular 16
- [#832](https://github.com/convertigo/convertigo/issues/832) [Download HTTP Transaction] Fixed, Doesn't fail if no lastModified header is returned
- [#833](https://github.com/convertigo/convertigo/issues/833) [Palette] Fixed, SharedComponent with the same name in different projects are all visibles
- [#834](https://github.com/convertigo/convertigo/issues/834) [Studio] Fixed, Adding and use a sharedComponent appears in page even if the Mobile Builder tab is already opened
- [#837](https://github.com/convertigo/convertigo/issues/837) [Studio] Fixed, Baserow view stub generator now generates table_id in quotes
- [#839](https://github.com/convertigo/convertigo/issues/839) [NGX] Fixed, App sources are well updated when Cut/Paste or Dnd/Move between different pages

---

## 8.2.7

#### Improvements:

- [#803](https://github.com/convertigo/convertigo/issues/803) [Analytics] Upgrade Google Analytics to G4
- [#804](https://github.com/convertigo/convertigo/issues/804) [Studio] Option to create sequences with call transaction when importing from Web Services
- [#806](https://github.com/convertigo/convertigo/issues/806) [Studio] Baserow stub wizard handle upload file via url for Create and Update

#### Bug Fixes:

- [#805](https://github.com/convertigo/convertigo/issues/805) [Engine] Fixed, allow blank key of JSON parsed by Convertigo
- [#810](https://github.com/convertigo/convertigo/issues/810) [Studio] Fixed, 'Export variables to main sequence' on a Call_transaction Step keeps the 'Is file upload' value on the variable
- [#812](https://github.com/convertigo/convertigo/issues/812) [OpenApi] Fixed, REST WS reference creation trough a YAML prevents NPE

---

## 8.2.6

#### Bug Fixes:

- [#793](https://github.com/convertigo/convertigo/issues/793) [FlashUpdate] Fixed, flashupdate from Android APK is ok
- [#798](https://github.com/convertigo/convertigo/issues/798) [NGX] Fixed, bypass Close actions error like 'overlay does not exist'
- [#799](https://github.com/convertigo/convertigo/issues/799) [Android] Fixed, auto Android SDK setup handles build-tools > 30 and prevents the 'DX not found' error

---

## 8.2.5

#### Improvements:

- [#787](https://github.com/convertigo/convertigo/issues/787) [Cache] Add MariaDB + PostgreSQL drivers for the Cache Database

#### Bug Fixes:

- [#788](https://github.com/convertigo/convertigo/issues/788) [CI] Fixed, Gradle 8 is supported for the Convertigo plugin
- [#789](https://github.com/convertigo/convertigo/issues/789) [NGX] Fixed, SCSS Isolation (removed duplicated declaration)
- [#790](https://github.com/convertigo/convertigo/issues/790) [Studio] Fixed, gradle installation works for Android local build
- [#792](https://github.com/convertigo/convertigo/issues/792) [MobileBuilder] Fixed, doesn't normalize NGX attribute value if the name is set on drop from the palette

---

## 8.2.4

#### Improvements:

- [#721](https://github.com/convertigo/convertigo/issues/721) [Studio] Ability to have multiple stubs for a single sequence
- [#775](https://github.com/convertigo/convertigo/issues/775) [TestPlatform] Introduction of new JSON and XML formatters, along with color-coded results
- [#785](https://github.com/convertigo/convertigo/issues/785) [Docker] The JDWP debug port is now optional

#### Bug Fixes:

- [#776](https://github.com/convertigo/convertigo/issues/776) [Admin] Fixed, no more random NullPointerExceptions on the connection page under heavy session load
- [#778](https://github.com/convertigo/convertigo/issues/778) [Step] Fixed , the __testcase variable can now be used in CallTransaction or CallSequence
- [#779](https://github.com/convertigo/convertigo/issues/779) [Studio] Fixed, right-clicking in the mobile viewer loader no longer disrupts the highlight component feature
- [#781](https://github.com/convertigo/convertigo/issues/781) [Server] Fixed, the Angular router can now handle paths with dots
- [#783](https://github.com/convertigo/convertigo/issues/783) [Step] Fixed, file-based steps' path properties will no longer accept a blank expression, avoiding runtime errors. Undefined values are now used instead
- [#784](https://github.com/convertigo/convertigo/issues/784) [Step] Fixed, the Rename File Step no longer deletes the file when the source and target are the same

---

## 8.2.3

#### Improvements:

- [#764](https://github.com/convertigo/convertigo/issues/764) [Engine] Allow Docker image to output Convertigo logs into stdout
- [#767](https://github.com/convertigo/convertigo/issues/767) [MobileBuilder] Apple Silicon downloads x64 NodeJS if arm64 doesn't exist
- [#769](https://github.com/convertigo/convertigo/issues/769) [Engine] Update to the latest jGit
- [#770](https://github.com/convertigo/convertigo/issues/770) [Admin] See symbols with default value in the symbols' page

#### Bug Fixes:

- [#765](https://github.com/convertigo/convertigo/issues/765) [Admin] Fixed, import symbols with no change in priority server will not delete the global_symbols.properties file
- [#768](https://github.com/convertigo/convertigo/issues/768) [Studio] Fixed, removed the old animated gif during execution that can cause JVM crash on MacOS

---

## 8.2.2

#### Improvements:

- [#758](https://github.com/convertigo/convertigo/issues/758) [Studio] Add a Menu to set the UIText i18n property recursively
- [#761](https://github.com/convertigo/convertigo/issues/761) [Docker] Add arm/v7 and arm64/v8 images to Docker Hub

#### Bug Fixes:

- [#757](https://github.com/convertigo/convertigo/issues/757) [Engine] Fixed, format of engine.properties with default properties
- [#759](https://github.com/convertigo/convertigo/issues/759) [Mobile Builder] Fixed, select container popover options now displays in latest chrome version
- [#760](https://github.com/convertigo/convertigo/issues/760) [Studio] Fixed, updated files for 'CI > Update Gradle'

---

## 8.2.1

#### Improvements:

- [#751](https://github.com/convertigo/convertigo/issues/751) [Server] Upgrade dependencies to reduces CVE

#### Bug Fixes:

- [#752](https://github.com/convertigo/convertigo/issues/752) [Studio] Fixed, Source picker is now correct when using dynamic XPath syntax
- [#753](https://github.com/convertigo/convertigo/issues/753) [Studio] Fixed, prevents Browser initialization failure on MacOS
- [#754](https://github.com/convertigo/convertigo/issues/754) [FullSync] Fixed, cluster mode now detected for CouchDB 3.2
- [#755](https://github.com/convertigo/convertigo/issues/755) [Studio] Fixed, prevents errors in the console view at first Studio launch

---

## 8.2.0

#### New Features:

- [#681](https://github.com/convertigo/convertigo/issues/681) [Studio] New Palette View for all Convertigo objects
- [#685](https://github.com/convertigo/convertigo/issues/685) [Studio] New Pickers View changing on object selection
- [#698](https://github.com/convertigo/convertigo/issues/698) [Studio] Set the object name on drop from palette
- [#700](https://github.com/convertigo/convertigo/issues/700) [Studio] Synchronize some objects Name property with an other property value
- [#704](https://github.com/convertigo/convertigo/issues/704) [Studio] Release a MacOS Apple Silicon (ARM64) build
- [#711](https://github.com/convertigo/convertigo/issues/711) [Studio] New Interactive Tutorial View
- [#719](https://github.com/convertigo/convertigo/issues/719) [Studio] New NoCode BaseRow Databases View
- [#725](https://github.com/convertigo/convertigo/issues/725) [Mobile Builder] New QRCode toolbar icon to easily open the viewer with a phone with live update
- [#731](https://github.com/convertigo/convertigo/issues/731) [NGX] Add an 'event' entry in picker for Action sources
- [#738](https://github.com/convertigo/convertigo/issues/738) [NGX] Add a navParams picker
- [#741](https://github.com/convertigo/convertigo/issues/741) [Sequence] Step with source (XPath) can use JS expression inside predicate

#### Improvements:

- [#694](https://github.com/convertigo/convertigo/issues/694) [Studio] Use Eclipse base 2022-12
- [#695](https://github.com/convertigo/convertigo/issues/695) [Billing] Insert 'sessionStart' and 'sessionStop' entries
- [#697](https://github.com/convertigo/convertigo/issues/697) [Admin] Fast project edit loading
- [#701](https://github.com/convertigo/convertigo/issues/701) [Write CSV Step] Use originalKeyName attribute to write line titles of CSV file
- [#702](https://github.com/convertigo/convertigo/issues/702) [Studio] New project wizard explicitly propose to create Web / Desktop & Mobile projects
- [#703](https://github.com/convertigo/convertigo/issues/703) [Docker] Allow to start the container with a non-root user
- [#706](https://github.com/convertigo/convertigo/issues/706) [Studio] The modal "Find an Object" have a full width substring field
- [#708](https://github.com/convertigo/convertigo/issues/708) [Studio] Dialog box to warn and ask the application building before deploying a project
- [#714](https://github.com/convertigo/convertigo/issues/714) [NGX] Hint for the Reorder group component
- [#715](https://github.com/convertigo/convertigo/issues/715) [NGX] Add 'color' property for the Icon component
- [#724](https://github.com/convertigo/convertigo/issues/724) [NGX] Improve handling of enable/disable and application sources generation when whole pages are disabled
- [#726](https://github.com/convertigo/convertigo/issues/726) [Studio] Add screenshots in the 'readme.md' generated file to explain how to import the project
- [#728](https://github.com/convertigo/convertigo/issues/728) [Studio] Review the Deployment wizard window size
- [#729](https://github.com/convertigo/convertigo/issues/729) [Studio] NgxComponent Source Picker show which icon is selected
- [#735](https://github.com/convertigo/convertigo/issues/735) [Studio] Add 'enableOnOffLabels' property to the NXG Toggle component
- [#736](https://github.com/convertigo/convertigo/issues/736) [NGX] Hint 'IfAction' for the AlertAction
- [#737](https://github.com/convertigo/convertigo/issues/737) [NGX] Hint for the FabContainer
- [#739](https://github.com/convertigo/convertigo/issues/739) [NGX] Handle ion-input type file
- [#744](https://github.com/convertigo/convertigo/issues/744) [Server] Custom HTTP 404 page
- [#745](https://github.com/convertigo/convertigo/issues/745) [Studio] Some project treeview optimizations

#### Bug Fixes:

- [#634](https://github.com/convertigo/convertigo/issues/634) [Studio] Fixed, can now D&D a Component having the same name as SharedComponent
- [#638](https://github.com/convertigo/convertigo/issues/638) [HTTP] Fixed, POST request with followRedirect set to true on transaction now redirects using GET
- [#667](https://github.com/convertigo/convertigo/issues/667) [NGX] Fixed, builders now don't release while project is being loaded
- [#680](https://github.com/convertigo/convertigo/issues/680) [NGX] Fixed, Cordova plugin dependencies now correctly installed
- [#682](https://github.com/convertigo/convertigo/issues/682) [Studio] Fixed, closing a Sequence or a Connector editor now don't close all editors of the project
- [#683](https://github.com/convertigo/convertigo/issues/683) [Studio] Fixed, we can now source on step ReadCSV if the property "Title line" is set to true
- [#684](https://github.com/convertigo/convertigo/issues/684) [HTTP] Fixed, JSON_HTTP_transaction's Content-Type dynamic property now overrides default HTTP headers defined in properties table
- [#686](https://github.com/convertigo/convertigo/issues/686) [SQL Connector] Fixed, now can parse JSON of NULL integers
- [#687](https://github.com/convertigo/convertigo/issues/687) [NGX] Fixed, shared components are now "synchronized" properly with the project
- [#691](https://github.com/convertigo/convertigo/issues/691) [NGX] Fixed, Angular.json's assets properties are now generated for serve (HMR) configuration
- [#692](https://github.com/convertigo/convertigo/issues/692) [Studio] Fixed, adds blue background for JS properties of some steps like the SMTP step
- [#696](https://github.com/convertigo/convertigo/issues/696) [SQL] Fixed, now generates correct JSON types for stored procedure responses
- [#705](https://github.com/convertigo/convertigo/issues/705) [Studio] Fixed, creating a symbol from a sequence variable using text editor works
- [#707](https://github.com/convertigo/convertigo/issues/707) [NGX] Fixed, a textInput cannot have a property [ngModel]="''" generated when it's property binding is set to "not set"
- [#709](https://github.com/convertigo/convertigo/issues/709) [Studio] Fixed, renaming a variable now doesn't spread to more variables than it's supposed to
- [#712](https://github.com/convertigo/convertigo/issues/712) [NGX] Fixed, Chart Component: Title chart now in default chart options
- [#717](https://github.com/convertigo/convertigo/issues/717) [Studio] Fixed, mobile builder highlight selection works on hi-DPI screen
- [#718](https://github.com/convertigo/convertigo/issues/718) [Studio] Fixed, property "root Path" fixed on some REST WS import
- [#722](https://github.com/convertigo/convertigo/issues/722) [Studio] Fixed, SQL project Wizard now correctly set Database URL property
- [#730](https://github.com/convertigo/convertigo/issues/730) [FullSync] Fixed, source picker now handles correctly key values with space(s)
- [#740](https://github.com/convertigo/convertigo/issues/740) [NGX] Fixed, disabling a FormatLayout component is effective
- [#742](https://github.com/convertigo/convertigo/issues/742) [NGX] Fixed, can now access scope variables from a customAction
- [#743](https://github.com/convertigo/convertigo/issues/743) [NGX] Fixed, repaired the "create a shared component from the selected object" action
- [#746](https://github.com/convertigo/convertigo/issues/746) [Studio] Fixed, recording XSD Schema from response handles duplicated type name
- [#747](https://github.com/convertigo/convertigo/issues/747) [NGX] Fixed, now correctly use the spinner property of Showloading

---

## 8.1.0

#### New Features:

- [#553](https://github.com/convertigo/convertigo/issues/553) [Ngx Builder] Display in Ngx palette shared components/actions provided by projects
- [#610](https://github.com/convertigo/convertigo/issues/610) [Studio] Automatic generation of project MD files
- [#622](https://github.com/convertigo/convertigo/issues/622) [Ngx Builder] New Font and FontStyle component
- [#629](https://github.com/convertigo/convertigo/issues/629) [Ngx Builder] Added an Assets section in Ngx Source picker
- [#630](https://github.com/convertigo/convertigo/issues/630) [Steps] Added in "Change to" feature : From XML Elements to JSON Field and vice-versa
- [#644](https://github.com/convertigo/convertigo/issues/644) [Docker] Easy configuration to enable HTTPS
- [#660](https://github.com/convertigo/convertigo/issues/660) [Ngx Builder] New icon picker with preview for all "Icon" properties

#### Improvements:

- [#493](https://github.com/convertigo/convertigo/issues/493) [Studio] Setup automatic GitRepo creation and Initial Commit on project creation
- [#620](https://github.com/convertigo/convertigo/issues/620) [Studio] Manage conflicts of components with the same name from different libraries and used in the same project
- [#621](https://github.com/convertigo/convertigo/issues/621) [Steps] ReadCSV step: Add some improvements to Step
- [#624](https://github.com/convertigo/convertigo/issues/624) [Studio] Update Eclipse, Tomcat, Browser, Java to latest
- [#628](https://github.com/convertigo/convertigo/issues/628) [Studio] Object selection in Convertigo Project View now reveals corresponding file in Project Explorer View
- [#639](https://github.com/convertigo/convertigo/issues/639) Added multiline support for requestable variable default value (single and multi valued)
- [#640](https://github.com/convertigo/convertigo/issues/640) [Studio] Update the default GitHub Actions file with build cache configuration to speed up builds
- [#643](https://github.com/convertigo/convertigo/issues/643) [Admin] Add server time + timezone in admin console
- [#646](https://github.com/convertigo/convertigo/issues/646) [Mobile] Handle hard reload of PWA
- [#649](https://github.com/convertigo/convertigo/issues/649) [Ngx Builder] Upgrade NGX TPL for the 8.1.0 release (Angular 14.2.3, Ionic 6.2.8)
- [#664](https://github.com/convertigo/convertigo/issues/664) [Admin] Toggle "Go to end" to stop log fetch before the end
- [#669](https://github.com/convertigo/convertigo/issues/669) [Studio] Auto-expand DOM tree for items having only one child
- [#679](https://github.com/convertigo/convertigo/issues/679) [Studio] Remove unwanted Help buttons in new object wizards

#### Bug Fixes:

- [#568](https://github.com/convertigo/convertigo/issues/568) [Ngx Builder] Fixed, shared Components handle correctly page events and subscribe handlers
- [#587](https://github.com/convertigo/convertigo/issues/587) [Ngx Builder] Fixed, when starting a project that contains errors, it is displayed into mobile builder view 
- [#626](https://github.com/convertigo/convertigo/issues/626) [Fullsync] Fixed, GetDocument transaction in JSON output now contains all attachments
- [#627](https://github.com/convertigo/convertigo/issues/627) [FS/CouhDB Connector] Fixed, PutDocumentAttachment on PouchDB handles space with + for docid
- [#633](https://github.com/convertigo/convertigo/issues/633) Fixed, the generation of the transaction schema from the xml fix the occurrences of nodes
- [#636](https://github.com/convertigo/convertigo/issues/636) [FullSync] Fixed, PouchDB mode filter ACL for _changes
- [#637](https://github.com/convertigo/convertigo/issues/637) [NGX] Fixed, changing Chart type now have effect
- [#641](https://github.com/convertigo/convertigo/issues/641) [SQL] Fixed, now add error element in case of failure
- [#642](https://github.com/convertigo/convertigo/issues/642) [NGX] Fixed, NavParams are now updated after a root/push/navigate page
- [#645](https://github.com/convertigo/convertigo/issues/645) [Studio] Fixed, importing .car from same workspace and already present doesn't pop error
- [#650](https://github.com/convertigo/convertigo/issues/650) [NGX] Fixed, added slot=start attribute in menuAutoItems
- [#651](https://github.com/convertigo/convertigo/issues/651) Fixed, Process exec don't lock the Thread
- [#656](https://github.com/convertigo/convertigo/issues/656) Fixed, project schema cache is now cleared on successful project delete / import / deploy / reload
- [#657](https://github.com/convertigo/convertigo/issues/657) [NGX Builder] Fixed, UIComponent reorder now don't fail anymore
- [#658](https://github.com/convertigo/convertigo/issues/658) Fixed, importing Swagger OAS3 API now creates Transactions with the 'Include data type in XML response node' to True
- [#661](https://github.com/convertigo/convertigo/issues/661) [Rest Web Service Reference] Fixed, added labels for URL authentication account wizard
- [#662](https://github.com/convertigo/convertigo/issues/662) [FullSync] Fixed, PouchDB now can start if another Studio is already open
- [#666](https://github.com/convertigo/convertigo/issues/666) Fixed, client-side errors don't contain sensitive information
- [#668](https://github.com/convertigo/convertigo/issues/668) [SequenceStep] Fixed, Source Picker of a SequenceStep don't show "output false" elements
- [#670](https://github.com/convertigo/convertigo/issues/670) [Admin] Fixed, speed up logviewer init
- [#671](https://github.com/convertigo/convertigo/issues/671) [Studio] Fixed, Engine Preferences: speed up mouse wheel scroll and fix the scroll height
- [#672](https://github.com/convertigo/convertigo/issues/672) [Node] Fixed, use the right node bin folder for MacOS and Linux
- [#674](https://github.com/convertigo/convertigo/issues/674) [Studio] Fixed, hightlight elements supported for recursive shared components
- [#675](https://github.com/convertigo/convertigo/issues/675) [Studio] Fixed, project creation invite to perform the registration wizard
- [#676](https://github.com/convertigo/convertigo/issues/676) [Step] Fixed, JsonSource handles a single string element
- [#678](https://github.com/convertigo/convertigo/issues/678) [Mac] Fixed, drag-n-drop from the NGX Palette to the Builder

---

## 8.0.2

#### Bug Fixes:

- [#632](https://github.com/convertigo/convertigo/issues/632) [UrlMapper] Fixed, JSON requests no more abnormally too long
- [#635](https://github.com/convertigo/convertigo/issues/635) [FullSync] Fixed, replication re-use the ACL pre-filter for CouchDB server

---

## 8.0.1

#### Improvements:

- [#612](https://github.com/convertigo/convertigo/issues/612) [Studio] Better on boarding process
- [#616](https://github.com/convertigo/convertigo/issues/616) [Studio] Icons on blank project first Studio start 
- [#618](https://github.com/convertigo/convertigo/issues/618) [Studio] Add a Run project button

#### Bug Fixes:

- [#611](https://github.com/convertigo/convertigo/issues/611) [NGX] Fixed, when using a segment into a single page application routes are now generated correctly 
- [#614](https://github.com/convertigo/convertigo/issues/614) [Studio] Fixed, no more error on MacOS when try to deploy or export an unbuild project
- [#619](https://github.com/convertigo/convertigo/issues/619) [HttpConnector] Fixed, now handle __header_ HTTP variable value and remove prefix

---

## 8.0.0

#### New Features:

- [#143](https://github.com/convertigo/convertigo/issues/143) [Mobile Builder + NGX] New Switch Action component in Action Chains
- [#183](https://github.com/convertigo/convertigo/issues/183) [NGX] News Ionic 6 / Angular 13 based framework for Front end projects
- [#498](https://github.com/convertigo/convertigo/issues/498) [NGX] Builder now generates Standard Angular components for SharedComponents
- [#501](https://github.com/convertigo/convertigo/issues/501) [HttpConnector] Added a DownloadHttpTransaction
- [#520](https://github.com/convertigo/convertigo/issues/520) [Studio] auto configures PouchDB and uses it for FullSync projects
- [#526](https://github.com/convertigo/convertigo/issues/526) [Sequence] Added JSON Steps : Object, Array, Field
- [#527](https://github.com/convertigo/convertigo/issues/527) [Studio] Execute Sequence and Transaction with JSON output toggle button
- [#528](https://github.com/convertigo/convertigo/issues/528) [Sequence] Added a new JsonSourceStep to transform a source to a JS Object
- [#530](https://github.com/convertigo/convertigo/issues/530) [Sequence] Added a JSON to XML Step
- [#531](https://github.com/convertigo/convertigo/issues/531) [Studio] Added a 'Upgrade to Ngx Framework' wizard to Migrate old Ionic3 projects to NGX
- [#537](https://github.com/convertigo/convertigo/issues/537) [Sequence] Added a ReadJSON and a WriteJSON steps
- [#566](https://github.com/convertigo/convertigo/issues/566) [NGX] Create a 'Create shared Component' from existing components Wizard 
- [#567](https://github.com/convertigo/convertigo/issues/567) [CI] Update CLI CI/CD Build for NGX
- [#579](https://github.com/convertigo/convertigo/issues/579) [Studio] Enhanced Drag & Drop in treeview or in app viewer for better previews

#### Improvements:

- [#291](https://github.com/convertigo/convertigo/issues/291) [NGX] Improved event catch/finally feature for actions
- [#424](https://github.com/convertigo/convertigo/issues/424) [NGX] Use faster PNPM instead of NPM for Front End builder?s Package Manager
- [#506](https://github.com/convertigo/convertigo/issues/506) [Studio] Use Eclipse 2021-12 as base, JDK 17.0.2 and Tomcat 9.0.62
- [#510](https://github.com/convertigo/convertigo/issues/510) [Builder + NGX] Added an "Application Init" event
- [#515](https://github.com/convertigo/convertigo/issues/515) [SMTP Step] Added a property to customize the SSL version
- [#529](https://github.com/convertigo/convertigo/issues/529) [Studio] Hide lib_ projects from project with a toggle button in project view header.
- [#533](https://github.com/convertigo/convertigo/issues/533) [NGX] You can now use JSON.stringify()  in TS expressions
- [#535](https://github.com/convertigo/convertigo/issues/535) [Studio] New transactions are now by default Private
- [#539](https://github.com/convertigo/convertigo/issues/539) [Studio] Added an End point Wizard when building locally IPA or APK iOS/Android apps
- [#540](https://github.com/convertigo/convertigo/issues/540) [Studio] Deploying an Ngx Project now checks that a Valid build is present in DisplayObjects
- [#541](https://github.com/convertigo/convertigo/issues/541) [NGX] Set I18N translate to false by default for Ngx Text beans
- [#571](https://github.com/convertigo/convertigo/issues/571) [Studio] Enable 'Hint' components when dragging NGX Components from palette
- [#572](https://github.com/convertigo/convertigo/issues/572) [Sequence] Read file Steps (XML, JSON & CSV) now appends result without the 'step' element
- [#576](https://github.com/convertigo/convertigo/issues/576) [Admin] In admin console List of certificates are now displayed in alphabetical order
- [#577](https://github.com/convertigo/convertigo/issues/577) [NGX] Added new setLocal action in NGX palette
- [#578](https://github.com/convertigo/convertigo/issues/578) [NGX] We Allow now to embed custom Angular settings such as assets or styles when using a CustomAction
- [#588](https://github.com/convertigo/convertigo/issues/588) [Studio] Added New button to highlight grids for App Viewer
- [#590](https://github.com/convertigo/convertigo/issues/590) [NGX] Added new ngOnchanges event for shared components
- [#595](https://github.com/convertigo/convertigo/issues/595) [Studio] Now, warn the user when built application isn't up-to-date on Deploy/Export/Local build
- [#604](https://github.com/convertigo/convertigo/issues/604) [Studio] Take next available port if tomcat list port is already used
- [#605](https://github.com/convertigo/convertigo/issues/605) [Studio] DisplayObjects/mobile/assets changes are now reflected at run time in real time

#### Bug Fixes:

- [#415](https://github.com/convertigo/convertigo/issues/415) [Admin] Fixed, we cannot add anymore an expired key in the key manager
- [#480](https://github.com/convertigo/convertigo/issues/480) [HttpConnector] Fixed, prevents HTTP Transaction to "auto-retry" on failure
- [#554](https://github.com/convertigo/convertigo/issues/554) [Studio] Fixed, typo issues in New HTTP transaction wizard
- [#580](https://github.com/convertigo/convertigo/issues/580) [Engine] Fixed, execution of TestCase with default variable's value set to <null>
- [#593](https://github.com/convertigo/convertigo/issues/593) [Build] Fixed, cordova Android build fails to retrieve the JDK8
- [#594](https://github.com/convertigo/convertigo/issues/594) [Studio] Fixed, test connection of PostgreSQL connector displays wrong DB Type
- [#596](https://github.com/convertigo/convertigo/issues/596) [Studio] Fixed, importing REST WS reference does not set a default transaction preventing the option to Update schema from current connector data
- [#600](https://github.com/convertigo/convertigo/issues/600) [Studio] Fixed, objects name are now  fully displayed in Projects tree view
- [#609](https://github.com/convertigo/convertigo/issues/609) [Engine] Replicate push attachment failed from Android SDK

---

## 7.9.8

#### New Features:

- [#552](https://github.com/convertigo/convertigo/issues/552) [MobileBuilder] Create a 'Convert to shared Component' Wizard on MobileComponent right Click.
- [#557](https://github.com/convertigo/convertigo/issues/557) [Studio] Add the Eclipse UpdateSite support for Studio update

#### Improvements:

- [#551](https://github.com/convertigo/convertigo/issues/551) Update RhinoJS to 1.7.14

#### Bug Fixes:

- [#543](https://github.com/convertigo/convertigo/issues/543) Fixed, NodeJS process are now properly killed when user closes the mobile builder page on Windows 11
- [#545](https://github.com/convertigo/convertigo/issues/545) Fixed, no more error on dynamic variable in HTTP transaction sub path property
- [#546](https://github.com/convertigo/convertigo/issues/546) [MobileBuilder] Fixed, can now edit a fragment object
- [#547](https://github.com/convertigo/convertigo/issues/547) [Studio] Fixed, no more freeze at cross-referenced SharedAction modification
- [#548](https://github.com/convertigo/convertigo/issues/548) [MobileBuilder] Fixed, a project with its own mobile template doesn't create a ProjectReference on itself anymore
- [#549](https://github.com/convertigo/convertigo/issues/549) Fixed, external projects targeted by the mobile components of a project are now taken into account in its dependencies
- [#555](https://github.com/convertigo/convertigo/issues/555) Fixed, renaming a transaction now renames the xsd file associated
- [#556](https://github.com/convertigo/convertigo/issues/556) [CLI] Fixed, no more build failed with NPE if a project dependency doesn't load
- [#558](https://github.com/convertigo/convertigo/issues/558) [Studio] Fixed, disabled 'Index' sub menu in other connectors than CouchDB and FullSync

---

## 7.9.7

#### Improvements:

- [#534](https://github.com/convertigo/convertigo/issues/534) [Server] Add a new product zip file distribution for Windows Server (Convertigo + JDK + Tomcat)
- [#536](https://github.com/convertigo/convertigo/issues/536) Test Cases can use internal variables or parameters
- [#538](https://github.com/convertigo/convertigo/issues/538) Daily check for Activation keys about to expire and write Error in logs as warning

#### Bug Fixes:

- [#525](https://github.com/convertigo/convertigo/issues/525) Fixed, common jar files from <workspace>/libs/ are now in classpath
- [#532](https://github.com/convertigo/convertigo/issues/532) [MobileBuilder] Fixed, switching between pages holding tabs loads the first tab page

---

## 7.9.6

#### Improvements:

- [#519](https://github.com/convertigo/convertigo/issues/519) Workspace relatives paths can resolve sibling projects with ./projects/AnotherProject/

#### Bug Fixes:

- [#517](https://github.com/convertigo/convertigo/issues/517) Fixed, multipart 'text/plain' parts are read as UTF-8 by default
- [#518](https://github.com/convertigo/convertigo/issues/518) Fixed, prevents same named contexts to terminate concurrent execution
- [#522](https://github.com/convertigo/convertigo/issues/522) [Studio] Fixed, MobileBuilder progression message restored
- [#523](https://github.com/convertigo/convertigo/issues/523) [Studio] Fixed, Windows installer use a shorter install path prefix

---

## 7.9.5

#### Improvements:

- [#505](https://github.com/convertigo/convertigo/issues/505) [Studio] Speed up MobileBuilder page generation at SharedComponent modification
- [#508](https://github.com/convertigo/convertigo/issues/508) [MobileBuilder] Speed up project opening by checking translation availability from existing files
- [#513](https://github.com/convertigo/convertigo/issues/513) [Studio] Ship the JVM for MacOS and Linux too
- [#516](https://github.com/convertigo/convertigo/issues/516) [TestPlatform] Add a "Binary" mode next to "XML" and "Json"

#### Bug Fixes:

- [#503](https://github.com/convertigo/convertigo/issues/503) Fixed, proxy domains exceptions now configured for CouchDB/FullSync and NPM
- [#504](https://github.com/convertigo/convertigo/issues/504) [SiteClipper] Fixed, updated certificate using the Certificate Manager used without need to restart
- [#507](https://github.com/convertigo/convertigo/issues/507) Fixed, the evaluation of a SmartType property is isolated from parallel execution
- [#514](https://github.com/convertigo/convertigo/issues/514) Fixed, SMTP Step now force usage of TLSv1.2 to prevent the SSLHandshakeException

---

## 7.9.4

#### Improvements:

- [#491](https://github.com/convertigo/convertigo/issues/491) Update to latest versions of Java, Tomcat 9 and JxBrowser

#### Bug Fixes:

- [#494](https://github.com/convertigo/convertigo/issues/494) [Studio] Fixed, prevents random freeze at Studio startup while opening MobileBuilder projects
- [#495](https://github.com/convertigo/convertigo/issues/495) [FullSync] Fixed, no more update conflict for PostBulk with "update if change" property in case of multiple docs with the same _id
- [#496](https://github.com/convertigo/convertigo/issues/496) [Studio] Fixed, no more Editor conflict for SequenceJS with the same name inside a same project
- [#497](https://github.com/convertigo/convertigo/issues/497) [FullSync] Fixed, prevents the "function_clause error" for the ?Purge database? transaction
- [#499](https://github.com/convertigo/convertigo/issues/499) [Admin] Fixed, confirmation pop-up appears on top of the Log levels
- [#500](https://github.com/convertigo/convertigo/issues/500) Fixed, TestPlaform redirect works for project that ends with "projects"

---

## 7.9.3

#### Bug Fixes:

- [#478](https://github.com/convertigo/convertigo/issues/478) [Studio] Fixed, symbols usage in the JS Steps? Editor (not replaced by values)
- [#482](https://github.com/convertigo/convertigo/issues/482) [Admin] Fixed, no more NPE if deleting projects too quickly
- [#483](https://github.com/convertigo/convertigo/issues/483) [Studio] Fixed, no more "Unknown transaction" with HTTP calls on a reloaded project
- [#484](https://github.com/convertigo/convertigo/issues/484) [Studio] Fixed, Schema & Picker views are empty if a broken source exists in the project
- [#485](https://github.com/convertigo/convertigo/issues/485) [Admin] Fixed, licence links are ok in footer
- [#486](https://github.com/convertigo/convertigo/issues/486) Fixed, Form PDF Step now handles the originalKeyName attribute
- [#489](https://github.com/convertigo/convertigo/issues/489) [MobileBuilder] Fixed, events are correctly unsubscribed on page leave
- [#490](https://github.com/convertigo/convertigo/issues/490) [Studio] Fixed, new object wizards allow to set existing bean name but not to validate it

---

## 7.9.2

#### Bug Fixes:

- [#477](https://github.com/convertigo/convertigo/issues/477) [Studio] Fixed the syntax coloring of JS editors with the default Dark theme
- [#479](https://github.com/convertigo/convertigo/issues/479) [Admin] Fixed the deadlock in case of double import of a project with circular dependencies

#### Improvements:

- [#481](https://github.com/convertigo/convertigo/issues/158) [Studio] Use the latest Browser component

---

## 7.9.1

#### Bug Fixes:

- [#474](https://github.com/convertigo/convertigo/issues/474) Fixed, PWA manifest.json can be now customized at project level
- [#475](https://github.com/convertigo/convertigo/issues/475) [Admin] Fixed the CouchDB sidebar at the DB view (no more ajax error)
- [#476](https://github.com/convertigo/convertigo/issues/476) Can now clone git+ssh project reference (added the ssh library dependency)

---

## 7.9.0

#### New Features:

- [#90](https://github.com/convertigo/convertigo/issues/90) [MB] Add support of Actions in Mobile Picker
- [#93](https://github.com/convertigo/convertigo/issues/93) [MB] Mobile Picker Sources are now more user friendly
- [#113](https://github.com/convertigo/convertigo/issues/113) [MB] Add Support of Project References for MB projects
- [#162](https://github.com/convertigo/convertigo/issues/162) [MB] Add App event to listen result of auto login
- [#326](https://github.com/convertigo/convertigo/issues/326) [MB] Add a ion-card-title component for Ionic v3 Cards
- [#343](https://github.com/convertigo/convertigo/issues/343) [MB] Add support of Shared Components in Mobile Picker
- [#375](https://github.com/convertigo/convertigo/issues/375) [SQL] Retrieve the value of a clob data
- [#382](https://github.com/convertigo/convertigo/issues/382) [Gradle] Build native Android and IOS application locally using gradle
- [#388](https://github.com/convertigo/convertigo/issues/388) [REST API] Add a Full OAS3 API support
- [#392](https://github.com/convertigo/convertigo/issues/392) [MB] Use now standard Angular's service worker for PWA
- [#420](https://github.com/convertigo/convertigo/issues/420) Add an Autostart property to a sequence object
- [#422](https://github.com/convertigo/convertigo/issues/422) New SAML 2 SSO, ExtendedComponents, Geocoding, Vonage, GoogleSheets and Microsoft Excel libraries are available from the New Project wizard
- [#426](https://github.com/convertigo/convertigo/issues/426) [Docker] Images can use sudo and can be disabled at image launch
- [#427](https://github.com/convertigo/convertigo/issues/427) [Studio] Added a menu update gradle files + menu to enable github actions file
- [#428](https://github.com/convertigo/convertigo/issues/428) [Studio] Support http verb PATCH in yaml WS import wizard
- [#437](https://github.com/convertigo/convertigo/issues/437) [MB] Can now edit several Custom Actions at the same time, limit code lines
- [#455](https://github.com/convertigo/convertigo/issues/455) [FS] PostDocument and PostBulkDocument transactions now handles _use_merge policy

#### Improvements:

- [#158](https://github.com/convertigo/convertigo/issues/158) [Studio] Updated Browser component to the latest version (7.13)
- [#230](https://github.com/convertigo/convertigo/issues/230) [Studio] Upgrade to the latest Eclipse and use the Wild Web Developer plugin for JS/TS/YAML ...
- [#295](https://github.com/convertigo/convertigo/issues/295) [Docker] The default tomcat session timeout is now configurable for Docker images
- [#324](https://github.com/convertigo/convertigo/issues/324) [MB] Form components are now displayed by name in project's tree view
- [#359](https://github.com/convertigo/convertigo/issues/359) [MB] Improved documentation for Subscribe and Publish components concerning Event data property
- [#360](https://github.com/convertigo/convertigo/issues/360) [MB] SignaturePad Component : Add onBeginEvent and onEndEvent
- [#364](https://github.com/convertigo/convertigo/issues/364) Bump config.xml cordova-version to 10.0.0 to support latest Cordova Version
- [#368](https://github.com/convertigo/convertigo/issues/368) Get/PutDocumentAttachmentTransaction now have a base64 mode
- [#379](https://github.com/convertigo/convertigo/issues/379) [Studio] For the REST web service import wizard: Add Petstore OpenApi sample urls
- [#384](https://github.com/convertigo/convertigo/issues/384) [SQL] Use a short validation query for Oracle connection pools
- [#389](https://github.com/convertigo/convertigo/issues/389) [FS] FullSyncListener now calls the Sequences with _conflicts data information
- [#390](https://github.com/convertigo/convertigo/issues/390) [HTTP] New HttpTransaction configured to not use HTTP pool by default
- [#391](https://github.com/convertigo/convertigo/issues/391) [FS] New FullSync connector configured to deny anonymous replication
- [#404](https://github.com/convertigo/convertigo/issues/404) [MB] Add a data return property to the CloseModal Action
- [#405](https://github.com/convertigo/convertigo/issues/405) [Studio] Prevents Eclipse to refresh node_modules folders
- [#408](https://github.com/convertigo/convertigo/issues/408) [Admin] Import symbols default radio buttons to Merge + Priority import
- [#411](https://github.com/convertigo/convertigo/issues/411) [Studio] Replace the modal window "What's new" by a "Video starting" editor
- [#412](https://github.com/convertigo/convertigo/issues/412) Make the Swagger OAS3 console the default one
- [#413](https://github.com/convertigo/convertigo/issues/413) Projects' templates retrieved from their github repositories
- [#421](https://github.com/convertigo/convertigo/issues/421) [MB] Page actions now log the name of the page being handled
- [#423](https://github.com/convertigo/convertigo/issues/423) [MB] If, IfElse and CallSequence actions display their current settings in tree view
- [#430](https://github.com/convertigo/convertigo/issues/430) [MB] The Modal/Root/Push/Popover actions display their target page in tree view
- [#433](https://github.com/convertigo/convertigo/issues/433) [Server] Disable the auto GC preference by default and wait 10 min every each GC
- [#439](https://github.com/convertigo/convertigo/issues/439) [Gradle] Added a retry policy for the deploy task
- [#440](https://github.com/convertigo/convertigo/issues/440) Explicit error message when deploying a project not compatible with the server version
- [#441](https://github.com/convertigo/convertigo/issues/441) [Studio] Http Variable displays Http Name in the TreeView if different of the bean name
- [#442](https://github.com/convertigo/convertigo/issues/442) [Studio] Shrink the XML result of editor and add a button to get the full result
- [#447](https://github.com/convertigo/convertigo/issues/447) [Studio] Popup warning if a FullSync View is not compiling and accepted by CouchDB
- [#454](https://github.com/convertigo/convertigo/issues/454) [REST API] "version" added for the OAS2 YAML
- [#459](https://github.com/convertigo/convertigo/issues/459) [MB] Prevents blank lines generated in ts file for pages containing multiple component with identifier
- [#464](https://github.com/convertigo/convertigo/issues/464) Differ SAP initialization to allow real SAP driver installation
- [#466](https://github.com/convertigo/convertigo/issues/466) [HttpConnector] Add a Basic Preemptive authentication mode

#### Bug Fixes:

- [#271](https://github.com/convertigo/convertigo/issues/271) [MB] Source Picker now recognizes the 'type' attribute in XML Steps
- [#347](https://github.com/convertigo/convertigo/issues/347) [MB] Finally Handler are now executed only once
- [#358](https://github.com/convertigo/convertigo/issues/358) [MB] PublishEvent component, Event data property in TS mode is now empty by default
- [#362](https://github.com/convertigo/convertigo/issues/362) Server now restart properly after clicking "Restart engine" button in administration console
- [#365](https://github.com/convertigo/convertigo/issues/365) [MB] No more HTTP 404 on en.json and fr.json on fresh MB project
- [#367](https://github.com/convertigo/convertigo/issues/367) [FS] Fix replication for documents with a c8oGrp containing a dot
- [#369](https://github.com/convertigo/convertigo/issues/369) Connection string of a SQL connector does not override user/pwd properties
- [#370](https://github.com/convertigo/convertigo/issues/370) No more StackOverflow when generating DOM instance for recursive Schema
- [#374](https://github.com/convertigo/convertigo/issues/374) [SQL] No more unexpected exception using the DatabaseCacheManager with an SQL Server
- [#377](https://github.com/convertigo/convertigo/issues/377) [MB] Can now re-select the same element after a "remove highlight"
- [#378](https://github.com/convertigo/convertigo/issues/378) [REST API] Fix import a Swagger RestService through a http (not https)  url
- [#385](https://github.com/convertigo/convertigo/issues/385) [SQL] Connection pool is now reset when modifying connector properties
- [#386](https://github.com/convertigo/convertigo/issues/386) [Rest] Avoid max session exceeded in case new session initiated for authentication
- [#387](https://github.com/convertigo/convertigo/issues/387) [MB] Fixed Camera component on Chrome
- [#398](https://github.com/convertigo/convertigo/issues/398) Fixed SchemaManager stackoverflow when projects reference each others
- [#400](https://github.com/convertigo/convertigo/issues/400) Fixed FullSync listener not always triggered on a CouchDB cluster
- [#403](https://github.com/convertigo/convertigo/issues/403) [Studio] Fixed show in picker a Fullsync transaction for some special cases
- [#406](https://github.com/convertigo/convertigo/issues/406) [Studio] Fixed Convertigo project URL parser wizard can loose property
- [#409](https://github.com/convertigo/convertigo/issues/409) [Admin] Fixed logviewer endless loop
- [#410](https://github.com/convertigo/convertigo/issues/410) Prevent a project to be imported more than once if it has circular references
- [#414](https://github.com/convertigo/convertigo/issues/414) New session key now taken into account
- [#416](https://github.com/convertigo/convertigo/issues/416) Fixed event loop exception in EngineLogView because of widget disposed
- [#417](https://github.com/convertigo/convertigo/issues/417) Fixed IOException when EngineLogView is disposed
- [#429](https://github.com/convertigo/convertigo/issues/429) Copy/Paste of a Shared Component now updates sourced properties (SC mode)
- [#431](https://github.com/convertigo/convertigo/issues/431) [Studio] Prevent too long editor folder name (fails to write files on Windows)
- [#432](https://github.com/convertigo/convertigo/issues/432) Handle end of context(s) in Internal Requester
- [#434](https://github.com/convertigo/convertigo/issues/434) Can now CTRL+mouse drag of a Sequence in a PageEvent component
- [#435](https://github.com/convertigo/convertigo/issues/435) [MB] Fixed issue when setting Marker property of the Call_Sequence component in the Sequence Picker
- [#436](https://github.com/convertigo/convertigo/issues/436) Fixed projects are not displaying in project view when studio starts and there are no projects reopened automatically.
- [#438](https://github.com/convertigo/convertigo/issues/438) Fixed Null pointer exception when importing a swagger YAML with no summary
- [#443](https://github.com/convertigo/convertigo/issues/443) Fixed synchronization issues while using parallel steps in sequencer
- [#445](https://github.com/convertigo/convertigo/issues/445) [FS] Automatically clear contexts from FullSync listeners
- [#446](https://github.com/convertigo/convertigo/issues/446) Can build/serve old Ionic3 project based on custom or old tpl which does not specify nodeJsVersion
- [#448](https://github.com/convertigo/convertigo/issues/448) Prevents shadow context created when requesting a project that does not exist
- [#449](https://github.com/convertigo/convertigo/issues/449) [RESTWS] Avoid exception while importing YAML because of a null security scheme
- [#450](https://github.com/convertigo/convertigo/issues/450) Fixed error Pop-up at startup: "this.row" is null
- [#453](https://github.com/convertigo/convertigo/issues/453) JGit now use proxy settings
- [#457](https://github.com/convertigo/convertigo/issues/457) Fixed subscribe handler is not working in some cases
- [#463](https://github.com/convertigo/convertigo/issues/463) [Admin] Fixed saving a Variable default value using the Project Edit from the admin Console
- [#467](https://github.com/convertigo/convertigo/issues/467) Fixed PDF Step is not handling Base64 image properly 
- [#473](https://github.com/convertigo/convertigo/issues/473) [FlashUpdate] Ignore the res folder and config.xml for the source package

---

## 7.8.0

#### New Features:

- [#304](https://github.com/convertigo/convertigo/issues/304) Gradle can now generate and compile MobileBuilder applications
- [#335](https://github.com/convertigo/convertigo/issues/335) Add a JVM memory Graph to the Administration console Home page
- [#341](https://github.com/convertigo/convertigo/issues/341) Docker add a COOKIE_SAMESITE environment variable to set the policy
- [#350](https://github.com/convertigo/convertigo/issues/350) Added new Engine property "Automatically GC on low usage" to administration console

#### Improvements:

- [#300](https://github.com/convertigo/convertigo/issues/300) Attribute / jAttribute Steps now show value in tree view
- [#307](https://github.com/convertigo/convertigo/issues/307) Project compatibility version set to the highest used bean version
- [#314](https://github.com/convertigo/convertigo/issues/314) Studio: Documentation links now open in default external browser
- [#316](https://github.com/convertigo/convertigo/issues/316) Mobile Builder Calendar component now fires onChange event
- [#317](https://github.com/convertigo/convertigo/issues/317) Mobile Builder Toggle button component now have a 'color' property
- [#320](https://github.com/convertigo/convertigo/issues/320) Mobile Builder Badge component now has a 'item-end' value for the 'Item position' property
- [#322](https://github.com/convertigo/convertigo/issues/322) Add an "Auto Pull" property for Reference Project (git case)
- [#323](https://github.com/convertigo/convertigo/issues/323) Engine "Git Container" property defaults to workspace/git (instead of <user>/git)
- [#330](https://github.com/convertigo/convertigo/issues/330) Enforce administration console security
- [#342](https://github.com/convertigo/convertigo/issues/342) Studio: Speed up project load/save

#### Bug Fixes:

- [#296](https://github.com/convertigo/convertigo/issues/296) Fixed, new sessions that try to sync a non public database shouldn't be referenced in session count
- [#298](https://github.com/convertigo/convertigo/issues/298) Fixed, TreeViewComponent does not build anymore in production mode
- [#299](https://github.com/convertigo/convertigo/issues/299) Fixed, NullPointerException  while importing JSON WS reference
- [#310](https://github.com/convertigo/convertigo/issues/310) Fixed, a copy/paste of a customAction fails to copy complete customs action's code
- [#311](https://github.com/convertigo/convertigo/issues/311) Fixed, log files renamed by the log viewer aren't automatically removed
- [#312](https://github.com/convertigo/convertigo/issues/312) Fixed, wrong labels for Icon 'icon name' property
- [#313](https://github.com/convertigo/convertigo/issues/313) Fixed, HTTP 404 on documentation links for CallSequence Action and Fullsync Actions
- [#315](https://github.com/convertigo/convertigo/issues/315) Fixed, PageEvent of a shared component may not be generated in calling page
- [#321](https://github.com/convertigo/convertigo/issues/321) Fixed, Server: file path properties with .// can now resolve projects outside of the project folder
- [#325](https://github.com/convertigo/convertigo/issues/325) Fixed, Mobile Builder PageEvent of a shared component is correctly generated in calling page
- [#327](https://github.com/convertigo/convertigo/issues/327) Fixed, Mobile Builder Button component 'button form type' property is set to "false" in template
- [#328](https://github.com/convertigo/convertigo/issues/328) Fixed, Mobile Builder properties set with 'not set' value are correctly saved for all components
- [#331](https://github.com/convertigo/convertigo/issues/331) Fixed, no more NullPointerException when importing symbols file
- [#333](https://github.com/convertigo/convertigo/issues/333) Fixed, memory is well freed after a PostFind call (CouchDb/FullSync connector)
- [#334](https://github.com/convertigo/convertigo/issues/334) Fixed, Mobile Builder CallSequence component "Disable auto login" property is correctly saved
- [#337](https://github.com/convertigo/convertigo/issues/337) Fixed, Mobile builder Log component has twice the fatal level in "Level" property
- [#338](https://github.com/convertigo/convertigo/issues/338) Fixed, Studio speed up modifying shared objects' properties
- [#355](https://github.com/convertigo/convertigo/issues/355) Fixed, Mobile builder subscribe handler is properly removed 
- [#356](https://github.com/convertigo/convertigo/issues/356) Fixed, when DB prefix for FullSync is used, databases document count and size columns are now filled properly
- [#357](https://github.com/convertigo/convertigo/issues/357) Fixed, server "Convertigo Server local URL" property default to 28080 port

---

## 7.7.0

#### New Features:

- [#115](https://github.com/convertigo/convertigo/issues/115) Add an 'Export light' wizard for project
- [#117](https://github.com/convertigo/convertigo/issues/117) MobileBuilder, add Pause Action component
- [#147](https://github.com/convertigo/convertigo/issues/147) Add support of PageEvent & SubscribeHandler for SharedComponent
- [#166](https://github.com/convertigo/convertigo/issues/166) Global FullSync database name prefix
- [#168](https://github.com/convertigo/convertigo/issues/168) Add a 'Finally' handler in mobile builder
- [#169](https://github.com/convertigo/convertigo/issues/169) Mobile Builder, add the Ionic Lazy loading feature
- [#184](https://github.com/convertigo/convertigo/issues/184) Import and reference projects by a GIT url
- [#192](https://github.com/convertigo/convertigo/issues/192) Administration Console, integrate Fauxton (CouchDB console)
- [#269](https://github.com/convertigo/convertigo/issues/269) __body and Application/json header should automatically transmit JSON from Xpath Source
- [#290](https://github.com/convertigo/convertigo/issues/290) CouchDB Connector, add a PostFind transaction for Mango Query

#### Improvements:

- [#146](https://github.com/convertigo/convertigo/issues/146) Save shared actions and shared components in separate YAML files.
- [#153](https://github.com/convertigo/convertigo/issues/153) Use Eclipse 2019-06 as Studio base
- [#155](https://github.com/convertigo/convertigo/issues/155) Use Tomcat 9 for Studio and Docker
- [#162](https://github.com/convertigo/convertigo/issues/162) MobileBuilder, add an auto login event
- [#172](https://github.com/convertigo/convertigo/issues/172) MobileBuilder, automatically enable the Angular ProdMode when building in production mode
- [#181](https://github.com/convertigo/convertigo/issues/181) MobileBuilder, add the value 'mini' in the Button size property
- [#191](https://github.com/convertigo/convertigo/issues/191) MobileBuilder, add variables to SharedComponent
- [#201](https://github.com/convertigo/convertigo/issues/201) Set the PWA App title to the MobileApplication's  'Application Name' property 
- [#206](https://github.com/convertigo/convertigo/issues/206) CouchDB Connector, add query parameters for server Databases Transaction
- [#212](https://github.com/convertigo/convertigo/issues/212) Save DesignDocument multi-lines into the YAML project
- [#217](https://github.com/convertigo/convertigo/issues/217) MobileBuilder, add a new i18n property for text components
- [#245](https://github.com/convertigo/convertigo/issues/245) MobileBuilder, add a 'Disable auto login' property to the CallSequence Action
- [#247](https://github.com/convertigo/convertigo/issues/247) MobileBuilder, Signature component generates a PNG with transparent background
- [#248](https://github.com/convertigo/convertigo/issues/248) MobileBuilder, can use 'translate.instant()' in any pages
- [#262](https://github.com/convertigo/convertigo/issues/262) MobileBuilder, modified default debug log for CustomAction component
- [#266](https://github.com/convertigo/convertigo/issues/266) MobileBuilder, add new 'Changes detection strategy'  property for Page bean

#### Bug Fixes:

- [#224](https://github.com/convertigo/convertigo/issues/224) Fixed, error when opening lib_AmazonLEX project.
- [#225](https://github.com/convertigo/convertigo/issues/225) Fixed, bean Camera not working in web browser
- [#237](https://github.com/convertigo/convertigo/issues/237) Fixed, MobileBuilder, 'Post data' component posts extra data
- [#246](https://github.com/convertigo/convertigo/issues/246) Fixed, MobileBuilder, some issues with beans using latest TPL
- [#265](https://github.com/convertigo/convertigo/issues/265) Fixed, MobileBuilder, renaming a Page name breaks Application build

---

## 7.6.6

#### Improvements:

- [#189](https://github.com/convertigo/convertigo/issues/189) Possibility to add a new palette component/action by drag and dropping on its folder
- [#276](https://github.com/convertigo/convertigo/issues/276) Set Cookie 'HttpOnly' and 'Secure' (also via Docker setting)
- [#277](https://github.com/convertigo/convertigo/issues/277) Global symbols: Values with .secret suffix are stored ciphered and hidden
- [#278](https://github.com/convertigo/convertigo/issues/278) Password policy for administration console account (via Reg. Exp.)
- [#280](https://github.com/convertigo/convertigo/issues/280) Test Platform Convertigo header display : width too small


#### Bug Fixes:

- [#268](https://github.com/convertigo/convertigo/issues/268) Fixed, SampleMobileOffChat project does not work as intented since 7.6.4
- [#270](https://github.com/convertigo/convertigo/issues/270) Fixed, prevent random NPE on heavy charge
- [#272](https://github.com/convertigo/convertigo/issues/272) Fixed, global_symbols.properties no more write at startup
- [#273](https://github.com/convertigo/convertigo/issues/273) Fixed, premature end of Sequence if calls done on the same Context at the same time
- [#279](https://github.com/convertigo/convertigo/issues/279) Fixed, Dev Network link is broken in Administration Console and Test Platform

---

## 7.6.5

#### Improvements:

- [#231](https://github.com/convertigo/convertigo/issues/231) Mobile Builder, alert the user for packages reinstallation when changing the template
- [#243](https://github.com/convertigo/convertigo/issues/243) Static resources are now correctly managed by navigator caches
- [#251](https://github.com/convertigo/convertigo/issues/251) Improve the Studio FindObject dialog for full text search
- [#252](https://github.com/convertigo/convertigo/issues/252) Add some missing properties for CouchDB transactions for CouchDB 2.x server
- [#254](https://github.com/convertigo/convertigo/issues/254) TestPlatform now displays immediately, without waiting for revision computation
- [#258](https://github.com/convertigo/convertigo/issues/258) Add a message in log about how to set a JDBC Oracle Driver
- [#259](https://github.com/convertigo/convertigo/issues/259) Add new actionName, actionFunction available as properties in a CustomAction

#### Bug Fixes:

- [#202](https://github.com/convertigo/convertigo/issues/202) Fixed, SmtpStep failed with 'no object DCH for MIME'
- [#203](https://github.com/convertigo/convertigo/issues/203) Fixed, Mobile Builder, default value is now used when a boolean property is not set
- [#208](https://github.com/convertigo/convertigo/issues/208) Fixed, removed unwanted standard output on XMLCopyStep execution
- [#209](https://github.com/convertigo/convertigo/issues/209) Fixed, Studio Git Pull don't fail if there are only files to delete
- [#210](https://github.com/convertigo/convertigo/issues/210) Fixed, Studio Open TestPlatform action directly open the platform without redirect
- [#213](https://github.com/convertigo/convertigo/issues/213) Fixed, background of selected variable with the JS Editor is visible using the Dark Theme
- [#218](https://github.com/convertigo/convertigo/issues/218) Fixed, no more CacheManager backup error at startup
- [#219](https://github.com/convertigo/convertigo/issues/219) Fixed, Studio can now open existing projects without '.project'
- [#220](https://github.com/convertigo/convertigo/issues/220) Fixed, Mobile Application can build with special characters in Application fields (author, name, description ?)
- [#221](https://github.com/convertigo/convertigo/issues/221) Fixed, using twice the same SharedComponent in a Page declares only once its action declarations
- [#222](https://github.com/convertigo/convertigo/issues/222) Fixed, Studio MobileBuilder viewer will not mix application editor if opened at the same time
- [#229](https://github.com/convertigo/convertigo/issues/229) Fixed, FlashUpdate now follows MobileBuilder application changes to trigger an update
- [#232](https://github.com/convertigo/convertigo/issues/232) Fixed, Server session count is now correct when using an overflow session key
- [#233](https://github.com/convertigo/convertigo/issues/233) Fixed, Studio can edit symbols of combo properties by validating the symbol option again
- [#239](https://github.com/convertigo/convertigo/issues/239) Fixed, FullSync transaction can post/bulk over a deleted document with an override policy
- [#244](https://github.com/convertigo/convertigo/issues/244) Fixed, CORS response headers (Access-Control-) also set for error responses
- [#249](https://github.com/convertigo/convertigo/issues/249) Fixed, Mobile Builder Styles within a menu component are not appended into App.scss
- [#250](https://github.com/convertigo/convertigo/issues/250) Fixed, can now DnD a sequence on a CustomAction to create a CallSequence action
- [#253](https://github.com/convertigo/convertigo/issues/253) Fixed, Android localbuild application can reach non https endpoint
- [#256](https://github.com/convertigo/convertigo/issues/256) Fixed, CouchDB Connector Purge transaction doesn?t create useless documents anymore
- [#260](https://github.com/convertigo/convertigo/issues/260) Fixed, can build mobile source package can be downloaded for multiple platforms at the same time

---

## 7.6.4

#### Improvements:

- [#187](https://github.com/convertigo/convertigo/issues/187) MobileBuilder, improved assets files copy at build time
- [#188](https://github.com/convertigo/convertigo/issues/188) UrlMapper, added a parameter 'terminate session' to Operations that can be turned off
- [#193](https://github.com/convertigo/convertigo/issues/193) CouchDB and FullSync connectors, optimized PostBulk merge/override to do less requests
- [#196](https://github.com/convertigo/convertigo/issues/196) MobileBuilder, prevent empty value for properties in TS or SC mode to avoid build failure

#### Bug Fixes:

- [#182](https://github.com/convertigo/convertigo/issues/182) Fixed, file upload variable in Test Platform
- [#185](https://github.com/convertigo/convertigo/issues/185) Fixed, MobileBuilder Shared action use right variables values (not previous ones)
- [#186](https://github.com/convertigo/convertigo/issues/186) Fixed, MobileBuilder shared action can use 'this' keyword in actions variable/property values
- [#194](https://github.com/convertigo/convertigo/issues/194) Fixed, FullSync getDocument transaction is now succeeding for docid containing a +
- [#195](https://github.com/convertigo/convertigo/issues/195) Fixed, MobileBuilder RootPage properties are correctly saved
- [#197](https://github.com/convertigo/convertigo/issues/197) Fixed, re-introduce the fake sapjco3.jar to be replaced by the real sapjco3.jar
- [#199](https://github.com/convertigo/convertigo/issues/199) Fixed, MobileBuilder Alert action 'Enable Backdrop Dismiss' property is now correctly working
- [#200](https://github.com/convertigo/convertigo/issues/200) Fixed, calling /index.jsp correctly returns the index.html content

---

## 7.6.3

#### Improvements:

- [#178](https://github.com/convertigo/convertigo/issues/178) New 'Application' property 'Use click for tap' to differently handle '(tap)' and 'onTap' events
- [#165](https://github.com/convertigo/convertigo/issues/165) Right-clicking a 'SharedComponent' component from another project in Mobile App Viewer now focus element in treeview

#### Bug Fixes:

- [#179](https://github.com/convertigo/convertigo/issues/179) Fixed, UrlMapper is correctly exported with the project from the Server Administration console
- [#180](https://github.com/convertigo/convertigo/issues/180) Fixed, retry FullSync insertion when an IllegalStateException occurs, using a new HttpClient

---

## 7.6.2

#### Improvements:

- [#112](https://github.com/convertigo/convertigo/issues/112) In Studio, GIT actions that modify the project source asks user for refresh project
- [#174](https://github.com/convertigo/convertigo/issues/174) In Studio, Project menu have now 'GitFlow', 'Compare' and 'Replace' entries
- [#175](https://github.com/convertigo/convertigo/issues/175) In Studio, startup time divided by 4
- [#176](https://github.com/convertigo/convertigo/issues/176) MobileBuilder, improve application performances

#### Bug Fixes:
 
- [#164](https://github.com/convertigo/convertigo/issues/164) Fixed, MobileBuilder Sliding Tabs compile and can by used
- [#167](https://github.com/convertigo/convertigo/issues/167) Fixed, prevents deadlocks on calls for interdependent projects
- [#170](https://github.com/convertigo/convertigo/issues/170) Fixed, MobileBuilder RootPage action can now pass data to rooted page
- [#171](https://github.com/convertigo/convertigo/issues/171) Fixed, the javascript 'use' function is now available instead of throwing a 'ClassCastException'
- [#173](https://github.com/convertigo/convertigo/issues/173) Fixed, init admin password with Docker variable supported again

---

## 7.6.1

#### Bug Fixes:

- [#17](https://github.com/convertigo/convertigo/issues/17) Fixed, MobileBuilder Treeview component now compiles
- [#138](https://github.com/convertigo/convertigo/issues/138) Fixed, no more pop-up alert after closing a JS Editor in the Studio
- [#144](https://github.com/convertigo/convertigo/issues/144) Fixed, MobileBuilder can use older MB projects templates without compilation failures
- [#145](https://github.com/convertigo/convertigo/issues/145) Fixed, MobileBuilder Camera action works in the App Viewer
- [#148](https://github.com/convertigo/convertigo/issues/148) Fixed, no more collapsed treeview when adding / removing a component to a project
- [#149](https://github.com/convertigo/convertigo/issues/149) Fixed, no more freeze when using the SourcePicker
- [#152](https://github.com/convertigo/convertigo/issues/152) Fixed, can rename tasks in Scheduler's Administration widget
- [#156](https://github.com/convertigo/convertigo/issues/156) Fixed, prevent infinite loop of Scheduler execution, Jobs Group dependencies cannot be circular 
- [#157](https://github.com/convertigo/convertigo/issues/157) Fixed, Mobile Builder demos now build with their respective templates
- [#159](https://github.com/convertigo/convertigo/issues/159) Fixed, MobileBuilder build can fail if project folder name is different of the projet name
- [#160](https://github.com/convertigo/convertigo/issues/160) Fixed, the onSessionLost event subscription is now correctly generated in app.component.ts
- [#161](https://github.com/convertigo/convertigo/issues/161) Fixed, HttpTransaction using SSL for an untrusted certificate will work over a Squid proxy
- [#163](https://github.com/convertigo/convertigo/issues/163) Fixed, AnimateAction is now working again and can be placed inside a Shared Component

---

## 7.6.0

#### New Features:

- [#3](https://github.com/convertigo/convertigo/issues/3) UrlMapper: use swagger 3 and handle oas2 / oas3
- [#8](https://github.com/convertigo/convertigo/issues/8) Make Convertigo project more VCS friendly, use a shrinked yaml project descriptor split in several files
- [#9](https://github.com/convertigo/convertigo/issues/9) Integrate PDFBox jar file and create a PDF form step
- [#13](https://github.com/convertigo/convertigo/issues/13) Add a Mobile Builder Tooltip component
- [#22](https://github.com/convertigo/convertigo/issues/22) Implement a Database revision number for FullSync that allows to reset clients FS base automatically
- [#42](https://github.com/convertigo/convertigo/issues/42) Make a dark theme for Convertigo Studio
- [#54](https://github.com/convertigo/convertigo/issues/54) Studio: remove unwanted menu and toolbar item for Convertigo perspective
- [#55](https://github.com/convertigo/convertigo/issues/55) Better security for CouchDB to restrict access for the admin user
- [#60](https://github.com/convertigo/convertigo/issues/60) Add a Mobile Builder Ionic Infinite component
- [#72](https://github.com/convertigo/convertigo/issues/72) Add a Mobile Builder ClearDataSource action
- [#73](https://github.com/convertigo/convertigo/issues/73) Authenticated session responses now contains a X-Convertigo-Authenticated header (used by SDK)
- [#74](https://github.com/convertigo/convertigo/issues/74) Add support of Mobile Builder application shared actions
- [#76](https://github.com/convertigo/convertigo/issues/76) Introduce use, context.server.set/get and context.project.set/get to JS scope to cache Java method loading and share memory across sessions
- [#78](https://github.com/convertigo/convertigo/issues/78) Add a server and a project classloader directory for extra libraries
- [#88](https://github.com/convertigo/convertigo/issues/88) Add a CouchDB/FullSync AllDocs and Purge transactions
- [#95](https://github.com/convertigo/convertigo/issues/95) Add support of Mobile Builder App events
- [#101](https://github.com/convertigo/convertigo/issues/101) Add a Mobile Builder IterateAction action
- [#105](https://github.com/convertigo/convertigo/issues/105) Ctrl+DND a Sequence in a Mobile Builder action creates the CallSequence component
- [#106](https://github.com/convertigo/convertigo/issues/106) Ctrl+DND a FullSync View in a Mobile Builder action creates the getView component
- [#111](https://github.com/convertigo/convertigo/issues/111) Add support of Mobile Builder App Shared Components
- [#124](https://github.com/convertigo/convertigo/issues/124) Ctrl+DND a SharedAction on an event or action creates the InvokeAction
- [#125](https://github.com/convertigo/convertigo/issues/125) Ctrl+DND a SharedComponent on a component creates the UseShared component
- [#127](https://github.com/convertigo/convertigo/issues/127) Add a Mobile Builder ClosePopover action
- [#137](https://github.com/convertigo/convertigo/issues/137) Add a new IfElseAction working with a ElseHandler
- [#141](https://github.com/convertigo/convertigo/issues/141) Add the lib_AmazonLEX in the new project wizard

#### Improvements:

- [#1](https://github.com/convertigo/convertigo/issues/1) Studio: Import and use projects from any folder (projects can be imported from a git repository elsewhere on the disk)
- [#19](https://github.com/convertigo/convertigo/issues/19) Update JxBrowser to 6.23.1 and use the 64bit version for Windows
- [#27](https://github.com/convertigo/convertigo/issues/27) Improve Database cache manager for Oracle XMLTYPE/CLOB handling
- [#33](https://github.com/convertigo/convertigo/issues/33) New Engine property to configure the current server Endpoint URL
- [#35](https://github.com/convertigo/convertigo/issues/35) Prevent Swagger console errors to popup in server mode
- [#44](https://github.com/convertigo/convertigo/issues/44) Do not initialize connector for 'void' transaction
- [#46](https://github.com/convertigo/convertigo/issues/46) UrlMapper: Basic authentication is improved by checking is user is already connected
- [#66](https://github.com/convertigo/convertigo/issues/66) Add a Mobile Builder Chooser action that both support iOS and Android
- [#67](https://github.com/convertigo/convertigo/issues/67) Add a Mobile Builder Progress Bar component
- [#71](https://github.com/convertigo/convertigo/issues/71) Mobile Builder SetGlobal actions are now displayed according to 'Property' and 'value' properties
- [#77](https://github.com/convertigo/convertigo/issues/77) JS 'log' object accepts log4j levels methods (fatal, info, warn, trace)
- [#81](https://github.com/convertigo/convertigo/issues/81) Enhance support for wkWebview for iOS for better performances
- [#86](https://github.com/convertigo/convertigo/issues/86) Improve Mobile Builder page generation speed
- [#87](https://github.com/convertigo/convertigo/issues/87) Sort mobile app template projects by descending order
- [#89](https://github.com/convertigo/convertigo/issues/89) Modal Action bean must have a parameter to suspend while displayed
- [#94](https://github.com/convertigo/convertigo/issues/94) HttpConnector: Use the standard SSL stack if there is no specific certificate for a Project
- [#96](https://github.com/convertigo/convertigo/issues/96) Convertigo SDK settings are now customizable
- [#102](https://github.com/convertigo/convertigo/issues/102) Re-use compiled expression for RhinoJS to reduce JVM classes memory space and have better performance
- [#104](https://github.com/convertigo/convertigo/issues/104) Scheduler now handles order and limited number of parallel job
- [#109](https://github.com/convertigo/convertigo/issues/109) Mobile component help in Reference Manual is better rendered
- [#110](https://github.com/convertigo/convertigo/issues/110) Mobile Builder Sequence Calls Actions loading spinner can now be disabled, and 2 new actions has been added to show and close loading spinners
- [#133](https://github.com/convertigo/convertigo/issues/133) Improve MB ForEach bean, now item and index elements be customized so they can be inserted in nested loops.
- [#134](https://github.com/convertigo/convertigo/issues/134) Handle TenantID in OAuth action for Azure
- [#140](https://github.com/convertigo/convertigo/issues/140) Enable GZip compression for text response by default

#### Bug Fixes:

- [#18](https://github.com/convertigo/convertigo/issues/18) Fixed the double save dialog on Studio closing without saving
- [#24](https://github.com/convertigo/convertigo/issues/24) Fixed the Call Step generates invalid call data when a variable sources a self closing tag
- [#26](https://github.com/convertigo/convertigo/issues/26) Fixed Mobile Builder CalendarPickerAction to work as expected
- [#32](https://github.com/convertigo/convertigo/issues/32) Fixed invalid XML generated with some unicode characters
- [#38](https://github.com/convertigo/convertigo/issues/38) Fixed extra blank lines added in saved content of editable mobile components
- [#39](https://github.com/convertigo/convertigo/issues/39) Fixed extra markers added in saved TS content of editable mobile components
- [#41](https://github.com/convertigo/convertigo/issues/41) Fixed the Mobile Palette that does not show all components
- [#43](https://github.com/convertigo/convertigo/issues/43) Fixed JDBC Oracle poor performances
- [#47](https://github.com/convertigo/convertigo/issues/47) Fixed UrlMapper: context/session not always removed
- [#48](https://github.com/convertigo/convertigo/issues/48) Fixed Sequencer: attributes generated through a 'copyOf' step are not appended to DOM
- [#53](https://github.com/convertigo/convertigo/issues/53) Fixed the administration project list displays wrong deployment date after export
- [#57](https://github.com/convertigo/convertigo/issues/57) Fixed Mobile Builder: some projects do not load due to a validator considered as invalid
- [#59](https://github.com/convertigo/convertigo/issues/59) Fixed Mobile Builder component drop that needs to install dependencies is not triggering the reinstall of dependencies
- [#61](https://github.com/convertigo/convertigo/issues/61) Fixed the greyed Next button when you select a new Fullsync Listener
- [#62](https://github.com/convertigo/convertigo/issues/62) Fixed typo of some bean documentation
- [#65](https://github.com/convertigo/convertigo/issues/65) Fixed the random disappearing of files from DisplayObjects/mobile/assets/i18n
- [#68](https://github.com/convertigo/convertigo/issues/68) Fixed barcodescanner plugin build for Android
- [#69](https://github.com/convertigo/convertigo/issues/69) Fixed all Mobile palette documentation links now to ionic3 documentation
- [#75](https://github.com/convertigo/convertigo/issues/75) Fixed 'Null' error when deleting a project
- [#80](https://github.com/convertigo/convertigo/issues/80) Fixed the popup error when no bean found with the search bar
- [#85](https://github.com/convertigo/convertigo/issues/85) Fixed the Invalid Thread Exception when adding a Component that requiring additional packages
- [#103](https://github.com/convertigo/convertigo/issues/103) Fixed Scheduler to count session and clear contexts
- [#114](https://github.com/convertigo/convertigo/issues/114) Fixed Certificate Mappings configuration in administration
- [#116](https://github.com/convertigo/convertigo/issues/116) Fixed NPE in the Studio stdout console when selecting the LogView
- [#126](https://github.com/convertigo/convertigo/issues/126) Fixed 'heap out of memory' for some Mobile Builder builds in production mode
- [#128](https://github.com/convertigo/convertigo/issues/128) Fixed the transpilation failure for empty value in TS mode of Mobile Builder actions
- [#129](https://github.com/convertigo/convertigo/issues/129) Fixed Mobile Builder missing rebuilds 
- [#130](https://github.com/convertigo/convertigo/issues/130) Fixed Http response always in UTF-8 even if the Requestable defines an another Charset

## [pre 7.6.0 versions changelog](CHANGELOG.pre.7-6-0.md)
