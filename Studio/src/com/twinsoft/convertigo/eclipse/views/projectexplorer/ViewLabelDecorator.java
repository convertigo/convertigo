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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

class ViewLabelDecorator implements ILabelDecorator {
	
	public ViewLabelDecorator() {
	}

	public Image decorateImage(Image image, Object element) {
		Image decoratedImage = image;
			
		if (element == null) {
			return null;
		}
		
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)element;
			if (!databaseObjectTreeObject.isInherited) {
				decoratedImage = getDecoratedImageFromCache(image, databaseObjectTreeObject);
			}
		}
		
		return decoratedImage;
	}

	public Image getDecoratedImageFromCache(Image image, Object element) {
		String decoratedImageName = getDecoratedImageName(element);
		
		Image decoratedImage = ViewImageProvider.getDecoratedImageFromCache(decoratedImageName, element);
		if (decoratedImage == null) {
			if (element instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)element;
				decoratedImage = image;
				
				if (databaseObjectTreeObject.isDefault) {
					decoratedImage = getOverlayImageIcon(decoratedImage, "bean_default.gif", OverlayImageIcon.BOTTOM_RIGHT);
				}
				
				if (databaseObjectTreeObject.isUnderCvs) {
					decoratedImage = getOverlayImageIcon(decoratedImage, (databaseObjectTreeObject.isCheckedOut ? "bean_checkedout.gif":"bean_checkedin.gif"), OverlayImageIcon.TOP_LEFT);
				}
				else {
					decoratedImage = getOverlayImageIcon(decoratedImage, "bean_nocvs.gif", OverlayImageIcon.TOP_LEFT);
				}

				//if (databaseObjectTreeObject.isLinked) {
				//	decoratedImage = getOverlayImageIcon(decoratedImage, "bean_linked.gif", OverlayImageIcon.TOP_RIGHT);
				//}
				
				ViewImageProvider.setDecoratedImageFromCache(decoratedImageName, decoratedImage);
			}
		}

		return decoratedImage;
	}
	
	private Image getOverlayImageIcon(Image image, String iconName, int corner) {
		ImageDescriptor overlayDescriptor = ImageDescriptor.createFromFile(ViewImageProvider.class, iconName);
		OverlayImageIcon overlayImageIcon = new OverlayImageIcon(image, overlayDescriptor, corner);
		Image OverlayImageIcon = overlayImageIcon.createImage();
		return OverlayImageIcon;
	}
	
	public String getDecoratedImageName(Object element) {
		String decoratedImageName = null;
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)element;
			decoratedImageName = databaseObjectTreeObject.getImageName();
			if (!databaseObjectTreeObject.isInherited) {
				String cvsStatus = (databaseObjectTreeObject.isCheckedOut ? "_cvsout":"_cvsin");

				decoratedImageName += (databaseObjectTreeObject.isDefault ? "_default":"");
				decoratedImageName += (databaseObjectTreeObject.isUnderCvs ? cvsStatus:"_nocvs");
				//decoratedImageName += (databaseObjectTreeObject.isLinked ? "_linked":"");
			}
		}
		else {
			try {
				decoratedImageName = element.toString();
			}
			catch (Exception e) {
				decoratedImageName = "image";
			}
		}
		return decoratedImageName;
	}
	
	public String decorateText(String text, Object element) {
		if (element == null) {
			return null;
		}
		
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)element;
			if (databaseObjectTreeObject.isCheckedOut) {
				return text + " (somebody)";
			}
		}
		return null;
	}
	
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void dispose() {
//		 TODO Auto-generated method stub
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
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
