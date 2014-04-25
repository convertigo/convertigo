
package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

public class GlobalsSymbolsWarnDialog extends Dialog {
	
	private StyledText labelProperty, labelObjectType, labelProject = null;
	private StyledText textFailure = null;
	private Image image = null;
	private Button buttonDoThis = null;	
	private Button buttonOk, buttonIgnore = null;
	private boolean doThisForAllCurrentProjectSymbols = false;
	private boolean createAll = false;
	private boolean showCheckBox = true;
	private String projectName, propertyName,
	propertyValue, objectName, objectType; 
	private Display display;
 
	/**
	 * Create the dialog.
	 * @param parentShell, errorMessage
	 */
	public GlobalsSymbolsWarnDialog(Shell parentShell, String projectName, String propertyName,
			String propertyValue, String objectName, String objectType, boolean showCheckBox) {
		super(parentShell);
		this.projectName = projectName;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.objectName = objectName;
		this.objectType = objectType;
		this.showCheckBox = showCheckBox;
		this.setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Undefined Global Symbols");
		//newShell.setSize(470,270); 
		display = newShell.getDisplay();
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		StyleRange styleBold = null;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		gridLayout.marginTop = 10;
		gridLayout.horizontalSpacing = 2;
		Color back = parent.getBackground();
		
		container.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.verticalSpan = 6;
		
		StyledText emptySpace1 = new StyledText(container, SWT.WRAP);
		emptySpace1.setEditable(false);
		emptySpace1.setText("     ");
		emptySpace1.setLayoutData(gridData);
		emptySpace1.setBackground(back);
		
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.verticalSpan = 4;
		
		image = new Image(parent.getDisplay(), parent.getDisplay().getSystemImage(SWT.ICON_WARNING), 0);
		Label labelImage = new Label(container, SWT.NONE);
		labelImage.setImage(image);
		labelImage.setLayoutData(gridData);
		
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.verticalSpan = 4;
		
		StyledText emptySpace2 = new StyledText(container, SWT.WRAP);
		emptySpace2.setEditable(false);
		emptySpace2.setText(" ");
		emptySpace2.setLayoutData(gridData);
		emptySpace2.setBackground(back);
		
		//First message
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		
		List<String> symbolsNames = DatabaseObject.extractSymbol(propertyValue);
		textFailure = new StyledText(container, SWT.WRAP);
		if (symbolsNames.size() == 1) {
			textFailure.setText("Undefined Global Symbol: "+symbolsNames.get(0));
			textFailure.setFocus();
		}
		if (symbolsNames.size() > 1) {
			textFailure.setText(symbolsNames.size()+" Undefined Global Symbols");
			textFailure.setFocus();
		}
		
		textFailure.setEditable(false);
		FontData[] fD = textFailure.getFont().getFontData();
		fD[0].setHeight(10);
		textFailure.setFont( new Font(display, fD[0]));
		textFailure.setLayoutData(gridData);
		textFailure.setBackground(back);
		
		if (symbolsNames.size()==1) {
			styleBold = new StyleRange();
			styleBold.start = 25;
			styleBold.length = symbolsNames.get(0).length();
			styleBold.fontStyle = SWT.BOLD;
			textFailure.setStyleRange(styleBold);
		}
		if (symbolsNames.size()>1) {
			styleBold = new StyleRange();
			styleBold.start = 0;
			styleBold.length = (symbolsNames.size()+"").length();
			styleBold.fontStyle = SWT.BOLD;
			textFailure.setStyleRange(styleBold);
		}
		
		//Project
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelProject = new StyledText(container, SWT.WRAP);
		labelProject.setEditable(false);
		labelProject.setText("     ●  Project: "+projectName);
		labelProject.setLayoutData(gridData);
		labelProject.setBackground(back);
		
		if (projectName!=null) {
			styleBold = new StyleRange();
			styleBold.start = 17;
			styleBold.length = projectName.length();
			styleBold.fontStyle = SWT.BOLD;
			labelProject.setStyleRange(styleBold);
		}
		
		//Object type & value
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelObjectType = new StyledText(container, SWT.WRAP);
		labelObjectType.setEditable(false);
		labelObjectType.setText("     ●  "+objectType+": "+objectName);
		labelObjectType.setLayoutData(gridData);
		labelObjectType.setBackground(back);
		
		if (objectName!=null && objectType!=null) {
			styleBold = new StyleRange();
			styleBold.start = objectType.length()+10;
			styleBold.length = objectName.length();
			styleBold.fontStyle = SWT.BOLD;
			labelObjectType.setStyleRange(styleBold);
		}
		
		//Property name & value
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelProperty = new StyledText(container, SWT.WRAP);
		labelProperty.setEditable(false);
		labelProperty.setText("     ●  "+propertyName+": "+propertyValue);
		labelProperty.setLayoutData(gridData);
		labelProperty.setBackground(back);
		
		if (propertyValue!=null && propertyName!=null) {
			styleBold = new StyleRange();
			styleBold.start = propertyName.length()+10;
			styleBold.length = propertyValue.length();
			styleBold.fontStyle = SWT.BOLD;
			labelProperty.setStyleRange(styleBold);
		}
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		gridData.grabExcessHorizontalSpace = true;
		
		Label labelInfo = new Label(container, SWT.WRAP );
		labelInfo.setText("\nNote: You can also create all global symbols for one project by right-clicking on the Project and choose \"Create global symbols\"");
		labelInfo.setForeground(new Color(display, 0, 164, 200));
		labelInfo.setLayoutData(gridData);
		
		if (showCheckBox==true) {
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
	
			buttonDoThis = new Button(container, SWT.CHECK | SWT.WRAP);
			buttonDoThis.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					doThisForAllCurrentProjectSymbols = buttonDoThis.getSelection();
					buttonOk.setText(doThisForAllCurrentProjectSymbols==true ? "Create symbols" : "Create '"+DatabaseObject.extractSymbol(propertyValue).get(0)+"' symbol");
					buttonIgnore.setText(doThisForAllCurrentProjectSymbols==true ? "Ignore all" : "Ignore");
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					doThisForAllCurrentProjectSymbols = buttonDoThis.getSelection();
					buttonOk.setText(doThisForAllCurrentProjectSymbols==true ? "Create symbols" : "Create '"+DatabaseObject.extractSymbol(propertyValue).get(0)+"' symbol");
					buttonIgnore.setText(doThisForAllCurrentProjectSymbols==true ? "Ignore all" : "Ignore");
				}
			});
			buttonDoThis.setText("Do this for all current project symbols");
			buttonDoThis.setLayoutData(gridData);	
		}
		return container;
	}
	
	public boolean getCreateAction(){
		return createAll;
	}
	
	public boolean getCheckButtonSelection() {
		 return doThisForAllCurrentProjectSymbols;
	}
	
	@Override
	protected void okPressed() {
		try {
			createAll = true;
			ProjectUtils.addUndefinedGlobalSymbol(propertyValue);
			
			//Refresh the project 
//			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
//			TreeObject treeObject = projectExplorerView.findTreeObjectByUserObjectQName(projectName);
//			projectExplorerView.reloadProject(treeObject);
		} catch (Exception e) {
			Engine.logBeans.error("Error during saving the global symbols file!\n"+e.getMessage());
		}
		close();
	}
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
	
		List<String> symbolsNames = DatabaseObject.extractSymbol(propertyValue);
		if (symbolsNames.size()==1) {
			buttonOk = createButton(parent, IDialogConstants.OK_ID, "Create '"+symbolsNames.get(0)+"' symbol", true);
		}else{
			buttonOk = createButton(parent, IDialogConstants.OK_ID, "Create symbols", true);
		}
		
		buttonOk.setEnabled(true);
		
		buttonIgnore = createButton(parent, IDialogConstants.CLOSE_ID, "Ignore", true);
		buttonIgnore.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createAll = false;
				close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				createAll = false;
				close();
			}
		});
	}
}