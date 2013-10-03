package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobiledevices.BlackBerry6;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class MobileResourceHelper {
	
	private static final Pattern alphaNumPattern = Pattern.compile("[\\.\\w]*");
	
	public static String makeZipPackage(HttpServletRequest request) throws Exception {
		String originalMobileResourcesPath;
		String tmpMobileResourcesPath;
		String tmpMobileWwwPath;
		
		String application = request.getParameter("application");
		String applicationID = request.getParameter("applicationID");
		String endPoint = request.getParameter("endpoint");

		if (!endPoint.endsWith("/")) {
			endPoint += "/";
		}
		
		// Checking the project exists
		if (!Engine.theApp.databaseObjectsManager.existsProject(application)) {
			throw new ServiceException("Unable to build application '" + application
					+ "'; reason: the project does not exist");
		}
	
		originalMobileResourcesPath = Engine.PROJECTS_PATH + "/" + application + "/"
				+ MobileDevice.RESOURCES_PATH;
		tmpMobileResourcesPath = Engine.PROJECTS_PATH + "/" + application + "/_private/mobile";
		tmpMobileWwwPath = tmpMobileResourcesPath + "/www";	
		
		
		// Check forbidden characters in application ID (a-zA-Z0-9.-)
		if (!Pattern.matches("[\\.\\w]*", applicationID)) {
			throw new ServiceException("The application ID is not valid: '" + applicationID
					+ "'.\nThe only valid characters are upper and lower letters (A-Z, a-z), "
					+ "digits (0-9), a period (.) and a hyphen (-).");
		}
	
		// Make copy of mobile resources and remove undesired OS and SVN
		// files/directories		
		FileUtils.deleteDirectory(new File(tmpMobileResourcesPath));
		FileUtils.copyDirectory(new File(originalMobileResourcesPath), new File(tmpMobileWwwPath),
				new FileFilter() {
					public boolean accept(File pathname) {
						String filename = pathname.getName();
						return !filename.equals(".DS_Store") && !filename.equalsIgnoreCase("thumbs.db")
								&& !filename.equals(".svn");
					}
				});
	
		// Check forbidden characters in resources file names (only alphanumeric
		// chars) if a BlackBerry build is required
		Project project = Engine.theApp.databaseObjectsManager.getProjectByName(application);
		List<MobileDevice> mobileDevices = project.getMobileDeviceList();
		for (MobileDevice mobileDevice : mobileDevices) {
			if (mobileDevice instanceof BlackBerry6) {
				checkAlphaNumericCharsInFileNames(new File(tmpMobileWwwPath), tmpMobileWwwPath, originalMobileResourcesPath);
				break;
			}
		}
		
		File indexHtmlFile = new File(tmpMobileWwwPath + "/index.html");
		String sIndexHtml = FileUtils.readFileToString(indexHtmlFile);
		
		// Update endpoint for C8O server
		File serverJsFile = new File(tmpMobileWwwPath + "/sources/server.js");
		if (serverJsFile.exists()) {
			// Sencha based projects
			
			String sServerJsHtml = FileUtils.readFileToString(serverJsFile);
			sServerJsHtml = sServerJsHtml.replaceAll("/\\* DO NOT REMOVE THIS LINE endpoint \\: '' \\*/", "endpoint : '" + endPoint + "'");
			FileUtils.writeStringToFile(serverJsFile, sServerJsHtml);
			
			// Update sencha script reference (to non debug version)
			
			sIndexHtml = sIndexHtml.replaceAll("js/senchatouchdebugwcomments\\.js", "js/senchatouch.js");
			// "js/mobilelib.js" "../../../../scripts/mobilelib.js"
			File debugSenchaJsLibFile = new File(tmpMobileWwwPath + "/js/senchatouchdebugwcomments.js");
			debugSenchaJsLibFile.delete();
			
			// Update mobilelib script reference and copy from commom scripts if
			// needed
			sIndexHtml = resolveFile(sIndexHtml, "scripts/mobilelib.js", "js/mobilelib.js", tmpMobileWwwPath);
		} else {
			// jquerymobile based projects
			
			StringBuffer sbIndexHtml = new StringBuffer();
			BufferedReader br = new BufferedReader(new StringReader(sIndexHtml));
			String line = br.readLine();
			while (line != null) {
				if (!line.contains("<!--") && line.contains("\"../../../../")) {
					String file = line.replaceFirst(".*\"\\.\\./\\.\\./\\.\\./\\.\\./(.*?)\".*", "$1");
					File inFile = new File(Engine.WEBAPP_PATH + "/" + file);
					
					boolean needImages = file.matches(".*jquery\\.mobile\\..*?min.css");
					
					file = file.replace("scripts/", "js/");
					File outFile = new File(tmpMobileWwwPath + "/" + file);
					outFile.getParentFile().mkdirs();
					FileUtils.copyFile(inFile, outFile);
					line = line.replaceFirst("\"\\.\\./\\.\\./\\.\\./\\.\\./.*?\"", "\"" + file + "\"");
					
					if (needImages) {
						File inImages = new File(inFile.getParentFile(), "images");
						File outImages = new File(outFile.getParentFile(), "images");
						FileUtils.copyDirectory(inImages, outImages);
					}
					
					if (file.matches(".*/jquery\\.mobilelib\\..*?js")) {
						serverJsFile = outFile;
						String sJs = FileUtils.readFileToString(serverJsFile);
						sJs = sJs.replaceAll(Pattern.quote("url : \"../../\""), "url : \"" + endPoint + "\"");
						FileUtils.writeStringToFile(serverJsFile, sJs);
					} else if (file.matches(".*/c8o\\.core\\..*?js")) {
						serverJsFile = outFile;
						String sJs = FileUtils.readFileToString(serverJsFile);
						sJs = sJs.replaceAll(Pattern.quote("endpoint_url: \"\""), "endpoint_url: \"" + endPoint + "\"");
						FileUtils.writeStringToFile(serverJsFile, sJs);
					}
				}
				sbIndexHtml.append(line + "\n");
				line = br.readLine();
			}
			
			sIndexHtml = sbIndexHtml.toString();
		}
		
		FileUtils.writeStringToFile(indexHtmlFile, sIndexHtml);
		
		// Update config.xml
		Document configXmlDocument = XMLUtils.loadXml(tmpMobileWwwPath + "/config.xml");
		Element configXmlDocumentElement = configXmlDocument.getDocumentElement();
		configXmlDocumentElement.setAttribute("id", applicationID);
	
		FileWriter fileWriter = new FileWriter(tmpMobileWwwPath + "/config.xml");
		XMLUtils.prettyPrintDOMWithEncoding(configXmlDocument, "UTF-8", fileWriter);
		fileWriter.close();
	
		// Build the ZIP file for the mobile device
		String mobileArchiveFileName = tmpMobileResourcesPath + "/" + application + ".zip";
		ZipUtils.makeZip(mobileArchiveFileName, tmpMobileResourcesPath + "/www", null);
		
		return mobileArchiveFileName;
	}
	
	private static void checkAlphaNumericCharsInFileNames(File resourcePath, String tmpMobileWwwPath, String originalMobileResourcesPath) throws ServiceException {
		String filename = resourcePath.getName();
		if (!alphaNumPattern.matcher(filename).matches()) {
			String translatedResourcePathName = resourcePath.getPath().replaceAll(
					Pattern.quote(tmpMobileWwwPath), originalMobileResourcesPath);
			throw new ServiceException("The file or directory '" + translatedResourcePathName
					+ "' contains non alpha-numeric characters. "
					+ "BlackBerry build does not allow non alpha-numeric characters in file/directory names. "
					+ "You must rename it with alpha-numeric characters only.");
		}
		if (resourcePath.isDirectory()) {
			File[] filesList = resourcePath.listFiles();
			for (File file : filesList) {
				checkAlphaNumericCharsInFileNames(file, tmpMobileWwwPath, originalMobileResourcesPath);
			}
		}
	}
	
	private static String resolveFile(String html, String origin, String dest, String tmpMobileWwwPath) throws FileNotFoundException, IOException {
		File destFile = new File(tmpMobileWwwPath + "/" + dest);
		if (!destFile.exists()) {
			IOUtils.copy(new FileInputStream(Engine.WEBAPP_PATH + "/" + origin), new FileOutputStream(destFile));
			html = html.replaceAll(Pattern.quote("../../../../" + origin), dest);
		}
		return html;
	}
}
