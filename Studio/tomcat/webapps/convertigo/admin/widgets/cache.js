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

function cache_update(){
	callService("cache.ShowProperties", function(xml){
		
		$(xml).find("cacheType").each(function() {			
			$("input[name=cacheType]").removeAttr("checked");
			
			var  n=$(this).parent().find("*").length;					
			if(n>1){//database			
				$("#cacheTypeRadioDatabase").attr("checked","checked");
				$("#cacheConfigurationDiv").show();		
			}
			else{//file			
				$("#cacheTypeRadioFile").attr("checked","checked");
				$("#cacheConfigurationDiv").hide();
			
			}
		});	
		$(xml).find("databaseType").each(function() {
			$("#databaseDriverSelect").val($(this).text());
		});	
		$(xml).find("serverName").each(function() {
			$("#cacheServerName").val($(this).text());
		});		
		$(xml).find("port").each(function() {
			$("#cachePort").val($(this).text());
		});	
		$(xml).find("databaseName").each(function() {
			$("#cacheDatabaseName").val($(this).text());
		});	
		$(xml).find("userName").each(function() {
			$("#cacheUserName").val($(this).text());
		});			
		$(xml).find("userPassword").each(function() {
			var txt=$(this).text();			
			$(".cacheUserPassword").each(function() {				
				$(this).val(txt);
			});
		});	
	});	
}

function cache_init(){
	$("#cacheConfigurationDiv").hide();
	$("#cacheTypeRadioFile").click(function(){
		$("#cacheConfigurationDiv").hide();
	});
	$("#cacheTypeRadioDatabase").click(function(){
		$("#cacheConfigurationDiv").show();
	});
	$("#cacheClearEntries").click(function(){
		callService("cache.Clear", function(xml){
			showInfo("Cache entries cleared.");
		});
	});
	$("#cacheApply").click(function(){
		configure();
	});
	$("#cacheCreateTable").click(function(){
		configure("create");
	});
	
	
	cache_update();	
}

function configure(create){	
	var messageError="";
	var params={"cacheType":$("input[name=cacheType]:checked").attr("value")};	
	$("#cacheTypeRadioDatabase:checked").each(function(){//database
		if($("#cachePass1").val()!=$("#cachePass2").val()){//the two password fileds are differents
			messageError="The password and its confirmation are different! <br/><br/> Retype them...";			
		}
		else{
			var messageErrorTmp="<ul>";					
			$(".required").each(function(){
				if($(this).val().length<=0){//a field required is empty
					messageErrorTmp+="<li>"+$(this).attr("name")+"</li>";
				}
			});
			messageErrorTmp+="</ul>";
			if(messageErrorTmp!="<ul></ul>"){
				messageError="The following fiels are empty please fill them:"+messageErrorTmp;
			}else{
				if(create){					
					params["create"]="";
				}				
				params["databaseDriver"]=$("#databaseDriverSelect").val();
				params["databaseServerName"]=$("#cacheServerName").val();				
				params["databaseServerPort"]=$("#cachePort").val();				
				params["databaseName"]=$("#cacheDatabaseName").val();
				params["user"]=$("#cacheUserName").val();				
				params["password"]=$("#cachePass1").val();
			}						
		}
	});
	if(messageError==""){
		callService("cache.Configure", function(xml){
			showInfo("Cache sucessfully configured.");
		},params);
	}
	else{
		showError(messageError);
	}
	
}

