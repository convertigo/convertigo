/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.DateUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XMLGenerateDatesStep extends XMLGenerateStep implements ITagsProperty, ISimpleTypeAffectation {

	private static final long serialVersionUID = 6690947224439520417L;

	private XMLVector<String> startDefinition = new XMLVector<String>();
	private XMLVector<String> stopDefinition = new XMLVector<String>();
	private XMLVector<String> daysDefinition = new XMLVector<String>();
	
	private String inputFormat = "dd/MM/yyyy";
	private String inputLocale = "FR";
	private String outputFormat = "EEEE, d MMMM yyyy";
	private String outputLocale = "FR";
	private boolean split = true;
	private boolean calendarCompatibility = false;
	
	private transient StepSource startSource = null;
	private transient StepSource stopSource = null;
	private transient StepSource daysSource = null;
	
	public XMLGenerateDatesStep() {
		super();
	}

	@Override
    public XMLGenerateDatesStep clone() throws CloneNotSupportedException {
    	XMLGenerateDatesStep clonedObject = (XMLGenerateDatesStep) super.clone();
    	clonedObject.startSource = null;
    	clonedObject.stopSource = null;
    	clonedObject.daysSource = null;
        return clonedObject;
    }

	@Override
    public XMLGenerateDatesStep copy() throws CloneNotSupportedException {
    	XMLGenerateDatesStep copiedObject = (XMLGenerateDatesStep) super.copy();
        return copiedObject;
    }
	
	protected String getActionName() {
		return "GenerateDates";
	}

	public XMLVector<String> getStartDefinition() {
		return startDefinition;
	}

	public void setStartDefinition(XMLVector<String> startDefinition) {
		this.startDefinition = startDefinition;
		startSource = new StepSource(this,startDefinition);
	}

	public XMLVector<String> getStopDefinition() {
		return stopDefinition;
	}

	public void setStopDefinition(XMLVector<String> stopDefinition) {
		this.stopDefinition = stopDefinition;
		stopSource = new StepSource(this,stopDefinition);
	}
	
	public XMLVector<String> getDaysDefinition() {
		return daysDefinition;
	}

	public void setDaysDefinition(XMLVector<String> daysDefinition) {
		this.daysDefinition = daysDefinition;
		daysSource = new StepSource(this,daysDefinition);
	}

	public String getInputFormat() {
		return inputFormat;
	}

	public void setInputFormat(String inputFormat) {
		this.inputFormat = inputFormat;
	}

	public String getInputLocale() {
		return inputLocale;
	}

	public void setInputLocale(String inputLocale) {
		this.inputLocale = inputLocale;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public String getOutputLocale() {
		return outputLocale;
	}

	public void setOutputLocale(String outputLocale) {
		this.outputLocale = outputLocale;
	}

	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		this.split = split;
	}

	public boolean isCalendarCompatibility() {
		return calendarCompatibility;
	}

	public void setCalendarCompatibility(boolean calendarCompatibility) {
		this.calendarCompatibility = calendarCompatibility;
	}
	
	protected StepSource getStartSource() {
		if (startSource == null) startSource = new StepSource(this,startDefinition);
		return startSource;
	}

	protected StepSource getStopSource() {
		if (stopSource == null) stopSource = new StepSource(this,stopDefinition);
		return stopSource;
	}

	protected StepSource getDaysSource() {
		if (daysSource == null) daysSource = new StepSource(this,daysDefinition);
		return daysSource;
	}

	@Override
	protected String getSpecificLabel() throws EngineException {
    	StepSource stepSource = null;
    	String startLabel = "";
    	String stopLabel = "";
    	String daysLabel = "";
    	String label = "";
    	
    	stepSource = getStartSource();
    	if (stepSource != null) {
    		startLabel = stepSource.getLabel();
			if (startLabel.equals("! broken source !"))
				return " (! broken source in Start !)";
    	}
    	stepSource = getStopSource();
    	if (stepSource != null) {
    		stopLabel = stepSource.getLabel();
			if (stopLabel.equals("! broken source !"))
				return " (! broken source in Stop !)";
    	}
    	stepSource = getDaysSource();
    	if (stepSource != null) {
    		daysLabel = stepSource.getLabel();
			if (daysLabel.equals("! broken source !"))
				return " (! broken source in Days !)";
    	}
    	
    	daysLabel = daysLabel.equals("") ? "??":daysLabel;
    	label = "("+ "@("+daysLabel+"),..." +")";
    	return label;
    }

	@Override
	public void stepMoved(StepEvent stepEvent) {
    	StepSource stepSource = null;
    	
    	stepSource = getStartSource();
    	if (stepSource != null) {
    		stepSource.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
    	}
    	
    	stepSource = getStopSource();
    	if (stepSource != null) {
    		stepSource.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
    	}
    	
    	stepSource = getDaysSource();
    	if (stepSource != null) {
    		stepSource.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
    	}
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		boolean bInvalid = false;
		try {
			if (startDefinition.isEmpty() || stopDefinition.isEmpty() || daysDefinition.isEmpty()) {
				bInvalid = true;
				Engine.logBeans.warn("Skipping GenerateDates step \""+ getName() +"\" : definitions are empty");
			}
			else {
				NodeList startList = getStartSource().getContextValues();
				NodeList stopList = getStopSource().getContextValues();
				NodeList dayList = getDaysSource().getContextValues();
				
				String start = getNodeValue(startList.item(0));
				String stop = getNodeValue(stopList.item(0));
				String[] days = getNodeValue(dayList.item(0)).split(",");
				
				List<Integer> v = new ArrayList<Integer>(days.length);
				for (int i=0; i< days.length; i++) {
					int day = Integer.parseInt(days[i],10);
					if (!calendarCompatibility) {
						day = day + 1;
						day = ((day == 8) ? 1:day);
					}
					if ((1 <= day) && (day <= 7)) {
						v.add(Integer.valueOf(day));
					}
				}
				
				/* Fix: #1085 - Treats input days as non ordered */
				Collections.sort(v);
				if (v.get(0).equals(1)) {
					v.remove(0);
					v.add(1);
				}
				/* End fix */
				
				DateFormat dfInput = new SimpleDateFormat(inputFormat, DateUtils.getLocale(inputLocale));
				dfInput.setTimeZone(TimeZone.getDefault());
				
				Date dateStart = dfInput.parse(start);
				Date dateStop = dfInput.parse(stop);

				Calendar c1 = Calendar.getInstance(TimeZone.getDefault());
				c1.setTime(dateStart);
				
				Calendar c2 = Calendar.getInstance(TimeZone.getDefault());
				c2.setTime(dateStop);
				
				if (c2.before(c1))
					Engine.logBeans.warn("Skipping GenerateDates step \""+ getName() +"\" : end date lower than start date");
				
				c2.add(Calendar.DATE, 1);
				
				DateFormat dfOutput = new SimpleDateFormat(outputFormat, DateUtils.getLocale(outputLocale));
				dfOutput.setTimeZone(TimeZone.getDefault());
				
				if (v.isEmpty()) {
					bInvalid = true;
				}
				else {
					Integer first = (Integer)v.get(0);
					Integer last = (Integer)v.get(v.size() - 1);
					
					while (c1.before(c2)) {
						int i = c1.get(Calendar.DAY_OF_WEEK);
						Integer day = Integer.valueOf(i);
						if (v.contains(day)) {
							Element element = doc.createElement("date");
							if (split) {
								Element dow = (Element)element.appendChild(doc.createElement("dayOfWeek"));
								int idow = c1.get(Calendar.DAY_OF_WEEK);
								idow = (calendarCompatibility ? idow:idow-1);
								idow = (idow==0) ? 7: idow;
								dow.appendChild(doc.createTextNode(String.valueOf(idow)));
								
								Element dom = (Element)element.appendChild(doc.createElement("day"));
								dom.appendChild(doc.createTextNode(String.valueOf(c1.get(Calendar.DATE))));
								
								Element month = (Element)element.appendChild(doc.createElement("month"));
								int imonth = c1.get(Calendar.MONTH);
								imonth = (calendarCompatibility ? imonth:imonth+1);
								String smonth = String.valueOf(imonth);
								smonth = (!calendarCompatibility && (smonth.length() == 1)) ? "0"+ smonth:smonth;
								month.appendChild(doc.createTextNode(smonth));
								
								Element year = (Element)element.appendChild(doc.createElement("year"));
								year.appendChild(doc.createTextNode(String.valueOf(c1.get(Calendar.YEAR))));

								stepNode.appendChild(element);
							}
							else {
								Date date = c1.getTime();
								String dateValue = dfOutput.format(date);
								element.appendChild(doc.createTextNode(dateValue));
								stepNode.appendChild(element);
							}
							
							int amount = 1;
							int size = v.size();

							if (size == 1) {
								amount = 7;
							}
							else {
								if (day.equals(last)) {
									amount = Math.abs(last.intValue() - first.intValue());
								}
								else {
									Integer next = (Integer)v.get(v.indexOf(day)+1);
									amount = next.intValue() - day.intValue();
									if (amount < 0)
										amount = 7 + amount;
								}
							}
							c1.add(Calendar.DATE, amount);
						}
						else {
							c1.add(Calendar.DATE, 1);
						}
					}
				}
			}
		}
		catch (Exception e) {
			bInvalid = true;
			Engine.logBeans.error("Skipping GenerateDates step \""+ getName() +"\"", e);
		}
		finally {
			if (bInvalid) {
				String nodeValue = getGenerateValue();
				stepNode.appendChild(doc.createTextNode(nodeValue));
			}
		}
	}
	
	@Override
	public String getSchemaDataType() {
		return "xsd:string"; //"xsd:date";
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("inputLocale") ||
			propertyName.equals("outputLocale")) {
			return Locale.getISOCountries();
		}
		return super.getTagsForProperty(propertyName);
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		
		if (!startDefinition.isEmpty() && !stopDefinition.isEmpty() && !daysDefinition.isEmpty()) {
			XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
			element.setType(cType);
			
			XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
			cType.setParticle(sequence);
			
			XmlSchemaElement date = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
			date.setName("date");
			date.setMinOccurs(0);
			date.setMaxOccurs(Long.MAX_VALUE);
			sequence.getItems().add(date);
			
			if (split) {
				cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
				date.setType(cType);
				
				sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
				cType.setParticle(sequence);
				
				XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
				elt.setName("dayOfWeek");
				elt.setSchemaTypeName(Constants.XSD_STRING);
				sequence.getItems().add(elt);
				
				elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
				elt.setName("day");
				elt.setSchemaTypeName(Constants.XSD_STRING);
				sequence.getItems().add(elt);
				
				elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
				elt.setName("month");
				elt.setSchemaTypeName(Constants.XSD_STRING);
				sequence.getItems().add(elt);
				
				elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
				elt.setName("year");
				elt.setSchemaTypeName(Constants.XSD_STRING);
				sequence.getItems().add(elt);
				
			} else {
				date.setSchemaTypeName(getSimpleTypeAffectation());
			}
		}
		
		return element;
	}
}
