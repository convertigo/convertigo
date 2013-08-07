/** needs jquery.js weblib.core.js weblib.mobile.js ctf.core.js */

$.extend(true, C8O, {
	isActivePage: function (fromPage) {
		var ret = $.mobile.activePage.is(fromPage);
		C8O.log.trace("ctf.jquerymobile: is '" + fromPage + "' the active page ? " + ret);
		return ret;
	},
	
	_changePage: C8O.changePage,
	changePage: function (goToPage, options, callback) {
		// Bind a listener on the 'pagebeforeshow' event in order
		// to render bindings only after the page is shown
		$(document).one("pagebeforeshow", function (event) {
			C8O._changePage(goToPage, options, callback)
		});
		
		// Change page
		C8O.log.debug("ctf.jquerymobile: change page to " + goToPage);
		$.mobile.changePage(goToPage, options);
	},
	
	__onDocumentReadyEnd: C8O._onDocumentReadyEnd, 
	_onDocumentReadyEnd: function (callback) {
		$.mobile.changePage.defaults.allowSamePageTransition = true;
		$(document).on("pagebeforecreate", "[data-role=page]", function () {
			C8O.log.debug("ctf.jquerymobile: new DOM page loaded, initialize it");
			C8O.__onDocumentReadyEnd(callback);
		});
	},
	
	__renderFinish: C8O._renderFinish,
	_renderFinish: function ($elt) {
		try {
			C8O.__renderFinish($elt);
			$elt.trigger("create");
		} catch (e) {
			C8O.log.warn("ctf.jquerymobile: render finish failed", e);
		}
	}
});