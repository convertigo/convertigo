/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
        C8O.log.info("ctf.jqm : change page to \"" + goToPage + "\"");
        $(":mobile-pagecontainer").pagecontainer("change", goToPage, options);
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
                $page.data("c8o-ctf-init", true);
                C8O._ctfjqm_onDocumentReadyEnd(callback, $page);
            }
        };
        
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
            var refreshed = false;
            var role = $elt.jqmData("role");

            try {
                if (role) {
                    var widget = $elt.data("mobile-" + role);
                    if (widget) {
                        if (widget.refresh) {
                            widget.refresh();
                            refreshed = true;
                        } else if (widget._create) {
                            widget._create();
                            refreshed = true;
                        }
                    }
                }
            } catch (e) {
                C8O.log.warn("ctf.jqm : failed to refresh by role:" + role, e);
            }
            
            if (!refreshed) {
                $elt.enhanceWithin();
            }
        } catch (e) {
            C8O.log.warn("ctf.jqm : render finish failed", e);
        }
    }
});

C8O.addHook("_newContent", function ($elt) {
    $elt.find("form[data-c8o-call]").attr("data-ajax", "false");
});