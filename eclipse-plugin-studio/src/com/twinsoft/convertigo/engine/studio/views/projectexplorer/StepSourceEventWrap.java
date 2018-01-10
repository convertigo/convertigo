package com.twinsoft.convertigo.engine.studio.views.projectexplorer;

import java.util.EventObject;

import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;

public class StepSourceEventWrap extends EventObject {

    private static final long serialVersionUID = 447266135792228736L;

    private String xpath = ".";

    public StepSourceEventWrap(Object step) {
        super(step);
    }

    public StepSourceEventWrap(StepSource source) throws EngineException {
        super(source.getStep());
        this.xpath = source.getXpath();
    }

    public StepSourceEventWrap(Object step, String xpath) {
        super(step);
        this.xpath = xpath;
    }

    public String getXPath() {
        return xpath;
    }
}
