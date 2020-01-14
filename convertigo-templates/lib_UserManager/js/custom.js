/*******************************************************
 *******************************************************
 * public C8O API for CEMS 7.3.0
 * for a jQuery desktop application
 *
 * Dependences in HTML file:
 * * jquery(.min).js
 * * c8o.core.js
 * * c8o.desktop.js
 * * [ctf.core.js] (include to use CTF instead of XSL)
 * * [c8o.fullsync.js + pouchdb.min.js] (include to use the fullsync local database)
 * * custom.js (this file)
 *
 * You can find documentation about Convertigo Templating Framework here:
 * http://www.convertigo.com/document/latest/reference-manual/convertigo-templating-framework/
 *
 * You can find documentation about Convertigo Internationalization Framework here:
 * http://www.convertigo.com/document/latest/reference-manual/internationalization-framework/
 *
 *******************************************************
 *******************************************************/

/*******************************************************
 * Global variables *
 *******************************************************/

$.extend(true, C8O, {
    /**
     * init_vars variables values can only be set before the "init_finish" hook,
     * by the code or by the first query,
     * their values must be strings,
     * their state cannot be modified later.
     *
     * If set by query, variable name should be preceded by __
     * for example: ?__enc=true&... or #__enc=true&...
     */
    init_vars: {
//        enc: "false", /** enables rsa encoding */
//        fs_server: null, /** force a couchdb server for fullsync. 'null' use pouchdb. Else expects 'http://user:pass@server:port' */
//        fs_force_pouch: false, /** force to use pouch, even if fs_server isn't null */
//        fs_force_pouch_replication: false, /** force to use pouch for sync and replicate commands, even if the fs_server isn't null */
//        i18n: "", /** in case of multi-language application, force usage of the language selected. Empty string while select the browser language */
//        testplatform: "auto" /** auto/true/false: automatically redirect to the testplatform if no parameter is set, force testplaform if true or just call C8O if false */
    },
    
    /**
     * ro_vars variables values can only be set directly here, not dynamically
     */
    ro_vars: {
//        i18n_files: [] /** list of language available for the application. The first is the default language. The application must have an i18n folder with 1 file per language like: i18n/en.json */
    },
    
    /**
     * vars variables values can be set at any time,
     * by the code, by the query or by passing arguments to C8O.call(),
     * their values must be strings,
     * their state can be modified later.
     *
     * Value can be modified by code,
     * for example: C8O.vars.ajax_method="GET"
     *
     * If set by query, variable name should be preceded by __
     * for example: ?__ajax_method=GET&... or #__ajax_method=GET&...
     */
    vars: {
//        ajax_method: "POST", /** POST/GET: http method to request CEMS */
//        auto_refresh: "true", /** true/false: allow auto refresh feature for clipping */
//        auto_resize: "true", /** true/false: allow C8O to perform resize after content filled */
//        endpoint_url: "", /** base of the URL CEMS calls. Should not be modified */
//        first_call: "true", /** true/false: automatically call convertigo using the page query/hash parameters, after the init_finished hook */
//        fs_default_db: null, /** default fullsync database used for call on fs://.[action] */
//        fs_default_design: null, /** default design document to use for fs://[db].view with no ddoc parameter */
//        log_level: "warn", /** none/error/warn/info/debug/trace: filter logs that appear in the browser console */
//        log_line: "false", /** true/false: add an extra line on Chrome console with a link to the log */
//        log_remote: "true", /** true/false: send client log to the C8O "Devices" logger depending on its log level */
//        requester_prefix: "", /** string prepend to the .xml or .cxml requester */
//        resize_offset: "50", /** integer: number of pixel added to the automatic resize */
//        send_portal_username: "true", /** true/false: (gatein only) automatically add a portal_username parameter with the name of the logger user */
//        target_append: "false", /** true/false: append content to target_id or to body element */
//        target_id: "", /** element id: element id for result insertion or a selected jquery object */
//        use_siteclipper_plugin: "true", /** true/false: use the iframe encapsulation for siteclipper request */
//        wait_threshold: "100", /** integer: number of milliseconds before the wait div show up. 0 or negative value show it directly */
        xsl_side: "none" /** client/server/none: force the side of the xsl transformation or pure XML */
    }
});

