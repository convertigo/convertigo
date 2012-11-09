package com.twinsoft.convertigo.eclipse.popup.actions;

public class ButtonSpec {
	public String label;
	public int value;
	public boolean defaultButton;
	
	public ButtonSpec(String _label, int _value, boolean _defaultButton){
		label = _label;
		value = _value;
		defaultButton = _defaultButton;
		};
		
		public String getLabel() {
			return label;
		}
		
		public int getValue() {
			return value;
		}
		
		public boolean getDefault() {
			return defaultButton;
		}
	}
