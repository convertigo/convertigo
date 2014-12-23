<%@ page
	contentType="text/html; charset=iso-8859-1"
	language="java"
	import="
		java.util.*,
		java.io.*,
		java.net.*,
		java.text.*,		
		java.lang.management.*,
		javax.xml.transform.Source,
		javax.xml.transform.URIResolver,
		javax.xml.transform.Transformer,
		javax.xml.transform.TransformerException,
		javax.xml.transform.TransformerFactory,
		javax.xml.transform.stream.StreamResult,
		javax.xml.transform.stream.StreamSource
	"
%>
<%!

String DIR_BUILD = "/devplatform/build";
String DIR_QUALIF_TOMCAT_WEBAPP = "/var/lib/tomcat6/webapps/convertigo";
String DIR_QUALIF_TOMCAT_WEBAPP_LIB = DIR_QUALIF_TOMCAT_WEBAPP + "/WEB-INF/lib";
String DIR_TOMCAT_WEBAPP_COSINUS_QUALIF = "/var/lib/tomcat6/webapps/cosinus/qualif";

public class FileComparatorByDateOfModification implements Comparator<File> {

	public int compare(File f1, File f2) {
		Long l1 = f1.lastModified();
		Long l2 = f2.lastModified();
		int cmp = l1.compareTo(l2);
		return -cmp;
	}

	public boolean equals(File obj) {
		return true;
	}	
	
}

public File[] getFiles_ExceptDirectoryListingXSL_SortedByDate(String directoryPath){
	File dir = new File(directoryPath);
	if (dir.exists()) {
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals("directory-listing.xsl");
			}
		});
		Arrays.sort(files, new FileComparatorByDateOfModification());
		return files;
	}
	return null;
}

public String getOutputDate(String product) {
	File[] files=getFiles_ExceptDirectoryListingXSL_SortedByDate(DIR_BUILD + "/" + product + "/dist");	
	if (files!=null && files.length != 0) {
		String outputDate = new Date(files[0].lastModified()).toString();
		return outputDate;
	}	
	return "n/a";
}

public String getLatestBuildVersion(String product) {
	File[] files=getFiles_ExceptDirectoryListingXSL_SortedByDate(DIR_BUILD + "/" + product + "/dist");	
	if (files!=null && files.length != 0) {
		String outputDate = files[0].getName();
		int i = outputDate.indexOf('-');
		outputDate = outputDate.substring(0, i);
		return outputDate;
	}	
	return "n/a";
}

public String getLatestBuildRevision(String product) {	
	File[] files=getFiles_ExceptDirectoryListingXSL_SortedByDate(DIR_BUILD + "/" + product + "/dist");	
	if (files!=null && files.length != 0) {
		String outputDate = files[0].getName();
		int i = outputDate.indexOf('-');
		outputDate = outputDate.substring(i+2);
		return outputDate;
	}	
	return "n/a";
}

public String getAvailableBuilds(String product) {
	File[] files = getFiles_ExceptDirectoryListingXSL_SortedByDate(DIR_BUILD + "/" + product + "/dist");	
	String result = "";
	if (files != null) {
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			String builtFrom = "?";
			File fileSvnOrigin = new File(DIR_BUILD + "/" + product + "/dist/" + name + "/.svn_origin");
			if (fileSvnOrigin.exists()) {
				try {
					BufferedReader br = new BufferedReader(new FileReader(fileSvnOrigin));
					builtFrom = br.readLine();
					br.close();
				}
				catch(Exception e) {
					builtFrom = "error svn origin: " + e.getMessage();
					e.printStackTrace();
				}
			}
			result += "<option value=\"" + name + "\">" + name + " (built from " + builtFrom +")</option>\n";
		}
	}
	return result;
}

public String getInstalledBuildFullVersion() {
	File dir = new File(DIR_QUALIF_TOMCAT_WEBAPP_LIB);
	if (dir.exists()) {
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.indexOf("engine") != -1);
			}
		});
		if (files.length != 0) {
			String outputDate = files[0].substring(18);
			int i = outputDate.lastIndexOf('.');
			outputDate = outputDate.substring(0, i);
			return outputDate;
		}
	}
	return "n/a";
}

public String getInstallDate(String product) {
	File dir = new File(DIR_QUALIF_TOMCAT_WEBAPP_LIB);
	if (dir.exists()) {
		String outputDate = new Date(dir.lastModified()).toString();
		return outputDate;
	}
	return "n/a";
}

