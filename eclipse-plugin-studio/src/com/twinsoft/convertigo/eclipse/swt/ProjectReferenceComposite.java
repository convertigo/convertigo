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

package com.twinsoft.convertigo.eclipse.swt;

import org.apache.commons.lang3.StringUtils;
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
	private boolean userEvent = true;
	
	private Text completGitUrl;
	private Text projectName;
	private Text gitUrl;
	private Text projectPath;
	private Text gitBranch;
	private Button autoPull;

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
		completGitUrl = new Text(this, SWT.NONE);
		completGitUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		completGitUrl.addModifyListener(e -> {
			if (!userEvent) {
				return;
			}
			try {
				userEvent = false;
				parser.setUrl(completGitUrl.getText());
				projectName.setText(StringUtils.defaultString(parser.getProjectName()));
				gitUrl.setText(StringUtils.defaultString(parser.getGitUrl()));
				projectPath.setText(StringUtils.defaultString(parser.getProjectPath()));
				gitBranch.setText(StringUtils.defaultString(parser.getGitBranch()));
				autoPull.setSelection(parser.isAutoPull());
				if (onChange != null) {
					onChange.run();
				}
			} finally {
				userEvent = true;
			}
		});

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(gd);

		label = new Label(this, SWT.NONE);
		label.setText("Project name");
		projectName = new Text(this, SWT.NONE);
		projectName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		projectName.addModifyListener(e -> {
			if (!userEvent) {
				return;
			}
			try {
				userEvent = false;
				updateParser(onChange);
			} finally {
				userEvent = true;
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Git or http URL");
		gitUrl = new Text(this, SWT.NONE);
		gitUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gitUrl.addModifyListener(e -> {
			if (!userEvent) {
				return;
			}
			try {
				userEvent = false;
				updateParser(onChange);
			} finally {
				userEvent = true;
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Project Path");
		projectPath = new Text(this, SWT.NONE);
		projectPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		projectPath.addModifyListener(e -> {
			if (!userEvent) {
				return;
			}
			try {
				userEvent = false;
				updateParser(onChange);
			} finally {
				userEvent = true;
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Git branch");
		gitBranch = new Text(this, SWT.NONE);
		gitBranch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gitBranch.addModifyListener(e -> {
			if (!userEvent) {
				return;
			}
			try {
				userEvent = false;
				updateParser(onChange);
			} finally {
				userEvent = true;
			}
		});

		label = new Label(this, SWT.NONE);
		label.setText("Auto reset/pull");
		autoPull = new Button(this, SWT.CHECK);
		autoPull.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		autoPull.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!userEvent) {
					return;
				}
				try {
					userEvent = false;
					updateParser(onChange);
				} finally {
					userEvent = true;
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		completGitUrl.setText(parser.toString());
	}
	
	private void updateParser(Runnable onChange) {
		parser.setProjectName(projectName.getText());
		parser.setGitUrl(gitUrl.getText());
		parser.setProjectPath(projectPath.getText());
		parser.setGitBranch(gitBranch.getText());
		parser.setAutoPull(autoPull.getSelection());
		completGitUrl.setText(parser.getProjectUrl());
		if (onChange != null) {
			onChange.run();
		}
	}

	public ProjectUrlParser getParser() {
		return parser;
	}

}
