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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

class ViewImageProvider {

	/**
	 * The Hastable cache of loaded images
	 */
	private static Map<String, Image> imagesCache = new HashMap<String, Image>(1024);
	
	/**
	 * Retrieve an image name given an object
	 * 
	 * @param object
	 * @return
	 */
	public static String getImageName(Object object) {
		String imageName = null;
		if (object instanceof DatabaseObjectTreeObject) {
			imageName = ((DatabaseObjectTreeObject)object).getImageName();
		}
		else if (object instanceof PropertyTableTreeObject) {
			imageName = "property";
		}
		else if (object instanceof PropertyTableRowTreeObject) {
			imageName = "property";
		}
		else if (object instanceof PropertyTableColumnTreeObject) {
			imageName = "property";
		}
		else if ((object instanceof VariableTreeObject) || object instanceof VariableTreeObject2) {
			imageName = "variable";
		}
		else {
			try {
				imageName = object.toString();
			}
			catch (Exception e) {
				imageName = "image";
			}
		}
		return imageName;
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
		String imageName = getImageName(object);
		
		Image image = imagesCache.get(imageName);
		if (image == null) {
			Device device = Display.getCurrent();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
			image = new Image(device, inputStream);
			
			ImageData imageData = getImageData(image, object);
			image = new Image(device, imageData);
			
			imagesCache.put(imageName, image);
		}
		return image;
	}
	
	public static Image getImageFromCache(String iconName, Image base, Object object) {
		String imageName = getImageName(object);
		
		Image image = imagesCache.get(imageName);
		if (image == null) {
			Device device = Display.getCurrent();
			
			ImageData imageData = getImageData(base, object);
			image = new Image(device, imageData);
			
			imagesCache.put(imageName, image);
		}
		return image;
	}
	
	public static Image getDecoratedImageFromCache(String imageName, Object object) {
		return imagesCache.get(imageName);
	}
	
	public static void setDecoratedImageFromCache(String imageName, Image decoratedImage) {
		imagesCache.put(imageName, decoratedImage);
	}
	
	/**
	 * Dispose all loaded images
	 */
	/*public static void dispose() {
		String imageName = null;
		Image image = null;
		for (Enumeration e = imagesCache.keys(); e.hasMoreElements(); ) {
			imageName = (String) e.nextElement();
			if (imageName != null) {
				image = (Image) imagesCache.get(imageName);
				imagesCache.put(imageName, null);
			}
			if (image != null)
				image.dispose();
		}
	}*/
	
