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
	_onDocumentReadyEnd: function (callback, $page) {
		$.mobile.changePage.defaults.allowSamePageTransition = true;
		
		C8O._ctfjqm_onDocumentReadyEnd(callback, $page);

		$("[data-role=page]").data("c8o-ctf-init", true);

		var onPageBeforeCreate = C8O._onPageBeforeCreate;
		C8O._onPageBeforeCreate = function ($page) {
			onPageBeforeCreate($page);
			if (!$page.data("c8o-ctf-init")) {
				C8O.log.debug("ctf.jqm : new DOM page loaded, initialize CTF on it");
				$(this).data("c8o-ctf-init", true);
				C8O._ctfjqm_onDocumentReadyEnd(callback, $page);
			}
		};
		
//		$(document).on("pagebeforecreate", "[data-role=page]", function () {
//			var $page = $(this);
//			if (!$page.data("c8o-ctf-init")) {
//				C8O.log.debug("ctf.jqm : new DOM page loaded, initialize CTF on it");
//				C8O._ctfjqm_onDocumentReadyEnd(callback, $page);
//				$(this).data("c8o-ctf-init", true);
//			}
//		});
		
		C8O._ctfjqm_onJqmInitFinished();
	},
	
	_ctfjqm_onJqmInitFinished: C8O._onJqmInitFinished,
	_onJqmInitFinished: function () {
		C8O.log.info("c8o.jqm : differ jquery mobile");
	},
	
	_ctfjqm_renderFinish: C8O._renderFinish,
	_renderFinish: function ($elt) {
		try {
			C8O._ctfjqm_renderFinish($elt);
			
			if ($elt.data("mobile-page")) {
				C8O.log.info("ctf.jqm : mobile page");
				$elt.data("mobile-page")._trigger("create", null, {})
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