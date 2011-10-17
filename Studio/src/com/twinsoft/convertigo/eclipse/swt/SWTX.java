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

package com.twinsoft.convertigo.eclipse.swt;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * @author Kosta, Friederich Kupzog
 */
public class SWTX {
	  public static final int EVENT_SWTX_BASE = 1000;
	
	  public static final int EVENT_TABLE_HEADER = EVENT_SWTX_BASE + 1;
	
	  public static final int EVENT_TABLE_HEADER_CLICK = EVENT_SWTX_BASE + 2;
	
	  public static final int EVENT_TABLE_HEADER_RESIZE = EVENT_SWTX_BASE + 3;
	
	  //
	  public static final int ALIGN_HORIZONTAL_MASK = 0x0F;
	
	  public static final int ALIGN_HORIZONTAL_NONE = 0x00;
	
	  public static final int ALIGN_HORIZONTAL_LEFT = 0x01;
	
	  public static final int ALIGN_HORIZONTAL_LEFT_LEFT = ALIGN_HORIZONTAL_LEFT;
	
	  public static final int ALIGN_HORIZONTAL_LEFT_RIGHT = 0x02;
	
	  public static final int ALIGN_HORIZONTAL_LEFT_CENTER = 0x03;
	
	  public static final int ALIGN_HORIZONTAL_RIGHT = 0x04;
	
	  public static final int ALIGN_HORIZONTAL_RIGHT_RIGHT = ALIGN_HORIZONTAL_RIGHT;
	
	  public static final int ALIGN_HORIZONTAL_RIGHT_LEFT = 0x05;
	
	  public static final int ALIGN_HORIZONTAL_RIGHT_CENTER = 0x06;
	
	  public static final int ALIGN_HORIZONTAL_CENTER = 0x07;
	
	  public static final int ALIGN_VERTICAL_MASK = 0xF0;
	
	  public static final int ALIGN_VERTICAL_TOP = 0x10;
	
	  public static final int ALIGN_VERTICAL_BOTTOM = 0x20;
	
	  public static final int ALIGN_VERTICAL_CENTER = 0x30;
	
	  //
	  private static GC m_LastGCFromExtend;
	
	  private static Map<String, Point> m_StringExtentCache = new HashMap<String, Point>();
	
	  private static synchronized Point getCachedStringExtent(GC gc, String text) {
	    if (m_LastGCFromExtend != gc) {
	      m_StringExtentCache.clear();
	      m_LastGCFromExtend = gc;
	    }
	    Point p = m_StringExtentCache.get(text);
	    if (p == null) {
	      if (text == null)
	        return new Point(0, 0);
	      p = gc.stringExtent(text);
	      m_StringExtentCache.put(text, p);
	    }
	    return new Point(p.x, p.y);
	  }
	
