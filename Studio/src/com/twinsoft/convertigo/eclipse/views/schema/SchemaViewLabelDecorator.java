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

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import com.twinsoft.convertigo.eclipse.views.schema.model.XsdNode;

class SchemaViewLabelDecorator implements ILabelDecorator {
	
	public SchemaViewLabelDecorator() {
	}

	public Image decorateImage(Image image, Object element) {
		Image decoratedImage = image;
			
		if (element == null) {
			return null;
		}
		
		if (element instanceof XsdNode) {
			XsdNode xsdNode = (XsdNode)element;
			if (xsdNode.hasOccurs()) {
				decoratedImage = getDecoratedImageFromCache(image, xsdNode);
			}
		}
		
		return decoratedImage;
	}

	private Image getDecoratedImageFromCache(Image image, Object element) {
		String decoratedImageName = getDecoratedImageName(element);
		
		Image decoratedImage = SchemaViewLabelProvider.getDecoratedImageFromCache(decoratedImageName, element);
		if (decoratedImage == null) {
			decoratedImage = image;
			if (element instanceof XsdNode) {
				if (((XsdNode)element).hasOccurs()) {
					String overLayImageName = getOccurenceImageName(element);
					decoratedImage = getOverlayImageIcon(decoratedImage, overLayImageName, OverlayImageIcon.BOTTOM_RIGHT);
				}
			}
			SchemaViewLabelProvider.setDecoratedImageFromCache(decoratedImageName, decoratedImage);
		}

		return decoratedImage;
	}
	
	private String getOccurenceImageName(Object element) {
		if (element instanceof XsdNode) {
			XsdNode xsdNode = (XsdNode)element;
			if (xsdNode.hasOccurs()) {
				String imageName = "occurrence";
				if (xsdNode.getObject().hasAttribute("minOccurs")) {
					String minOccurs = xsdNode.getObject().getAttribute("minOccurs");
					if (minOccurs.equals("0")) imageName += "_zero";
					else if (minOccurs.equals("1")) imageName += "_one";
					else imageName += "_n";
				}
				else 
					imageName += "_one";
				
				if (xsdNode.getObject().hasAttribute("maxOccurs")) {
					String maxOccurs = xsdNode.getObject().getAttribute("maxOccurs");
					if (maxOccurs.equals("1")) imageName += "_one";
					else if (maxOccurs.equals("unbounded")) imageName += "_unbounded";
					else imageName += (imageName.indexOf("_n") != -1) ? "_m":"_n";
					
				}
				return imageName += ".gif";
			}
		}
		return null;
	}
	
	private Image getOverlayImageIcon(Image image, String iconName, int corner) {
		ImageDescriptor overlayDescriptor = ImageDescriptor.createFromFile(SchemaViewLabelDecorator.class, "images/"+iconName);
		OverlayImageIcon overlayImageIcon = new OverlayImageIcon(image, overlayDescriptor, corner);
		Image OverlayImageIcon = overlayImageIcon.createImage();
		return OverlayImageIcon;
	}
	
	private String getDecoratedImageName(Object element) {
		String decoratedImageName = element.getClass().getSimpleName();
		if (element instanceof XsdNode) {
			if (((XsdNode)element).hasOccurs()) {
				decoratedImageName += "_" + getOccurenceImageName(element);
			}
		}
		return decoratedImageName;
	}
	
	public String decorateText(String text, Object element) {
		if (element == null) {
			return null;
		}
		
		if (element instanceof XsdNode) {
			XsdNode xsdNode = (XsdNode)element;
			if (xsdNode.useType()) {
				return text + " : " + xsdNode.getObject().getAttribute("type");
			}
		}
		return null;
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
