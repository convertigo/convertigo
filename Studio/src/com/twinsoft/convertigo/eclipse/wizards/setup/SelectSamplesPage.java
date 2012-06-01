package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SelectSamplesPage extends WizardPage {

	private List<String> projects = new ArrayList<String>();
	private Tree projectSamples;
	private Composite container;
	
	public SelectSamplesPage () {
		super("Select samples");
		setTitle("Demos and Samples");
		setDescription("Select demos and samples to be installed.");
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

		projectSamples = new Tree(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		final File directory = new File("C:/Users/rahmanf/workspace/CemsStudio/tomcat/webapps/convertigo/templates/project");
		
		final TreeItem itemDocumentationSamples = new TreeItem(projectSamples, SWT.CHECK);
		itemDocumentationSamples.setText("Documentation Samples");
		final TreeItem itemDemo = new TreeItem(projectSamples, SWT.CHECK);
		itemDemo.setText("Mashup demo projects");
		final TreeItem itemMobileSamples = new TreeItem(projectSamples, SWT.CHECK);
		itemMobileSamples.setText("Mobile samples");
		final TreeItem itemReferenceManualSamples = new TreeItem(projectSamples, SWT.CHECK);
		itemReferenceManualSamples.setText("Reference Manual samples");
		final TreeItem itemSqlConnectorSamples = new TreeItem(projectSamples, SWT.CHECK);
		itemSqlConnectorSamples.setText("SQL Connector samples");
		
		projectSamples.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
    		  for (File file : directory.listFiles()) {
    				String name = file.getName();
    				if (name.startsWith("sample_documentation_")) {
    					 if (itemDocumentationSamples.getChecked()) {
    						 projects.add(name);
    					 }
    				} else if (name.startsWith("demo_")) {
    					if (itemDemo.getChecked()) {
    						projects.add(name);
    					}
    				} else if (name.startsWith("sample_refManual_")) {
    					if (itemReferenceManualSamples.getChecked()) {
    						projects.add(name);
    					}
    				} else if (name.startsWith("sampleMobile")) {
    					if (itemMobileSamples.getChecked()) {
    						projects.add(name);
    					}
    				} else if (name.startsWith("sample_database_SQL_")) {
    					if (itemSqlConnectorSamples.getChecked()) {
    						projects.add(name);
    					}
    				} else if ("lib_GoogleMaps.car".equals(name)) {
    					if (itemDemo.getChecked()) {
    						projects.add(name);
    					}
    				}
    			}
	      }
	    });
	    
		getSamplesAndDemos(itemDocumentationSamples, itemDemo, itemMobileSamples, itemReferenceManualSamples, itemSqlConnectorSamples);
		projectSamples.setLayoutData(layoutData);
		
		// Required to avoid an error in the system
		setControl(container);
//		setPageComplete(true);		
	}
	
	private void getSamplesAndDemos(TreeItem itemDocumentationSamples, TreeItem itemDemo, TreeItem itemMobileSamples, TreeItem itemReferenceManualSamples, TreeItem itemSqlConnectorSamples) {
		try {
		
			DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
			
			// création d'un constructeur de documents
			DocumentBuilder constructeur = fabrique.newDocumentBuilder();
			
         		// lecture du contenu d'un fichier XML avec DOM
			File xml = new File("C:/Users/rahmanf/workspace/CemsStudio/plugin.xml");
			Document document = constructeur.parse(xml);

			document.getDocumentElement().normalize();
			NodeList list = document.getElementsByTagName("wizard");
			int nbNode = list.getLength();
			for (int i=0 ; i < nbNode ; i++) {
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
		}catch(ParserConfigurationException pce){
			System.out.println("Erreur de configuration du parseur DOM");
			System.out.println("lors de l'appel à fabrique.newDocumentBuilder();");
		}catch(SAXException se){
			System.out.println("Erreur lors du parsing du document");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}catch(IOException ioe){
			System.out.println("Erreur d'entrée/sortie");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}
	}
	
	public String getSamples() {
		String result = "";
		for (String sample : projects) {
			result = result + sample;
		}
		return result;
	}
	
}
