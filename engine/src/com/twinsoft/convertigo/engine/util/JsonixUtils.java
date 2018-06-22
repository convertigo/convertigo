package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;

public class JsonixUtils {

	protected static final String XMLSCHEMA_JSONSCHEMA_URL = "http://www.jsonix.org/jsonschemas/w3c/2001/XMLSchema.jsonschema";
	
    protected static void runJsonixCompiler(String schemaUrl, File targetDir, String projectName, File wsdlFile) throws IOException, InterruptedException {
    	Engine.logEngine.debug("(JsonixUtils) runJsonixCompiler...");
    	long timeStart = System.currentTimeMillis();
    	
    	// Retrieve classpath
    	StringBuffer buffer = new StringBuffer();
    	for (URL url : ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs()) {
    		buffer.append(new File(url.getPath())).append(System.getProperty("path.separator"));
    	}
    	String classpath = buffer.toString();
    	
    	// Add jars to classpath
    	String version = ProductVersion.productVersion + (ProductVersion.tag == null ? "" : "-" + ProductVersion.tag);
    	String engineJarPath = "convertigo-engine-"+ version +".jar";
    	String jsonixJarPath = "jsonix-schema-compiler-full-2.3.9.jar";
    	try {
    		jsonixJarPath = new File(org.hisrc.jsonix.JsonixMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    		engineJarPath = jsonixJarPath.substring(0, jsonixJarPath.indexOf("jsonix-schema-compiler-full")) + engineJarPath;
        	if (classpath.indexOf("jsonix-schema-compiler-full") == -1) {
        		classpath += engineJarPath + System.getProperty("path.separator"); // must be first for classes in patch
        		classpath += jsonixJarPath + System.getProperty("path.separator"); // second
        	}
		} catch (URISyntaxException use) {}
    	Engine.logEngine.debug("(JsonixUtils) using classpath: "+ classpath);
    	
    	// Run Jsonix schema compiler
    	//String[] args = {"java", "-jar", jarPath , "-generateJsonSchema", "-wsdl", "-logLevel", "INFO", 
    	//							"-d", targetDir.getCanonicalPath(), "-p", projectName, wsdlFile.getCanonicalPath()};
    	String[] args = {"java", "-cp", classpath , "org.hisrc.jsonix.JsonixMain", "-generateJsonSchema", "-wsdl", "-logLevel", "INFO", 
								"-d", targetDir.getCanonicalPath(), "-p", projectName, wsdlFile.getCanonicalPath()};
    	
    	boolean mergeError = true;
    	
        ProcessBuilder pb = ProcessUtils.getProcessBuilder("", Arrays.asList(args));
        
        pb.directory(targetDir.getCanonicalFile());
		
		pb.redirectErrorStream(mergeError);

        Process p = pb.start();
        
        try {
	        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
	        while((line = br.readLine()) != null){
	        	Engine.logEngine.debug(line);
	        }
        } catch (IOException e) {
        	Engine.logEngine.error("Error while executing jsonix compiler", e);
        }
        
        if (!mergeError) {
	        try {
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = br.readLine()) != null) {
					Engine.logEngine.error(line);
				}
	        } catch (IOException e) {
	        	Engine.logEngine.error("Error while executing jsonix compiler", e);
	        }
        }
        
        p.waitFor();
        
		String xmlSchemaUrl = schemaUrl.substring(0, schemaUrl.indexOf("/projects")) + "/oas/XMLSchema.jsonschema";

		File jsonschemaFile = new File(targetDir, projectName+".jsonschema" );
		if (jsonschemaFile.exists()) {
			String content = FileUtils.readFileToString(jsonschemaFile, "UTF-8");
			content = content.replaceFirst("\"id\":\"[^#]*#\"", "\"id\":\""+ schemaUrl +"#\"");
			content = content.replaceAll(XMLSCHEMA_JSONSCHEMA_URL, xmlSchemaUrl);
			FileUtils.write(jsonschemaFile, content, "UTF-8");
		}
		
 		long timeStop = System.currentTimeMillis();
		System.out.println("JsonixCompiler for "+ projectName + "| Times >> total : " + (timeStop - timeStart) + " ms");
    }

}
