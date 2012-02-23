/*******************************************************
 *******************************************************
 * public C8O API for CEMS 5.6.0 *
 *******************************************************
 *******************************************************/


/*******************************************************
 * Global variables *
 *******************************************************/

$.extend(true, C8O, {
	/**
	 * init_vars variables values can only be set before the first C8O.call(),
	 * by the code or by the first query,
	 * their values must be strings, 
	 * their state cannot be modified later.
	 * 
	 * If set by query, variable name should be preceded by __
	 * for example: ?__enc=true&...
	 */
	init_vars : {
//		enc : "false", /** enables rsa encoding */
//		testplatform : "auto" /** auto/true/false : automatically redirect to the testplatform if no parameter is set, force testplaform if true or just call C8O if false */
	},
	/**
	 * vars variables values can be set at any time, 
	 * by the code, by the query or by passing arguments to C8O.call(), 
	 * their values must be strings,
	 * their state cannot be modified later.
	 * 
	 * Value can be modified by code, 
	 * for example: C8O.vars.ajax_method="GET"
	 * 
	 * If set by query, variable name should be preceded by __
	 * for example: ?__ajax_method=GET&...
	 */
	vars : {
//		ajax_method : "POST", /** POST/GET : http method to request CEMS */
//		auto_refresh : "true", /** true/false : allow auto refresh feature for clipping */
//		auto_resize : "true", /** true/false : allow weblib to perform resize after content filled */
//		requester_prefix : "", /** string prepend to the .xml or .cxml requester */
//		resize_offset : "50", /** integer : number of pixel added to the automatic resize */
//		send_portal_username : "true", /** true/false : (gatein only) automatically add a portal_username parameter with the name of the logger user */
//		target_append : "false", /** true/false : append content to target_id or to body element */
//		target_id : "", /** element id : element id for result insertion or a selected jquery object */
//		use_siteclipper_plugin : "true", /** true/false : use the iframe encapsulation for siteclipper request */
//		xsl_side : "client" /** client/server : force the side of the xsl transformation */
	}
});

/**
 * ReadOnly variables are also available
 * C8.ro_vars variables values can be READ at any time by the code 
 * and must not be modified.
 * 
 * C8O.ro_vars.portal_username : string containing the name of the current logged user (gatein only)
 * C8O.ro_vars.widget_name : string containing the name of the current widget, if any 
 */


/*******************************************************
 * Functions *
 *******************************************************/


/**
 * addHook function
 * some part of the weblib can be customized using a hook
 * just specify the hook name and its handler
 * all existing hook are explain bellow
 * name : string of the hook name
 * fn : function of the handler
 */
//C8O.addHook(name, fn);

/** 
 *  addRecallParameter function
 *  force C8O.call() to send automatically parameters
 *  added by this function with its last value
 *  already added parameter are __connector and __context
 *  to save a new value for a parameter, specify it to the C8O.call() function
 *  or call C8O.addRecallParameter again
 *  parameter_name : string of the parameter name to automatically send
 *  parameter_value (optional) : initial value for this parameter
 */
//C8O.addRecallParameter(parameter_name, parameter_value);

/**
 * call function
 * make an AJAX request to CEMS in order to execute
 * a transaction or a sequence using specified parameters
 * data : string (query form) or Object (key/value) or HTML Form element
 *          used as AJAX parameters
 */
//C8O.call(data)

/** 
 *  doMashupEvent function
 *  dispatch a mashup event to the current container if any
 *  via the invocation of mashup_event hook
 *  event_name : string of the parameter name to automatically send
 *  payload (optional) : key/value map object ( {key: "value"} ) or an HTML Element.
 *                             In case of HTML Element, its attributes are transformed to a key/value map object.
 */
//C8O.doMashupEvent(event_name, payload);

/**
 * doNavigationBarEvent function
 * for HTML connector only
 * send an action to the navigation bar of the connector
 * action : string of value 'backward', 'forward', 'stop' or 'refresh'
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
 * height (optional) : number of the iframe height in pixel
 *                          automatically calculed if empty
 * options (optional) : options parameter for the jquery animate() function
 *                          see http://api.jquery.com/animate/ for more details
 */
//C8O.doResize(height, options);

/**
 * getLastCallParameter function
 * used for retrieve a parameter from the previous call
 *  or all parameter in a object key/value
 *  key : string of the parameter name
 *  return : string of the parameter value or undefined
 *             or retrieve object with key/value of all parameters
 */
//C8O.getLastCallParameter(key);

