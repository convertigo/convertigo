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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.common.Table;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.twinj.zoneListener;

public class ColumnEditor extends JavelinPropertyTableEditor implements SwingValuePanelSupport, zoneListener  {
    
    private JButton addButton;
    
    public ColumnEditor(Composite parent) {
        super(parent);
        
        dialogTitle = "Columns definition of the table";
        columnNames = new String[] { "Label", "Initial column", "Final column", "Line index" };
        templateData = new Object[] { "label", new Integer(0), new Integer(0), new Integer(0) };
    }
    
    public void paintValue(Graphics gfx, Rectangle box) {
        gfx.setColor(Color.blue);
        gfx.fillRect(box.x, box.y, box.width, box.height);
        gfx.setColor(Color.lightGray);
        for (int i = -box.height ; i < box.width ; i += 6) {
            gfx.drawLine(box.x + i, box.y + box.height, box.x + i + box.height, box.y);
        }
    }
    
//    public void setValue(Object o) {
//        super.setValue(o);
//        updateComboBox();
//    }

    private void updateComboBox() {
        int i = jComboBoxColumns.getSelectedIndex();
        jComboBoxColumns.removeAllItems();
        for(List<Object> v : data)
        	jComboBoxColumns.addItem(v.get(0) + " [" + v.get(1) + ", " + v.get(2) + "]");
        
        try {
            if (i == -1) jComboBoxColumns.setSelectedIndex(0);
            else jComboBoxColumns.setSelectedIndex(i);
        }
        catch(IllegalArgumentException iae) {
            // Ignore
            if (jComboBoxColumns.getItemCount() > 0) {
                jComboBoxColumns.setSelectedIndex(0);
            }
        }
    }

    /**
     * Sets the Javelin zone according to the property.
     */
    public void setJavelinZoneFromProperty() {
        if (javelin == null) {
            throw new IllegalArgumentException("The Javelin object is null");
        }
        int i = jComboBoxColumns.getSelectedIndex();

        try {
            List<Object> v = data.get(i);
            Object o1 = v.get(1);
            Object o2 = v.get(2);

            int c1 = (o1 instanceof String ? Integer.parseInt((String) o1) : ((Integer) o1).intValue());
            int c2 = (o2 instanceof String ? Integer.parseInt((String) o2) : ((Integer) o2).intValue());

            Rectangle zone = new Rectangle(c1, 0, c2 - c1 + 1, javelin.getScreenHeight());
            javelin.setSelectionZone(zone);
        }
        catch(ArrayIndexOutOfBoundsException e) {
            // Ignore because no item has been selected in the combo box
        }
    }
    
    /** Sets the property according to the current Javelin selected zone.
     */
    public void setPropertyValueFromJavelinZone() {
        if (javelin == null) {
            throw new IllegalArgumentException("The Javelin object is null");
        }

        Rectangle zone = javelin.getSelectionZone();
        if ((zone.width < 1) || (zone.height < 1)) return;
        
        int i = jComboBoxColumns.getSelectedIndex();
        
        if (i == -1) return;
        
        List<Object> v = data.get(i);
        v.set(1, new Integer(zone.x));
        v.set(2, new Integer(zone.width + zone.x - 1));
        
        updateComboBox();
//        firePropertyChange();
    }
    
    private JComboBox jComboBoxColumns = new JComboBox();
    
