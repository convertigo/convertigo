package com.convertigo.jdbc;

import java.io.File;

public class CommonDriver {

	public static void init(String classPath, String jarName, String howToGetIt) {
		if (howToGetIt == null || howToGetIt == "") {
			howToGetIt = "ask to your database administrator to have one.";
		}
		
		String message = "(" + classPath + ") This is not the true " + jarName + ", due to license issue we cannot provide it.\n\n"
				+ "If you do not have an official " + jarName + ", " + howToGetIt + "\n\n"
				+ "You have to stop CEMS, replace the placeholder " + jarName + " with an official renamed jar and start CEMS.\n\n";
		try {
			Class<?> engine = Class.forName("com.twinsoft.convertigo.engine.Engine");
			boolean isStudioMode = (Boolean) engine.getMethod("isStudioMode").invoke(null);
			String path;
			if (isStudioMode) {
				path = new File("" + engine.getField("WEBAPP_PATH").get(null) + "/../../../lib/").getCanonicalPath();
			} else {
				path = new File("" + engine.getField("WEBAPP_PATH").get(null) + "/lib/").getCanonicalPath();
			}
			message += "This file should probably be in this folder : " + path;
			Object logger = engine.getField("logEngine").get(null);
			Class<?> c_logger = logger.getClass();
			c_logger.getMethod("error", Object.class).invoke(logger, message);
		} catch(Exception e) {
			message += "Sorry, we can not determine where your Convertigo instance is due to " + e.getClass().getName() + " : " + e.getMessage();
		}
		message = "============================================\n\n" + message
				+ "\n\n============================================";
		System.err.println(message);
		throw new FakeJarException(message);
	}
	
}
