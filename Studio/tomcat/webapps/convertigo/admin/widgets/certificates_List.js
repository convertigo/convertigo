/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

var NEW_MAPPING_MESSAGE_TEXT="(choose a project)";
var NEW_MAPPING_MESSAGE_VALUE="chooseProject";
var NEW_CERTIFICATE_MESSAGE_TEXT="(choose a certificate)";
var NEW_CERTIFICATE_MESSAGE_VALUE="chooseCertificate";
var GROUP_MESSAGE="Group not specified!";
var VIRTUAL_SERVER_MESSAGE="Virtual server not specified!";

function certificates_List_init(){
	
	$("#certificatesInstall").button({
		icons : {
			primary : "ui-icon-circle-plus"
		}
	});
	
	
	
	$("#certificatesRemove").button({
		icons : {
			primary : "ui-icon-circle-minus"
		}
	}).click(function(){
		$("#certificates_ListRemoveDialogDynamicPart").empty().append(
				$("#certificates_ListCertificatesToConfigure").clone().attr('id','certificates_ListRemoveDialogSelectCertificate')
		);
		$("#certificates_ListRemoveDialog").dialog('open');
	});
		
	$("#certificates_ListRemoveDialog").dialog({
		autoOpen : false,
		width : 495,		
		title : "Remove certificate",		
		modal : true,
		buttons : {
			Remove : function(){
				var certificateToDeleteName=$("#certificates_ListRemoveDialogSelectCertificate").val();
				if(certificateToDeleteName==NEW_CERTIFICATE_MESSAGE_VALUE){
					showInfo("Please choose a valid certificate");
				}else{
					showConfirm("Do you realy want to remove the certificate \""+certificateToDeleteName+"\" from the disk?",function(){					
						removeCertificate(certificateToDeleteName);
						showInfo("the certificate \""+certificateToDeleteName+"\" has been successfully removed");
						$("#certificates_ListRemoveDialog").dialog('close');
						return true;
					});
				}				
			},
			Cancel : function () {
				$(this).dialog('close');
				return false;
			}
		}
	});			

	new AjaxUpload("certificatesInstall", {
		action: "services/certificates.Install",
		responseType: "xml",
		onSubmit : function(file , ext){		
			startWait(50);
			
		},
		onComplete: function(file, response) {				
			clearInterval(this.tim_progress);			
			endWait();
			if($(response).find("error").length>0){
				showError($(response).text());				
			}else{
				showInfo("The certificate "+file+" has been successfully deployed.");				
			}
			certificates_List_update();
		}
	});	
	
	
	
	
	$("#certificatesList").jqGrid(
			{
				datatype : "local",
				colNames : [ 'Certificate / Store', 'Type', 'Password', 'Group', 'Delete', 'Update'],
				colModel : [ {
					name : 'certificateStore',
					index : 'certificateStore',					
					align : "left"
				}, {
					name : 'type',
					index : 'type',					
					align : "left"
				}, {
					name : 'password',
					index : 'password',
					align : "left"
				}, {
					name : 'group',
					index : 'group',
					align : "left"
				} 
				, {
					name : 'btnDelete',
					index : 'btnDelete',
					sortable : false,
					align : "center"
				} 
				, {
					name : 'btnValid',
					index : 'btnValid',
					sortable : false,
					align : "center"
				} ],
				autowidth : true,
				viewrecords : true,
				height : 'auto',
				sortable : true,
				pgbuttons : false,
				pginput : false,
				toppager : false,
				emptyrecords : 'No certificates configured',
				altRows : true,		
				rowNum: '1000000',
				caption : "Installed certificates"
				
			});		
	
	
	
	$("#certificatesAnonymousMappings").jqGrid(
			{
				datatype : "local",
				colNames : [ 'Project Name', 'Certificate / Store','Delete','Update'],
				colModel : [ {
					name : 'projectName',
					index : 'projectName',					
					align : "left"
				}, {
					name : 'certificateStore',
					index : 'certificateStore',					
					align : "left",
					formatter: formatterCertificateStore
				},{
					name : 'btnDelete',
					index : 'btnDelete',
					sortable : false,
					align : "center"
				} 
				, {
					name : 'btnValid',
					index : 'btnValid',
					sortable : false,
					align : "center"
				}],
				autowidth : true,
				viewrecords : true,
				height : 'auto',
				sortable : true,
				pgbuttons : false,
				pginput : false,
				toppager : false,
				emptyrecords : 'No mappings',
				altRows : true,			
				rowNum: '1000000',
				caption : "Mappings for anonymous users"
			});	
	
	$("#certificatesUserMappings").jqGrid(
			{
				datatype : "local",
				colNames : [ 'Project Name','Virtual server','Authorization Group','User','Certificate / Store','Delete','Update'],
				colModel : [ {
					name : 'projectName',
					index : 'projectName',					
					align : "left"
				}, {
					name : 'virtualServer',
					index : 'virtualServer',					
					align : "left"
				}, {
					name : 'authorizationGroup',
					index : 'authorizationGroup',
					align : "left"
				}, {
					name : 'user',
					index : 'user',
					align : "left"
				} 				
				,{
					name : 'certificateStore',
					index : 'certificateStore',
					align : "center",
					formatter: formatterCertificateStore
				}
				,{
					name : 'btnDelete',
					index : 'btnDelete',
					sortable : false,
					align : "center"
				} 
				, {
					name : 'btnValid',
					index : 'btnValid',
					sortable : false,
					align : "center"
				}
				],
				autowidth : true,
				viewrecords : true,
				height : 'auto',
				sortable : true,
				pgbuttons : false,
				pginput : false,
				toppager : false,
				emptyrecords : 'No new candidates',
				altRows : true,		
				rowNum: '1000000',
				caption : "Mappings for carioca users"
			});	
		
	certificates_List_update();
	
	
}

