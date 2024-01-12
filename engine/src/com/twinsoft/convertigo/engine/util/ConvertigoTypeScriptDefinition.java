/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.twinsoft.convertigo.engine.Engine;

public class ConvertigoTypeScriptDefinition {
	private SortedSet<Class<?>> classes = new TreeSet<>((c1, c2) -> c1.getName().compareTo(c2.getName()));
	private StringBuilder append = new StringBuilder();

	private void handleCls(Class<?> cls) {
		if (cls == null) {
			return;
		}

		while (cls.isArray()) {
			cls = cls.getComponentType();
		}

		if (cls.isPrimitive()) {
			return;
		}

		String name = cls.getName();
		if (name.matches("(jdk|sun|org\\.(mozilla|eclipse|apache\\.xerces|xml)|w3c\\.xml|"
				+ "java\\.lang\\.(module|ref|reflect)|java\\.security)\\..*")) {
			return;
		}

		if (classes.add(cls)) {
			handleCls(cls.getSuperclass());
			for (Field field: cls.getDeclaredFields()) {
				handleCls(field.getType());
			}
			for (Method method: cls.getDeclaredMethods()) {
				handleCls(method.getReturnType());
				for (Class<?> type: method.getParameterTypes()) {
					handleCls(type);
				}
			}
		}
	}

