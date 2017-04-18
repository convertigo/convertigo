package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Sequence;

public interface WrapDatabaseObject extends WrapObject {

	default boolean instanceOf(Class<?> cls) {
		return cls.isInstance(getObject());
	};
	
	Object getObject();
	WrapDatabaseObject getParent();
	String getName();
	void hasBeenModified(boolean hasBeenModified);
	
	void closeAllEditors();
	void closeSequenceEditors(Sequence object);
	void closeConnectorEditors(Connector object);

}