var certificatesSelectObject;
var projectsSelectObject;

function certificates_List_update(){
	
	clearTables();	
	var certificateName;
	
	callService("certificates.List", function(xml){		
		
		
		var error;
		//certificates
		$(xml).find("certificate").each(function() {
			
			certificateName = $(this).attr("name");
			error="";
			if($(this).attr("validPass")!='true'){
				error='<br/><img src="images/error_16x16.gif"> invalid password!';
			}
			$("#certificatesList").jqGrid("addRowData",
				certificateName, 				
				{ certificateStore: certificateName, 
				type: createTypeSelect($(this).attr("type")),
				password: "<input type='password' value='"+$(this).attr("password")+"' />"+error,
				group:"<input type='text' value='"+$(this).attr("group")+"' />",
				btnDelete: "<a href=\"javascript: deleteCertificate('" + certificateName + "')\"><img border=\"0\" title=\"Delete\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
				btnValid: "<a href=\"javascript: updateCertificate('" + certificateName +"')\"><img border=\"0\" title=\"Update\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
				}
			);
		});
		
		
		//candidates
		var candidateSelectObject = "<select id='certificates_ListCertificatesToConfigure'><option value='"+NEW_CERTIFICATE_MESSAGE_VALUE+"'>"+NEW_CERTIFICATE_MESSAGE_TEXT+"</option>";
		$(xml).find("candidate").each(function(){
			candidateSelectObject += "<option value='"+$(this).attr("name")+"'>"+$(this).attr("name")+"</option>";				
		});
		candidateSelectObject += "</select>";
				
		$("#certificatesList").jqGrid("addRowData",
				"new",{				
					certificateStore: candidateSelectObject, 
					type: createTypeSelect(""),
					password: "<input type='password'/>",
					group: "<input type='text'/>",							
					btnValid: "<a href=\"javascript: updateCertificate()\"><img border=\"0\" title=\"Update\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
					} 	
		);			
		
		
		
		
		callService("projects.List", function(xml2){
			projectsSelectObject = "<select><option value='"+NEW_MAPPING_MESSAGE_VALUE+"'>"+NEW_MAPPING_MESSAGE_TEXT+"</option>";
			$(xml2).find("project").each(function(){
				projectsSelectObject += "<option value='"+$(this).attr("name")+"'>"+$(this).attr("name")+"</option>";				
			});
			projectsSelectObject += "</select>";
			
			certificatesSelectObject = "<select>";		
			$(xml).find("certificate").each(function() {
				certificatesSelectObject += "<option value='"+$(this).attr("name")+"'>"+$(this).attr("name")+"</option>";				
			});		
			certificatesSelectObject += "</select>";		
						
			var projectName,deleteParams;	
			
			//anonymous mappings
			$(xml).find("anonymous > binding").each(function(){			
				certificateName = $(this).attr("certificateName");
				projectName= $(this).attr("projectName");				
				
				deleteParams="('" + projectName + "','"+certificateName+"');";
				
				$("#certificatesAnonymousMappings").jqGrid("addRowData",
					certificateName+"/"+projectName,{	
					projectName:projectName,
					certificateStore: certificateName,
					btnDelete: "<a href=\"javascript: deleteMapping"+deleteParams+" \"><img border=\"0\" title=\"Delete mapping\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
					btnValid: "<a href=\"javascript: deleteMappingWithoutUpdate"+deleteParams+" configureMapping('certificatesAnonymousMappings','"+projectName+"','"+certificateName+"')\"><img border=\"0\" title=\"Update mapping\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
					} 	
				);
				
			});
			
			var virtualServeur,imputationGroup,userName;
			
			//carioca mappings					
			$(xml).find("carioca > binding").each(function(){			
				certificateName = $(this).attr("certificateName");
				projectName= $(this).attr("projectName");	
				virtualServeur=$(this).attr("virtualServerName");
				imputationGroup=$(this).attr("imputationGroup");
				userName=$(this).attr("userName");
				
				deleteParams="('" + projectName + "','"+certificateName+"','tas','"+virtualServeur+"','"+imputationGroup+"','"+userName+"');";
				
				$("#certificatesUserMappings").jqGrid("addRowData",
					certificateName+"/"+projectName,{	
					projectName:projectName,
					certificateStore: certificateName,
					virtualServer:"<input type='text' value='"+virtualServeur+"' />",
					authorizationGroup:"<input type='text' value='"+imputationGroup+"'/>",
					user:"<input value='"+userName+"' />",
					btnDelete: "<a href=\"javascript: deleteMapping"+deleteParams+" \"><img border=\"0\" title=\"Delete mapping\" src=\"images/convertigo-administration-picto-delete.png\"></a>",
					btnValid: "<a href=\"javascript:  deleteMappingWithoutUpdate"+deleteParams+" configureMapping('certificatesUserMappings','"+projectName+"','"+certificateName+"')\"><img border=\"0\" title=\"Update mapping\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
					} 	
				);
				
				
				
			});
			
			var selectProjectName = projectsSelectObject;
			var selectCertificateName = certificatesSelectObject;
			
			//new entries for mappings

			$("#certificatesAnonymousMappings").jqGrid("addRowData",
					"new",{ 
				projectName: selectProjectName,
				certificateStore:selectCertificateName,				
				btnValid: "<a href=\"javascript:configureMapping('certificatesAnonymousMappings','"+NEW_MAPPING_MESSAGE_VALUE+"')\"><img border=\"0\" title=\"Update mapping\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
				} 	
			);
			
			$("#certificatesUserMappings").jqGrid("addRowData",
					"new",{ 
				projectName: selectProjectName,
				certificateStore:selectCertificateName,
				virtualServer:"<input type='text'/>",
				authorizationGroup:"<input type='text'/>",
				user:"<input type='text'/>",				
				btnValid: "<a href=\"javascript: configureMapping('certificatesUserMappings','"+NEW_MAPPING_MESSAGE_VALUE+"')\"><img border=\"0\" title=\"Update mapping\" src=\"images/convertigo-administration-picto-validate.png\"></a>"
				} 	
			);
			
			// Update			
			
			$("#certificatesUserMappings").find("tr").change(function(){
				UpdateAuthAndUser($(this));
			});
			
			$("#certificatesUserMappings").find("tr").each(function(){
				UpdateAuthAndUser($(this));
			});		
						
			
			
		});			
	});	
}