	  public static int drawTextVerticalAlign(GC gc, String text, int textAlign,
	      int x, int y, int w, int h) {
	    if (text == null)
	      text = "";
	Point textSize = getCachedStringExtent(gc, text);
	{
	  boolean addPoint = false;
	  while ((text.length() > 0) && (textSize.x >= w)) {
	    text = text.substring(0, text.length() - 1);
	    textSize = getCachedStringExtent(gc, text + "...");
	    addPoint = true;
	  }
	  if (addPoint)
	    text = text + "...";
	  textSize = getCachedStringExtent(gc, text);
	  if (textSize.x >= w) {
	    text = "";
	    textSize = getCachedStringExtent(gc, text);
	  }
	}
	//
	if ((textAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_TOP) {
	  gc.drawText(text, x, y);
	  gc.fillRectangle(x, y + textSize.y, textSize.x, h - textSize.y);
	  return textSize.x;
	}
	if ((textAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_BOTTOM) {
	  gc.drawText(text, x, y + h - textSize.y);
	  gc.fillRectangle(x, y, textSize.x, h - textSize.y);
	  return textSize.x;
	}
	if ((textAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_CENTER) {
	  int yOffset = (h - textSize.y) / 2;
	  gc.drawText(text, x, y + yOffset);
	  gc.fillRectangle(x, y, textSize.x, yOffset);
	  gc.fillRectangle(x, y + yOffset + textSize.y, textSize.x, h
	      - (yOffset + textSize.y));
	  return textSize.x;
	}
	throw new SWTException(
	    "H: "
	            + (textAlign & ALIGN_VERTICAL_MASK));
	  }
	
	  public static void drawTransparentImage(GC gc, Image image, int x, int y) {
	    if (image == null)
	      return;
	    Point imageSize = new Point(image.getBounds().width,
	        image.getBounds().height);
	    Image img = new Image(Display.getCurrent(), imageSize.x, imageSize.y);
	    GC gc2 = new GC(img);
	    gc2.setBackground(gc.getBackground());
	    gc2.fillRectangle(0, 0, imageSize.x, imageSize.y);
	    gc2.drawImage(image, 0, 0);
	    gc.drawImage(img, x, y);
	    gc2.dispose();
	    img.dispose();
	  }
	
	  public static void drawImageVerticalAlign(GC gc, Image image,
	      int imageAlign, int x, int y, int h) {
	    if (image == null)
	      return;
	    Point imageSize = new Point(image.getBounds().width,
	        image.getBounds().height);
	    //
	if ((imageAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_TOP) {
	  drawTransparentImage(gc, image, x, y);
	  gc.fillRectangle(x, y + imageSize.y, imageSize.x, h - imageSize.y);
	  return;
	}
	if ((imageAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_BOTTOM) {
	  drawTransparentImage(gc, image, x, y + h - imageSize.y);
	  gc.fillRectangle(x, y, imageSize.x, h - imageSize.y);
	  return;
	}
	if ((imageAlign & ALIGN_VERTICAL_MASK) == ALIGN_VERTICAL_CENTER) {
	  int yOffset = (h - imageSize.y) / 2;
	  drawTransparentImage(gc, image, x, y + yOffset);
	  gc.fillRectangle(x, y, imageSize.x, yOffset);
	  gc.fillRectangle(x, y + yOffset + imageSize.y, imageSize.x, h
	      - (yOffset + imageSize.y));
	  return;
	}
	throw new SWTException(
	    "H: "
	            + (imageAlign & ALIGN_VERTICAL_MASK));
	  }
	
	  public static void drawTextImage(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h) {
	    Point textSize = getCachedStringExtent(gc, text);
	    Point imageSize;
	    if (image != null)
	      imageSize = new Point(image.getBounds().width,
	          image.getBounds().height);
	    else
	      imageSize = new Point(0, 0);
	    //
	/*
	 * Rectangle oldClipping = gc.getClipping(); gc.setClipping(x, y, w, h);
	 */
	try {
	  if ((image == null)
	      && ((textAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_CENTER)) {
	    Point p = getCachedStringExtent(gc, text);
	    int offset = (w - p.x) / 2;
	    if (offset > 0) {
	      drawTextVerticalAlign(gc, text, textAlign, x + offset, y, w
	          - offset, h);
	      gc.fillRectangle(x, y, offset, h);
	      gc
	          .fillRectangle(x + offset + p.x, y, w
	              - (offset + p.x), h);
	    } else {
	      p.x = drawTextVerticalAlign(gc, text, textAlign, x, y, w, h);
	      // gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
	  gc.fillRectangle(x + p.x, y, w - (p.x), h);
	  // offset = (w - p.x) / 2;
	  // gc.fillRectangle(x, y, offset, h);
	  // gc.fillRectangle(x + offset + p.x, y, w - (offset + p.x),
	  // h);
	    }
	    return;
	  }
	  if (((text == null) || (text.length() == 0))
	      && ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_CENTER)) {
	    int offset = (w - imageSize.x) / 2;
	    // System.out.println("w: " + w + " imageSize" + imageSize + "
	// offset: " + offset);
	    drawImageVerticalAlign(gc, image, imageAlign, x + offset, y, h);
	    gc.fillRectangle(x, y, offset, h);
	    gc.fillRectangle(x + offset + imageSize.x, y, w
	        - (offset + imageSize.x), h);
	    return;
	  }
	  if ((textAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_LEFT) {
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_NONE) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          y, w, h);
	      gc.fillRectangle(x + textSize.x, y, w - textSize.x, h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_LEFT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x
	          + imageSize.x, y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x, y, h);
	      gc.fillRectangle(x + textSize.x + imageSize.x, y, w
	          - (textSize.x + imageSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_RIGHT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x + w
	          - imageSize.x, y, h);
	      gc.fillRectangle(x + textSize.x, y, w
	          - (textSize.x + imageSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_RIGHT_LEFT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x
	          + textSize.x, y, h);
	      gc.fillRectangle(x + textSize.x + imageSize.x, y, w
	          - (textSize.x + imageSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_RIGHT_CENTER) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          y, w - imageSize.x, h);
	      int xOffset = (w - textSize.x - imageSize.x) / 2;
	      drawImageVerticalAlign(gc, image, imageAlign, x
	          + textSize.x + xOffset, y, h);
	      gc.fillRectangle(x + textSize.x, y, xOffset, h);
	      gc.fillRectangle(x + textSize.x + xOffset + imageSize.x, y,
	          w - (textSize.x + xOffset + imageSize.x), h);
	      return;
	    }
	    throw new SWTException(
	        "H: "
	            + (imageAlign & ALIGN_HORIZONTAL_MASK));
	  } // text align left
	  if ((textAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_RIGHT) {
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_NONE) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          -1000, w, h);
	      drawTextVerticalAlign(gc, text, textAlign, x + w
	          - textSize.x, y, w, h);
	      gc.fillRectangle(x, y, w - textSize.x, h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_LEFT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          -1000, w - imageSize.x, h);
	      drawTextVerticalAlign(gc, text, textAlign, x + w
	          - textSize.x, y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x, y, h);
	      gc.fillRectangle(x + imageSize.x, y, w
	          - (textSize.x + imageSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_LEFT_RIGHT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          -1000, w - imageSize.x, h);
	      drawTextVerticalAlign(gc, text, textAlign, x + w
	          - textSize.x, y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x + w
	          - (textSize.x + imageSize.x), y, h);
	      gc.fillRectangle(x, y, w - (textSize.x + imageSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_LEFT_CENTER) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          -1000, w - imageSize.x, h);
	      drawTextVerticalAlign(gc, text, textAlign, x + w
	          - textSize.x, y, w - imageSize.x, h);
	      int xOffset = (w - textSize.x - imageSize.x) / 2;
	      drawImageVerticalAlign(gc, image, imageAlign, x + xOffset,
	          y, h);
	      gc.fillRectangle(x, y, xOffset, h);
	      gc.fillRectangle(x + xOffset + imageSize.x, y, w
	          - (xOffset + imageSize.x + textSize.x), h);
	      return;
	    }
	    if ((imageAlign & ALIGN_HORIZONTAL_MASK) == ALIGN_HORIZONTAL_RIGHT) {
	      textSize.x = drawTextVerticalAlign(gc, text, textAlign, x,
	          -1000, w - imageSize.x, h);
	      drawTextVerticalAlign(gc, text, textAlign, x + w
	          - (textSize.x + imageSize.x), y, w - imageSize.x, h);
	      drawImageVerticalAlign(gc, image, imageAlign, x + w
	          - imageSize.x, y, h);
	      gc.fillRectangle(x, y, w - (textSize.x + imageSize.x), h);
	      return;
	    }
	    throw new SWTException(
	        "H: "
	            + (imageAlign & ALIGN_HORIZONTAL_MASK));
	  } // text align right
	  throw new SWTException(
	      "H: "
	          + (textAlign & ALIGN_HORIZONTAL_MASK));
	} // trye
	finally {
	  // gc.setClipping(oldClipping);
	    }
	  }
	
	  public static void drawTextImage(GC gc, String text, int textAlign,
	      Image image, int imageAlign, Rectangle r) {
	    drawTextImage(gc, text, textAlign, image, imageAlign, r.x, r.y,
	        r.width, r.height);
	  }
	
	  public static void drawButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h,
	      Color face, Color shadowHigh, Color shadowNormal, Color shadowDark,
	      int leftMargin, int topMargin) {
	    Color prevForeground = gc.getForeground();
	    Color prevBackground = gc.getBackground();
	    try {
	      gc.setBackground(face);
	      gc.setForeground(shadowHigh);
	      gc.drawLine(x, y, x, y + h - 1);
	      gc.drawLine(x, y, x + w - 2, y);
	      gc.setForeground(shadowDark);
	      gc.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
	      gc.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
	      gc.setForeground(shadowNormal);
	      gc.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 2);
	      gc.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
	      //
	      gc.fillRectangle(x + 1, y + 1, leftMargin, h - 3);
	      gc.fillRectangle(x + 1, y + 1, w - 3, topMargin);
	      gc.setForeground(prevForeground);
	      drawTextImage(gc, text, textAlign, image, imageAlign, x + 1
	          + leftMargin, y + 1 + topMargin, w - 3 - leftMargin, h - 3
	          - topMargin);
	    } finally {
	      gc.setForeground(prevForeground);
	      gc.setBackground(prevBackground);
	    }
	  }
	
	  public static void drawButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h, Color face) {
	    Display display = Display.getCurrent();
	    drawButtonUp(gc, text, textAlign, image, imageAlign, x, y, w, h, face,
	        display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
	        display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW), display
	            .getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW), 2, 2);
	  }
	
	  public static void drawButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, Rectangle r, int leftMargin,
	      int topMargin) {
	    Display display = Display.getCurrent();
	    drawButtonUp(gc, text, textAlign, image, imageAlign, r.x, r.y, r.width,
	        r.height, display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND),
	        display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
	        display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW), display
	            .getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW),
	        leftMargin, topMargin);
	  }
	
	  public static void drawButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h) {
	    Display display = Display.getCurrent();
	    drawButtonUp(gc, text, textAlign, image, imageAlign, x, y, w, h,
	        display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND), display
	            .getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW),
	        display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW), display
	            .getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW), 2, 2);
	  }
	
	  public static void drawButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, Rectangle r) {
	    //Display display = Display.getCurrent();
	    drawButtonUp(gc, text, textAlign, image, imageAlign, r.x, r.y, r.width,
	        r.height);
	  }
	
	  public static void drawButtonDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h,
	      Color face, Color shadowNormal, int leftMargin, int topMargin) {
	    Color prevForeground = gc.getForeground();
	    Color prevBackground = gc.getBackground();
	    try {
	      gc.setBackground(face);
	      gc.setForeground(shadowNormal);
	      gc.drawRectangle(x, y, w - 1, h - 1);
	      gc.fillRectangle(x + 1, y + 1, 1 + leftMargin, h - 2);
	      gc.fillRectangle(x + 1, y + 1, w - 2, topMargin + 1);
	      gc.setForeground(prevForeground);
	      drawTextImage(gc, text, textAlign, image, imageAlign, x + 2
	          + leftMargin, y + 2 + topMargin, w - 3 - leftMargin, h - 3
	          - topMargin);
	    } finally {
	      gc.setForeground(prevForeground);
	      gc.setBackground(prevBackground);
	    }
	  }
	
	  public static void drawButtonDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h) {
	    Display display = Display.getCurrent();
	    drawButtonDown(gc, text, textAlign, image, imageAlign, x, y, w, h,
	        display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND), display
	            .getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW), 2, 2);
	  }
	
	  public static void drawButtonDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, Rectangle r) {
	    drawButtonDown(gc, text, textAlign, image, imageAlign, r.x, r.y,
	        r.width, r.height);
	  }
	
	  public static void drawButtonDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h, Color face) {
	    Display display = Display.getCurrent();
	    drawButtonDown(gc, text, textAlign, image, imageAlign, x, y, w, h,
	        face, display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
	        2, 2);
	  }
	
	  public static void drawButtonDeepDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h) {
	    Display display = Display.getCurrent();
	    gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
	    gc.drawLine(x, y, x + w - 2, y);
	    gc.drawLine(x, y, x, y + h - 2);
	    gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
	    gc.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
	    gc.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
	    gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	    gc.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
	    gc.drawLine(x + w - 2, y + h - 2, x + w - 2, y + 1);
	    //
	gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
	gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	gc.fillRectangle(x + 2, y + 2, w - 4, 1);
	gc.fillRectangle(x + 1, y + 2, 2, h - 4);
	//
	    gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	    drawTextImage(gc, text, textAlign, image, imageAlign, x + 2 + 1,
	        y + 2 + 1, w - 4, h - 3 - 1);
	  }
	
	  public static void drawButtonDeepDown(GC gc, String text, int textAlign,
	      Image image, int imageAlign, Rectangle r) {
	    drawButtonDeepDown(gc, text, textAlign, image, imageAlign, r.x, r.y,
	        r.width, r.height);
	  }
	
	  public static void drawFlatButtonUp(GC gc, String text, int textAlign,
	      Image image, int imageAlign, int x, int y, int w, int h,
	      Color face, Color shadowLight, Color shadowNormal, int leftMargin,
	      int topMargin) {
	    Color prevForeground = gc.getForeground();
	    Color prevBackground = gc.getBackground();
	    try {
	      gc.setForeground(shadowLight);
	      gc.drawLine(x, y, x + w - 1, y);
	      gc.drawLine(x, y, x, y + h);
	      gc.setForeground(shadowNormal);
	      gc.drawLine(x + w, y, x + w, y + h);
	      gc.drawLine(x + 1, y + h, x + w, y + h);
	      //
	  gc.setBackground(face);
	  gc.fillRectangle(x + 1, y + 1, leftMargin, h - 1);
	  gc.fillRectangle(x + 1, y + 1, w - 1, topMargin);
	  //
	      gc.setBackground(face);
	      gc.setForeground(prevForeground);
	      drawTextImage(gc, text, textAlign, image, imageAlign, x + 1
	          + leftMargin, y + 1 + topMargin, w - 1 - leftMargin, h - 1
	          - topMargin);
	    } finally {
	      gc.setForeground(prevForeground);
	      gc.setBackground(prevBackground);
	    }
	  }
	
	  public static void drawShadowImage(GC gc, Image image, int x, int y,
	      int alpha) {
	    Display display = Display.getCurrent();
	    Point imageSize = new Point(image.getBounds().width,
	        image.getBounds().height);
	    //
	    ImageData imgData = new ImageData(imageSize.x, imageSize.y, 24,
	        new PaletteData(255, 255, 255));
	    imgData.alpha = alpha;
	    Image img = new Image(display, imgData);
	    GC imgGC = new GC(img);
	    imgGC.drawImage(image, 0, 0);
	    gc.drawImage(img, x, y);
	    imgGC.dispose();
	    img.dispose();
	  }

	public static void main(String[] args) {
	    // create a shell...
	    Display display = new Display();
	    Shell shell = new Shell(display);
	    shell.setLayout(new FillLayout());
	    shell.setText("KTable examples");
	
	    // put a tab folder in it...
	    TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
	
	    // Item 1: a Text Table
	    TabItem item1 = new TabItem(tabFolder, SWT.NONE);
	    item1.setText("Text Table");
	    Composite comp1 = new Composite(tabFolder, SWT.NONE);
	    item1.setControl(comp1);
	    comp1.setLayout(new FillLayout());
	
	    // put a table in tabItem1...
	    KTable table = new KTable(comp1, SWT.V_SCROLL | SWT.H_SCROLL);
	    table.setRowSelectionMode(true);
	    //table.setMultiSelectionMode(true);
	    table.setModel(new KTableModelExample());
	
	    // display the shell...
	    shell.setSize(600, 600);
	    shell.open();
	    while (!shell.isDisposed()) {
	      if (!display.readAndDispatch())
	        display.sleep();
	    }
	    display.dispose();
	}
}
