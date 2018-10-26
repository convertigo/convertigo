package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class UrlAuthenticationTreeObject extends DatabaseObjectTreeObject implements INamedSourceSelectorTreeObject {

	public UrlAuthenticationTreeObject(Viewer viewer, DatabaseObject object) {
		super(viewer, object);
	}

	public UrlAuthenticationTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public UrlAuthentication getObject() {
		return (UrlAuthentication) super.getObject();
	}

	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {

			@Override
			Object thisTreeObject() {
				return UrlAuthenticationTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof UrlAuthentication) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c) ||
						TransactionTreeObject.class.isAssignableFrom(c))
					{
						list.add("authRequestable");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof UrlAuthentication) {
					return "authRequestable".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof UrlAuthentication) {
					if ("authRequestable".equals(propertyName)) {
						return nsObject instanceof RequestableObject;
					}
				}
				return false;
			}

			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}

			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof UrlAuthentication) {
								if ("authRequestable".equals(propertyName)) {
									((UrlAuthentication)getObject()).setAuthRequestable(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						
						ConvertigoPlugin.projectManager.getProjectExplorerView().updateTreeObject(UrlAuthenticationTreeObject.this);
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
	
}
