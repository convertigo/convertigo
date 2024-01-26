/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.steps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class PdfFormStep extends Step implements IStepSmartTypeContainer, IStepSourceContainer {

	private static final long serialVersionUID = 2273726638385880897L;

	/* COMBO BOX */

	public enum Action {
		fillForm, getFields
	}

	/* Properties */
	private transient XMLVector<String> sourceDefinition = new XMLVector<String>();
	private transient PDDocument pdf = null;
	private transient PDAcroForm acroForm = null;
	private transient Set<SmartType> smartTypes = null;
	private transient List<PDField> pdfFields;
	
	/* bean infos properties */
	private SmartType filePath = new SmartType();
	private SmartType targetFile = new SmartType();
	private SmartType fieldsList = new SmartType();
	private Action action = Action.fillForm;


	public PdfFormStep() {
		super();
		setOutput(false);
		xml = true;
	}

	/* Setters and Getters */
	public SmartType getFilePath() {
		return filePath;
	}

	public void setFilePath(SmartType filePath) {
		this.filePath = filePath;
	}

	public SmartType getFields() {
		return fieldsList;
	}

	public void setFields(SmartType fieldsList) {
		this.fieldsList = fieldsList;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public SmartType getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(SmartType targetFile) {
		this.targetFile = targetFile;
	}

	@Override
	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	@Override
	public Set<SmartType> getSmartTypes() {
		if (smartTypes != null) {
			if (!hasChanged) {
				return smartTypes;
			} else {
				smartTypes.clear();
			}
		} else {
			smartTypes = new HashSet<SmartType>();
		}
		smartTypes.add(fieldsList);
		smartTypes.add(filePath);
		smartTypes.add(targetFile);
		return smartTypes;
	}

	@Override
	public PdfFormStep clone() throws CloneNotSupportedException {
		PdfFormStep clonedObject = (PdfFormStep) super.clone();
		clonedObject.smartTypes = null;
		clonedObject.filePath = filePath.clone();
		clonedObject.targetFile = targetFile.clone();
		clonedObject.fieldsList = fieldsList.clone();
		return clonedObject;
	}

	@Override
	public String getStepNodeName() {
		return "PDFfields";
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");

		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}

	private void fillForm() throws EngineException {
		try {
			// Case of JSON usage
			if (fieldsList.isUseExpression()) {
				String json = null;
				JSONObject job = null;
				JSONArray keys = null;

				json = fieldsList.getSingleString(this);
				job = new JSONObject(json);
				keys = job.names();

				Engine.logBeans.debug("(PdfFormStep) Creating a new JSON Object");

				for (int i = 0; i < keys.length(); i++) {

					try {
						String key = keys.getString(i);
						String value = job.getString(key);

						Engine.logBeans.debug("(PdfFormStep) Key : " + key + " Value: " + value);

						replaceField(key, value, pdf);
					} catch (JSONException e) {
						Engine.logBeans.error("(PdfFormStep) Keys or values UNKNOWN", e);
					}

				}
			}
			// Case of source
			else if (fieldsList.isUseSource()) {
				NodeList nl;
				try {
					nl = (NodeList) fieldsList.getObject(this);
					for (int i = 0; i < nl.getLength(); i++) {
						Node child = nl.item(i);
						NodeList lc = child.getChildNodes(); // get child for each elements
						for (int j = 0; j < lc.getLength(); j++) {
							Node cElement = lc.item(j);
							String key;
							if(lc.item(j).getAttributes().getNamedItem("originalKeyName") != null) {
								key = lc.item(j).getAttributes().getNamedItem("originalKeyName").getNodeValue();
							}
							else {
								key = lc.item(j).getNodeName();
							}
							String value = cElement.getFirstChild().getNodeValue();
							replaceField(key, value, pdf);
							Engine.logBeans.debug("(PdfFormStep) Node name = " + key + " , value = " + value);
						}

					}
				} catch (EngineException e) {
					Engine.logBeans.warn("(PdfFormStep) Failed to retrieve fields from source", e);
				} // fields
			}
		} // try
		catch (JSONException e) {
			Engine.logBeans.error("(PdfFormStep) Error creating JSONObject", e);
			setErrorStatus(true);
		}
	}

	/* Methods */
	private PDRectangle getFieldArea(PDField field) {
		COSDictionary fieldDict = field.getCOSObject();
		COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
		return new PDRectangle(fieldAreaArray);
	}

	/**
	 * Name is the field's name Data is his value doc is the pdf you're working on
	 * 
	 * @param name
	 * @param value
	 * @param doc
	 */
	private void handleImages(String name, String value, PDDocument doc) throws EngineException {
		PDField field = acroForm.getField(name);
		PDImageXObject pdImageXObject = null;
		if (field != null) {
			Engine.logBeans.debug("(PdfFormStep) handleImage - field not null");
			List<PDAnnotationWidget> widgets = field.getWidgets();
			if (widgets != null & widgets.size() > 0) {
				PDAnnotationWidget annotationWidget = widgets.get(0); // just need one widget

				File image = new File(value);
				if (image.exists()) {
					Engine.logBeans.debug("(PdfFormStep) Reading image from file : " + image);
					try {
						pdImageXObject = PDImageXObject.createFromFile(value, doc);
						Engine.logBeans.debug("(PdfFormStep) Image created from file");
					} catch (IOException e) {
						Engine.logBeans.error("(PdfFormStep) Failed to read image from file " + image, e);
					}

				} else {
					// As this is not a file, it can be a Base64, so read it as base64
					Engine.logBeans.debug("(PdfFormStep) Reading image from base64...");
					if (value != "") {
						try (ByteArrayInputStream bais = new ByteArrayInputStream(
								DatatypeConverter.parseBase64Binary(value))) {
							pdImageXObject = LosslessFactory.createFromImage(doc, ImageIO.read(bais));
						} catch (IOException e) {
							Engine.logBeans.error("(PdfFormStep) Failed to read image from base64", e);
						}
					}
				}

				/*
				 * Create PDF appearance
				 */
				float imageScaleRatio = (float)pdImageXObject.getHeight() / (float)pdImageXObject.getWidth();
				PDRectangle buttonPosition = getFieldArea(field);
				float height = buttonPosition.getHeight();
				float width = height / imageScaleRatio;
				float x = buttonPosition.getLowerLeftX();
				float y = buttonPosition.getLowerLeftY();

				Engine.logBeans.debug("(PdfFormStep) Image coords (x, y, w, h) : " + x + "," + y + "," + width + "," + height + ",");

				PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(doc);
				pdAppearanceStream.setResources(new PDResources());
				try (PDPageContentStream pdPageContentStream = new PDPageContentStream(doc, pdAppearanceStream)) {
					pdPageContentStream.drawImage(pdImageXObject, x, y, width, height);

					pdAppearanceStream.setBBox(new PDRectangle(x, y, width, height));
					PDAppearanceDictionary pdAppearanceDictionary = annotationWidget.getAppearance();

					if (pdAppearanceDictionary == null) {
						pdAppearanceDictionary = new PDAppearanceDictionary();
						annotationWidget.setAppearance(pdAppearanceDictionary);
					}
					pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
					Engine.logBeans.debug("(PdfFormStep) Image is inserted");
				} catch (IOException e) {
					Engine.logBeans.warn("(PdfFormStep) Failed to insert the image", e);
				}
			} else {
				Engine.logBeans.debug("(PdfFormStep) Misconfiguration of placeholder '" + name + "' - no widgets(actions) found");
			}
		}
	}

	/**
	 * name is the field's name data is his value doc is the pdf you're working on
	 * Put values in the field
	 * 
	 * @param name
	 * @param data
	 * @param doc
	 */
	private void replaceField(String name, String data, PDDocument doc) {
		acroForm = doc.getDocumentCatalog().getAcroForm();
		PDField field = acroForm.getField(name);
		try {
			Engine.logBeans.debug("(PdfFormStep) replaceField: replacing field - " + field);
			if (field != null) {
				Engine.logBeans.debug("(PdfFormStep) replaceField: - field is not null");
				if (field.isReadOnly()) {
					field.setReadOnly(false);
					Engine.logBeans.debug("(PdfFormStep) replaceField: - field read only is false");
				}

				String fieldType = field.getClass().getSimpleName(); // return the type of the field
				Engine.logBeans.debug("(PdfFormStep) Field type = " + fieldType);

				switch (fieldType) {
				case "PDTextField":
					field.setValue(data);
					Engine.logBeans.debug("(PdfFormStep) replaceField - PDTextField value set " + data);
					break;
				case "PDCheckBox":
						if (data.equals("On") || data.equals("True") || data.equals("Yes")) {
							((PDCheckBox) field).check();
							Engine.logBeans.debug("(PdfFormStep) replaceField - PDFCheckBox value set " + data);
						} else {
							((PDCheckBox) field).unCheck();
							Engine.logBeans.debug("(PdfFormStep) replaceField - PDFCheckBox value unset " + data);
						}
					break;
				// We handle pushButtons as a placeholder for images
				case "PDPushButton":
					try {
						String imgPath = getAbsoluteFilePath(data);
						handleImages(name, imgPath, doc);
					} catch (EngineException e) {
						Engine.logBeans.error("(PdfFormStep) Source image file not found", e);
					}
					Engine.logBeans.debug("(PdfFormStep) replaceField - PDPushButton value set");
					break;
				default:
					Engine.logBeans.error("(PdfFormStep) UNKNOWN field " + field.getClass().getSimpleName());
					setErrorStatus(true);
					break;
				}
				field.setReadOnly(true);
			} else {
				Engine.logBeans.error("(PdfFormStep) No field found with name:" + name);
				setErrorStatus(true);
			}
		} catch (IOException e) {
			Engine.logBeans.error("(PdfFormStep) replaceField FAILED", e);
		}
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			// Load PDF file and Form Catalog;
			evaluate(javascriptContext, scope, "fieldsList", fieldsList);
			evaluate(javascriptContext, scope, "filePath", filePath);
			evaluate(javascriptContext, scope, "targetFile", targetFile);

			try {
				try {
					Engine.logBeans.debug("(PdfFormStep) Opening PDF file : " + filePath.getSingleString(this));

					String sSourcePath = filePath.getSingleString(this);
					String afp = getAbsoluteFilePath(sSourcePath);
					File file = new File(afp);
					pdf = PDDocument.load(file);
					PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
					acroForm = docCatalog.getAcroForm();
				} catch (IOException e) {
					Engine.logBeans.error("(PdfFormStep) Error opening PDF file", e);
					setErrorStatus(true);
				}

				if (action == Action.fillForm) {
					fillForm();

					// save the pdf document to a target file
					try {
						String sTargetFile = targetFile.getSingleString(this);
						String tafp = getAbsoluteFilePath(sTargetFile);
						pdf.save(tafp);
						Engine.logBeans.debug("(PdfFormStep) PDF SAVED");
					} catch (IOException e) {
						Engine.logBeans.error("(PdfFormStep) FAILED TO SAVE PDF", e);
					}
				} else if (action == Action.getFields) {
					if (acroForm != null) {
						pdfFields = acroForm.getFields();
						return super.stepExecute(javascriptContext, scope);
					}
				}
			} finally {
				try {
					if (pdf != null) {
						pdf.close();
					}
				} catch (Exception e) {
					Engine.logBeans.warn("(PdfFormStep) FAILED TO CLOSE PDF", e);
				}
			}
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);

		/*
		 * <PDFfields>
		 */
		XmlSchemaComplexType cType0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType0);

		XmlSchemaSequence sequence0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType0.setParticle(sequence0);

		XmlSchemaElement elt1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());

		XmlSchemaAttribute attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("type");
		attr.setFixedValue("array");
		cType0.getAttributes().add(attr);

		elt1.setName("field");
		elt1.setMinOccurs(0);
		elt1.setMaxOccurs(Long.MAX_VALUE);
		sequence0.getItems().add(elt1);

		/*
		 * field
		 * 
		 */
		XmlSchemaComplexType cType1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		attr = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAttribute());
		attr.setName("type");
		attr.setFixedValue("object");
		cType1.getAttributes().add(attr);
		elt1.setType(cType1);

		XmlSchemaSequence sequence1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType1.setParticle(sequence1);

		XmlSchemaElement eltType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		eltType.setName("type");
		eltType.setSchemaTypeName(Constants.XSD_STRING);
		sequence1.getItems().add(eltType);

		XmlSchemaElement eltVal = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		eltVal.setName("val");
		eltVal.setSchemaTypeName(Constants.XSD_STRING);
		sequence1.getItems().add(eltVal);

		XmlSchemaElement eltName = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		eltName.setName("name");
		eltName.setSchemaTypeName(Constants.XSD_STRING);
		sequence1.getItems().add(eltName);

		return element;

	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		pdfFields.forEach((field) -> {
			String val = field.getValueAsString();
			String type = field.getFieldType();
			String name = field.getFullyQualifiedName();
			Element fld = doc.createElement("field");
			fld.setAttribute("type", "object");

			Node nodeType = fld.appendChild(doc.createElement("type"));
			nodeType.appendChild(doc.createTextNode(type));

			Node nodeVal = fld.appendChild(doc.createElement("val"));
			nodeVal.appendChild(doc.createTextNode(val));

			Node nodeName = fld.appendChild(doc.createElement("name"));
			nodeName.appendChild(doc.createTextNode(name));
			stepNode.appendChild(fld);
		});
		stepNode.setAttribute("type", "array");
	}
}