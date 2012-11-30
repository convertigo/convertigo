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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;

import org.apache.ws.commons.schema.XmlSchemaCollection;

public class WSDLUtils {

	public static XmlSchemaCollection readSchemas(Definition definition) {
		XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		schemaCol.setBaseUri(definition.getDocumentBaseURI());
		readSchemas(schemaCol, definition);
		return schemaCol;
	}
	
	private static void readSchemas(XmlSchemaCollection schemaCol, Definition definition) {
		// Read schemas of included WSDL files
		Map<String, List<Import>> imap = GenericUtils.cast(definition.getImports());
		Iterator<String> it1 = imap.keySet().iterator();
		while (it1.hasNext()) {
			String uri = it1.next();
			List<Import> list = imap.get(uri);
			for (Import imp: list) {
				readSchemas(schemaCol,imp.getDefinition());
			}
		}
		
		// Read schemas of WSDL types
		Types types = definition.getTypes();
		Iterator<?> exs = types.getExtensibilityElements().iterator();
		while (exs.hasNext()) {
			ExtensibilityElement ee = (ExtensibilityElement)exs.next();
			if (ee instanceof Schema) {
				schemaCol.read(((Schema)ee).getElement());
			}
		}
	}
		
//	@SuppressWarnings("unchecked")
//	public static Map<String, String> dumpSchemas(String schemasDir, Definition definition) {
//		Map<String, String> nsmap = new HashMap<String, String>();
//		if (definition != null) {
//			// Create schemas directory
//			File dir = new File(schemasDir);
//			if (!dir.exists())
//				dir.mkdirs();
//			
//			Map<String, List<Import>> imap = definition.getImports();
//			// Case wsdl has no wsdl import
//			if (imap.isEmpty()) {
//				dumpSchemas(schemasDir,definition,nsmap);
//			}
//			// Case wsdl has wsdl import(s)
//			else {
//				Iterator<String> it1 = imap.keySet().iterator();
//				while (it1.hasNext()) {
//					String uri = it1.next();
//					List<Import> list = imap.get(uri);
//					for (Import imp: list) {
//						dumpSchemas(schemasDir,imp.getDefinition(),nsmap);
//					}
//				}
//			}
//		}
//		return nsmap;
//	}
//	
//	public static Map<String, String> dumpSchemas(String schemasDir, Definition definition, Map<String, String> nsmap) {
//		List<String> xsdFilePaths = new ArrayList<String>();
//
//		HashMap<String, String> options = new HashMap<String, String>();
//		options.put(OutputKeys.METHOD, "xml");
//		options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
//		options.put(OutputKeys.INDENT, "yes");
//		
//		XmlSchemaCollection schemaCol = new XmlSchemaCollection();
//		schemaCol.setBaseUri(definition.getDocumentBaseURI());
//		
//		Types types = definition.getTypes();
//		Iterator<?> exs = types.getExtensibilityElements().iterator();
//		while (exs.hasNext()) {
//			ExtensibilityElement ee = (ExtensibilityElement)exs.next();
//			if (ee instanceof Schema) {
//				Schema schema = (Schema)ee;
//				
//				// Read schema from wsdl types
//				XmlSchema xmlSchema = schemaCol.read(schema.getElement());
//				
//				// Set new location for imported schemas
//				Iterator<?> it = xmlSchema.getItems().getIterator();
//				while (it.hasNext()) {
//					XmlSchemaObject ob = (XmlSchemaObject)it.next();
//					if (ob instanceof XmlSchemaImport) {
//						XmlSchemaImport xmlSchemaImport = ((XmlSchemaImport)ob);
//						String imns = xmlSchemaImport.getNamespace();
//						if (imns != null)
//							xmlSchemaImport.setSchemaLocation(StringUtils.normalize(imns) + ".xsd");
//					}
//				}
//				
//				// Write schema to file
//				String ns = xmlSchema.getTargetNamespace();
//				if ((ns!=null) && !ns.equals("")) {
//					String xsdFileName = StringUtils.normalize(ns) + ".xsd";
//					String xsdFilePath = schemasDir + "/"+ xsdFileName;
//					FileOutputStream fos = null;
//					try {
//						fos = new FileOutputStream(xsdFilePath);
//						xmlSchema.write(fos, options);
//						if (!xsdFilePaths.contains(xsdFilePath))
//							xsdFilePaths.add(xsdFilePath);
//						
//						String prefns = xmlSchema.getNamespaceContext().getPrefix(ns);
//						String prefix = prefns;
//						int index = 1;
//						while (nsmap.containsKey(prefix))
//							prefix = prefns + index++;
//						nsmap.put(prefix, ns);
//					}
//					catch (Exception e) {
//						if (fos != null) {
//							try {
//								fos.close();
//							} catch (IOException e1) {}
//						}
//						
//						File file = new File(xsdFilePath);
//						if (file.exists()) {
//							try {
//								xsdFilePaths.remove(xsdFilePath);
//								file.delete();
//							} catch (Exception e2) {}
//						}
//					}
//					finally {
//						if (fos != null) {
//							try {
//								fos.close();
//							} catch (IOException e1) {}
//						}
//					}
//				}
//			}
//		}
//		
//		// Now add missing imports if needed
//		try {
//			XmlSchemaCollection sc = new XmlSchemaCollection();
//			sc.setBaseUri(schemasDir+"/");
//			for (String xsdFilePath: xsdFilePaths) {
//				if (!new File(xsdFilePath).exists()) continue;
//				// Read schema from file
//				XmlSchema xmlSchema = sc.read(new InputSource(new FileInputStream(xsdFilePath)),null);
//				
//				String[] prefixes = new String[]{};
//				try {
//					prefixes = xmlSchema.getNamespaceContext().getDeclaredPrefixes();
//				}
//				catch (Exception e) {}
//				for (int j=0;j<prefixes.length; j++) {
//					String prefix = prefixes[j];
//					String nsuri = xmlSchema.getNamespaceContext().getNamespaceURI(prefix);
//					String xsdImportName = StringUtils.normalize(nsuri) + ".xsd";
//					String xsdImportFilePath = schemasDir + "/"+ xsdImportName;
//					if (new File(xsdImportFilePath).exists()) {
//						if (!nsuri.equals(xmlSchema.getTargetNamespace())) {
//							XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
//							xmlSchemaImport.setNamespace(nsuri);
//							xmlSchemaImport.setSchemaLocation(xsdImportName);
//							
//							boolean bFound = false;
//							Iterator<Object> it2 = GenericUtils.cast(xmlSchema.getItems().getIterator());
//							while (it2.hasNext()) {
//								Object ob = it2.next();
//								if (ob instanceof XmlSchemaImport) {
//									if (((XmlSchemaImport)ob).getNamespace().equals(nsuri)) {
//										bFound = true;
//										break;
//									}
//								}
//							}
//							if (!bFound) {
//								xmlSchema.getItems().add(xmlSchemaImport);
//							}
//						}
//					}
//				}
//				
//				// Write schema to file
//				FileOutputStream fos = null;
//				try {
//					fos = new FileOutputStream(xsdFilePath);
//					xmlSchema.write(fos, options);
//				}
//				catch (Exception e) {}
//				finally {
//					if (fos != null) {
//						try {
//							fos.close();
//						} catch (IOException e1) {}
//					}
//				}
//			}
//		}
//		catch (Exception e) {
//			e.printStackTrace(System.out);
//		}
//		
//		return nsmap;
//	}
	
}