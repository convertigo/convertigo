package com.sap.conn.jco;

public class JCoException extends Exception {
	static final long serialVersionUID = 20050909001L;

	static String getKeyForGroup(int group) {
		return "";
	}

	public JCoException(int group, String key, String message) {
		this(group, key, message, null, '\000', null, null, null, null);
	}

	public JCoException(int group, String message) {
		this(group, getKeyForGroup(group), message, null, '\000', null, null,
				null, null);
	}

	public JCoException(int group, String key, String message, Throwable cause) {
		this(group, key, message, null, '\000', null, null, cause, null);
	}

	public JCoException(int group, String message, Throwable cause) {
		this(group, getKeyForGroup(group), message, null, '\000', null, null,
				cause, null);
	}

	public JCoException(int group, String key, String message,
			String messageClass, char messageType, String messageNumber,
			String[] messageParameters) {
		this(group, key, message, messageClass, messageType, messageNumber,
				messageParameters, null, null);
	}

	public JCoException(int group, String key, String message,
			String messageClass, char messageType, String messageNumber,
			String[] messageParameters, Throwable cause) {
		this(group, key, message, messageClass, messageType, messageNumber,
				messageParameters, cause, null);
	}

	public JCoException(int group, String message, String messageClass,
			char messageType, String messageNumber, String[] messageParameters,
			Throwable cause, String raisedBy) {
		this(group, getKeyForGroup(group), message, messageClass, messageType,
				messageNumber, messageParameters, cause, raisedBy);
	}

	public JCoException(int group, String key, String message,
			String messageClass, char messageType, String messageNumber,
			String[] messageParameters, Throwable cause, String raisedBy) {
		super(message, cause);
	}

	public final int getGroup() {
		return 0;
	}

	public final String getKey() {
		return "";
	}

	public String getMessageClass() {
		return "";
	}

	public String getMessageNumber() {
		return "";
	}

	public String getMessageText() {
		return null;
	}

	public String getMessageParameter(int index) {
		return null;
	}

	public String[] getMessageParameters() {
		return null;
	}

	public char getMessageType() {
		return ' ';
	}

	public String toString() {
		return "";
	}
}
