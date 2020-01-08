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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.servlets.RestApiServlet;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.SwaggerUtils;

public class ModelObjectEditorComposite extends AbstractDialogComposite {
	protected List list = null;

	public ModelObjectEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		initialize();
		
		String referenceModel = (String) cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		Project project = cellEditor.databaseObjectTreeObject.getProjectTreeObject().getObject();
		fillList(project, referenceModel);		
	}

	protected void initialize() {
		Label label0 = new Label (this, SWT.NONE);
		label0.setText ("Please choose a model");
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 200;
		list = new List(this, SWT.BORDER | SWT.V_SCROLL);
		list.setLayoutData (data);
        
		GridLayout gridLayout = new GridLayout();
		setLayout(gridLayout);
		setSize(new Point(408, 251));
	}
	
	private void fillList(Project project, String referenceModel) {
		if (project != null) {
			UrlMapper urlMapper = project.getUrlMapper();
			if (urlMapper != null) {
				java.util.List<String> modelList = new ArrayList<String>();
				try {
					String mapperModels = urlMapper.getModels();
					JSONObject json = mapperModels.isEmpty() ? new JSONObject() : new JSONObject(mapperModels);
					Iterator<?> it = json.keys();
					while (it.hasNext()) {
						modelList.add((String) it.next()); 
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				try {
					String projectName = project.getName();
					File targetDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + SwaggerUtils.jsonSchemaDirectory);
					RestApiServlet.buildSwaggerDefinition(projectName, false);
					
					Collection<File> jsonschemas = FileUtils.listFiles(targetDir, new String[] { "jsonschema" }, false);
					for (File file : jsonschemas) {
						String id = file.getName() + "#";
						if (file.getName().equals(projectName+".jsonschema")) continue;
						
						String content = FileUtils.readFileToString(file, "UTF-8");
						JSONObject json = new JSONObject(content);
						JSONObject definitions = json.getJSONObject("definitions");
						Iterator<?> it = definitions.keys();
						while (it.hasNext()) {
							String key = (String) it.next();
							modelList.add(id + "/definitions/"+ key); 
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				modelList.add("");
				Collections.sort(modelList);
				
				int index = 0, selected = -1;
				for (String ref: modelList) {
					list.add(ref);
					if (ref.equalsIgnoreCase(referenceModel)) {
						selected = index;
					}
					index++;
				}
				
				if (selected != -1) {
					list.setSelection(selected);
				}
			}
		}
	}
	
	@Override
	public Object getValue() {
		if (list.getSelectionCount() == 1) {
			return list.getSelection()[0];
		}
		return "";
	}

}
