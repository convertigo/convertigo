/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.common.FontSource;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;

public class NgxFontSourceEditorComposite extends AbstractDialogComposite {

	private Composite leftPane;
	private ScrolledComposite scrolledComposite;
	private Table table;
	private Text txtSearch;
	private Button clearBtn;
	private Combo cbStyle, cbWeight, cbSubset;
	private C8oBrowser previewBrowser;
	
	private FontSource fs;
	private String selectedFontId;
	private String selectedFontStyle;
	private String selectedFontWeight;
	private String selectedFontSubset;
	
	public NgxFontSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		fs = (FontSource) cellEditor.getEditorData();
		initialize();
		search(txtSearch.getText(), fs.getFontId());
	}

	private void initialize() {
		setLayout(new GridLayout(2, false));
		
		leftPane = new Composite(this, SWT.NONE);
		leftPane.setLayout(new GridLayout(1, false));
		leftPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		leftPane.setBounds(0, 0, 32, 32);
		
		Composite searchPane = new Composite(leftPane, SWT.NONE);
		searchPane.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		searchPane.setLayout(new GridLayout(3, false));
		
		Label lblSearch = new Label(searchPane, SWT.NONE);
		lblSearch.setText("Search");
		
		txtSearch = new Text(searchPane, SWT.BORDER);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtSearch.setText("");
		txtSearch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				search(txtSearch.getText());
			}
		});
		
		clearBtn = new Button(searchPane, SWT.NONE);
		clearBtn.setText("X");
		clearBtn.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				txtSearch.setText("");
				txtSearch.setFocus();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				txtSearch.setText("");
				txtSearch.setFocus();
			}
		});
		
		scrolledComposite = new ScrolledComposite(leftPane, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		
		table = new Table(scrolledComposite, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addSelectionListener(new SelectionListener() {
			
			private void widgetSelected(TableItem[] selectedItems) {
				try {
					selectedFontId = selectedItems[0].getText(0);
					enableOK(selectedFontId != null);
					fillCombos();
					previewFont();
				} catch (Exception e) {}
			}
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetSelected(table.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(table.getSelection());
			}
		});
		
		TableColumn tblclmnFontName = new TableColumn(table, SWT.NONE);
		tblclmnFontName.setWidth(100);
		tblclmnFontName.setText("Font name");
		
		TableColumn tblclmnFontCategory = new TableColumn(table, SWT.NONE);
		tblclmnFontCategory.setWidth(100);
		tblclmnFontCategory.setText("Font type");
		
		scrolledComposite.setContent(table);
		scrolledComposite.setMinSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Composite composite = new Composite(leftPane, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		composite.setLayout(new GridLayout(2, false));
		
		Label lblStyle = new Label(composite, SWT.NONE);
		lblStyle.setText("Style");
		
		cbStyle = new Combo(composite, SWT.READ_ONLY);
		cbStyle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cbStyle.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedFontStyle = cbStyle.getItem(cbStyle.getSelectionIndex());
				previewFont();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selectedFontStyle = cbStyle.getItem(cbStyle.getSelectionIndex());
				previewFont();
			}
		});
		
		Label lblWeight = new Label(composite, SWT.NONE);
		lblWeight.setText("Weight");
		
		cbWeight = new Combo(composite, SWT.READ_ONLY);
		cbWeight.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cbWeight.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedFontWeight = cbWeight.getItem(cbWeight.getSelectionIndex());
				previewFont();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selectedFontWeight = cbWeight.getItem(cbWeight.getSelectionIndex());
				previewFont();
			}
		});
		
		Label lblSubset = new Label(composite, SWT.NONE);
		lblSubset.setText("Subset");
		
		cbSubset = new Combo(composite, SWT.READ_ONLY);
		cbSubset.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cbSubset.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedFontSubset = cbSubset.getItem(cbSubset.getSelectionIndex());
				previewFont();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selectedFontSubset = cbSubset.getItem(cbSubset.getSelectionIndex());
				previewFont();
			}
		});
		
		previewBrowser = new C8oBrowser(this, SWT.MULTI | SWT.WRAP);
		previewBrowser.setUseExternalBrowser(true);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.minimumWidth = 300;
		gridData.minimumHeight = 300;
		previewBrowser.setLayoutData(gridData);
		
		this.setSize(new org.eclipse.swt.graphics.Point(500,500));
	}

	private void selectTableItem(String fontId) {
		if (fontId != null) {
			for (TableItem tableItem: table.getItems()) {
				if (tableItem.getText(0).equals(fontId)) {
					table.setSelection(tableItem);
					Event event = new Event();
					event.item = tableItem;
					table.notifyListeners(SWT.Selection, event);
					return;
				}
			}
		} else {
			table.deselectAll();
			table.notifyListeners(SWT.Selection, null);
		}
	}
	
	private void fillTable(String searchedFont) {
		selectedFontId = null;
		table.removeAll();
		
		Map<String, JSONObject> map = ComponentManager.getFonts();
		List<String> fontByNames = new ArrayList<>(map.keySet());
		Collections.sort(fontByNames);
		
		for (String fontName: fontByNames) {
			if (!searchedFont.isBlank() && !fontName.isBlank() && 
						fontName.indexOf(searchedFont) == -1) {
				continue;
			}
			try {
				JSONObject jsonFont = map.get(fontName);
				String fontType = jsonFont.getString("type");
				TableItem tableItem = new TableItem(table, SWT.NONE);
				tableItem.setText(new String[] {fontName, fontType});
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void search(String searchedFont) {
		search(searchedFont, null);
	}
	
	private void search(String searchedFont, String fontSourceId) {
		final Display display = getDisplay();
		final Shell shell = display.getActiveShell();
		
		display.asyncExec(() -> {
			Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
			shell.setCursor(waitCursor);
			
			if (!txtSearch.isDisposed() && !table.isDisposed()) {
				enableOK(false);
				selectTableItem(null);
				
				fillTable(searchedFont);
				previewFont();
				
				if (fontSourceId != null) {
					selectTableItem(fontSourceId);
				}
			}
			
			shell.setCursor(null);
			waitCursor.dispose();
		});
	}
	
	private void clearCombos() {
		selectedFontStyle = null;
		selectedFontWeight = null;
		selectedFontSubset = null;
		
		cbStyle.removeAll();
		cbWeight.removeAll();
		cbSubset.removeAll();
		
	}
	
	private String getDefFontStyle() {
		if (selectedFontId != null && selectedFontId.equals(fs.getFontId())) {
			String fontSourceStyle = fs.getFontStyle();
			if (fontSourceStyle != null) {
				return fontSourceStyle;
			}
		}
		return "normal";
	}
	
	private String getDefFontWeight() {
		if (selectedFontId != null && selectedFontId.equals(fs.getFontId())) {
			String fontSourceWeight = fs.getFontWeight();
			if (fontSourceWeight != null) {
				return fontSourceWeight;
			}
		}
		return "400";
	}
	
	private String getDefFontSubset(String defSubset) {
		if (selectedFontId != null && selectedFontId.equals(fs.getFontId())) {
			String fontSourceSubset = fs.getFontSubset();
			if (fontSourceSubset != null) {
				return fontSourceSubset;
			}
		}
		return defSubset;
	}
	
	private void fillCombos() {
		clearCombos();
		if (selectedFontId != null && !selectedFontId.isBlank()) {
			Map<String, JSONObject> map = ComponentManager.getFonts();
			JSONObject jsonFont = map.get(selectedFontId);
			if (jsonFont != null) {
				try {
					int defStyleIndex = 0;
					JSONArray styles = jsonFont.getJSONArray("styles");
					for (int i = 0; i < styles.length(); i++) {
						String style = styles.getString(i);
						defStyleIndex = style.equals(getDefFontStyle()) ? i : defStyleIndex;
						cbStyle.add(style);
					}
					cbStyle.select(defStyleIndex);
					selectedFontStyle = cbStyle.getItem(defStyleIndex);
				} catch (Exception e) {
					
				}
				
				try {
					int defWeightIndex = 0;
					JSONArray weights = jsonFont.getJSONArray("weights");
					for (int i = 0; i < weights.length(); i++) {
						String weight = weights.getString(i);
						defWeightIndex = weight.equals(getDefFontWeight()) ? i : defWeightIndex;
						cbWeight.add(weight);
					}
					cbWeight.select(defWeightIndex);
					selectedFontWeight = cbWeight.getItem(defWeightIndex);
				} catch (Exception e) {
					
				}
				
				try {
					int defSubsetIndex = 0;
					String defSubset = jsonFont.has("defSubset") ? jsonFont.getString("defSubset"):null;
					JSONArray subsets = jsonFont.getJSONArray("subsets");
					for (int i = 0; i < subsets.length(); i++) {
						String subset = subsets.getString(i);
						defSubsetIndex = subset.equals(getDefFontSubset(defSubset)) ? i : defSubsetIndex;
						cbSubset.add(subset);
					}
					cbSubset.select(defSubsetIndex);
					selectedFontSubset = cbSubset.getItem(defSubsetIndex);
				} catch (Exception e) {
					
				}
			}
		}
	}
	
	private void previewFont() {
		if (previewBrowser == null)
			return;
		
		String html = "<html><body>Please select a font and wait for its loading<br>then apply style, weight and or subset</body></html>";
		
		if (selectedFontId != null && !selectedFontId.isBlank()) {
			JSONObject jsonFont = ComponentManager.getFont(selectedFontId);
			if (jsonFont != null) {
				try {
					String unicoderange = "";
					try {
						unicoderange = jsonFont.getJSONObject("unicodeRange").getString(selectedFontSubset);
					} catch (Exception e) {}
					
					JSONObject jonUrl = jsonFont.getJSONObject("variants")
												.getJSONObject(selectedFontWeight)
												.getJSONObject(selectedFontStyle)
												.getJSONObject(selectedFontSubset)
												.getJSONObject("url");
					String urls = "";
					@SuppressWarnings("unchecked")
					Iterator<String> it = jonUrl.keys();
					while (it.hasNext()) {
						String format = it.next();
						if (!urls.isEmpty()) urls += ",";
						urls += " url('"+ jonUrl.getString(format) +"') format('"+ format +"')";
					}
					
					String fontFamily = jsonFont.getString("family");
					//String fontCategory = jsonFont.getString("category");
					
					String fontface = "@font-face{\n"
							+ "font-family: \""+ fontFamily +"\";\n"
							+ "font-style: \""+ selectedFontStyle +"\";\n"
							+ "font-variant: \""+ selectedFontSubset +"\";\n"
							+ "font-weight: "+ selectedFontWeight +";\n"
							+ "font-display: block;\n"
							+ "src:"+ urls + ";\n"
							+ "unicode-range: "+ unicoderange +"\n"
							+ "}";
					
					html = "<!DOCTYPE html> \n"
							+ "<html><head> \n"
							+ "<script type=\"text/javascript\"> \n"
							+ "document.oncontextmenu = new Function(\"return false\"); \n"
							+ "</script> \n"
							+ "<style> \n"
							+ fontface + " \n"
							+ "</style> \n"
							+ "</head> \n"
							+ "<body> \n"
							+ "<p><font face=\"" + fontFamily + "\" size=\"20\"> \n"
							+ "<br>Sphinx of black quartz, judge my vow.\n"
							+ "</font></p> \n"
							+ "</body></html>";
							
					//System.out.println(html);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				html = "<html><body>Unabled to preview font !</body></html>";
			}
		}
		
		previewBrowser.setText(html);		
	}
	
	
	@Override
	public void performPostDialogCreation() {
		super.performPostDialogCreation();
		enableOK(selectedFontId != null);
	}

	private void enableOK(boolean enabled) {
		if (parentDialog != null) {
			((EditorFrameworkDialog)parentDialog).enableOK(enabled);
		}
	}
	
	public Object getValue() {
		if (selectedFontId != null) {
			String fontFamily = selectedFontId;
			try {
				fontFamily = ComponentManager.getFonts().get(selectedFontId).getString("family");
			} catch (Exception e) {}
			return new FontSource(selectedFontId, fontFamily, selectedFontWeight, selectedFontStyle, selectedFontSubset);
		}
		return fs;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
