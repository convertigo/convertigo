package com.sap.conn.jco;

public abstract interface JCoCustomDestination extends JCoDestination {
	public abstract UserData getUserLogonData();

	public static abstract interface UserData {
		public abstract void setClient(String paramString);

		public abstract void setUser(String paramString);

		public abstract void setAliasUser(String paramString);

		public abstract void setPassword(String paramString);

		public abstract void setLanguage(String paramString);

		public abstract void setLogonCheck(int paramInt);

		public abstract void setX509Certificate(String paramString);

		public abstract void setSSOTicket(String paramString);

		public abstract void setExternalIDData(String paramString);

		public abstract void setExternalIDType(String paramString);

		public abstract void requestSSOTicket(boolean paramBoolean);
	}
}
