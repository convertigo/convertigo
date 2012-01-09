/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class GenerateJavaStubDialog extends MyAbstractDialog implements Runnable {

	public final static int WEB_SERVICE_JAVA_CLIENT_STUB = 0;
	public final static int EJB_STUB = 1;

	private int stubType;
	
	// Paths variables
	private String projectName;
	private String axis_path;
	private String stub_path;
	
	private String wsjc_path;
	private String wsjc_src_path;
	private String wsjc_classes_path;

	private String ejb_path;
	private String server_path;
	private String client_path;
	private String server_src_path;
	private String server_src_twinsoft_path;
	private String server_src_axis_path;
	private String client_src_path;
	private String client_src_twinsoft_path;
	private String server_classes_path;
	private String client_classes_path;
	private String server_META_INF_path;

	private String convertigo_jar_path;
	
	private String wsdl_path;
	
	private ProgressBar progressBar = null;
	private Label labelProgression = null;
	
	public GenerateJavaStubDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, int stubType) {
		super(parentShell, dialogAreaClass, dialogTitle);
		this.stubType = stubType;
	}

	@Override
	protected void okPressed() {
		try {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
			progressBar = ((GenerateJavaStubDialogComposite)dialogComposite).progressBar;
			labelProgression = ((GenerateJavaStubDialogComposite)dialogComposite).labelProgression;

			Thread thread = new Thread(this);
			thread.start();
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to generate stub!");
		}
		finally {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
		}
	}

	public void run() {
		final Display display = getParentShell().getDisplay();
		Thread progressBarThread = new Thread("Progress Bar thread") {
			@Override
			public void run() {
				int i = 0;
				while (true) {
					try {
						i += 5;
						if (i >= 100) i = 0;
						final int j = i;
						display.asyncExec(new Runnable() {
							public void run() {
								progressBar.setSelection(j);
							}
						});
						
						sleep(500);
					}
					catch(InterruptedException e) {
						break;
					}
				}
			}
		};

		//Studio.theApp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		try {
			progressBarThread.start();
			generateStub();
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to generate the stub");
		}
		finally {
			//Studio.theApp.setCursor(Cursor.getDefaultCursor());

			try {
				progressBarThread.interrupt();
				
				display.asyncExec(new Runnable() {
					public void run() {
						setReturnCode(OK);
						close();
					}
				});
				
			}
			catch (Throwable e) {
				ConvertigoPlugin.logException(e, "Unable to generate the stub");
			}
		}
	}

	public void setTextLabel(String text) {
		final Display display = getParentShell().getDisplay();
		final String labelText = text;
		display.asyncExec(new Runnable() {
			public void run() {
				labelProgression.setText(labelText);
			}
		});
	}
	
	public void generateStub() {
		//Studio.theApp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		projectName = ConvertigoPlugin.projectManager.currentProject.getName().substring(0, 1).toUpperCase() + ConvertigoPlugin.projectManager.currentProject.getName().substring(1);
		axis_path = "/com/twinsoft/convertigo/projects/" + ConvertigoPlugin.projectManager.currentProject.getName();
		stub_path = Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_lib";
		convertigo_jar_path = Engine.WEBAPP_PATH + "/../../../lib";

		try {
			if (stubType == WEB_SERVICE_JAVA_CLIENT_STUB) {
				generateWebServiceJavaClient();
			}
			else {
				generateEJB();
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to generate the stub");
		}
		finally {
			//Studio.theApp.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private void deleteFile(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0 ; i < files.length ; i++) {
				deleteFile(files[i]);
			}
		}
		file.delete();
	}

	public void generateEJB() {
		// EJB paths
		ejb_path = stub_path + "/EJB";
		server_path = ejb_path + "/serverEJB";
		client_path = ejb_path + "/clientEJB";

		server_src_path = server_path + "/src";
		server_src_twinsoft_path = server_src_path + "/com/twinsoft/convertigo/ejb";
		server_src_axis_path = server_src_path + axis_path;

		client_src_path = client_path + "/src";
		client_src_twinsoft_path = client_src_path + "/com/twinsoft/convertigo/ejb";

		server_classes_path = server_path + "/classes";
		client_classes_path = client_path + "/classes";

		server_META_INF_path = server_classes_path + "/META-INF";

		wsdl_path = server_path + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + ".wsdl";
        
		try {
			File f;            

			f = new File(ejb_path);
			deleteFile(f);

			// Creation of directories
			setTextLabel("Directories creation");
			f = new File(server_src_twinsoft_path); f.mkdirs();
			f = new File(server_src_axis_path); f.mkdirs();
			f = new File(server_META_INF_path); f.mkdirs();
            
			f = new File(client_src_twinsoft_path); f.mkdirs();
            
			f = new File(server_classes_path); f.mkdirs();
			f = new File(client_classes_path); f.mkdirs();
            
			// creation of the wsdl File
			setTextLabel("Generating the WSDL document");
			String wsdl = generateWsdl();
			BufferedWriter bw = new BufferedWriter(new FileWriter(wsdl_path));
			bw.write(wsdl);
			bw.close();

			// Creation of the Java Files
			setTextLabel("Generating the Java files");
			if(createJavaFiles() == 1)
				return;
            
			// Creation of the META-INF Files
			if (createMeta_InfFiles() == 1)
				return;
            
			// Compile
			setTextLabel("Compiling the Java files");
			if (compile() == 1)
				return;
            
			// creation of file de test testEJB.jsp
			setTextLabel("Generating the EJB file");
			String testEJB = generateFile(wsdl_path, "ejb/testEJB.jsp.xsl");
			bw = new BufferedWriter(new FileWriter(client_path + "/testEJB.jsp"));
			bw.write(testEJB.substring(44));
			bw.close();
            
			// Creation of jar files
			String serverArchiveFilename = server_path + "/" + projectName + "EJB.jar";
			String jarFiles[][] = {
				{convertigo_jar_path + "/axis.jar"},
				{convertigo_jar_path + "/commons-discovery.jar"},
				{convertigo_jar_path + "/commons-logging.jar"},
				{convertigo_jar_path + "/jaxrpc.jar"},
				{convertigo_jar_path + "/saaj-api.jar"}
			};
			ZipUtils.makeZip(
				serverArchiveFilename, 
				server_classes_path,
				null,
				jarFiles);

			String clientArchiveFilename = client_path + "/Client" + projectName + "EJB.war";
			String jspFile[][] = {
				{client_path + "/testEJB.jsp", "testEJB.jsp"}
			};
			setTextLabel("Generating the WAR");
			ZipUtils.makeZip(
				clientArchiveFilename, 
				client_classes_path, 
				"WEB-INF/classes", 
				jspFile);
			setTextLabel("Generating the JAR file");
			clientArchiveFilename = client_path + "/Client" + projectName + "EJB.jar";
			ZipUtils.makeZip(clientArchiveFilename, client_classes_path, null);

			setTextLabel("Job finished");
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to generate the stub");
		}
		finally {
			//Studio.theApp.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void generateWebServiceJavaClient() {
		// Web Service Java client paths
		wsjc_path = stub_path + "/WebServiceJavaClient";
		wsjc_src_path = wsjc_path + "/src";
		wsjc_classes_path = wsjc_path + "/classes";

		wsdl_path = wsjc_path + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + ".wsdl";
        
		try {
			File f;            

			f = new File(wsjc_path);
			deleteFile(f);

			// Creation of directories
			setTextLabel("Directories creation");
			f = new File(wsjc_src_path); f.mkdirs();
			f = new File(wsjc_classes_path); f.mkdirs();
            
			// Creation of the WSDL file
			setTextLabel("Generating the WSDL document");
			String wsdl = generateWsdl();
			BufferedWriter bw = new BufferedWriter(new FileWriter(wsdl_path));
			bw.write(wsdl);
			bw.close();

			// Creation of the Java Files
			setTextLabel("Generating the Java files");
			createAxisStub(wsjc_src_path);
            
			// Compile
			setTextLabel("Compiling the Java files");
			compile(wsjc_src_path + axis_path, wsjc_classes_path);
            
//			// creation of test class Test.java
//			jsetTextLabel(java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/GenerateJavaStubDialog").getString("label.progression.ejb_generation"));
//			String test = generateFile(wsdl_path, "/wsjc/Test.jsp.xsl");
//			bw = new BufferedWriter(new FileWriter(wsjc_path + "/Test.java"));
//			bw.write(test.substring(44));
//			bw.close();
            
			// Creation of the Java archive
			String serverArchiveFilename = wsjc_path + "/WebServiceClient_" + projectName + ".jar";
			String jarFiles[][] = {
				{convertigo_jar_path + "/axis.jar"},
				{convertigo_jar_path + "/commons-discovery.jar"},
				{convertigo_jar_path + "/commons-logging.jar"},
				{convertigo_jar_path + "/jaxrpc.jar"},
				{convertigo_jar_path + "/saaj-api.jar"}
			};
			ZipUtils.makeZip(
				serverArchiveFilename, 
				wsjc_classes_path,
				null,
				jarFiles);

			setTextLabel("Job finished");
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to generate the stub");
		}
		finally {
			//Studio.theApp.setCursor(Cursor.getDefaultCursor());
		}
	}

	private String generateFile(String wsdl_path, String template) throws Exception {
		//System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		//TransformerFactory tFactory = TransformerFactory.newInstance();
		TransformerFactory tFactory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
		StreamSource streamSource = new StreamSource(new File(Engine.TEMPLATES_PATH + "/" + template).toURI().toASCIIString());
		Transformer transformer = tFactory.newTransformer(streamSource);
		StringWriter sw = new StringWriter();
		transformer.transform(new StreamSource(new File(wsdl_path).toURI().toASCIIString()), new StreamResult(sw));
		String xsl = sw.toString();
		return xsl;
	}
    
	protected String generateWsdl() throws EngineException {
		Project currentProject = ConvertigoPlugin.projectManager.currentProject;
		String projectName = currentProject.getName();
		String servletURI = "http://localhost/convertigo/projects/" + projectName + "/" + projectName + ".ws";

		return WebServiceServlet.generateWsdl(true, servletURI, currentProject);
	}

	private void createAxisStub(String path) throws Exception {
		// Creation of the Java classes with Axis
		/*String[] args = new String[] { "--output", path, wsdl_path };
		MyWSDL2Java wsdl2java = new MyWSDL2Java();
		wsdl2java.run2(args);*/
	}

	protected int createJavaFiles() {
		String[] files = {
			"librairies axis",
			"EJBBean.java",
			"EJBUtil.java",
			"EJBLocal.java",
			"EJBLocalHome.java",
			"EJB.java",
			"EJBHome.java",
			"EJBSession.java"
		};
		int i = 0;
		try {
			createAxisStub(server_src_path);
			
			// creation of Java files for ejb
			String javaFile = "";
			BufferedWriter bw;
			String projectNameFilePrefix = Character.toUpperCase(projectName.charAt(0)) + projectName.substring(1);
			
			i++;
			// EJBBean file
			javaFile = generateFile(wsdl_path, "ejb/" + files[i] + ".xsl");
			bw = new BufferedWriter(new FileWriter(server_src_twinsoft_path + "/" + projectNameFilePrefix + files[i]));
			bw.write(javaFile.substring(44));
			bw.close();

			i++;
			// EJBUtil file
			javaFile = generateFile(wsdl_path, "ejb/" + files[i] + ".xsl");
			bw = new BufferedWriter(new FileWriter(client_src_twinsoft_path + "/" + projectNameFilePrefix + files[i]));
			bw.write(javaFile.substring(44));
			bw.close();
            
			for(i++ ; i < files.length ; i++){
				javaFile = generateFile(wsdl_path, "ejb/" + files[i] + ".xsl");
				bw = new BufferedWriter(new FileWriter(client_src_twinsoft_path + "/" + projectNameFilePrefix + files[i]));
				bw.write(javaFile.substring(44));
				bw.close();
				bw = new BufferedWriter(new FileWriter(server_src_twinsoft_path + "/" + projectNameFilePrefix + files[i]));
				bw.write(javaFile.substring(44));
				bw.close();
			}
			return 0;
		}
		catch(Exception e){
			ConvertigoPlugin.logException(e, "Unable to generate the Java file" + " " + files[i]);
			return 1;
		}
	}
    
    
	protected int createMeta_InfFiles(){
		String[] files = {
			"ejb-jar.xml",
			"jboss.xml",
			"jonas-ejb-jar.xml",
			"orion-ejb-jar.xml",
			"weblogic-ejb-jar.xml",
			"jrun-ejb-jar.xml"
		};
		int i = 0;
		String meta_infFile;
		BufferedWriter bw;
		try{            
			for (i = 0; i < files.length ; i++){
				meta_infFile = generateFile(wsdl_path, "ejb/" + files[i] + ".xsl");
				bw = new BufferedWriter(new FileWriter(server_META_INF_path + "/" + files[i]));
				bw.write(meta_infFile);
				bw.close();
			}                
			return 0;
		}
		catch(Exception e){
			ConvertigoPlugin.logException(e, "Unable to generate the META-INF file" + " " + files[i]);
			return 1;
		}
	}
    
	private class CompileException extends Exception {
		private static final long serialVersionUID = -3086754149374743434L;
		public CompileException() {
				super();
		}
	}
	
	protected int compile(){
		File javaDir;
		String[] javaFiles;
		String[] params = {"-classpath", "", "-d", server_classes_path, "-sourcepath", server_src_path, "-source", "1.4", ""};
		try{
			// Compilation of server classes (package for axis);  
			javaDir = new File(server_src_axis_path);
			javaFiles = javaDir.list();       
			params[1] = 
				convertigo_jar_path + "/axis.jar" + File.pathSeparatorChar + 
				convertigo_jar_path + "/jaxrpc.jar";
			for (int i = 0 ; i < javaFiles.length ; i++){
				params[8] = server_src_axis_path + "/" + javaFiles[i];
				if (runJavac(params) != 0)
					throw new CompileException();
				if (javaFiles[i].endsWith("Response.java")){
					params[3] = client_classes_path;
					if (runJavac(params) != 0)
						throw new CompileException();
					params[3] = server_classes_path;
				}
			}

			// Compilation of server classes (package com.twinsoft.convertigo.ejb)
			javaDir = new File(server_src_twinsoft_path);           
			javaFiles = javaDir.list();
			params[1] = 
				server_classes_path + File.pathSeparatorChar +
				convertigo_jar_path + "/ejb.jar" + File.pathSeparatorChar +
				convertigo_jar_path + "/axis.jar" + File.pathSeparatorChar +
				convertigo_jar_path + "/jaxrpc.jar";
			for (int i = 0 ; i < javaFiles.length ; i++){
				params[8] = server_src_twinsoft_path + "/" + javaFiles[i];
				if (runJavac(params) != 0)
					throw new CompileException();
			}

			// Compilation of client classes (package com.twinsoft.convertigo.ejb)
			javaDir = new File(client_src_twinsoft_path);
			javaFiles = javaDir.list(); 
			params[1]=
				server_classes_path + File.pathSeparatorChar +
				convertigo_jar_path + "/ejb.jar" + File.pathSeparatorChar +
				convertigo_jar_path + "/axis.jar" + File.pathSeparatorChar +
				convertigo_jar_path + "/jaxrpc.jar";
			params[3] = client_classes_path;
			params[5] = client_src_path;
			for (int i = 0 ; i < javaFiles.length ; i++){
				params[8] = client_src_twinsoft_path + "/" + javaFiles[i];
				if (runJavac(params) != 0)
					throw new CompileException();
			}
			
			return 0;
		}
		catch(CompileException e){
			ConvertigoPlugin.logException(e, "Please look at the stdout log panel for more informations.\nUnable to compile the file" + " " + params[8] + ".");
			return 1;
		}
		catch(Exception e){
			ConvertigoPlugin.logException(e, "Unable to compile the file" + " " + params[8]);
			return 1;
		}
	}
	
	private int runJavac(String[] params) /*throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException*/ {
		/*Class javacClass = Class.forName("com.sun.tools.javac.Main");
		Method compileMethod = javacClass.getMethod("compile",
				new Class[] { String[].class });
		Object result = compileMethod.invoke(null, params);
		return ((Integer) result).intValue();*/
		return com.sun.tools.javac.Main.compile(params);
	}

	protected void compile(String sourcePath, String classPath) throws CompileException {
		File javaDir;
		String[] javaFiles;
		String[] params;
		try {
			// Compilation of classes  
			javaDir = new File(sourcePath);
			javaFiles = javaDir.list();       
			
			params = new String[8 + javaFiles.length];
			params[0] = "-sourcepath";
			params[1] = sourcePath;
			params[2] = "-classpath"; 
			params[3] = convertigo_jar_path + "/axis.jar" + File.pathSeparatorChar + convertigo_jar_path + "/jaxrpc.jar";
			params[4] = "-d";
			params[5] = classPath;
			params[6] = "-source";
			params[7] = "1.4";
			
			for (int i = 0 ; i < javaFiles.length ; i++){
				params[8 + i] = sourcePath + "/" + javaFiles[i];
			}
			
			if (runJavac(params) != 0)
				throw new CompileException();
		}
		catch(CompileException e){
			ConvertigoPlugin.logException(e, "Please look at the stdout log panel for more informations.\nUnable to compile the file.");
		}
		catch(Exception e){
			ConvertigoPlugin.logException(e, "Unable to compile the file.");
		}
	}
	
}
