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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.beans.BeanInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFunctionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentViewTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.VariableTreeObject2;
import com.twinsoft.convertigo.eclipse.views.references.model.DboNode;
import com.twinsoft.convertigo.engine.util.WeakValueHashMap;

public class ViewImageProvider {

	/**
	 * The Hastable cache of loaded images
	 */
	private static Map<String, Image> imagesCache = new WeakValueHashMap<String, Image>(1024);
	
	/**
	 * Retrieve an image name given an object
	 * 
	 * @param object
	 * @return
	 */
	private static String getImageName(Object object) {
		String imageName = null;
		if (object instanceof DatabaseObject dbo) {
			imageName = MySimpleBeanInfo.getIconName(dbo, BeanInfo.ICON_COLOR_16x16);
		} else if (object instanceof DatabaseObjectTreeObject) {
			imageName = ((DatabaseObjectTreeObject)object).getImageName();
		} else if (object instanceof DboNode) {
			DatabaseObject dbo = ((DboNode) object).getTarget();
			imageName = MySimpleBeanInfo.getIconName(dbo, MySimpleBeanInfo.ICON_COLOR_16x16);
		} else if (object instanceof PropertyTableTreeObject) {
			imageName = "property";
		} else if (object instanceof PropertyTableRowTreeObject) {
			imageName = "property";
		} else if (object instanceof PropertyTableColumnTreeObject) {
			imageName = "property";
		} else if ((object instanceof VariableTreeObject) || object instanceof VariableTreeObject2) {
			imageName = "variable";
		} else {
			try {
				imageName = object.getClass().getCanonicalName();
			}
			catch (Exception e) {
				imageName = "image";
			}
		}
		return imageName;
	}

	public static Image getImageFromCache(String iconName) {
		return getImageFromCache(iconName, null);
	}
	
	/**
	 * Retrieve an image from cache for given object.
	 * Creates it if needed.
	 * 
	 * @param iconName the full path name relative to plugin
	 * @param object
	 * @return
	 */
	public static Image getImageFromCache(String iconName, Object object) {
		String imageName = object == null ? iconName : getImageName(object);

		Image image = imagesCache.get(imageName);
		if (image == null) {
			String defIconName = "/com/twinsoft/convertigo/beans/core/images/default_color_16x16.png";
			if (iconName == null) {
				iconName = imageName;
				if (iconName == null) {
					iconName = defIconName;
				}
			}
			InputStream inputStream = null;
			Device device = Display.getCurrent();
			try {
				if (iconName.startsWith("/com/twinsoft/convertigo/")) {
					inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
					if (inputStream == null) {
						inputStream = ConvertigoPlugin.class.getResourceAsStream(defIconName);
					}
					image = new Image(device, inputStream);
				} else {
					image = new Image(device, iconName);
				}
				
				ImageData imageData = getImageData(image, object);
				image.dispose();
				image = new Image(device, imageData);
				imagesCache.put(imageName, image);
			} catch (Throwable e) {
				System.out.println("Cannot load image " + imageName);
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e1) {
					}
				}
			}
		}
		return image;
	}
	
	static Image getImageFromCache(String imageName, Image base, Object object) {	
		Image image = imagesCache.get(imageName);
		
		if (image == null) {
			Device device = Display.getCurrent();
			
			ImageData imageData = getImageData(base, object);
			image = new Image(device, imageData);
			
			imagesCache.put(imageName, image);
		}
		return image;
	}
	
	private static ImageData getImageData(Image image, Object object) {
		ImageData imageData = image.getImageData();
		
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		
		if (object instanceof DatabaseObjectTreeObject) {
			databaseObjectTreeObject = (DatabaseObjectTreeObject) object;
		} else if (object instanceof PropertyTableTreeObject) {
			PropertyTableTreeObject table = (PropertyTableTreeObject) object;
			if (table != null) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
			}
		} else if (object instanceof PropertyTableRowTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableRowTreeObject) object).getParentTable();
			if (table != null) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject) table.getTreeObjectOwner();
			}
		} else if (object instanceof PropertyTableColumnTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableColumnTreeObject) object).getParentTable();
			if (table != null) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject) table.getTreeObjectOwner();
			}
		} else if (object instanceof FolderTreeObject) {
			FolderTreeObject t_folder = (FolderTreeObject) object;
			if (t_folder.getParent() instanceof DatabaseObjectTreeObject) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject) t_folder.getParent();
			}
		} else if (object instanceof DesignDocumentViewTreeObject) {
			DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)object;
			if (ddvto != null) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject) ddvto.getTreeObjectOwner();
			}
		} else if (object instanceof DesignDocumentFunctionTreeObject) {
			DesignDocumentFunctionTreeObject ddfto = (DesignDocumentFunctionTreeObject)object;
			if (ddfto != null) {
				databaseObjectTreeObject = (DatabaseObjectTreeObject) ddfto.getTreeObjectOwner();
			}
		}
		
		if (databaseObjectTreeObject != null) {
			// SWT palette isn't the same on all OS
			// GIF style
			if(imageData.palette.colors != null) {
				for (int i = 0; i < imageData.palette.colors.length; i ++) {
					if (i != imageData.transparentPixel) {
						RGB rgb = imageData.palette.colors[i];
						
						imageData.palette.colors[i] = changeHue(rgb, databaseObjectTreeObject);
					}
				}
			}
			// PNG style and Linux
			else { 			
				for (int i = 0; i < imageData.height; i++) {
					for (int j = 0; j < imageData.width; j++) {
						RGB rgb = imageData.palette.getRGB(imageData.getPixel(j, i));
						
						imageData.setPixel(j, i, imageData.palette.getPixel(changeHue(rgb,databaseObjectTreeObject)));
					}
				}
			}
		}
		return imageData;
	}	

    private static RGB changeHue(RGB rgb, DatabaseObjectTreeObject databaseObjectTreeObject) {
    	
    	float[] HSB = rgb.getHSB();
    	
    	if (!databaseObjectTreeObject.isEnabled()) {
    		setColor(HSB, 0);
		} else if (databaseObjectTreeObject.isInherited) {
			HSB[1] = 0;
			HSB[2] *= 0.9f;
		} else if (databaseObjectTreeObject.isDetectedObject) {
			setColor(HSB, 120);
		} else if (databaseObjectTreeObject.hasAncestorDisabled()) {
			setColor(HSB, 40);
		} else {
			HSB = null;
		}
    	
    	if(HSB != null) {
    		rgb = new RGB(HSB[0], HSB[1], HSB[2]);
    	}
    	
    	return rgb;
    }
	
	static private void setColor(float[] HSB, float hue) {
		HSB[0] = hue;
		
		if (HSB[1] < 0.2f) {
			HSB[1] = 0.2f;
		} else if (HSB[1] > 0.8f) {
			HSB[1] = 0.8f;
		}
	}    
}