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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.beans.rest.PathParameter;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EngineException;

public class UrlMappingTreeObject extends DatabaseObjectTreeObject {

	public UrlMappingTreeObject(Viewer viewer, DatabaseObject object) {
		this(viewer, object, false);
	}

	public UrlMappingTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public UrlMapping getObject() {
		return (UrlMapping) super.getObject();
	}
	
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			try {
				boolean needReload = false;
				UrlMapping urlMapping = getObject();
				// An UrlMappingOperation has been added : add all path parameters
				if (databaseObject instanceof UrlMappingOperation) {
					if (urlMapping.equals(databaseObject.getParent())) {
						UrlMappingOperation operation = (UrlMappingOperation)databaseObject;
						if (operation.bNew) {
							for (String variableName : urlMapping.getPathVariableNames()) {
		    					UrlMappingParameter parameter = null;
		    					try {
		    						parameter = operation.getParameterByName(variableName);
		    					}
		    					catch (EngineException e) {
		    						try {
			    						parameter = new PathParameter();
			    						parameter.setName(variableName);
			    						parameter.bNew = true;
			    						
			    						operation.add(parameter);
			    						operation.hasChanged = true;
			    						needReload = true;
		    						} catch (EngineException ex) {
		    							ConvertigoPlugin.logException(ex, "Error when adding the parameter \""+variableName+"\"");
		    						}
		    					}
							}
						}
						if (needReload) {
							ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
						}
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logWarning(e, "Could not reload in tree Mapping \""+databaseObject.getName()+"\" !");
			}
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			
			// Case Mapping path has changed
			if ("path".equals(propertyName)) {
				if (treeObject.equals(this)) {
    		    	try {
    		    		UrlMapping urlMapping = (UrlMapping) databaseObject;
    		    		
    		    		String oldPath = (String)treeObjectEvent.oldValue;
    		    		
    		    		List<String> oldPathVariableNames = urlMapping.getPathVariableNames(oldPath);
    		    		List<String> newPathVariableNames = urlMapping.getPathVariableNames();
    		    		
    		    		if (!oldPathVariableNames.equals(newPathVariableNames)) {
    		    			int oldLen = oldPathVariableNames.size();
    		    			int newLen = newPathVariableNames.size();
    		    			
							/*MessageBox messageBox = new MessageBox(viewer.getControl().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO); 
							messageBox.setMessage("Do you really want to update mapping path parameters?");
							messageBox.setText("Update parameters\"?");
							int ret = messageBox.open();*/
    		    			int ret = SWT.YES;
							
							if (ret == SWT.YES) {
	    		    			// case of deletion
	    		    			if (newLen < oldLen) {
		    		    			for (String variableName : oldPathVariableNames) {
		    		    				if (!newPathVariableNames.contains(variableName)) {
					    		    		for (UrlMappingOperation operation : urlMapping.getOperationList()) {
					    		    			for (UrlMappingParameter parameter : operation.getParameterList()) {
					    		    				if (parameter.getName().equals(variableName)) {
					    		    					if (parameter.getType().equals(Type.Path)) {
					    		    						try {
					    		    							parameter.delete();
					    		    							operation.remove(parameter);
					    		    							operation.hasChanged = true;
					    		    						} catch (EngineException e) {
					    		    							ConvertigoPlugin.logException(e, "Error when deleting the parameter \""+variableName+"\"");
					    		    						}
					    		    					}
					    		    				}
					    		    			}
					    		    		}
		    		    				}
		    		    			}
	    		    			}
	    		    			// case of rename
	    		    			else if (newLen == oldLen) {
	    		    				for (int i=0; i<oldLen; i++) {
	    		    					String variableName = oldPathVariableNames.get(i);
				    		    		for (UrlMappingOperation operation : urlMapping.getOperationList()) {
				    		    			for (UrlMappingParameter parameter : operation.getParameterList()) {
				    		    				if (parameter.getName().equals(variableName)) {
				    		    					if (parameter.getType().equals(Type.Path)) {
				    		    						try {
				    		    							parameter.setName(newPathVariableNames.get(i));
				    		    							parameter.hasChanged = true;
				    		    						} catch (EngineException e) {
				    		    							ConvertigoPlugin.logException(e, "Error when renaming the parameter \""+variableName+"\"");
				    		    						}
				    		    					}
				    		    				}
				    		    			}
				    		    		}
	    		    				}
	    		    			}
	    		    			// case of creation
	    		    			else {
	    		    				for (String variableName : newPathVariableNames) {
		    		    				for (UrlMappingOperation operation : urlMapping.getOperationList()) {
		    		    					UrlMappingParameter parameter = null;
		    		    					try {
		    		    						parameter = operation.getParameterByName(variableName);
		    		    					}
		    		    					catch (EngineException e) {
		    		    						try {
			    		    						parameter = new PathParameter();
			    		    						parameter.setName(variableName);
			    		    						parameter.bNew = true;
			    		    						
			    		    						operation.add(parameter);
			    		    						
		    		    						} catch (EngineException ex) {
		    		    							ConvertigoPlugin.logException(ex, "Error when adding the parameter \""+variableName+"\"");
		    		    						}
		    		    					}
		    		    				}
	    		    				}
	    		    			}
    		    			
	    		    			ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
							}
    		    		}
    		    		
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Mapping \""+databaseObject.getName()+"\" !");
    				}
				}
			}
		}
	}
}
