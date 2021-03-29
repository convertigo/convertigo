/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class MobileApplicationTranslationsDialogComposite extends MyAbstractDialogComposite {

	private List<String> languages = null;
	
	private Combo cbFrom, cbTo;
	private Button btnAuto;
	private Label lbLangFrom;
	private Label lbLangTo;
	
	public MobileApplicationTranslationsDialogComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		this.setLayout(gridLayout);
		
		Label lbFrom = new Label(this, SWT.NONE);
		lbFrom.setText("Please select the source language:");
		lbFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		cbFrom = new Combo(this, SWT.READ_ONLY);
		cbFrom.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lbLangFrom.setText(getLanguage(cbFrom));
			}
		});
		cbFrom.setItems(getLanguages());
		cbFrom.select(getIndex(Locale.getDefault().getLanguage()));
		cbFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		lbLangFrom = new Label(this, SWT.NONE);
		lbLangFrom.setText(Locale.getDefault().getDisplayLanguage());
		lbLangFrom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lbTo = new Label(this, SWT.NONE);
		lbTo.setText("Please select the target language:");
		lbTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		cbTo = new Combo(this, SWT.READ_ONLY);
		cbTo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lbLangTo.setText(getLanguage(cbTo));
			}
		});
		cbTo.setItems(getLanguages());
		cbTo.select(getIndex(Locale.getDefault().getLanguage()));
		cbTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		lbLangTo = new Label(this, SWT.NONE);
		lbLangTo.setText(Locale.getDefault().getDisplayLanguage());
		lbLangTo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnAuto = new Button(this, SWT.CHECK | SWT.WRAP);
		btnAuto.setText(" Automatic translation from source to target language ?\n(using Free Google Translate API - limited by quotas)");
		btnAuto.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label lbInfo = new Label(this, SWT.WRAP);
		String s = 	"\nFor each of the source and target languages, if no translation file exists, it will be created in the " +
					"\"DisplayObjects/mobile/assets/i18n\" directory of your project: <language>.json." +
					"\nIf the file exists, it will simply be updated: the previous translations will not be modified." + 
					"\nIf you want to perform automatic translation from the source language to the target one, please check the option.";
		lbInfo.setText(s);
		lbInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
	}

	private String[] getLanguages() {
		if (languages == null) {
			languages = new ArrayList<String>();
			Locale locales[] = Locale.getAvailableLocales();
			for (int i = 0; i < locales.length; i++) {
				String language = locales[i].getLanguage();
				if (!languages.contains(language) && !language.isEmpty()) {
					languages.add(language);
				}
			}
			Collections.sort(languages);
		}
		return languages.toArray(new String[languages.size()]);
	}
	
	private int getIndex(String language) {
		return languages.indexOf(language);
	}
	
	private String getLanguage(Combo cb) {
		Locale locale = new Locale(cb.getText());
		return locale.getDisplayLanguage();
	}
	
	@Override
	public Object getValue(String name) {
		if ("from".equals(name)) {
			return new Locale(cbFrom.getText());
		}
		if ("to".equals(name)) {
			return new Locale(cbTo.getText());
		}
		if ("auto".equals(name)) {
			return btnAuto.getSelection();
		}
		return null;
	}
}
