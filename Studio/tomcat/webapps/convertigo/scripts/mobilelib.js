/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

/* public C8O Mobile API for CEMS 5.6.0 */

Ext.namespace('C8O');

Ext.apply(C8O, {
	regexp: {
		extractURL: new RegExp("(.*?/projects/)(.*?)(/.*)"),
		extractParent: new RegExp("(.*)\\.([^.]*)")
	}
});
/**
 * Override the standard reader readRecords method to handle exceptions
 */
/*
var readerReadRecords = Ext.data.Reader.prototype.readRecords;
Ext.override(Ext.data.Reader, {
	readRecords: function (data) {
		try {
			readerReadRecords.call(this, data);
		} catch (Err) {
			Ext.Msg.alert('Convertigo', 'Error while receiving data : ' + Err);
		}
	}
});
*/

/**
 * @class C8O.Server
 * @extends Ext.util.Observable
 * 
 *** Extend Ext.util.Observable : http://dev.sencha.com/deploy/touch/docs/?class=Ext.util.Observable ***
 * Defines a Convertigo server. Each C8O mobile project must specify at least one
 * Server. This object will be used to interact with the C80 server directly
 * or through the C80.Store object.
 * 
 * sample code to create :
 * 
 * app.server = new C80.Server({
 * 		endpoint: 'http://<myC8OServer>/convertigo/projects/<MyProject>
 * });
 * 
 */
C8O.Server = Ext.extend(Ext.util.Observable, {
	/**
	 * Constructor
	 */
	constructor: function (config) {
		if (Ext.isDefined(config.endpoint)) {
			this.endpoint = config.endpoint;
		} else {
			var mUrl = C8O.regexp.extractURL.exec(window.location.href);
			this.endpoint = mUrl[1] + mUrl[2];
		}
		this.endpoint += '/request.jsonp';
	},
	
	/**
	 * Prepare an args object to a plain key/value object
	 * 
	 * The args object can contains :
	 *  * form : an Ext form object, used to retrieve fields
	 *  * params : a plain key/value object copied to the result
	 *  * connector : the connector name used by convertigo
	 *  * transaction : the transaction name used by convertigo
	 *  * sequence : the transaction name used by convertigo
	 */
	prepareParameters: function (args) {
		var params = {};
		if (Ext.isDefined(args.form)) {
			Ext.apply(params, args.form.getValues(false)); // also disabled fields
		}
		if (Ext.isDefined(args.params)) {
			Ext.apply(params, args.params);
		}
		if (Ext.isDefined(args.connector)) {
			params.__connector = args.connector;
		}
		if (Ext.isDefined(args.transaction)) {
			params.__transaction = args.transaction;
		}
		if (Ext.isDefined(args.sequence)) {
			params.__sequence = args.sequence;
		}
		return params;
	},
	
	/**
	 * Wrapper to execute a C8O Transaction or Sequence
	 *
	 * Sample code to invoke :
	 * 
	 * 	server.execute({
	 * 		transaction: '<transac name>', connector: '<connector name>', // or
	 * 		sequence: '<sequence name>',	//
	 * 		project : '<project name>', // the optional project name used by convertigo
	 * 		params: {						// Optional Additional parameters
	 * 			param1: 'value1',
	 * 			param2: 'value3',
	 * 			param3: 'value3',
	 * 		}, 
	 * 		form: MyForm					// Optional form  from which query string will be build
	 * 		mask: mask,						// the optional 'loading' mask
	 * 		callback: function (data) {		// A call back to handle return data
	 * 			console.log("Call C8O ok" + data);
	 * 		}
	 *  });
	 */
	execute: function (args) {		
		var params = this.prepareParameters(args);		
		if (Ext.isDefined(args.mask)) {
			args.mask.show();
		}
		var url = this.endpoint; 
		if (Ext.isDefined(args.project)) {
			url = this.__endpointForProjectName(args.project);
		}
		Ext.util.JSONP.request({
			url: url,
			params: params,
			callbackKey: '__callback',
			callback: function(data) {
				if (Ext.isDefined(args.mask)) {
					args.mask.hide();
				}
				args.callback(data);
			}
		});
	},
	
	/**
	 * Compute a modified endpoint in order to use requestables from another Convertigo project.
	 * It's an internal method for mobilelib usage only !
	 * 
	 * Sample code to invoke :
	 * 
	 * app.server.__endpointForProjectName('projectName');
	 */
	__endpointForProjectName: function(projectName) {
		var mEndpoint = C8O.regexp.extractURL.exec(this.endpoint);
		return mEndpoint[1] + projectName + mEndpoint[3];
	}
});