/**
 * ReadOnly variables are also available
 * C8.ro_vars variables values can be READ at any time by the code
 * and must not be modified.
 *
 * C8O.ro_vars.portal_username: string containing the name of the current logged user (gatein only)
 * C8O.ro_vars.widget_name: string containing the name of the current widget, if any
 */

/*******************************************************
 * Functions *
 *******************************************************/

/**
 * addHook function
 * some part of the C8O can be customized using a hook
 * just specify the hook name and its handler
 * all existing hook are explain bellow
 * name: string of the hook name
 * fn: function of the handler
 */
//C8O.addHook(name, fn);

/**
 *  addRecallParameter function
 *  force C8O.call() to send automatically parameters
 *  added by this function with its last value
 *  already added parameter are __connector and __context
 *  to save a new value for a parameter, specify it to the C8O.call() function
 *  or call C8O.addRecallParameter again
 *  parameter_name: string of the parameter name to automatically send
 *  parameter_value (optional): initial value for this parameter
 */
//C8O.addRecallParameter(parameter_name, parameter_value);

/**
 * appendValue function
 * append value in data.key:
 * * set value if no previous
 * * make or reuse an array and push the value at the end
 * data: Object (key/value) that will be modified
 * key: string, key of the data to modify
 * value: any object pushed into data.key
 */
//C8O.appendValue(data, key, value);

/**
 *  doMashupEvent function
 *  dispatch a mashup event to the current container if any
 *  via the invocation of mashup_event hook
 *  event_name: string of the parameter name to automatically send
 *  payload (optional): key/value map object ( {key: "value"} ) or an HTML Element.
 *                             In case of HTML Element, its attributes are transformed to a key/value map object.
 */
//C8O.doMashupEvent(event_name, payload);

/**
 * call function
 * make an AJAX request to CEMS in order to execute
 * a transaction or a sequence using specified parameters
 * data: string (query form) or Object (key/value) or HTML Form element
 *          used as AJAX parameters
 */
//C8O.call(data);

/**
 * canLog function
 * tell if the actual C8O.vars.log_level allow to log
 * level: string (error/warn/info/debug/trace) log level to test
 * return: true > can log
 *           false > cannot log
 */
//C8O.canLog(level);

/**
 * convertHTML function
 * copy an XML element to an HTML element or create a new fragment
 * input: XML element to copy to an HTML element into the ouput or a new fragment element
 * output (optional): HTML element where the input copy is appended
 * return: HTML element, output element or a new <fragment> element with the imported input
 */
//C8O.convertHTML(input, output);

/**
 * deleteAllCacheEntries function
 * remove all local cache entries
 * success (optional): function, callback of the success for the DB cleaning
 * error (optional): function (err), callback of the failure for the DB cleaning
 */
//C8O.deleteAllCacheEntries(success, error);

/**
 * formToData function
 * copy all form's inputs into the data object or a new one.
 * Inputs names are the keys and inputs values are the values of the data object.
 * In case of multivalued, value is turn into an array.
 * form: raw or jQuery FORM element
 * data (optional): object (key/value) where values are copied
 * return: the data object or a new one with copied form's inputs values
 */
//C8O.formToData($form, data);

/**
 * getBrowserLanguage function
 * return: a string of the current detected language, in 2 characters
 */
//C8O.getBrowserLanguage();

/**
 * doNavigationBarEvent function
 * for HTML connector only
 * send an action to the navigation bar of the connector
 * action: string of value 'backward', 'forward', 'stop' or 'refresh'
 */
//C8O.doNavigationBarEvent(action);

/**
 * doReconnect function
 * reload the current window with the initial query
 */
//C8O.doReconnect();

