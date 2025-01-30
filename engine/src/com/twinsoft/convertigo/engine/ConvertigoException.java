/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine;

/**
 * This class manages generic exception type for the Convertigo project.
 */
public class ConvertigoException extends Exception {

	private static final long serialVersionUID = -1744965690693358173L;

	private ConvertigoError error = null;
	
	public ConvertigoException() {
        super();
    }
    
    public ConvertigoException(String message) {
        this(message, null);
    }
    
    public ConvertigoException(String message, Throwable cause) {
        super(message);

        if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
            initCause(cause);
        }
    }
    
    public String getErrorMessage() {
    	return getMessage();
    }
    
    public String getErrorDetails() {
		String details = "";
		Throwable eCause = this;
		while ((eCause = eCause.getCause()) != null)  {
			if (eCause.getMessage() != null) {
				if (details.length() > 0)
					details += ", ";
				details += eCause.getMessage();
			}
		}
    	return details;
    }

    public ConvertigoError getError() {
    	if (error == null) {
    		error = ConvertigoError.initError(this);
    	}
    	return error;
    }
}