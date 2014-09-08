/*******************************************************
 *******************************************************
 * public C8O API for CEMS 7.2.0
 * for a jQuery Mobile application using the CTF
 * 
 * Dependences in HTML file:
 * * jquery(.min).js
 * * c8o.core.js
 * * c8o.jquerymobile.js
 * * ~ c8o.cordova.js (for builded application)
 * * ctf.core.js
 * * ctf.jquerymobile.js
 * * custom.js (this file)
 * * jquery.mobile(.min).js
 * 
 * You can find documentation about Convertigo Templating Framework here:
 * http://help.convertigo.com/latest/topic/com.twinsoft.convertigo.studio.help/help/helpRefManual/convertigoTemplatingFramework.html
 * or
 * http://help.convertigo.com/7.2.0/topic/com.twinsoft.convertigo.studio.help/help/helpRefManual/convertigoTemplatingFramework.html
 * 
 * You can find documentation about Convertigo Internationalization Framework (CTF plugin) here:
 * http://help.convertigo.com/latest/topic/com.twinsoft.convertigo.studio.help/help/helpRefManual/internationalization.html
 * or
 * http://help.convertigo.com/7.2.0/topic/com.twinsoft.convertigo.studio.help/help/helpRefManual/internationalization.html
 * 
 *******************************************************
 *******************************************************/


/*******************************************************
 * Global variables *
 *******************************************************/

$.extend(true, C8O, {
	/**
	 * init_vars variables values can only be set before the "init_finish" hook,
	 * by the code,
	 * their values must be strings, 
	 * their state cannot be modified later.
	 */
	init_vars: {
//		enc: "false", /** enables rsa encoding */
//		i18n: "" /** in case of multi-language application, force usage of the language selected. Empty string will select the browser language */
	},
	
	/**
	 * ro_vars read-only variables values can only be set directly here, not dynamically
	 */
	ro_vars: {
//		i18n_files: [] /** list of language available for the application. The first is the default language. The application must have an i18n folder with 1 file per language like: i18n/en.json */
	},
	
	/**
	 * cordova read-only variables values can only be set directly here, not dynamically. Used by c8o.cordova.js
	 */
	cordova: {
//		androidSenderID: ""
	},
	
	/**
	 * vars variables values can be set at any time.
	 * by the code, or by passing arguments to C8O.call() by adding __ 
	 * their values must be strings,
	 * their state can be modified later.
	 * 
	 * Value can be modified by code, 
	 * for example: C8O.vars.ajax_method="GET"
	 */
	vars: {
//		ajax_method: "POST", /** POST/GET: http method to request CEMS */
//		endpoint_url: "", /** base of the URL CEMS calls. Should not be modified */
//		first_call: "false", /** true/false: automatically call convertigo using the page query/hash parameters, after the init_finished hook */
//		log_level: "warn", /** none/error/warn/info/debug/trace: filter logs that appear in the browser console */
//		log_line: "false", /** true/false: add an extra line on Chrome console with a link to the log */
//		requester_prefix: "", /** string prepend to the .xml or .cxml requester */
/** c8o.cordova.js vars */
//		local_cache_parallel_downloads: 5 /** for local cache response to store, set the maximum number of parallel downloads for attachments. 0 will disable download */		
	},
	
	options: {
//		loading: {} /** loading option object argument for the $.mobile.loading("show") called by C8O.waitShow() */
	},
	
	routingTable: [
//		{
//			/**
//			* calledRequest parameter
//			* indicates on which requestables result the actions occur
//			* Can be one or more requestables, comma separated with the form :
//			* [project].connector.transaction
//			* or
//			* [project].sequence
//			* or
//			* [project].connector.* or [project].* or *
//			*/
//			calledRequest: "<the called C8O requestables>",
//
//			/**
//			* actions parameter
//			* array of actions, all executed in order if the current result match the 'calledRequest'
//			*/
//			actions: [
//				{
//					/**
//					 * afterRendering function
//					 * called after the rendering process.
//					 * $doc: JQuery object of the XML document response
//					 * c8oData: key/value parameters of the request
//					 */
//					afterRendering: function ($doc, c8oData) {
//					
//					},
//	
//					/**
//					 * beforeRendering function
//					 * called before the rendering process.
//					 * $doc: JQuery object of the XML document response
//					 * c8oData: key/value parameters of the request
//					 */
//					beforeRendering: function ($doc, c8oData) {
//						
//					},
//	
//					/**
//					 * condition function or selector
//					 * can be either a jQuery selector on the C8O XML response or a JavaScript function.
//					 * Called before the page changes.
//					 * The condition is considered as validated if the jQuery selector returns a non empty list,
//					 * or if the JS function returns true
//					 * $doc: JQuery object of the XML document response
//					 * c8oData: key/value parameters of the request
//					 */
//					condition: "jQuery selector",
//					condition: function ($doc, c8oData) {
//						return true;
//					},
//	
//					/**
//					 * fromPage parameter
//					 * list of HTML element ID defining the page we come from
//					 * before calling the C8O request
//				 	* (useful in order to route to different pages according to the origin page).
//					 * Use the .is(selector) from JQuery.
//					 * Sample: "#page1, #page2, #page3"
//					 */
//					fromPage: "",
//	
//					/**
//					 * goToPage parameter
//					 * an HTML page or an HTML element ID to display after the C8O call.
//					 * If not present, it means a local rendering (i.e. in the same page).
//					 */
//					goToPage: "",
//	
//					/**
//					 * options parameter
//					 * an optional transition information
//					 * (matching the jQueryMobile transition object format)
//					 * used to display the page given in the goToPage parameter.
//					 */
//					options: {}
//				}
//			]
//		},
//		/** full template condensed, must be comma separated */
//		{
//			calledRequest: "<the called C8O requestables>",
//			actions: [
//				{
//					condition: "<jQuery selector or function>",
//					fromPage: "<page ID>",
//					goToPage: "<page ID>",
//					options: {
//					},
//					beforeRendering: function ($doc, c8oData) {
//					},
//					afterRendering: function ($doc, c8oData) {
//					}
//					
//				}
//			]
//		}
	]
});

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
 * appendValues function
 * merge values of the source Object into the data Object
 * using appendValue on each source keys
 * data: Object (key/value) that will be modified
 * source: Object (key/value) that will be merged into data but not modified
 */
