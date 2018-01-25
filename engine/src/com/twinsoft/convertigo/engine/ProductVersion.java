package com.twinsoft.convertigo.engine;


public class ProductVersion {

    public static String majorProductVersion = "7";
    public static String minorProductVersion = "6";
    public static String servicePack = "0";
    public static String tag = "beta";
    public static String revision = null;
    
    public static String productVersion =
    	ProductVersion.majorProductVersion + "." +
    	ProductVersion.minorProductVersion + "." +
    	ProductVersion.servicePack;
    
    public static String helpVersion = productVersion; /** release = productVersion */
    
    public static String fullProductVersionID =
    		ProductVersion.productVersion +
    		(ProductVersion.tag == null ? "" : "_" + ProductVersion.tag) +
    		(ProductVersion.revision == null ? "" : "-v" + ProductVersion.revision);
    
    public static String fullProductVersion =
        	ProductVersion.productVersion +
        	(ProductVersion.tag == null ? "" : "_" + ProductVersion.tag) +
        	(ProductVersion.revision == null ? "" : " (build " + ProductVersion.revision + ")");

    public static void main(String[] args) {
        System.out.println(ProductVersion.fullProductVersion);
    }
	
}
