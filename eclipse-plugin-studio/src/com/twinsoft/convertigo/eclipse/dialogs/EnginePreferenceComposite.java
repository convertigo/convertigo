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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyCategory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class EnginePreferenceComposite extends Composite {

	/* Shares of the foreground mixed into the background of the theme, in percent */
	private static final int HOVER_SHADE = 8;
	private static final int STRIPE_SHADE = 6;
	private static final int SEPARATOR_SHADE = 20;
	private static final int ARROW_SHADE = 75;
	private static final int COUNT_SHADE = 55;

	private static final int HEADER_PADDING = 6;
	private static final int HEADER_GAP = 8;

	private ScrolledComposite scrolledComposite;
	private Composite content;

	private Map<PropertyName, String> modifiedProperties;
	private List<String> filterList = null;

	private Map<PropertyCategory, Section> sectionsMap = new LinkedHashMap<PropertyCategory, Section>();

	public EnginePreferenceComposite(Composite parent, int style) {
		super(parent, style);
		createContents();
	}

	EnginePreferenceComposite(Composite parent, int style, List<String> filterList) {
		super(parent, style);
		this.filterList = filterList;
		createContents();
	}

	private void createContents() {
		setLayout(new FillLayout());
		modifiedProperties = new HashMap<PropertyName, String>();

		scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setShowFocusedControl(true);
		scrolledComposite.getVerticalBar().setIncrement(16);

		content = new Composite(scrolledComposite, SWT.NONE);
		GridLayout contentLayout = new GridLayout(1, false);
		contentLayout.marginWidth = contentLayout.marginHeight = 0;
		contentLayout.verticalSpacing = 0;
		content.setLayout(contentLayout);
		scrolledComposite.setContent(content);

		for (PropertyCategory propertyCategory : PropertyCategory.getSortedValues()) {
			if (propertyCategory.isVisible()) {
				if (filterList == null || filterList.contains(propertyCategory.getDisplayName())) {
					sectionsMap.put(propertyCategory, new Section(propertyCategory.getDisplayName()));
				}
			}
		}

		for (final PropertyName property : PropertyName.values()) {
			if (property.isVisible() && property.getCategory().isVisible()) {
				Section section = sectionsMap.get(property.getCategory());

				if (section != null) {
					Composite line = new Composite(section.body, SWT.NONE);
					GridLayout layout = new GridLayout(2, false);
					layout.marginLeft = layout.marginTop = layout.marginRight = layout.marginBottom = 2;
					layout.verticalSpacing = 0;
					line.setLayout(layout);
					GridData data = new GridData(GridData.FILL_HORIZONTAL);
					line.setLayoutData(data);
					section.addLine(line);

					Label propertyLabel = new Label(line, SWT.WRAP);
					data = new GridData(240, SWT.DEFAULT);
					data.verticalAlignment = SWT.TOP;
					propertyLabel.setText(property.getDescription());
					propertyLabel.setLayoutData(data);

					switch (property.getType()) {
					case Text: {
						final Text propertyEditor = new Text(line, SWT.BORDER);
						data = new GridData(GridData.FILL_HORIZONTAL);
						data.verticalAlignment = SWT.TOP;
						propertyEditor.setLayoutData(data);

						String value = EnginePropertiesManager.getProperty(property);
						String originalValue = EnginePropertiesManager.getOriginalProperty(property);
						propertyEditor.setText(originalValue);
						if (!originalValue.equals(value)) propertyEditor.setToolTipText(value);

						propertyEditor.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent e) {
								modifiedProperties.put(property, propertyEditor.getText());
							}
						});
						break;
					}

					case PasswordHash:
					case PasswordPlain: {
						final Text propertyEditor = new Text(line, SWT.BORDER);
						data = new GridData(GridData.FILL_HORIZONTAL);
						data.verticalAlignment = SWT.TOP;
						propertyEditor.setLayoutData(data);

						String value = EnginePropertiesManager.getProperty(property);
						propertyEditor.setText(value);
						propertyEditor.setEchoChar('*');

						propertyEditor.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent e) {
								modifiedProperties.put(property, "" + propertyEditor.getText());
							}
						});
						break;
					}

					case Boolean: {
						final Button propertyEditor = new Button(line, SWT.CHECK);

						data = new GridData();
						data.verticalAlignment = SWT.TOP;
						propertyEditor.setLayoutData(data);

						boolean value = Boolean.parseBoolean(EnginePropertiesManager.getProperty(property));
						propertyEditor.setSelection(value);

						propertyEditor.addSelectionListener(new SelectionListener() {
							public void widgetSelected(SelectionEvent e) {
								modifiedProperties.put(property, "" + propertyEditor.getSelection());
							}

							public void widgetDefaultSelected(SelectionEvent e) {
								modifiedProperties.put(property, "" + propertyEditor.getSelection());
							}
						});

						break;
					}

					case Combo: {
						final Combo propertyEditor = new Combo(line, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);

						data = new GridData(GridData.FILL_HORIZONTAL);
						data.verticalAlignment = SWT.TOP;
						propertyEditor.setLayoutData(data);

						String value = EnginePropertiesManager.getProperty(property);
						String comboDisplay, comboValue;
						int selectedIndex = 0;
						int i = 0;
						for (ComboEnum comboItem : property.getCombo()) {
							comboDisplay = comboItem.getDisplay();
							if (comboDisplay != null) {
								comboValue = comboItem.getValue();
								propertyEditor.add(comboDisplay);
								propertyEditor.setData(comboDisplay, comboValue);
								if (value.equals(comboValue)) {
									selectedIndex = i;
								}
								i++;
							}
						}
						propertyEditor.select(selectedIndex);

						propertyEditor.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent e) {
								modifiedProperties.put(property, "" + propertyEditor.getData(propertyEditor.getText()));
							}
						});

						break;
					}

					case Array: {
						final Text propertyEditor = new Text(line, SWT.BORDER | SWT.MULTI);

						data = new GridData(GridData.FILL_HORIZONTAL);
						data.verticalAlignment = SWT.TOP;
						propertyEditor.setLayoutData(data);

						String[] originalValue = EnginePropertiesManager.getOriginalPropertyAsStringArray(property);

						for (String item : originalValue) {
							propertyEditor.append(item + "\r\n");
						}

						String v1 = EnginePropertiesManager.getProperty(property);
						String v2 = EnginePropertiesManager.getOriginalProperty(property);
						if (!v1.equals(v2)) propertyEditor.setToolTipText(v1);

						propertyEditor.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent e) {
								modifiedProperties.put(property, propertyEditor.getText());
							}
						});

						break;
					}
					}
				}
			}
		}

		for (Section section : sectionsMap.values()) {
			section.initHeaderSize();
			section.setExpanded(filterList != null);
		}

		scrolledComposite.addListener(SWT.Resize, e -> updateScrolledSize());
		updateScrolledSize();

		// The theme styles the new widgets once they are created: derive the stripes from its colors afterwards
		getDisplay().asyncExec(() -> {
			if (!isDisposed()) {
				for (Section section : sectionsMap.values()) {
					if (section.expanded) {
						section.applyStripes();
					}
				}
			}
		});
	}

	private void updateScrolledSize() {
		if (scrolledComposite.isDisposed()) {
			return;
		}
		// the content follows the visible width, only its height scrolls
		int width = scrolledComposite.getClientArea().width;
		Point size = content.computeSize(width > 0 ? width : SWT.DEFAULT, SWT.DEFAULT, true);
		scrolledComposite.setMinSize(0, size.y);
	}

	/** Mixes a share of the second color into the first one, so that it follows the light or dark theme */
	private static Color mix(Color color, Color other, int percent) {
		RGB a = color.getRGB();
		RGB b = other.getRGB();
		return new Color(
				a.red + (b.red - a.red) * percent / 100,
				a.green + (b.green - a.green) * percent / 100,
				a.blue + (b.blue - a.blue) * percent / 100);
	}

	public Map<PropertyName, String> getModifiedProperties(){
		return modifiedProperties;
	}

	/** A collapsible section: a header reachable with the keyboard, and the lines of its properties */
	private class Section {
		private final String title;
		private final Canvas header;
		private final Composite body;
		private final List<Composite> stripedLines = new ArrayList<Composite>();
		private int count = 0;
		private boolean expanded = false;
		private boolean hover = false;

		private Section(String title) {
			this.title = title;

			header = new Canvas(content, SWT.DOUBLE_BUFFERED);
			header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			header.setCursor(header.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			header.addListener(SWT.Paint, this::paintHeader);
			header.addListener(SWT.MouseEnter, e -> setHover(true));
			header.addListener(SWT.MouseExit, e -> setHover(false));
			header.addListener(SWT.MouseUp, e -> {
				if (e.button == 1 && header.getClientArea().contains(e.x, e.y)) {
					toggle();
				}
			});
			header.addListener(SWT.Traverse, e -> {
				switch (e.detail) {
				case SWT.TRAVERSE_RETURN:
					// handled by the key listener, rather than the default button of the dialog
					e.doit = false;
					break;
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_PAGE_NEXT:
				case SWT.TRAVERSE_PAGE_PREVIOUS:
					e.doit = true;
					break;
				}
			});
			header.addListener(SWT.KeyDown, e -> {
				if (e.character == ' ' || e.character == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					toggle();
				}
			});
			header.addListener(SWT.FocusIn, e -> header.redraw());
			header.addListener(SWT.FocusOut, e -> header.redraw());
			header.getAccessible().addAccessibleListener(new AccessibleAdapter() {
				@Override
				public void getName(AccessibleEvent e) {
					e.result = title;
				}
			});
			header.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
				@Override
				public void getRole(AccessibleControlEvent e) {
					e.detail = ACC.ROLE_PUSHBUTTON;
				}

				@Override
				public void getState(AccessibleControlEvent e) {
					e.detail = ACC.STATE_FOCUSABLE | (expanded ? ACC.STATE_EXPANDED : ACC.STATE_COLLAPSED);
					if (header.isFocusControl()) {
						e.detail |= ACC.STATE_FOCUSED;
					}
				}
			});

			body = new Composite(content, SWT.NONE);
			GridLayout layout = new GridLayout(1, true);
			layout.marginLeft = layout.marginTop = layout.marginRight = 2;
			layout.marginBottom = HEADER_GAP;
			layout.verticalSpacing = 4;
			body.setLayout(layout);
			body.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		}

		private void addLine(Composite line) {
			if (count++ % 2 == 1) {
				stripedLines.add(line);
			}
		}

		private String getCountText() {
			return count + (count == 1 ? " property" : " properties");
		}

		private void initHeaderSize() {
			GC gc = new GC(header);
			try {
				gc.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
				Point titleSize = gc.textExtent(title);
				gc.setFont(JFaceResources.getDialogFont());
				Point countSize = gc.textExtent(getCountText());
				GridData data = (GridData) header.getLayoutData();
				data.widthHint = 2 * HEADER_PADDING + getArrowSize(titleSize.y) + titleSize.x + countSize.x + 2 * HEADER_GAP;
				data.heightHint = Math.max(titleSize.y, countSize.y) + 2 * HEADER_PADDING + 1;
			} finally {
				gc.dispose();
			}
		}

		private int getArrowSize(int textHeight) {
			return Math.max(7, textHeight / 2);
		}

		private void setHover(boolean hover) {
			this.hover = hover;
			header.redraw();
		}

		private void toggle() {
			setExpanded(!expanded);
			content.layout(true, true);
			updateScrolledSize();
		}

		private void setExpanded(boolean expanded) {
			this.expanded = expanded;
			((GridData) body.getLayoutData()).exclude = !expanded;
			body.setVisible(expanded);
			if (expanded) {
				applyStripes();
			}
			header.redraw();
		}

		/** Alternates the background of the lines with a shade derived from the current theme */
		private void applyStripes() {
			Color stripe = mix(body.getBackground(), body.getForeground(), STRIPE_SHADE);
			for (Composite line : stripedLines) {
				line.setBackground(stripe);
				for (Control child : line.getChildren()) {
					if (child instanceof Label || child instanceof Button) {
						child.setBackground(stripe);
					}
				}
			}
		}

		private void paintHeader(Event e) {
			GC gc = e.gc;
			Rectangle area = header.getClientArea();
			int height = area.height - 1;
			Color background = header.getBackground();
			Color foreground = header.getForeground();

			if (hover) {
				gc.setBackground(mix(background, foreground, HOVER_SHADE));
				gc.fillRectangle(0, 0, area.width, height);
			}

			gc.setAntialias(SWT.ON);
			gc.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
			Point titleSize = gc.textExtent(title);

			// the chevron: pointing right when collapsed, down when expanded
			int size = getArrowSize(titleSize.y);
			int half = size / 2;
			int depth = size * 3 / 4;
			int cx = HEADER_PADDING + half;
			int cy = height / 2;
			gc.setBackground(mix(background, foreground, ARROW_SHADE));
			if (expanded) {
				gc.fillPolygon(new int[] {cx - half, cy - depth / 2, cx + half, cy - depth / 2, cx, cy + depth - depth / 2});
			} else {
				gc.fillPolygon(new int[] {cx - depth / 2, cy - half, cx + depth - depth / 2, cy, cx - depth / 2, cy + half});
			}

			int x = HEADER_PADDING + size + HEADER_GAP;
			gc.setForeground(foreground);
			gc.drawText(title, x, (height - titleSize.y) / 2, true);
			x += titleSize.x + HEADER_GAP;

			gc.setFont(JFaceResources.getDialogFont());
			String countText = getCountText();
			Point countSize = gc.textExtent(countText);
			gc.setForeground(mix(background, foreground, COUNT_SHADE));
			gc.drawText(countText, x, (height - countSize.y) / 2, true);

			gc.setForeground(mix(background, foreground, SEPARATOR_SHADE));
			gc.drawLine(0, height, area.width, height);

			if (header.isFocusControl()) {
				gc.drawFocus(1, 1, area.width - 2, height - 2);
			}
		}
	}
}
