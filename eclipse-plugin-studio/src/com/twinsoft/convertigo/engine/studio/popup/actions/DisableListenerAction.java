package com.twinsoft.convertigo.engine.studio.popup.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.SetPropertyResponse;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapObject;

public class DisableListenerAction extends AbstractRunnableAction {

	public DisableListenerAction(WrapStudio studio) {
		super(studio);
	}

	@Override
	protected void run2() throws Exception {
        try {
			WrapObject[] treeObjects = studio.getSelectedObjects().toArray(new WrapObject[0]);

			for (int i = treeObjects.length - 1; i >= 0; --i) {
				WrapDatabaseObject treeObject = (WrapDatabaseObject) treeObjects[i];
				if (treeObject.instanceOf(Listener.class)) {
					//StepView stepTreeObject = (StepView) treeObject;

					Listener listener = (Listener) treeObject.getObject();
					listener.setEnabled(false);

					//stepTreeObject.setEnabled(true);
					//stepTreeObject.hasBeenModified(true);

//		                TreeObjectEvent treeObjectEvent = new TreeObjectEvent(stepTreeObject, "isEnable", false, true);
//		                explorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
				}
			}
//				explorerView.refreshSelectedTreeObjects();
        }
        catch (Throwable e) {
            throw e;
        	//ConvertigoPlugin.logException(e, "Unable to enable step!");
        }
//        finally {
//			shell.setCursor(null);
//			waitCursor.dispose();
	}

	@Override
	public Element toXml(Document document, String qname) throws ConvertigoException, Exception {
		Element response = super.toXml(document, qname);
		if (response != null) {
			return response;
		}

		return new SetPropertyResponse("isEnabled").toXml(document, qname);
	}
}
