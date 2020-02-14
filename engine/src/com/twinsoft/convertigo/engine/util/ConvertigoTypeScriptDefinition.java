package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class ConvertigoTypeScriptDefinition {
	SortedSet<Class<?>> classes = new TreeSet<>((c1, c2) -> c1.getName().compareTo(c2.getName()));
	StringBuilder append = new StringBuilder();
	
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
			String pkg = cls.getPackageName() + ".";
			if (pkg.equals(exPkg)) {
				String code = getCode(nPrepend, cls);
				if (!code.isBlank()) {
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
				if (!code.isBlank()) {
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
				tsd.handleCls(Context.class);
				tsd.handleCls(LogWrapper.class);
				tsd.handleCls(StringUtils.class);
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
					FileUtils.write(file[0], code, "UTF-8");
				} catch (IOException e) {
					Engine.logStudio.error("(ConvertigoTypeScriptDefinition) Failed to write: " + file[0], e);
				}
			}
		}
		return file[0];
	}
	
	public static void main(String[] args) throws IOException {
//		System.out.println("Handle " + tsd.classes.size() + " classes.");
//		System.out.println(tsd.getCode());
//		for (Class<?> cls: tsd.classes) {
//			System.out.println(cls.getCanonicalName());
//		}
	}

}
