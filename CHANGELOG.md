# Changelog

## 7.6.0

#### New Features:

- [#42](https://github.com/convertigo/convertigo/issues/42) Make a dark theme for Convertigo Studio
- [#60](https://github.com/convertigo/convertigo/issues/60) Add support of Ionic Infinite Scroll
- [#73](https://github.com/convertigo/convertigo/issues/73) Response of authenticated session contains a X-Convertigo-Authenticated header (used by SDK)
- [#74](https://github.com/convertigo/convertigo/issues/74) Add support of application shared actions
- [#76](https://github.com/convertigo/convertigo/issues/76) Introduce use, server.set/get and project.set/get to JS scope to cache Java method loading and share memory across sessions
- [#78](https://github.com/convertigo/convertigo/issues/78) Add a server and a project classloader directory for extra libraries
- [#90](https://github.com/convertigo/convertigo/issues/90) Add support of Actions in Mobile Picker
- * [#11](https://github.com/convertigo/convertigo/issues/11) Add a session connection table information panel in Convertigo admin
- [#13](https://github.com/convertigo/convertigo/issues/13) Add a Tooltip pseudo bean to mobile palette
- [#14](https://github.com/convertigo/convertigo/issues/14) Add a new action bean to close Modal
- [#22](https://github.com/convertigo/convertigo/issues/22) Implement a Database revision number for FullSync that allow to reset clients FS base automatically
- [#29](https://github.com/convertigo/convertigo/issues/29) Gradle Plugin to build Convertigo Project to CAR file
- * [#3](https://github.com/convertigo/convertigo/issues/3) UrlMapper: v2
- [#54](https://github.com/convertigo/convertigo/issues/54) Studio: remove unwanted menu and toolbar item for Convertigo perspective
- [#55](https://github.com/convertigo/convertigo/issues/55) Securing CouchDB to restrict access for the admin user
- [#72](https://github.com/convertigo/convertigo/issues/72) [MB] Add a ClearDataSource action bean
- [#88](https://github.com/convertigo/convertigo/issues/88) Add a CouchDB/FullSync AllDocs and Purge transactions
- [#8](https://github.com/convertigo/convertigo/issues/8) Make Convertigo project VCS friendly
- [#9](https://github.com/convertigo/convertigo/issues/9) Integrate PDFBox jar file and create a PDF form step

#### Improvements:

- [#77](https://github.com/convertigo/convertigo/issues/77) JS `log` object accepts log4j levels methods (fatal, info, warn, trace)
- [#81](https://github.com/convertigo/convertigo/issues/81) Enhance support for wkWebview for iOS
- [#86](https://github.com/convertigo/convertigo/issues/86) Improve Mobile Builder page generation to be more stable
- * [#15](https://github.com/convertigo/convertigo/issues/15) Improve Studio performances for some actions
- * [#19](https://github.com/convertigo/convertigo/issues/19) Update JxBrowser to 6.21 and use the 64bit version for windows
- * [#1](https://github.com/convertigo/convertigo/issues/1) Studio: Import and use projects from any folder
- * [#27](https://github.com/convertigo/convertigo/issues/27) Improve Database cache manager for Oracle XMLTYPE/CLOB handling
- * [#33](https://github.com/convertigo/convertigo/issues/33) New property to configure the current server Endpoint URL
- * [#35](https://github.com/convertigo/convertigo/issues/35) Prevent Swagger console errors to popup in server mode
- * [#44](https://github.com/convertigo/convertigo/issues/44) Do not initialize connector for 'void' transaction
- * [#46](https://github.com/convertigo/convertigo/issues/46) UrlMapper: Basic authentication may be improved
- * [#71](https://github.com/convertigo/convertigo/issues/71) Display setGlobal action using 'Property' and 'value' properties
- * [#87](https://github.com/convertigo/convertigo/issues/87) Sort mobile app template projects by descending order
- * [#89](https://github.com/convertigo/convertigo/issues/89) Modal Actionbean must have a parameters to be possibly blocking while display 


#### Bug Fixes:

- * [#12](https://github.com/convertigo/convertigo/issues/12) File upload not working in test platform sequence
- * [#16](https://github.com/convertigo/convertigo/issues/16) Projects not visible on admin page
- * [#17](https://github.com/convertigo/convertigo/issues/17) Treewview component doesn't work in production mode
- * [#18](https://github.com/convertigo/convertigo/issues/18) Double save dialog on Studio closing
- * [#20](https://github.com/convertigo/convertigo/issues/20) CloseModalAction.ts contains Garbage resulting from improper merge
- * [#21](https://github.com/convertigo/convertigo/issues/21) Multivalued variable values are <null> at project loading
- * [#23](https://github.com/convertigo/convertigo/issues/23) Bad "sessions in use" in admin console
- * [#24](https://github.com/convertigo/convertigo/issues/24) Call Step generates invalid call data when a variable sources a self closing tag
- * [#26](https://github.com/convertigo/convertigo/issues/26) [MB] CalendarPickerAction isn't working as expected
- * [#28](https://github.com/convertigo/convertigo/issues/28) Bad deployment if the ".car" is dropped in the workspace
- * [#32](https://github.com/convertigo/convertigo/issues/32) Xerces failed to write some unicode characters
- * [#38](https://github.com/convertigo/convertigo/issues/38) Extra blank lines added in saved content of editable mobile components
- * [#39](https://github.com/convertigo/convertigo/issues/39) Extra markers added in saved TS content of editable mobile components
- * [#40](https://github.com/convertigo/convertigo/issues/40) Some projects do not load randomly (studio or server)
- * [#41](https://github.com/convertigo/convertigo/issues/41) Mobile Palette does not show all components (studio)
- * [#43](https://github.com/convertigo/convertigo/issues/43) Poor database performances
- * [#45](https://github.com/convertigo/convertigo/issues/45) Administration: sorting projects by date is incorrect
- * [#47](https://github.com/convertigo/convertigo/issues/47) UrlMapper: context/session not always removed
- * [#48](https://github.com/convertigo/convertigo/issues/48) Sequencer: Attributes generated through a 'copyOf' step are not appended to DOM
- * [#49](https://github.com/convertigo/convertigo/issues/49) Wrong session counter if session limit is reached
- * [#4](https://github.com/convertigo/convertigo/issues/4) Two sequences with same name but not same case does not execute individually
- * [#51](https://github.com/convertigo/convertigo/issues/51) Add missing JDBC libraries
- * [#57](https://github.com/convertigo/convertigo/issues/57) Some projects do not load due to a validator considered as invalid
- * [#59](https://github.com/convertigo/convertigo/issues/59) Drop a bean that needs an install of dependencies is not triggering a reinstall of dependencies
- * [#61](https://github.com/convertigo/convertigo/issues/61) Next button still greyed out when you select a new Fullsync Listener
- * [#62](https://github.com/convertigo/convertigo/issues/62) Typo on PageEvent bean documentation
- * [#63](https://github.com/convertigo/convertigo/issues/63) Admin web / Bad parameters in projets
- * [#64](https://github.com/convertigo/convertigo/issues/64) Mobile Builder: Dragging a folder make crash the studio
- * [#68](https://github.com/convertigo/convertigo/issues/68) Update barcodescanner to cordova plugin 8.0.1
- * [#69](https://github.com/convertigo/convertigo/issues/69) Map all Mobile palette documentation links to ionc3 docs
- * [#6](https://github.com/convertigo/convertigo/issues/6) About Studio : replace strings ((CEMS_VERSION)) and ((CURRENT_YEAR))
- * [#75](https://github.com/convertigo/convertigo/issues/75) Null error when delete a project
- * [#79](https://github.com/convertigo/convertigo/issues/79) Bean FullsyncSyncAction must override _local/c8o document
- * [#80](https://github.com/convertigo/convertigo/issues/80) Not found bean in search bar raises a pop-up
- * [#82](https://github.com/convertigo/convertigo/issues/82) Alert on mobile with flashupdate

## [pre 7.6.0 versions changelog](CHANGELOG.pre.7-6-0.md)
