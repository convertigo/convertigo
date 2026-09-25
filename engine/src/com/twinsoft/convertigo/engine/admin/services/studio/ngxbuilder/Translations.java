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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.TranslateUtils;

/**
 * Creates or updates the translations files of an NGX application, as the Eclipse Studio does: the titles
 * of its pages and its plain texts, with the ones of the shared components it uses, are the keys of the
 * files of the source and target languages, in DisplayObjects/mobile/assets/i18n.
 * <ul>
 * <li>id: the application</li>
 * <li>from, to: the source and target languages, as "en" or "fr"</li>
 * </ul>
 */
@ServiceDefinition(name = "Translations", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Translations extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var from = language(request.getParameter("from"));
		var to = language(request.getParameter("to"));
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!(dbo instanceof ApplicationComponent application)) {
			throw new ServiceException("The object " + id + " is not an NGX application.");
		}
		if (from == null || to == null) {
			throw new ServiceException("The languages must be codes as \"en\" or \"fr\".");
		}
		var texts = texts(application);
		var i18nDir = new File(application.getProject().getDirPath(), "DisplayObjects/mobile/assets/i18n");
		var files = new ArrayList<String>();
		for (var language : from.equals(to) ? List.of(from) : List.of(from, to)) {
			var file = new File(i18nDir, language + ".json");
			TranslateUtils.storeTranslations(texts, file);
			files.add(application.getProject().getName() + "/DisplayObjects/mobile/assets/i18n/" + file.getName());
		}
		try {
			// the application is generated again with its languages
			application.updateSourceFiles();
		} catch (Throwable t) {
			Engine.logStudio.debug("(Translations) the application is not generated again", t);
		}
		response.put("done", true);
		response.put("texts", texts.size());
		response.put("files", files);
	}

	private static String language(String code) {
		return code != null && code.matches("[a-z]{2,3}") ? code : null;
	}

	/**
	 * @return the titles of the pages and the plain texts of an application and of the shared components it
	 *         uses, once each
	 */
	private static List<String> texts(ApplicationComponent application) throws Exception {
		var texts = new ArrayList<String>();
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				String text = null;
				if (databaseObject instanceof PageComponent page) {
					text = page.getTitle();
				} else if (databaseObject instanceof UIUseShared useShared) {
					var shared = useShared.getTargetSharedComponent();
					if (shared != null && !useShared.isRecursive()) {
						super.walk(shared);
					}
				} else if (databaseObject instanceof UIText uiText && Mode.PLAIN.equals(uiText.getTextSmartType().getMode())) {
					text = uiText.getTextSmartType().getValue();
				}
				if (text != null && !texts.contains(text)) {
					texts.add(text);
				}
				super.walk(databaseObject);
			}
		}.init(application);
		return texts;
	}
}