public String getBuildTestsReportDate(String product) {
	File buildTestsReportFile = new File(DIR_TOMCAT_WEBAPP_COSINUS_QUALIF + "/" + product + ".html");
	if (buildTestsReportFile.exists()) {
		String buildTestsReportDate = new Date(buildTestsReportFile.lastModified()).toString();
		return buildTestsReportDate;
	}
	return "n/a";
}

public String getBuildTestsReportSuccessRate(String product) {
	try {
		File buildTestsReportFile = new File(DIR_TOMCAT_WEBAPP_COSINUS_QUALIF + "/" + product + ".html");
		if (buildTestsReportFile.exists()) {
			FileReader fileReader = new FileReader(buildTestsReportFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			StringBuffer sbReport = new StringBuffer("");
			while ((line = bufferedReader.readLine()) != null) {
				sbReport.append(line);
			}
	
			String sReport = sbReport.toString();
			int i = sReport.indexOf("<h2>Summary</h2>");
			if (i != -1) {
				i += 16;
				int j = sReport.indexOf("</table>", i);
				if (j != -1) {
					j += 8;
					String summary = sReport.substring(i, j);
					return summary;
				}
			}
		}
		return "<p>Summary unavailable</p>";
	} catch(Exception e) {
		return "<p>Exception: " + e.getMessage() + "</p>";
	}
}

public static String getSVNbranches() {
	BufferedReader svnBuf = null;
	String result = "";
	
	try {
		svnBuf = new BufferedReader(
				new InputStreamReader(
						Runtime.getRuntime()
								.exec("svn ls file:///devplatform/svn/twinsoft/CEMS/branches")
								.getInputStream()));

			
		String line = svnBuf.readLine();
		while (line != null) {
			result = "<option>branches/" + line.substring(0, line.length() - 1) + "</option>" + result;
			line = svnBuf.readLine();
		}
		result = "<option>trunk</option>" + result;
	} catch (IOException e) {			
		return result;
	}
	return result;
}

public String getRanorexTestsReportSuccessRate() {
		TransformerFactory tFactory = null;
		try {
			StringWriter sw = new StringWriter();

			tFactory = TransformerFactory.newInstance();

			tFactory.setAttribute("generate-translet", Boolean.FALSE);
			tFactory.setAttribute("auto-translet", Boolean.FALSE);

			URL urlStyleSheet = new URL("http://kalifus/convertigoAutoTest/RanorexReport.xsl");
			InputStream isStyleSheet = urlStyleSheet.openStream();
			Transformer transformer = tFactory.newTransformer(new StreamSource(isStyleSheet));

			URL urlXml = new URL("http://kalifus/convertigoAutoTest/convertigoTestLog.rxlog");
			InputStream isXml = urlXml.openStream();
			transformer.transform(new StreamSource(isXml), new StreamResult(sw));

			String result = sw.getBuffer().toString();
			
			int i = result.indexOf("<table border=\"1\" cellspacing=\"0\" width=\"100%\">");
			int j = result.indexOf("</table>", i);
			
			return result.substring(i, j + 8);
		} catch(Exception e) {
			String message = "<p>" + e.getMessage() + "</p>\n";
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			message += "<pre>" + stringWriter.toString() + "</pre>\n";
			return message;
		}
}

String outputDate = null;
String buildTestsReportDate = null;

%>

<html>
<head>
<title>:: Convertigo - Bienvenue sur Cosinus ::</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<style type="text/css">
	<!--
	@import url("cosinus.css");
	-->
</style>
</head>

<script language="JavaScript">

function build(product) {
	var o = document.getElementById(product + "BuildSvnBranch");
	var svnBranch = (o == null ? "n/a" : o.value);

	o = document.getElementById(product + "ForceBuild");
	var forceBuild = false;
	if (o != null) forceBuild = o.checked;

	o = document.getElementById(product + "BuildWin32StudioOnly");
	var buildWin32StudioOnly = false;
	if (o != null) buildWin32StudioOnly = o.checked;
	
	var brandLabel = "C8O";
	o = document.getElementById("cemsBuildBrandLabel");
	if (o != null) brandLabel = o.value;

	var target = "all";
	if (buildWin32StudioOnly) target = "studio-win32-only";

	if (product == "convertigo_libs") {
		svnBranch = "trunk";
		target = "build";
	}

	if (confirm("Do you really want to launch a new build for product '" + product + "'?\n" +
	"Branch: " + svnBranch + "\nForceBuild: " + forceBuild + "\nAnt target: " + target +
	"\nBuild Win32 studio only: " + buildWin32StudioOnly)) {
		window.open("exec.jsp?" +
			"CommandArgs=" + escape("/devplatform/build/build.sh")
			+ "&CommandArgs=" + escape(product)
			+ "&CommandArgs=" + escape("-DsvnBranch=" + svnBranch)
			+ "&CommandArgs=" + escape("-Dtarget=" + target)
			+ "&CommandArgs=" + escape("-DforceBuild=" + forceBuild)
			+ "&CommandArgs=" + escape("-DbrandLabel=" + brandLabel)
			+ "&CommandExecDir=" + escape("/devplatform/build")
			+ "&CommandName=" + escape("Product build")
			+ "&CommandComment=" + escape("SVN branch: " + svnBranch)
		);
	}
}

function buildTemplates() {
	var o = document.getElementById("cemsBuildSvnBranch");
	var svnBranch = (o == null ? "n/a" : o.value);

	if (confirm("Do you really want to launch a new build of C-EMS templates?\n" + "Branch: " + svnBranch)) {
		window.open("exec.jsp?" +
			"CommandArgs=" + escape("/devplatform/build/build.sh")
			+ "&CommandArgs=" + escape("cems")
			+ "&CommandArgs=" + escape("-DsvnBranch=" + svnBranch)
			+ "&CommandArgs=" + escape("-Dtarget=templates")
			+ "&CommandExecDir=" + escape("/devplatform/build")
			+ "&CommandName=" + escape("Templates build")
			+ "&CommandComment=" + escape("SVN branch: " + svnBranch)
		);
	}
}

function addToRefBin(product) {
	var svnLabel = document.getElementById(product + "RefBinSvnLabel").value;
	var svnComment = document.getElementById(product + "RefBinSvnComment").value;
	var svnType = document.getElementById(product + "RefBinSvnType").value;
	var svnSource = document.getElementById(product + "RefBinSvnSource").value;
	var build = document.getElementById(product + "RefBinBuild").value;
	var svnRevision = build.substring(build.indexOf("-v") + 2);

	if (confirm("Do you really want to add the current build of product '" + product + "' to RefBin?\n" +
	"Label: " + svnLabel + "\nComment: " + svnComment + "\nBuild: " + build + "\nType: " + svnType +
	"\nSource: " + svnSource + "\nRevision: " + svnRevision)) {
		window.open("exec.jsp?" +
			"CommandArgs=" + escape("/devplatform/build/refbin.sh")
			+ "&CommandArgs=" + escape(product)
			+ "&CommandArgs=" + escape(build)
			+ "&CommandArgs=" + escape(svnLabel)
			+ "&CommandArgs=" + escape( "\"" + svnComment + "\"")
			+ "&CommandArgs=" + escape(svnType)
			+ "&CommandArgs=" + escape(svnSource)
			+ "&CommandArgs=" + escape(svnRevision)
			+ "&CommandExecDir=" + escape("/devplatform/build")
			+ "&CommandName=" + escape("Refbin product")
			+ "&CommandComment=" + escape("Build: " + build)
		);
	}
}

function qualif(product) {
	var build = document.getElementById(product + "QualifBuild").value;
	if (confirm("Do you really want to launch a new qualif for product '" + product + "'?\n" +
	"Build: " + build)) {
		window.open("exec.jsp?" +
			"CommandArgs=" + escape("/devplatform/qualif/qualif.sh")
			+ "&CommandArgs=" + escape(product)
			+ "&CommandArgs=" + escape(build)
			+ "&CommandExecDir=" + escape("/devplatform/qualif")
			+ "&CommandName=" + escape("Product qualif")
			+ "&CommandComment=" + escape("Build: " + build)
		);
	}
}

</script>

<body>

<div id="global">
<!-- Header --> 
    <div class="header">
        <div class="logo">
        	<a href="index.html"><img src="images/logo-convertigo.png" alt="Convertigo - Bienvenue sur Cosinus" align="left" border="0"/></a>
        </div>
        <div id="menu">
            <ul class="menu">
				<li class="item01"><a href="#cems">C-EMS</a></li>
                <li class="item02"><a href="#twinsoft">Twinsoft</a></li>
                <li class="item03"><a href="#pobi">POBI</a></li>
            </ul>
        </div>
    </div>
<!-- Fin Header -->     
<!-- Contenu -->    
    <div class="content">
    	<div class="visuel">
        	<img src="images/header.png" alt="Convertigo - Welcome on Cosinus" align="left" border="0"/>
        </div>
        <div class="topcentre"></div>
        <div class="centre">
		<!-- Average system load --> 
            <div class="contentrub1">
                <h1>Average system load</h1>
                <hr/>               
                <div class="contentcentre1">
                    <div class="picto">
                    	<img src="images/picto-load.png" alt="Convertigo - Average system load" align="left" border="0"/>
                    </div>
                    <p class="info"><%
OperatingSystemMXBean opsys = ManagementFactory.getOperatingSystemMXBean();
%>
<%=(int) (opsys.getSystemLoadAverage() * 100) %></p>
                </div>
            </div>
		<!-- Memory usage -->            
            <div class="contentrub1">
                <h1>Memory usage</h1>
                <div class="spacerV"></div>
                <hr/>                
                <div class="contentcentre1">
                    <div class="picto">
                    	<img src="images/picto-memory.png" alt="Convertigo - Memory usage" align="left" border="0"/>
                    </div>
                    <p class="info"><%
com.sun.management.OperatingSystemMXBean opsysImpl = (com.sun.management.OperatingSystemMXBean) opsys;
double totalMem = opsysImpl.getTotalPhysicalMemorySize();
double usedMem = totalMem - opsysImpl.getFreePhysicalMemorySize();
%>
<%= (int) (usedMem / 1024 / 1024) %> Mb / <%= (int) (totalMem / 1024 / 1024)%> Mb<br/>
(<%=(int) (100 * usedMem / totalMem) %>%)</p>
                </div>
            </div>    
        <!-- Root partition usage --> 
            <div class="contentrub1">
                <h1>Dev Platform partition usage</h1>
                <hr/>                               
                <div class="contentcentre1">
                    <div class="picto">
                    	<img src="images/picto-root.png" alt="Convertigo - Dev Platform partition usage" align="left" border="0"/>
                    </div>
                    <p class="info"><%
File file = new File("/devplatform");
long rootPartitionSize = file.getTotalSpace() / 1024 / 1024;
long rootPartitionUsed= rootPartitionSize - file.getFreeSpace() / 1024 / 1024;
%>
<%= rootPartitionUsed%> Mb / <%= rootPartitionSize %> Mb<br/>
(<%=(int) (100 * rootPartitionUsed/ rootPartitionSize) %>%)
</p>
                </div>                
            </div>
        <!-- Convertigo Enterprise Mashup Server --> 
            <div class="generalcontent">
                <a class="anchor" name="cems">
                	<h2>Convertigo Enterprise Mashup Server</h2>
                </a>                                                       
                <hr/>
                <div class="cemsleft">
                    <!-- C-EMS BUILD -->
                    <div class="contentcentre3">
                        <div class="contentrub2">
                        <h3>Build</h3>
                            <ul>
                                <li><strong>Latest build date:</strong> <%=getOutputDate("cems")%></li>
                                <li><strong>Latest build version:</strong> <%= getLatestBuildVersion("cems")%></li>
                                <li><strong>Latest build revision:</strong> <%= getLatestBuildRevision("cems")%></li>
                            </ul>     
                            <a href="javascript: build('cems')">Launch a new build</a> based on following SVN branch:
                            <br/><br/>
                            <label for="cemsBuildSvnBranch">Branch</label>
                            <select class="champ" id="cemsBuildSvnBranch">                                
				  <%= getSVNbranches() %>
                            </select>
                            <br/><br/>
                            <label for="cemsBuildBrandLabel">Brand label</label>
                            <select class="champ" id="cemsBuildBrandLabel">
                                <option>C8O</option>
                                <option>CGI</option>
                            </select>
                            <br/><br/>
                            <label for="cemsForceBuild"><input type="checkbox" id="cemsForceBuild" value="true">Force a new build</label>
                            <br/>
                            <label for="cemsBuildWin32StudioOnly"><input type="checkbox" id="cemsBuildWin32StudioOnly" value="true">Build Win32 studio only</label>
                            <br/><br/>
<% if ((outputDate = getOutputDate("cems")) != null) { %>
                            <a href="build/cems" target="blank">See build outputs repository</a>
<% } %>
                            <br/><br/>

                            <a href="javascript: buildTemplates()">Launch a new build of templates
                            <br/>
                            <a href="build/cems_templates" target="blank">See templates build repository</a>
                            <br/>
                        </div>    
                    </div>
		    
                    <!-- C-EMS REFBIN -->
                    <div class="contentcentre3">
                        <div class="contentrub2">
                        <h3>RefBin</h3> 
                            Select a build:
                                <select id="cemsRefBinBuild" class="champ">
<%= getAvailableBuilds("cems") %>
                                </select>
                            <br/><br/>
                            RefBin label: 
                                <input type="text" id="cemsRefBinSvnLabel" value="<%= getLatestBuildVersion("cems")%>" class="champ">
                            <br/><br/>
                            Comment: 
                                <input type="text" id="cemsRefBinSvnComment" value="" size="50" class="champ">
                            <br/><br/>
                            Commit to SVN as a new
                                <select id="cemsRefBinSvnType" class="champ">
                                    <option value="tags">tag</option>
                                    <option value="branches">branch</option>
                                </select>
                            <br/><br/>
                            from source: 
                            <select class="champ" id="cemsRefBinSvnSource">
                                <%= getSVNbranches() %>
                            </select>
                            <br/><br/>
                            <a href="javascript: addToRefBin('cems')">Add the selected build to RefBin repository</a>
                            <br/>
                            <a href="refbin/cems" target="blank">See RefBin repository</a><br/>
                            <a href="http://sourceforge.net/projects/convertigo/files/" target="blank">Sourceforge Download Center</a>
                        </div>    
                    </div>
                </div>      
                <div class="cemsright">
                    <!-- C-EMS QUALIF -->
                    <div class="contentcentre3">
                        <div class="contentrub2">
                        <h3>Qualif</h3> 
                            <a href="javascript: qualif('cems')">Launch a new qualif</a> based on the following build:
                                <br/><br/>
                                <select id="cemsQualifBuild" class="champ">
<%= getAvailableBuilds("cems") %>
                                </select>
                                <br/><br/>
                                
                                <h4>Latest installation</h4>
                                <ul class="list">
                                    <li><strong>Version:</strong> <%= getInstalledBuildFullVersion()%></li>
                                    <li><strong>Date:</strong> <%=getInstallDate("cems")%></li>
                                </ul>                               
                                <a href="http://cosinus:18080/convertigo/" target="blank">Go to C-EMS</a>
                                <br/>
                                <a href="http://cosinus:18080/convertigo/admin" target="blank">Go to C-EMS administration console</a>
                                <br/><br/>
                                
<% if ((buildTestsReportDate = getBuildTestsReportDate("cems")) != null) { %>
                                <h4>Latest tests</h4>
                                <ul class="list">
                                    <li><strong>Date:</strong> <%=buildTestsReportDate%></li>
                                </ul>                                 
                                <a href="nightly.txt" target="blank">Display nightly shell script output</a>
                                <br/><br/>
                                <%=getBuildTestsReportSuccessRate("cems")%>
                                <a href="qualif/cems.html" target="blank">Display latest Engine tests report</a>
                                <br/><br/>
<% } %>
                                <!--%=getRanorexTestsReportSuccessRate()%>
                                <a href="http://kalifus/convertigoAutoTest/convertigoTestLog.rxlog" target="blank">Display latest GUI tests report</a-->
                        </div>    
                    </div>             
                </div>
            </div>     
                    
        <!-- Twinsoft / Convertigo libs -->    
            <div class="generalcontent">
                <a class="anchor" name="twinsoft">
                	<h2>Twinsoft / Convertigo libs</h2>
                </a>                                                     
                <hr/>
                <div class="contentcentre2">
                    <div class="logo-part">
                        <img src="images/logo-twinsoft.png" alt="Convertigo - Twinsoft" align="left" border="0"/>
                    </div> 
                    <a href="javascript: build('convertigo_libs')">Launch a new build</a><br/>  
<% if ((outputDate = getOutputDate("convertigo_libs")) != null) { %>
                    <a href="build/convertigo_libs" target="blank">Outputs</a> <%=outputDate%><br/>
<% } %>
                    <a href="refbin/convertigo_libs" target="blank">RefBin</a>         
                 </div> 
             </div>   
                        
        <!-- POBI -->
        	<div class="generalcontent"> 
                <a class="anchor" name="pobi">
                	<h2>POBI</h2>
                </a>                                                          
                <hr/>  
                <div class="contentcentre2">
                    <div class="logo-part">
                        <img src="images/logo-pobi.png" alt="Convertigo - POBI Banque de France" align="left" border="0"/>
                    </div>
                    <a href="javascript: build('pobi')">Launch a new build</a><br/>  
<% if ((outputDate = getOutputDate("pobi")) != null) { %>
                    <a href="build/pobi" target="blank">Outputs</a> <%=outputDate%><br/>  
<% } %>
                    <a href="refbin/pobi" target="blank"> RefBin</a>       
                 </div>  
             </div>   
                             
        </div>      
    </div>
<!-- Footer -->    
    <div class="footer">
    	<hr/>Copyright � 2009-2010 Convertigo. All rights reserved.
    </div>
<!-- Fin Footer -->    
<!-- Fin Contenu --> 
</div>

</body>
</html>
