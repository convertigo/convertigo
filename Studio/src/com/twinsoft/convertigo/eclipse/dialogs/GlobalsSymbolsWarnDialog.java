
package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

public class GlobalsSymbolsWarnDialog extends Dialog {
	
	private StyledText labelIntro, labelProperty, labelObjectName, labelObjectType, labelProject = null;
	private Text textFailure = null;
	private Image image = null;
	private Button buttonDismiss = null;	
	private boolean skipNextWarning = false;
	private String projectName, propertyName,
	propertyValue, failureMessage, objectName, objectType; 
 
	/**
	 * Create the dialog.
	 * @param parentShell, errorMessage
	 */
	public GlobalsSymbolsWarnDialog(Shell parentShell, String projectName, String propertyName,
			String propertyValue, String failureMessage, String objectName, String objectType) {
		super(parentShell);
		this.projectName = projectName;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.failureMessage = failureMessage;
		this.objectName = objectName;
		this.objectType = objectType;
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Undefined Global Symbols");
		newShell.setMinimumSize(400,310); 
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		StyleRange styleItalic = null;
		StyleRange styleBold = null;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginTop = 10;
		gridLayout.horizontalSpacing = 10;
		Color back = parent.getBackground();
		
		container.setLayout(gridLayout);
		
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.verticalSpan = 7;
		gridData.grabExcessHorizontalSpace = true;
		
		image = new Image(parent.getDisplay(), parent.getDisplay().getSystemImage(SWT.ICON_WARNING), 0);
		Label labelImage = new Label(container, SWT.NONE);
		labelImage.setImage(image);
		labelImage.setLayoutData(gridData);
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);

		labelIntro = new StyledText(container, SWT.NONE);
		labelIntro.setText("Compilation error for property '"+propertyName+"':");
		labelIntro.setEditable(false);
		labelIntro.setLayoutData(gridData);
		labelIntro.setBackground(back);
		
		if (propertyName!=null) {
			styleBold = new StyleRange();
			styleBold.start = 32;
			styleBold.length = propertyName.length();
			styleBold.fontStyle = SWT.BOLD;
			labelIntro.setStyleRange(styleBold);
		}
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		
		textFailure = new Text(container, SWT.MULTI);
		textFailure.setText(failureMessage);
		textFailure.setEditable(false);
		textFailure.setLayoutData(gridData);
		textFailure.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelProperty = new StyledText(container, SWT.NONE);
		labelProperty.setEditable(false);
		labelProperty.setText("\nProperty value: '"+propertyValue+"'");
		labelProperty.setLayoutData(gridData);
		labelProperty.setBackground(back);
		
		if (propertyValue!=null) {
			styleBold = new StyleRange();
			styleBold.start = 18;
			styleBold.length = propertyValue.length();
			styleBold.fontStyle = SWT.BOLD;
			labelProperty.setStyleRange(styleBold);
		}
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelObjectName = new StyledText(container, SWT.NONE);
		labelObjectName.setEditable(false);
		labelObjectName.setText("● Object name: '"+objectName+"'");
		labelObjectName.setLayoutData(gridData);
		labelObjectName.setBackground(back);
		
		if (objectName!=null) {
			styleBold = new StyleRange();
			styleBold.start = 16;
			styleBold.length = objectName.length();
			styleBold.fontStyle = SWT.BOLD;
			labelObjectName.setStyleRange(styleBold);
			
			styleItalic = new StyleRange();
			styleItalic.start = 2;
			styleItalic.length = 13;
			styleItalic.fontStyle = SWT.ITALIC;
			labelObjectName.setStyleRange(styleItalic);
		}
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelObjectType = new StyledText(container, SWT.NONE);
		labelObjectType.setEditable(false);
		labelObjectType.setText("● Object type: '"+objectType+"'");
		labelObjectType.setLayoutData(gridData);
		labelObjectType.setBackground(back);
		
		if (objectType!=null) {
			styleBold = new StyleRange();
			styleBold.start = 16;
			styleBold.length = objectType.length();
			styleBold.fontStyle = SWT.BOLD;
			labelObjectType.setStyleRange(styleBold);
			
			styleItalic = new StyleRange();
			styleItalic.start = 2;
			styleItalic.length = 13;
			styleItalic.fontStyle = SWT.ITALIC;
			labelObjectType.setStyleRange(styleItalic);
		}
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		
		labelProject = new StyledText(container, SWT.NONE);
		labelProject.setEditable(false);
		labelProject.setText("● Project: '"+projectName+"'\n");
		labelProject.setLayoutData(gridData);
		labelProject.setBackground(back);
		
		if (projectName!=null) {
			styleBold = new StyleRange();
			styleBold.start = 12;
			styleBold.length = projectName.length();
			styleBold.fontStyle = SWT.BOLD;
			labelProject.setStyleRange(styleBold);
			
			styleItalic = new StyleRange();
			styleItalic.start = 2;
			styleItalic.length = 9;
			styleItalic.fontStyle = SWT.ITALIC;
			labelProject.setStyleRange(styleItalic);
		}
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		
		Label labelInfo = new Label(container, SWT.NONE);
		labelInfo.setText("Note: You can also create all global symbols for one project \nby right-clicking on the Project and choose \"Declare global symbols\"");
		labelInfo.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE));
		labelInfo.setLayoutData(gridData);
		
		buttonDismiss = new Button(container, SWT.CHECK);
		buttonDismiss.setText("Skip next pop-ups");
		buttonDismiss.setLayoutData(gridData);	
		
		return container;
	}
	
	public boolean getSkipWarning(){
		return skipNextWarning;
	}
	
	@Override
	protected void okPressed() {
		skipNextWarning = buttonDismiss.getSelection();
		try {
			ProjectUtils.addUndefinedGlobalSymbol(propertyValue);
			ProjectUtils.refreshTheProject(projectName);
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
		Button button = null;
		
		String[] symbolsNames = DatabaseObject.extractSymbol(propertyValue);
		if (symbolsNames.length==1) {
			button = createButton(parent, IDialogConstants.OK_ID, "Create '"+symbolsNames[0]+"' symbol", true);
		}else{
			button = createButton(parent, IDialogConstants.OK_ID, "Create symbols", true);
		}
		
		button.setEnabled(true);
		
		Button buttonCancel = createButton(parent, IDialogConstants.CANCEL_ID, "Close", true);
		buttonCancel.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				skipNextWarning = buttonDismiss.getSelection();
				close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				skipNextWarning = buttonDismiss.getSelection();
				close();
			}
		});
	}
}