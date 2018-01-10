/*
 * Copyright (c) 2001-2013 Convertigo SA.
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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/property_editors/SqlQueryEditorComposite.java $
 * $Author: jmc $
 * $Revision: 35531 $
 * $Date: 2013-10-22 18:18:44 +0200 (mar., 22 oct. 2013) $
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

public class CacheEditorComposite extends AbstractDialogComposite {

	private Group groupResponseLifetime = null;
	private Group groupOptions = null;
	private Text textResponseLifetime = null;
	private Label labelSeparator, labelSeparator1 = null;
	private Button buttonToggleShowHide = null;
	private Button buttonGenerate = null;
	private Button buttonDelete = null;
	private Combo comboMode = null;
	private Combo comboHours, comboMinutes, comboSeconds = null;
	private Combo comboDayWeek, comboDayMonth = null;
	private Text textTimeInSeconds = null;
	private String responseLifeTime = null;

	public CacheEditorComposite(Composite parent, int style,
			AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);

		// Initialize widgets if we have already a value
		String value = (String) cellEditor.databaseObjectTreeObject
				.getPropertyValue(cellEditor.propertyDescriptor.getId());
		responseLifeTime = value;

		initialize();
		// We initialize widgets in case we have already a value
		initializeWidgets();
						
		this.getShell().addShellListener(new ShellListener() {
			public void shellActivated(ShellEvent event) {
				getDisplay().getActiveShell().setSize(370, 300);
			}
			public void shellClosed(ShellEvent arg0) { }
			public void shellDeactivated(ShellEvent arg0) { }
			public void shellDeiconified(ShellEvent arg0) { }
			public void shellIconified(ShellEvent arg0) { }
		});
	}

	private void initializeWidgets() {
		String patterns[] = { "absolute,([0-9]*)",
				"daily,([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])",
				"weekly,([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]),([1-7])",
				"monthly,([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]),([01][0-9]|2[0-9]|3[01])" };

		textResponseLifetime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				manageOK();
			}
		});
		textResponseLifetime.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
			}

			@Override
			public void focusGained(FocusEvent arg0) {
				manageOK();
			}
		});

		for (int i = 0; i < patterns.length; i++) {
			Pattern p = Pattern.compile(patterns[i]);

			Matcher matcher = p.matcher(responseLifeTime);
			if (matcher.find()) {
				buttonToggleShowHide.setSelection(true);
				showGeneratorTool(true, true);

				comboMode.select(i);
				// In case we have absolute value
				if (i == 0) {
					textTimeInSeconds.setText(matcher.group(1));
				} else {
					// In case we have daily, weekly or monthly value
					showWidgetsGeneratorTools(i, true);
					comboHours.select(Integer.parseInt(matcher.group(1)));
					comboMinutes.select(Integer.parseInt(matcher.group(2)));
					comboSeconds.select(Integer.parseInt(matcher.group(3)));

					if (i == 2)
						comboDayWeek
								.select(Integer.parseInt(matcher.group(4)) - 1);
					if (i == 3)
						comboDayMonth
								.select(Integer.parseInt(matcher.group(4)) - 1);
				}

			}
		}

	}

	private void initialize() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(GridData.FILL_BOTH));

		/* GROUP Response Lifetime */
		groupResponseLifetime = new Group(this, SWT.NONE);
		groupResponseLifetime.setText("Response lifetime");
		groupResponseLifetime.setLayout(new GridLayout(2, false));
		groupResponseLifetime.setLayoutData(new GridData(GridData.FILL_BOTH));

		textResponseLifetime = new Text(groupResponseLifetime, SWT.BORDER);
		if (responseLifeTime != null)
			textResponseLifetime.setText(responseLifeTime);
		textResponseLifetime.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));

		buttonDelete = new Button(groupResponseLifetime, SWT.FLAT | SWT.NONE);
		buttonDelete.setText("X");
		buttonDelete.setToolTipText("Delete value");
		buttonDelete.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				deleteValue();
				CacheEditorComposite.this.parentDialog.okPressed();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				deleteValue();
				CacheEditorComposite.this.parentDialog.okPressed();
			}
		});
		buttonDelete.setLayoutData(new GridData());

		buttonToggleShowHide = new Button(groupResponseLifetime, SWT.TOGGLE);
		buttonToggleShowHide.setText("Show generator tool");
		buttonToggleShowHide.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				showGeneratorTool(buttonToggleShowHide.getSelection(), false);
			}
		});
		GridData toggleData = new GridData(GridData.FILL_HORIZONTAL);
		toggleData.horizontalSpan = 2;
		buttonToggleShowHide.setLayoutData(toggleData);

		/* END GROUP Response Lifetime */

		/* GROUP Generator tools */
		GridData optionsData = new GridData(GridData.FILL_BOTH);
		optionsData.exclude = true;
		optionsData.horizontalSpan = 2;

		groupOptions = new Group(groupResponseLifetime, SWT.NONE);
		groupOptions.setText("Generator tool");
		groupOptions.setVisible(false);
		groupOptions.setLayoutData(optionsData);
		groupOptions.setLayout(new GridLayout(7, false));

		// Manage all buttons from the generator tools
		manageGenerateButtons();

		GridData generateData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		generateData.horizontalSpan = 7;

		buttonGenerate = new Button(groupOptions, SWT.NONE);
		buttonGenerate.setText("Generate");
		buttonGenerate.setLayoutData(generateData);
		buttonGenerate.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				textResponseLifetime.setText("");
				if (comboMode.getSelectionIndex() == 0) {
					if (textTimeInSeconds.getText().matches("[0-9]*"))
						textResponseLifetime.setText("absolute,"
								+ textTimeInSeconds.getText());
					else {
						MessageBox error = new MessageBox(getShell(),
								SWT.ICON_ERROR);
						error.setMessage("Please enter a number!");
						error.open();
					}
				} else {
					textResponseLifetime.setText(comboMode.getItem(comboMode
							.getSelectionIndex())
							+ ","
							+ comboHours.getItem(comboHours.getSelectionIndex())
							+ ":"
							+ comboMinutes.getItem(comboMinutes
									.getSelectionIndex())
							+ ":"
							+ comboSeconds.getItem(comboSeconds
									.getSelectionIndex()));
					if (comboMode.getSelectionIndex() == 2)
						textResponseLifetime.setText(textResponseLifetime
								.getText()
								+ ","
								+ comboDayWeek.getItem(comboDayWeek
										.getSelectionIndex()));
					if (comboMode.getSelectionIndex() == 3)
						textResponseLifetime.setText(textResponseLifetime
								.getText()
								+ ","
								+ comboDayMonth.getItem(comboDayMonth
										.getSelectionIndex()));
				}
			}
		});

		/* END GROUP Generator tools */
	}

	private void showGeneratorTool(boolean show, boolean auto) {
		groupOptions.setVisible(show);
		buttonToggleShowHide
				.setText(!buttonToggleShowHide.getSelection() ? "Show generator tool"
						: "Hide generator tool");

		((GridData) groupOptions.getLayoutData()).exclude = !show;

		Point p = this.getShell().getSize();
		if (!auto)
			getShell().pack(true);
		else
			pack(true);

		this.getShell().setSize(p);
	}

	private void showWidgetsGeneratorTools(int mode, boolean auto) {
		comboHours.setVisible(mode > 0);
		((GridData) comboHours.getLayoutData()).exclude = mode <= 0;

		labelSeparator.setVisible(mode > 0);
		((GridData) labelSeparator.getLayoutData()).exclude = mode <= 0;

		comboMinutes.setVisible(mode > 0);
		((GridData) comboMinutes.getLayoutData()).exclude = mode <= 0;

		labelSeparator1.setVisible(mode > 0);
		((GridData) labelSeparator1.getLayoutData()).exclude = mode <= 0;

		comboSeconds.setVisible(mode > 0);
		((GridData) comboSeconds.getLayoutData()).exclude = mode <= 0;

		textTimeInSeconds.setVisible(mode == 0);
		((GridData) textTimeInSeconds.getLayoutData()).exclude = mode > 0;

		comboDayWeek.setVisible(mode == 2);
		((GridData) comboDayWeek.getLayoutData()).exclude = mode != 2;

		comboDayMonth.setVisible(mode == 3);
		((GridData) comboDayMonth.getLayoutData()).exclude = mode != 3;

		if (!auto)
			getShell().pack(true);
		else
			pack(true);
	}

	private void manageOK() {
		parentDialog.enableOK(!textResponseLifetime.getText().equals(""));
	}

	private void manageGenerateButtons() {
		comboMode = new Combo(groupOptions, SWT.READ_ONLY);
		String items[] = { "absolute", "daily", "weekly", "monthly" };
		comboMode.setItems(items);
		comboMode.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				showWidgetsGeneratorTools(comboMode.getSelectionIndex(), false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				showWidgetsGeneratorTools(comboMode.getSelectionIndex(), false);
			}
		});
		comboMode.select(0);
		comboMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData textTimeInSecData = new GridData(GridData.FILL_HORIZONTAL);
		textTimeInSecData.horizontalSpan = 5;

		textTimeInSeconds = new Text(groupOptions, SWT.BORDER);
		textTimeInSeconds.setMessage("time in secs");
		textTimeInSeconds.setLayoutData(textTimeInSecData);

		for (Control control : Arrays.asList(comboHours = new Combo(
				groupOptions, SWT.READ_ONLY), labelSeparator = new Label(
				groupOptions, SWT.READ_ONLY), comboMinutes = new Combo(
				groupOptions, SWT.READ_ONLY), labelSeparator1 = new Label(
				groupOptions, SWT.READ_ONLY), comboSeconds = new Combo(
				groupOptions, SWT.READ_ONLY), comboDayMonth = new Combo(
				groupOptions, SWT.READ_ONLY), comboDayWeek = new Combo(
				groupOptions, SWT.READ_ONLY))) {

			if (control instanceof Label) {
				GridData data = new GridData();
				data.exclude = true;
				((Label) control).setText(":");
				control.setLayoutData(data);
			} else {
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				data.exclude = true;
				control.setLayoutData(data);
			}
			control.setVisible(false);
		}

		String hours[] = new String[24];
		for (int i = 0; i < hours.length; i++)
			hours[i] = i < 10 ? "0" + i : i + "";
		comboHours.setItems(hours);
		comboHours.select(0);

		String minutes[] = new String[60];
		for (int i = 0; i < minutes.length; i++)
			minutes[i] = i < 10 ? "0" + i : i + "";
		comboMinutes.setItems(minutes);
		comboMinutes.select(0);

		comboSeconds.setItems(minutes);
		comboSeconds.select(0);

		String weekly[] = new String[7];
		for (int i = 0; i < weekly.length; i++)
			weekly[i] = (i + 1) + "";
		comboDayWeek.setItems(weekly);
		comboDayWeek.select(0);

		String monthly[] = new String[31];
		for (int i = 0; i < monthly.length; i++)
			monthly[i] = (i + 1) < 10 ? "0" + (i + 1) : (i + 1) + "";
		comboDayMonth.setItems(monthly);
		comboDayMonth.select(0);
	}

	@Override
	public Object getValue() {
		return textResponseLifetime.getText();
	}

	public Object deleteValue() {
		textResponseLifetime.setText("");
		return getValue();
	}
}