function formatterCertificateStore(cellvalue, options, rowObject) {
	return certificatesSelectObject.replace("value='" + cellvalue + "'", "value='" + cellvalue + "' selected=\"selected\"");
}

function UpdateAuthAndUser($line){
	
		 var virtualServeur=$line.find("td[aria-describedby='certificatesUserMappings_virtualServer'] > input").val();
		 var imputationGroup=$line.find("td[aria-describedby='certificatesUserMappings_authorizationGroup'] > input").val();				
		 var $cell;
		 var val;
		 
		 if(virtualServeur!=undefined){					 
			 
			 if(imputationGroup.length<=0 || imputationGroup==VIRTUAL_SERVER_MESSAGE){
				 $line.find("td[aria-describedby='certificatesUserMappings_user'] > input").attr("disabled","true").val(GROUP_MESSAGE);
			 }
			 else{
				 $cell=$line.find("td[aria-describedby='certificatesUserMappings_user'] > input");
				 $cell.removeAttr("disabled");
				 val= $cell.val();
				 if(val==GROUP_MESSAGE)
					 $cell.val("");
			 }
			 if(virtualServeur.length<=0){
				 $line.find("td[aria-describedby='certificatesUserMappings_authorizationGroup'] > input").attr("disabled","true").val(VIRTUAL_SERVER_MESSAGE);
			 }else{				
				 
				 $cell=$line.find("td[aria-describedby='certificatesUserMappings_authorizationGroup'] > input");
				 $cell.removeAttr("disabled");
				 val= $cell.val();
				 if(val==VIRTUAL_SERVER_MESSAGE)
					 $cell.val("");
			 }
		 }			
		
}


