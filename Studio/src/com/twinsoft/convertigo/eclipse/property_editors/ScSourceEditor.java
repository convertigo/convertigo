package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.widgets.Composite;

public class ScSourceEditor  extends AbstractDialogCellEditor {

    public ScSourceEditor(Composite parent) {
        super(parent);

        dialogTitle = "ScreenClass source";
        dialogCompositeClass = ScSourceEditorComposite.class;
    }
}
