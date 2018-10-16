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
// import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
// import org.apache.pdfbox.pdmodel.common.COSArrayList;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
		this.xml = true;
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

	public void setFields(SmartType Fields) {
		this.fieldsList = Fields;
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
		return clonedObject;
	}

	@Override
	public PdfFormStep copy() throws CloneNotSupportedException {
		PdfFormStep copiedObject = (PdfFormStep) super.copy();
		return copiedObject;
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

	private void fillForm() {
		try {
			// Case of JSON usage
			if (fieldsList.isUseExpression()) {
				String jSon = null;
				JSONObject job = null;
				JSONArray keys = null;

				try {
					jSon = fieldsList.getSingleString(this);
				} catch (EngineException e1) {
					e1.printStackTrace();
				}
				job = new JSONObject(jSon);
				keys = job.names();

				Engine.logBeans.debug("Creating a new JSON Object");

				for (int i = 0; i < keys.length(); i++) {

					try {
						String key = keys.getString(i);
						String value = job.getString(key);

						Engine.logBeans.debug("Key : " + key + " Value: " + value);

						replaceField(key, value, pdf);
					} catch (JSONException e) {
						Engine.logBeans.error("Keys or values UNKNOWN");
						e.printStackTrace();
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
							String key = lc.item(j).getNodeName();
							String value = cElement.getFirstChild().getNodeValue();
							replaceField(key, value, pdf);
							Engine.logBeans.debug("Node name = " + key + " , value = " + value);
						}

					}
				} catch (EngineException e) {
					e.printStackTrace();
				} // fields
			}
		} // try
		catch (JSONException e) {
			Engine.logBeans.error("Error creating JSONObject", e);
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
	private void handleImages(String name, String value, PDDocument doc) {
		PDField field = acroForm.getField(name);
		PDImageXObject pdImageXObject = null;
		if (field != null) {
			Engine.logBeans.debug("handleImage - field not null");
			List<PDAnnotationWidget> widgets = field.getWidgets();
			if (widgets != null & widgets.size() > 0) {
				PDAnnotationWidget annotationWidget = widgets.get(0); // just need one widget

				File image = new File(value);
				if (image.exists()) {
					Engine.logBeans.debug("Reading image from file : " + image);
					try {
						pdImageXObject = PDImageXObject.createFromFile(value, doc);
						Engine.logBeans.debug("Image created from file");
					} catch (IOException e) {
						Engine.logBeans.error("Failed to read image from file " + image);
						e.printStackTrace();
					}

				} else {
					// As this is not a file, it can be a Base64, so read it as base64
					Engine.logBeans.debug("Reading image from base64...");
					if (value != "") {
						try {
							ByteArrayInputStream bais = new ByteArrayInputStream(
									DatatypeConverter.parseBase64Binary(value));
							pdImageXObject = LosslessFactory.createFromImage(doc, ImageIO.read(bais));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				/*
				 * Create PDF appearance
				 */
				int imageScaleRatio = pdImageXObject.getHeight() / pdImageXObject.getWidth();
				PDRectangle buttonPosition = getFieldArea(field);
				float height = buttonPosition.getHeight();
				float width = height / imageScaleRatio;
				float x = buttonPosition.getLowerLeftX();
				float y = buttonPosition.getLowerLeftY();

				Engine.logBeans.debug("Image coords (x, y, w, h) : " + x + "," + y + "," + width + "," + height + ",");

				PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(doc);
				pdAppearanceStream.setResources(new PDResources());
				PDPageContentStream pdPageContentStream;
				try {
					pdPageContentStream = new PDPageContentStream(doc, pdAppearanceStream);
					pdPageContentStream.drawImage(pdImageXObject, x, y, width, height);

					pdAppearanceStream.setBBox(new PDRectangle(x, y, width, height));
					PDAppearanceDictionary pdAppearanceDictionary = annotationWidget.getAppearance();

					if (pdAppearanceDictionary == null) {
						pdAppearanceDictionary = new PDAppearanceDictionary();
						annotationWidget.setAppearance(pdAppearanceDictionary);
					}
					pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
					pdPageContentStream.close();
					Engine.logBeans.debug("Image is inserted");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Engine.logBeans.debug("Misconfiguration of placeholder '" + name + "' - no widgets(actions) found");
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
			Engine.logBeans.debug("replaceField: replacing field - " + field);
			if (field != null) {
				Engine.logBeans.debug("replaceField: - field is not null");
				if (field.isReadOnly()) {
					field.setReadOnly(false);
				}

				String fieldType = field.getClass().getSimpleName(); // return the type of the field
				Engine.logBeans.debug("Field type = " + fieldType);

				switch (fieldType) {
				case "PDTextField":
					field.setValue(data);
					Engine.logBeans.debug("replaceField - PDTextField value set " + data);
					break;
				case "PDCheckBox":
					if (data == "true") {
						((PDCheckBox) field).check();
						Engine.logBeans.debug("replaceField - PDFCheckBox value set " + data);
					} else {
						((PDCheckBox) field).unCheck();
						Engine.logBeans.debug("replaceField - PDFCheckBox value unset " + data);
					}
					break;
				// We handle pushButtons as a placeholder for images
				case "PDPushButton":
					try {
						String imgPath = this.getAbsoluteFilePath(data);
						handleImages(name, imgPath, doc);
					} catch (EngineException e) {
						Engine.logBeans.error("Source image file not found");
						e.printStackTrace();
					}
					Engine.logBeans.debug("replaceField - PDPushButton value set");
					break;
				default:
					Engine.logBeans.error("UNKNOWN field " + field.getClass().getSimpleName());
					setErrorStatus(true);
					break;
				}
				field.setReadOnly(true);
			} else {
				Engine.logBeans.error("No field found with name:" + name);
				setErrorStatus(true);
			}
		} catch (IOException e) {
			Engine.logBeans.error("replaceField FAILED", e);
		}
	}

	// private void addFieldInfos(Node node, String name) {
	//
	// }
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {

		if (isEnabled()) {
			// Load PDF file and Form Catalog;
			evaluate(javascriptContext, scope, fieldsList);
			evaluate(javascriptContext, scope, filePath);
			evaluate(javascriptContext, scope, targetFile);

			try {
				Engine.logBeans.debug("Opening PDF file : " + filePath.getSingleString(this));

				String sSourcePath = filePath.getSingleString(this);
				String afp = this.getAbsoluteFilePath(sSourcePath);
				File file = new File(afp);
				pdf = PDDocument.load(file);
				PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
				acroForm = docCatalog.getAcroForm();
			} catch (IOException e) {
				Engine.logBeans.error("Error opening PDF file", e);
				setErrorStatus(true);
			}

			if (action == Action.fillForm) {
				this.fillForm();

				// save the pdf document to a target file
				try {
					String sTargetFile = targetFile.getSingleString(this);
					String tafp = this.getAbsoluteFilePath(sTargetFile);
					pdf.save(tafp);
					pdf.close();
					Engine.logBeans.debug("PDF SAVED");
				} catch (IOException e) {
					Engine.logBeans.error("FAILED TO SAVE PDF");
					e.printStackTrace();
				}
			} else if (action == Action.getFields) {
				if (acroForm != null) {
					pdfFields = acroForm.getFields();
					return super.stepExecute(javascriptContext, scope);
				}
			}
		}
		return false;
	}

	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		// element.setSchemaTypeName(getSimpleTypeAffectation());

		/*
		 * <PDFfields>
		 */
		XmlSchemaComplexType cType0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType0);

		XmlSchemaSequence sequence0 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType0.setParticle(sequence0);

		XmlSchemaElement elt1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());

		XmlSchemaAttribute attr = new XmlSchemaAttribute();
		attr.setName("type");
		attr.setFixedValue("object");

		elt1.setName("field");
		elt1.setMinOccurs(0);
		sequence0.getItems().add(elt1);

		/*
		 * field
		 * 
		 */
		XmlSchemaComplexType cType1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		elt1.setType(cType1);

		XmlSchemaSequence sequence1 = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType1.setParticle(sequence1);

		XmlSchemaElement eltVal = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		eltVal.setName("val");
		eltVal.setSchemaTypeName(Constants.XSD_STRING);
		sequence1.getItems().add(eltVal);

		XmlSchemaElement eltType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		eltType.setName("type");
		eltType.setSchemaTypeName(Constants.XSD_STRING);
		sequence1.getItems().add(eltType);

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
