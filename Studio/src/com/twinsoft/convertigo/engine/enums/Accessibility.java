package com.twinsoft.convertigo.engine.enums;

public enum Accessibility {
	Public(0),
	Hidden(1),
	Private(2);
	
	int code;
	
	Accessibility(int code) {
		this.code = code;
	}
	
	public int code() {
		return code;
	}
	
	public final static String[] accessibilities = new String[] {
		Public.name(),
		Hidden.name(),
		Private.name()
	};
	
	public static Accessibility valueOf(int code) {
		switch (code) {
		case 0 : return Public;
		case 1 : return Hidden;
		default : return Private;
		}
	}
}
