package com.twinsoft.convertigo.engine.studio.property_editors;

import org.w3c.dom.Document;

public interface IStepSourceEditorWrap {

    public void selectItemsInTree(String[] items);
    public String[] findTreeItems(String xpath);
    public Document getDom();
}
