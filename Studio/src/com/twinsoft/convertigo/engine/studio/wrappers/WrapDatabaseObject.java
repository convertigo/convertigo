package com.twinsoft.convertigo.engine.studio.wrappers;

public interface WrapDatabaseObject extends WrapObject {

	default boolean instanceOf(Class<?> cls) {
		return cls.isInstance(getObject());
	};

	Object getObject();
	WrapDatabaseObject getParent();
	String getName();
	void hasBeenModified(boolean hasBeenModified);
}
