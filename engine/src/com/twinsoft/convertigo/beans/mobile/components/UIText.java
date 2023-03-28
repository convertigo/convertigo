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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.TranslateUtils;

public class UIText extends UIComponent implements ITagsProperty {
	
	private static final long serialVersionUID = 4062617301596626610L;

	public UIText() {
		super();
	}

	@Override
	public UIText clone() throws CloneNotSupportedException {
		UIText cloned = (UIText) super.clone();
		return cloned;
	}

	private MobileSmartSourceType textValue = new MobileSmartSourceType("some text");
	
	public MobileSmartSourceType getTextSmartType() {
		return textValue;
	}

	public void setTextSmartType(MobileSmartSourceType textValue) {
		this.textValue = textValue;
	}

	private boolean i18n = false;

	public boolean isI18n() {
		return i18n;
	}

	public void setI18n(boolean i18n) {
		this.i18n = i18n;
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}
	
    @Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        throw new EngineException("You cannot add to a text component a database object of type " + databaseObject.getClass().getName());
    }

	protected String getTextValue() {
		String value = textValue.getValue();
		if (!Mode.PLAIN.equals(textValue.getMode())) {
			value = "{{" + value + "}}";
		} else if (isI18n()) {
			value = TranslateUtils.htmlIonicTranslate(getProject(), value);
		}
		return value;
	}
	
	protected String getTextLabel() {
		String label = textValue.getLabel();
		if (!Mode.PLAIN.equals(textValue.getMode())) {
			label = "{{" + label + "}}";
		}
		return label;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			return StringEscapeUtils.escapeHtml4(getTextValue()) + System.getProperty("line.separator");
		}
		else {
			return "";
		}
	}

	@Override
	public String toString() {
		String label = getTextLabel();
		return label.isEmpty() ? "?" : label;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("textValue")) {
			return new String[] {""};
		}
		return new String[0];
	}

	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		boolean updated = false;
		String smartValue = textValue.getSmartValue();
		if (smartValue.indexOf(oldString) != -1 || Pattern.compile(oldString).matcher(smartValue).find()) {
			textValue.setSmartValue(smartValue.replaceAll(oldString, newString));
			updated = this.hasChanged = true;
		}
		return updated;
	}
}
