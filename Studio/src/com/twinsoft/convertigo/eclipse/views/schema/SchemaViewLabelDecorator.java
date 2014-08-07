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

import java.util.HashMap;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SchemaViewLabelDecorator implements ILabelDecorator {
	private static Map<String, Image> imagesCache = new HashMap<String, Image>();
	
	public SchemaViewLabelDecorator() {
	}

	public Image decorateImage(Image image, Object element) {
		Image decoratedImage = image;
		
		if (element == null) {
			decoratedImage = null;
		} else if (element instanceof XmlSchemaParticle) {
			XmlSchemaParticle particle = (XmlSchemaParticle) element;
			
			long min = particle.getMinOccurs();
			long max = particle.getMaxOccurs();
			
			String occur = null;
			if (min == 0) {
				occur = "zero";
				if (max > 0) {
					occur += "_" + (max == 1 ? "one" : max == Long.MAX_VALUE ? "unbounded" : "n");
				}
			} else if (min == 1) {
				if (max > 1) {
					occur = "one_" + (max == Long.MAX_VALUE ? "unbounded" : "n");
				}
			} else {
				occur = "n";
				if (max > min) {
					occur += "_" + (max == Long.MAX_VALUE ? "unbounded" : "m");
				}
			}
			if (occur != null) {
				decoratedImage = getOverlayImageOccur(decoratedImage, element, occur);
			}			
		} else if (element instanceof XmlSchemaAttribute) {
			XmlSchemaAttribute attribute = (XmlSchemaAttribute) element;
			XmlSchemaUse use = attribute.getUse();
			if (use.equals(XmlSchemaUtils.attributeUseOptional)) {
				decoratedImage = getOverlayImageOccur(decoratedImage, element, "zero_one");
			}
		}
		
		return decoratedImage;
	}
	
	private Image getOverlayImageOccur(Image image, Object element, String occur) {
		Image decoratedImage;
		occur = "occurrence_" + occur + ".gif";
//		String cacheName = element.getClass().getSimpleName() + "_" + occur;
		String cacheName = SchemaViewLabelProvider.getKey(element) + "_" + occur;
		Image cachedImage = imagesCache.get(cacheName);
		if (cachedImage == null) {
			decoratedImage = getOverlayImageIcon(image, occur, OverlayImageIcon.BOTTOM_RIGHT);
			imagesCache.put(cacheName, decoratedImage);
		} else {
			decoratedImage = cachedImage;
		}
		return decoratedImage;
	}
	
	private Image getOverlayImageIcon(Image image, String iconName, int corner) {
		ImageDescriptor overlayDescriptor = ImageDescriptor.createFromFile(SchemaViewLabelDecorator.class, "images/"+iconName);
		OverlayImageIcon overlayImageIcon = new OverlayImageIcon(image, overlayDescriptor, corner);
		Image OverlayImageIcon = overlayImageIcon.createImage();
		return OverlayImageIcon;
	}
	
	public String decorateText(String text, Object element) {
		String decoratedText = text;
		if (element != null) {
			if (element instanceof XmlSchemaObject) {
				XmlSchemaObject xso = (XmlSchemaObject) element;
				
				XmlSchemaType type = null;
				String value = null;
				if (element instanceof XmlSchemaElement) {
					type = SchemaMeta.getType(xso, ((XmlSchemaElement) element).getSchemaTypeName());
				} else if (element instanceof XmlSchemaAttribute) {
					XmlSchemaAttribute attr = (XmlSchemaAttribute) element;
					type = SchemaMeta.getType(xso, attr.getSchemaTypeName());	
					value = attr.getDefaultValue();
					value = value == null ? attr.getFixedValue() : "default:" + value;
				} else if (element instanceof XmlSchemaSimpleContentExtension) {
					type = SchemaMeta.getType(xso, ((XmlSchemaSimpleContentExtension) element).getBaseTypeName());
				} else if (element instanceof XmlSchemaSimpleTypeRestriction) {
					type = SchemaMeta.getType(xso, ((XmlSchemaSimpleTypeRestriction) element).getBaseTypeName());
				} else if (element instanceof XmlSchemaEnumerationFacet) {
					XmlSchemaEnumerationFacet enumerationFacet = (XmlSchemaEnumerationFacet) element;
					decoratedText += " [" + enumerationFacet.getValue() + "]";
				}

				if (value != null) {
					decoratedText += " {" + value + "}";
				}
				
				if (type != null && type instanceof XmlSchemaSimpleType) {
					decoratedText += " [" + SchemaMeta.getPrefix(type) + ":" + type.getName() + "]";
				}
				
				int size = SchemaMeta.getReferencedDatabaseObjects(xso).size();
				
				if (size > 1 || size >= 0 && element instanceof XmlSchemaComplexType && ((XmlSchemaComplexType) element).getName() != null) {
					decoratedText += " (" + size + ")";
				}
				
				String prefix = SchemaMeta.getPrefix(xso);
				if (prefix != null) {
					decoratedText = prefix + ":" + decoratedText;
				}
			}
		}
		return decoratedText;
	}
	
	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}
	
	/**
	 * 
	 * OverlayImageIcon class
	 *
	 */
	class OverlayImageIcon extends CompositeImageDescriptor {
		
		private static final int TOP_LEFT = 0;
		private static final int TOP_RIGHT = 1;
		private static final int BOTTOM_LEFT = 2;
		private static final int BOTTOM_RIGHT = 3;
		
		private Image mBase;
		private Image mOverlay;
		private int mCorner;
		
		public OverlayImageIcon(Image baseImage, ImageDescriptor overlay) {
			this(baseImage, overlay, BOTTOM_RIGHT);
		}
	
		public OverlayImageIcon(Image baseImage, ImageDescriptor overlay, int corner) {
			mCorner = corner;
			mBase = baseImage;
			if (overlay != null)
				mOverlay = overlay.createImage();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
		 */
		protected void drawCompositeImage(int width, int height) {

			drawImage(mBase.getImageData(), 0, 0);
			
			ImageData imageData = mOverlay.getImageData();
			switch (mCorner) {
				case TOP_LEFT :
					drawImage(imageData, 0, 0);
					break;
				case TOP_RIGHT :
					drawImage(imageData, mBase.getBounds().width - imageData.width, 0);
					break;
				case BOTTOM_LEFT :
					drawImage(imageData, 0, mBase.getBounds().height - imageData.height);
					break;
				case BOTTOM_RIGHT :
					drawImage(imageData, mBase.getBounds().width - imageData.width, mBase.getBounds().height - imageData.height);
					break;
			}
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
		 */
		protected Point getSize() {
			Point size = null;
			if (mBase != null) {
				size = new Point(mBase.getBounds().width, mBase.getBounds().height);
			}
			return size;
		}
	}

}
