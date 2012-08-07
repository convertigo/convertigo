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

package com.twinsoft.convertigo.eclipse.moz;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.mozilla.xpcom.XPCOMException;

import com.twinsoft.convertigo.engine.parsers.SelectionChangedListener;
import com.twinsoft.convertigo.engine.parsers.WebViewerTabManager;



public class XulToolBar extends Composite implements SelectionChangedListener{

	private Map<String, ToolItem> buttons = null;
	private Browser mozillaBrowser = null;
	private XulWebViewerImpl xulWebviewer = null;
	private ToolBar left_toolbar = null;
	private ToolBar right_toolbar = null;
	
	private ToolItem bt_back = null;
	private ToolItem bt_forward = null;
	private ToolItem bt_refresh = null;
	private ToolItem bt_stop = null;
	private Label lb_addr = null;
	private Text tx_addr = null;
	private ToolItem bt_go = null;
	private ToolItem bt_allow_alert = null;
	private ToolItem bt_tab_new = null;
	private ToolItem bt_tab_close = null;
	private ToolItem bt_tab_previous = null;
	private ToolItem bt_tab_next = null;
	private Text tx_tab_id = null;
	private Label lb_tab_nbr = null;
	
	private LocationListener locationListener = new LocationListener(){
		public void changed(LocationEvent event) {
			tx_addr.setText(mozillaBrowser.getUrl());
		}

		public void changing(LocationEvent event) { }
	};

	public XulToolBar(Composite parent,int style) {
		super(parent,style);
		initialize();
		initIcons();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridLayout gridLayout = new GridLayout(10, false);
        gridLayout.marginWidth = gridLayout.marginHeight = gridLayout.horizontalSpacing = 1;
        gridLayout.marginBottom = 4;
        this.setLayout(gridLayout);
        left_toolbar = new ToolBar(this, SWT.NONE);
        bt_back = new ToolItem(left_toolbar, SWT.PUSH);
        bt_forward = new ToolItem(left_toolbar, SWT.PUSH);
        bt_refresh = new ToolItem(left_toolbar, SWT.PUSH);
        bt_stop = new ToolItem(left_toolbar, SWT.PUSH);
        new ToolItem(left_toolbar, SWT.SEPARATOR);
        lb_addr = new Label(this, SWT.RIGHT);
        lb_addr.setText(" Address");
        tx_addr = new Text(this, SWT.BORDER);
        tx_addr.setText("http://");
        tx_addr.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tx_addr.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == '\r' || e.character == '\r') mozillaBrowser.setUrl(tx_addr.getText());
			}
			public void keyReleased(KeyEvent e) {
			}
        });
        right_toolbar = new ToolBar(this, SWT.NONE);
        bt_go = new ToolItem(right_toolbar, SWT.PUSH);
        new ToolItem(right_toolbar, SWT.SEPARATOR);
        bt_allow_alert = new ToolItem(right_toolbar, SWT.CHECK);
        bt_tab_new = new ToolItem(right_toolbar, SWT.PUSH);
        bt_tab_close = new ToolItem(right_toolbar, SWT.PUSH);
        bt_tab_previous = new ToolItem(right_toolbar, SWT.PUSH);
        bt_tab_next = new ToolItem(right_toolbar, SWT.PUSH);
        bt_back.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			mozillaBrowser.back();
        		} catch (XPCOMException e1) {
        			// if no back available
        		}
        	}
        });
        bt_forward.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			mozillaBrowser.forward();
        		} catch (XPCOMException e1) {
        			// if no forward available
        		}
        	}
        });
        bt_refresh.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			mozillaBrowser.refresh();
        		} catch (XPCOMException e1) {
        			// if no refresh available
        		}
        	}
        });
        bt_stop.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			mozillaBrowser.stop();
        		} catch (XPCOMException e1) {
        			// if no stop available
        		}
        	}
        });
        bt_go.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			mozillaBrowser.setUrl(tx_addr.getText());
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
        bt_allow_alert.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			setEnable("allow alert box",bt_allow_alert.getSelection());
        			xulWebviewer.setAllowAlertBox(bt_allow_alert.getSelection());
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
        bt_tab_new.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			xulWebviewer.createTab(true);
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
        bt_tab_close.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			xulWebviewer.deleteTab();
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
        bt_tab_previous.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			xulWebviewer.getPreviousTab();
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
        bt_tab_next.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
        	public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
        		try {
        			xulWebviewer.getNextTab();
        		} catch (XPCOMException e1) {
        			// if no setUrl available
        		}
        	}
        });
                
        tx_tab_id = new Text(this, SWT.RIGHT);
        tx_tab_id.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == '\r' || e.character == '\r') {
					try{
						int val = Integer.parseInt(tx_tab_id.getText());
						xulWebviewer.getTabManager().getTab(val);
					}catch (Exception ex) {}
					tx_tab_id.setText(""+xulWebviewer.getTabManager().getIndex(xulWebviewer));
				}
			}
			public void keyReleased(KeyEvent e) {
			}
        });
        tx_tab_id.setLayoutData(new GridData(20,SWT.DEFAULT));
        lb_tab_nbr = new Label(this, SWT.LEFT);
        lb_tab_nbr.setText("/0");
	}
	
	private void initIcons(){
		Object [][] tmp_buttons = {
			{bt_back,"back"}, {bt_forward,"forward"},	{bt_refresh,"refresh"},
			{bt_stop,"stop"}, {bt_go,"go"}, {bt_allow_alert,"allow alert box"},
			{bt_tab_new,"new tab"}, {bt_tab_close,"close tab"}, {bt_tab_previous,"previous tab"}, {bt_tab_next,"next tab"}
		};
		
		buttons = new HashMap<String, ToolItem>(tmp_buttons.length);
		for(int i=0;i<tmp_buttons.length;i++){
			buttons.put((String) tmp_buttons[i][1], (ToolItem)tmp_buttons[i][0]);
			((ToolItem)tmp_buttons[i][0]).setToolTipText((String)tmp_buttons[i][1]);
			setEnable((String)tmp_buttons[i][1],true);
		}
	}
	
	public void setEnable(String name, boolean enable){
		String url = "/com/twinsoft/convertigo/eclipse/moz/images/"+((enable)?"e_":"d_")+name+".png";
		buttons.get(name).setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(url)));
	}
	
	public void setMozillaBrowser(Browser mb){
		mozillaBrowser = mb;
		tx_addr.setText(mozillaBrowser.getUrl());
		bt_allow_alert.setSelection(xulWebviewer.isAllowAlertBox());
		setEnable("allow alert box", xulWebviewer.isAllowAlertBox());
		mozillaBrowser.addLocationListener(locationListener);
	}
	
	public void setXulWebViewer(XulWebViewerImpl xulWebviewer){
		this.xulWebviewer = xulWebviewer;
		setMozillaBrowser(xulWebviewer.getBrowser());
		final WebViewerTabManager wvtm = xulWebviewer.getTabManager();
		wvtm.addChangeListener(this);
		addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				wvtm.removeChangeListener(XulToolBar.this);
				mozillaBrowser.removeLocationListener(locationListener);
			}
		});
        changed();
	}
	
	public void changed(){
		WebViewerTabManager tabManager = xulWebviewer.getTabManager();
		int max = tabManager.getNumberOfTab();
		tx_tab_id.setText(""+xulWebviewer.getTabManager().getIndex(xulWebviewer));
	    lb_tab_nbr.setText("/"+max);
	}
}
