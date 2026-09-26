/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.tutorial;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * A page of the Studio tutorials of the Convertigo site, for the Tutorials panel of the web Studio, as the
 * Tutorial view of the Eclipse Studio shows it: the page keeps its resources on the site and gets the
 * bridge the tutorial talks to, posting its messages to the Studio. The Studio shows it in a sandboxed
 * frame, which keeps it away from the session of the Studio.
 * <ul>
 * <li>path: the path of the page on the site, under /studio-tutorials or /tutorials-low-code-studio</li>
 * </ul>
 */
@ServiceDefinition(name = "Page", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Page extends DownloadService {
	static final String SITE = "https://www.convertigo.com";
	static final Pattern TUTORIAL_PATH = Pattern.compile("/(studio-tutorials|tutorials-low-code-studio)(/[\\w-]+)*/?");

	/** the bridge of the tutorial, as the Eclipse Studio injects it: its messages go to the Studio */
	private static final String BRIDGE = """
			<base href="%s/">
			<script>
			(function () {
				var success = false;
				var page = %s;
				window.IDE = {
					message: function (message) {
						parent.postMessage({ c8oTutorial: message }, '*');
					}
				};
				window.tutoGoNext = function () {
					if (window.$) {
						$('.tutoerror').hide();
						$('.tutosuccess').fadeIn(500);
					}
					success = true;
				};
				window.addEventListener('message', function (event) {
					if (event.source === parent && event.data && event.data.c8oTutorial === 'next') {
						window.tutoGoNext();
					}
				});
				// the tutorial pages stay in the Studio
				document.addEventListener('click', function (event) {
					var link = event.target && event.target.closest ? event.target.closest('a[href]') : null;
					if (!link) {
						return;
					}
					var url = new URL(link.getAttribute('href'), '%s/');
					if (url.origin === '%s' && /^\\/(studio-tutorials|tutorials-low-code-studio)/.test(url.pathname)) {
						event.preventDefault();
						location.href = page + '?path=' + encodeURIComponent(url.pathname);
					} else if (url.origin !== location.origin && !url.href.endsWith('#')) {
						event.preventDefault();
						window.open(url.href, '_blank', 'noopener');
					}
				}, true);
				document.addEventListener('DOMContentLoaded', function () {
					if (!window.$) {
						return;
					}
					$(document).on('click', '.expandablegif', function (event) {
						IDE.message({ type: 'imgEnter', url: event.target.getAttribute('src') });
					}).on('click', "a:contains('ᐅ')", function (event) {
						if (success || !event.originalEvent || !event.originalEvent.isTrusted) {
							return true;
						}
						$('.tutoerror').fadeIn(500);
						return false;
					}).on('click', '.tutook', function () {
						$("a:contains('ᐅ')")[0].click();
					}).on('click', '.tutocancel', function () {
						$('.tutoerror').fadeOut(500);
						$('.tutosuccess').fadeOut(500);
					});
					IDE.message({ type: 'control', json: JSON.stringify(window.tuto || []) });
				}, { once: true });
			})();
			</script>
			""";

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		var path = request.getParameter("path");
		if (path == null || path.isBlank()) {
			path = "/studio-tutorials";
		}
		if (!TUTORIAL_PATH.matcher(path).matches()) {
			throw new ServiceException("The page " + path + " is not a tutorial of the Studio.");
		}
		String html;
		try (var got = Engine.theApp.httpClient4.execute(new HttpGet(SITE + path))) {
			var code = got.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new ServiceException("The tutorial " + path + " answers " + code + ".");
			}
			html = IOUtils.toString(got.getEntity().getContent(), StandardCharsets.UTF_8);
		}
		var page = request.getRequestURI();
		var bridge = BRIDGE.formatted(SITE, quote(page), SITE, SITE);
		var head = html.indexOf("<head>");
		html = head == -1 ? bridge + html : html.substring(0, head + 6) + bridge + html.substring(head + 6);
		response.setContentType("text/html; charset=UTF-8");
		// the page runs in its own origin, away from the session of the Studio
		response.setHeader("Content-Security-Policy", "sandbox allow-scripts allow-popups allow-popups-to-escape-sandbox");
		response.getWriter().write(html);
	}

	private static String quote(String text) {
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	@Override
	public boolean isXsrfCheck() {
		// the frame of the Studio loads it without the header of the calls
		return false;
	}
}
