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
	private static Color colorBorder = new Color(1, 143, 208);
	private static Color colorFill = Color.WHITE;

	private static void write(String text, int x, int y, float size, boolean border) {
		GlyphVector gv = font.deriveFont(Font.BOLD, size).createGlyphVector(frc, text);
		Rectangle2D box = gv.getVisualBounds();
		Shape shape = gv.getOutline(x - (int) box.getX(), y - (int) box.getY());
		if (border) {
			g.setStroke(new BasicStroke(size / 7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(colorBorder);
			g.draw(shape);
			g.setColor(colorFill);
			g.fill(shape);
		} else {
			g.setColor(colorFill);
			g.fill(shape);
		}
	}
	
	public static void main(String[] args) throws Exception {		
		String version = System.getProperty("c8o_version", "X.Y.Z");
		String codename = System.getProperty("c8o_codename", "code name");
		String code = System.getProperty("c8o_code", "Cn");
		String copyright = System.getProperty("c8o_copyright", "Copyright © 2001-" + Calendar.getInstance().get(Calendar.YEAR) + " - Convertigo");
		String output = System.getProperty("c8o_output", "splash.bmp");
		
		File output_file = new File(output);
		
		System.out.println("Making Splash : " + output_file.getCanonicalPath());
		System.out.println("Version       : " + version);
		System.out.println("Codename      : " + codename + " (" + code + ")");
		System.out.println("Copyright     : " + copyright);
		
		BufferedImage background = ImageIO.read(new File("convertigo_background.png"));
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
				RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		g.drawImage(background, 0, 0, null);
		g.drawImage(logo, 350, 255, 132, 48, null);
		
		try (FileInputStream fis = new FileInputStream("Interstate Light.ttf")) {
			font = Font.createFont(Font.TRUETYPE_FONT, fis);
		}
		
		write(codename, 270, 175, 42f, true);
		write(code, 280, 225, 25f, true);
		write(version, 340, 230, 13f, true);
		write(copyright, 270, 312, 9.5f, false);
		
		ImageIO.write(splash, "bmp", output_file);
	}

}
