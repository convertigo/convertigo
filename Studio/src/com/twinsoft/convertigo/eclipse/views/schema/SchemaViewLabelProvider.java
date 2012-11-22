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

package com.twinsoft.convertigo.eclipse.views.schema;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewContentProvider.NamedList;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class SchemaViewLabelProvider implements ILabelProvider, IColorProvider {
	private static Map<String, Image> imagesCache = new HashMap<String, Image>();
	
	public SchemaViewLabelProvider() {
	}

	public void dispose() {
	}

	public Image getImage(Object element) {
		String key = getKey(element);
		Image image = imagesCache.get(key);
		if (image == null) {
			String iconName = key;
			Device device = Display.getCurrent();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream("/com/twinsoft/convertigo/eclipse/views/schema/images/" + iconName + ".gif");
			if (inputStream == null) {
				inputStream = ConvertigoPlugin.class.getResourceAsStream("/com/twinsoft/convertigo/eclipse/views/schema/images/unresolved.gif");
			}
			image = new Image(device, inputStream);
			
			imagesCache.put(key, image);
		}
		return image;
	}
	
	public static String getKey(Object element) {
		String key = element.getClass().getSimpleName();
		if (element instanceof NamedList) {
			key = ((NamedList) element).getName().toLowerCase() + "_folder";
		} else if (element instanceof XmlSchema) {
			key = "schema";
		} else if (element instanceof XmlSchemaDocumentation || element instanceof XmlSchemaAppInfo) {
			key = "notation";
		}  else if (element instanceof XmlSchemaObject) {
			key = key.contains("Extension") ?
				"extension" :
				key.substring(9).replaceAll("(\\p{Upper})", "_$1").toLowerCase().substring(1);
			
			if (element instanceof XmlSchemaElement) {
				key += ((XmlSchemaElement)element).getRefName() != null ? "_ref":"";
			}
			else if (element instanceof XmlSchemaAttribute) {
				key += ((XmlSchemaAttribute)element).getRefName() != null ? "_ref":"";
			}
		} else {
			key = "unresolved";
		}
		return key;
	}
	
	public String getText(Object element) {
		String txt = null;
		if (element instanceof XmlSchema) {
			XmlSchema schema = (XmlSchema) element;
			String prefix = SchemaMeta.getPrefix(schema);
			txt = prefix + "{" + schema.getTargetNamespace() + "}";
		} else if (element instanceof XmlSchemaImport) {
			txt = "import " + getText(((XmlSchemaImport) element).getSchema());
		} else if (element instanceof XmlSchemaInclude) {
			//txt = "include " + "(" +((XmlSchemaInclude) element).getSchemaLocation() +")";
			txt = "include " + getText(((XmlSchemaInclude) element).getSchema());
		} else if (element instanceof XmlSchemaDocumentation || element instanceof XmlSchemaAppInfo) {
			NodeList nl = element instanceof XmlSchemaDocumentation ? ((XmlSchemaDocumentation) element).getMarkup() : ((XmlSchemaAppInfo) element).getMarkup();
			if (nl != null && nl.getLength() > 0) {
				txt = nl.item(0).getTextContent();
			} else {
				txt = "...";	
			}
		}  else if (element instanceof XmlSchemaObject) {
			if (element instanceof XmlSchemaElement) {
				if (((XmlSchemaElement) element).getRefName() == null) {
					txt = ((XmlSchemaElement) element).getName();
				}
			} else if (element instanceof XmlSchemaAttribute) {
				txt = ((XmlSchemaAttribute) element).getName();
			} else if (element instanceof XmlSchemaAttributeGroup) {
				txt = ((XmlSchemaAttributeGroup) element).getName().getLocalPart();
			} else if (element instanceof XmlSchemaGroup) {
				txt = ((XmlSchemaGroup)element).getName().getLocalPart();
			} else if (element instanceof XmlSchemaType) {
				XmlSchemaType type = (XmlSchemaType) element;
				String name = type.getName();
				if (name != null) {
					txt = name;
				}
			}
			if (txt == null) {
				txt = element.getClass().getSimpleName().substring(9);
				txt = "xsd:" + Character.toLowerCase(txt.charAt(0)) + txt.substring(1);
			}
		} else if (element instanceof NamedList) {
			txt = ((NamedList) element).getName();
		} else {
			txt = "<?>";
		}
		return txt;
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void addListener(ILabelProviderListener listener) {

	}

	public void removeListener(ILabelProviderListener listener) {
		
	}

	public Color getForeground(Object element) {
		if (element instanceof XmlSchemaObject) {
			XmlSchemaObject xso = (XmlSchemaObject) element;
			return Display.getCurrent().getSystemColor(
					SchemaMeta.isDynamic(xso) ?
							(SchemaMeta.isReadOnly(xso) ? SWT.COLOR_DARK_BLUE : SWT.COLOR_DARK_GREEN)
							: SWT.COLOR_DARK_GRAY
			);
		}
		return null;
	}

	public Color getBackground(Object element) {
		return null;
	}
}
