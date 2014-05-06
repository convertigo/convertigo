package com.twinsoft.convertigo.engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class UndefinedSymbolsException extends Exception {
	private static final long serialVersionUID = -4202689033508498831L;
	
	private Set<String> undefinedSymbols;
	
	UndefinedSymbolsException(Set<String> undefinedSymbols) {
		this.undefinedSymbols = Collections.unmodifiableSet(undefinedSymbols);
	}
	
	public void append(UndefinedSymbolsException undefinedSymbolsException) {
		undefinedSymbols = new HashSet<String>(undefinedSymbols);
		undefinedSymbols.addAll(undefinedSymbolsException.undefinedSymbols());
		undefinedSymbols = Collections.unmodifiableSet(undefinedSymbols);
	}
	
	public Set<String> undefinedSymbols() {
		return undefinedSymbols;
	}
}
