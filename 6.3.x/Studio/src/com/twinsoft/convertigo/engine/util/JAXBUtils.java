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

package com.twinsoft.convertigo.engine.util;

import org.xml.sax.SAXParseException;

import com.sun.tools.xjc.ConsoleErrorReporter;
import com.sun.tools.xjc.Driver;
import com.sun.tools.xjc.XJCListener;

public class JAXBUtils {
	public static void validateSchema(String xsdUrl, XJCListener listener) throws Exception {
		String[] args = new String[]{"-mode","SIGNATURE","-verbose",xsdUrl};
		runXJC(args, listener);
	}
	
	public static void compileSchema(String projectName, String outputDir, String xsdUrl, XJCListener listener) throws Exception {
		String[] args = new String[]{"-d",outputDir,"-p","com.convertigo.projects."+projectName,"-verbose",xsdUrl};
		runXJC(args, listener);
	}

	public static void runXJC(String[] args, XJCListener listener) throws Exception {
//		XJC Usage: xjc [-options ...] <schema file/URL/dir/jar> ... [-b <bindinfo>] ...
//		If dir is specified, all schema files in it will be compiled.
//		If jar is specified, /META-INF/sun-jaxb.episode binding file will be compiled.
//		Options:
//		  -nv                :  do not perform strict validation of the input schema(s)
//		  -extension         :  allow vendor extensions - do not strictly follow the
//		                        Compatibility Rules and App E.2 from the JAXB Spec
//		  -b <file/dir>      :  specify external bindings files (each <file> must have its own -b)
//		                        If a directory is given, **/*.xjb is searched
//		  -d <dir>           :  generated files will go into this directory
//		  -p <pkg>           :  specifies the target package
//		  -httpproxy <proxy> :  set HTTP/HTTPS proxy. Format is [user[:password]@]proxyHost:proxyPort
//		  -httpproxyfile <f> :  Works like -httpproxy but takes the argument in a file to protect password 
//		  -classpath <arg>   :  specify where to find user class files
//		  -catalog <file>    :  specify catalog files to resolve external entity references
//		                        support TR9401, XCatalog, and OASIS XML Catalog format.
//		  -readOnly          :  generated files will be in read-only mode
//		  -npa               :  suppress generation of package level annotations (**/package-info.java)
//		  -no-header         :  suppress generation of a file header with timestamp
//		  -target 2.0        :  behave like XJC 2.0 and generate code that doesnt use any 2.1 features.
//		  -xmlschema         :  treat input as W3C XML Schema (default)
//		  -relaxng           :  treat input as RELAX NG (experimental,unsupported)
//		  -relaxng-compact   :  treat input as RELAX NG compact syntax (experimental,unsupported)
//		  -dtd               :  treat input as XML DTD (experimental,unsupported)
//		  -wsdl              :  treat input as WSDL and compile schemas inside it (experimental,unsupported)
//		  -verbose           :  be extra verbose
//		  -quiet             :  suppress compiler output
//		  -help              :  display this help message
//		  -version           :  display version information
//
//
//		Extensions:
//		  -Xlocator          :  enable source location support for generated code
//		  -Xsync-methods     :  generate accessor methods with the 'synchronized' keyword
//		  -mark-generated    :  mark the generated code as @javax.annotation.Generated
//		  -episode <FILE>    :  generate the episode file for separate compilation
//
		if (listener == null)
			listener = getXJClistener();
		Driver.run(args, listener);
	}
	
	public static XJCListener getXJClistener() {
		XJCListener l = new XJCListener() {
            ConsoleErrorReporter cer = new ConsoleErrorReporter(System.out);
            public void generatedFile(String fileName, int count, int total) {
                message(fileName);
            }
            public void message(String msg) {
               	System.out.println(msg);
            }
            public void error(SAXParseException exception) {
                cer.error(exception);
            }
            public void fatalError(SAXParseException exception) {
                cer.fatalError(exception);
            }
            public void warning(SAXParseException exception) {
                cer.warning(exception);
            }
            public void info(SAXParseException exception) {
                cer.info(exception);
            }
		};
		return l;
	}

	public static void main(String[] args) {
		try {
			runXJC(args, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
