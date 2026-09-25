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

package com.twinsoft.convertigo.eclipse.swt;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class SwtUtils {
	static final public String CSS_CLASS_KEY = "org.eclipse.e4.ui.css.CssClassName";

	static public GridLayout newGridLayout(int numColumns, boolean makeColumnsEqualWidth, int horizontalSpacing, int verticalSpacing, int marginWidth, int marginHeight) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = numColumns;
		gridLayout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		gridLayout.horizontalSpacing = horizontalSpacing;
		gridLayout.verticalSpacing = verticalSpacing;
		gridLayout.marginWidth = marginWidth;
		gridLayout.marginHeight = marginHeight;
		return gridLayout;
	}

	/**
	 * Workaround for #1128, observed with SWT Win32 3.132.0.v20251124-0642:
	 * the workbench wraps the CTabFolder view menu onto an otherwise empty row
	 * when a view narrows, although the view's own toolbar still fits on one row.
	 * Recheck after Eclipse/SWT upgrades and remove this helper and its callers
	 * once Assistant/Admin stay on one row without it, including after switching
	 * tabs, resizing and moving views. Uses public SWT APIs and is Windows-only.
	 */
	public static void keepViewMenuOnTabRow(Control viewControl) {
		if (!"win32".equals(SWT.getPlatform())) {
			return;
		}
		boolean[] pending = {false};
		Runnable schedule = () -> {
			if (viewControl.isDisposed() || pending[0]) {
				return;
			}
			pending[0] = true;
			// Run after the workbench has installed or repositioned the view menu.
			viewControl.getDisplay().asyncExec(() -> {
				pending[0] = false;
				if (viewControl.isDisposed() || !viewControl.isVisible()) {
					return;
				}
				for (Composite parent = viewControl.getParent(); parent != null; parent = parent.getParent()) {
					if (parent instanceof CTabFolder folder) {
						Control menu = folder.getTopRight();
						int alignment = folder.getTopRightAlignment();
						// The Windows workbench can wrap its view menu onto an otherwise empty row.
						if (menu != null && !menu.isDisposed() && (alignment & SWT.WRAP) != 0) {
							folder.setTopRight(menu, alignment & ~SWT.WRAP);
						}
						break;
					}
				}
			});
		};
		viewControl.addListener(SWT.Show, event -> schedule.run());
		viewControl.addListener(SWT.Resize, event -> schedule.run());
		viewControl.addListener(SWT.Move, event -> schedule.run());
		schedule.run();
	}

	private static boolean lastDark = false;
	public static boolean isDark() {
		try {
			IThemeEngine themeEngine = (IThemeEngine) Display.getDefault().getData("org.eclipse.e4.ui.css.swt.theme");
			var activeTheme = themeEngine == null ? null : themeEngine.getActiveTheme();
			if (activeTheme != null) {
				String theme = (activeTheme.getId() + " " + activeTheme.getLabel()).toLowerCase(Locale.ROOT);
				if (theme.contains("dark")) {
					return lastDark = true;
				}
				if (theme.contains("light")) {
					return lastDark = false;
				}
			}
		} catch (Exception e) {
		}
		try {
			return lastDark = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell().getBackground().getRed() < 128;
		} catch (Exception e) {
			return lastDark;
		}
	}

	private static void mkDirs(IResource res) throws CoreException {
		if (res instanceof IFile) {
			mkDirs(res.getParent());
		} else if (res instanceof IFolder) {
			if (!res.exists()) {
				mkDirs(res.getParent());
				((IFolder) res).create(true, true, null);
			}
		}
	}

	public static void fillFile(IFile file, String text) {
		try (InputStream is = new ByteArrayInputStream(text.getBytes("UTF-8"))) {
			if (!file.exists()) {
				mkDirs(file);
				file.create(is, true, null);
			} else {
				file.setContents(is, true, false, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void refreshTheme() {
		try {
			IThemeEngine themeEngine = (IThemeEngine) Display.getDefault().getData("org.eclipse.e4.ui.css.swt.theme");
			themeEngine.setTheme(themeEngine.getActiveTheme(), true);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	public static void applyStyle(Control control, String style) {
	    try {
	    	var engine = CompositeElement.getEngine(control);
	        var id = "c8o-style-" + control.hashCode();
	        control.setData("org.eclipse.e4.ui.css.id", id);
	        var cssRule = "#" + id + " { " + style + " }";
	        engine.parseStyleSheet(new StringReader(cssRule));
	        engine.applyStyles(control, true);
	    } catch (Throwable t) {
	        t.printStackTrace();
	    }
	}

	public interface ImageOpener {
		InputStream open(String path) throws IOException;
	}

	public static final ImageOpener fileOpener = path -> new File(path).isFile() ? new FileInputStream(path) : null;

	/**
	 * Creates an image drawn with its double size variant on a HiDPI screen, when the
	 * variant exists: name_32x32.png for name_16x16.png, or name@2x.png for name.png.
	 * Without it, the variant of an svg image is drawn at double size, and the one of
	 * name_NNxNN.png is drawn from its name.svg source.
	 * Returns null when the image cannot be read.
	 */
	public static Image createImage(Device device, String path, ImageOpener opener, UnaryOperator<ImageData> filter) {
		var data = readImageData(path, opener);
		if (data == null) {
			return null;
		}
		var data2x = readImageData(getDoubleSizePath(path), opener);
		if (data2x == null) {
			data2x = drawSvgSource(path, opener, data.width * 2, data.height * 2);
		}
		if (data2x != null && (data2x.width != data.width * 2 || data2x.height != data.height * 2)) {
			data2x = null;
		}
		if (filter != null) {
			data = filter.apply(data);
			if (data2x != null) {
				data2x = filter.apply(data2x);
			}
		}
		var data1x = data;
		var data2 = data2x;
		return new Image(device, (ImageDataProvider) zoom -> zoom == 100 ? data1x : zoom == 200 ? data2 : null);
	}

	public static Image createImage(Device device, String path) {
		return createImage(device, path, ConvertigoPlugin.class::getResourceAsStream, null);
	}

	private static ImageData readImageData(String path, ImageOpener opener) {
		if (path == null) {
			return null;
		}
		try (var is = opener.open(path)) {
			return is == null ? null : new ImageData(is);
		} catch (Exception e) {
			return null;
		}
	}

	private static String getDoubleSizePath(String path) {
		var matcher = sizePattern.matcher(path);
		if (matcher.find()) {
			var size = Integer.toString(Integer.parseInt(matcher.group(1)) * 2);
			return path.substring(0, matcher.start()) + "_" + size + "x" + size + path.substring(matcher.end());
		}
		var dot = path.lastIndexOf('.');
		return dot > path.lastIndexOf('/') ? path.substring(0, dot) + "@2x" + path.substring(dot) : null;
	}

	private static final Pattern sizePattern = Pattern.compile("_(\\d+)x\\1(?=\\.[^./]+$)");

	private static final Pattern svgRootPattern = Pattern.compile("<svg\\b[^>]*>");
	private static final Pattern svgSizePattern = Pattern.compile("\\s(?:width|height)\\s*=\\s*(?:\"[^\"]*\"|'[^']*')");

	/**
	 * Draws the svg image, or the name.svg source of name_NNxNN.png, at width x height,
	 * centered and scaled to fit its viewBox, as the build does when it makes the png from the svg.
	 */
	private static ImageData drawSvgSource(String path, ImageOpener opener, int width, int height) {
		var svgPath = path;
		if (!path.endsWith(".svg")) {
			var matcher = sizePattern.matcher(path);
			if (!matcher.find()) {
				return null;
			}
			svgPath = path.substring(0, matcher.start()) + ".svg";
		}
		try (var is = opener.open(svgPath)) {
			if (is == null) {
				return null;
			}
			var svg = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			var root = svgRootPattern.matcher(svg);
			if (!root.find() || !root.group().contains("viewBox")) {
				return null;
			}
			var sized = "<svg width=\"" + width + "\" height=\"" + height + "\"" + svgSizePattern.matcher(root.group().substring(4)).replaceAll("");
			svg = svg.substring(0, root.start()) + sized + svg.substring(root.end());
			return new ImageData(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			return null;
		}
	}

	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel)bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(
					colorModel.getRedMask(),
					colorModel.getGreenMask(),
					colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					data.setPixel(x, y, pixel);
					if (colorModel.hasAlpha()) {
						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return data;
		}
		else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel)bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
			ComponentColorModel colorModel = (ComponentColorModel)bufferedImage.getColorModel();
			//ASSUMES: 3 BYTE BGR IMAGE TYPE
			PaletteData palette = new PaletteData(0x0000FF, 0x00FF00,0xFF0000);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			//This is valid because we are using a 3-byte Data model with no transparent pixels
			data.transparentPixel = -1;
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		}
		return null;
	}
	
	public interface SelectionListener extends org.eclipse.swt.events.SelectionListener {
		@Override
		default void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	public interface MouseDownListener extends MouseListener {
		@Override
		default void mouseDoubleClick(MouseEvent e) {}

		@Override
		default void mouseUp(MouseEvent e) {}
	}
	
	public static void setToolItemIcon(ToolItem toolItem, String iconPath, String text, String tooltip) {
		try {
			var image = ConvertigoPlugin.getDefault().getStudioIcon(iconPath);
			toolItem.setImage(image);
		} catch (IOException e1) {
			toolItem.setText(text);
		}
		toolItem.setToolTipText(tooltip);
	}

	public static void disposeAllChildren(Composite parent) {
		for (Control control : parent.getChildren()) {
			if (control instanceof Composite composite) {
				disposeAllChildren(composite);
			}
			control.dispose();
		}
	}
}
