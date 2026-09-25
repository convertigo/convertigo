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

import java.beans.Introspector;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.core.ISharedComponent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

/**
 * The TypeScript class of an NGX application, page or shared component, which the Studio edits as the
 * "Edit class" action of the Eclipse Studio: the class is generated, and the code written between its
 * markers is kept in the component.
 */
public class NgxClasses {
	private static final String SUFFIX = "#class";

	private NgxClasses() {
	}

	/**
	 * @return the id of the class document of an object of the tree
	 */
	public static String classId(String id) {
		return id + SUFFIX;
	}

	static boolean isClassId(String id) {
		return id != null && id.endsWith(SUFFIX);
	}

	/**
	 * @return whether the object has a TypeScript class to edit
	 */
	public static boolean handles(DatabaseObject dbo) {
		return dbo instanceof ApplicationComponent || dbo instanceof PageComponent || dbo instanceof ISharedComponent;
	}

	static DatabaseObject target(String id) throws Exception {
		var dbo = Utils.getDbo(id.substring(0, id.length() - SUFFIX.length()));
		if (!handles(dbo)) {
			throw new EngineException("The object " + id + " has no TypeScript class.");
		}
		return dbo;
	}

	/**
	 * @return the generated class of the object
	 */
	static File generate(DatabaseObject dbo) throws Exception {
		var builder = dbo.getProject().getMobileBuilder();
		String path;
		if (dbo instanceof ApplicationComponent application) {
			builder.writeAppComponentTempTs(application);
			path = builder.getTempTsRelativePath(application);
		} else if (dbo instanceof PageComponent page) {
			builder.writePageTempTs(page);
			path = builder.getTempTsRelativePath(page);
		} else {
			builder.writeCompTempTs((ISharedComponent) dbo);
			path = builder.getTempTsRelativePath((ISharedComponent) dbo);
		}
		return new File(dbo.getProject().getDirPath(), path);
	}

	/**
	 * @return the class of the object, with the code of the object between its markers: the built class
	 *         the builder generates again later can be older
	 */
	static String read(DatabaseObject dbo, File file) throws Exception {
		var content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		var code = code(dbo).getString();
		var matcher = Pattern.compile("/\\*Begin_c8o_(.+?)\\*/").matcher(code);
		while (matcher.find()) {
			var begin = "/*Begin_c8o_" + matcher.group(1) + "*/";
			var end = "/*End_c8o_" + matcher.group(1) + "*/";
			var block = block(code, begin, end);
			var current = block(content, begin, end);
			if (block != null && current != null) {
				content = content.replace(current, block);
			}
		}
		return content;
	}

	/**
	 * @return the text from a begin marker to its end marker, or null
	 */
	private static String block(String text, String begin, String end) {
		var from = text.indexOf(begin);
		var to = from < 0 ? -1 : text.indexOf(end, from);
		return to < 0 ? null : text.substring(from, to + end.length());
	}

	private static String property(DatabaseObject dbo) {
		return dbo instanceof ApplicationComponent ? "componentScriptContent" : "scriptContent";
	}

	/**
	 * @return the revision of the code of the object, which changes when the code changes
	 */
	static String revision(DatabaseObject dbo) throws Exception {
		return Get.sha256(code(dbo).getString());
	}

	private static FormatedContent code(DatabaseObject dbo) throws Exception {
		for (var pd : Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
			if (pd.getName().equals(property(dbo)) && pd.getReadMethod() != null) {
				return (FormatedContent) pd.getReadMethod().invoke(dbo);
			}
		}
		throw new EngineException("The object " + dbo.getName() + " has no TypeScript code.");
	}

	/**
	 * Keeps in the object the code the class holds between its markers.
	 */
	static void save(DatabaseObject dbo, String content) throws Exception {
		var code = new FormatedContent(MobileBuilder.getMarkers(content));
		for (var pd : Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
			if (pd.getName().equals(property(dbo)) && pd.getWriteMethod() != null) {
				var old = pd.getReadMethod().invoke(dbo);
				pd.getWriteMethod().invoke(dbo, code);
				dbo.hasChanged = true;
				BuilderUtils.dboChanged(dbo, property(dbo), old, code);
				return;
			}
		}
		throw new EngineException("The object " + dbo.getName() + " has no TypeScript code.");
	}
}
