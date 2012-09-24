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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.views.schema;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.schema.model.AllNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.AnnotationNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.AttributeGroupNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.AttributeNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.AttributesFolder;
import com.twinsoft.convertigo.eclipse.views.schema.model.ChoiceNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.ContentNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.DirectivesFolder;
import com.twinsoft.convertigo.eclipse.views.schema.model.ElementNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.ElementsFolder;
import com.twinsoft.convertigo.eclipse.views.schema.model.ExtensionNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.FolderNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.GroupNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.GroupsFolder;
import com.twinsoft.convertigo.eclipse.views.schema.model.ImportNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.IncludeNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.KeyNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.ListNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.RedefineNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.RestrictionNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.SchemaNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.SequenceNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.TreeNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.TypeNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.TypesFolder;
import com.twinsoft.convertigo.eclipse.views.schema.model.UnionNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.UniqueNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.XsdNode;

public class SchemaViewLabelProvider implements ILabelProvider {

	private static Map<String, Image> imagesCache = new HashMap<String, Image>(1024);
	
	public SchemaViewLabelProvider() {
		
	}

	public Image getImage(Object element) {
		String iconName = null;
		if (element instanceof FolderNode) {
			if (element instanceof AttributesFolder) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/attributes_folder.gif";
			}
			else if (element instanceof DirectivesFolder) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/directives_folder.gif";
			}
			else if (element instanceof ElementsFolder) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/elements_folder.gif";
			}
			else if (element instanceof GroupsFolder) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/groups_folder.gif";
			}
			else if (element instanceof TypesFolder) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/types_folder.gif";
			}
		}
		else if (element instanceof XsdNode) {
			XsdNode xsdNode = (XsdNode)element;
			if (element instanceof AllNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/all.gif";
			}
			else if (element instanceof AnnotationNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/annotation.gif";
			}
			else if (element instanceof AttributeNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/attribute.gif";
			}
			else if (element instanceof AttributeGroupNode) {
				if (xsdNode.useRef())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/attributegroup_ref.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/attributegroup.gif";
			}
			else if (element instanceof ChoiceNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/choice.gif";
			}
			else if (element instanceof ContentNode) {
				if (xsdNode.isComplexContent())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/complex_content.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/simple_content.gif";
			}
			else if (element instanceof ElementNode) {
				if (xsdNode.useRef())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/element_ref.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/element.gif";
			}
			else if (element instanceof ExtensionNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/extension.gif";
			}
			else if (element instanceof GroupNode) {
				if (xsdNode.useRef())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/group_ref.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/group.gif";
			}
			else if (element instanceof IncludeNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/include.gif";
			}
			else if (element instanceof ImportNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/import.gif";
			}
			else if (element instanceof KeyNode) {
				if (xsdNode.useRef())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/key_ref.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/key.gif";
			}
			else if (element instanceof ListNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/simple_type_list.gif";
			}
			else if (element instanceof RedefineNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/redefine.gif";
			}
			else if (element instanceof RestrictionNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/restriction.gif";
			}
			else if (element instanceof SchemaNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/schema.gif";
			}
			else if (element instanceof SequenceNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/sequence.gif";
			}
			else if (element instanceof TypeNode) {
				if (xsdNode.isComplexType())
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/complex_type.gif";
				else
					iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/simple_type.gif";
			}
			else if (element instanceof UniqueNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/simple_type_atomic.gif";
			}
			else if (element instanceof UnionNode) {
				iconName = "/com/twinsoft/convertigo/eclipse/views/schema/images/simple_type_union.gif";
			}
		}
		
		if (iconName == null)
			return null;
		Image image = getImageFromCache(iconName, (Object) element);
		return image;
	}

	public static Image getImageFromCache(String iconName, Object object) {
		Image image = imagesCache.get(iconName);
		if (image == null) {
			Device device = Display.getCurrent();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
			image = new Image(device, inputStream);
			
			ImageData imageData = image.getImageData();
			image = new Image(device, imageData);
			
			imagesCache.put(iconName, image);
		}
		return image;
	}
	
	public static Image getDecoratedImageFromCache(String iconName, Object object) {
		return imagesCache.get(iconName);
	}
	
	public static void setDecoratedImageFromCache(String iconName, Image decoratedImage) {
		imagesCache.put(iconName, decoratedImage);
	}
	
	public String getText(Object element) {
		return ((TreeNode) element).getName();
	}

	public void dispose() {

	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void addListener(ILabelProviderListener listener) {

	}

	public void removeListener(ILabelProviderListener listener) {
		
	}

}
