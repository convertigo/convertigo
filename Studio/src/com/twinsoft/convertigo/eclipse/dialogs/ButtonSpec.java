package com.twinsoft.convertigo.eclipse.dialogs;

public class ButtonSpec {
	public String label;
	public int value;
	public boolean defaultButton;
	
	public ButtonSpec(String _label, boolean _defaultButton){
		label = _label;
		defaultButton = _defaultButton;
		};
		
		public String getLabel() {
			return label;
		}
		
		public boolean getDefault() {
			return defaultButton;
		}
	}