/**
 * isDefined function
 * just check the existence of the argument
 * obj : something to test
 * return : true > obj exists
 *            false > obj doesn't exist
 */
//C8O.isDefined(obj);

/**
 * isUndefined function
 * just check the existence of the argument
 * obj : something to test
 * return : true > obj doesn't exist
 *            false > obj exists
 */
//C8O.isUndefined(obj);

/**
 * removeRecallParameter function
 * reversed effect of addRecallParameter function
 * remove a parameter from automatically
 * added parameter list
 * parameter_name : parameter name to remove from the list
 */
//C8O.removeRecallParameter(parameter_name);



/*******************************************************
 * List of possible hooks *
 *******************************************************/


/**
 *  call hook
 *  used before AJAX request to CEMS server
 *  can tweak data before sending
 *  or perform request itself
 *  
 *  data : key/value map of parameters sent to CEMS
 *  return : true > lets weblib perform the call
 *             false > weblib doen't perform the call
 */
//C8O.addHook("call", function (data) {
//	return true;
//});


/**
 *  document_ready hook
 *  used at page loading
 *  can perform some DOM tweak
 *  or break the processing of request
 *  
 *  return : true > lets weblib perform the init
 *             false > break the processing of request
 */
//C8O.addHook("document_ready", function () {
//	return true;
//});

/**
 *  init_finished hook
 *  used at page loading after weblib initialization
 *  can modify data parameter of the first call
 *  or break the processing of request
 *  
 *  return : true > lets weblib perform the first call
 *             false > break the processing of request
 */
//C8O.addHook("init_finished", function (data) {
//	return true;
//});

/**
 *  mashup_event hook
 *  used for handle doMashupEvent call
 *  and used to implement how to forward event
 *  to the 'mashup' container
 *  
 *  eventName : name of the event
 *  payload : key/value map object
 */
//C8O.addHook("mashup_event", function (eventName, payload) {
//
//});

/**
 *  receive_mashup_event hook
 *  used for handle Mashup event for this widget
 *  to the 'mashup' container
 *  
 *  event.origin : widget name of the event source
 *  event.name : name of the event
 *  event.payload : key/value map object
 *  event.target : widget name of the event target
 *  event.type : type of the event, the default is 'call'
 *  
 *  return : true > lets weblib consume the event
 *             false > event ignored by weblib
 */
//C8O.addHook("receive_mashup_event", function (event) {
//	return true;
//});

/**
 *  xml_response hook
 *  used for tweak, retrieve value or do transformation
 *  using the XML response from CEMS
 *  
 *  xml : pure DOM document
 *  return : true > lets weblib perform the xml
 *             false > break the processing of xml
 */
//C8O.addHook("xml_response", function (xml) {
//	return true;
//});

/**
 *  text_response hook
 *  used for tweak, retrieve value or do transformation
 *  using the text response from CEMS (after a server XSL transformation)
 *  
 *  aText : array with only one string, aText[0], of the text received
 *            and can be replaced by a new value
 *  return : true > lets weblib perform the inclusion in the DOM
 *             false > break the processing of the weblib
 */
//C8O.addHook("text_response", function (aText) {
//  var text = aText[0];
//	return true;
//});

/**
 *  resize_calculation hook
 *  used after the content is filled
 *  for calculate the height of the
 *  iframe element
 *  
 *  return : false > bypass weblib resize 
 *             type of 'number' > height for the iframe
 *             other > do standard resize
 */
//C8O.addHook("resize_calculation", function () {
//	return true;
//});


/**
 *  result_filled hook
 *  used after the content is filled
 *  but before set event listener
 *  and iframe resize
 *  
 *  $container : jquery object where the content has been added
 *  
 *  return : true > lets weblib perform the init 
 *             false > bypass weblib resize
 */
//C8O.addHook("result_filled", function ($container) {
//	return true;
//});

/**
 *  siteclipper_page_loaded hook
 *  used after a siteclipped page is loaded
 *  and automatically resized
 *  
 *  doc : document object of the current siteclipped page loaded
 */
//C8O.addHook("siteclipper_page_loaded", function (doc) {
//	
//});

/**
 *  siteclipper_page_unloaded hook
 *  used when a siteclipped page is unloaded
 *  
 *  $iframe : jQuery object with the iframe container of the siteclipped page selected
 *  
 *  return : true > lets weblib perform recude the iframe 
 *             false > bypass weblib resize
 */
//C8O.addHook("siteclipper_page_unloaded", function ($iframe) {
//	return true;
//});
