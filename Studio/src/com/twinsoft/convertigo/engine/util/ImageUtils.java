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
