package com.twinsoft.convertigo.engine;

import java.util.Collections;
import java.util.Set;


public class UndefinedSymbolsException extends Exception {
	private static final long serialVersionUID = -4202689033508498831L;
	
	private Set<String> undefinedSymbols;
	private Object incompletValue;
	
	UndefinedSymbolsException(Set<String> undefinedSymbols, Object incompletValue) {
		this.undefinedSymbols = Collections.unmodifiableSet(undefinedSymbols);
		this.incompletValue = incompletValue;
	}
	
	public Set<String> undefinedSymbols() {
		return undefinedSymbols;
	}
	
	public Object incompletValue() {
		return incompletValue;
	}
}
