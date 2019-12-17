# Changelog

## 7.6.6

#### Improvements:

- [#189](https://github.com/convertigo/convertigo/issues/189) Possibility to add a new palette component/action by drag and dropping on its folder
- [#276](https://github.com/convertigo/convertigo/issues/276) Set Cookie ‘HttpOnly’ and ‘Secure’ (also via Docker setting)
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

- [#202](https://github.com/convertigo/convertigo/issues/202) Fixed, SmtpStep failed with "no object DCH for MIME"
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
- [#188](https://github.com/convertigo/convertigo/issues/188) UrlMapper, added a parameter "terminate session" to Operations that can be turned off
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

- [#178](https://github.com/convertigo/convertigo/issues/178) New 'Application' property "Use click for tap" to differently handle "(tap)" and "onTap" events
- [#165](https://github.com/convertigo/convertigo/issues/165) Right-clicking a "SharedComponent" component from another project in Mobile App Viewer now focus element in treeview

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
- [#171](https://github.com/convertigo/convertigo/issues/171) Fixed, the javascript "use" function is now available instead of throwing a "ClassCastException"
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
- [#75](https://github.com/convertigo/convertigo/issues/75) Fixed ?Null? error when deleting a project
- [#80](https://github.com/convertigo/convertigo/issues/80) Fixed the popup error when no bean found with the search bar
- [#85](https://github.com/convertigo/convertigo/issues/85) Fixed the Invalid Thread Exception when adding a Component that requiring additional packages
- [#103](https://github.com/convertigo/convertigo/issues/103) Fixed Scheduler to count session and clear contexts
- [#114](https://github.com/convertigo/convertigo/issues/114) Fixed Certificate Mappings configuration in administration
- [#116](https://github.com/convertigo/convertigo/issues/116) Fixed NPE in the Studio stdout console when selecting the LogView
- [#126](https://github.com/convertigo/convertigo/issues/126) Fixed "heap out of memory" for some Mobile Builder builds in production mode
- [#128](https://github.com/convertigo/convertigo/issues/128) Fixed the transpilation failure for empty value in TS mode of Mobile Builder actions
- [#129](https://github.com/convertigo/convertigo/issues/129) Fixed Mobile Builder missing rebuilds 
- [#130](https://github.com/convertigo/convertigo/issues/130) Fixed Http response always in UTF-8 even if the Requestable defines an another Charset

## [pre 7.6.0 versions changelog](CHANGELOG.pre.7-6-0.md)
