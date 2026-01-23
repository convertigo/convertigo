/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboFolderType(type = FolderType.STYLE)
public class UIStyle extends UIComponent {

	private static final long serialVersionUID = -2762443938903661515L;

	public UIStyle() {
		super();
	}

	@Override
	public UIStyle clone() throws CloneNotSupportedException {
		UIStyle cloned = (UIStyle) super.clone();
		return cloned;
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}
	
    @Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        throw new EngineException("You cannot add to a custom component a database object of type " + databaseObject.getClass().getName());
    }

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		try {
			NodeList properties = element.getElementsByTagName("property");
			
			// migration of styleContent from String to FormatedContent
			Element propElement = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "styleContent");
			if (propElement != null) {
				Element valueElement = (Element) XMLUtils.findChildNode(propElement, Node.ELEMENT_NODE);
				if (valueElement != null) {
					Document document = valueElement.getOwnerDocument();
					Object content = XMLUtils.readObjectFromXml(valueElement);
					if (content instanceof String) {
						FormatedContent formated = new FormatedContent((String) content);
						Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, formated);
						propElement.replaceChild(newValueElement, valueElement);
						hasChanged = true;
						Engine.logBeans.warn("(UIStyle) 'styleContent' has been updated for the object \"" + getName() + "\"");
					}
				}
			}
		}
        catch(Exception e) {
            throw new EngineException("Unable to preconfigure the style component \"" + getName() + "\".", e);
        }
	}
    
	protected FormatedContent styleContent = new FormatedContent("");

	public FormatedContent getStyleContent() {
		return styleContent;
	}

	public void setStyleContent(FormatedContent styleContent) {
		this.styleContent = styleContent;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String computedStyle = styleContent.getString();
			if (!computedStyle.isEmpty())
				return computedStyle;
		}
		return "";
	}
}
