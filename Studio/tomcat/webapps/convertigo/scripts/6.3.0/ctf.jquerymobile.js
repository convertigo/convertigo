/** needs jquery.js weblib.core.js weblib.mobile.js ctf.core.js */

$.extend(true, C8O, {
	
	_ctfjqm_changePage: C8O._changePage,
	_changePage: function (goToPage, options, callback) {
		// Bind a listener on the 'pagebeforeshow' event in order
		// to render bindings only after the page is shown
		$(document).one("pagebeforeshow", function (event) {
			C8O._ctfjqm_changePage(goToPage, options, callback)
		});
		
		// Change page
		C8O.log.info("ctf.jqm: change page to \"" + goToPage + "\"");
		$.mobile.changePage(goToPage, options);
	},
	
	_isActivePage: function (fromPage) {
		var ret = $.mobile.activePage.is(fromPage);
		C8O.log.trace("ctf.jqm : is \"" + fromPage + "\" the active page ? " + ret);
		return ret;
	},
	
	_ctfjqm_onDocumentReadyEnd: C8O._onDocumentReadyEnd, 
	_onDocumentReadyEnd: function (callback) {
		$.mobile.changePage.defaults.allowSamePageTransition = true;
		$(document).on("pagebeforecreate", "[data-role=page]", function () {
			C8O.log.debug("ctf.jqm : new DOM page loaded, initialize it");
			C8O._ctfjqm_onDocumentReadyEnd(callback);
		});
	},
	
	_ctfjqm_renderFinish: C8O._renderFinish,
	_renderFinish: function ($elt) {
		try {
			C8O._ctfjqm_renderFinish($elt);
			
			if ($elt.data("mobile-page")) {
				C8O.log.info("ctf.jqm : mobile page");
				$elt.data("mobile-page")._trigger("create",null,{})
			} else {
				$elt.trigger("create");
		         if (!C8O.isUndefined($elt.listview)) {
		        	 try {
		        		 $elt.listview("refresh");
		        	 } catch (e) {
		        		 C8O.log.trace("ctf.jqm : listview failed to refresh", e);
		        	 }
				}
			}
		} catch (e) {
			C8O.log.warn("ctf.jqm : render finish failed", e);
		}
	}
});