//C8O.appendValues(data, source);

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
 *  getCordovaEnv function
 *  ** Needs cordova.js + c8o.cordova.js **
 *  return the specific value of the cordova environment or the whole environment (without parameter)
 *  the following keys should be present:
 *  	applicationAuthorName, applicationAuthorEmail, applicationAuthorWebsite, applicationDescription, applicationId, applicationName,
 *  	builtRevision, builtVersion, currentRevision, currentVersion,
 *  	endPoint, platform, platformName, projectName, uuid.
 *  
 *  key (optional): string of the environment key to return, or the whole key/value environment object if no parameter
 *  return: specific value or whole the cordova environment
 */
//C8O.getCordovaEnv(key);

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
 * log object and functions
 * write the msg string into the console.log if available
 * or call the hook "log" if added.
 * msg: string with the message to log
 * e (optional): exception object to add to the log
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
 *  splashscreenHide function
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hide the cordova splashscreen if any
 */
//C8O.splashscreenHide();

/**
 *  splashscreenHide function
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hide the cordova splashscreen if any
 */
//C8O.splashscreenShow();

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
 * by hiding the #c8oloading element and stop jquerymobile loading
 */
//C8O.waitHide();

/**
 * waitShow function
 * show the wait screen
 * by showing the #c8oloading element and start jquerymobile loading
 */
//C8O.waitShow();

/**
 * walk function
 * walk recursively a dom tree and apply a function on each text node and attributes
 * node: starting node of the walk, children will be walked recursively
 * data: contextual data passed to fn and fn_validate
 * fn  : function that process each text ; fn(txt, data, fn_validate){}
 * 	     this: current node
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
//	return true;
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
//	return true;
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
//	return true;
//});

/**
 *  device_ready hook
 *  ** Needs cordova.js + c8o.cordova.js **
 *  used at device startup (doesn't work on browser) to initialize device things
 *  or break the processing
 *  
 *  return: true > lets C8O perform the init
 *             false > break the processing of request
 */
//C8O.addHook("device_ready", function () {
//	return true;
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
//	return true;
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
 *  or break the processing of request
 *  
 *  params: key/value object decoded from the current query or hash string
 *  
 *  return: true > lets CTF handle the document
 *             false > break the processing of request
 */
//C8O.addHook("init_finished", function (params) {
//	return true;
//});

/**
 *  local_cache_check_attachment hook
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hook called when a url will be downloaded for the local cache
 *  
 *  url: "string" the current url to download
 *  element:    "string" the current element where the "url" is found
 *  data:  "object" data used to generate the C8O.call
 *  return: true > lets c8o.cordova download the url and substitute the xml
 *            false > skip this url
 */
//C8O.addHook("local_cache_check_attachment", function (url, element, data) {
//	return true;
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
//	return false;
//});

/**
 *  push_notification hook
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hook called when a notification is received on the mobile device
 *  
 *  sender: "string" sender of the notification : GCM (Google) or APN (Apple)
 *  msg:    "string" the message content of the notification
 *  event:  "object" the raw object from the notification, sender specific
 *  return: true > lets c8o.cordova handle the response (Apple badge or sound)
 *            false > do nothing
 *            
 *  note:  documentation of event object can be found here :
 *  https://github.com/phonegap-build/PushPlugin
 *            
 */
//C8O.addHook("push_notification", function (sender, msg, event) {
//	return true;
//});

/**
 *  push_register_failed hook
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hook called when the registration failed on the mobile
 *  
 *  error: "string" cause of the error
 */
//C8O.addHook("push_register_failed", function (error) {
//	
//});

/**
 *  push_register_success hook
 *  ** Needs cordova.js + c8o.cordova.js **
 *  Hook called when the registration success on the mobile
 *  
 *  result: content from the notification system
 *  return: true > lets c8o.cordova handle the response and notify C8O PushManager
 *            false > do nothing
 */
//C8O.addHook("push_register_success", function (result) {
//	return true;
//});

/**
 *  xml_response hook
 *  used for tweak, retrieve value or do transformation
 *  using the XML response from CEMS
 *  
 *  xml: pure DOM document
 *  return: true > lets the CTF perform the xml
 *             false > break the processing of xml
 */
//C8O.addHook("xml_response", function (xml, data) {
//	return true;
//});