/**
 * doResize function
 * perform a resize of the frame element if any
 * and calculate automatically the height if not provided
 * height (optional): number of the iframe height in pixel
 *                          automatically calculed if empty
 * options (optional): options parameter for the jquery animate() function
 *                          see http://api.jquery.com/animate/ for more details
 */
//C8O.doResize(height, options);

/**
 *  fs_getDB function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  retrieve the PouchDB object for the 'db' database and allow to use its API directly (see http://pouchdb.com/api.html)
 *  db (optional): name of the fullsync connector to use, default is the C8O.vars.fs_default_db
 *  return: the PouchDB instance for this 'db'
 */
//C8O.fs_getDB(db);

/**
 *  fs_resetDB function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  destroy the local 'db' database, that will be recreated at the next use 
 *  db (optional): name of the fullsync connector to use, default is the C8O.vars.fs_default_db
 */
//C8O.fs_resetDB(db);

/**
 *  fs_onChange function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  
 *  options (optional): all options default to false unless otherwise specified
 *  	options.db (optional): name of the fullsync connector to use, default is the C8O.vars.fs_default_db
 *      options.onChange: handler function called at each local database change
 *  return: a change object with this method
 *      .cancel(): stop the current change listener
 */
//C8O.fs_onChange(options);

/**
 *  fs_replicate_pull function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  start a database synchronization from the Convertigo database to the device
 *  see C8O.fs_sync for the 'options' and the 'return' value
 */
//C8O.fs_replicate_pull(options);

/**
 *  fs_replicate_push function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  start a database synchronization from the device to the Convertigo database
 *  see C8O.fs_sync for the 'options' and the 'return' value
 */
//C8O.fs_replicate_push(options);

/**
 *  fs_sync function
 *  ** Needs pouchdb.min.js + c8o.fullsync.js **
 *  start a bidirectional database synchronization between the device and the Convertigo database
 *  options (optional): all options default to false unless otherwise specified
 *  	options.db (optional): name of the fullsync connector to use, default is the C8O.vars.fs_default_db
 *      options.live: if true, starts subscribing to future changes in the source database and continue replicating them
 *      options.retry: if true will attempt to retry replications in the case of failure (due to being offline)
 *  return: a replication object with those methods
 *      .on(eventName, eventHandler): attach an event handler about the replication state
 *          eventName: can be "change", "complete" or "error"
 *          eventHandler: a function that receive a parameter
 *      .cancel(): stop the current replication
 *      
 */
//C8O.fs_sync(options);

/**
 * getLastCallParameter function
 * used for retrieve a parameter from the previous call
 *  or all parameter in a object key/value
 *  key: string of the parameter name
 *  return: string of the parameter value or undefined
 *             or retrieve object with key/value of all parameters
 */
//C8O.getLastCallParameter(key);

/**
 * isDefined function
 * just check the existence of the argument
 * obj: something to test
 * return: true > obj exists
 *            false > obj doesn't exist
 */
//C8O.isDefined(obj);

/**
 * isUndefined function
 * just check the existence of the argument
 * obj: something to test
 * return: true > obj doesn't exist
 *            false > obj exists
 */
//C8O.isUndefined(obj);

/**
 * log object
 * write the msg string into the console.log if available
 * or call the hook "log" if added.
 * msg: string with the message to log
 * e (optional): exception object to add to the log
 *               or a plain object
 *               or an array
 */
//C8O.log.error(msg, e);
//C8O.log.warn(msg, e);
//C8O.log.info(msg, e);
//C8O.log.debug(msg, e);
//C8O.log.trace(msg, e);

/**
 * removeRecallParameter function
 * reversed effect of addRecallParameter function
 * remove a parameter from automatically
 * added parameter list
 * parameter_name: parameter name to remove from the list
 */
//C8O.removeRecallParameter(parameter_name);

/**
 * serializeXML function
 * return a string representation of the xmlDom Document in a XML format
 * xmlDom: Document to transform
 * return: string of the xmlDom Document in a XML format
 */
//C8O.serializeXML(xmlDom);

/**
 * toJSON function
 * return a string representation of the data object (key/value) in a JSON format
 * data: object to transform
 * return: string of the data object in a JSON format
 */
