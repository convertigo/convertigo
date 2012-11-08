package com.twinsoft.convertigo.eclipse.views.loggers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

public class EngineLogViewComparator extends ViewerComparator {
	private int propertyIndex;
	private static final int DESCENDING = 1;
	private int direction = DESCENDING;

	public EngineLogViewComparator() {
		this.propertyIndex = 3;
		direction = 1 - DESCENDING;
	}

	public int getDirection() {
		return direction == 1 ? SWT.DOWN : SWT.UP;
	}

	public void setColumn(int column) {
		if (column == this.propertyIndex) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do an ascending sort
			this.propertyIndex = column;
			direction = DESCENDING;
		}
	}

	@Override
	public int compare(Viewer viewer, Object element1, Object element2) {
		LogLine line1 = (LogLine) element1;
		LogLine line2 = (LogLine) element2;
		int rc = 0;
		switch (propertyIndex) {
			case 0:
				rc = line1.getMessage().compareTo(line2.getMessage());
				break;
			case 1:
				rc = line1.getLevel().compareTo(line2.getLevel());
				break;
			case 2:
				rc = line1.getCategory().compareTo(line2.getCategory());
				break;
			case 3:
				rc = line1.getTime().compareTo(line2.getTime());
				break;
			case 4:
				rc = line1.getThread().compareTo(line2.getThread());
				break;
			case 5:
				//rc = line1.getExtra().compareTo(line2.getExtra());
				rc = line1.getClientIP().compareTo(line2.getClientIP());
				break;
			case 6:
				rc = line1.getConnector().compareTo(line2.getConnector());
				break;
			case 7:
				rc = line1.getContextID().compareTo(line2.getContextID());
				break;
			case 8:
				rc = line1.getProject().compareTo(line2.getProject());
				break;
			case 9:
				rc = line1.getTransaction().compareTo(line2.getTransaction());
				break;
			case 10:
				rc = line1.getUID().compareTo(line2.getUID());
				break;
			case 11:
				rc = line1.getUser().compareTo(line2.getUser());
				break;
			case 12:
				rc = line1.getSequence().compareTo(line2.getSequence());
				break;
			case 13:
				rc = line1.getClientHostName().compareTo(line2.getClientHostName());
				break;
			default:
				rc = 0;
		}

		if (direction == DESCENDING) {
			rc = -rc;
		}
		return rc;
	}
}