	private static ImageData getImageData(Image image, Object object) {
		ImageData imageData = image.getImageData();
		
		DatabaseObjectTreeObject databaseObjectTreeObject = null;
		if (object instanceof DatabaseObjectTreeObject) {
			databaseObjectTreeObject = (DatabaseObjectTreeObject)object;
		}
		else if (object instanceof PropertyTableTreeObject) {
			PropertyTableTreeObject table = (PropertyTableTreeObject)object;
			if (table != null) databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
		}
		else if (object instanceof PropertyTableRowTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableRowTreeObject)object).getParentTable();
			if (table != null) databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
		}
		else if (object instanceof PropertyTableColumnTreeObject) {
			PropertyTableTreeObject table = ((PropertyTableColumnTreeObject)object).getParentTable();
			if (table != null) databaseObjectTreeObject = (DatabaseObjectTreeObject)table.getTreeObjectOwner();
		}
		else if (object instanceof FolderTreeObject) {
			FolderTreeObject t_folder = (FolderTreeObject)object;
			if(t_folder.getParent() instanceof DatabaseObjectTreeObject)
				databaseObjectTreeObject = (DatabaseObjectTreeObject) t_folder.getParent();
		}
		if (databaseObjectTreeObject != null) {
			boolean unreachable = databaseObjectTreeObject.hasAncestorDisabled();
			// SWT palette isn't the same on all OS
			if(imageData.palette.colors!=null){
				RGB rgb;
				int irgb;
				int[] hsl;
				int[] _rgb;
				for (int i = 0; i < imageData.palette.colors.length; i ++) {
					if (i == imageData.transparentPixel) continue;
	
					rgb = imageData.palette.colors[i];
	
					if (!databaseObjectTreeObject.isEnabled()) {
						irgb = rgb.red << 16 | rgb.green << 8 | rgb.blue;
				        hsl = hslFloat2Int(rgb2hsl(irgb));
				        _rgb = hsl2rgb(0, 255, hsl[2]);
				        rgb = new RGB(_rgb[0], _rgb[1], _rgb[2]);
					} else if (databaseObjectTreeObject.isInherited) {
						irgb = rgb.red << 16 | rgb.green << 8 | rgb.blue;
				        hsl = hslFloat2Int(rgb2hsl(irgb));
				        _rgb = hsl2rgb(hsl[0], 0, hsl[2]);
				        rgb = new RGB(_rgb[0], _rgb[1], _rgb[2]);
					} else if (databaseObjectTreeObject.isDetectedObject) {
						irgb = rgb.red << 16 | rgb.green << 8 | rgb.blue;
				        hsl = hslFloat2Int(rgb2hsl(irgb));
				        _rgb = hsl2rgb(hsl[0], 0, hsl[2]);
				        rgb = new RGB(_rgb[0], _rgb[1], _rgb[2]);
					} else if (unreachable){
						irgb = rgb.red << 16 | rgb.green << 8 | rgb.blue;
				        hsl = hslFloat2Int(rgb2hsl(irgb));
				        _rgb = hsl2rgb(25, 255, hsl[2]);
				        rgb = new RGB(_rgb[0], _rgb[1], _rgb[2]);			
					}
					
			        imageData.palette.colors[i] = rgb;
				}
			}else{
				int irgb;
				int[] hsl;
				int[] _rgb = null;
				for(int i=0;i<imageData.height;i++){
					for(int j=0;j<imageData.width;j++){
						irgb = imageData.getPixel(j, i);
						
						if (!databaseObjectTreeObject.isEnabled()) {
					        hsl = hslFloat2Int(rgb2hsl(irgb));
					        _rgb = hsl2rgb(0, 255, hsl[2]);
					        irgb = _rgb[0] << 16 |_rgb[1] << 8 | _rgb[2];
						} else if (databaseObjectTreeObject.isInherited) {
					        hsl = hslFloat2Int(rgb2hsl(irgb));
					        _rgb = hsl2rgb(hsl[0], 0, hsl[2]);
					        irgb = _rgb[0] << 16 |_rgb[1] << 8 | _rgb[2];
						} else if (databaseObjectTreeObject.isDetectedObject) {
					        hsl = hslFloat2Int(rgb2hsl(irgb));
					        _rgb = hsl2rgb(0, 255, hsl[2]);
					        irgb = _rgb[0] << 16 |_rgb[1] << 8 | _rgb[2];
						} else if (unreachable){
					        hsl = hslFloat2Int(rgb2hsl(irgb));
					        _rgb = hsl2rgb(25, 255, hsl[2]);
					        irgb = _rgb[0] << 16 |_rgb[1] << 8 | _rgb[2];					
						}
						
				        imageData.setPixel(j, i, irgb);
					}
				}
			}
		}
		
		return imageData;
	}
	
    private static float[] rgb2hsl(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        
        return rgb2hsl(r, g, b);
    }
    
    private static float[] rgb2hsl(int r, int g, int b) {
        float fr = r / 255f;
        float fg = g / 255f;
        float fb = b / 255f;
        
        float min = Math.min(Math.min(fr, fg), fb);
        float max = Math.max(Math.max(fr, fg), fb);

        float l = (min + max) / 2f;

        float h, s;
        if (max - min < 0.001) {
            h = s = 0;
        }
        else {
            if (l < 0.5) {
                s = (max - min) / (max + min);
            }
            else {
                s = (max - min) / (2 - max - min);
            }
            if (fr == max) h = (fg - fb) / (max - min);
            else if (fg == max) h = 2 + (fg - fr) / (max - min);
            else h = 4 + (fr - fg) / (max - min);
        }
        
        float[] hsl = new float[3];
        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
        
        return hsl;
    }

    private static int[] hslFloat2Int(float[] hsl) {
        int hslInt[] = new int[3];
        hslInt[0] = Math.round(hsl[0] * 60f * 255f / 360f);
        hslInt[1] = Math.round(hsl[1] * 255f);
        hslInt[2] = Math.round(hsl[2] * 255f);
        return hslInt;
    }
    
    private static int[] hsl2rgb(int h, int s, int l) {
        float fh = h * 360f / 255f;
        float fs = s / 255f;
        float fl = l / 255f;

        return hsl2rgb(fh, fs, fl);
    }
    
    private static int[] hsl2rgb(float h, float s, float l) {
        float r, g, b;

        if (s == 0f) {
            r = g = b = l;
        }
        else {
            float temp2;
            if (l < 0.5f) {
                temp2 = l * (1f + s);
            }
            else {
                temp2 = l + s - (l * s);
            }
            
            float temp1 = 2f * l - temp2;
            
            h /= 360;
            
            float rtemp3 = h + 1f / 3f;
            if (rtemp3 < 0f) rtemp3++;
            if (rtemp3 > 1f) rtemp3--;
            
            float gtemp3 = h;
            if (gtemp3 < 0f) gtemp3++;
            if (gtemp3 > 1f) gtemp3--;

            float btemp3 = h - 1f / 3f;
            if (btemp3 < 0f) btemp3++;
            if (btemp3 > 1f) btemp3--;

            if (6f * rtemp3 < 1f) r = temp1 + (temp2 - temp1) * 6f * rtemp3;
            else if (2f * rtemp3 < 1f) r = temp2;
            else if (3f * rtemp3 < 2f) r = temp1 + (temp2 - temp1) * (2f / 3f - rtemp3) * 6f;
            else r = temp1;

            if (6f * gtemp3 < 1f) g = temp1 + (temp2 - temp1) * 6f * gtemp3;
            else if (2f * gtemp3 < 1f) g = temp2;
            else if (3f * gtemp3 < 2f) g = temp1 + (temp2 - temp1) * (2f / 3f - gtemp3) * 6f;
            else g = temp1;

            if (6f * btemp3 < 1f) b = temp1 + (temp2 - temp1) * 6f * btemp3;
            else if (2f * btemp3 < 1f) b = temp2;
            else if (3f * btemp3 < 2f) b = temp1 + (temp2 - temp1) * (2f / 3f - btemp3) * 6f;
            else b = temp1;
        }
        
        int[] rgb = new int[3];
        rgb[0] = Math.round(r * 255f);
        rgb[1] = Math.round(g * 255f);
        rgb[2] = Math.round(b * 255f);

        return rgb;
    }

}