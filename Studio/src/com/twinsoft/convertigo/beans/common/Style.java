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

package com.twinsoft.convertigo.beans.common;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class Style extends JavelinExtractionRule {
	
	private static final long serialVersionUID = -7865907907684343470L;
	
	/* Properties */
	private String 	fontSize 	= "";
	private int 	fontName 	= 0;
    private boolean bold 		= false;
    private boolean italic 		= false;
    private boolean underlined 	= false;
    private int 	color 		= 0;
    private int 	bgColor 	= 0;
    private boolean border 		= false;
    private int 	borderColor = 0;
    private String 	borderWidth = "";
    private String 	freeStyle 	= "";
    
    
	public Style() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		
		// format the string like css
		String formattedStyle = "";
		
		if (!freeStyle.equals("")) {
			formattedStyle = freeStyle;
		} else {
			if (fontName != 0) {
				formattedStyle = formattedStyle + "font-family:" + Fonts.FONTS[fontName] + "; ";
			}
			if (!fontSize.equals("")) {
				formattedStyle = formattedStyle + "font-size:" + fontSize + "; ";
			}
			if (bold) {
				formattedStyle = formattedStyle + "font-weight:bold; ";
			}
			if (italic) {
				formattedStyle = formattedStyle + "font-style:italic; ";
			}
			if (underlined) {
				formattedStyle = formattedStyle + "text-decoration:underline; ";
			}
			if (color != 0) {
				formattedStyle = formattedStyle + "color: " + Colors.COLORS[color] + ";";
			}
			if (bgColor != 0) {
				formattedStyle = formattedStyle + "background-color:" + Colors.COLORS[bgColor] + "; ";
			}
			if (border) {
				formattedStyle = formattedStyle + "border-style:solid; ";
			}
			if (borderColor != 0) {
				formattedStyle = formattedStyle + "border-color:" + Colors.COLORS[borderColor] + "; ";
			}
			if (!borderWidth.equals("")) {
				formattedStyle = formattedStyle + "border-width:" + borderWidth;
				if (borderWidth.indexOf("px") != -1)
					formattedStyle = formattedStyle + "; ";
				else
					formattedStyle = formattedStyle + "px; ";
			}
		}
		
		// add attribute to the block
		block.setOptionalAttribute("style", formattedStyle);
		
		xrs.hasMatched = true;
    	xrs.newCurrentBlock = block;
        return xrs;
	}

	
	public boolean isBold() {
		return bold;
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public int getFontName() {
		return fontName;
	}

	public void setFontName(int fontName) {
		this.fontName = fontName;
	}

	public String getFontSize() {
		return fontSize;
	}

	public void setFontSize(String fontSize) {
		this.fontSize = fontSize;
	}

	public boolean isItalic() {
		return italic;
	}

	public void setItalic(boolean italic) {
		this.italic = italic;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getBgColor() {
		return bgColor;
	}

	public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}

	public boolean isBorder() {
		return border;
	}

	public void setBorder(boolean border) {
		this.border = border;
	}

	public int getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(int borderColor) {
		this.borderColor = borderColor;
	}

	public String getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(String borderWidth) {
		this.borderWidth = borderWidth;
	}

	public String getFreeStyle() {
		return freeStyle;
	}

	public void setFreeStyle(String freeStyle) {
		this.freeStyle = freeStyle;
	}

	public boolean isUnderlined() {
		return underlined;
	}

	public void setUnderlined(boolean underlined) {
		this.underlined = underlined;
	}
    
}