C8O.Reader = Ext.extend(Ext.data.JsonReader, {
	constructor: function (config) {
		this.store = config.store;
		/**
		 * Call the default constructor with the config.
		 */
		C8O.Reader.superclass.constructor.call(this, config);
	},
	readRecords: function (data) {
		var root;
		try {
			root = this.getRoot(data);	
		} catch(e) {
			// node doesn't exist
		}
		if (Ext.isDefined(root) && !Ext.isArray(root)) {
			var match = C8O.regexp.extractParent.exec(this.root);
			if (match !== null) {
				var parent = match[1];
				var child = match[2];
				var getParent = this.createAccessor(parent);
				var parentItem = getParent(data);
				parentItem[child] = [root];
			}
		}
		if (Ext.isDefined(this.store.callback)) {
			var ret = this.store.callback.call(this.store, data);
			if (ret === false) {
				return {records: []};
			}
		}
		return C8O.Reader.superclass.readRecords.call(this, data);
	}
});

/**
 * @class C8O.Store
 * @extends Ext.data.Store
 * 
 *** Extend Ext.data.Store : http://dev.sencha.com/deploy/touch/docs/?class=Ext.data.Store ***
 * This defines the Convertigo Store object derived from the Ext.data.store
 * This store is automatically configured for a JSONP proxy pointing on a Convertigo
 * C8O.Server object. 
 * 
 * Sample of creation :
 * 
 *   store = new C8O.Store({
 *	    model: 'RoomList',						// a registered model
 *		root: 'document.resultats.resultat',	// the root object to iterate on
 *		server : server							// a C8O.Server object
 *	 });
 *	   
 */
C8O.Store = Ext.extend(Ext.data.Store, {
	
	/**
	 * Constructor
	 */
	constructor: function (config) {
		this.server = config.server;
		/**
		 * create a scriptTag Proxy and set it in the base Store class
		 */
		Ext.apply(config, {
			proxy: {
				type: 'scripttag',
				reader: new C8O.Reader({
					root: config.root,
					store: this
				}),			
				callbackParam: '__callback',
				listeners: {
			        exception: function (obj, response, operation) {
			            console.log(response);
			        }
			    }
			}
		});
		
		/**
		 * Call the default constructor with the config.
		 */
		C8O.Store.superclass.constructor.call(this, config);
	},

	/**
	 * Loads the store's data from Convertigo server.
	 * 
	 * @param options the options for load same as Ext.data.Store.load() options plus :
	 * 
	 * The options object can contains :
	 *  * form : an Ext form object, used to retrieve fields
	 *  * params : a plain key/value object copied to the result
	 *  * connector : the connector name used by convertigo
	 *  * transaction : the transaction name used by convertigo
	 *  * sequence : the transaction name used by convertigo
	 *  * project : the optional project name used by convertigo
	 *  * callback : the callback function called after data received, receive a rawData as parameter
	 *  	and can break default loading process by returning false
	 *  
	 *  If no option is given, the data will be loaded with the default transaction
	 *  
	 *  Sample of use :
	 *  
	 *  store.load({
	 *  	transaction: 'myTransaction',
	 *  	form: form
	 *  });
	 *   
	 */
	load: function (options) {
		var params = this.server.prepareParameters(options);
		this.callback = options.callback;
		this.proxy.url = this.server.endpoint;
		if (Ext.isDefined(options.project)) {
			this.proxy.url = this.server.__endpointForProjectName(options.project);
		}
		C8O.Store.superclass.load.call(this, {params: params});
	}
});

/**
 * Enhance tablet detection
 */
if (Ext.version.indexOf("1.1.") === 0) {
	Ext.is.platforms.push({
			property: 'userAgent',
			regex: /(?=.*android)(?=.*mobile)/i,
			identity: 'AndroidPhone'
		}, {
			property: 'userAgent',
			regex: /playbook/i,
			identity: 'Playbook'
	});
	Ext.is.init();
	Ext.is.AndroidTablet = Ext.is.Android && (!Ext.is.AndroidPhone);
	Ext.is.Tablet = Ext.is.iPad || (Ext.is.Android && !Ext.is.AndroidPhone) || Ext.is.Playbook;	
}

Ext.onReady(function () {
	/**
	 * Detect if it's a webkit browser and display a warning otherwise
	 */
	var ua = navigator.userAgent.toLowerCase();
	if (ua.indexOf(" applewebkit/") == -1) {
		var warn = Ext.DomHelper.append(Ext.getBody(), {
			tag: 'div',
			html: 'Warning: you are not using a Webkit based browser. Some issues can be present. Click to remove'
		}, true).setStyle({
			'position': 'absolute',
			'z-index': '10',
			'color': 'red',
			'background-color': 'white'
		}).on('click', function (e, t) {
			warn.remove();
		});
	}
	
	/**
	 * Enable the right css in auto mode
	 */
	var links = Ext.select('link[title]');
	if (links.getCount() > 1) {
		try {
			ua = window.frameElement.useragent;		
		} catch (e) { }
		
		var mode = (ua.indexOf('blackberry') != -1 ? 'bb6'
				: ua.indexOf('android') != -1 ? 'android'
				: ua.indexOf('windowsphone') != -1 ? 'windowsphone'
				: ua.search('iphone|ipad|ipod') != -1 ? 'apple'
				: 'senchatouch');
		
		links.filter('link[title=' + mode + ']').set({'title': null}, false);
		Ext.select('link[title]').remove();
	}
});