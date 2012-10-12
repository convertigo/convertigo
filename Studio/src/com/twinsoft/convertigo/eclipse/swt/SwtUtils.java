package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.swt.layout.GridLayout;

public class SwtUtils {
	static public GridLayout newGridLayout(int numColumns, boolean makeColumnsEqualWidth, int horizontalSpacing, int verticalSpacing, int marginWidth, int marginHeight) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = numColumns;
		gridLayout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		gridLayout.horizontalSpacing = horizontalSpacing;
		gridLayout.verticalSpacing = verticalSpacing;
		gridLayout.marginWidth = marginWidth;;
		gridLayout.marginHeight = marginHeight;
		return gridLayout;
	}
	
	static public GridLayout newGridLayout(int numColumns, boolean makeColumnsEqualWidth, int horizontalSpacing, int verticalSpacing, int marginWidth, int marginHeight, int marginRight,
															int marginTop, int marginBottom, int marginLeft) {
		GridLayout gridLayout = newGridLayout(numColumns, makeColumnsEqualWidth, horizontalSpacing, verticalSpacing, marginWidth, marginHeight);
		gridLayout.marginBottom = marginBottom;
		gridLayout.marginLeft = marginLeft;
		gridLayout.marginRight = marginRight;
		gridLayout.marginTop = marginTop;
		return gridLayout;
	}
}
