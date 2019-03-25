# Changelog

## 7.5.6 

#### NEW FEATURES (1):

- New, a SpeechToText action is now available

#### IMPROVEMENTS (4):

- Evol, added 'ionClose' and 'ionOpen' menu events for controls
- Evol, added 'delay' and 'speed' properties for Animation
- Evol, GetData action is now supported in Mobile picker
- New, app global shared object is now supported in Mobile picker

#### BUGS (4):

- Fixed, 7.5.5 introduce a regression about replication of deleted documents.
- Fixed, prevent unwanted behaviors using Animate action
- Fixed, Studio does not freeze any more when an object is dragged on a folder.
- Fixed, project deployment date is now preserved after an Export on server.

---

## 7.5.5 

#### IMPROVEMENTS (5):

- Awesome FullSync replication speed on CouchDB 2.x databases.
- New FullSync Connector property "anonymous replication" : "allow/deny" replication of anonymous sessions
- Updated Cordova to 8.1.1, Android Platform and related plugins.
- Handle automatically PWA service worker code and versioning
- Updated Cordova to 8.1.1, iOS Platform and related plugins.

#### BUGS (7):

- Fixed, prevent Scheduler failure in case of 'schedule' with no more future event.
- Fixed, the 'connection string' also override the subpath of HttpTransaction.
- Fixed, some Mobile Builder component properties cannot be turned "not set"
- Fixed, doesn't add extra blank lines when editing Mobile Builder components code.
- Fixed, doesn't add extra markers when editing Mobile Builder components code.
- Fixed, Studio can deploy on a Convertigo server listening at a root path.
- Fixed, admin console: sorting projects by date is correct now.

---

## 7.5.4 

#### NEW FEATURES (2):

- Added a Spinner Action bean.
- Administration console get a new table of active sessions.

#### IMPROVEMENTS (5):

- HashStep has an offset property.
- Can now edit multiple views of the same design document.
- lib_Salesforce connector now supports bulk data creation.
- lib_Salesforce connector now supports bulk data deletion.
- Symbols sorted alphabeticaly by default with the Admin console.

#### BUGS (11):

- Fixed, project name changed by an import also update mobile builder properties.
- Fixed, Modifying MobileBuilder 'Application' object properties does not fire app viewer rebuild
- Fixed, added missing properties to the DateTime component
- Fixed, deployed mobile builder projects remind the default root page.
- Fixed, handle attachment push from Android SDK with a CouchDB 1.7.1 server.
- Fixed, case of recreate FS document with same content still deleted.
- Fixed, cannot restart the Engine in a CORS environment.
- Fixed, wrong date detection for some log files.
- Fixed, redirect failure with HTTPS for the HTTP Connector.
- Fixed, Chart component for mobile builder now supports responsive mode by default and will fit in containers such as grids for better page layouts
- Fixed, Fullsync _bulk_get protocol for batch replication is now streamed so that client do not timeout anymore waiting for the first byte. This ensures better replication performances.

---

## 7.5.3 

#### NEW FEATURES (5):

- Added a new TreeView component for the Mobile Builder.
- Convertigo can GZip its HTTP responses. Must be turn on through the engine configuration.
- Added Piwik MATOMO support.
- Added a "CloseModal" Action in the mobile palette to close opened modal pages
- Added a"Tooltip" component to the mobile palette to handle tooltips for desktop applications

#### IMPROVEMENTS (10):

- Added a new Heading component for the Mobile Builder where the tag can be changed (h1, h2, h3...)
- The Split Step can now use a custom tagname even if the number of split is unknown.
- Speed up replication of deleted documents using native SDK.
- List Directory step can be sorted by date, size and name.
- Added 'Split pane layout' property to Application object to support ion-split-pane element.
- Convertigo Cache can now be stored in an Oracle database.
- A Mobile Builder Custom Validator can access page's variables and function through 'this'.
- Ship a minimal .gitignore for new projects to skip some temporary files to be committed in a Git repository.
- Studio: Avoid Xml parser 'content not allowed in prolog' message to be printed in the stdout console on object right clic.
- Use the latest version of Oracle OJDBC library.

#### BUGS (22):

- Fixed, replication now works if DB contains more than 100 deleted documents.
- Fixed, now the Sort Step doesn't modify the source and make a copy.
- Fixed, added contextual parameter 'clientip' in logs when using UrlMapper or internal requester.
- Fixed, now can reorder siblings items in the project tree using DND
- Fixed, no more error message when delete DBOs with children in the selection
- Fixed, now we cannot create object with the same name, even with a  different case, of a sibling object.
- Fixed, no more file-system alert when editing Mobile Builder 'style' beans.
- Fixed, cancelling a bean creation doesn't set the project changed.
- Fixed, no more NullPointerException with the "Reference View" when clicking on some Sequences
- Fixed, no more warning in log when deploying a MobileBuilder project on a server.
- Fixed, correctly update the source picker when using 'inputVars' step and adding new variables to a Sequence.
- Fixed, exported Mobile Builder projects from Administration console  are now correctly exported
- Fixed, JS encoding failure for MobileBuilder projects
- Fixed, the Schema view now cleans memory on refresh or on closing.
- Fixed, copy/paste a Mobile Builder page now generates the full page source.
- Fixed, now delete the 'ionic_tmp' temporary folder when a project is reloaded.
- Fixed, no more startup diagnostic warning if the current user's shell isn't defined in /etc/passwd.
- Fixed, now the 'is a file upload' property in variables is correctly used in Test platform.
- Fixed, check Destination configuration when call a SAP JCO function and retry in case of failure.
- Fixed, when a project is renamed, all Mobile Builder components using a MobileSmartSource property are updated.
- Fixed, added a 'c8oSecurity' key in document response when Fullsync replication failed due to Convertigo ACL instead of a 403 status.
- Fixed, prevents the import of the 'c8o' Design Document in FullSync connector when using Studio's "Import Design Document" menu

---

## 7.5.2 

#### NEW FEATURES (3):

