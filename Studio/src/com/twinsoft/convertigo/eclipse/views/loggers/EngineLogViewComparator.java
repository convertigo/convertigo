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
				rc = line1.getExtra().compareTo(line2.getExtra());
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