function createTypeSelect(typeSelect){
	var test=0;
	var rep="<select><option value='client' ";
	if(typeSelect=="client"){
		rep+="selected='true'";
		test=1;
	}
	rep+=">Client</option>";
	rep+="<option value='server' ";
	if(test==0){
		rep+="selected='true'";
	}
	rep+=">Server</option></select>";
	return rep;
}

function clearTables(){
	$("#certificatesList").jqGrid('clearGridData');
	$("#certificatesCandidates").jqGrid('clearGridData');
	$("#certificatesAnonymousMappings").jqGrid('clearGridData');
	$("#certificatesUserMappings").jqGrid('clearGridData');
}

function deleteCertificate(certificateName){	
	showConfirm("Are you sure you want to delete the certificate "+certificateName+" and all its associated mappings?",function(){
		callService("certificates.Delete", function(){
			certificates_List_update();
			showInfo("The certificate "+certificateName+" has been successfully deleted.");
			},
			{"certificateName_1":certificateName});
	});
	
}

function updateCertificate(certificate){
	
	var certificateName;
	var lineId;
	if(certificate){
		lineId=certificateName=certificate;
	}
	else{
		lineId="new";
		certificateName=$("tr[id='"+lineId+"'] > td[aria-describedby='certificatesList_certificateStore'] > select").val();
	}
	
	if(certificateName==NEW_CERTIFICATE_MESSAGE_VALUE){
		showError("Please choose a valid certificate.");
	}else{
		var password=$("tr[id='"+lineId+"'] > td[aria-describedby='certificatesList_password'] > input").val();
		var type=$("tr[id='"+lineId+"'] > td[aria-describedby='certificatesList_type'] > select").val();
		var group=$("tr[id='"+lineId+"'] > td[aria-describedby='certificatesList_group'] >input ").val();		
		callService("certificates.Configure", function(){
			showInfo("The certificate "+certificateName+" has been successfully updated.");
			certificates_List_update();}
		,{
			"name_0":certificateName,
			"pwd_0":password,
			"type_0":type,
			"group_0":group	
		});
	}
}

