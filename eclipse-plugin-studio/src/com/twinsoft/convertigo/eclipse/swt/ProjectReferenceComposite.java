/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class ProjectReferenceComposite extends Composite {
	private ProjectUrlParser parser;

	public ProjectReferenceComposite(Composite parent, int style, ProjectUrlParser parser) {
		this(parent, style, parser, null);
	}
	
	public ProjectReferenceComposite(Composite parent, int style, ProjectUrlParser parser, Runnable onChange) {
		super(parent, style);
		this.parser = parser;
		
		setLayout(new GridLayout(2, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		Label label = new Label(this , SWT.NONE);
		label.setLayoutData(gd);
		label.setText("<project name>=<git or http URL>[:path=<optional subpath>][:branch=<optional branch>]\n\n");
		
		label = new Label(this, SWT.NONE);
		label.setText("Project remote URL");
		Text completGitUrl = new Text(this, SWT.NONE);
		completGitUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(gd);
		
		label = new Label(this, SWT.NONE);
		label.setText("Project name");
		Text projectName = new Text(this, SWT.NONE);
		projectName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		projectName.addModifyListener(e -> {
			parser.setProjectName(projectName.getText());
			if (!completGitUrl.getText().equals(parser.getProjectUrl())) {
				completGitUrl.setText(parser.getProjectUrl());
			}
			if (onChange != null) {
				onChange.run();
			}
		});
		
		label = new Label(this, SWT.NONE);
		label.setText("Git or http URL");
		Text gitUrl = new Text(this, SWT.NONE);
		gitUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gitUrl.addModifyListener(e -> {
			parser.setGitUrl(gitUrl.getText());
			String val = parser.toString();
			if (!completGitUrl.getText().equals(val)) {
				completGitUrl.setText(val);
			}
			if (onChange != null) {
				onChange.run();
			}
		});
		
		label = new Label(this, SWT.NONE);
		label.setText("Project Path");
		Text projectPath = new Text(this, SWT.NONE);
		projectPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		projectPath.addModifyListener(e -> {
			parser.setProjectPath(projectPath.getText());
			if (!completGitUrl.getText().equals(parser.getProjectUrl())) {
				completGitUrl.setText(parser.getProjectUrl());
			} else {
				if (!projectPath.getText().isEmpty()) {
					projectPath.setText("");
				}
			}
			if (onChange != null) {
				onChange.run();
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Git branch");
		Text gitBranch = new Text(this, SWT.NONE);
		gitBranch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gitBranch.addModifyListener(e -> {
			parser.setGitBranch(gitBranch.getText());
			if (!completGitUrl.getText().equals(parser.getProjectUrl())) {
				completGitUrl.setText(parser.getProjectUrl());
			} else {
				if (!gitBranch.getText().isEmpty()) {
					gitBranch.setText("");
				}
			}
			if (onChange != null) {
				onChange.run();
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Auto reset/pull");
		Button autoPull = new Button(this, SWT.CHECK);
		autoPull.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		autoPull.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				parser.setAutoPull(autoPull.getSelection());
				if (!completGitUrl.getText().equals(parser.getProjectUrl())) {
					completGitUrl.setText(parser.getProjectUrl());
				} else {
					if (autoPull.getSelection()) {
						autoPull.setSelection(false);
					}
				}
				if (onChange != null) {
					onChange.run();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		completGitUrl.addModifyListener(e -> {
			parser.setUrl(completGitUrl.getText());
			if (parser.isValid()) {
				if (!projectName.getText().equals(parser.getProjectName())) {
					projectName.setText(parser.getProjectName());
				}
				if (!gitUrl.getText().equals(parser.getGitUrl())) {
					gitUrl.setText(parser.getGitUrl());
				}
			} else {
				String val = completGitUrl.getText();
				if (!projectName.getText().equals(val)) {
					projectName.setText(val);
				}
				val = "";
				if (!gitUrl.getText().equals(val)) {
					gitUrl.setText(val);
				}
				
			}
			String txt = parser.getProjectPath() == null ? "" : parser.getProjectPath();
			if (!projectPath.getText().equals(txt)) {
				projectPath.setText(txt);
			}
			txt = parser.getGitBranch() == null ? "" : parser.getGitBranch();
			if (!gitBranch.getText().equals(txt)) {
				gitBranch.setText(txt);
			}
			autoPull.setSelection(parser.isAutoPull());
			if (onChange != null) {
				onChange.run();
			}
		});
		
		completGitUrl.setText(parser.toString());
	}

	public ProjectUrlParser getParser() {
		return parser;
	}

}