    public void customizePanel(JPanel valuePanel, Container buttonsPanel) {
        valuePanel.setLayout(new BorderLayout());
        valuePanel.add(jComboBoxColumns);
        jComboBoxColumns.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setJavelinZoneFromProperty();
            }
        });
        
        addButton = new JButton();
        addButton.setFocusPainted(false);
        addButton.setEnabled(false);
        addButton.setPreferredSize(new Dimension(24, 24));
        addButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/new_line.png")));
        addButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/com/twinsoft/convertigo/eclipse/property_editors/images/table_editor/new_line.d.png")));
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                addButtonActionPerformed();
            }
        });
        buttonsPanel.add(addButton);
        
        if (javelin != null) {
			javelin.addZoneListener(this);
        }
    }
    
    private void addButtonActionPerformed() {
//        Studio.theApp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Rectangle zone = javelin.getSelectionZone();

            List<Object> v = new ArrayList<Object>(4);
            v.add(javelin.getString(zone.x, zone.y, zone.width).trim());
            v.add(Integer.valueOf(zone.x));
            v.add(Integer.valueOf(zone.x + zone.width - 1));
			v.add(Integer.valueOf(0));
            data.add(v);

            updateComboBox();
            jComboBoxColumns.setSelectedIndex(jComboBoxColumns.getItemCount() - 1);
//            firePropertyChange();
        }
        catch (Exception e) {
//            Studio.theApp.exception(e, "Unable to add the column.");
        }
        finally {
//            Studio.theApp.setCursor(Cursor.getDefaultCursor());
        }
    }
    
    public void handleSelectionChanged(com.twinsoft.twinj.twinxEvent0 twinxEvent0) {
        try {
            Rectangle selectedZone = javelin.getSelectionZone();
            boolean bSelected = (selectedZone != null) && (selectedZone.getWidth() != 0) && (selectedZone.getHeight() != 0);
            addButton.setEnabled(bSelected);
        } catch (Exception e) {
//            Studio.theApp.exception(e, "Unable to handle Javelin selection zone change.");
        }
    }
    
    /**
     * Sets the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static void setPropertyValueFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null)
            throw new IllegalArgumentException("The connector object is null");

    	try {
    		XMLRectangle zone = ((JavelinConnector) connector).getSelectionZone();
            if ((zone.width < 1) || (zone.height < 1)) 
            	return;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a JavelinConnector");
    	}
    	
        XMLVector<XMLVector<Object>> vTmp = null;;
        if (Table.class.isAssignableFrom(databaseObject.getClass())) {
        	vTmp = ((Table)databaseObject).getColumns();
        } else { } // not a table => no columns
// TODO trouver quelle colonne est sélectionnée dans la combo box et la mettre à jour
        /*Vector v = new XMLVector();
        v.add(jTmp.javelin.getString(zone.x, zone.y, zone.width).trim());
        v.add(new Integer(zone.x));
        v.add(new Integer(zone.x + zone.width - 1));
		v.add(new Integer(0));
        vTmp.add(v);
    	*/
    	try {
        	propertySetter.invoke(databaseObject, new Object[] { vTmp });
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
    	if (connector == null)
            throw new IllegalArgumentException("The connector object is null");

    	try {
            XMLRectangle zone = ((JavelinConnector) connector).getSelectionZone();            
            if ((zone.width < 1) || (zone.height < 1))
            	return null;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a JavelinConnector");
    	}
        
        XMLVector<XMLVector<Object>> vTmp = null;;
        if (Table.class.isAssignableFrom(databaseObject.getClass())) {
        	vTmp = ((Table)databaseObject).getColumns();
        } else { } // not a table => no columns

// TODO trouver quelle colonne est sélectionnée dans la combo box et la mettre à jour
        
        /*Vector v = new XMLVector();
        v.add(jTmp.javelin.getString(zone.x, zone.y, zone.width).trim());
        v.add(new Integer(zone.x));
        v.add(new Integer(zone.x + zone.width - 1));
		v.add(new Integer(0));
        vTmp.add(v);
    	*/

        return vTmp;
    }
    
    /**
     * Add a column to the property according to the current selected zone.
     * @param databaseObject
     * @param connector
     * @param setter
     */
    public static void addPropertyElementFromSelectionZone(DatabaseObject databaseObject, Connector connector, Method propertySetter) {
    	if (connector == null) {
            throw new IllegalArgumentException("The connector object is null");
        }

    	JavelinConnector jTmp = null;
    	try {
    		jTmp = (JavelinConnector) connector;
    	} catch (ClassCastException e) {
    		throw new IllegalArgumentException("The connector object is not a JavelinConnector");
    	}
    	
        XMLRectangle zone = jTmp.getSelectionZone();
        
        if ((zone.width < 1) || (zone.height < 1)) 
        	return;
        
        XMLVector<XMLVector<Object>> vTmp = null;
        if (Table.class.isAssignableFrom(databaseObject.getClass())) {
        	vTmp = ((Table)databaseObject).getColumns();
        } else { } // not a table => no columns
        
        XMLVector<Object> v = new XMLVector<Object>();
        v.add(jTmp.javelin.getString(zone.x, zone.y, zone.width).trim());
        v.add(new Integer(zone.x));
        v.add(new Integer(zone.x + zone.width - 1));
		v.add(new Integer(0));
        vTmp.add(v);
    	
    	try {
        	propertySetter.invoke(databaseObject, new Object[] { vTmp });
		} catch (Throwable e) {
			String message = "Error : "+e.getMessage(); 
            ConvertigoPlugin.logException(e, message);
		}
		
		jTmp.javelin.setSelectionZone(new XMLRectangle(zone.x, 0, zone.width, jTmp.javelin.getScreenHeight()));
    }
}