function removeCertificate(certificateName){	
	callService("certificates.Remove", function(xml){
		certificates_List_update();		
		$(xml).find("error").each(function(){
				showError($(this).text());
			});
		},
		{"certificateName":certificateName});
}

function deleteMapping(project,certificateName,tas,server,authgroup,user){
	showConfirm("Are you sure you want to delete the mapping "+project+"/"+certificateName+" ?",function(){
		var params=createDeleteMappingParams(project,certificateName,tas,server,authgroup,user);	
		callService("certificates.mappings.Delete", function(){				
			certificates_List_update();
			showInfo("The mapping has been successfully deleted.");
			},
			params);
	});
}

function deleteMappingWithoutUpdate(project,certificateName,tas,server,authgroup,user){
	var params=createDeleteMappingParams(project,certificateName,tas,server,authgroup,user);		
	callService("certificates.mappings.Delete", function(){},params);
}

function createDeleteMappingParams(project,certificateName,tas,server,authgroup,user){
	var type=$("tr[id='"+certificateName+"'] > td[aria-describedby='certificatesList_type'] >select").val();	
	var params="projects."+project+"."+type+".store";	
	var rad="";
	if(tas!=undefined){
		rad+="tas.";
		if(server!=undefined && server.length>0){
			rad+=server+".";
			if(authgroup!=undefined && authgroup.length>0){
				rad+=authgroup+".";
				if(user!=undefined && user.length>0){
					rad+=user+".";					
				}
			}
		}
	}
	params=rad+params;
	var paramsPlainJavascriptObject={"link_1":params}
	return paramsPlainJavascriptObject;
}

function configureMapping(table,project,certificate){	
	
	var noPoint=true;
	$("table[id='certificatesUserMappings'] td >input").each(function(){
		if($(this).val().indexOf(".")!=-1){
			noPoint=false;
		}
	})
	
	if(!noPoint){
		showError("The character '.' is forbidden into Virual server, authorization Group and User ");
	}else{
		var projectName;
		var certificate_project;
		if(project==NEW_MAPPING_MESSAGE_VALUE){
			projectName=$("tr[id='new'] > td[aria-describedby='"+table+"_projectName'] >select").val();	
			certificate_project="new";
		}else{
			projectName=project;
			certificate_project=certificate+"/"+project;
		}
		
		if(projectName==NEW_MAPPING_MESSAGE_VALUE){
			showError("Please choose a valid project.");
		}else{	
			
			
			var certificateName=$("tr[id='"+certificate_project+"'] > td[aria-describedby='"+table+"_certificateStore'] >select").val();	
			
			var kind;
			if(table!="certificatesAnonymousMappings"){
				kind="tas";
			}
			else{
				 kind="projects";			 
			}			
			var params={
					"targettedObject_0":kind,
					"cert_0":certificateName
				};
			if(kind=="tas"){
				//carioca
				var virtualServer=$("tr[id='"+certificate_project+"'] > td[aria-describedby='"+table+"_virtualServer'] >input").val();
				var group=$("tr[id='"+certificate_project+"'] > td[aria-describedby='"+table+"_authorizationGroup'] >input").val();
				var user=$("tr[id='"+certificate_project+"'] > td[aria-describedby='"+table+"_user'] >input").val();
				if(virtualServer.length>0){
					params["virtualServer_0"]=virtualServer;
					if(group.length>0){
						params["group_0"]=group;
						if(user.length>0){
							params["user_0"]=user;
						}	
					}	
				}
				params["project_0"]=projectName;
			}else{
				//anonymous
				params["convProject_0"]=projectName;
			}		
				
			callService("certificates.mappings.Configure", function(){			
				showInfo("The mapping "+projectName+"/"+certificateName+" has been successfully updated.");
				certificates_List_update();
				},
				params);
		}
	}
	
}

