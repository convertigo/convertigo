/*
 * Copyright (c) 2001-2013 Convertigo SA.
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

$.extend(true, C8O, {
	
	_cordova_notify_push_server: function (token) {
	    C8O._call({
	    	__project: "lib_PushManager",
	    	__sequence: "RegisterDevice",
	    	token: token
	    });
	},
	
	_cordova_onNotificationGCM: function (event) {
		switch (event.event) {
	        case "registered":
	        	C8O.log.debug("c8o.cordova: onNotificationGCM registered");
	        	
		        if (event.regid.length > 0) {
		        	C8O.log.info("c8o.cordova: onNotificationGCM registered regid: " + event.regid);
		        	
		        	if (C8O._hook("push_register_success", event.regid)) {
		        		C8O._cordova_notify_push_server("gcm:" + event.regid);
		        	}
		        }
		        break;
		        
	        default:
	        	C8O._hook("push_notification", "GCM", "TBD", event);
	        	break;
	    }
	},
	
	_cordova_onNotificationAPN: function (event) {
		if (C8O._hook("push_notification", "APN", "TBD", event)) {
			  if (event.sound) {
			        var snd = new Media(event.sound);
			        snd.play();
			  }
	
			  if (event.badge) {
			        pushNotification.setApplicationIconBadgeNumber(function () {
			        	// TODO
			        }, event.badge);
			  }
		}
	}
});

$(document).on("deviceready", function() {
	C8O.log.info("c8o.cordova: on deviceready");
	if (C8O._hook("device_ready")) {
		navigator.splashscreen.hide();
		C8O.log.info("c8o.cordova:window.plugins " + window.plugins);
		C8O.log.info("c8o.cordova:window.plugins.pushNotification " + window.plugins.pushNotification);
		
		if (C8O.isDefined(window.plugins) && C8O.isDefined(window.plugins.pushNotification)) {
			var pushNotification = window.plugins.pushNotification;
			
			C8O.log.info("c8o.cordova: pushNotification detected");
			
			var options;
			
			if (device.platform == 'android' || device.platform == 'Android') {
				C8O.log.info("c8o.cordova: Android detected");
				
				if (C8O.isDefined(C8O.cordova.androidSenderID) && C8O.cordova.androidSenderID.length > 0) {
					options = {
				    	"senderID": C8O.cordova.androidSenderID,
				    	"ecb": "C8O._cordova_onNotificationGCM"
				    };
				} else {
					C8O.log.error("c8o.cordova: no senderID for ");
					C8O._hook("push_register_failed", "missing senderID");
				}
			} else {
				C8O.log.info("c8o.cordova: IOs detected");
				
				options = {
		    		"badge": "true",
		    		"sound": "true",
		    		"alert": "true",
		    		"ecb": "C8O._cordova_onNotificationAPN"
			    };
			}
			
			pushNotification.register(
				function (result) {
					if (device.platform == 'android' || device.platform == 'Android') {
						C8O.log.info("c8o.cordova: Android PushNotificationRegistered: " + result);
					} else {
						C8O.log.info("c8o.cordova: iOS PushNotificationRegistered: " + result);
						if (typeof result == "string") {
							if (C8O._hook("push_register_success", result)) {
								C8O._cordova_notify_push_server("apns:" + result);
							}
						}
					}
				},
				function (error) {
					C8O.log.error("c8o.cordova: PushNotificationRegistered Failed: " + error);
					C8O._hook("push_register_failed", error);
				},
				options
			);
		}
	}
	C8O.log.debug("c8o.cordova: end deviceready");
});