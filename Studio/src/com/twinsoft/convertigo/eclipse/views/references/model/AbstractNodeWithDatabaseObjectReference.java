/*
* Copyright (c) 2001-2016 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.references.model;

import org.eclipse.swt.graphics.Image;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public abstract class AbstractNodeWithDatabaseObjectReference extends AbstractParentNode {
	
	protected AbstractNodeWithDatabaseObjectReference(
			AbstractParentNode parent, String name,
			DatabaseObject refDatabaseObject) {
		super(parent, name);
		setRefDatabaseObject(refDatabaseObject);
	}

	private DatabaseObject refDatabaseObject;

	public DatabaseObject getRefDatabaseObject() {
		return refDatabaseObject;
	}

	public void setRefDatabaseObject(DatabaseObject refDatabaseObject) {
		this.refDatabaseObject = refDatabaseObject;
	}

	@Override
	public String getName() {
		/*
		 * treat speacial case, see ViewRefLabelProvider.java
		 * public Image getImage(Object element)
		 */
		if (refDatabaseObject == null) {
			if (name.contains("entry") || name.contains("exit"))
				return name;
		}

		return (refDatabaseObject == null ? name + " (broken reference)" : name);
	}
}