//C8O.toJSON(data);

/**
 * translate function
 * if the i18n is enabled (C8O.ro_vars.files not empty)
 * this function translate each text node and each attribute content that contain
 * the __MSG_key__ marker, using the current dictionary.
 * It can also translate a key and return its value.
 * elt: element to translate or a string to translate
 * return: string translated or nothing in case of element parameter
 */
//C8O.translate(elt);

/**
 * waitHide function
 * hide the wait screen
 * by removing the element with the id wait_div
 */
//C8O.waitHide();

/**
 * waitShow function
 * show the wait screen
 * by putting the wait_div in the body element
 */
//C8O.waitShow();

/**
 * walk function
 * walk recursively a dom tree and apply a function on each text node and attributes
 * node: starting node of the walk, children will be walked recursively
 * data: contextual data passed to fn and fn_validate
 * fn  : function that process each text ; fn(txt, data, fn_validate){}
 *          this: current node
 *       txt : text to transform
 *       data: data passed to the walk function
 *       fn_validate: fn_validate passed to the walk function
 *       return: new value of txt, or null to do nothing
 * fn_validate: function that process each node and can stop the walk for its node
 *              node: current node to test
 *              data: data passed to the walk function
 *              return: true to continue the walk, false to stop
 */
//C8O.walk(node, data, fn, fn_validate);

/*******************************************************
 * List of possible hooks *
 *******************************************************/

/**
 *  call hook
 *  used before AJAX request to CEMS server
 *  can tweak data before sending
 *  or perform request itself
 *
 *  data: key/value map of parameters sent to CEMS
 *  return: true > lets C8O perform the call
 *             false > C8O doen't perform the call
 */
//C8O.addHook("call", function (data) {
//    return true;
//});

/**
 *  call_complete hook
 *  called after the xml_response, text_response or call_error hook
 *
 *  jqXHR: the jQuery object that enhance the XHR used by the call
 *  textStatus: text status of the Ajax response
 *  data: data used to generate the C8O.call
 *  return: true > hide the wait div if no pending call
 *             false > lets the wait div
 */
//C8O.addHook("call_complete", function (jqXHR, textStatus, data) {
//    return true;
//});

/**
 *  call_error hook
 *  called call_complete hook, in case of an Ajax error (network error, unparsable response)
 *
 *  jqXHR: the jQuery object that enhance the XHR used by the call
 *  textStatus: text status of the Ajax response
 *  errorThrown: caught cause of the error
 *  data: data used to generate the C8O.call
 *  return: true > log the error with C8O.log.error
 *             false > don't log the error
 */
//C8O.addHook("call_error", function (jqXHR, textStatus, errorThrown, data) {
//    return true;
//});

/**
 *  document_ready hook
 *  used at page loading
 *  can perform some DOM tweak
 *  or break the processing of request
 *
 *  return: true > lets C8O perform the init
 *             false > break the processing of request
 */
//C8O.addHook("document_ready", function () {
//    return true;
//});

/**
 *  get_language hook
 *  used at page loading before document_ready and when i18n is enable
 *  (no empty C8O.ro_vars.i18n_files)
 *  can modify data parameter of the first call
 *  or break the processing of request
 *
 *  params: key/value object decoded from the current query or hash string
 *
 *  return: string > the current language to use
 *            other cases > use the default language detection
 */
//C8O.addHook("get_language", function (params) {
//
//});

/**
 *  init_finished hook
 *  used at page loading after C8O initialization
 *  can modify data parameter of the first call
 *  or break the processing of request
 *
 *  params: key/value object decoded from the current query or hash string
 *
 *  return: true > lets C8O perform the first call
 *             false > break the processing of request
 */
//C8O.addHook("init_finished", function (params) {
//    return true;
//});

