package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class SelectSamplesPage extends WizardPage {

	private Tree categories;
	private Composite container;

	public SelectSamplesPage() {
		super("SelectSamplesPage");
		setTitle("Demos and samples");
		setDescription("Select the demos and samples you want to install. You will also be able to install them later.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		layout.marginWidth = 30;

		Label label = new Label(container, SWT.NONE);
		label.setText("Select samples :");

		GridData layoutData;
		layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.heightHint = 180;

		categories = new Tree(container, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		categories.setLayoutData(layoutData);

		TreeItem itemDocumentationSamples = new TreeItem(categories, SWT.CHECK);
		itemDocumentationSamples.setText("Documentation samples");
		TreeItem itemDemo = new TreeItem(categories, SWT.CHECK);
		itemDemo.setText("Mashup demo projects");
		TreeItem itemMobileSamples = new TreeItem(categories, SWT.CHECK);
		itemMobileSamples.setText("Mobile samples");
		TreeItem itemReferenceManualSamples = new TreeItem(categories, SWT.CHECK);
		itemReferenceManualSamples.setText("Reference Manual samples");
		TreeItem itemSqlConnectorSamples = new TreeItem(categories, SWT.CHECK);
		itemSqlConnectorSamples.setText("SQL Connector samples");

		categories.addSelectionListener(new SelectionListener() {				
			public void widgetSelected(SelectionEvent e) {
				TreeItem treeItem = (TreeItem) e.item;
				boolean categoryCheck = treeItem.getChecked();
				
				// Handle category selection
				if (treeItem.getParentItem() == null) {
					TreeItem[] children = treeItem.getItems();
					for (TreeItem childTreeItem : children) {
						childTreeItem.setChecked(categoryCheck);							
					}
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		getSamplesAndDemos(itemDocumentationSamples, itemDemo, itemMobileSamples, itemReferenceManualSamples,
				itemSqlConnectorSamples);

		// Required to avoid an error in the system
		setControl(container);
	}

	private void getSamplesAndDemos(TreeItem itemDocumentationSamples, TreeItem itemDemo,
			TreeItem itemMobileSamples, TreeItem itemReferenceManualSamples, TreeItem itemSqlConnectorSamples) {
		try {
	        Path path = new Path("plugin.xml");
	        InputStream pluginXml = FileLocator.openStream(ConvertigoPlugin.getDefault().getBundle(), path, false);

			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(pluginXml);

			document.getDocumentElement().normalize();
			NodeList list = document.getElementsByTagName("wizard");
			int nbNode = list.getLength();
			for (int i = 0; i < nbNode; i++) {
				Node item = list.item(i);
				Node attribut = item.getAttributes().getNamedItem("category");
				String category = attribut.getTextContent();
				if (category.endsWith("com.twinsoft.convertigo.eclipse.convertigodocumentationsamples")) {
					TreeItem treeItem = new TreeItem(itemDocumentationSamples, SWT.NONE);
					treeItem.setText(item.getAttributes().getNamedItem("name").getTextContent());
				} else if (category.endsWith("com.twinsoft.convertigo.eclipse.convertigorefmanualexamples")) {
					TreeItem treeItem = new TreeItem(itemReferenceManualSamples, SWT.NONE);
					treeItem.setText(item.getAttributes().getNamedItem("name").getTextContent());
				} else if (category.endsWith("com.twinsoft.convertigo.eclipse.convertigomobilesamples")) {
					TreeItem treeItem = new TreeItem(itemMobileSamples, SWT.NONE);
					treeItem.setText(item.getAttributes().getNamedItem("name").getTextContent());
				} else if (category.endsWith("com.twinsoft.convertigo.eclipse.convertigosqlsamples")) {
					TreeItem treeItem = new TreeItem(itemSqlConnectorSamples, SWT.NONE);
					treeItem.setText(item.getAttributes().getNamedItem("name").getTextContent());
				} else if (category.endsWith("com.twinsoft.convertigo.eclipse.convertigodemos")) {
					TreeItem treeItem = new TreeItem(itemDemo, SWT.NONE);
					treeItem.setText(item.getAttributes().getNamedItem("name").getTextContent());
				}
			}
		} catch (Exception e) {
			// TODO: handle error
		}
	}

	public Set<String> getSelectedProjects() {
		Set<String> selectedProjects = new HashSet<String>();
		
		TreeItem[] categoryItems = categories.getItems();
		
		for (TreeItem categoryItem : categoryItems) {
			TreeItem[] sampleItems = categoryItem.getItems();
			for (TreeItem sampleItem : sampleItems) {
				if (sampleItem.getChecked())
					selectedProjects.add(sampleItem.getText());				
			}
		}
		
		return selectedProjects;
	}

	@Override
	public IWizardPage getNextPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getNextPage();
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getPreviousPage();
	}

}
