/** needs jquery.js weblib.core.js weblib.mobile.js ctf.core.js */

$.extend(true, C8O, {
	isActivePage: function (fromPage) {
		return $.mobile.activePage.is(fromPage);
	},
	
	_changePage: C8O.changePage,
	changePage: function (goToPage, options, callback) {
		// Bind a listener on the 'pagebeforeshow' event in order
		// to render bindings only after the page is shown
		$(document).one("pagebeforeshow", function (event) {
			C8O._changePage(goToPage, options, callback)
		});
		
		// Change page
		$.mobile.changePage(goToPage, options);
	},
	
	__onDocumentReadyEnd: C8O._onDocumentReadyEnd, 
	_onDocumentReadyEnd: function (callback) {
		$.mobile.changePage.defaults.allowSamePageTransition = true;
		$(document).on("pagebeforecreate", "[data-role=page]", function () {
			C8O.__onDocumentReadyEnd(callback);
		});
	}
});