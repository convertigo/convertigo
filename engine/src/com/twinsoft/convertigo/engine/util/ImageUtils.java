/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.twinsoft.convertigo.engine.Engine;

public class ImageUtils {
	public static boolean pngToJpg(File pngFile, File jpgFile) {
		BufferedImage bufferedImage;

		try {
			//read image file
			bufferedImage = ImageIO.read(pngFile);

			// create a blank, RGB, same width and height, and a white background
			BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
					bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);

			// write to jpeg file
			ImageIO.write(newBufferedImage, "jpg", jpgFile);
		} catch (IOException e) {
			Engine.logEngine.debug("(ImageUtils) Failed to convert png to jpg file" , e);
			return false;
		}
		return true;
	}
}
