/*
* Copyright (c) 2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/editors/completion/CtfCompletionProposalsComputer.java $
 * $Author: jmc $
 * $Revision: 37416 $
 * $Date: 2014-06-24 15:45:16 +0200 (Tue, 24 Jun 2014) $
 */

package com.sap.conn.jco;

import java.io.File;

public class FakeJarException extends RuntimeException {
	private static final long serialVersionUID = 2538809576678969198L;
	boolean alreadyPrint = false;

	public FakeJarException(Class<?> cl) {
		super(makeMessage(cl));
		setStackTrace(new StackTraceElement[0]);
	}
	
	private static String makeMessage(Class<?> cl) {
		String classPath = cl.getCanonicalName();
		String jarName = "sapjco3.jar";
		String howToGetIt = "you can download it for free at http://service.sap.com/connectors with your own SAP account.\n"
				+ "Then open \"SAP Java Connector\", \"Tools and Services\" and under \"Download SAP JCo Release 3.0\"\n"
				+ "you can download the zip file corresponding to the current JVM.\n"
				+ "The " + jarName + " (with its libsapjco3.so or sapjco3.dll depending of your OS) must be extracted from the zip file.";

		String message = "(" + classPath + ") This is not the true " + jarName
				+ ", due to license issue we cannot provide it.\n\n"
				+ "If you do not have an official " + jarName + ", "
				+ howToGetIt + "\n\n"
				+ "You have to stop CEMS, replace the placeholder " + jarName
				+ " with an official renamed jar (and .so or .dll) and start CEMS.\n\n";
		
		try {
			Class<?> engine = Class.forName("com.twinsoft.convertigo.engine.Engine");
			boolean isStudioMode = ((Boolean) engine.getMethod("isStudioMode", new Class[0]).invoke(null, new Object[0])).booleanValue();

			String path;

			if (isStudioMode)
				path = new File(engine.getField("WEBAPP_PATH").get(null) + "/../../../lib/jco/").getCanonicalPath();
			else {
				path = new File(engine.getField("WEBAPP_PATH").get(null) + "/WEB-INF/lib/").getCanonicalPath();
			}
			
			message = message + "This file should probably be in this folder : " + path;			
		} catch (Exception e) {
			message = message + "Sorry, we can not determine where your Convertigo instance is due to " + e.getClass().getName() + " : " + e.getMessage();
		}
		
		message = "============================================\n\n" + message + "\n\n============================================";
		System.err.println(message);
		
		return message;
	}

	@Override
	public String getMessage() {
		String msg = alreadyPrint ? "This is not the true sapjco3.jar, please install it (with its libsapjco3.so or sapjco3.dll depending of your OS)." : super.getMessage();
		alreadyPrint = true;
		return msg;
	}
}
