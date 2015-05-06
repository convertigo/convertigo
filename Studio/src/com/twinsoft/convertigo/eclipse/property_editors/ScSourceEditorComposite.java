package com.twinsoft.convertigo.eclipse.property_editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class ScSourceEditorComposite  extends AbstractDialogComposite implements ITreeContentProvider,
	ILabelProvider, ISelectionChangedListener {
	
	private TreeViewer screenClasses;
	
	private class TVObject {
		protected String name;
		protected TVObject parent;

		public TVObject(String name) {
			this.name = name;
		}
	}
	
	private class TVRoot extends TVObject {
		public TVRoot() {
			super("root");
			screenClassList = new ArrayList<TVScreenClass>();
		}
	
		private List<TVScreenClass> screenClassList;
		
		public void addScreenClass(TVScreenClass screenClass) {
			screenClassList.add(screenClass);
			screenClass.parent = this;
		}

		public List<TVScreenClass> getScreenClasses() {
			return screenClassList;
		}
	}
	
	private class TVScreenClass extends TVObject {
		public TVScreenClass(String name) {
			super(name);
			screenClasses = new ArrayList<TVScreenClass>();
		}
		
		private List<TVScreenClass> screenClasses;
		
		public void addScreenClass(TVScreenClass screenClass) {
			screenClasses.add(screenClass);
			screenClass.parent = this;
		}

		public List<TVScreenClass> getScreenClasses() {
			return screenClasses;
		}
	}
	
	private String requestableSource;
	private ScreenClass sourceScreenClass;
	
	public ScSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		requestableSource = (String) cellEditor.databaseObjectTreeObject
		.getPropertyValue(cellEditor.propertyDescriptor.getId());
		
		HtmlConnector connector = (HtmlConnector) cellEditor.databaseObjectTreeObject.getObject().getConnector();
		
		sourceScreenClass = connector.getScreenClassByName(requestableSource);
		if (sourceScreenClass == null){
			ScreenClass defaultScreenClass = connector.getDefaultScreenClass();
			sourceScreenClass = connector.getScreenClassByName(defaultScreenClass.getName());
		}
		
		initialize();
	}

	private void initialize() {
		setLayout(new FillLayout());
		screenClasses = new TreeViewer(this);
		screenClasses.setContentProvider(this);
		screenClasses.setLabelProvider(this);
		screenClasses.setInput(getInitalInput());
		screenClasses.expandAll();
		screenClasses.addSelectionChangedListener(this);

		screenClasses.collapseAll();
	}
	
	@Override
	public void performPostDialogCreation() {
		if (selectedTVObject != null) {
			ISelection selection = new StructuredSelection(selectedTVObject);
			screenClasses.setSelection(selection, true);
		} else if (!cellEditor.databaseObjectTreeObject.getObject().bNew) {
			ConvertigoPlugin.logError("Requestable source not found ('" + requestableSource
					+ "'). Please check your projects or choose another source.", true);
			parentDialog.enableOK(false);
		}
	}
	
	private TVObject selectedTVObject = null;
	
	private Object getInitalInput() {
		TVRoot tvRoot = new TVRoot();
		
		HtmlConnector connector = (HtmlConnector) sourceScreenClass.getConnector();
		
		ScreenClass defaultScreenClass = connector.getDefaultScreenClass();
		
		TVScreenClass tvScreenClass = new TVScreenClass(defaultScreenClass.getName());
		tvRoot.addScreenClass(tvScreenClass);
		if (requestableSource.equals(defaultScreenClass.getName())) {
			selectedTVObject = tvScreenClass;
		}
		List<ScreenClass> screenClasses = defaultScreenClass.getInheritedScreenClasses();
		
		for (ScreenClass screenClass : screenClasses) {
			getInHeritedScreenClass(screenClass, tvScreenClass);
		}
		
		return tvRoot;
	}
	
	private void getInHeritedScreenClass(ScreenClass screenClass, TVScreenClass tvsScreenClass) {
		TVScreenClass tvClass = new TVScreenClass(screenClass.getName());
		tvsScreenClass.addScreenClass(tvClass);
		if (requestableSource.equals(screenClass.getName())) {
			selectedTVObject = tvClass;
		}
		List<ScreenClass> screenClasses = screenClass.getInheritedScreenClasses();
		for (ScreenClass sc : screenClasses) {
			getInHeritedScreenClass(sc, tvClass);
		}
	}
	
	public Object getValue() {
			TVScreenClass tvScreenClass = (TVScreenClass) ((IStructuredSelection) screenClasses.getSelection())
					.getFirstElement();
			requestableSource = tvScreenClass.name;
			
		return requestableSource;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}
	
	public void addListener(ILabelProviderListener listener) {
	}

	public void removeListener(ILabelProviderListener listener) {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object selectedObject = selection.getFirstElement();

			parentDialog.enableOK((selectedObject instanceof TVScreenClass));
		}
	}
	
	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject) element).name;
		}
		return null;
	}

	public Object[] getChildren(Object inputElement) {
		if (inputElement instanceof TVScreenClass) {
		TVScreenClass tvScreenClass = (TVScreenClass) inputElement;
		List<Object> list = new ArrayList<Object>();
		list.addAll(tvScreenClass.getScreenClasses());
		return list.toArray();
	} else if (inputElement instanceof TVRoot) {
		TVRoot tvRoot = (TVRoot) inputElement;
		return tvRoot.getScreenClasses().toArray();
	}
	return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TVObject) {
			return ((TVObject) element).parent;
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

}
