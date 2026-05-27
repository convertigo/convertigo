package com.convertigo.splash;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;

import javax.imageio.ImageIO;

public class MakeSplash {
	private static Font font;
	private static Graphics2D g;
	private static FontRenderContext frc;
	private static Color colorShadow = new Color(0, 18, 55, 190);
	private static Color colorBorder = new Color(175, 225, 255, 210);
	private static Color colorFill = Color.WHITE;

	private static void write(String text, int x, int y, float size, boolean border) {
		GlyphVector gv = font.deriveFont(Font.BOLD, size).createGlyphVector(frc, text);
		Rectangle2D box = gv.getVisualBounds();
		Shape shape = gv.getOutline(x - (int) box.getX(), y - (int) box.getY());
		g.translate(2, 2);
		g.setColor(colorShadow);
		g.fill(shape);
		g.translate(-2, -2);
		if (border) {
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(colorBorder);
			g.draw(shape);
		}
		g.setColor(colorFill);
		g.fill(shape);
	}

	private static void writeCentered(String text, int centerX, int y, float size, boolean border) {
		GlyphVector gv = font.deriveFont(Font.BOLD, size).createGlyphVector(frc, text);
		Rectangle2D box = gv.getVisualBounds();
		write(text, centerX - (int) Math.round(box.getWidth() / 2), y, size, border);
	}
	
	public static void main(String[] args) throws Exception {
		String version = System.getProperty("c8o_version", "X.Y.Z");
		String codename = System.getProperty("c8o_codename", "code name");
		String code = System.getProperty("c8o_code", "Cn");
		String copyright = System.getProperty("c8o_copyright", "Copyright © 2001-" + Calendar.getInstance().get(Calendar.YEAR) + " - Convertigo");
		String outputs = System.getProperty("c8o_outputs", "splash.bmp");
		
		System.out.println("Version       : " + version);
		System.out.println("Codename      : " + codename + " (" + code + ")");
		System.out.println("Copyright     : " + copyright);
		
		BufferedImage background = ImageIO.read(new File("background.png"));
		BufferedImage convertigo = ImageIO.read(new File("convertigo_studio.png"));
		BufferedImage logo = ImageIO.read(new File("eclipse_builton.png"));
		BufferedImage splash = new BufferedImage(
				background.getWidth(),
				background.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		
		g = splash.createGraphics();
		frc = g.getFontRenderContext();
		
		g.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		int width = background.getWidth();
		int height = background.getHeight();
		float scale = width / 500f;
		int bottomBandCenterY = Math.round(height - 54 * scale);
		int studioWidth = Math.round(320 * scale);
		int studioHeight = Math.round(137 * scale);
		int logoWidth = Math.round(132 * scale);
		int logoHeight = Math.round(48 * scale);
		g.drawImage(background, 0, 0, null);
		g.drawImage(convertigo, (width - studioWidth) / 2, Math.round(92 * scale), studioWidth, studioHeight, null);
		g.drawImage(logo, width - Math.round(145 * scale), bottomBandCenterY - logoHeight / 2, logoWidth, logoHeight, null);
		
		try (FileInputStream fis = new FileInputStream("Interstate Light.ttf")) {
			font = Font.createFont(Font.TRUETYPE_FONT, fis);
		}
		
		writeCentered(codename, width / 2, Math.round(bottomBandCenterY - 24 * scale), 34f * scale, true);
		writeCentered(code + "   " + version, width / 2, Math.round(bottomBandCenterY + 7 * scale), 17f * scale, false);
		writeCentered(copyright, width / 2, Math.round(height - 27 * scale), 11f * scale, false);
		
		for (String output: outputs.split(":")) {
			File output_file = new File(output);
			System.out.println("Making Splash : " + output_file.getCanonicalPath());
			String ext = output.replaceFirst(".*\\.(.*?)$", "$1");
			ImageIO.write(splash, ext, output_file);
		}
	}

}
