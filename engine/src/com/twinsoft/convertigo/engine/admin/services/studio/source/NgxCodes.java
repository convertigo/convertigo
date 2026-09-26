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

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICustom;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UIFont;
import com.twinsoft.convertigo.beans.ngx.components.UIFontStyle;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

/**
 * The code of an NGX component the Studio edits as the Eclipse Studio opens it on a double-click: the
 * TypeScript function of a custom action between its markers, the HTML template of a custom component,
 * the SCSS of a style in the rule of its element.
 */
public class NgxCodes {
	private static final String ACTION = "#action";
	private static final String HTML = "#html";
	private static final String STYLE = "#style";

	private NgxCodes() {
	}

	/**
	 * @return the suffix of the code document of the object, or null when it has no code to edit
	 */
	public static String suffix(DatabaseObject dbo) {
		if (dbo instanceof UICustomAction) {
			return ACTION;
		}
		if (dbo instanceof UICustom) {
			return HTML;
		}
		if (dbo instanceof UIStyle && !(dbo instanceof UIFont) && !(dbo instanceof UIFontStyle)) {
			return STYLE;
		}
		return null;
	}

	static boolean isCodeId(String id) {
		return id != null && (id.endsWith(ACTION) || id.endsWith(HTML) || id.endsWith(STYLE));
	}

	static DatabaseObject target(String id) throws Exception {
		var hash = id.lastIndexOf('#');
		var dbo = Utils.getDbo(id.substring(0, hash));
		if (!id.substring(hash).equals(suffix(dbo))) {
			throw new EngineException("The object " + id + " has no such code.");
		}
		return dbo;
	}

	static String language(DatabaseObject dbo) {
		return dbo instanceof UICustomAction ? "typescript" : dbo instanceof UICustom ? "html" : "scss";
	}

	static String fileName(DatabaseObject dbo) {
		return dbo.getName() + (dbo instanceof UICustomAction ? ".ts" : dbo instanceof UICustom ? ".html" : ".scss");
	}

	private static String marker(UICustomAction action) {
		return "function:" + action.getActionName();
	}

	/**
	 * @return the code to edit: the generated file of the function of an action, the template of a custom
	 *         component, the style in the rule of its element
	 */
	static String read(DatabaseObject dbo) throws Exception {
		if (dbo instanceof UICustomAction action) {
			if (action.getMainScriptComponent() == null) {
				throw new EngineException("The action " + action.getName() + " is not in a page or an application.");
			}
			var builder = action.getProject().getMobileBuilder();
			builder.writeFunctionTempTsFile(action, marker(action));
			var file = new File(action.getProject().getDirPath(), builder.getFunctionTempTsRelativePath(action));
			return Files.readString(file.toPath(), StandardCharsets.UTF_8);
		}
		if (dbo instanceof UICustom custom) {
			return custom.getCustomTemplate();
		}
		var style = (UIStyle) dbo;
		var content = style.getStyleContent().getString();
		var rule = rule(style);
		if (!rule.isEmpty()) {
			content = String.format(rule + (content.isEmpty() ? " {%n%s%n}" : " {%n%s}"), content);
		}
		return content;
	}

	private static String rule(UIStyle style) {
		var parent = style.getParent();
		if (style instanceof UIFont && parent instanceof ApplicationComponent) {
			return "html";
		}
		if (style instanceof UIFont && parent instanceof UISharedComponent shared) {
			return shared.getSelector();
		}
		return parent instanceof UIElement element ? "." + element.getTagClass() : "";
	}

	/**
	 * @return the revision of the code kept in the object, which changes when the code changes
	 */
	static String revision(DatabaseObject dbo) throws Exception {
		String code;
		if (dbo instanceof UICustomAction action) {
			code = action.getActionValue().getString();
		} else if (dbo instanceof UICustom custom) {
			code = custom.getCustomTemplate();
		} else {
			code = ((UIStyle) dbo).getStyleContent().getString();
		}
		return Get.sha256(code == null ? "" : code);
	}

	/**
	 * Keeps in the object the code edited, as the editors of the Eclipse Studio do.
	 */
	static void save(DatabaseObject dbo, String content) throws Exception {
		if (dbo instanceof UICustomAction action) {
			var old = action.getActionValue();
			var marker = MobileBuilder.getMarker(content, marker(action));
			if (marker.isEmpty()) {
				throw new EngineException("The markers of the function of " + action.getName() + " are missing.");
			}
			var code = new FormatedContent(MobileBuilder.getFormatedContent(marker, marker(action)));
			action.setActionValue(code);
			changed(action, "actionValue", old, code);
		} else if (dbo instanceof UICustom custom) {
			var old = custom.getCustomTemplate();
			custom.setCustomTemplate(content);
			changed(custom, "htmlTemplate", old, content);
		} else {
			var style = (UIStyle) dbo;
			var code = content;
			if (style.getParent() instanceof UIElement) {
				// the rule of the element is generated
				code = code.replaceFirst("^\\.class\\d+\\s?\\{\\r?\\n?", "");
				var end = code.lastIndexOf('}');
				code = end < 0 ? code : code.substring(0, end);
			}
			var old = style.getStyleContent();
			var formated = new FormatedContent(code);
			style.setStyleContent(formated);
			changed(style, "styleContent", old, formated);
		}
	}

	private static void changed(DatabaseObject dbo, String property, Object old, Object value) throws Exception {
		dbo.hasChanged = true;
		BuilderUtils.dboChanged(dbo, property, old, value);
	}
}
