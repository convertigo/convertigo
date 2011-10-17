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

package com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector;

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class TreeWrapper{
	private Tree tree;
	
	public TreeWrapper(Composite parent, int style) {
		tree = new Tree(parent, style);
	}

	public Tree getTree(){
		return tree;
	}
	
	public void addSelectionListener(SelectionListener listener) {
		
		tree.addSelectionListener(listener);
	}

	public void addTreeListener(TreeListener listener) {
		
		tree.addTreeListener(listener);
	}

	public void clear(int index, boolean all) {
		
		tree.clear(index, all);
	}

	public void clearAll(boolean all) {
		
		tree.clearAll(all);
	}

	public Point computeSize(int hint, int hint2, boolean changed) {
		
		return tree.computeSize(hint, hint2, changed);
	}

	public void deselectAll() {
		
		tree.deselectAll();
	}

	public TreeColumn getColumn(int index) {
		
		return tree.getColumn(index);
	}

	public int getColumnCount() {
		
		return tree.getColumnCount();
	}

	public int[] getColumnOrder() {
		
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

	public TreeItem getItem(int index) {
		
		return tree.getItem(index);
	}

	public TreeItem getItem(Point point) {
		
		return tree.getItem(point);
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

	public int indexOf(TreeColumn column) {
		
		return tree.indexOf(column);
	}

	public int indexOf(TreeItem item) {
		
		return tree.indexOf(item);
	}

	public void removeAll() {
		
		tree.removeAll();
	}

	public void removeSelectionListener(SelectionListener listener) {
		
		tree.removeSelectionListener(listener);
	}

	public void removeTreeListener(TreeListener listener) {
		
		tree.removeTreeListener(listener);
	}

	public void selectAll() {
		
		tree.selectAll();
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

	public void setInsertMark(TreeItem item, boolean before) {
		
		tree.setInsertMark(item, before);
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

	public void showColumn(TreeColumn column) {
		
		tree.showColumn(column);
	}

	public void showItem(TreeItem item) {
		
		tree.showItem(item);
	}

	public void showSelection() {
		
		tree.showSelection();
	}

	public void changed(Control[] changed) {
		
		tree.changed(changed);
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

	public void layout() {
		
		tree.layout();
	}

	public void layout(boolean changed, boolean all) {
		
		tree.layout(changed, all);
	}

	public void layout(boolean changed) {
		
		tree.layout(changed);
	}

	public void layout(Control[] changed) {
		
		tree.layout(changed);
	}

	public void setBackgroundMode(int mode) {
		
		tree.setBackgroundMode(mode);
	}

	public boolean setFocus() {
		
		return tree.setFocus();
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

	public Rectangle computeTrim(int x, int y, int width, int height) {
		
		return tree.computeTrim(x, y, width, height);
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

	public void addControlListener(ControlListener listener) {
		
		tree.addControlListener(listener);
	}

	public void addDragDetectListener(DragDetectListener listener) {
		
		tree.addDragDetectListener(listener);
	}

	public void addFocusListener(FocusListener listener) {
		
		tree.addFocusListener(listener);
	}

	public void addHelpListener(HelpListener listener) {
		
		tree.addHelpListener(listener);
	}

	public void addKeyListener(KeyListener listener) {
		
		tree.addKeyListener(listener);
	}

	public void addMenuDetectListener(MenuDetectListener listener) {
		
		tree.addMenuDetectListener(listener);
	}

	public void addMouseListener(MouseListener listener) {
		
		tree.addMouseListener(listener);
	}

	public void addMouseMoveListener(MouseMoveListener listener) {
		
		tree.addMouseMoveListener(listener);
	}

	public void addMouseTrackListener(MouseTrackListener listener) {
		
		tree.addMouseTrackListener(listener);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
		
		tree.addMouseWheelListener(listener);
	}

	public void addPaintListener(PaintListener listener) {
		
		tree.addPaintListener(listener);
	}

	public void addTraverseListener(TraverseListener listener) {
		
		tree.addTraverseListener(listener);
	}

	public Point computeSize(int hint, int hint2) {
		
		return tree.computeSize(hint, hint2);
	}

	public boolean dragDetect(Event event) {
		
		return tree.dragDetect(event);
	}

	public boolean dragDetect(MouseEvent event) {
		
		return tree.dragDetect(event);
	}

	public boolean forceFocus() {
		
		return tree.forceFocus();
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

	public void internal_dispose_GC(int hdc, GCData data) {
		
		tree.internal_dispose_GC(hdc, data);
	}
/*
	public int internal_new_GC(GCData data) {
		
		return tree.internal_new_GC(data);
	}
*/
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

	public void moveAbove(Control control) {
		
		tree.moveAbove(control);
	}

	public void moveBelow(Control control) {
		
		tree.moveBelow(control);
	}

	public void pack() {
		
		tree.pack();
	}

	public void pack(boolean changed) {
		
		tree.pack(changed);
	}

	public void redraw() {
		
		tree.redraw();
	}

	public void redraw(int x, int y, int width, int height, boolean all) {
		
		tree.redraw(x, y, width, height, all);
	}

	public void removeControlListener(ControlListener listener) {
		
		tree.removeControlListener(listener);
	}

	public void removeDragDetectListener(DragDetectListener listener) {
		
		tree.removeDragDetectListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		
		tree.removeFocusListener(listener);
	}

	public void removeHelpListener(HelpListener listener) {
		
		tree.removeHelpListener(listener);
	}

	public void removeKeyListener(KeyListener listener) {
		
		tree.removeKeyListener(listener);
	}

	public void removeMenuDetectListener(MenuDetectListener listener) {
		
		tree.removeMenuDetectListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		
		tree.removeMouseListener(listener);
	}

	public void removeMouseMoveListener(MouseMoveListener listener) {
		
		tree.removeMouseMoveListener(listener);
	}

	public void removeMouseTrackListener(MouseTrackListener listener) {
		
		tree.removeMouseTrackListener(listener);
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
		
		tree.removeMouseWheelListener(listener);
	}

	public void removePaintListener(PaintListener listener) {
		
		tree.removePaintListener(listener);
	}

	public void removeTraverseListener(TraverseListener listener) {
		
		tree.removeTraverseListener(listener);
	}

	public void setBackground(Color color) {
		
		tree.setBackground(color);
	}

	public void setBackgroundImage(Image image) {
		
		tree.setBackgroundImage(image);
	}

	public void setBounds(int x, int y, int width, int height) {
		
		tree.setBounds(x, y, width, height);
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

	public void setLocation(int x, int y) {
		
		tree.setLocation(x, y);
	}

	public void setLocation(Point location) {
		
		tree.setLocation(location);
	}

	public void setMenu(Menu menu) {
		
		tree.setMenu(menu);
	}

	public boolean setParent(Composite parent) {
		
		return tree.setParent(parent);
	}

	public void setSize(int width, int height) {
		
		tree.setSize(width, height);
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

	public Point toControl(int x, int y) {
		
		return tree.toControl(x, y);
	}

	public Point toControl(Point point) {
		
		return tree.toControl(point);
	}

	public Point toDisplay(int x, int y) {
		
		return tree.toDisplay(x, y);
	}

	public Point toDisplay(Point point) {
		
		return tree.toDisplay(point);
	}

	public boolean traverse(int traversal) {
		
		return tree.traverse(traversal);
	}

	public void update() {
		
		tree.update();
	}

	public void addDisposeListener(DisposeListener listener) {
		
		tree.addDisposeListener(listener);
	}

	public void addListener(int eventType, Listener listener) {
		
		tree.addListener(eventType, listener);
	}

	public void dispose() {
		
		tree.dispose();
	}

	public Object getData() {
		
		return tree.getData();
	}

	public Object getData(String key) {
		
		return tree.getData(key);
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

	public boolean isListening(int eventType) {
		
		return tree.isListening(eventType);
	}

	public void notifyListeners(int eventType, Event event) {
		
		tree.notifyListeners(eventType, event);
	}

	public void removeDisposeListener(DisposeListener listener) {
		
		tree.removeDisposeListener(listener);
	}

	public void removeListener(int eventType, Listener listener) {
		
		tree.removeListener(eventType, listener);
	}

	public void setData(Object data) {
		
		tree.setData(data);
	}

	public void setData(String key, Object value) {
		
		tree.setData(key, value);
	}

	public String toString() {
		
		return tree.toString();
	}

	public boolean equals(Object obj) {
		
		return tree.equals(obj);
	}

	public int hashCode() {
		
		return tree.hashCode();
	}
}
