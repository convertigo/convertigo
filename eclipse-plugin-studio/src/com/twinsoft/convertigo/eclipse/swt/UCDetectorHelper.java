/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.swt;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class UCDetectorHelper {

	public static void main(String[] args) throws Exception {
		Document doc;
		try (InputStream is = UCDetectorHelper.class.getResourceAsStream("/com/twinsoft/convertigo/beans/database_objects.xml")) {
			doc = XMLUtils.getDefaultDocumentBuilder().parse(is);
		} catch (Exception e) {
			throw e;
		}
		NodeIterator it = TwsCachedXPathAPI.getInstance().selectNodeIterator(doc, "//bean");
		Element elt;
		String generatedFile = UCDetectorHelper.class.getResource("UCDetectorHelper.class").getFile().replace("/bin/main/", "/src/").replace("UCDetectorHelper.class", "UCDetectorHelperGenerated.java");
		System.out.println("" + generatedFile);
		
		StringBuilder sb = new StringBuilder();
		sb.append("package com.twinsoft.convertigo.eclipse.swt;\n");
		sb.append("public class UCDetectorHelperGenerated {\n");
		sb.append("public static void main(String[] args) throws Exception {\n");
		sb.append("\tClass<?>[] beans = {\n");
		
		while ((elt = (Element) it.nextNode()) != null) {
//			System.out.println("" + elt.getAttribute("classname"));
			sb.append("\t\t").append(elt.getAttribute("classname")).append(".class, \n");
			sb.append("\t\t").append(elt.getAttribute("classname")).append("BeanInfo.class, \n");
		}
		
		sb.append("\t};\n\tbeans.toString();\n");
		sb.append("\t}\n}\n");
		
		FileUtils.write(new File(generatedFile), sb.toString(), StandardCharsets.UTF_8);
	}

}
