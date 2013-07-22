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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.twinj.iJavelin;

public class JavelinAttributeEditor extends AbstractDialogCellEditor {

	private Font defaultFont;
	private int defaultFontHeight;
	
    public JavelinAttributeEditor(Composite parent) {
        super(parent);

        dialogTitle = "Attributes";
        dialogCompositeClass = JavelinAttributeEditorComposite.class;
        
    	Label label = getDefaultLabel();
        defaultFont = label.getFont();
        defaultFontHeight = defaultFont.getFontData()[0].getHeight();
    }

    protected Control createContents(Composite cell) {
    	return super.createContents(cell);
    }
    
    protected void updateContents(Object value) {
    	super.updateContents(value);

    	Label label = getDefaultLabel();
    	
    	if (value == null) return;
    	
		int attribute = Integer.parseInt((String) value);
    	
        if (attribute != -1) {
            int bg = (attribute & iJavelin.AT_PAPER) >> 3;
            int fg = attribute & iJavelin.AT_INK;
            
            if ((attribute & iJavelin.AT_INVERT) != 0) {
                int tmp;
                tmp = bg;
                bg = fg;
                fg = tmp;
            }
            
            String sbg;
			if ((attribute & JavelinExtractionRule.DONT_CARE_BACKGROUND_ATTRIBUTE) != 0) {
				bg = -1;
				sbg = "indifferent background";
			}
			else {
				sbg = getColorAsString(bg);
			}
			label.setBackground(getColor(bg));
			
			String s = " ";
			String sDontCare = ": do not care";
			String sIntense = "bold";
			String sUnderlined = "underlined";
			String sBlink = "blink";
			String sReverse = "reverse";

			String sfg;
			if ((attribute & JavelinExtractionRule.DONT_CARE_FOREGROUND_ATTRIBUTE) == 0) {
				sfg = getColorAsString(fg);
			}
			else {
				fg = -1;
				sfg = "indifferent foreground";
			}
			
			if (fg == bg) {
				switch (bg) {
				case -1: fg = 0; break;
				case 0: fg = 7; break;
				case 1: fg = 7; break;
				case 2: fg = 0; break;
				case 3: fg = 7; break;
				case 4: fg = 7; break;
				case 5: fg = 7; break;
				case 6: fg = 0; break;
				case 7: fg = 0; break;
				}
			}
			
			label.setForeground(getColor(fg));
			
			s += sfg + " on " + sbg + ",";

			if ((attribute & JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) == 0) {
				if ((attribute & iJavelin.AT_BOLD) != 0) s += " " + sIntense + ",";
			}
			else {
				s += " " + sIntense + sDontCare + ",";
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) == 0) {
				if ((attribute & iJavelin.AT_INVERT) != 0) s += " " + sReverse + ",";
			}
			else {
				s += " " + sReverse + sDontCare + ",";
			}
			
			if ((attribute & JavelinExtractionRule.DONT_CARE_BLINK_ATTRIBUTE) == 0) {
				if ((attribute & iJavelin.AT_BLINK) != 0) s += " " + sBlink + ",";
			}
			else {
				s += " " + sBlink + sDontCare + ",";
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE) == 0) {
				if ((attribute & iJavelin.AT_UNDERLINE) != 0) s += " " + sUnderlined + ",";
			}
			else {
				s += " " + sUnderlined + sDontCare + ",";
			}

			s = s.substring(0, s.length() - 1);
			
            Font f;
            Display display = getControl().getShell().getDisplay();
            if (((attribute & JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) == 0) && ((attribute & iJavelin.AT_BOLD) != 0)) {
                f = new Font(display, "Tahoma", defaultFontHeight, SWT.BOLD);
            }
            else  {
                f = new Font(display, "Tahoma", defaultFontHeight, SWT.NONE);
            }
            Font previousFont = label.getFont();
            label.setFont(f);
            if (!previousFont.equals(defaultFont)) {
            	previousFont.dispose();
            }

//            FontMetrics fm = gfx.getFontMetrics();
//            int h = fm.getAscent();
//            int w = fm.stringWidth(s) + 2;
//            int x = box.x + 2;
//            int y = box.y + (box.height + h) / 2 - 2;
//
//			if (((attribute & ExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE) == 0) && ((attribute & iJavelin.AT_UNDERLINE) != 0)) {
//                gfx.drawLine(x, y+1, w, y+1);
//				if (((attribute & ExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) == 0) && ((attribute & iJavelin.AT_BOLD) != 0)) {
//					gfx.drawLine(x, y+2, w, y+2);
//	            }
//			}

            label.setText(s);
        }
        else {
        	label.setForeground(getColor(-1));
        	label.setBackground(getColor(7));
        	label.setFont(defaultFont);
        	label.setText(" do not care");
        }
    }

	private Color getColor(int c) {
		Display display = getControl().getShell().getDisplay();
		switch (c) {
			case -2: return display.getSystemColor (SWT.COLOR_GRAY);
			case -1: return display.getSystemColor (SWT.COLOR_GRAY);
			default:
			case 0: return display.getSystemColor (SWT.COLOR_BLACK);
			case 1: return display.getSystemColor (SWT.COLOR_RED);
			case 2: return display.getSystemColor (SWT.COLOR_GREEN);
			case 3: return display.getSystemColor (SWT.COLOR_YELLOW);
			case 4: return display.getSystemColor (SWT.COLOR_BLUE);
			case 5: return display.getSystemColor (SWT.COLOR_MAGENTA);
			case 6: return display.getSystemColor (SWT.COLOR_CYAN);
			case 7: return display.getSystemColor (SWT.COLOR_WHITE);
		}
	}
    
	private String getColorAsString(int c) {
		switch (c) {
			default:
			case 0: return "black";
			case 1: return "red";
			case 2: return "green";
			case 3: return "yellow";
			case 4: return "blue";
			case 5: return "magenta";
			case 6: return "cyan";
			case 7: return "white";
		}
	}
 
	/**
     * Sets the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static void setPropertyValueFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
    	XMLRectangle zone = jTmp.getSelectionZone();
    	
    	if (zone.width < 1) 
        	return;
        
    	int att = jTmp.javelin.getCharAttribute(zone.x, zone.y);
        
        try {
        	propertySetter.invoke(databaseObject, new Object[] { new Integer(att) });
		} catch (Throwable e) {
			String message = "Error : "+e.getMessage(); 
            ConvertigoPlugin.logException(e, message);
		}
    }
    
    /**
     * Gets the value to put in the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static Object getSelectionZoneValue(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a iJavelin");
    	}
    	
    	XMLRectangle zone = jTmp.getSelectionZone();
    	
    	if (zone.width < 1) 
        	return null;
        
    	int att = jTmp.javelin.getCharAttribute(zone.x, zone.y);
        
        return new Integer(att);
    }
}
