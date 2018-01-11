package com.twinsoft.convertigo.engine.studio.views.projectexplorer;

import java.util.EventListener;

public interface StepSourceListenerWrap extends EventListener {
    
    public void sourceSelected(StepSourceEventWrap stepSourceEvent);
}
