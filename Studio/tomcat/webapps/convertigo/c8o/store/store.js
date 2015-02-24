$.extend(true, C8O, {
	init_vars: {
		i18n: ""
	},
	
    ro_vars: {
    	i18n_files: ["en", "fr"]
    }
});

var store = {
	vars: {
		base_url: window.location.href.replace(new RegExp("(.*/).*"), "$1"),
		two_parent_level: "../../",
		custom_theme: "CustomTheme",
		c8oplayer: "c8oplayer://",
		convertigo_player_url_android: "https://play.google.com/store/apps/details?id=com.convertigo.mobile.ConvertigoPlayer",
		convertigo_player_url_ios: "https://itunes.apple.com/us/app/convertigo-player/id910448988?mt=8",
		convertigo_player_url_wp8: "http://www.windowsphone.com/en-us/store/app/convertigo-player/1c550e9f-f981-4b54-8328-8cbe26781e56",
		admin_services: "admin/services/",
		requested_platform: "all",
		android_platform: "Android",
		ios_platform: "IOs",
		wp8_platform: "WindowsPhone8",
		onAndroidDevice: false,
		onIosDevice: false,
		onWindowsPhone8Device: false,
		displayBanner: "displayBanner"
	},
	
	init: function () {
		C8O._getCallUrl = function () {
		    return store.vars.admin_services + C8O.vars.service;
		};
		
		$.ajaxSetup({
			type : "POST"
		});
		
		store.vars.admin_services = store.vars.two_parent_level + store.vars.admin_services;
		
		store.getRequestedPlatform();
		store.manageLoginLogoutBtn();
		
		store.manageBanner();
		store.createFooter();
		
		store.checkAuthentication();
	},
	
	call: function (service, data, callback) {
		C8O.vars.service = service;
		C8O.call(data);
	},
	
	numberOfApps: function() {
		return $("div.grid3 > div.mtm.col").length;
	},
	
	authenticate: function (login, password) {
		$.ajax({
		    data: {
		    	authUserName: login,
		    	authPassword: password,
		    	authType: "login"
		    },
		    url: store.getAuthenticateServiceUrl(),
		    dataType: "xml",
		    success: function (data, textStatus, jqXHR) {
		    	// Authenticate OK
		    	if (store.isAuthenticated($(data))) {
		    		$("form").slideUp("slow", function () {
		    			$("#logout").removeClass("hidden");
			    		$("#logout").slideDown("fast");
		    			store.updateUserName($(data));
		    		});
		    		
		    		store.getStoreContent();
		    	}
		    	// Authenticate not OK
		    	else {
		    		for (var i = 0; i < 8; ++i) {
						$("#loginform").animate({right : ((i % 2 == 0) ? "+" : "-") + "=10px"}, 50);
					}
		    	}
		    }
		});
	},
	
	checkAuthentication: function () {
		$.ajax({
			url: store.getServiceUrl("engine.CheckAuthentication"),
			success: function (data, textStatus, jqXHR) {
				if (store.isAuthenticated($(data))) {
					$("#logout").removeClass("hidden");
					store.updateUserName($(data));
		    	}
				else {
					$("#userValue").text(C8O.translate("defaultUserName"));
					$("form").removeClass("hidden");
		    	}
				
				// Load applications
				store.getStoreContent();
			}
		});
	},
	
	isAuthenticated: function ($data) {
		return $data.find("roles>role[name='AUTHENTICATED']").length > 0;
	},
	
	updateUserName: function ($data) {
		var user = $data.find("user").text();
		$("#userValue").text(user.length > 0 ? user : C8O.translate("guest"));
	},
	
	addMediaQueryFooter: function (imgWidth) {
		$("head").append("<style type='text/css'>" +
		          "@media screen and (max-width : 480px) {" + 
		          ".imgDl {width: " + imgWidth + "px !important;}" +
		          ".platformContainer {height:70px !important;line-height:70px !important;}" +
		          "#footer{height: 70px !important;line-height:70px !important;}" +
		          "}</style>");
	},
	
	addStoreLogo: function (imgName, url) {
		$("footer").append('<div class="platformContainer"><a href="' + url + '"><img class="imgDl' + store.vars.custom_theme + '" src="' + imgName +'"></a></div>');
	},
	
	addStoreLogoAndroid: function () {
		store.addStoreLogo("player_android.png", store.vars.convertigo_player_url_android);
	},
	
	addStoreLogoApple: function () {
		store.addStoreLogo("player_apple.png", store.vars.convertigo_player_url_ios);
	},
	
	addStoreLogoWP: function () {
		store.addStoreLogo("player_wp.png", store.vars.convertigo_player_url_wp8);
	},
	
	createFooter: function () {
		// Not mobile
		if (store.vars.requested_platform === "all") {
			store.addStoreLogoAndroid();
			store.addStoreLogoWP();
			store.addStoreLogoApple();
			
			store.addMediaQueryFooter(95);
			return;
		}
		
		// Mobile
		if (store.vars.requested_platform === store.vars.android_platform) {
			store.addStoreLogoAndroid();
		}
		else if (store.vars.requested_platform === store.vars.ios_platform) {
			store.addStoreLogoApple();
		}
		else if (store.vars.requested_platform === store.vars.wp8_platform) {
			store.addStoreLogoWP();
		}
		
		store.addMediaQueryFooter(140);
	},
	
	manageBanner: function () {
		if (window.localStorage.getItem(store.vars.displayBanner) == null) {
			$("#banner").removeClass("hidden");
			$("#closeBtn").removeClass("hidden");
		}
		else {
			$("#closeBtn").addClass("hidden");
			$("#banner").addClass("hidden");
			$("#banner").slideUp();
		}

		$("#infos img").click(function (evt) {
			$("#banner").removeClass("hidden");
			$("#banner").slideDown(function () {
				$("#closeBtn").removeClass("hidden");
			});
		});
		
		$("#closeBtn").click(function (evt) {
			$(this).addClass("hidden");
			$("#banner").slideUp();
			window.localStorage.setItem(store.vars.displayBanner, false);
		});
	},
	
	manageLoginLogoutBtn: function () {
		$("#login").click(function (evt) {
			evt.preventDefault();
			store.authenticate($("input[name='username']").val(), $("input[name='password']").val());
		});
		
		$("#logout").click(function (evt) {
			store.logout();
		});
	},
	
	getRequestedPlatform: function () {
		if (/Android/i.test(window.navigator.userAgent)) {
			store.vars.onAndroidDevice = true;
			store.vars.requested_platform = store.vars.android_platform;
		}
		else if (/iPad/i.test(window.navigator.userAgent) || /iPhone/i.test(window.navigator.userAgent) || /iPod/i.test(window.navigator.userAgent)) {
			store.vars.onIosDevice = true;
			store.vars.requested_platform = store.vars.ios_platform;
		}
		else if (/Windows Phone 8/i.test(window.navigator.userAgent)) {
			store.vars.onWindowsPhone8Device = true;
			store.vars.requested_platform = store.vars.wp8_platform;
		}
	},
	
	getStoreContent: function () {
		store.call("projects.GetStoreContent", {platform: store.vars.requested_platform});
	},
	
	logout: function () {
		$.ajax({
		    data: {
		    	authType: "logout"
		    },
		    url: store.getAuthenticateServiceUrl(),
		    success: function (data, textStatus, jqXHR) {
		    	document.location.reload();
		    }
		});
	},
	
	getWebAppUrl: function (value) {
		return store.vars.base_url + store.vars.two_parent_level + "projects/" + store.getProjectName($(this)) + "/DisplayObjects/mobile/app.html";
	},
	
	getProjectName: function ($elem) {
		return $elem.parents("*[data-project]:first").data("project");
	},
	
	getIconUrl: function (value) {
		return store.vars.base_url + store.vars.two_parent_level + "projects/" + store.getProjectName($(this)) + "/DisplayObjects/mobile/icon.png";
	},
	
	getPlatformClass: function (value) {
		return value.toLowerCase() + store.vars.custom_theme;
	},
	
	getPlatformUrl: function (value) {
		var package_url = store.getServiceUrl("mobiles.GetPackage?") + "project=" + store.getProjectName($(this)) + "&platform=" + value;
		
		// URL = launch player and install app
		if (!store.displayQrCode()) {
			return store.vars.c8oplayer + package_url;
		}

		// URL = QR code
		var currentPlatform = $(this).parent().prev().attr("class").replace(/ .*/, "").replace(new RegExp("(.*)" + store.vars.custom_theme), "$1");

		var target_url;
		if (currentPlatform === store.vars.android_platform.toLowerCase()) {
			target_url = store.vars.convertigo_player_url_android;
		}
		else if (currentPlatform === store.vars.ios_platform.toLowerCase()) {
			target_url = store.vars.convertigo_player_url_ios;
		}
		else if (currentPlatform === store.vars.wp8_platform.toLowerCase()) {
			target_url = store.vars.convertigo_player_url_wp8;
		}
		
		target_url += "#" + package_url;
		
		return store.vars.base_url + store.vars.two_parent_level + "qrcode?" + $.param({
			o: "image/png",
			e: "L",
			s: 4,
			d: target_url
		});
	},
	
	getLastHrefGenerated: function (value) {
		return $(this).prev().find("a").attr("href");
	},
	
	getServiceUrl: function (serviceName) {
		return store.vars.base_url + store.vars.admin_services + serviceName;
	},
	
	getAuthenticateServiceUrl: function () {
		return store.getServiceUrl("engine.Authenticate");
	},
	
	displayQrCode: function () {
		// We display QR code for all devices except for Android and iOS devices
		return !store.vars.onAndroidDevice && !store.vars.onIosDevice;
	},
	
	onAfterRendering: function ($doc, c8oData) {
		if (store.numberOfApps() > 0) {
			// Align icons
			if (window.navigator.userAgent.indexOf("Chrome") != -1 || window.navigator.userAgent.indexOf("Firefox") != -1
				|| window.navigator.userAgent.indexOf("Trident") != -1) {
				$("div.valignIcon").css("margin-bottom", "8px");
			}
			else if (window.navigator.userAgent.indexOf("Safari") != -1) {
				$("div.line").css("margin-bottom", "5px");
			}
			
			// Add QR code LightBox only if we do not launch the player
		    if (store.displayQrCode()) {
			    $(".fancybox").fancybox();
		    }
			
			// ScrollBar for platforms
		    $(".blockPlatform").mCustomScrollbar({
		        theme:"dark-thin"
		    });
		    
			$("#noApps").text("");
			
			$("div").find(".mtm.col").last().addClass("vspace");
		}
		// No app found
		else {
			$("#noApps").text(C8O.translate("noApp"));
		}
		
		// To place footer at the bottom
		$("body").css({"margin-bottom": $("footer").height()});
	}
};

C8O.addHook("init_finished", function (params) {
	store.init();
});
