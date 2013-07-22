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

package com.twinsoft.convertigo.beans.steps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.DateUtils;

public class XMLDateTimeStep extends XMLConcatStep implements ITagsProperty{

	private static final long serialVersionUID = -9090204919218726488L;

	private String inputFormat = "dd/MM/yyyy HH:mm:ss";
	private String inputLocale = "FR";
	private String outputFormat = "yyyy-MM-dd HH:mm:ss";
	private String outputLocale = "US";
	
	public XMLDateTimeStep() {
		super();
	}

	@Override
    public XMLDateTimeStep clone() throws CloneNotSupportedException {
    	XMLDateTimeStep clonedObject = (XMLDateTimeStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLDateTimeStep copy() throws CloneNotSupportedException {
    	XMLDateTimeStep copiedObject = (XMLDateTimeStep) super.copy();
        return copiedObject;
    }

	@Override
	protected String getActionName() {
		return "Date";
	}

	@Override
	protected String getActionValue() throws EngineException {
		String nodeValue = super.getActionValue();
		if (!nodeValue.equals("")) {
			try {
				Date date = null;
				if (nodeValue.equalsIgnoreCase("now")) {
					date = new Date();
				}
				else {
					DateFormat dfInput = new SimpleDateFormat(inputFormat, DateUtils.getLocale(inputLocale));
					dfInput.setTimeZone(TimeZone.getDefault());
					date = dfInput.parse(nodeValue);
				}
				
				DateFormat dfOutput = new SimpleDateFormat(outputFormat, DateUtils.getLocale(outputLocale));
				dfOutput.setTimeZone(TimeZone.getDefault());
				nodeValue = dfOutput.format(date);
			} catch (Exception e) {
				Engine.logBeans.warn(e.getMessage());
				setErrorStatus(true);
			}
		}
		return nodeValue;
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

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("outputLocale") || propertyName.equals("inputLocale")){
			return Locale.getISOCountries();
		}
		return super.getTagsForProperty(propertyName);
	}
}