	private String convertType(Class<?> cls, Class<?> type) {
		String sType;
		if (type.equals(String.class) || type.equals(Character.class) || type.equals(char.class)) {
			sType = "string";
		} else if (type.isAssignableFrom(Number.class) || type.equals(int.class) || type.equals(long.class)
				|| type.equals(float.class) || type.equals(double.class) || type.equals(short.class)) {
			sType = "number";
		} else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
			sType = "boolean";
		} else if (classes.contains(type)) {
			sType = cls.getPackage().equals(type.getPackage()) ? type.getSimpleName() : type.getCanonicalName();
		} else {
			sType = "any";
		}
		if (type.isArray()) {
			sType = "Array<" + sType + ">";
		}
		return sType;
	}

	private String getCode(String prepend, Class<?> cls) {
		String nPrepend = prepend + "  ";
		StringBuilder sb = new StringBuilder();
		for (Field field: cls.getFields()) {
			int mod = field.getModifiers();
			if (field.getDeclaringClass().equals(cls) && Modifier.isPublic(mod)) {
				sb.append(nPrepend);
				if (Modifier.isStatic(mod)) {
					sb.append("static ");
				}
				sb.append(field.getName()).append(": ").append(convertType(cls, field.getType())).append("\n");
			}
		}
		for (Method method: cls.getMethods()) {
			int mod = method.getModifiers();
			if (method.getDeclaringClass().equals(cls) && Modifier.isPublic(mod)) {
				sb.append(nPrepend);
				if (Modifier.isStatic(mod)) {
					sb.append("static ");
				}
				sb.append(method.getName()).append("(");
				boolean comma = false;
				for (Parameter parameter: method.getParameters()) {
					if (comma) {
						sb.append(", ");
					} else {
						comma = true;
					}
					sb.append(parameter.getName()).append(": ").append(convertType(cls, parameter.getType()));
				}
				sb.append(")");
				if (!method.getReturnType().equals(Void.TYPE)) {
					sb.append(": ").append(convertType(cls, method.getReturnType()));
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private String getCode(String exPkg, String prepend, Queue<Class<?>> classes) {
		String nPrepend = prepend + "  ";
		StringBuilder sb = new StringBuilder();
		if (!exPkg.isEmpty()) {
			exPkg += ".";
		}
		Class<?> cls;
		while ((cls = classes.peek()) != null) {
			String pkg = getPackageName(cls) + ".";
			if (pkg.equals(exPkg)) {
				String code = getCode(nPrepend, cls);
				if (StringUtils.isNotBlank(code)) {
					sb.append(prepend).append("declare class " + cls.getSimpleName());
					Class<?> scls = cls.getSuperclass();
					if (scls != null && !scls.equals(Object.class) && !scls.equals(Enum.class)) {
						sb.append(" extends ");
						if (!scls.getPackage().equals(cls.getPackage())) {
							sb.append(scls.getCanonicalName());
						} else {
							sb.append(scls.getSimpleName());
						}
					}
					sb.append(" {\n").append(code);
					sb.append(prepend).append("}\n");
				}
				classes.poll();
			} else if (pkg.startsWith(exPkg)) {
				String namespace = pkg.substring(exPkg.length(), pkg.indexOf('.', exPkg.length()));
				String nPkg = pkg.substring(0, pkg.indexOf('.', exPkg.length()));
				String code = getCode(nPkg, nPrepend, classes);
				if (StringUtils.isNotBlank(code)) {
					sb.append(prepend).append("declare namespace ").append(namespace).append(" {\n");
					sb.append(code);
					sb.append(prepend).append("}\n");
				}
			} else {
				break;
			}
		}
		return sb.toString();
	}

	private String getCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getCode("", "", new LinkedList<>(classes)));
		return sb.append(append).toString();
	}

	private static File[] file = new File[]{null};
	public static File getDeclarationFile() {
		synchronized (file) {
			if (file[0] == null) {
				long ts = System.currentTimeMillis();
				ConvertigoTypeScriptDefinition tsd = new ConvertigoTypeScriptDefinition();
				tsd.handleCls(com.twinsoft.convertigo.engine.Engine.class);
				tsd.handleCls(com.twinsoft.convertigo.engine.Context.class);
				tsd.handleCls(com.twinsoft.convertigo.engine.util.LogWrapper.class);
				tsd.handleCls(com.twinsoft.convertigo.engine.util.Crypto2.class);
				tsd.handleCls(com.twinsoft.convertigo.engine.util.XMLUtils.class);
				tsd.handleCls(com.twinsoft.convertigo.engine.util.FileUtils.class);
				tsd.handleCls(java.lang.System.class);
				tsd.handleCls(java.math.BigInteger.class);
				tsd.handleCls(java.security.Signature.class);
				tsd.handleCls(java.security.spec.RSAPublicKeySpec.class);
				tsd.handleCls(org.apache.commons.lang3.StringUtils.class);
				tsd.handleCls(org.apache.commons.codec.binary.Base64.class);
				tsd.handleCls(org.apache.commons.codec.binary.Hex.class);
				tsd.handleCls(org.apache.commons.codec.binary.StringUtils.class);
				tsd.handleCls(org.w3c.dom.Document.class);

				tsd.append.append("declare var context: com.twinsoft.convertigo.engine.Context\n")
				.append("declare var log: com.twinsoft.convertigo.engine.util.LogWrapper\n")
				.append("declare var dom: org.w3c.dom.Document\n")
				.append("declare function use(cls: string): any\n")
				.append("declare function include(path: string): any\n");
				String code = tsd.getCode();
				ts = System.currentTimeMillis() - ts;
				Engine.logStudio.info("(ConvertigoTypeScriptDefinition) Handle " + tsd.classes.size() + " classes in " + ts  + " ms.");
				file[0] = new File(Engine.USER_WORKSPACE_PATH, "studio/convertigo.d.ts");
				try {
					FileUtils.write(file[0], code, StandardCharsets.UTF_8);
				} catch (IOException e) {
					Engine.logStudio.error("(ConvertigoTypeScriptDefinition) Failed to write: " + file[0], e);
				}
			}
		}
		return file[0];
	}

	static private String getPackageName(Class<?> c) {
		String pn;
		while (c.isArray()) {
			c = c.getComponentType();
		}
		if (c.isPrimitive()) {
			pn = "java.lang";
		} else {
			String cn = c.getName();
			int dot = cn.lastIndexOf('.');
			pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
		}
		return pn;
	}

	public static void main(String[] args) throws IOException {
		//		System.out.println("Handle " + tsd.classes.size() + " classes.");
		//		System.out.println(tsd.getCode());
		//		for (Class<?> cls: tsd.classes) {
		//			System.out.println(cls.getCanonicalName());
		//		}
	}

}
