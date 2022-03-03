/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.beans.transactions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.AutoRemoveFilePolicy;
import com.twinsoft.convertigo.engine.enums.DynamicHttpVariable;
import com.twinsoft.convertigo.engine.enums.FileExistPolicy;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class DownloadHttpTransaction extends AbstractHttpTransaction {

	private static final long serialVersionUID = -3501666022297702027L;
	private static final byte[] empty = new byte[0];
	private static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
	private FileExistPolicy fileExistPolicy = FileExistPolicy.override;
	private String folder = "";
	private String filename = "";
	private AutoRemoveFilePolicy autoRemoveFilePolicy = AutoRemoveFilePolicy.contextEnding;
	
	private transient File file;
	private transient boolean skip;
	private transient int status;
	private transient boolean success;
	private transient String contentType;
	private transient String currentFolder;
	private transient String currentFilename;

	
	public DownloadHttpTransaction() {
		super();
	}

	@Override
	public void runCore() throws EngineException {
		file = null;
		skip = false;
		status = -1;
		success = false;
		contentType = null;
		
		super.runCore();
	}

	@Override
	public void parseInputDocument(Context context) throws EngineException {
		super.parseInputDocument(context);
		
		currentFolder = folder;
		currentFilename = filename;
		
		for (RequestableVariable v: getAllVariables()) {
			if (v.getName().equals(DynamicHttpVariable.__download_folder.name())) {
				RequestableHttpVariable var = (RequestableHttpVariable) v;
				Object o = var.getDefaultValue();
				if (o != null && o instanceof String) {
					currentFolder = (String) o;
				}
			} else if (v.getName().equals(DynamicHttpVariable.__download_filename.name())) {
				RequestableHttpVariable var = (RequestableHttpVariable) v;
				Object o = var.getDefaultValue();
				if (o != null && o instanceof String) {
					currentFilename = (String) o;
				}
			} 
		}
		
		NodeList nl = context.inputDocument.getElementsByTagName("download-folder");
		if (nl.getLength() == 1) {
			Element elt = (Element) nl.item(0);
			String val  = elt.getAttribute("value");
			if (StringUtils.isNotBlank(val)) {
				if (getAccessibility() == Accessibility.Private) {
					currentFolder = val;
				} else {
					Engine.logBeans.error("(DownloadHttpTransaction) The transaction isn't Private, the download-folder cannot be override.");
				}
			}
		}
		 
		nl = context.inputDocument.getElementsByTagName("download-filename");
		int len = nl.getLength();
		if (len > 0) {
			Element elt = (Element) nl.item(0);
			String val  = elt.getAttribute("value");
			if (StringUtils.isNotBlank(val)) {
				if (getAccessibility() == Accessibility.Private) {
					currentFilename = val;
				} else {
					Engine.logBeans.error("(DownloadHttpTransaction) The transaction isn't Private, the download-folder cannot be override.");
				}
			}
		}
	}
	
	@Override
	public void makeDocument(byte[] httpData) throws Exception {
		String t = context.statistics.start(EngineStatistics.GENERATE_DOM);

		try {
			Document doc = context.outputDocument;
			Element root = doc.getDocumentElement();
			Element elt = doc.createElement("success");
			root.appendChild(elt);
			elt.setTextContent(Boolean.toString(success));
			elt.setAttribute("type", "boolean");
			
			elt = doc.createElement("status");
			root.appendChild(elt);
			elt.setTextContent(Integer.toString(status));
			elt.setAttribute("type", "integer");
			if (success) {
				elt = doc.createElement("filepath");
				root.appendChild(elt);
				elt.setTextContent(file.getAbsolutePath());
				
				elt = doc.createElement("filename");
				root.appendChild(elt);
				elt.setTextContent(file.getName());
				
				elt = doc.createElement("folder");
				root.appendChild(elt);
				elt.setTextContent(file.getParent());
				
				elt = doc.createElement("length");
				root.appendChild(elt);
				elt.setTextContent(Long.toString(file.length()));
				elt.setAttribute("type", "long");
				
				elt = doc.createElement("contentType");
				root.appendChild(elt);
				elt.setTextContent(contentType);
				
				elt = doc.createElement("lastModified");
				root.appendChild(elt);
				elt.setTextContent(Long.toString(file.lastModified()));
				elt.setAttribute("type", "long");
				
				elt = doc.createElement("skip");
				root.appendChild(elt);
				elt.setTextContent(Boolean.toString(skip));
				elt.setAttribute("type", "boolean");
				
				String projectDir = getConnector().getProject().getDirFile().getAbsolutePath();
				if (file.getAbsolutePath().startsWith(projectDir)) {
					elt = doc.createElement("url");
					root.appendChild(elt);
					String url = HttpUtils.originalRequestURL(context.httpServletRequest);
					url = url.substring(0, url.lastIndexOf("/"));
					url += file.getAbsolutePath().substring(projectDir.length()).replace('\\', '/');
					elt.setTextContent(url);
				}
			}
		}
		finally {
			context.statistics.stop(t);
		}
	}
	
	@Override
	public byte[] readResult(InputStream in, HttpMethod method) throws IOException {
		status = method.getStatusCode();
		if (status < 200 || status >= 300) {
			Engine.logBeans.info("(DownloadHttpTransaction) Status HTTP is " + status + " not 2xx, doesn't try to download file.");
			return null;
		}
		
		boolean removeParent = false;
		
		String folder = currentFolder;
		if (folder.isBlank()) {
			folder = ".//_data/download/" + Long.toString(Math.round(Math.random() * 999999), Character.MAX_RADIX);
			removeParent = true;
		}
		String filename = currentFilename;
		if (filename.isBlank()) {
			filename = method.getURI().getPath();
			int i = filename.lastIndexOf("/");
			if (i >= 0) {
				filename = filename.substring(i + 1);
			}
			if (filename.isBlank()) {
				filename = "file.bin";
			}
		}
		String filepath = folder;
		if (!folder.endsWith("/") && !folder.endsWith("\\")) {
			filepath += "/";
		}
		
		long lastModified = -1;
		try {
			String sDate = HeaderName.LastModified.getResponseHeader(method);
			Date d = format.parse(sDate);
			lastModified = d.getTime();
		} catch (ParseException e) {
		}
		
		filepath += filename;
		file = Engine.theApp.filePropertyManager.getFileFromProperty(filepath, getProject().getName());
		
		if (fileExistPolicy == FileExistPolicy.increment) {
			file = FileUtils.incrementFilename(file);
		}
		
		switch (autoRemoveFilePolicy) {
		case contextEnding: context.addFileToDeleteAtEndOfContext(removeParent ? file.getParentFile() : file); break;
		case sessionEnding: context.addFileToDeleteAtEndOfSession(removeParent ? file.getParentFile() : file); break;
		default: break;
		}
		
		Engine.logBeans.debug("(DownloadHttpTransaction) Prepare to download to: " + file);
		if (file.exists()) {
			if (fileExistPolicy == FileExistPolicy.override) {
				skip = false;
			} else if (fileExistPolicy == FileExistPolicy.overrideNewer) {
				skip = lastModified >= file.lastModified();
			} else if (fileExistPolicy == FileExistPolicy.overrideSize) {
				String sLen = HeaderName.ContentLength.getResponseHeader(method);
				if (sLen != null && Long.parseLong(sLen) == file.length()) {
					skip = true;
				}
			} else {
				skip = true;
			}
		}
		
		if (!skip) {
			file.getParentFile().mkdirs();
			try (FileOutputStream out = new FileOutputStream(file)) {
				IOUtils.copy(in, out);
				if (lastModified > 0) {
					file.setLastModified(lastModified);
				}
				success = true;
			}
		} else {
			success = true;
		}
		
		if (success) {
			contentType = HeaderName.ContentType.getResponseHeader(method);
			if (StringUtils.isBlank(contentType)) {
				try {
					contentType = context.httpServletRequest.getServletContext().getMimeType(file.getAbsolutePath());
				} catch (Exception e) {
				}
				if (StringUtils.isBlank(contentType)) {
					contentType = MimeType.OctetStream.value();
				}
			}
			if (file.exists()) {
				context.set("__downloadedFileLength", file.length());
			}
		}
		
		return empty;
	}

	protected XmlSchemaElement createXmlSchemaElement(XmlSchema xmlSchema, String tagname, NameValuePair... nvp) {
		XmlSchemaElement element = new XmlSchemaElement();
		element.setName(tagname);
		if (nvp.length > 0) {
			XmlSchemaComplexType cType = new XmlSchemaComplexType(xmlSchema);
			element.setType(cType);

			XmlSchemaSimpleContent simpleContent = new XmlSchemaSimpleContent();
			cType.setContentModel(simpleContent);

			XmlSchemaSimpleContentExtension simpleContentExtension = new XmlSchemaSimpleContentExtension();
			simpleContent.setContent(simpleContentExtension);

			simpleContentExtension.setBaseTypeName(Constants.XSD_STRING);

			XmlSchemaObjectCollection attrs = simpleContentExtension.getAttributes();
			for (NameValuePair p: nvp) {
				XmlSchemaAttribute attribute = new XmlSchemaAttribute();
				attribute.setName(p.getName());
				attribute.setSchemaTypeName(Constants.XSD_STRING);
				attribute.setDefaultValue(p.getValue());
				attrs.add(attribute);
			}
		} else {
			element.setSchemaTypeName(Constants.XSD_STRING);
		}
		element.setMinOccurs(0);
		element.setMaxOccurs(1);
		return element;
	}
	
	@Override
	protected XmlSchemaComplexType addSchemaResponseDataType(XmlSchema xmlSchema) {
		XmlSchemaComplexType xmlSchemaComplexType = super.addSchemaResponseDataType(xmlSchema);
		XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence)xmlSchemaComplexType.getParticle();
		if (xmlSchemaSequence == null) {
			xmlSchemaComplexType.setParticle(xmlSchemaSequence = new XmlSchemaSequence());
		}
		XmlSchemaObjectCollection items = xmlSchemaSequence.getItems();
		
		items.add(createXmlSchemaElement(xmlSchema, "success", new NameValuePair("type", "boolean")));
		items.add(createXmlSchemaElement(xmlSchema, "status", new NameValuePair("type", "integer")));
		items.add(createXmlSchemaElement(xmlSchema, "filepath"));
		items.add(createXmlSchemaElement(xmlSchema, "filename"));
		items.add(createXmlSchemaElement(xmlSchema, "folder"));
		items.add(createXmlSchemaElement(xmlSchema, "length", new NameValuePair("type", "long")));
		items.add(createXmlSchemaElement(xmlSchema, "contentType"));
		items.add(createXmlSchemaElement(xmlSchema, "lastModified", new NameValuePair("type", "long")));
		items.add(createXmlSchemaElement(xmlSchema, "skip", new NameValuePair("type", "boolean")));
		items.add(createXmlSchemaElement(xmlSchema, "url"));
		return xmlSchemaComplexType;
	}

	public FileExistPolicy getFileExistPolicy() {
		return fileExistPolicy;
	}

	public void setFileExistPolicy(FileExistPolicy fileExistPolicy) {
		this.fileExistPolicy = fileExistPolicy;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public AutoRemoveFilePolicy getAutoRemoveFilePolicy() {
		return autoRemoveFilePolicy;
	}

	public void setAutoRemoveFilePolicy(AutoRemoveFilePolicy autoRemoveFilePolicy) {
		this.autoRemoveFilePolicy = autoRemoveFilePolicy;
	}
}