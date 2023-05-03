/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector;

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

class TreeWrapper{
	private Tree tree;
	
	TreeWrapper(Composite parent, int style) {
		tree = new Tree(parent, style);
	}

	public Tree getTree(){
		return tree;
	}
	
	void addSelectionListener(SelectionListener listener) {
		
		tree.addSelectionListener(listener);
	}
	
	void deselectAll() {
		
		tree.deselectAll();
	}

	public TreeColumn getColumn(int index) {
		
		return tree.getColumn(index);
	}

	public int getColumnCount() {
		
		return tree.getColumnCount();
	}

	int[] getColumnOrder() {
		
		return tree.getColumnOrder();
	}

	public TreeColumn[] getColumns() {
		
		return tree.getColumns();
	}

	public int getGridLineWidth() {
		
		return tree.getGridLineWidth();
	}

	public int getHeaderHeight() {
		
		return tree.getHeaderHeight();
	}

	public boolean getHeaderVisible() {
		
		return tree.getHeaderVisible();
	}

	TreeItem getItem(int index) {
		
		return tree.getItem(index);
	}

	public int getItemCount() {
		
		return tree.getItemCount();
	}

	public int getItemHeight() {
		
		return tree.getItemHeight();
	}

	public TreeItem[] getItems() {
		
		return tree.getItems();
	}

	public boolean getLinesVisible() {
		
		return tree.getLinesVisible();
	}

	public TreeItem getParentItem() {
		
		return tree.getParentItem();
	}

	public TreeItem[] getSelection() {
		
		return tree.getSelection();
	}

	public int getSelectionCount() {
		
		return tree.getSelectionCount();
	}

	public TreeColumn getSortColumn() {
		
		return tree.getSortColumn();
	}

	public int getSortDirection() {
		
		return tree.getSortDirection();
	}

	public TreeItem getTopItem() {
		
		return tree.getTopItem();
	}

	public void removeAll() {
		
		tree.removeAll();
	}

	public void setColumnOrder(int[] order) {
		
		tree.setColumnOrder(order);
	}

	public void setFont(Font font) {
		
		tree.setFont(font);
	}

	public void setHeaderVisible(boolean show) {
		
		tree.setHeaderVisible(show);
	}
	
	public void setItemCount(int count) {
		
		tree.setItemCount(count);
	}

	public void setLinesVisible(boolean show) {
		
		tree.setLinesVisible(show);
	}

	public void setRedraw(boolean redraw) {
		
		tree.setRedraw(redraw);
	}

	public void setSelection(TreeItem item) {
		
		tree.setSelection(item);
	}

	public void setSelection(TreeItem[] items) {
		
		tree.setSelection(items);
	}

	public void setSortColumn(TreeColumn column) {
		
		tree.setSortColumn(column);
	}

	public void setSortDirection(int direction) {
		
		tree.setSortDirection(direction);
	}

	public void setTopItem(TreeItem item) {
		
		tree.setTopItem(item);
	}

	public int getBackgroundMode() {
		
		return tree.getBackgroundMode();
	}

	public Control[] getChildren() {
		
		return tree.getChildren();
	}

	public Layout getLayout() {
		
		return tree.getLayout();
	}

	public boolean getLayoutDeferred() {
		
		return tree.getLayoutDeferred();
	}

	public Control[] getTabList() {
		
		return tree.getTabList();
	}

	public boolean isLayoutDeferred() {
		
		return tree.isLayoutDeferred();
	}

	public void setBackgroundMode(int mode) {
		
		tree.setBackgroundMode(mode);
	}

	public void setLayout(Layout layout) {
		
		tree.setLayout(layout);
	}

	public void setLayoutDeferred(boolean defer) {
		
		tree.setLayoutDeferred(defer);
	}

	public void setTabList(Control[] tabList) {
		
		tree.setTabList(tabList);
	}

	public Rectangle getClientArea() {
		
		return tree.getClientArea();
	}

	public ScrollBar getHorizontalBar() {
		
		return tree.getHorizontalBar();
	}

