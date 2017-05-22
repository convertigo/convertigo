/*
 * Copyright (c) 2001-2017 Convertigo. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of  Convertigo  or in accordance  with  the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes  no  representations  or  warranties  about  the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

var instanceUpdateDate=0;

function engine_GetStatus_init() {
	$("#statusRestartEngine").button({ icons: { primary: "ui-icon-power" } });
	$("#statusRestartEngine").click(function() {
		callService("engine.Stop", function() {
			callService("engine.Start", function() {				
				engine_GetStatus_update();				
			});
		});
	});
	
	$('.tdStartEngine').hide();

	$('.btnStartEngine').click(function() {
		callService("engine.Start", function() {
		});
	});

	$('.btnStopEngine').click(function() {
		callService("engine.Stop", function() {
		});

	});

	engine_GetStatus_update();
}

function engine_GetStatus_update() {
	callService("engine.GetStatus", function(xml) {
		if ($(xml).find("engineState").text() == "started") {
			$("#statusStartedLed").attr("src", "images/convertigo-administration-picto-bullet-green.png");
			$("#statusStoppedLed").attr("src", "images/convertigo-administration-picto-bullet-gray.png");
		} else {
			$("#statusStartedLed").attr("src", "images/convertigo-administration-picto-bullet-gray.png");
			$("#statusStoppedLed").attr("src", "images/convertigo-administration-picto-bullet-red.png");}

		$("#statusStartStopVerb").html($(xml).find("engineState").text());
		var startDate = new Date(parseInt($(xml).find("startStopDate").text()));
		$("#statusStartDate").text(startDate);
		$("#statusUptimeDays").text($(xml).find("runningElapse").attr("days"));
		$("#statusUptimeHours").text($(xml).find("runningElapse").attr("hours"));
		$("#statusUptimeMinutes").text($(xml).find("runningElapse").attr("minutes"));
		$("#statusUptimeSeconds").text($(xml).find("runningElapse").attr("seconds"));
		$("#statusProduct").html($(xml).find("version").attr("product"));
		$("#statusBuild").html($(xml).find("version").attr("build"));
		$("#statusEngine").html($(xml).find("version").attr("engine"));
		$("#statusObjects").html($(xml).find("version").attr("beans"));	
		$("#statusBuildDate").html($(xml).find("build").attr("date"));
		$("#statusBuildFilename").html($(xml).find("build").attr("filename"));	
		
		$("#statusLicenceType").text($(xml).find("version").attr("licence-type"));	
		$("#statusLicenceNumber").text($(xml).find("version").attr("licence-number"));
		var lses = $(xml).find("version").attr("licence-sessions");
		$("#statusLicenceSessions").text((lses == "") ? "n/a":lses);
		var lend = $(xml).find("version").attr("licence-end");
		$("#statusLicenceEndDate").text(lend == "" ? "n/a":dateFormat(lend, "ddd, mmm dS yyyy"));
		
		if ($(xml).find("version").attr("licence-expired") === "true") {
			$("#statusLicenceEndDate").css("color", "red");
			$("#statusLicenceEndDate").css("font-weight", "bold");
		} 
		
		instanceUpdateDate++;
		init_date_started_from(instanceUpdateDate);		
	});
}


function init_date_started_from(instance){
	dateUpdateRunning=true;
	var dateFrom=$("#statusStartDateRunningIE").text();
	var daysInit=parseInt(dateFrom.replace(/day(.*)/g,""));
	var hoursInit=parseInt(dateFrom.replace(/hour(.*)/g,"").replace(/(.*)day/g,"").replace(/[^\d]/g,""));
	var minutesInit=parseInt(dateFrom.replace(/minute(.*)/g,"").replace(/(.*)hour/g,"").replace(/[^\d]/g,""));
	var secondsInit=parseInt(dateFrom.replace(/(.*)minute/g,"").replace(/[^\d]/g,""));		
	var dateSince1970=new Date().getTime();
	dateSince1970-=1000*(secondsInit+(60*(minutesInit+(60*(hoursInit+(24*daysInit))))));
	engine_GetStatus_update_hour(dateSince1970,instance);
}


function engine_GetStatus_update_hour(dateSince1970,instance){	
	var newDate = new Date().getTime();	
	var diff = newDate - dateSince1970;
	diff /= 1000;//time in seconds
	var seconds = parseInt(diff % 60);
	diff /= 60;//time in minutes
	var minutes = parseInt(diff % 60);
	diff /= 60;//time in hours
	var hours = parseInt(diff % 24);
	var days = parseInt(diff / 24);//time in days
	
	if (instance == instanceUpdateDate) {
		$("#statusStartDateRunningIE").html(""+
				days+" day"+plurialSuffix(days)+", "+
				hours+" hour"+plurialSuffix(hours)+", "+
				minutes+" minute"+plurialSuffix(minutes)+", "+			
				seconds+" second"+plurialSuffix(seconds)+", "			
		);	
			
		if ($("#engine_GetStatus:visible").length > 0) {
			setTimeout(function() {			
				engine_GetStatus_update_hour(dateSince1970,instance);			
			}, 1000);
		}
	}
}

function plurialSuffix(value){
	if(value>0)
		return "s";
	return "";
}