- Added an Event subscribe/ publish for notifying from one page to another.
- Added the 'Animate' action to be able to trigger an animation on any other component, and an "Animation" Attribute to be able to set the animation type when the component is displaye
- You can now use "import * as module ... " is CustomAction "page import" property. (Useful for importing plain JS code

#### IMPROVEMENTS (7):

- It is now possible to set the 'item position' property on an icon defined in page for Menu
- It is now possible to add/use actions under Menu components
- It is now possible to add/use Forms under Menu components
- Now, an error message is displayed if a Mobile Builder template is missing instead of an ugly stack trace
- Convertigo FullSync is now compatible and qualified with CouchDB 2.11 (And CouchDB 1.61)
- Added a button "Clear" to the search edit in the mobile palett
- Added 'CanEnter' and 'CanLeave', page's lifecycle event for Mobile Builder page events.

#### BUGS (11):

- Fixed: Sequence context not automatically discarded when an Authentication exception occurs.
- Fixed: "Item title" property of "MenuItem" is now correctly migrated from 7.5.0 projects
- Fixed: It is now possible to paste an Object to Project tree even if "Copy" has not been used before (Useful to paste an object sent by mail for example
- Fixed: Session ending doesn't terminate all Contexts. Now clean up is done correctly.
- Fixed manually defined Imports in page class.
- Fixed: End transactions or each connector are now called when we have multi-connector contexts and the session terminates.
- Fixed: Modifying symbols with special characters is now done in UTF8 to preserve special character encoding. And it is possible to import large symbol files (service uses POST instead of GET)
- Fixed: ReadCSV Step handles now all end lines and quoted new lines
- Fixed: HTTP Connector, in POST transaction method, POST variables are now correctly URL encoded
- Fixed: some issues in Mobile Picker have been fixed
- Fixed, now Ctrl + C/V do copy/paste of value for smartType and nullable properties.

---

## 7.5.1 

#### NEW FEATURES (15):

- FullSync PostDocument and PostBulkDocument can choose the ACL policy from a property.
- Added a Get Data fullsync action bean.
- Added a Put Attachment fullsync action bean
- Added a Delete Attachment action bean
- Added an OAuth/OpenID action bean
- Added an FSImage fullsync bean
- Added a Modal Page action bean.
- New version of sampleMobileOffChat using the MobileBuilder.
- Added a Calendar Picker action bean
- Added a Signature Pad bean
- Added a Popover Page action bean
- Added an Alert action
- Added "Delete Data" Fullsync action
- A new package is available in Convertigo delivery to install multi instances Convertigo server on linux environments
- Added a TextToSpeech native action bean

#### IMPROVEMENTS (27):

- FullSync PostData action can now explicitly set a GroupName to the document by using the 'c8oGrp' property.
- Added new Delivery receipt, Read receipt properties to SMTP step
- MenuItem components can now have bindable (Sourceable) Text properties
- Server Multi-instance launch script control.sh is now provided by default in product delivery.
- Added an Action Sheet native action
- Update the Mobile Builder Viewer core (Based Chromium 60).
- Errors can now be caught by 'Error Handler' or 'Failure Handler' actions.
- Support now iPhone X screen size compatibility for new projects.
- FullSync __live queries are now ignored when executing CAF routes.
- GooglePLus action now supports the "Logout" verb
- The Studio now embeds a GIT client.
- Prevents to open the Mobile Builder if the 'template Project' property is invalid.
- "Live" option for "Get Data" FullSync action is now available and you can also add variables for extra options
- Added new properties for ion-tabs component
- Mobile Builder: toggle UI Component highlight on double click.
- Mobile Builder: Camera action bean works out of Cordova (HTML5 mode).
- Input text can now be align left right or center
- Improve Palette help text readability
- Added new properties in FabContainer component
- FullSync Get action has now a 'Marker' property.
- Enable use of jSON objects in (TS) fields.
- Mobile Builder: ActionLog also do remote logs with level configuration.
- Mobile builder: add markers for the editable areas in the TypeScript editor.
- You can now use Mobile Picker to "source" components under a Menu
- Added new Installation program for Convertigo Studio.
- Added minute values to DateTime component
- You can now Mock returns from Cordova based action when Cordova is not available  (PWA, or Development time)

#### BUGS (13):

- Fixed: SQL: Regression for queries that do not return a resultset (insert, update...) causing NullPointerException
- Fixed, warn the user that the local build failed due to a file permission issue.
- Fixed, a Mobile Builder project testPlatform won't load before the first Mobile Builder launch.
- Fixed, Using JSON syntax {} in an object property can cause HTML generation errors.
- Fixed, QueryView properties cannot be set back to 'not set'
- Fixed, object fields with dash characters causes incorrect mobile source syntax generation.
- Fixed, Copying a Theme bean from a projectA app to a projectB app does not rebuild app
- Fixed, FullSync Listener doesn't launch if some projects are broken.
- Fixed, missing ionChange event to Event bean
- Fixed, Missing "import * as ts from 'typescript'" when a CustomAction is directly under an event with no other action in chain
- Make Grid row and Grid heights combo box editable.
- Fixed, Disabling an Attr which name is 'class' has no effect
- Fixed, Can not save a Style containing braces.

---

## 7.5.0 

#### NEW FEATURES (3):

- Convertigo Studio is now based on a Neon 4.6.3 64 Bits.
- Added TypeScript plugin used by the Mobile Builder.
- New Mobile Builder RMAD tool based on Cordova, Angular and Ionic frameworks.

#### IMPROVEMENTS (8):

- New property for Write XML and CVS Steps that allow to write "Output True" elements only
- Distinguish the "HTTP session timeout" and a new "Context timeout" Project property
- New studio preference to disable the "auto open default connector"
- Make WriteBase64 Step sourceable to get the computedFilePath.
- The Studio delete projects in background (doesn't hang UI).
- JS/CSS minifier return 404 if there is no resources.
- Add a ResetDatabase transaction for Couchdb and Fullsync Connectors.
- Add a Studio preference "Local build folder" to prevent too long path

#### BUGS (4):

- Fixed, Legacy emulator can not play .etr record if ssl is enabled.
- Fixed, the "Sender email address" of SMTP Step property isn't javascriptable
- Fixed, HttpTransaction follow redirect: use the same method, choose if redirect and add headers in HttpInfo.
- Fixed, complex variables can be used as cache key.

---

## 7.4.8 

#### NEW FEATURES (1):

- New Security Filter system: you can define a set of rules to accept or deny requests.

#### IMPROVEMENTS (1):

- Prevents "session already invalidated" exceptions

#### BUGS (4):

- Fixed, "Java exception" for SQL verbs returning no result sets is "Generate JSON type" property is set to true
- Fixed, broken log viewer when the log folder is full.
- Fixed, .NET SDK FullSync replication uses too many sessions and causes licensing issues.
- Fixed, the "String or Null" property editor can be turned to  "null" again

---

## 7.4.7 

#### NEW FEATURES (4):

- An iOS built application can be installed Over-The-Air using a QRcode on the Test Platform.
- 5250 Legacy emulator now handles STRPCCMD 5250 command.
- A Global Symbol value can contain another Global Symbols.
- Added a 64-bit Convertigo Server installer for Windows.

#### IMPROVEMENTS (11):

- Remove renamed log files, respecting the "Log4J default appender max backup index" property
- MultiPart upload variables can specify or predict their ContentType.
- Added 2 more administration roles, extends the TEST_PLATFORM role to access hidden and private projects.
- Optimize fullsync servlet for attachment streaming.
- URLMapper now correctly handles multiple files upload
- Updated platform config.xml files with latest Cordova versions for demos and project templates.
- Allow the URL minifier in Tomcat version above 7.0.69
- Upgrade Tomcat to 7.0.82 for security improvement.
- Upgrade Java JDK to 1.8.0_152 for security improvement.
- SQL Transactions can generate "type" attributes for a better JSON transformation
- Support OPTIONS method for Convertigo requesters (.json/.xml/...) and UrlMappers.

#### BUGS (10):

- Fixed, HTTP Basic auth now support non-ASCII character.
- Fixed, prevent the web administration of the Studio to restart the Engine.
- Fixed, prevent projects to be open multiple times.
- Fixed, can not delete Design Document if couchDB server is not running.
- Fixed, HTTP 500 thrown when modifying a value in CONFIG widget from Admin Console
- Fixed, BASIC authentication on HTTP Connector does not use Connection string/sUrl.
- Fixed, huge http data can freeze the Studio.
- Fixed, Java NPE when the engine is restarted from the admin.
- Fixed, invalid cast using the ProcessExecStep with a JS array.
- Fixed, prevent administration console status update error on "Engine Restart"

---

## 7.4.6 

#### IMPROVEMENTS (5):

- Steps jAttribute and Attribute must not have a "output" property
- Licence keys can be removed from the administration console.
- Warn when Convertigo Server or Studio is connected to a wrong version of CouhDB (Should be 1.6.1).
- Studio deploy window now sorted by servers's name.
- Add the Content-Length HTTP header for static files of a project.

#### BUGS (12):

- Fixed, Administrative console error listing connections when a session is expired.
- Fixed, moving by Drag An drop a father node in itself causes complete sub tree destruction.
- Fixed, ArrayIndexOutOfBoundsException on some complex 5250 fields screens
- Fixed, licence expiration date always display the first key entered.
- Fixed, continuous replication loop if client have not access to the last document.
- Fixed, prevent the FullSync servlet to create session for OPTIONS requests.
- Fixed, the sampleMobileOffChat fix the "Access restriction" error
- Fixed, prevent FS replication to be frozen if the CouchDB enable gzip for _changes.
- Fixed, terminal SNA default support the 132 column mode.
- Fixed, administration now works with Wildfly.
- Fixed, .cpdf requester to make PDF from response.
- Fixed, non-convertigo project directories in the workspace are now ignored, preventing an exception in the logs.

---

## 7.4.5 

#### BUGS (4):

- Fixed, 5250 connector optimization for numerous display messages (ex: status messages).
- Fixed, CORS FullSync replication with Angular C8O SDK.
- Fixed, FullSync replication with numerous groups and large database causes client timeout: split documents processing.
- Fixed, parameters not saved at scheduler job creation.

---

## 7.4.4 

#### NEW FEATURES (2):

- Add a new Main/Advanced property to disable the automatic zip project backup.
- Fullsync handles groups for authenticated users and documents.

#### IMPROVEMENTS (3):

- Speed up the initial replication of a fullsync database, especially for the fileTransfer feature of the SDK client.
- The SiteClipper streams resources faster.
- Add additional CORS headers needed by the Convertigo Angular client.

#### BUGS (10):

- Fixed, in the scheduler admin, editing keep the "sending" variable state
- Fixed, prevent ConcurrentModificationException on huge server requests.
- Fixed, readXML schema generation can freeze the studio with too big XML file.
- Fixed, SubXPath property of multivalued variables also used for variable from JS scope.
- Fixed, automatically unwrap Native Java Object of many Steps with a JS evaluation.
- Fixed, allow replication of huge database with thousands of documents.
- Fixed, the fullsync transaction with the policy "merge" also merge sub-objects
- Fixed, in Studio the filename of the temporary file for a SequenceJS edition is shortened, so Windows can delete it.
- Fixed, the URL Mapper handles destruction of contexts.
- Fixed, prevents a case of IndexOutOfBoundsException when using the admin log viewer.

---

## 7.4.3 

#### NEW FEATURES (2):

- Improved support of stored procedures/functions for SQL connector.
- Added steps to GET/SET/REMOVE object from session.

#### IMPROVEMENTS (5):

- Session availability check also done for FullSync replications.
- Use JSSE SSL stack for Secure TN5250.
- Speed up the Studio logviewer loading with huge logs.
- Add the LockedFile API to the lib_FileTransfer project.
- HttpTransaction can force the response data charset and the surrounding CDATA is now optional.

#### BUGS (10):

- Fixed, schema generation of transaction updated from current generated XML.
- Fixed, the legacy transaction ignores invalid character for XML.
- Fixed, a possible replication error if a couchdb is linked with an older Convertigo.
- Fixed, wrong schema generated when SAP transaction has 'changing' parameters.
- Fixed, a special case of FullSync continuous replications now releases the tomcat threads correctly.
- Fixed, execution of a sequence after a cut/paste in an another project where the initial project was closed.
- Fixed, log viewer now uses the admin console log file name property.
- Fixed, URL Mapper skips the CORS header for big responses.
- Fixed, JXPath engine accepts XPath functions that return numbers.
- Fixed, iterator with output false content can cause StackOverflowError.

---

## 7.4.2 

#### NEW FEATURES (3):

- Add an action "Change to" Element to jElement steps
- New Compact transaction for FullSync and CouchDB connectors.
- 'node_modules' folder is not exported nor deployed.

#### IMPROVEMENTS (9):

- Webization: Code and CSS improvements
- SAP Connector add support of group logon.
- Stop button works between connection attempts of an unavailable server.
- The FullSyncServlet will ignore some headers to prevent some network errors.
- Incoming PUT request with Content-Type x-www-form-urlencoded decodes body as parameters.
- Ship the latest JVM 1.8.0_102.
- Speed up the FullSync replication startup with devices.
- The lib_FileTransfer now auto removes orphans chunks and compacts the database.
- A FullSync listener can be disabled.

#### BUGS (29):

- Fixed, 'Show Blocks' button in a legacy project now shows blocks.
- Fixed, case of error pop-up when deleting objects in Studio.
- Fixed, 'Debug mode' button in a legacy project now enables the debug feature.
- Fixed, 'Show Blocks' button in a legacy project prevents a ClassCastException.
- Fixed, 'ReadCSV' step prevents some 'corrupted' reading error.
- Fixed, 'Debug mode' button in a legacy project now stops on each extraction rule when XMLizing.
- Fixed, prevent 'Last detected screen class: (unknown)' in HTML project.
- Fixed, SQL Connector using JNDI works in Studio for right click Execute.
- Fixed, schema of XmlCopyStep copies the good schema subset of the source.
- Fixed, context.requestedObject.getCurrentHttpParameters() gives a copy: cannot modify the default property in Studio.
- Fixed, administration main page can display an error dialog on some system.
- Fixed, symbols failure for some combo properties.
- Fixed, an issue with sequence variables named 'default' or 'var'.
- Fixed, administration console user can be edited without any roles.
- Fixed, JXPath mode allow non-path expression, like string.
- Fixed, add the missing "Param attname" property for the DeleteDocumentAttachment transaction of a FullSync or CouchDB connector
- Fixed, file upload on webapps for IE11 and Edge.
- Fixed, the mobile application property 'splashRemoveMode' is handled for built mobile application.
- Fixed, logs.Add now works after a fullsync replication.
- Fixed, target transaction icons of Call Transactions are now visible.
- Fixed, cannot import a project with same name as template.
- Fixed, 'false' keyword detection for SNA commands extraction rule.
- Fixed, CTF can now chains data-c8o-if attribute for sibling elements.
- Fixed, no more NPE from the Studio References view in case of a broken reference.
- Fixed, restore 'statistics' element in requestable's response schema, if enabled.
- Fixed, a case of bad schema with a ReadCSV.
- Fixed, prevent some case of "change to" jAttribute to jElement that can cause an error
- Fixed, now handle KEY_FIELDMINUS to change numeric sign for Javelin 5250 connector.
- Fixed, Studio doesn't take too much time to enable/disable step/statements.

---

## 7.4.1 

#### NEW FEATURES (3):

- Add in standard the JDBC Postgresql driver.
- Set in the Extra log field "user" the current authenticated user of the session
- Add a Sub XPath property for MultiValued variables of Call steps. That allow to select contents of each values.

#### IMPROVEMENTS (8):

- New dialog box to import Swagger YAML/JSON webservice definitions.
- Reference view handle URL Mapper Operation.
- The ReadXML step shows 10000 characters max in Debug log or the whole XML in Trace log.
- The "Mobile Application" project disable the SplashScreenSpinner by default
- Deploy and export feature ignore 'CVS' folders.
- The lib_FileTransfer project is compatible with the Client SDK 2.0.4 and the uploadFile API.
- For the linux server install, increase the max open file limit for the Convertigo user.
- CTF also handles #suffix for transaction and sequence 'call' and 'listen'.

#### BUGS (7):

- Fixed incomplet refactoring with project renaming.
- Fixed, NPE when FullSync replication push with no FS listener in the FS connector.
- Fixed, add the whitelist plugin in the config.xml for migration of project < 7.4.0 .
- Fixed in test platform: No HTML escaping for project comments.
- Fixed, download built mobile package on a WAS server.
- Also allow slash at the end of "Convertigo server enpoint" property. Fix replication issues in this case
- Fixed ClassCastException using the "Get from session" step in special case

---

## 7.4.0 

#### NEW FEATURES (18):

- Add a linux 64 .run installer shipped with a 64 bit JVM.
- Add the URL mapper module. It exposes Convertigo mBaaS services as RESTful URLs.
- Added 'Generate and Link structure' contextual menu on a Call_transaction step variable to automatically create Complex and Element steps sourced on that variable right before the Call step.
- The linux .run installer accepts parameters for automatic installation (see -h).
- A HttpTransaction accepts variables in the URL and handles new parameters to send a request with a custom body : __body and __contentType.
- Import YAML or jSON REST service definitions to generate automatically a REST connector.
- In Studio the "Open web administration console" menu also authenticate as admin
- Use the LDAPAuthentication step to search and bind on LDAP directories. Can also retrieve LDAP attributes.
- New property "JSON object output" of Project, used to specify allow to remove the "document" root element for JSON output
- Add a "Set response header" step to add custom HTTP header to the sequence output
- Add a "Set response status" step to set a HTTP statut code to the sequence output
- Added new variable's property called 'isRequired' to set it as value mandatory.
- Users can be created and roles affected to access the administration console with limited privileges.
- Add a "Get request header" step to read HTTP header of the sequence request
- Global symbols can be use for all properties with a boolean or a combo editor.
- Local build auto install the right cordova version in a dedicated folder
- lib_Twitter can now send status with a picture
- CouchDbConnector and FullSyncConnector generate and interpret the type attribute for xmlToJson and jsonToXml conversions.

#### IMPROVEMENTS (18):

- Http transaction can send binaries as plain body (_body variable) or as multipart (doFileUpload property).
- Web or hybrid application have a C8O.vars.wait_threshold property that prevents showing the wait_div for very short requests (like FS requests).
- A requestable launched from a FullSyncListener uses the user's session that is replicating and is automatically authenticated.
- In the studio, editable comments in green color are displayed next to objects.
- You can now choose whether or not to include test cases when you deploy/export a project.
- The local build is done in a dedicated folder for each platform.
- Alphabetically re-ordered configuration categories except for "Main Parameters" and "Log4J root logger" from "Logs" category that both appear first
- Projects properties are sorted on the project edit administration page.
- C8O.log.<level> can accept a plain object or an array
- The Push Notification step can source a complex structure with title, picture, led, etc... to make advanced notification.
- Remove the export date attribute from the project xml file to prevent SCM conflict
- Support Cordova/Phonegap 5.4.1 for local and remote build of Android and iOS. Support 6.1.1 for local build of Windows.
- Drastically improve the performance about XPath evaluation (improve many features speed).
- Run a requestable from the Studio doesn't use the HTTP stack for the requesting.
- A scheduled job doesn't use the HTTP stack for the requesting Convertigo.
- A new property CordovaPlatform of MobilePlatform allow to use custom cordova platform.
- Drop old mobile platform and keep Androit, iOS, Windows UWP, Windows 8.1 and WindowsPhone 8.1
- The Studio XPath editors now allow to write before the anchor to use XPath functions.

#### BUGS (11):

- Fixed, prevent too many fullsync continuous replications to lock CouchDB communications.
- Fixed, prevent the phantom Context per thread on call transaction.
- Fixed emulator logs are outputed to stdout instead of engine.log
- Fixed, NPE when importing some old projects.
- Fixed, automatically escape some characters in mobile application name.
- Fixed, contextual menu on "Listeners" folder of a Fullsync connector
- Fixed, on the admin project page, properties of the Project bean can be seen.
- Fixed, prevent the NPE of the LDAP Authentication step
- Fixed, the Environment variable admin dialog strips non valid XML characters (prevent browser error).
- Fixed, you can pick a nodelist for simple variables of a call step.
- Fixed, completion error for html with the CTF on a blank project.

---

## 7.3.6 

#### NEW FEATURES (1):

- Introduce new FullSync listeners for deleted document or filtered by filter

#### IMPROVEMENTS (2):

- The project Deploy dialog now suggests to deploy on a local server (for new workspace).
- Add a 'uuid' extra log variable for calls from devices

#### BUGS (10):

- Fix the Convertigo Studio shutdown failure (process not terminated)
- Fix the Reference View issues for transaction handlers.
- Fix some NPE when a sequence with sequence/transaction steps is called at the same time on the same context.
- Fix the AS/400 Restore state command causing an error status to be sent to the AS/400 server
- Restore the missing property display name "XML attributes to include" for JavelinTransactio
- Fix the bug for following - (minus) signs not allowed in numeric fields for Javelin Connector.
- Fix the migration of projects from 6.2 and earlier
- Fix the NPE on project opening in Studio, when a dependency is not open or doesn't exist.
- Fix the couchbase-lite-phonegap-plugin version to 1.1.1 for Android  remote build because the latest 1.2.0 failed.
- Commands extraction rule computes a wrong column when two commands are found in the same block

---

## 7.3.5 

#### NEW FEATURES (1):

- lib_Sigfox template library for integrating with the Sigfox IoT network

#### BUGS (3):

- Project .car export, deployment and mobile build application ignore files starting with "." (like .svn, .git, .DS_Store, ...
- Variables with "WSDL exposed=false" was incorrectly included in the WSD
- Cookies are preserved across connectors and projects for a given user session

---

## 7.3.4 

#### NEW FEATURES (3):

- New step Remove Session to requests the end of the current session and its related contexts.
- New lib_FileTransfer project that allow to make a safe replication of big files, based on the fullsync connector.
- Convertigo Client SDK requests are accepted on Convertigo Standard or Extended servers.

#### IMPROVEMENTS (2):

- Drastically speeds up sequencer steps using sources.
- As Convertigo is now licensed on a Client Session basis, the mobile client sessions are monitored by the "Convertigo SE" licence key

#### BUGS (12):

- Decode the attribute type="string" as a string value for json requester
- Fixed the files corrupted when retrieving with MTOM (like PDF and other formats)
- Fixed random NullPointerException when executing a PutDocumentAttachmentTransaction
- Corrected a bug preventing creating projects from WSDL referencing an useless import.
- Corrected a bug in WSDL generation of a project using a wsdl reference. 
- Corrected a bug in Schema generation for projects using internal  XSDs
- Documentation updated to match the actual behavior for "Max iterations" property in step Iterato
- Fixed mobile application remote building using couchbase-lite
- Remove html tags of tooltips in the new object wizard
- Fixed the 5250 issue: error message erased immediately
- Updated Content-Security-Policy meta needed for fullsync operation within test platform.
- Fixed a bug preventing creating projects from WSDL referencing an useless binding.

---

## 7.3.3 

#### IMPROVEMENTS (2):

- A project can be configured to allow a CORS "Origin". Can solve iOS 9 issues
- It's possible to choose how to send file (base64, MTOM) with a SOAP request. Can also choose to allow download of SOAP attachments.

#### BUGS (9):

- In Studio, importing a project with undefined symbols now correctly opens the symbol creation dialog
- The JSON HTTP transaction also handle simple JSON type responses (string, null, boolean, number)
- In Studio, no more error message when using the + or - key on a selected Project object
- Statement GetCookies also works with quote in cookie values
- Fix schema generation of the ReadCSV step with special column names
- c8o.fullsync.js: fs://.post with _use_policy merge also works if the document doesn't already exist
- In Studio, fix the import of WSDL with <wsp:Policy> tag
- Testplatform now handle simple quote for variable values when execute in the Execution result div
- Studio ReferencesView works with closed projects

---

## 7.3.2 

#### NEW FEATURES (1):

- Add a new SiteClipper Rule and a new HTTP & HTML Transaction's property to set the pool policy for its HTTP connection

#### IMPROVEMENTS (2):

- Studio: engine can load only opened projects
- The transactions' and sequences' accessibilities are prepend to their names using icons

#### BUGS (2):

- Fixed the Flash Update loop case for applications built before CEMS 7.3
- Fixed the URL used by the HTML transaction,  uses the 'Sub path' property

---

## 7.3.1 

#### IMPROVEMENTS (4):

- Allow for Couch document create, xsd types: negativeInteger, nonNegativeInteger, nonPositiveInteger
- Support CouchBaseLite 1.1.0 for mobile fullsync projects
- Use cordova 5.1.1 for remote build of mobile templates and samples
- Add a JBoss configuration file in convertigo's war files.

#### BUGS (7):

- FullSync design documents also synced at deploy time and when fullsync engine properties changed
- Change a variable to a multivalued doesn't lose its assigned schema type
- The count of used licences keys displayed in the admin is now fixed
- Mac OS X Studio can now run requestables using the right click without an error dialog
- changelog.txt restored in the studio folder
- The "Log4J devices output logger" now works with the "Inherited from root logger" valu
- No more error on editing a SQL query with a dynamic variable

---

## 7.3.0 

#### NEW FEATURES (10):

- Convertigo Schema can be automatically validated in the schema view by a toggle button.
- Convertigo now interfaces to Google analytics to report events on transaction and sequence executions.
- Convertigo now features a new private "App Store" to enable pilot deployments
- New CouchDB connector that allow interaction with a CouchDB or CouchBase server : databases, documents, services
- A new google polymer mobile template demo is now included in the sample and demo wizard.
- Convertigo now features the FullSync connector enabling complete disconnected mode offline scenarios.
- HTTP connector method can be overridden via a new property.
- You can modify logging levels directly form the Log Viewer console in the eclipse studio.
- An AngularJS/Ionic sample project is now added the the demo and sample wizard.
- The .json and .jsonp requesters handle "type" attributes of JSON transaction to conserve the JSON forma

#### IMPROVEMENTS (16):

- Upgrade platform to the latest Eclipse 4.4.2 and Java 1.8.0_45
- Default jQuery framework is upgraded to 2.14 and jQuery Mobile to 1.4.5
- iOS config.xml is set by default to webviewbounce=false to prevent application to "bounce"
- CTF application using formatters can now use the $elt parameter to access the target element to be formatted.
- By default, the "graying" of the mobile app while waiting server's response is disabled, now only the wait spinner is displaye
- Now, call transaction steps are displaying the called target transaction's icons as their icon.
- A new "Open documentation" link has been added to the Eclipse Convertigo menu
- Harmonization of menu studio icons
- The engine log viewer is now limited in character size and not anymore in lines, preventing out of memory errors for large logs contents.
- Mobile application remote builds are now isolated by endpoints and built revision is available on the test platform
- Engine Logs in the standard eclipse console can be disabled to prevent out of memory errors for large logs contents.
- The local build now handle IPhone 6 & 6+ icons and splashes sizes
- Local build now support cordova 4
- The mobile remote build use the Phonegap 3.7.0 version by default
- The .json requester return a header "Content-Type: application/json" and the .jsonp "Content-Type: application/javascript"
- Now, call a SOAP web service causing a SOAP fault, will automatically generate a standard C8O error.

#### BUGS (34):

- Fixed a bug where no data were written in file if there were only one data line and if title line was set to true.
- Fix the import of an old project changing its name
- The extra parameter __content_type works with .json and .jsonp
- The "admin" logger in "INFO" doesn't cause endless messages in the log viewe
- The "Sequence" tab now show the call_XX steps results in the left pane
- Renaming a transaction referenced as "start transaction" in a project now also renames the referenc
- Fixed stack overflow when executing recursive sequences [support case #qa dev]
- 'Product version check' unchecked do less engine logs
- Fixed, In test platform, default connectors and default transactions are now automatically opened (not collapsed).
- Fixed, "org.apache.xmlbeans.XmlException?: error: Prefix can't be mapped to no namespace: http" when importing some WSDL
- Fixed, ReadXML step with XML files containing same elements with different namespaces.
- Fixed, setting a variable visibility to none (-1) now hides the default value.
- Fixed, delete project after adding new WSDL reference.
- Fixed, variables  with visibility set to -1 are still visible in request using XSL templates.
- Fixed, moving a Call transaction having its "connection string" property sourced from an other step causes a "broken source"
- Fixed, "Broken source" is now shown on an invalid source for a call transaction "Connexion string" property
- Fixed, button for creating tag_name rule for javelin connector now sets an valid XML tag name by default.
- Fixed, creating XMLTagname rule is now taking selection zone in account.
- Fixed, Studio freezes when quitting without saving project.
- Fixed, removing servers definitions in deploy window works as expected.
- Fixed, studio freezes when uses clicks "show in picker" on an "unsourced" iterator
- Fixed, removing an open Sequence_JS step now closes automatically the editor tab.
- Fixed, importing a WSDL Schema and click "finish" without setting/changing the combo box creates an empty reference
- Studio Linux: can clear a default value with the "X" butto
- The parameter __header_<header-name> do not modify the "HTTP headers" property of the transactio
- Fixed, aborting sequences calling other sequences now aborts the whole sequence tree
- Fixed, project name is now displayed on local build's summary dialog for Windows Phone 8 builds. 
- Fixed, Copy Step schema when source is a Call_sequence whose schema was not generated before
- Fixed, Search in studio log view now resets result count when search string changes.
- Fixed, local cache is not using _uid as cache key anymore.
- Fixed, logs containing heavy messages such a base64 image dumps taking a very long time to be indexed for the log viewer, has been optimized.
- The local build prevents preferences duplication for the generated config.xml
- Fixed, using __uri variables to modify dynamically the HTTP connector's URI now doesn't modify the transaction definition.
- Fixed, logging a " " (space) using log.message() causes log information to be displayed in the wrong columns

---

## 7.2.5 

#### NEW FEATURES (1):

- The SiteClipperConnector can use authentications (Anonymous, Basic and NTLM V1 and V2)

#### BUGS (4):

- JsonHttpTransaction now handle Long and null type of values
- JsonHttpTransaction JSONP response detection doesn't break regular JSON responses
- Steps containing IfFileExistThenElse step can be copy/pasted
- Add an engine property to defined the max child elements under root of schema dom instance (default 100, previously hard coded at 50)

---

## 7.2.4 

#### IMPROVEMENTS (1):

- New Studio registration wizard for PSC : registration on the new Convertigo website

#### BUGS (9):

- Fix the character encoding issue with the JS editor on the Windows Studio
- Remove step_id for complex variables of call transaction/sequence step
- Ignore comment line -- (double dash) in SQL multi lines queries
- Prevents studio freeze when closing an HTML connector editor while its transaction execution
- Source XPath can be dropped on call transaction/sequence variables
- No more studio's error log message when using the reference view and an HTML connector containing a function statement
- Use the right Cordova platform name for Windows8 local build
- Fixed the null pointer exception on empty JSON transaction's answer
- Prevents a stack overflow error in Studio when running a recursive sequence. Also running a sequence will disabled call transaction steps will not open automatically these connectors.

---

## 7.2.3 

#### IMPROVEMENTS (1):

- SiteClipper also handle PUT, DELETE, HEAD, OPTIONS and TRACE HTTP verbs.

---

## 7.2.2 

#### IMPROVEMENTS (1):

- HttpConnector support NTLMv2.

#### BUGS (1):

- Browser window of HTML Connector on server see the real window size.

---

## 7.2.1 

#### IMPROVEMENTS (9):

- Landscape splashscreens were added for Mobile applications, in default new project and in samples (Mob TV, Vacations, and US directory)
- Added Windows Phone 8 platform in Mob TV mobile sample
- Removed unused icon 'icon62tile' from Windows Phone 8 platform
- Improved the Source picker property editor: the source picker editor now expends the sequence until the current step (with the *) so it is easier to see where we are, and selects the previously pickable step in tree.
- Improved the Error structure step display in project's tree in Projects view
- Improved SAP transaction: SAP export parameters are correctly managed by SAP transaction
- When re-importing a SAP transaction, if BAPI definition has changed, now retrieves fresh data
- Improved drag-and-drop on a Concat step: drag-and-drop from the Source picker view to a Concat step now adds this source at the end of the list of sources of the Concat step
- Now the error structure step has an output schema: we can pick a source on it

#### BUGS (12):

- Fixed: Unhandled event loop exception - StackOverflowError when two projects reference each other: now the schema are correctly generated even when two project reference each other
- Fixed bug: when renaming a project at import, objects of the project named with the same name were also renamed, now only the project object is renamed. Call transaction / call sequence steps also are changed, so they reference the new name of the project.
- Fixed bug: Can't select an Iterator inside the source picker editor. The iterator step is once again pickable in the source picker editor.
- Fixed: set the default name empty for new mobile application (use the project name)
- Fixed schema bug when importing two web service references: sometimes a second imported WSDL override the first one's schema. Now each imported web service reference leads to properly separated imported WSDL files in dedicated folders inside wsdl folder.
- Fixed bug: when 2 sequences have the same name, in 2 different projects, executing one of the sequences displayed the response in both sequences editors. Now each sequence editor only contains its own response.
- Fixed bug: copy / paste an Error step now does not lose the sources that could be defined in smart type properties
- Fixed bug: executing a SAP transaction now retrieves fresh data (not data from JCO cache)
- Fixed NullPointerException when importing from a BAPI with no input parameter
- Fixed bug: now we can pick a source on nested iterators
- Drag & drop from the source picker view also works with MacOS & Linux
- Fixed bug: NullPointerException that could appear when showing step XML in the Source Picker view

---

## 7.2.0 

#### NEW FEATURES (11):

- Convertigo Studio now has a local build feature based on an existing locally installed Cordova build system. A new right-click menu on the Mobile platform enables user to run local build locally on their workstation.
- Mobile device logs are now available in Server engine logs. They are sent by the device to the server when activated (activation is made using  variable: C8O.vars.log_remote) and following the log level defined in the server.
- Added a new SAP connector and SAP transaction objects for connections to SAP environments through JCO connector
- Added a new right-click menu entry 'Create steps structure from XML' on sequence and container steps, that will automatically create a bunch of steps in the sequence/selected step to generate the appropriate XML in sequence ouput
- Added a new SAP Logon transaction to be able to change credentials on SAP connector.
- Added a new API in client JavaScript (in c8o.cordova.device.js library): the client/mobile JavaScript code is now able to retrieve built application information using the C8O.getCordovaEnv(key) method. The key parameter is the key of the value to retrieve, such as applicationAuthorName, applicationAuthorEmail, applicationAuthorWebsite, applicationDescription, applicationId, applicationName, builtRevision, builtVersion, currentRevision, currentVersion, endPoint, platform, platformName, projectName, and uuid.
- A new property is added to Mobile application object, Splashscreen hiding mode, allowing to configure when the splashscreen should be removed (automatically before/after Flash Update, or manually with JavaScript code)
- A new logger is created in Server, named 'device logger', dedicated to mobile device logs.
- For iOS platform, the local build feature copies resources with their folders in Xcode environment.
- Added a new SAP BAPI JCO project available in 'New Project wizard' 
- Added local cache JavaScript library in Convertigo scripts in c8o.cordova.device.js (available for Android and iOS)

#### IMPROVEMENTS (15):

- Removed MSXML support for internal Document.
- The 'Import symbols' feature in Administration Console is now improved with a new mode that permits to add new symbols by merging them with actual symbols
- Improved Local cache JavaScript library so it works now on Windows Phone and Windows 8 client apps
- The 'Export symbols' functionality has been improved to be able to choose the symbols to export (and not export the whole list of symbols of the server)
- In the Studio, we removed the Browser tab next to the response XML tab in the connector/sequence editors. The Studio now uses the .pxml requester to execute any transaction/sequence contrary to before (it used to use the index.html page in the Browser tab).
- Since Convertigo 7.1.0, SVN is not delivered in Studio anymore. The Operating Guide has been improved to complete the Studio installation documentation to add the SVN kit plugin installation procedure.
- The Operating Guide has been improved to add the Studio installation procedures on Linux and Mac platforms.
- The JSON HTTP transaction now handles JSONP responses.
- Improved Mobile application display in Test Platform: now the current revision number is displayed for each platform, in addition to built version number and current version number
- In Studio, a dialog box is displayed after the 'Local build' ends, with success or error message.
- Improved the SQL connector to detect closed connection, not retry in this case, but re-opening a new connection
- In Reference Manual, in Convertigo Objects documentation, now object properties are sorted by alphabetical order of display name.
- Add support for Cordova 3.6.3 in config.xml of Android and iOS platforms
- Mob Tv and US directory mobile sample projects now include Local Cache feature
- At first Studio startup, an 'SVN discovery connectors' popup is shown to install SVN connectors. If you close this window, you had no way to repopen it later (so basically you could not use SVN). Now a new 'Open SVN Discovery Connectors' entry in Convertigo menu allows to reopen this popup.

#### BUGS (13):

- Fixed bug in map in US directory and search room mobile sample projects
- Fixed Mob Tv mobile demo issue on Windows Phone 8
- Fixed bug: when opening Source picker in a step: if previous steps are not generating XML, we now prevent them from being selected (they are displayed in red), and, if no previous step is available in Source picker, a message is displayed
- Fixed bug when creating a new SQL project, you could not name it "sql"
- Fixed: ArrayIndexOutOfBound exception when 'Right-click' => 'New' => 'Step' on a 'Set authenticated user' step, 'Get authenticated user' step, 'Remove authenticated user' step, 'Set in session' step, and 'Get from session' step
- Fixed: Since version 7.1.0, Mashup demo was not working anymore, due to scripts incompatibility between all projects.
- Fixed ClassCastException when deleting a closed project
- For Apple store and Google play store, we need to declare an app version number. Fixed bug: the application number version is now automatically added as store version number in config.xml files of iOS and Android platforms.
- Fixed a bug where the generated xml file by the writeXML step could not be deleted until the Convertigo Engine is stopped.
- Fixed: Convertigo Engine froze when using XML HTTP Transaction for downloading XML with BUM
- Fixed bug in documentation, in 'Rewrite location header' site clipper extraction rule: the rule rewrites 'Location' header and not 'Content-Location' header
- Fixed bug in SMTP send step: when no source is selected, it correctly uses the sequence's output XML
- Fixed writeXML step bug when 'append' property is set to true and 'encoding' property set to UTF-8 breaks generated XML file.

---

## 7.1.2 

#### IMPROVEMENTS (9):

- Now screen class exit handlers (HTML and Javelin/Legacy connectors) support "Continue" return value, similar to empty or "" return value
- Added more explicit logs on Certificates manager in Convertigo
- *.p12 certificates are now managed by Convertigo. To add a new certificate, use the Administration Console, in Certificates page: *.p12 extension files are now authorized.
- Added HTTP response headers in information displayed in HttpInfo tag in HTTP transaction XML response
- Now Convertigo engine does not sent HTTP 500 by default on errors/exceptions
- When a variable is renamed, a popup is shown to propose to also rename its references (for example in call transaction or call sequence steps).
- Added a "change to..." option between screen class entry and exit handlers, and also between "Default transaction entry" and "Default transaction exit" handlers
- When a variable is renamed, if one of the two buttons (in all projects and in local project only) is pressed, we also rename the variable in the local test cases.
- Improved icons for IfIsIn, IfIsInThenElse, IfFileExists and IfFileExistsThenElse steps

#### BUGS (11):

- Fixed bug in Test Platform: executing a transaction or a sequence does not launch twice the request to Convertigo anymore
- Fixed Google map bug on several mobile sample projects (US directory, conference rooms sample, local cache sample)
- Fixed bug: In a sequence, when you used the "Change to" contextual menu to change an IfExist Step to a IfIsIn step, the step name was appended with a "1" and so on if you repeated the process. Now the name is kept unchanged
- Fixed bug: SQL transaction schema was erroneous for "Element with attributes" and "Flat element" Output mode
- Fixed bug: "Import WS Reference" or "New -> Reference" when choosing a local file thanks to the "Browse..." button, the URL is now correctly updated so the file can be imported
- Fixed bug: in IE11, C8O library performed .cxml request to Convertigo instead of .xml or .pxml, even when no XSL transformation is used (only for project not using the CTF). This was corrected by the add of a new value "none" for "xsl_side" C8O variable defined in the custom.js file
- Fixed bug: when using "Change to..." between ifXpathExist and ifXpathExistThenElse statements, the xpath was escaped a second time, corrected no
- Fixed mobile projects loading div z-index: was not superior to all jQuery Mobile elements, now is passed to 9999999, should be enough
- Fixed bug: when you execute a transaction or sequence from the test platform, selecting XML mode, now displays correctly the XML response and not only the inside text contents
- Removed "Update schema..." entries in right-click on Sequence menu: since 7.1.0 version of Convertigo it was not useful anymor
- Fixed bug: "Change to" icons were not displayed in right-click men

---

## 7.1.1 

#### NEW FEATURES (4):

- Global symbol values are now configurable using environment variables
- Support oAuth authentication protocol to access REST apis with HTTP connector
- Added a new Twitter library project, named lib_Twitter, to Convertigo Studio. It enables users to use the Twitter REST.JSON API.
- Environment variables are now displayed in Global Symbols page in Administration Console, as they can be used in symbol values

#### IMPROVEMENTS (12):

- "userReference" attribute is now present in transaction schema when using "Update schema from transaction's definition" contextual men
- When removing a Mobile platform from a project, now a confirmation popup proposes to remove or not the associated resources
- When removing an HTTP connector using SOAP templates, now a confirmation popup proposes to remove or not the associated SOAP templates folder from resources. When removing a Javelin connector containing Traces, now a confirmation popup proposes to remove or not the associated Traces folder from resources. 
- Improved "Import symbols" pop-in message box style
- Added "data-c8o-call-action" to CTF completio
- When importing a WSDL, transactions are created with the HTTP headers property containing SOAPAction header with empty value
- Added status bar plugin by default in iOS platform config.xml file to avoid problems with the status bar in iOS 7
- Environment variables are now displayed in Home page in Administration Console
- Status bar is now displayed by default in Android applications
- Disabled components highlighting when clicking on them in Windows Phone
- Improved SMTP send step: Default values for Content-type property have been enhanced to add the UTF-8 charset
- Improved the "New Reference" wizard, when choosing the "WS reference" object: harmonized this form with the two others that allow to create a new WS reference ("New Project" wizard and "Import WS reference" in right-click menu on Project

#### BUGS (13):

- Fixed bug in Site Clipper project: the default disabled extraction rule for JavaScript screenclass now also replaces the ":" character between host and port by ","
- Fixed deployment errors of Convertigo projects on cloud
- Fixed bug: deleting all running connections in Studio's Administration Console does not prevent any further execution anymore
- Fixed bug: when an app was built with Flash Update build mode to light, then the resources were not updated and the app was not working 
- Fixed bug: error popup about ReferenceObject was displayed each time a modification was made in a project
- Fixed a Nullpointer Exception in HTTP transaction for HTTP HEAD verb requests
- Fixed calls to Stored Procedures in SQL transactions.
- Fixed bug in documentation: "If" statement link in Table of Content was pointing to the CTF "If"
- Fixed bugs in "Import WS reference" and "New SOAP web service project" wizards: wrong URL or empty URL now displays an error message and does not create the project/connecto
- Fixed bug: 'References' folder of a project was badly displayed (Undefineds) when editing a project in Projects page of Administration Console
- Fixed bug: when opening a project developed in an older version of Convertigo in 7.1.0, the "Connection string" property of Call transaction step was not correctly updated, the source's XPath was bugge
- Automatically disable Flash Update when BlackBerry 10 platform is detected, as it can not work for now
- Fixed bug: it is now possible to create a new Reference from References folder

---

## 7.1.0 

#### NEW FEATURES (33):

- Added a new __removeNamespaces reserved parameter in Convertigo that allows to remove all namespaces from the XML output
- Added the new FlashUpdate feature for iOS, Android and Windows phone 8, allowing to easily update mobile application without redeployment in stores
- The SQL transaction can now execute several SQL queries inside one transaction. The auto-commit property is added to SQL transaction in order to define the auto-commit functionality (after each query, after the last query or no auto-commit - the user needs to specify "COMMIT;")
- Added the Push Manager library project in Convertigo, enabling to store devices in a database and finally use the Push feature
- Released MacOS version of Convertigo Studio, based on Eclipse 4.3.2
- Released Linux version of Convertigo Studio, based on Eclipse 4.3.2 and Java 1.7.0
- Added the new Push step to easily send Push notifications from sequences, and added the Push feature in C8O core library to easily retrieve push notifications and manage them in the mobile applications
- Added new WSDL import schema, XSD import schema and XSD include schema reference objects
- Added three new methods to context object API to retrieve interesting URLs:  "context.getAbsoluteResquestedUrl()", "context.getConvertigoUrl()" and "context.getProjectUrl()"
- SQL transaction with a Query parameterized with variables is now protected from SQL injection, using two syntaxes to use variables in the SQL query: {variable_name} activates the SQL injection protection, and {{variable_name}} does not activate the SQL injection protection.
- Added two new functions in C8O object API: C8O.formToData($form, data) and C8O.appendValue(data, key, value)
- Added the new Sort step: a step that sort a list of elements from a source (text, number, date,...)
- Added an 'Open test platform' entry to the right-click menu on Project in the Studio to open the test platform on the project. Also added 'Launch Fullscreen' and 'Launch in test platform' entries to the right-click menu on Mobile devices in Studio to test the mobile application fullscreen or in the test platform.
- Added the ability to configure a Mobile builder account at Convertigo engine level (in the Administration Console's Configuration page). Also added the ability to configure certificates/keys titles and passwords for each Mobile platform at Convertigo engine level (in the Administration Console's Configuration page). They are used by default if not overridden in projects (in Mobile application and Mobile platforms objects). 
- Each mobile platform can be built separately now in Convertigo, using separated resources and specific config.xml dedicated for each platform
- Added the ability to use a Mobile builder account by project defined in the Mobile application object in the project. Also added the ability to override the certificates/keys titles and passwords for each Mobile platform in the project.
- Added three steps to set, get and remove authenticated user from context/HTTP session, invoking the context JavaScript methods doing so
- The SQL Connector now supports JDBC configuration using JNDI datasource
- Added a new property 'Authenticated user as cache key' to sequences/transactions to be able to use the authenticated user from context/HTTP session as an additional key to the cache
- If a project contains undefined global symbols, we have now the possibility to declare all of them in the Global symbols list of the engine. In Studio, when right-clicking on a project, and using the "Create Global Symbols" entry. In the Administration Console, in the Projects page, when editing a project, you can see the list of undefined global symbols and a button allows to create them
- Added a new Error step that allows the developer to generate an applicative error XML output from its sequence. It generates an XML "error" tag name with "type" attribute value "project", and sub elements such as "code", "message" and "details"
- Added new steps "Set in session" and "Get from session" to store and retrieve data from HTTP sessio
- Added HTML completion for Convertigo Templating Framework and JQuery mobile attributes
- Added data-c8o-call-action attribute in CTF, added next to a data-c8o-call attribute, it performs a page change just before the call
- In Administration console, it is now possible to import a whole global symbol configuration through a standard properties file. It completely overrides the configuration previously existing in the server. This old configuration is saved as a file in the same directory.
- Now HTTP connector supports NTLM authentication: some properties were added/updated in HTTP connector to be able to configure this NTLM authentication
- Added a new Web service reference object
- In Administration console, it is now possible to export a whole global symbol configuration to a standard properties file.
- Added new properties in SQL transaction to be able to customize the tag name of each data row and columns in XML output (default are "row" and "column"
- Mobile application object has been updated and Mobile devices removed and replaced by Mobile Platform objects (matching possible platforms that we can build using PhoneGap)
- Starting from version 7.1, each version of Convertigo Studio is installed in a separate Directory in the user's folder: you can install several versions of Convertigo Studio on your machine, and no more problem of administrator rights needed to run, splash screen always dispayed at startup, etc.
- Added a new mobile sample project including local cache functionality
- Added Vacations mobile sample project in Convertigo Studio

#### IMPROVEMENTS (39):

- Added a warning popup if a user tries to build a mobile application on Convertigo Test Platform with endpoint located on "localhost" or "127.0.0.1"
- The Deploy window has been improved to not be modal, i.e. to be able to reduce it thanks to the 'Run in background' button
- Now the Convertigo server protects sensitive files from being easily downloaded in the project scope
- The HTTP transactions can add HTTP status information in their output XML: a new node includes the HTTP status code and the HTTP headers from HTTP response, as well as the raw HTTP data in case of error
- Improved the context cryptography functions: added new markers to differentiate old crypto hexstrings from new ones
- Added a 'Test connection' button in new SQL project wizard to test the connection to the database. Also added a 'Test connection' button in the SQL connector editor.
- Changed some icons in the Administration Console (Test platform, Cache and Test Project icons) for harmonization purpose.
- Added placeholder jar files for the DB2 SQL driver (we can not deliver them), they only have to be replaced by real DB2 jars to be running in Studio and Server
- Question mark buttons are present on every page of the Administration Console,to ease the access to related page on Convertigo help
- It is now possible to load a project including undefined global symbols in Studio or Server. In Studio, a popup indicates the error, allowing to create automatically the missing symbols or ignoring them. In any case, logs are added to indicate that a symbol is missing.
- Added a new output mode "FLAT ELEMENT" for SQL transactions
- Upgraded Eclipse to 4.3.2 and Java to 1.7.0 for Windows Convertigo Studio
- Improved the "Creation date of the context" and "Last access date to the context" icons in "Connections" page of the Administration Console
- Changed 'expression' property to a single line JavaScriptable input for 'Context Add text node', 'Log' and 'Set context' statements
- Improvement of standard C8O error schema: all Convertigo Exceptions generate an error tag with "type" attribute value "c8o"
- At execution of an object containing an undefined global symbol, a popup is displayed to show the error (in Studio). In both Studio and Server, the execution is stopped and logs are added to indicate that this symbol is missing.
- jquery.mobile 1.4.2 is the default version to use for mobile application (jquery.mobile 1.3.2 still available in 6.3 resources)
- Ship server installer with Tomcat 7.0.47
- Ship the studio with an embedded Tomcat 7.0.47
- Mobile application are built using phonegap/cordova 3.3.0 (and 2.9.0 for deprecated platforms).
- Admin Console Projects page has been improved: UI is lighter and more organized, the page allows more functionalities (like "Delete all", undefined Global Symbols management, etc.
- (! undefined symbol !) message is displayed in the Projects view next to objects that have an undefined Global Symbol in their properties values
- Import WSDL popup improved by adding a "Browse" button to choose a local WSDL fil
- In Projects view, the contextual menu on Project object has been reorganized
- Improved the Global Symbols administration page UI and functionalities
- Improved the Certificates page UI in Administration Console
- In the New SQL transaction wizard, added the possibility to directly set the SQL query of the SQL transaction thanks to a new page in the wizard
- Response lifetime property of sequence/transaction objects now proposes a more user-friendly editor that includes a generator tool to help computing the lifetime value.
- Now Convertigo responds to all HTTP verbs (PUT,GET,POST,DELETE,HEAD). The verb can be retrieved inside JavaScript code using: context.httpServletRequest.getMethod()
- Created all icons and splashscreens resources used by config.xml in mobile platforms resources 
- When removing a variable from an SQL query, now a confirmation popup proposes to remove or not the variable from the SQL transaction
- Improved HSQLDB sample project with SQL transaction executing multi queries
- Updated the Studio installation procedure in Operating Guide
- In Configuration page of the Administration Console, empty password inputs are emptied when clicking on the field for edition and starting writting. Not empty password are filled with dots and not emptied when editing.
- Now SQL connector/SQL transaction can execute COMMIT and ROLLBACK statements
- Speed up beans' loading and initialization at Studio startup
- Improved the Write file steps (Write XML, Write CSV) XML output: added data about URL and paths of the generated file in order to be sourced later in the sequence
- Import WSDL popup now proposes to use credentials if the distant WSDL is protected by a basic authentication
- Global Symbols page in Administration Console now prevents from adding or editing symbols including forbidden characters: '{', '=' and '}' are forbidden in symbol name

#### BUGS (40):

- Fixed bug about SQL transaction variables that were not always correctly replaced by their value in the SQL query at execution
- Fixed bug : no more listview refresh warning in CTF logs
- Fixed bug: jQuery mobile widgets dynamically created thanks to the CTF are now correctly refreshed and displayed
- SQL transaction variables are now correctly created when using {var_name} or {{var_name}} in the SQL query
- Fixed bug missing syntax coloring and contextual help in JavaScript code editors (legacy transaction handlers editor, JavaScript property editors, etc.)
- Fixed multiple inappropriate errors seen in Error log view when opening and closing Studio
- Fixed bug: Convertigo now serves correctly resources including a space in their name
- Fixed issue with pooled contexts that fail to load during engine startup if one of the server's projects is broken
- Fixed bug: now changing from single valued variable to multi valued variable keeps the 'HTTP name' property value.
- Fixed copy/paste problem: when pasting several successive times, the pasted elements are not cut anymore but well duplicated.
- Fixed bug: the Carioca authenticated session is kept when changing project in the same context
- Fixed issue with certificates and empty passwords in Administration Console
- Fixed some cases where unrecoverable errors where not hidden by "Hide detailed information in case of unrecoverable servlet error" Configuration property
- Fixed bug: __header_NAME reserved engine variable can now be sourced in a Call_transaction step
- Fixed project edition (objects properties edition) in Administration Console
- Fixed bug: the "Ignore SOAP envelope" property in HTTP transaction now always removes the SOAP envelope from XML respons
- Fixed the display bug in Administration Console of global symbols list when one or more symbol contains a comma.
- Fixed the bug of "Deploy" window size: when deploying a project, the "Deploy" window is now correctly extended to a correct size and the deployment configurations are fully visible
- Fix repetitions in SQL transaction output when tables used in the query have several colums with the same name.
- Fix overwriting of attributes in SQL transaction output when tables used in the query have several colums with the same name.
- Fixed bug: Project comment property is now correctly displayed in Test Platform, including returns to line.
- Fixed completion pop-up alert in Studio JS editors.
- Fixed bug in Administration Console: after sorting a grid on one column (for example in Global symbols page or Projects page), the grid was limited to 20, now it displays all items
- Fixed 'New project wizard' bug on HTML connector: now the connector is created with the 'isHttps' property to true if we check the SSL box
- Fixed XML copy bug due to XPath evaluation when a Call Transaction/Sequence Step result is inside a picked Complex Step
- Fixed bad reference in 'Get Attachment' statement documentation
- Fixed bug: reserved parameters "__stub" and "__user_reference" are now usable with all Convertigo requester
- Fixed display bug in Administration Console: now a Global Symbol containing '<' and '>' in its value is correctly displayed and can be properly edited.
- Fixed bugs about projects synchronization and objects refresh, between Administration Console and Studio, when adding/editing Global Symbols
- Fixed malformed file path with WSDL import when clicking browse button
- The "Projects" View is now present in Convertigo Studio's "Resource" perspective
- In Projects view, broken source text in concat Step is now correct.
- Fixed Copy step schema
- Fixed: SQL query using Global Symbols are now correctly executed.
- Fixed message "Widget is disposed" when deploying a project from Studio to a cloud server
- Fixed bug: in Administration Console, opening a project's Test Platform opens a new tab, opening a second project's Test Platform now  opens a second tab and works
- Fixed some bugs with empty responses in cache for MobTV sample mobile project
- Fixed ConcurrentModificationException bug often happening at Studio startup when a large number of projects are opened and loading at the same time
- Fixed bug on Administration Console: when user session is expired, authentication to the administration console is lost, but some administration widgets did not show it: now when authentication is lost, the user is redirected to the login page.
- Fixed global symbols name and projects name sorting in Administration console.

---

## 7.0.6 

#### IMPROVEMENTS (1):

- Pobi proxy biller improved for new added features

---

## 7.0.5 

#### NEW FEATURES (3):

- Upadted BDF key algorithms in Pobi XSL plugin
- Added support of global symbols in Pobi XSL plugin
- Updated Pobi Fiben costs configuration file

---

## 7.0.4 

#### NEW FEATURES (1):

- Pobi plugins: evolution of configurable expiry date

#### IMPROVEMENTS (1):

- Pobi plugins: fixed biller exception on self closed tags

#### BUGS (2):

- Fixed bug: regression in context removal and session invalidation
- Fixed bug: issue on dynamic cache configuration

---

## 7.0.3 

#### NEW FEATURES (1):

- Pobi plugins : added handling of configurable expiry date

#### BUGS (4):

- DatabaseCacheManager : Fixed invalid sql request retry in case of SQLException
- Web Service interface : fixed issue of parameter's handling
- Pobi XML plugins : fixed NumberFormatException because of an untreated case
- Pobi XML plugins : fixed issue in handling of RALPH error

---

## 7.0.2 

#### NEW FEATURES (1):

- Added a new engine configuration parameter "Maximum number of contexts" that allows a Convertigo administrator to limit the maximum number of contexts created in a Convertigo serve

#### BUGS (5):

- Reuse already opened SSL socket, even for differents contexts
- Fixed external plugin issue : custom log using log4j DailyRollingFileAppender
- Fixed NPE when pretty print SOAP message
- Fixed bug about HTTP client blocking even when the transaction timeout is finished: now HTTP client uses the same timeout as the transaction
- Fixed bug about yyyy-mm-dd_custom.log created even if property set to false

---

## 7.0.1 

#### IMPROVEMENTS (1):

- Updated properties and improved and transaction's name check

---

## 7.0.0 

#### NEW FEATURES (3):

- Added a new mobile application object used to centralize all mobile application build parameters and mobile devices
- Added a new HTML connector extraction rule enabling to create a screen shot of the current HTML page
- Made xml generation statistics available as xml data

#### IMPROVEMENTS (2):

- Updated the test platform to get build parameters from the new mobile application object
- Added project version property in the test platform and the project list (in the web admin console)

#### BUGS (5):

- Fixed an issue with selection Xpath generation when a quote is in an attribute
- Improved automatic renaming of Call steps when transactions or sequences are renamed
- Fixed a JVM crash when accessing an SSL site through a card reader.
- Fixed possible NPE in ThreadManager
- Fixed issue preventing a certificate to be updated when its password is invalid

---

## 6.3.6 

#### NEW FEATURES (1):

- Added three new methods to context object API to manage the authenticated user:  "context.setAuthenticatedUser(userID)", "context.getAuthenticatedUser()" and "context.removeAuthenticatedUser()"

#### IMPROVEMENTS (2):

- Added logs in engine logger - TRACE mode - for debugging RSA management in Convertigo
- Improved the Legacy webization: now it works in IE11. Note that projects must be deployed checking the assembling XSL stylesheets.

#### BUGS (3):

- Fixed alert message when adding a new "Create event" statement in an HTML transaction
- Rolled-back the fix of 6.3.5 version "Fixed bug: no more listview refresh warning in CTF logs" => This fix introduced a regression and is rolled-back for 6.3.x versions. It will be fixed in 7.1.0 version
- Fixed date input JavaScript issue in Legacy webization projects

---

## 6.3.5 

#### NEW FEATURES (1):

- The Convertigo Templating Framework and Convertigo JavaScript libraries are compatible with Windows Phone 7.5 and 8 (in order to build PhoneGap application for Windows Phone)

#### BUGS (3):

- Fixed IllegalArgumentException in Administration Console when editing a project properties
- Fixed bug: Statistics popup now displays HTTP connector counts
- Fixed bug : no more listview refresh warning in CTF logs

---

## 6.3.4 

#### NEW FEATURES (1):

- Added a new "Statistics" menu entry when right-clicking on Project: opens a statistics popup about objects present in the projec

#### IMPROVEMENTS (1):

- The Convertigo Internationalization library documentation has been re-organized to be more understandable and the examples displaying has been improved.

#### BUGS (1):

- Fix: disable by default the axis2 administration console.

---

## 6.3.3 

#### NEW FEATURES (2):

- Added 'if' and 'if-not' verbs to Convertigo Templating Framework 
- Convertigo workspace folder can be opened from Convertigo menu in the Studio

#### IMPROVEMENTS (8):

- Screen class detection log in DEBUG mode showed too often, it is now set in TRACE mode
- Fixed context authentications bug: when a sequence or transaction authenticates a context, the http session is therefore authenticated and all contexts created within the same http session are automatically authenticated too
- Improved the CTF gallery: added the new 'if' and 'if-not' verbs
- Added Convertigo mobile website sample CTF project called sampleMobileConvertigoWebMobile in Mobile samples category
- The Convertigo Templating Framework documentation has been improved to display the examples in a better way.
- Renamed topics in CTF gallery demo project to be more specific
- The Convertigo Templating Framework documentation has been re-organized to be more understandable.
- Fixed __stub calls with 'Authenticated context required' property set to true

#### BUGS (9):

- Fixed abusive warn log 'Unnormalized name : "on[Please choose a screen class]Entry" for databaseObject (ScEntryHandlerStatement)'
- Corrected a bug in Mobile device object documentation, "Device screen height" property referred to width instead of height
- Fixed bug on Convertigo Web mobile demo project: did not work on server.
- Fixed i18n JS filename in CTF Gallery mobile project
- Fixed some bugs in MobTV demo project.
- Call transaction or sequence steps in 'Internal invoke' mode are aware of the AuthenticatedUser in context.
- Fixed call Sequence step in 'Internal invoke mode' with __stub=true
- Fixed waitForDataStable() legacy synchronization method on OS400
- Fix deployment failure on https://trial.convertigo.net

---

## 6.3.2 

#### IMPROVEMENTS (6):

- Added a property to put HTTP infos (status, headers, ...) into HTTP transaction results
- Added global symbols for keyring library project
- Improved keyring library by adding a functional sequence creating database and loading sample data from an XML file.
- Added sample mobile CTF project called sampleMobileUsDirectory in Mobile samples category
- Added sample mobile CTF project called sampleMobileMobTV in Mobile samples category
- Improved starting Studio wizard: added a proxy test connection button and re-enabled anonymous registration

#### BUGS (4):

- SQL query editor is now open centered on Convertigo Studio.
- Fixed the bug in "Deploy" window: when deploying a project, the deployment configurations are now visible
- Harmonized the use of ciphering key in keyring library project
- Splash screen is now centered on the main monitor (no more stuck at the edge of the main monitor)

---

## 6.3.1 

#### BUGS (3):

- Now allows to put content in HTTP PUT methods
- Fixed the Welcome page not being fullscreen at first startup
- Fixed double declaration of the document_ready hook in custom.js

---

## 6.3.0 

#### NEW FEATURES (2):

- Added the CTF component (Convertigo Template Framework): the fastest way to design a mobile or a desktop application
- HTML Connector : extraction rule to make a screenshot of the current screen class

#### IMPROVEMENTS (11):

- SqlTransaction now correctly handles "TRUNCATE TABLE" SQL request
- Added bi-directional "Change To" options between Element and Attribute steps (XML and JS) and between jSource and jSimpleSource step
- The "Execute" option is now before the "Execute from stub" optio
- Removed the phonegab builder account configuration from the administration console
- Added an option in test platform in order to enable mobile package download
- Renamed 'Convertigo' perspective to 'Convertigo API' and created a new perspective called 'Convertigo UI'
- Changed "Write binary from Base64" Step icon to be more representativ
- Prevent replay of requests when the C8O encryption plugin is enabled
- Improved SQL connections pool in order to support firewalled architectures
- Mobile projects have been passed to phonegap version 2.9.0 from 2.1.0
- Changed Convertigo Studio Welcome page (Help > Welcome) that presents links to "Getting started" videos

#### BUGS (12):

- Fixed an issue with variables encoding in HTML connector
- Fixed xmlCopy step wrong behavior when copy an empty result (cause step_id and step_copy attributes).
- Fixed the too long disabling or enabling of extraction rules selection
- Fixed removing sequence's context of cached responses
- Fixed popup exception for some disabled statements and fix folder colors
- Fixed an issue in HTTP variables's visibility when the "http name" property was different from the variables's name
- Added the possibility to change an IfExist step into an IfIsIn step and vice-versa.
- Added the possibility to change a jSimpleSource step into a jSource step and vice-versa.
- Fixed the default SQL transaction handler for new SQL project
- Fixed the mapping deletion in Certificates administration console.
- Fixed: When clicking in the reference view on an item contained in a closed project, it show an error message and the focus move to the closed project.
- Fixed a random error in connections servlet from Administration console.

---

## 6.2.6 

#### NEW FEATURES (1):

- Convertigo now includes a regrouping and minification functionality for resources served by the server

#### IMPROVEMENTS (2):

- Improved SQL connections pool in order to support firewalled architectures (for 6.2.6)
- Added a new logger dedicated to security token feature

#### BUGS (2):

- Fixed NullPointerException in Site Clipper when using proxy.pac in Proxy configuration
- Fixed database access problem when requesting a new security token after a long time of inactivity (for security tokens managed in database mode)

---

## 6.2.5 

#### BUGS (1):

- Fixed bug: when visibility is set to hidden in log, Complex variables in entry were still visible in logs (received SOAP envelop, and input document)

---

## 6.2.4 

#### NEW FEATURES (1):

- Added the possibility to configure a database to store the security tokens for several instances of Convertigo running in parallel to be able to share the list of valid tokens and accept requests made by clients whose token was generated by a "brother" Convertigo (available since 6.2.4 for 6.2 versions of Convertigo, since 6.3.5 for 6.3 versions of Convertigo and 7.1.0 for 7.1 versions of Convertigo

---

## 6.2.3 

#### NEW FEATURES (1):

- Enabled to hide system information delivery in case of unrecoverable error

#### BUGS (4):

- Fixed auto-increment number in legacy LU Device
- Fixed an issue with MTOM attachment when no filename was provided
- Fixed missing configuration option in weblogic.xml file (show-archived-real-path-enabled)
- Fixed issue in SiteClipper with absolute URLs containing dash characters

---

## 6.2.2 

#### NEW FEATURES (6):

- Added engine startup diagnostics
- Added byte array public encode/decode methods : encodeToByteArray and decodeFromByteArray
- Added 2 new SiteClipper extraction rules providing full JavaScript access (both request and response extraction rule)
- Any transaction or sequence can now easily answer a stub response (i.e. a fixed response stored in an XML file)
- Now mark the HTTP session as authenticated when using the engine.Authenticate admin web service
- Upgraded JQuery to 1.9.1 and JQuery Mobile to 1.3.1

#### IMPROVEMENTS (2):

- Modified the default value for the XML grouping property of SqlTransaction
- Prevented brute force attack on engine Authenticate service

#### BUGS (6):

- Fixed copy/paste of IfThenElse statements
- Fixed validation of XML tag names and attributes (they are now fully XML compliant)
- Fixed all requesters in order to correctly handle all the C8O internal variables (i.e. beginning with __)
- Fixed SocketException (connection reset) errors in SiteClipper
- Fixed issue with explicit context in case of file upload
- Fixed screen class choice algo in SiteClipper context

---

## 6.2.1 

#### NEW FEATURES (6):

- Microsoft webpart support for SharePoint
- Implement dynamic header management in HTTPStatements (__header_<Header_Name>=value)
- Added a new Google Maps project template
- Added Scribe library (allows OAuth authentication) and a sample project lib_LinkedIn  using this library
- Added a keyring library project
- Added a new JSON transaction for HTTP connector

#### IMPROVEMENTS (2):

- Improved test platform to allow not to send some requestable parameters
- Fixed slow web services in case of lots of sessions

#### BUGS (11):

- Fixed mobile application build through a proxy
- Fixed drag and drop issue when trying to paste an SQL transaction in an HTML connector
- can not move a XSLStyleSheet from a screen class to another
- Fixed a web admin issue with certificates: deleting a certificate now removes all mapping related to the certificate
- Fixed mouse wheel event that did not work in IE for date fields (in web admin log viewer)
- Updated PhoneGap Javascript file references in mobile projects (now uses cordova.js instead of phonegap.js)
- Fixed issue with Xpath migration and XmlCopy step
- Fixed log levels configuration in the admin log viewer
- Fixed NPE when using the References View in case of unloadable projects (wrong XML definition file for instance)
- Fixed Authenticate admin web service that did not correctly return the authenticated user name
- Fixed template projects containing an invalid project version property

---

## 6.2.0 

#### NEW FEATURES (15):

- A new sample project for Sequencer is added in the New project wizard
- Add an iterator step that works on JavaScript scope multivalued variables
- Add a simple file with the current version of Convertigo in the WEB-INF folder
- Added a new Engine Log view in the studio
- Created a new installation wizard for the studio
- Statements with XPath can now wait a delay if the XPath doesn't exist before doing the action
- Added new GetTextStatement
- Improved project versioning (added two new properties in Project object: version and Exported)
- JS engine now support JSON.parse and JSON.stringify
- Studio and server are now accessible in HTTPS (by default, port 18081 and 28443 respectively)
- Added a Test Platform User Guide
- Updated PhoneGap option file (config.xml) to PhoneGap V2
- Added a user reference input variable that is automatically inserted as an attribute of the root node of the XML response
- Added LGPL MySQL JDBC driver in the studio
- Added an HelloWorld sample

#### IMPROVEMENTS (36):

- Fixed Linux/Tomcat installation procedure (Operating Guide)
- Added new 'Export variables to main sequence' menu in order to easily import varaibles from CallTransaction or CallSequence steps
- Fixed tree scrolling issue when using drag and drop in studio's projects view
- Fixed SQL query not shown with computed variables when an error occurs on query execution
- Now takes in account in real time global symbols modification
- Improved error logging of ProcessExec step
- Masked sensitive properties (such as passwords) in studio properties editor and in web admin
- Now the studio automatically opens all needed connector's editors when executing a sequence invoking some calls on other sequences or transactions
- Multi-valued variables are now seen as a JS array of JS elements
- Http Variable reuse name for the HTTP name
- jIterator and Iterator steps now have a 'starting index' property, a JS index and item scope variables.
- Added 'continue' value to the "Result" property of screen class exit handler
- The source picker now automatically updates XPaths when navigating the source picker tree view with the keyboard arrows
- Deleting a screen class used as a synchronizer in a statement will change target to Default_Screen_class
- Improved the Administration Console Scheduler page, functionalities and graphisms.
- Multiple screen classes can now be selected in the "New transaction handler" window
- Withdrawed mobile connector from new connector wizard
- Perform some optimization after profiling observation
- Improved project loading in studio
- In CEMS web Admin log viewer, the Real-time mode is until now and active the auto-scroll
- Improved WS reference import (now automatically update schemas from discovered WS transactions)
- Changed Get URL, Get Nodes and Inject JS in browser icon statements to reflect Studio JS style (pink)
- Clicking on the Web Application QrCode from the Project Test Platform opens the link in a new page
- Now uses a JavaScript array of parameters (instead of a simple string parameter) for the command line of the ProcessExec step
- Changed css and images for legacy webization template
- Improved log text for proxy settings
- Disabled automatic update of sequence' schema
- Improved the Wait div (cf. weblib.js) behavior (now correctly handles parallel calls)
- New transactions and sequences are now public by default
- Added combo boxes for seconds (startDate, startEnd) into the logs viewer (Admin console)
- Improved several unexplicit popups messages and buttons in Studio
- Parallel steps now generate an ordered DOM (nodes are added in the order of the declared steps instead of the order of the threads termination)
- Removed auto-open project feature in studio
- The default transaction of an HTML connector is named XMLize
- Mobile Application name appears in the project testplatform
- xvnc.port added to Java system property if available (Linux)

#### BUGS (61):

- Fixed a Stack Error while creating new scheduled job if no jobs or schedules are present
- Fixed issue when trying to import a project
- Fixed REST project creation that failed in Studio new project wizard
- Fixed a threading issue (NPE) in case a parallel calls
- Fixed a authentication error on the admin or the testplatform when the HttpSession expire
- Fixed rendering issues with mobile projects (fixed viewport, splash screen)
- Fixed ReplaceString rule to rewrite absolute URLs in JS (for new SiteClipper project)
- Prevented several build processes to be executed at the same time if we click several time on the build button without waiting for the end of process
- Fixed issue with '\n' characters in object JS
- Fixed NPE when executing a project
- Fixed issue when trying to drag and drop a transaction in a sequence in order to create the related CallTransaction
- Fixed NPE when trying to execute a transaction
- New transactions or sequences are now public by default (instead of hidden)
- Fixed StackOverFlowException on jElement steps
- Fixed deployment of projects through proxy with authentication set in administration console.
- Fixed issue when sorting projects by name in the web admin console (the list was truncated)
- Fixed issue regarding sequence and connector having the same name: CallTransaction did not work in such cases
- Fixed text links of widgets menu in Administration console
- Fixed the use of default value when a Source return an empty nodelist
- Fixed issue when generating SOAP fault with 1.5 JRE
- Fixed issue with Chrome browser and web admin console (tables columns headers are not correctly aligned with contents)
- Fixed regression about new statement creation in studio
- Fixed Duplicate step message issue when destination file already exists and overwrite is false
- "Proxy manager" logger was missing in the configuratio
- Fixed AssertionFailedException in studio ("assertion failed: ContentViewer must have a content provider when input is set"
- Fixed "Certificates" page tooltip
- Fixed NPE in XMLCount step
- Improved exception logging while using jException step
- Fixed the shutdown of a server with a traceplayer
- Fixed NPE that sometimes occurred within a CallTransaction step
- Fixed a StackOverflowError when first saving project after having added a handler
- Fixed issue when adding a Call_function statement to an handler
- Fixed issue when trying to add a sequence and save project in an HTML project
- Fixed an exception about missing global_symbols.properties file
- Fix : prevent scheduler failure on scheduler.xml reading [support case #00001577]
- Fixed a migration issue
- Removed excessive migration logs in the engine.log
- Fixed typo in CRON Wizard "Days of week"
- Fixed 'Hours' positioning in the scheduler's CRON wizard
- XMLCopy step doesn't copy the whole XML of the source
- Fixed filename download issues under Chrome when downloading logs or projects from the web admin console
- Fixed 'Get Nodes' statement to use the current browser DOM
- Fixed References view exception when reference project link is broken
- Fixed regression in the web admin log viewer (start and end date combo boxes were not responding as expected)
- Fixed regression in the web admin log viewer in case of start date superior to end date
- Fixed NPE when invoking a non existant requestable
- Fixed issue with References view that remains empty when an used transaction is renamed or deleted
- Fixed issue while trying to create a new Sencha-based mobile project
- Fixed deployment error messages
- Fixed issue with the References view when renaming a referenced object
- Fixed ReadXML step that did not work with root element different than "document"
- Fixed an issue in Studio where all objects were passed in bold if you added a new object to project
- Fixed issue with XMLCopy step
- Fixed a broken reference in References view when renaming a transaction/sequence
- Modified variables icon for Call steps (transaction and sequence)
- Fixed documentation of List directory step (wrong description)
- Fixed SiteClipper transaction documentation (site clipper URL description was wrong)
- Fixed error when deleting contexts from the web admin
- Fixed issue with CXF stub when requesting Convertigo with MTOM
- Fixed NPE when trying to use a connector not opened in the studio thru the test platform
- Fixed issue when building mobile application (the Test Platform GUI returned too quickly with OK status although the build was not yet finished)

=---

## 6.1.12 
=
#### BUGS (1):

- Fixed NPE in vt220 emulator when not using the session monitor

=---

## 6.1.11 
=
#### BUGS (1):

- Fixed issue when adding a C8O engine variable (i.e. starting with "__") to a CallTR or CallSeq setup in internal invoke mod

=---

## 6.1.10 
=
#### NEW FEATURES (4):

- Added a secure protocol between C8O widgets, portals and C8O requestables
- Any requestable can now receive files as parameters in a HTTP multipart request
- (Weblib) added a first_call parameter and waitHide/waitShow methods
- Now conserved JS libraries from older CEMS in an archive folder

#### BUGS (9):

- Fixed issue about secured connection required property that was not correctly applied
- Fixed end of context transaction that locked incoming requests [support case #00001596]
- Fixed the logviewer realtime mode in the web administration
- Fixed default weblib wait div that covers the whole screen
- Fix : Convertigo testplaform documentation link fixed
- Fixed issue in studio when selecting the 'secured connection required' option for a requestable (unable to execute it)
- Fixed issue with HTML transaction's variable creation
- Fixed wrong generated URL in test platform for server webapp not named "convertigo"
- Fixed issue with default ciphering passphrase

---

## 6.1.9 

#### NEW FEATURES (1):

- Now allows to globally enable or disable the response cache feature (thanks to a new engine property)

#### IMPROVEMENTS (2):

- Now allows to directly setup the mobile application name inside the config.xml file
- Improved the UsageMonitor (added thread statistics and dead lock detection)

#### BUGS (5):

- Fixed iOS mobile application builds (due to PhoneGap 2 specs)
- Fixed SiteClipper connector issue on Cloud causing an "unkown http method GET" error for URI with special characters such as space
- Fixed NPE in SiteClipper connector (getHostConfiguration)
- Fixed bug in the test platform: the Projects table header is no longer hidden when the list of projects is loaded
- Fixed proxy configuration handling in site clipper

---

## 6.1.8 

#### NEW FEATURES (3):

- Improved SmtpSend step: attachment files can be added to the email
- Improved SmtpSend step: XSL transformation can be performed on the sequence's XML
- Now Convertigo allows the user to modify the .car file name (instead of requiring the same name than the project it contains), the project can be imported or exported directly with a different name

#### IMPROVEMENTS (5):

- Improved SmtpSend step: 'Subject' and 'Recipients' properties are now javascriptable
- Now test file existence before exporting to XML or CAR
- Set beans logger log level on 'Inherited from root logger' by default
- Hide proxy password in DEBUG log level
- Fixed mix between JS String and Java String in jElement step

#### BUGS (4):

- Fixed IfThenElse and IfXpathExistsThenElse statements failing to reload
- Fixed StackOverFlow exception when changing a property for multi objects selection
- Fixed CallSequence's default multi valued variables converted to one single valued variable
- Fixed NPE when trying to delete a context in the admin console

---

## 6.1.7 

#### NEW FEATURES (1):

- Added a new translation extraction rule for legacy connectors

#### BUGS (4):

- Fixed a display bug in testplatform (double display of data after logging)
- Fixed a bad log in web service (about hidden variables)
- Fixed SQL result error when using  implicit columns in SQL request (returning the memory reference of data instead of the data itself)
- Fixed deadlock when using sequence and RemoveContextStep

---

## 6.1.6 

#### NEW FEATURES (1):

- DocumentCompletedTrigger can be stop on html alert

---

## 6.1.5 

#### BUGS (3):

- Fixed svn link lost when a project is reloaded from xml
- Fixed ConcurrentModificationException when using InputVariablesStep in case of heavy load server
- Fixed issue with CallTr/CallSeq under Websphere (because of session ID containing underscores)

---

## 6.1.4 

#### IMPROVEMENTS (1):

- Improved performance of XMLCopy step when using very big data

---

## 6.1.3 

#### IMPROVEMENTS (1):

- Fixed SQL connector 'connectionString' override only when needed

#### BUGS (2):

- Fixed issue with legacy connectors that were resetted after each transaction
- Fixed new crypto algorithm

---

## 6.1.2 

#### NEW FEATURES (7):

- Added a new IfXPathExistsThenElse statement
- Added two SQL connector sample projects based on an HSQDB database and an Excel file database. Both are in a new category of the New Project Wizard, named "SQL Connector samples", in Convertigo > Samples
- Added MTOM support for file uploads
- added a new jAttribute step
- Added a new References view meant to display relations between Convertigo objects (screen classes, transactions, sequences)
- Create icons for "References" vie
- Added accessibility to requestables

#### IMPROVEMENTS (9):

- Changing the targeted Transaction/Sequence in a Call Step will automatically change its default created name. Same occurs if you change the name of the Transaction or Sequence targeted by the Call Step.
- Screen class handlers creation and update becomes easier as the screen class can be selected in a tree view representing the screen classes hierarchy of the project, and the popup window/editor can be enlarge to display the whole tree view of screen classes
- Improved handling of long XML log traces
- Updated 'Browser property change' statement documentation
- Improved crypto services
- Added a "change to" functionality between IfXpathExists and IfXpathExistsThenElse statement
- Added documentation for "changeto" functionality between two object
- In Studio, connector editor/sequence editor is cleared when starting a new transaction/sequence execution: previous result is not displayed anymore.
- Added a secured connection required property to transaction and sequence objects

#### BUGS (23):

- When using Convertigo cache, char encoding was not retained correctly for transaction or sequence output xml
- Fix : XML copy works on attributes
- Fixed: XPath is now correctly added to the IfXpathExists and IfXpathExistsThenElse statements when created from the XPath evaluator.
- Fixed handling of URLs without protocol (http: or https:) in SiteClipper (cf. RFC 3986, section 4.2)
- Fix : picked values inside a Iterator/ParallelStep/Serial are now correct
- Added a message box asking to save project before deploying it.
- Fixed:  no more error when adding a new SQL connector to a project
- Fixed: now the Convertigo developer can create an Attribute step or a jAttribute step in a If or IfThenElse step.
- Fixed: When 2 connectors of 2 different projects have the same name, executing a transaction in one of them used to display the result in the Output tabs of both connector editors => now only displays the result in the correct connector editor.
- Fixed: you have two HTML connectors opened at the same time, when a parent handler is selected in the Projects view and a correct Xpath is generated in the Xpath evaluator of the other HTML connector, the Generate New Statement button is activated in the Xpath evaluator panel but when clicking on this button to add the statement, an error message is displayed and explains clearly that the selected handler belongs to another html connector
- Fixed minor error with MTOM file name
- Added the ConvertigoError element in sequence's schema
- Fixed text display for 'CSS injector' extraction rule and 'Browser property change' statement
- Fixed: "Update Configuration" button of Configuration page in the Adminsitration Console is now enabled when removing a value and not setting a new one
- Fixed error when generating SOAP fault details
- Fixed: Linux Xvnc Depth and Geometry properties are now text properties and have correct default values "16" and "320x240" (visible in Administration Console Configuration page)
- Fix : HTTP redirection with absolute URL (begin with /) for the HttpConnector (and inherited)
- The testplatform in XML mode now request a text/plain content-type, so the XML result is now correctly displayed in the Result panel.
- Fixed issue with newly added URL criterion (using the HTML Xpath evaluator) that does not appear in the project's tree
- Fix: Not-usable value "from last detected step" in "Sheet location" property of Sequence has been removed for available choices
- Fixed: focus is set back on the new element after a "change to" functionality use on a step/statement/variabl
- Fixed issue with log levels panel in web admin log viewer
- Fixed: the pxml requester now respect the output character set of the requestable

---

## 6.1.1 

#### NEW FEATURES (1):

- Added a new Generate file hashcode step

#### IMPROVEMENTS (8):

- Now handles complex variable in Call Sequence steps
- Improve ElementStep (jElement) versus multivalued variables
- Move HttpClient admin properties in a "HTTP client configuration" categor
- Added a button in HTML connector editor to remove ALERT elements from the DOM
- Added an SSL connector to the studio's embedded Tomcat.
- Projects view 'Save all' button is now always activated so the user can save his proejct at any time, also using the 'Ctrl + S' keys pressed.
- New icons to differentiate Transaction/Sequence from Call_Transaction/Call_Sequence steps [support case #dev qa]
- Now Reference Manual displays large icons for Convertigo objects

#### BUGS (10):

- Fixed an issue about escaping in Xpath evaluator
- No more alert message when no SiteClipperConnector selected in a ContinueWithSiteClipperStatement
- Fixed an issue when trying to add an attribute step to a jElement step
- Fixed 'Execute' button to update its variable values to the new injected ones, via 'Edit' button
- Fixed test case edition with protected value. Hidden value not set to target variable.
- Passed project.html page to "IE8 standards" compatibility mod
- Now correctly handles the AS clause in SQL transaction
- Fixed studio logger exception when saving project
- Newly added columns in Table HTML extraction rule or Record now have their property 'Extract children' set to 'false' by default. User has to set it to 'true' when needed only.
- The HTML XPath evaluator preserve empty text nodes in the result tree

---

## 6.1.0 

#### NEW FEATURES (1):

- Added a new SQL project into the New Project wizard

#### IMPROVEMENTS (9):

- Now automatically import requestable's variables when creating a test case
- Added an explicit engine parameter defining if the engine should send an HTTP 500 error code status in case of SOAP fault
- Added templates of code (HTML and JS) in separate files of jQuery mobile template project.
- Added PhoneGap JS libraries reference in mobile templates
- Removed wizard page to name the Connector when creating new Sequencer project
- 'Input variables' step modification: step is now in 'Others' category and default output is set to false
- Connector changed to a SQL connector in Sencha template project
- Improved the execution buttons inside the test platform (added new requesters such as JSON and pure XML)
- Allow to copy testcases variables into parent requestable object in the test platform

#### BUGS (2):

- Fixed issue with very quick processes when using the ProcessExec step
- Fixed the migration check process

---

## 6.0.7 

#### BUGS (1):

- Fixed ConcurrentModificationException can occure at requestable startup

---

## 6.0.6 

#### NEW FEATURES (3):

- Now allows to provide with a prefix and namespace for an Attribute step element
- Added a new FileUpload statement in the HTML connector
- Now allows Siteclipper resizing to be disabled using C8O.var.auto_resize = 'false'

#### IMPROVEMENTS (9):

- Updated mobile lib to be able to call a sequence or transaction from another project
- Improved SOAP fault error handling: allowing HTTP 500 status code
- Added a new button triggering a RESET action on IBM connectors
- Removed demo projects from server packages
- Improved default selected object in the new object wizard
- Modified audit logger pattern to add more information
- Improved the format of project XML definition files in order to make them much easier to merge in source control systems
- Added an IfThenElse statement
- Improved the sequence source chooser for CallSequence steps (now filtering projects without sequences)

#### BUGS (12):

- Fixed WriteCSV step issue (all lines content was equal to the last generated line)
- Fixed regression regarding entry/exit handler return options (did not offer empty option anymore)
- Updated the SiteClipper URLs generation according to IIS needs (IIS seems to not allow ampersand characters in the URL body)
- Removed namespace in fault code element for CXF compliance
- Reduce log level in case of very big input or output doms
- Fixed NPE in HTML connector when auto refresh DOM is ON and in case of window.open
- Fixed source list (transactions or sequences) for CallTransaction or CallSequence steps
- Fixed issue in the web lib when a form tries to submit a value with spaces
- Fixed Javascript weblib's siteclipper plugin error if the Convertigo iframe is included in an another domain
- Fix transaction or sequence's variables order in web service exposition
- Fixed issue when renaming a sequence: does not automatically update CallSequence source
- Fixed context name badly generated when __sequence and __connector parameters are received (in Studio)

---

## 6.0.5 

#### NEW FEATURES (2):

- Web Service: SOAP responses are generated considering project's schema default forms
- Web Sercice : Enabled project to specify schema default forms (qualified/unqualified)

#### BUGS (1):

- Fixed too small list for screen classes synchronizer in studio

---

## 6.0.4 

#### NEW FEATURES (1):

- Enabled HTTP transaction to output HTTP data as string or as base64

#### BUGS (7):

- Fixed issue with automatic combo fill for sequence's CallTransaction and CallSequence
- You can not paste sequence variables to a transaction or Call_Transaction Step any more
- Improved transaction's schema update from generated xml: fixed 'xsd:string' instead of 'xsd:anyURI'
- Fixed empty 'xsd:simpleType' under 'xsd:element' in project's schema
- Fixed empty namespace in web service response
- Fixed a regression in CallTransaction and CallSequence steps (XSD update issue when creating step or changing call source)
- Fixed sequences and transactions 'Character set' encoding: now correctly used in SOAP response

---

## 6.0.3 

#### NEW FEATURES (6):

- Reduced schema types size in web service description
- Added property validation preventing a wrong usage of global symbols
- Add a "Download started" trigger for HTML statement
- Added a "No wait" synchronizer for HTML statement
- Added a new Log step
- Added a new Audit logger

#### IMPROVEMENTS (4):

- Handle multideclared values as multivalued in a SOAP Envelope
- Improved Exception step now allowing to set an XML details part
- Added structured entry parameters management for sequences
- Enabled remove of encompassing "response" node in case of web service respons

#### BUGS (5):

- Fixed exception when importing a project referencing non-existant global symbols
- Fixed search feature not focusing found objects
- Fixed unreachable source now acting as an empty source instead of throw Exception in server
- Fixed studio freeze when setting a bad encoding in ProcessExec step
- Fixed unneeded popup alert in studio during property validation

---

## 6.0.2 

#### NEW FEATURES (1):

- Added a jSimpleSource step to convert a Source to a String

#### BUGS (2):

- Fixed legacy Table extraction rule when manually creating columns.
- Fixed error message at Debian startup

---

## 6.0.1 

#### NEW FEATURES (3):

- Added a new ProcessExec step able to execute a process from a given command line
- Added a Duplicate step for copy/paste one file or directory inside the same parent directory
- Added a new ListDir step able to list all the files of a given directory

#### IMPROVEMENTS (3):

- Added a global management of deployment configurations
- Simplified Copy and Move file/directory steps
- Added a new project's property in order to specify the namespace URI to use for web service

#### BUGS (3):

- Fixed upgrade control (downgrade is forbidden)
- Fixed studio crash under MacOS when trying to execute a transaction or a sequence
- Fixed Save dialog that appears twice when closing the studio with some modified but not saved projects

---

## 6.0.0 

#### NEW FEATURES (14):

- Sequence variables directly available as step sources
- Improved the functionality to "stop" a transaction or a sequenc
- Added a new statement ifXPathExists for xpath detection
- Added an icon to jump to Test platform from Administration Console and login page
- Added an unified proxy configurator
- Added a new "URL" criterion for HTML screen clas
- Server Installation on Windows and Linux (with several application servers) documented in Operating Guide
- Studio Installation on Windows documented in Operating Guide
- Creation of a new documentation: Operating Guide
- Add a jQueryMobile template project
- Added a new sequence step called 'Remove context' to destroy a named call_transaction step
- Added steps for file manipulation
- Online eclipse help (drastically reduced Studio installer's size)
- Added an introduction on Administration Console in Operating Guide

#### IMPROVEMENTS (22):

- Improved SQL connector to take in account CallTransaction step's connection string override
- Added open, close and delete actions on multiple selected projects
- Mobile library file weblibMobile.js renamed to mobilelib.js
- Improved custom.js documentation (inline comments)
- New user workspace creation/update process
- The creation of "Handler" is now available from the "Functions" folde
- Removed the browser control from style sheet's editor
- Renamed the "Url" HTML extraction rule to "Page URL"
- Improved mobilizer project information after creation
- Improved focus handling of newly created objects in studio
- Improved beans layout in new object wizard
- Fixed issue when renaming an object with text from the clipBoard
- Now allows to double click on a legacy transaction handler in order to directly open it in the handler editor
- Improved error handling when deploying a project in the studio
- Improved project deployment dialog layout (auto adjust controls when increasing or decreasing dialog size)
- Now allows to remove a deployment configuration from the saved list
- Now allows to use self signed or untrusted certificate for project deployment
- Build phone gap application based on template_mobileJQueryMobile
- Added a property to SQL connector to auto close connection at the end of each transaction/sequence
- Improved studio performances (studio launch, project loading, object loading...)
- Transaction/Sequence JS are now displayed by their name.
- In steps creation wizard, jElement step moved to "XML steps" category instead of "JavaScript steps"

#### BUGS (28):

- Fixed euro character handling in legacy project
- Fixed issue in IBM TN5250 where the emulator would stop responding in a specific screen
- Fixed Siebel mobile template issue under Safari
- Fixed mobile viewport issue in test platform (wrong ratio layout)
- Removed unused HTTPStatement property 'formName'
- Fixed Xpath generation for HTML Markup that contains double dots punctuation
- Fixed mobile template in Test platform for Safari
- Fixed SQL connection closing issue
- Fixed issue with getAttachment statement without project name
- Fixed duplicate 'loading' divs in Mobile template project
- Fix threading issue for simultaneous requests on the same context
- Fixed the inconsistency of the schema file (XSD) when a project is renamed
- Fixed NPE in Log4JHelper.mdcClear()
- Fixed the drag and drop for the object's priority reordering
- Fixed tooltip text for components of the object's creation wizard
- Fixed SiteClipper project template (wrong ReplaceString object in Javascript screen class)
- Fixed the state of the 'Save all' button
- "Return" step now exits correctly from the parent "sequence"
- Fixed "Break" step to exit correctly from the parent loop
- Improved studio preferences dialog layout
- Fixed issue about project deployment and 'XSL assemble' option dialog in web admin console
- Fixed unexpected errors when requesting the engine stop
- Fixed IBM emulator broken when entering special characters like '??' or '???'
- Fixed wrong infinite loops detection for transaction/sequence
- Fixed Studio freeze when closing due to dead lock
- Fixed: Scheduler GroupJobs not working
- Fixed performance issues when manipulating the project tree view (e.g. copy/cut/paste, object adding/removing/renaming) with large projects
- Fixed refresh button behavior in studio (now completely reload objects from XML files definition in _data directory)

---

## 5.5.3 

#### NEW FEATURES (8):

- Add a "Details" feature for the 'new mobile project' wizar
- Added a US Directory mobile Sencha-based sample project
- Added a mobile Sencha-based sample project able to download a PDF file
- Updated mobilizer project template in order to show correct icons on desktop/favorite add (depending on the type of device) and to ask access not to all functionnality of the device (but only necessary ones) when installing the native app
- Improving Convertigo mobilelib to extend Sencha Touch Ext.is.Tablet function for it to take into account Android tablet and Playbook
- Siebel Mobile application sample using Sencha Mobilizer and Convertigo Siebel connector
- Added a New York Times RSS mobile Sencha-based sample project
- Improved weblib: C8O.call() now handles FORM as parameter

#### IMPROVEMENTS (8):

- Update mosaic.js to support the new XML hub format
- Improved the "store.load" and "app.server.execute" methods by adding a "project" parameter that overrides ponctually the project for that Convertigo cal
- Added a new XML output format for SQL transaction (ELEMENT_WITH_ATTRIBUTES)
- Improved Sencha Touch template project and mobilelib.js framework
- Shared mobilelib.js to use the latest version easily
- Added a new mobile project template without any selected feature
- Fixed special characters rendering issue with HTTP connector
- Use UTF-8 as default output charset for new requestables

#### BUGS (19):

- Fixed issue about PDF download (NullPointerException in GetAttachment statement)
- ProxyPath webapps for crossdomain issue is compatible servlet-api 2.4 (shipped with GateIn)
- Fixed GetAttachmentStatement that did not wait the current download to be complete
- Fixed mobile lib regression (exception when calling a store.load() with a project name as parameter)
- Fixed IBM TN 5250 trace recorder creating an empty file
- Fixed wrong downloaded filename for BlackBerry mobile builds output (missing .jad extension)
- Fixed issue of double splash screen with installed native mobile application
- Fixed a typo issue in comment property of mobile template project.
- Fixed last location entry ('Batignolles') for template mobile project.
- Fixed bug about project name set in creation wizard not set in .project file.
- Fixed bug about mobile application animation property (flip).
- Fixed issue with infinite loop protection for html transaction handlers
- Removed illegal characters in PhoneGap's config.xml 'id' attribute
- Fixed user and emulators loggers missing in configuration editors (web and studio)
- Fixed JSONP wrong mime-type in transaction response
- Fixed issue with attachment relative-url attribute that was computed from project workspace instead of webapp directory
- Fixed issue with mobile lib and CSS automatic selection in the test platform
- Fixed GoogleMap mobile feature: do not ask anymore geolocation activation at application startup
- Fixed: Chrome can save administration password

---

## 5.5.2 

#### IMPROVEMENTS (1):

- Added AS400/DB2 pool support

#### BUGS (1):

- Fixed issue with legacy project creation in studio

---

## 5.5.1 

#### NEW FEATURES (3):

- Easiest way to setup mysql jdbc driver in Convertigo Studio
- Auto-sized siteclipper widget
- Interactions supported between siteclipper widgets

#### IMPROVEMENTS (1):

- Auto retrieve and send __portal_username for GateIn gadget

#### BUGS (6):

- fix : SiteClipperConnector domains listing support live change in Studio
- Fixed named contexts issue in Webclipper
- Fixed issue about project test platform with password field on IE
- Added support for iPod Touch browser userAgent
- Fixed issue with WebClipper and relative URLs containing only the parameters part
- Fix : check dirty doesn't work for named context of WebClipping project

---

## 5.5.0 

#### NEW FEATURES (9):

- Add a QRCode generator button on the test plateform
- Added visibility policy for transaction/sequence variables (to hide sensitive data for instance)
- Add mobile skins to the test plateform
- Added technical reference documentation about Javelin object
- Added technical reference documentation about HTTP protocol interface to Convertigo
- Added technical reference documentation about Web Service interface to Convertigo
- The 'Test Platform' pages are now account protected
- Fixed the issue when not selecting the 'Login form' feature in the Sencha template project
- Created a new mobile framework for developing mobile application based on Sencha and PhoneGap technologies

#### IMPROVEMENTS (3):

- SiteClipper now supports variables and test cases
- SiteClipper URL criteria can include the query string
- Added a mechanism to prevent infinite loops in handlers

#### BUGS (20):

- Fixed missing icon in new object wizard while resizing
- Fixed engine startup failulre when Hibernate session factory creation has failed
- Fixed badly ordered children in the project tree when increasing/decreasing priority
- Prevented pool from being created from other connectors than legacy ones
- Fixed issue of project's widget visibility after project deletion in Administration interface
- Automatic resizing of tooltips in the administration interface
- Fixed issue with table extraction rule when using the wizard from HTML connector's editor
- Fixed issue when trying to change the 'Product version check' configuration property in the web admin console
- Now warns end-user when trying to rename a project object with a name containing forbidden characters
- Fixed issue with '}' sign in default value of symbol
- Fixed focus issue in New Object wizard
- Added object type in web admin console when editing project's objects
- Fixed HTTP/HTTPS issue about AdoptClientCookies statement
- Fixed issue with JSON requester that failed to be parsed on client side if statistics are set to 'true'
- The database used in the context of Convertigo Mashup Sequencer documentation  is now working correctly with the two default tables: articles and web_sites.
- Project compatibility mode setting is now enabled in web admin
- Fixed issue about Client_instruction_set_value extraction rule with multi-valued variable
- Now handles multivalued variables in test platform
- Fixed issue when trying to add a new statement on JS function statement
- logviewer can now filter using 'extra' variable

---

## 5.4.5 

#### NEW FEATURES (1):

- Added a new statement called "Record for Site Clipper Statement" to prevent refresh behavior of the first Site Clipper reques

#### IMPROVEMENTS (1):

- Handle CPS usb card for Windows

---

## 5.4.4 

#### BUGS (3):

- Fixed SiteClipper's ClientInstruction rules cause NullPointerException after a SiteClipper transaction [support case #00001486]
- Fixed issue about download of unspecified file size
- Fixed issue producing wrong predicates in source picker used within sequences

---

## 5.4.3 

#### IMPROVEMENTS (1):

- The 'news' window does not cover the trial window anymore

#### BUGS (3):

- Fixed PDF download issue if Adobe Acrobat Reader is installed
- Fixed "HTTP 500 internal server error" when requesting an URL containing colon characters (':') in its query string in SiteClippe
- Fixed Javascript error in IE when trying to execute interactions between widgets involving SiteClipper

---

## 5.4.2 

#### BUGS (2):

- Fixed issue about Dharma JDBC-ODBC bridge
- SiteClipper check query encoding to prevent exception

---

## 5.4.1 

#### NEW FEATURES (2):

- Use siteclipper rules to automatically fill fields on the client side with value from context
- Prefix for Convertigo requester can be set in custom.js

#### IMPROVEMENTS (2):

- Handle secured connections (HTTPS) in SiteClipper connector
- Improved HTTP 500 responses of SiteClipper with a more explicit message and reset the context lifetime

#### BUGS (4):

- Fixed credential statement when User property is not set.
- Fixed static resources delivery in case C8O web-app path is not the first path element
- Fixed a bug in 3270 panel when the panel is empty
- Fixed issue with '=' sign in default value of symbol

---

## 5.4.0 

#### NEW FEATURES (16):

- Added a connector object in SiteClipper
- Added a ScreenClass object in SiteClipper
- Added a string replacement rule in SiteClipper
- Added a Javascript code injector rule in SiteClipper
- Added a CSS injector in SiteClipper
- Added a regular expression match criterion in SiteClipper
- Added an URL match criterion in SiteClipper
- Added a MIME type match criterion in SiteClipper
- Added a new Transaction object in SiteClipper
- Added a new String Replacement rule in SiteClipper
- Added a new criterion for URL matching in SiteClipper
- Added a new MIME type detection criterion in SiteClipper
- Added a transaction object in SiteClipper
- Added a help button for each object property in web admin project editor
- Added a Rewrite Location Header rule in SiteClipper
- Added an automatic Rewrite Absolute URLs rule in SiteClipper

#### IMPROVEMENTS (3):

- Added a button to delete all contexts at once.
- Default response charset property in Site Clipper Connector
- Criteria and Extraction rules for Site Clipper Connector are separated into categories

#### BUGS (7):

- Fixed issue with trusted certificates on Websphere
- Billing manager now retrieves the user name for ticket insertion
- Fixed display of screen classes for SiteClipper projects in web admin project editor
- Fixed issue when renaming SiteClipper project
- Last detected object is now displayed with another decoration style than modified objects' one
- Fixed hidden properties in web admin project editor
- Fixed issue in web admin project editor and checkbox controls that always remained checked regardless the property value (true or false)

---

## 5.3.3 

#### NEW FEATURES (2):

- Added the possibility to not forward client headers for the site clipper
- Added a new HTML statement for client side cookies adoption

#### IMPROVEMENTS (5):

- Now handles multi-domain connectors for SiteClipper
- Improved HTTP headers forwarding policy (according to the existence of the header) in HTML connector
- Implement HTTP 302 redirect location with absolute path instead of absolute URL
- Improved Panel extraction rule for handling special panels with text in their frame
- Billing manager uses "username" context variabl

#### BUGS (2):

- fix : handle deflate content encoding for data received by the site clipper
- Fix NPE in SiteClipper about wrong usage of the ContentType header

---

## 5.3.1 

#### IMPROVEMENTS (1):

- Enable browser cache for project resources (send 304 not changed)

#### BUGS (5):

- Fixed "Assemble XSL" feature to comply with the declared XML encodin
- Protected access to C8O javascript object in interaction hub
- Fixed images not correctly aligned in CMC doc in Studio Help
- Fixed demo package when using IE
- Fixed a regression on lib_GoogleMaps project

---

## 5.3.0 

#### NEW FEATURES (7):

- Create an open social generator servlet
- Added a button to the project test platform page allowing to generate the gadget URL or XML definition needed by a portal catalog
- First simple version of the new mashup composer
- Added the zoom feature for gadgets in GateIn without reloading content
- Bean's priority can now simply be changed using drag and drop amongst other beans
- weblib C8O.doMashupEvent handle HTML element as payload
- Added certificates manager widget in Administration console.

#### IMPROVEMENTS (8):

- GateIn hub engine now reads interactions definition from XML and is integrated to weblib.js
- Rename lib_GoogleMaps connectors and update references
- Fixed documentation for GenerateDates step
- Updated documentation for Parallel step
- Updated Call sequence and Call Transaction steps documentation
- Improved web service import overall performances
- Improved Iterator step performances
- Added a button to reach the test platform from the projects list in the web admin

#### BUGS (16):

- Fixed wrong layout when using the web admin logviewer in Firefox (too many scroll bars)
- Fix : failed to index and rename some log files
- Fix : can find start date in specific case of log file
- Fixed "update configuration" button in Ms Internet Explore
- Fixed studio freeze when step's source contains huge data
- Fixed Javascript runtime error on ReadXML when input file property is a variable
- Fixed a NullPointerException when stopping the engine [support case #00001456]
- Fixed the current worker threads counter in web admin
- enengine.GetStatus admin service is now only called once when clicking Home button in Web Admin.
- Fixed the project URL in the project's name creation page of the New project wizard
- Fixed empty connections list display in web admin when using IE
- Fixed a cache problem when changing Step's priority.
- Doesn't display empty sequences part on test platform when the page is waiting for content
- Sample projects are not compatible with Google pages containing Doodles instead of standard Google logo => changed the screen classes definitions and their criteria Xpath for both standard and containing Doodles pages to be correctly detected (impacted sample_documentation_CWI and sample_documentation_CWC projects)
- Now handles terminal type property in IBM emulators
- Fixed projects widget titles in Ms Internet Explorer

---

## 5.2.3 

#### BUGS (5):

- Fixed issue in web admin when editing project transactions
- Fixed updating of object properties view in web admin
- Fixed GET parameters decoding when a value contains '=' characters
- Fixed charset handling in site clipper context
- Fixed wrong HTTP redirect cases in site clipper context

---

## 5.2.2 

#### BUGS (3):

- Fixed issue when IE received an admin services errors
- Fixed "HTTP 500 internal server error" when trying to validate the configuration in the web admi
- Icons for project treeview in C-EMS server admin are restored

---

## 5.2.1 

#### IMPROVEMENTS (1):

- In legacy connectors context, SSL setup is now directly available from the connector properties (SSL enablement and server certificates auto trust)

#### BUGS (2):

- Fixed SSL connection issue for legacy 3270 connectors
- Fixed the SQLException when using the SQL connector with SQL server

---

## 5.2.0 

#### NEW FEATURES (21):

- Added DB2 support for SQL connector
- Allowed steps properties to be scriptable
- New web administration console
- Contextual help for object creation (in 'new object' wizard)
- Adding a 'Context Add Text Node' statement now updates the transaction schema
- Allowed to choose the name of the project when importing a project from an archive file
- Linked the new object wizard to the eclipse help to easily reach the reference manual
- Added a new library project providing a Web service API accessing Google Spreadsheets API features
- Added a new popup menu on steps to output it as true or false, recursively or not
- Created real-time monitoring widgets for the new admin console
- Added a strong and easy text ciphering Javascript library
- Added PAC proxy configuration for HTML connectors
- Create a reverse proxy servlet
- Created a Flex legacy on-the-fly project template
- Improved the weblib: use C8O.call() instead of recreating iframe
- Now provides a 64bits WAR for linux platforms
- Handle NTLM authentication for proxy (HTTP connectors) [support case #00001443]
- Added a new library project providing a visual widget to be inserted in Mashup Composer and a Web service API accessing Google Maps API features
- Now reminds of previous extracted WSDL types when updating extraction rules
- Now auto generates blank template file for xsl stylesheets
- Now forces tag names created by extraction rules to be normalized

#### IMPROVEMENTS (14):

- In legacy on-the-fly Flex projects, combo boxes are now directly editable
- The scheduler now ouputs its logs in a specific log4j logger
- Allows drag and drop of Xpath on a Table Extraction Rule row
- Now allows to create a new project directly from a web service
- Implemented Ctrl+C and Ctrl+X keyboard shortcuts for variables' default value settings
- Saving jSimple Statement now changes object save state
- Copying a project now also copies all its resources
- Legacy Flex on-the-fly projects now automatically sort drop down lists
- The new project index page now generates links for all requestables and test cases
- Improved the uninstalling wizard for deleting user workspace
- Now ignores '.svn' directories when loading convertigo projects
- Added a new 'delta time' column in the log viewer, that shows the time difference between two consecutive lines
- Added a go-to-end button to the log viewer
- Admin now logs at INFO level

#### BUGS (97):

- Fixed empty ETR files created by the trace recorder
- Fixed NullPointerException when trying to capture legacy screens
- Project deployment now uses the same credential as the web administration (could be edited)
- Fixed WSDL types according to the legacy transaction's 'Remove blocks' parameter
- Legacy in convertigo server with websphere 6 problem with TerminalSNA.txt
- Bull emulator javelin.getString() method cannot return emulator status line
- Can't open transaction file on Legacy integrator project
- Exception in demo Flex in Mosaic in IE
- Display bug in project page in firefox
- Couldn't duplicate project
- Xul fastparse failing cause an error message with big stacktrace
- Google map not working in demo_mashup
- Fixed translation bugs
- Security issues about user rights (admin privileges no more required)
- Demo package doesn't work in proxy mode
- Default_Screen_class must not have criteria
- Trial version : first launch of the studio with an empty projects view
- Unable to deploy a project from studio to server
- link on documentation not refer to the right url
- DefaultBlockFactory appears as ? in Reference Manual
- Test cases don't send certificate
- Authentication failed loop in HTML connector
- can't create any new sheet
- import ws reference send error null pointer exception
- convertigo linux doesn't start some times
- Duplicate project doesn't create ws folder in soap-template folder
- Can't archive logs twice a day
- Erreur generating WSDL when wsdl Style is empty
- Can not deactivate "Enable the projects data compatibility mode"
- certificates issuers not taken into account in engine.properties
- .json error on iGoolate sandbox project
- Error on WSDL file for SoapAction attribute
- Bug in XMLVariable extraction rule
- weblib client xslt doesn't work on android browser
- 'Enable XulRunner proxy' is set to true once i set log4j.logger.cemsBeans to DEBUG
- MSVC redistribuable files are now added to server and studio win32 installers
- Couldn't be able to create new screen class from button in legacy
- No default IMG if resource picture not present
- Couldn't create new variable once i generate request form Transaction
- Upgrade convertigo server doen't work
- Broken demo IP Search
- automatic jump to next field not working
- attribute style not set by FLEX renderer
- blank choice not applied for loggers preferences
- Can not type letter in the Field/Drop-down list object
- 'null' sent for field __field_c21_l8 if left blank
- Can not drop source on a Call_transaction variable
- Typing letters in drop down list sends nothing to convertigo
- Background color and title color of popup are psychedelic!
- Short descriptions of properties should be displayed in a tool tip
- The configuration accordion should collapsed by default
- Remove the date and time from the header in new admin
- Migration is very long
- Can't drop source on Transform step from an iterator step
- Can't see on source picker tab the path of Call transaction variable
- Images not rendered at correct z-index
- New button layout does not support height of 1
- Can not validate field anymore by pressing ENTER key
- Admin console sould not be larger than 1280 px Wide
- Studio installation: wrong label about user workspace
- Deploying project on admin problems
- Missing maxOccurs="unbounded" for schema of XMLRecord with a single ite
- Wrong image in demo Flex in Mosaic
- Error on Closing project without saving
- Unable to copy/paste object name
- Connector and Sequence containers do not have collapse icon in Test Platform page
- Error on starting Convertigo
- Unhandled event loop exception
- Error while trying to get WSDL project
- Incoherent response between http transaction and web browser
- Extra border for SalesForce GoLeads widget on IE
- Get Error once the session is closed
- Error opening ressources without stack
- Studio opening make the Window unresponsive
- demo_usDirectory SearchBusiness transaction fails
- English error in property of browser property change statement
- blue line for odd rows on projects list table
- HTML convertigo admin java error
- Configuration modifications are no more saved for Billing System.
- XSD file missing on openning lib_GoogleMap
- Extra Information on log include infos not related to admin
- Error on migration of  lib_GoogleDocs
- Error on filter on logs
- Error on starting Convertigo
- Error on creating a Web service project having a name similar to one just deleted before
- Translation error in traceplayer port error message
- The sort options of the jquery tables don't work
- update configuration button isnot enabled on changing advanved properties
- Missing GetArticleData transaction schema in sample_documentation_CLI project
- Goto end button is grayed after exiting RealTime mode
- Upgrade the level of the 'Step <StepName> (<Priority>) done' log
- The XVNC property is not configured as a boolean property in new admin
- Deployment of project is successfully done but the project is not include in the project list
- Error Stack after deleting a project and restarting engine
- Studio fails to start with billing properties
- Unable to save engine configuration under IE8
- Cron wizard displayed on jobs information

---

## 5.1.8 

#### IMPROVEMENTS (1):

- Add SSL and Terminal type support for IBM emulators

---

## 5.1.6 

#### NEW FEATURES (2):

- Allowed the re-use of existing contexts in case of reverse proxy request
- Added the new weblib.js

#### IMPROVEMENTS (1):

- Added a comment column in reverse proxy module definition table for string replacements

#### BUGS (5):

- Fixed SSL KeyManagerFactory when using JVM IBM
- Fixed EngineException in web admin when trying to deploy a project from IE6/IE7 [support case #00001458]
- Fixed NullPointerException when trying to import WS reference with no namespace
- Fixed wrong authentication in mashup composer sometimes
- Fixed missing JSR168 servlet definition in mashup composer

---

## 5.1.4 

#### NEW FEATURES (1):

- Added a new JavaScript library for legacy FLEX projects

#### IMPROVEMENTS (2):

- Enhancement of the Image rule: a new attribute field was needed in Image type blocks
- Added a new editor for legacy extraction rules "mashup event" propert

#### BUGS (2):

- Fixed missing context name when calling convertigo through a widget interaction
- Fixed no error message displayed in demo_usDirectory project when searching a lead from demo_SalesForce, changed this lead in SalesForce leads list

---

## 5.1.2 

#### NEW FEATURES (1):

- Allowed to forward some chosen requested HTTP headers to target for HttpConnector and HtmlConnector

#### BUGS (1):

- Fixed deployment of project on trial cloud space with trial registration

---

## 5.1.0 

#### NEW FEATURES (9):

- Auto launch Xvnc server using DISPLAY environment variable on Cems Server Linux
- New log framework
- Enable parametrization of any database object text property
- HTML Extraction should handle other type than node and string
- New sequencer mapping module (Using Drag & Drop)
- Add a new stat counter for C-EMS worker thread creation
- Move projects and user data from c-ems plugin/war to user workspace
- Add <wsdl:documenation> based on Transaction's comment
- Add targetId  and targetAppend features to standard weblib

#### IMPROVEMENTS (21):

- Disabled support for client JSP in projects
- Weblib doesn't handle when xsl doesn't create a BODY
- Update demo projects, template projects to load CSS files before JS files
- Display bean long description in New Object Wizard
- PUT and DELETE requests for GoogleDocs project
- Request template for GoogleDocs project
- Improve schema extraction from transaction generated XML
- GenerateDatesStep should treat input days as non ordered
- Create a logger for statistics
- Steps sources are not correctly displayed in Projects tree
- Improve HTTP transaction schema
- Improve Choice legacy extraction rule by choosing radio mode
- WriteXML Step : ID and output
- Add new property for input format for generateDate step
- Improvement of the USDirectory project
- Choose a workspace directory on installation procedure
- Update FOP version in Convertigo to 1.0
- Mismatch between rows and columns on XMLRecord extraction rule
- Auto generated XMLTable extraction rule seems not correct when first column is defined as header
- Added a jsp compatibility mode for old projects
- Projects' list should be ordered with ignoring case

#### BUGS (117):

- Convertigo server is not installed correctly
- case of infinite loop and deadlock on context removing
- Invalid Sequence's schema
- issuer certificate on a CWI project deployed on the server
- hsqldb: session is closed many times once the sequence is executed
- Crash after configuration modification and html transaction execution
- Comments not added in CDATA sections in project schema file
- Beans.xml file not correctly generated
- can't create new project
- HttpConnector does not handle correctly 'null' variable default values when using an XML request template
- Can't see all blocks in source
- Windows server installation uses client JVM
- Error on installation of Convertigo studio
- Generate XML in Legacy publisher is no more working
- Generate XML in web integrator is no more working
- Can not import 5.0.4 project to 5.0.9
- Error on loading javelin project
- Mashup Composer (DF)  page Designer is broken
- Can't edit project on windows server
- Url to launch projects after Studio deployment ends to a blank page
- Deploy a project from admin
- demo_SalesForce and demo_useDirectory should not auto_refresh
- df/dfe/interface/provider can return malformed XML
- Clean project's schema remove WS reference import declaration used by XmlHttpTransaction
- Old wsdlTypes containing 'connector_requestableResponse' should be migrated to 'connector__requestableResponse'
- WriteCSVStep don't detect titles
- Properties view does not handle "selectionChanged" event correctl
- Wizard of the Table extraction rule : starting row
- Import WSDL fails if studio is installed in "Program Files" director
- If Java 1.5 is used, Input SOAP requests are truncated if bigger than 8 ko
- Change Studio/Engine Preferences to work with Loggers
- Demos index.jsp are designed with twinsoft logos
- SQL transaction variables are not automatically created after writting the SQL request
- websphere cannot install convertigo war
- Demo projects not available in linux server
- Demo project Legacy doesn't work in server installation
- Sequencer : Copy/Paste and Drag and drop allow user to insert an <element> inside another <element>
- WebClips with 'mailto' links should be client side
- Fix the convertigo statistics for HTML connector
- expiration date cache file not reset when changes are made
- Log Viewer : jumping to next page can display no data
- 'property.projectName.display_name' instead of 'Project' in Call_TR/SQ Step
- Add a cheat sheet dispaly in the list twice the same thing
- Exception in web clipping project
- Disabled screen class entry handler still being taken into account
- Create a config property handling SSL output debug mode
- Criteria Reverse result property should change displayed name accordingly
- Step DnDed from Then to Else can be lost
- Error when lauching trace
- Error while analyzing the project "cems"
- From source picker, the source of transaction isn't displayed correctly from schema generated from xml generated from transaction
- Impossible to open 2 tabs from 2 transactions with the same name
- Incorrect RPC request message part in WSDL for multivalued variables
- convertigo session confusion
- Right click / execute on a sequence should popup a message if the sequence is not opened
- Invalid negative statitics
- Unable to import ws reference on a new project
- Beans.xml generation not correct when '|' character in long description
- In beans.xml file, "expert" category should be renamed "selection" for legacy extraction rules propertie
- can't no more drag from xpath
- Direct change of 'is Active' property does not refresh element color
- Trial registration certificate is no more available once we install convertigo studio
- Demo package : widgets are not centered
- Contextual menu 'Change to' should be relevant for steps
- Get error on connection admin once i undeploy the project
- Get error on benching html project
- Error generated on benching HTML project
- Improve displayed name of XMLtagName extraction rule
- Mixed logs between projects
- Error 500 when launching webization project
- Error when clicking on XMLize button
- Error on Importing WS reference
- [Extraction rule:] Replace Text
- Windchill web site change language itself in 5.0.9 but not in 5.0.4
- Cannot set or change properties default value when using external symbol
- Default SQL transaction of newly created project (based on template) does't work anymore
- Connector criteria is only diplayed by "?" on new criteria wizar
- ROOT context is missing in the Studio's embedded Tomcat
- import ws reference WS send an error
- SQLTransaction now creates two rows for XML response
- Add a title in next page on wizard creation project
- long description in New Object Wizard contains references
- Demo projects not auto opened with studio
- Bad display in demo legacy widget
- Bad page increment in trace viewer
- DF doesn't work with linux install
- Error import wsdl
- WS import: the deletion of a created connector does not delete its corresponding directory under 'soap-templates' directory
- Date steps: Wrong dates generated if ouputLocale=US
- Deleting a legacy connector does not delete the corresponding traces folder
- Description property is hidden but should be displayed in Reference Manual
- demo_Viadeo can't be updated on the trial
- Error 404 on Widgets - DreamFace on IE8
- SQLTransaction: incorrect schema type generated from definition
- Guest button on dfLogin page of Mashup Composer didn't clear the password
- Servlet error in Web administration : Cache category
- HTTP 500 error in Web administration : Download logs
- Error on Synchro Tabs in admin server
- Relook server start page
- convertigo.webapp_path saved in absoluteURL via Web Admin
- convertigo.webapp_path and xulrunner.url saved in absolute URL via Studio Eclipse
- Emulators keys expiration date not checked
- Project's WSDL retrieval may not contain inline schema
- Copy->paste a project didn't create index.html
- Variable's "newPriority" attribute not retrieved on deserializatio
- HTML and PDF Reference Manual internal property name
- Include a changelog file
- Bad display in html  admin projects
- menuDIV not correctly displayed in Demo Package
- No engine/admin logs after purge in C8O Linux Server
- Selective delete of logs do not work
- demo_SalesForce project doesn't work correctly in IE and Chrome
- legacy capture trace error
- Fixed Studio crash when manipulating legacy emulators
- Trigger Editor for statement CLICK : label too evasive
- In step GeneratesDates, if dates into Input search days are in a bad order the extraction isn't correct
- Double connector when creating new template project

=---

## 5.0.10 
=
#### IMPROVEMENTS (2):

- Can't paste a text in a variable's default value in case of 'null' value
- The return value of a Return statement is now able to be bubbled to the parent handler

#### BUGS (3):

- When creating a Function statement on a transaction, the wizard doesn't allow to enter the statement name
- StackOverflowException exception when setting a value in a test case variable
- Incorrect web service URL in WSDL

---

## 5.0.8 

#### BUGS (1):

- Fixed basic authentication caching issue

---

## 5.0.6 

#### BUGS (1):

- Get attachment fails because Content-Type detection is case sensitive

---

## 5.0.4 

#### NEW FEATURES (16):

- Statement wait for xpath
- Function statement not fully implemented
- Add download and save feature/statement to Convertigo
- New and fast parse method in HTML Connector and DOM Tree
- Added InheritContext property in callSequence step
- No userAgent for HTML connector
- Choose not to send empty transaction's variables
- Drag and drop without Control
- Disabled statements and steps appear with red icon but text stays green
- Add XsdType property for XMLElementStep
- Add a "WSDL style" property on Projec
- Add a "WSDL with inline schemas" property on Projec
- Creation of a JSON interface to call Convertigo web services
- Retrieve xulrunner warning and exception console to our console
- Add a new criteria Xpath does'nt exist
- Add a new criteria String doesn't exist

#### IMPROVEMENTS (6):

- Performance problems on sequencer
- Add a link to the online registration page in Cems Studio registration window
- No need to have in the contextual menu to add a new step in the "IfExistsThenElse" ste
- Add testcases launch in index.jsp of template projects
- Bean creation wizard : double clic on item should do "Next"
- Add Backward and Forward buttons to Eclipse Help and HTML Help

#### BUGS (46):

- Custom error http 500 message
- Legacy publishing templates do not webize XML
- Transaction and Sequence variables problems
- httpconnector can't connect on proxy
- Empty content in Element step when source comes from an SQL transaction
- Default names for all projects objects should always be normalized
- javelin : Col. position in emulator footer is wrong and change with emulator size
- XSL files editor generates Exception when opening from Convertigo Projects view
- PKCS#11 authentication error log in Engine
- XML table description rows not deletable
- SmtpSend Step behaviour
- XSD update error when copy/paste transaction
- studio freese in double clicking on a trace
- problem with the ERP button in the demoPackage
- Exception when adding a step in a sequence (XSD update problem)
- dreamface.properties does'nt contains googleapi templates
- Execution problem when sub-element string is chosen in the source picker
- Sales-force Lead widget search function leads to a error
- Project's clean action does not correctly clean schema file
- the studio freeze where you launch a trace when another is opened
- "New => Variable" option is missing on Variables folder right-click contextual men
- Double quotes escape not necessary in Xpath property
- couldn't rename criterion
- Error adding project in Cvs
- Index out of bound whil navigating to the last page with Convertigo Admin
- (CookieService) setCookieValue failed
- Button "add element from current selection to the highlight property" disable all the tim
- Log viewer : '\' char processed as an escape sequence
- SOAP Ws not compatible with Flex4
- synchronization by screen class of the click statement  not updated when screen_class name changes
- Can not convert empty IFs Steps
- Can not import project if -Dconvertigo_global_symbols path is incorrect
- Legacy connector : Table extraction rule, Columns property not auto refreshed.
- Salesforce DEMO not compatible with google Chrome
- df base doesn't display df menus
- Multiples cookies don't work with some proxies
- Missing maxoccurs="unbounded" for XMLTable rule schem
- Bad statistics returned in xml client
- Convertigo webservice schema does not work with PHP client.
- Log contextual data are invalid
- Error logs when changing Convertigo Studio preferences
- Big etr file
- Bean creation wizard : regression in dialog (ObjectsExplorerComposite)
- Remove blocks in Legacy integrator project works only for the first blocks
- Exception in SmtpSend step
- Regression: SQL variables no more visible after query change

---

## 5.0.2 

#### NEW FEATURES (3):

- Add set of test values for each transaction or sequence
- Cannot create CallTransaction or CallSequence under Complex step
- Counters for convertigo supervision

#### IMPROVEMENTS (3):

- Zombies count in pools incorrect
- Log viewer does not show javascript syntax errors in filter field
- Add client IP and host name to logs

#### BUGS (36):

- Impossible to connect to SVN in Convertigo Studio
- Servlet internal error in admin log viewer
- Servlet error while viewing logs
- Resize code in scriptlib not working for integration transactions
- engine.log not re-created when deleted via admin console
- Statements logs repeated twice
- Demo salesforce do not display leads detail and search box doesent' work
- Engine.log can not be purge in Studio/Server for Windows
- Link error on "help content" studi
- LogViewer: invalid lines are added in case of filtering
- Log viewer: Parsing problem for column "Message"
- NullPointerException when pasting a step
- HTML Wizard project summary is false
- Convertigo logs are written to CemsAppender AND stdout
- Xpath that contains simple quotes not escaped in javascriptable xpath property
- Traces <connectorName> directory not appearing in eclipse resources before refresh
- Reached max connectors
- Unable to open an imported project
- Projects which name ends by the letter "p" are not usable
- Tool for generating automatic beans documentation
- Default values for input variables in legacy transaction not taken into account.
- Wrong "href" for Help Toc topic
- Admin configuration service should handle all parameters dynamicly
- NoSuchElementException when using daily cache on a sequence
- Tomcat looses parameters without any explanation
- Trace folder is missing in project studio tree view
- CacheManager error at cache expiration
- Studio build should build feature and product
- Welcome page not displayed on studio startup
- Log viewer doesn't work with IE
- JSP Error when click on next page button
- double click not working on explicit variables
- Admin service for configuration broken
- Servlet internal error in admin log viewer
- Navigation icons change unexpectedly
- Log viewer: sort on time column does not work

---

## 5.0.0 

#### NEW FEATURES (18):

- Simple way to configure TracePlayer in Convertigo Server
- Distribute documentation projects in Convertigo studio
- Clipplet auto refresh, using isDomDirty
- Create CAR file from a project on the server
- Improve variable management and types
- DreamFace 2.0 integration in Convertigo
- Drag & drop support in Convertigo Studio
- HttpTransaction generator from WSDL
- Generate schema of a Sequence
- Implement sub elements in rules for displaying in project tree
- Simple transformation of step IfThenElse to step If
- Separate HTML extraction rules in two categories in wizard
- A simple way to configure the log properties
- New SleepStep
- Studio should be declared as an eclipse feature
- Add/Move import feature into Eclipse Import menu
- Enhanced properties for engine
- Add pool usage statistics

#### BUGS (122):

- Check dom dirty script doesn't work in Convertigo Server clipping project
- Web service explorer not included in Convertigo Studio
- ScreenClass detection is not reliable
- Bug in readCsv step
- Problem when importing a project with '.xml' file
- 3270 emulator crashes when starting
- Studio 4.6 Demopackage freeze at launch with LegacyCRM project
- Dreamface studio doesn't show Convertigo's widgets parameters
- Missing syntax colouring for JSP files
- Bad display in convertigo admin when changing project parameter
- Xulrunner leak references in Java (memory leak)
- DF base compatibility
- Pre-configured users for the Studio embeded DreamFace should not be John and Jim
- Convertigo pop-ups are too small
- New dataview created with convertigo studio not working
- HttpClient failed on Https redirect
- Screen zone related parameters display not refreshed when modified through "lock icon" wizard in legacy project
- HTML connector UIEvent should simulate focus and blur
- Cannot extract XML structure from target transaction on a Call Transaction step when target is not in the same project
- WSDL types created by alphabetical order, issue in Sequence picker
- Host with :-1 invalid port number
- href not set to # in webclipping when enable parent extraction is set to true
- bad resize df dataview with ie
- Repair webclipping tunnel for DominoWebAccess
- Repair help dialog in Convertigo Studio
- DND: Drags only paste
- DND: Drags of a transaction on a Connector should also works
- DND: Drags on a transaction does not work
- DND: Drags of screenclass child bean on another screenclass does not work if it has no child bean of the same type
- WebClipping and Radio buttons
- Exception in log engine when no transaction selected in callTRansaction step
- Unable to update XSD and WSDL
- Invalid URI error if ressource image to get contains "\"
- Wrong referrer for ressource images from an external css file in HTTP Tunnel mode
- Invoke Browser JS Statement fail in websites with frames
- Error while update XSD file
- Exception closing Convertigo Studio
- Projects running on 4.5 not supported on 4.6
- Ticket #455 (WebClipping domDirty in server mode) is not fixed in 4.6
- Product Registration pop-up refers to Twinsoft
- Convertigo 4.6 installer copied the demo projects although I chose not to
- Workspace launcher indicates "fabienb" director
- Projects tree view images (icons) not present and generate errors
- SalesForce demo projects freeze studio when opening
- Log4j errors when starting up
- ROOT error when starting embedded tomcat
- Install directory should not contain version number
- Wrong image in SalesForce demo HTML description page
- Convertigo Studio uninstall process doesn't remove shortcuts
- Dead lock when opening legacy connector based project with trace player
- Unable to update XSD and WSDL from current generated XML
- java.lang.ClassCastException when launching Convertigo web interface
- Some Migrations to 4.6 fails.
- NullPointerException thrown by ContextManager vulture
- SVN Repositories View missing in Convertigo 4.6
- SqlTransaction does not run anymore due to new java 1.5 code
- Convertigo's dreamface.properties file confusing with DF's one
- Renaming a project doesn't remove xsd and wsdl files with old name
- Error using Concat Step
- Regression with HttpConnector / Headers
- Problem with project type in index.jsp test platform
- Problem with studio.properties
- Exception with CacheManager in Engine log
- Pop-up error when clicking on a transaction's Sheets object
- Pop-up error when clicking on a legacy connector
- Error logs with new sequencer project
- LogManager does not compile at r20623
- log.message not supported in transaction SimpleStatement
- java.lang.OutOfMemoryError: PermGen space in Studio
- Migration fails for a 4.5 project imported from its .car
- Studio labels
- Drag and Drop bug with XMLTable extraction rule
- java.lang.IndexOutOfBoundsException in legacyCRM in studio 4.6 with demo package
- Old 4.5 sequencer project with hsqldb connecteur does not work anymore
- Problem with the use of these statements : ContextSetStatement & ContextGetStatement
- Project archive contains temporary xsd/wsdl files
- Remove source from a Copy Step does nothing
- DreamFace Next/Back button and  Next screen doesn't work in IE
- No document completed on some sites since Xul 1.9
- Admin convertigo46 / Error when uploading certificat
- Migration bug with sequence including transaction steps
- Editing data widget not possible in Mashup Composer
- Name created for new DF event is not appropriate
- New widget created from Convertigo Studio is not auto-start
- Enhance Mashup Dialog boxes
- Credentials Statements don't work in Convertigo 46
- Web services Java stub should not be generated by the studio anymore (use Web Services Explorer instead)
- The steps scenarios doesn't work corectly
- An error occurs when we double click on a project
- Remove unused consoles in the studio
- XulRunner 1.9.1 seems to make studio unstable
- Web admin broken
- Cannot deploy from studio
- When deployment in studio fails, there is no error message
- Trace folder empty when Importing leagcy project containing traces
- Error when clicking the Config Icon in ADMIN
- Dnd xpath on statement does not make it a string type
- StringIndexOutOfBoundsException in LogManager
- ClassCastException when delete a step (just for one special case)
- HSQLDB.JAR is missing in tomcat/lib for Windows Server Installation
- HTML connector projects fails to run in Windows Server
- Keys.txt not provided in Studio Installation
- HSQLDB path to database not set correctly for windows installation.
- TracePlayer malfunction onWindows Installation.
- MaxCVsExceededException is not clearly handled in the studio
- Wrong edition window name "Transaction WSDL types"
- Wrong tree label for jDoWhile step
- Change default node name of Complex step from "element" to "complex"
- Web service import should set the first created connector's transaction to its default one
- No message, either success or failure after entering Key in trial version of Studio
- the usDirectory project doesn't work in the demoPAckage
- Trial download text and picture issues
- problem with the yahoo connector
- logger configuration - add combo box
- Make pool contexts usable with non-atomic calls
- Typo error in Admin console [support case #00001028]
- Engine launches pools even if we are in studio
- 'null' folder created at install root
- Wrong icon for Convertigo Studio Icon
- Import project under Linux Studio
- DF interactions are not using new javascriptable binding functionnalities
- Possibly Convertigo 5.0 migration bug

==---

## 4.5 SP4 
==
#### BUGS (1):

- CallTransaction step does not work (ClassCastException)

==---

## 4.5 SP3 
==
#### BUGS (5):

- Yet Another accent problem in HTTP Connector
- Loose of connector and transaction 'default' property
- Repair Convertigo Wizard button on Vista
- Default transaction launched by a callTransaction step instead of properties defined transaction
- problem with WebClipping project with 2 connectors in DF

==---

## 4.5 SP1 
==
#### NEW FEATURES (3):

- Use the same JSessionID for a given Sequence and its sub elements
- Make  "context" property of callTransaction step scriptabl
- Bull emulator state messages, such as Disconnected by peer, or Connection refused, should be available in the transaction context.

#### BUGS (6):

- Bug in readCsv step with several empty successive columns
- No more Vdx/Bull connection
- Bug in readCsv step with $ separator
- Automatically incremented ServiceCode for AS400 connector bug when two transactions are called from parrallel step
- Exception when importing project '.car' file
- Engine.properties file from build is not complete

=============
#### Version 4.5 :

=============

#### NEW FEATURES (12):

- Pooling sql connections
- Add alert and prompt box in the DOM
- Improve pop-up management with a tab -like implementation
- Add a statistics counter to synchronizer object
- HTML connector on linux should manage Xvnc
- Sequence project template
- WriteFileStep should be hable to append to it's output file
- Speed up WebViewer instances in Convertigo server
- Auto refresh webization when emulator change
- Relative path to webapps/convertigo in configuration files
- In Scheduler Manager, click on job/trigger name change the checkbox state
- Status bar only shows main version of Convertigo Studio

#### BUGS (42):

- SQL request executed twice
- Regression WhileStep
- HTML clipping apply user request  on kompass.fr does not convey the correct selected index in the search combo box
- web service xml : Problem encoding accent on linux
- Bug IfIsIn step : returns true when xpath gives an empty nodeList
- Index.jsp doesn't launch good connector's transaction  (in multi-connectors project)
- Error.xsl always applied when exception
- Transaction switches contexts after preceding transaction was in timeout
- ParalleleStep locked until Sequence timeout
- Minitel template do not use normalized bean names
- Http template do not use normalized bean name
- Impossible to rename Block Factory bean
- DKU webisation template do not use normalized bean names
- DKU integration template do not use normalized bean names
- 3270/5250 webisation&integration templates do not use normalized bean names
- VT integration template do not use normalized bean names
- CICS integration template do not use normalized bean names
- Adding a new screen class does not update the screen classes combo list in handlers screen class property
- Relative image paths in frames are refer to frame source adress instead of root source adress on site kompass.fr
- HEAD extract twice in WebClipping template, in GenericWebPage
- Generated WSDL for inverted XML tables is incorrect
- Extract WSDL types in legacy projects generates errors
- Extract WSDL types in HTML projects does not work
- Installing convertigo sequencer server not setting "server url" parameter for call transaction ste
- HTTPConnector : deadlock with big data displayed in left window
- Project rename should modify 'project' property of TransactionStep/SequenceStep
- Legacy transactions WSDL cannot be used as a source in sequencer steps
- Error when creating a Call Transaction step in a sequence when project does not have a default connector with a default transaction
- HTTP Connector URL encode GET parameter twice
- Can't extract WSDL on a HTML transaction which is not in the default connector
- Too much HTML Connector cookies in response header make a tomcat exception
- Cut/Paste statement into new transaction into no handler
- no migration for object name with a space in projects made with old studio
- Max threads in parallel step not continuously launched
- Wrong referer for Record extraction rule in a website with frames
- Zombi HTML Connector
- JS Scope must be stacked
- Multivaluated variable values of a sequence are not correctly handled by CallXX steps
- Duplicate objects in tree after a cut-paste action, and reload project
- After a save in Js/Xml/Xsl editors, databeseObject is not updated in cache
- Renaming screen classes does not rename transaction handlers automatically in legacy projects
- Duplicated child beans under parent after rename of both parent and child + save + close + open