	public ScrollBar getVerticalBar() {
		
		return tree.getVerticalBar();
	}

	public void addKeyListener(KeyListener listener) {
		
		tree.addKeyListener(listener);
	}

	public void addMouseListener(MouseListener listener) {
		
		tree.addMouseListener(listener);
	}

	public Accessible getAccessible() {
		
		return tree.getAccessible();
	}

	public Color getBackground() {
		
		return tree.getBackground();
	}

	public Image getBackgroundImage() {
		
		return tree.getBackgroundImage();
	}

	public int getBorderWidth() {
		
		return tree.getBorderWidth();
	}

	public Rectangle getBounds() {
		
		return tree.getBounds();
	}

	public Cursor getCursor() {
		
		return tree.getCursor();
	}

	public boolean getDragDetect() {
		
		return tree.getDragDetect();
	}

	public boolean getEnabled() {
		
		return tree.getEnabled();
	}

	public Font getFont() {
		
		return tree.getFont();
	}

	public Color getForeground() {
		
		return tree.getForeground();
	}

	public Object getLayoutData() {
		
		return tree.getLayoutData();
	}

	public Point getLocation() {
		
		return tree.getLocation();
	}

	public Menu getMenu() {
		
		return tree.getMenu();
	}

	public Monitor getMonitor() {
		
		return tree.getMonitor();
	}

	public Composite getParent() {
		
		return tree.getParent();
	}

	public Shell getShell() {
		
		return tree.getShell();
	}

	public Point getSize() {
		
		return tree.getSize();
	}

	public String getToolTipText() {
		
		return tree.getToolTipText();
	}

	public boolean getVisible() {
		
		return tree.getVisible();
	}
	
	public boolean isEnabled() {
		
		return tree.isEnabled();
	}

	public boolean isFocusControl() {
		
		return tree.isFocusControl();
	}

	public boolean isReparentable() {
		
		return tree.isReparentable();
	}

	public boolean isVisible() {
		
		return tree.isVisible();
	}
	
	public void addFocusListener(FocusListener focusListener) {
		tree.addFocusListener(focusListener);
	}

	public void addListener(int eventType, Listener listener) {
		tree.addListener(eventType, listener);
	}
	
	public void setBackground(Color color) {
		
		tree.setBackground(color);
	}

	public void setBackgroundImage(Image image) {
		
		tree.setBackgroundImage(image);
	}

	public void setBounds(Rectangle rect) {
		
		tree.setBounds(rect);
	}

	public void setCapture(boolean capture) {
		
		tree.setCapture(capture);
	}

	public void setCursor(Cursor cursor) {
		
		tree.setCursor(cursor);
	}

	public void setDragDetect(boolean dragDetect) {
		
		tree.setDragDetect(dragDetect);
	}

	public void setEnabled(boolean enabled) {
		
		tree.setEnabled(enabled);
	}

	public void setForeground(Color color) {
		
		tree.setForeground(color);
	}

	public void setLayoutData(Object layoutData) {
		
		tree.setLayoutData(layoutData);
	}

	public void setLocation(Point location) {
		
		tree.setLocation(location);
	}

	public void setMenu(Menu menu) {
		
		tree.setMenu(menu);
	}

	public void setSize(Point size) {
		
		tree.setSize(size);
	}

	public void setToolTipText(String string) {
		
		tree.setToolTipText(string);
	}

	public void setVisible(boolean visible) {
		
		tree.setVisible(visible);
	}

	public Object getData() {
		
		return tree.getData();
	}

	public Display getDisplay() {
		
		return tree.getDisplay();
	}

	public int getStyle() {
		
		return tree.getStyle();
	}

	public boolean isDisposed() {
		
		return tree.isDisposed();
	}
	
	public void setData(Object data) {
		
		tree.setData(data);
	}

	@Override
	public String toString() {
		
		return tree.toString();
	}

	@Override
	public boolean equals(Object obj) {
		
		return tree.equals(obj);
	}

	@Override
	public int hashCode() {
		
		return tree.hashCode();
	}
}