/**
 *  log hook
 *  used on each C8O.log.xxx call.
 *  Allow to:
 *   * handle log message (put in div, send request …)
 *   * prevent log writing (return false)
 *   * modify the message (return a new msg content).
 *
 *  level: "string" level of this log, between error/warn/info/debug/trace
 *  msg: "string" the log message
 *  e: can be anything or nothing, but linked with the error
 *  return: "string" > logs in console and override the msg
 *             false > doesn't log in console
 *             nothing or true > logs in console
 */
//C8O.addHook("log", function (level, msg, e) {
//    return false;
//});

/**
 *  mashup_event hook
 *  used for handle doMashupEvent call
 *  and used to implement how to forward event
 *  to the 'mashup' container
 *
 *  eventName: name of the event
 *  payload: key/value map object
 */
//C8O.addHook("mashup_event", function (eventName, payload) {
//
//});

/**
 *  receive_mashup_event hook
 *  used for handle Mashup event for this widget
 *  to the 'mashup' container
 *
 *  event.origin: widget name of the event source
 *  event.name: name of the event
 *  event.payload: key/value map object
 *  event.target: widget name of the event target
 *  event.type: type of the event, the default is 'call'
 *
 *  return: true > lets C8O consume the event
 *             false > event ignored by C8O
 */
//C8O.addHook("receive_mashup_event", function (event) {
//    return true;
//});

/**
 *  resize_calculation hook
 *  used after the content is filled
 *  for calculate the height of the
 *  iframe element
 *
 *  return: false > bypass C8O resize
 *             type of 'number' > height for the iframe
 *             other > do standard resize
 */
//C8O.addHook("resize_calculation", function () {
//    return true;
//});

/**
 *  result_filled hook
 *  used after the content is filled
 *  but before set event listener
 *  and iframe resize
 *
 *  $container: jquery object where the content has been added
 *
 *  return: true > lets C8O perform the init
 *             false > bypass C8O resize
 */
//C8O.addHook("result_filled", function ($container) {
//    return true;
//});

/**
 *  siteclipper_page_loaded hook
 *  used after a siteclipped page is loaded
 *  and automatically resized
 *
 *  doc: document object of the current siteclipped page loaded
 */
//C8O.addHook("siteclipper_page_loaded", function (doc) {
//
//});

/**
 *  siteclipper_page_unloaded hook
 *  used when a siteclipped page is unloaded
 *
 *  $iframe: jQuery object with the iframe container of the siteclipped page selected
 *
 *  return: true > lets C8O perform recude the iframe
 *             false > bypass C8O resize
 */
//C8O.addHook("siteclipper_page_unloaded", function ($iframe) {
//    return true;
//});

/**
 *  text_response hook
 *  used for tweak, retrieve value or do transformation
 *  using the text response from CEMS (after a server XSL transformation)
 *
 *  aText: array with only one string, aText[0], of the text received
 *            and can be replaced by a new value
 *  return: true > lets C8O perform the inclusion in the DOM
 *             false > break the processing of the C8O
 */
//C8O.addHook("text_response", function (aText) {
//  var text = aText[0];
//    return true;
//});

/**
 *  wait_hide hook
 *  used after xml_response execution
 *  or on C8O.waitHide() call
 *  and hide the transparent mask
 *  
 *  data:  "object" data used to generate the C8O.call
 *  return: true > lets C8O hide the loading mask
 *             false > doesn't hide anything
 */
//C8O.addHook("wait_hide", function (data) {
//    return true;
//});

/**
 *  wait_show hook
 *  used at C8O.call calling
 *  or on C8O.waitShow() call
 *  and display a transparent mask
 *  that prevents the user to act
 *  
 *  data:  "object" data used to generate the C8O.call
 *  return: true > lets C8O display the loading mask
 *             false > doesn't display anything
 */
//C8O.addHook("wait_show", function (data) {
//    return true;
//});

/**
 *  xml_response hook
 *  used for tweak, retrieve value or do transformation
 *  using the XML response from CEMS
 *
 *  xml: pure DOM document
 *  return: true > lets C8O perform the xml
 *             false > break the processing of xml
 */
//C8O.addHook("xml_response", function (xml) {
//    return true;
//});
