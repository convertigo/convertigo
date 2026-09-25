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

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import jakarta.annotation.PostConstruct;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;

/**
 * The activity bar on the left edge of the window, as the one of Cursor or Visual Studio Code: each icon
 * shows its view, in the left column of the Convertigo perspective, the icon of the visible view being
 * lit and marked with the Convertigo accent. The last icon, at the bottom, opens the preferences. It is a
 * tool control of the left trim bar of the window, added by ConvertigoPlugin.
 */
public class ActivityBar {
	private static final int WIDTH = 44;
	private static final int ITEM_HEIGHT = 42;
	private static final int ICON_SIZE = 24;
	private static final Color ACCENT = new Color(0, 200, 247);
	private static final Color DARK_LINE = new Color(43, 43, 43);
	private static final Color LIGHT_LINE = new Color(229, 229, 229);
	private static final RGB DARK_IDLE = new RGB(133, 133, 133);
	private static final RGB DARK_HOT = new RGB(204, 204, 204);
	private static final RGB DARK_ACTIVE = new RGB(255, 255, 255);
	private static final RGB LIGHT_IDLE = new RGB(97, 97, 97);
	private static final RGB LIGHT_HOT = new RGB(59, 59, 59);
	private static final RGB LIGHT_ACTIVE = new RGB(31, 31, 31);

	private record Item(String icon, String tooltip, String viewId, String commandId) {
	}

	private static final Item[] ITEMS = {
			new Item("projects", "Projects", "com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView", null),
			new Item("sourcepicker", "Source Picker", "com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView", null),
			new Item("references", "References", "com.twinsoft.convertigo.eclipse.views.references.ReferencesView", null),
			new Item("git", "Git Staging", "org.eclipse.egit.ui.StagingView", null),
			new Item("search", "Search", "org.eclipse.search.ui.views.SearchView", null)
	};
	private static final Item SETTINGS = new Item("settings", "Preferences", null, "org.eclipse.ui.window.preferences");

	private final Map<String, Image> images = new HashMap<>();
	private Canvas canvas;
	private IWorkbenchWindow window;
	private int hot = -1;

	@PostConstruct
	public void createControl(Composite parent) {
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED) {

			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return new Point(WIDTH, (ITEMS.length + 1) * ITEM_HEIGHT);
			}
		};
		canvas.addPaintListener(e -> paint(e.gc));
		canvas.addListener(SWT.MouseMove, e -> {
			int index = indexAt(e.y);
			if (index != hot) {
				hot = index;
				canvas.setToolTipText(index < 0 ? null : item(index).tooltip());
				canvas.redraw();
			}
		});
		canvas.addListener(SWT.MouseExit, e -> {
			hot = -1;
			canvas.redraw();
		});
		canvas.addListener(SWT.MouseUp, e -> {
			int index = indexAt(e.y);
			if (index >= 0) {
				run(item(index));
			}
		});
		IPartListener2 partListener = new IPartListener2() {

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				redraw();
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				redraw();
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				redraw();
			}
		};
		canvas.addDisposeListener(e -> {
			if (window != null) {
				window.getPartService().removePartListener(partListener);
			}
			images.values().forEach(Image::dispose);
			images.clear();
		});
		// the workbench window of the trim bar exists once the window is rendered
		canvas.getDisplay().asyncExec(() -> {
			if (!canvas.isDisposed() && window() != null) {
				window.getPartService().addPartListener(partListener);
				canvas.redraw();
			}
		});
	}

	private IWorkbenchWindow window() {
		if (window == null) {
			for (var w : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				if (w.getShell() == canvas.getShell()) {
					window = w;
				}
			}
		}
		return window;
	}

	private void redraw() {
		if (!canvas.isDisposed()) {
			canvas.redraw();
		}
	}

	/** The items are drawn from the top, the settings item at the bottom of the bar. */
	private int settingsY() {
		return Math.max(ITEMS.length * ITEM_HEIGHT, canvas.getSize().y - ITEM_HEIGHT - 4);
	}

	private int indexAt(int y) {
		if (y >= settingsY() && y < settingsY() + ITEM_HEIGHT) {
			return ITEMS.length;
		}
		int index = y / ITEM_HEIGHT;
		return index >= 0 && index < ITEMS.length ? index : -1;
	}

	private static Item item(int index) {
		return index < ITEMS.length ? ITEMS[index] : SETTINGS;
	}

	private void paint(GC gc) {
		var dark = SwtUtils.isDark();
		var page = window() == null ? null : window.getActivePage();
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		var size = canvas.getSize();
		gc.setForeground(dark ? DARK_LINE : LIGHT_LINE);
		gc.drawLine(size.x - 1, 0, size.x - 1, size.y);
		for (int i = 0; i <= ITEMS.length; i++) {
			var item = item(i);
			int y = i < ITEMS.length ? i * ITEM_HEIGHT : settingsY();
			boolean active = item.viewId() != null && page != null && isVisible(page, item.viewId());
			if (active) {
				gc.setBackground(ACCENT);
				gc.fillRectangle(0, y + 6, 2, ITEM_HEIGHT - 12);
			}
			var rgb = active ? (dark ? DARK_ACTIVE : LIGHT_ACTIVE) : i == hot ? (dark ? DARK_HOT : LIGHT_HOT) : (dark ? DARK_IDLE : LIGHT_IDLE);
			var image = image(item.icon(), rgb);
			if (image != null) {
				gc.drawImage(image, (WIDTH - ICON_SIZE) / 2, y + (ITEM_HEIGHT - ICON_SIZE) / 2);
			}
		}
	}

	private static boolean isVisible(IWorkbenchPage page, String viewId) {
		var ref = page.findViewReference(viewId);
		var part = ref == null ? null : ref.getPart(false);
		return part != null && page.isPartVisible(part);
	}

	private Image image(String icon, RGB rgb) {
		return images.computeIfAbsent(icon + rgb, k -> SwtUtils.createImage(canvas.getDisplay(), "icons/studio/activity/" + icon + ".svg", path -> {
			var url = FileLocator.find(ConvertigoPlugin.getDefault().getBundle(), new Path(path), null);
			return url == null ? null : url.openStream();
		}, tint(rgb)));
	}

	private static UnaryOperator<ImageData> tint(RGB rgb) {
		return data -> {
			var tinted = (ImageData) data.clone();
			int pixel = tinted.palette.getPixel(rgb);
			for (int y = 0; y < tinted.height; y++) {
				for (int x = 0; x < tinted.width; x++) {
					tinted.setPixel(x, y, pixel);
				}
			}
			return tinted;
		};
	}

	private void run(Item item) {
		try {
			if (window() == null) {
				return;
			}
			if (item.viewId() != null) {
				window.getActivePage().showView(item.viewId());
			} else {
				window.getService(IHandlerService.class).executeCommand(item.commandId(), null);
			}
		} catch (Exception e) {
			Engine.logStudio.debug("(ActivityBar) cannot open " + item.tooltip() + ": " + e);
		}
	}
}
