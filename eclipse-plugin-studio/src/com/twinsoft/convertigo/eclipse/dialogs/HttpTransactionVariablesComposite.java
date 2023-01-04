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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DynamicHttpVariable;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class HttpTransactionVariablesComposite extends ScrolledComposite {
	private AbstractHttpTransaction httpTransaction = null;
	private List<Button> checkboxes = new ArrayList<>(DynamicHttpVariable.values().length);
	private List<Text> customs = new LinkedList<>();
	
	public HttpTransactionVariablesComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	public HttpTransactionVariablesComposite(Composite parent, int style, AbstractHttpTransaction httpTransaction) {
		super(parent, style);
		build(httpTransaction);
	}
	
	public void build(AbstractHttpTransaction httpTransaction) {
		this.httpTransaction = httpTransaction;
		checkboxes.clear();
		customs.clear();
		Composite parent = getParent();
		ScrolledComposite scrolledComposite = this;
		
		for (Control c : this.getChildren()) {
			c.dispose();
		}
		
		Composite composite = new Composite(scrolledComposite, SWT.NONE);
		
		scrolledComposite.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				composite.setSize(scrolledComposite.getSize().x, composite.getSize().y);
			}
		});
		scrolledComposite.setExpandHorizontal(true);
		GridLayout gl = new GridLayout(3, false);
		gl.marginLeft = 10;
		gl.marginRight = 10;
		gl.horizontalSpacing = 20;
		composite.setLayout(gl);
		
		Label title = new Label(composite, SWT.NONE);
		title.setText("Add or remove dynamic variables for this '" + httpTransaction.getName() + "' HTTP Transaction.");
		title.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		
		for (DynamicHttpVariable v: DynamicHttpVariable.values()) {
			if (!v.can(httpTransaction) || v.prefix() != null) {
				continue;
			}
			Button checkBtn = new Button(composite, SWT.CHECK);
			checkboxes.add(checkBtn);
			checkBtn.setSelection(httpTransaction.getVariable(v.name()) != null);
			checkBtn.setData(DynamicHttpVariable.class.getName(), v);
			checkBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			Label labelName = new Label(composite, SWT.NONE);
			labelName.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			FontData fontData = labelName.getFont().getFontData()[0];
			Font font = new Font(parent.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			labelName.setFont(font);
			labelName.setText(v.display());
			C8oBrowser browserDescription = new C8oBrowser(composite, SWT.MULTI | SWT.WRAP);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.heightHint = 60;
			browserDescription.setLayoutData(gd);
			browserDescription.setUseExternalBrowser(true);
			if (SwtUtils.isDark()) {
				browserDescription.setBackground(parent.getBackground());
			}
			browserDescription.setText("<html>" +
					"<head>" +
					"<script type=\"text/javascript\">" +
						"document.oncontextmenu = new Function(\"return false\");" +
					"</script>" +
					"<style type=\"text/css\">" +
						  "body {" +
						    "margin: auto;" +
						    "height: 60px;" +
						    "display: table-cell;" +
						    "vertical-align: middle;" +
						    "font-family: Tahoma new, sans-serif;" +
						    "font-size: 0.7em;" +
						    "overflow-y: auto;" +
						    "color: $foreground$;" +
						    "background-color: $background$ } \n" +
						  "a { color: $link$; }" +
					"</style></head><body>" + v.description() + "</body></html>");
		}
		for (DynamicHttpVariable v: DynamicHttpVariable.values()) {
			if (!v.can(httpTransaction) || v.prefix() == null) {
				continue;
			}
			Button checkBtn = new Button(composite, SWT.NONE);
			checkBtn.setText("Add");
			checkBtn.setSelection(httpTransaction.getVariable(v.name()) != null);
			checkBtn.setData(DynamicHttpVariable.class.getName(), v);
			checkBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			Label labelName = new Label(composite, SWT.NONE);
			labelName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			FontData fontData = labelName.getFont().getFontData()[0];
			Font font = new Font(parent.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			labelName.setFont(font);
			labelName.setText(v.display());
			C8oBrowser browserDescription = new C8oBrowser(composite, SWT.MULTI | SWT.WRAP);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.heightHint = 60;
			browserDescription.setLayoutData(gd);
			browserDescription.setUseExternalBrowser(true);
			if (SwtUtils.isDark()) {
				browserDescription.setBackground(parent.getBackground());
			}
			browserDescription.setText("<html>" +
					"<head>" +
					"<script type=\"text/javascript\">" +
						"document.oncontextmenu = new Function(\"return false\");" +
					"</script>" +
					"<style type=\"text/css\">" +
						  "body {" +
						    "margin: auto;" +
						    "height: 60px;" +
						    "display: table-cell;" +
						    "vertical-align: middle;" +
						    "font-family: Tahoma new, sans-serif;" +
						    "font-size: 0.7em;" +
						    "overflow-y: auto;" +
						    "color: $foreground$;" +
						    "background-color: $background$ } \n" +
						  "a { color: $link$; }" +
					"</style></head><body>" + v.description() + "</body></html>");
			Composite addComposite = new Composite(composite, SWT.NONE);
			addComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
			FillLayout fl = new FillLayout(SWT.VERTICAL);
			fl.spacing = 5;
			addComposite.setLayout(fl);
			checkBtn.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					Composite line = new Composite(addComposite, SWT.NONE);
					line.setLayout(new GridLayout(4, false));
					
					new Label(line, SWT.NONE).setText("Name:");
					
					Text txt = new Text(line, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
					txt.setData(DynamicHttpVariable.class.getName(), v);
					txt.setToolTipText("set the variable name (empty is ignored)");
					customs.add(txt);
					
					new Label(line, SWT.NONE).setText(" Value:");
					
					txt = new Text(line, SWT.BORDER);
					txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
					txt.setToolTipText("set the variable default value (empty is the null value)");
					customs.add(txt);
					
					scrolledComposite.setContent(composite);
					composite.pack();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}
		scrolledComposite.setContent(composite);
		composite.pack();
	}
	
	public void applyProceed() {
		for (Button check: checkboxes) {
			DynamicHttpVariable v = (DynamicHttpVariable) check.getData(DynamicHttpVariable.class.getName());
			Variable dboVar = httpTransaction.getVariable(v.name());
			if (check.getSelection() && dboVar == null) {
				try {
					RequestableHttpVariable newDboVar = new RequestableHttpVariable();
					newDboVar.setName(v.name());
					if (v == DynamicHttpVariable.__body) {
						newDboVar.setHttpMethod("POST");
					}
					httpTransaction.addVariable(newDboVar);
				} catch (EngineException e) {
				}
			} else if (!check.getSelection() && dboVar != null) {
				httpTransaction.removeVariable((RequestableVariable) dboVar);
			}
		}
		for (Iterator<Text> i = customs.iterator(); i.hasNext();) {
			Text txtName = i.next();
			Text txtValue = i.next();
			if (txtName.getText().isBlank()) {
				continue;
			}
			DynamicHttpVariable v = (DynamicHttpVariable) txtName.getData(DynamicHttpVariable.class.getName());
			try {
				RequestableHttpVariable newDboVar = new RequestableHttpVariable();
				String name = v.prefix() + txtName.getText();
				String normalized = StringUtils.normalize(name);
				newDboVar.setName(normalized);
				if (!name.equals(normalized)) {
					newDboVar.setHttpName(txtName.getText());
				}
				if (v == DynamicHttpVariable.__POST_) {
					newDboVar.setHttpMethod("POST");
				}
				if (!txtValue.getText().isEmpty()) {
					newDboVar.setValueOrNull(txtValue.getText());
				}
				httpTransaction.addVariable(newDboVar);
			} catch (EngineException e) {
			}
		}
	}
	
}
