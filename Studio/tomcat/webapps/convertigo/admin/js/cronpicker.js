function getId(id) {
	return document.getElementById(id);
}

function createRange(sRange) {
	var aRange = sRange.split(",");
	var newRange = new Array();
	var i=0;
	while (i<aRange.length) {
		var deb = aRange[i];
		var fin = aRange[i];
		var inc = 1;
		while ((i+inc < aRange.length) && (1*aRange[i+inc] == 1*aRange[i]+inc)) {
			fin = 1*aRange[i+inc];
			inc++;
		}
		if (deb == fin) {
			newRange.push(deb);
		}
		else {
			newRange.push(deb + "-" + fin);
		}
		i = i + inc;
	}
		
	return newRange.toString();
}

function generateCron() {
	var sCronResult = "";
	var secondes = "0";
	var minutes = "";
	var heures = "";
	var joursdumois = "";
	var mois = "";
	var joursdelasemaine = "";
	
	minutes = generateCronFragment('schedulerCronWizardMinutes');
	minutes = createRange(minutes);
	heures = generateCronFragment('schedulerCronWizardHours');
	heures = createRange(heures);
	joursdumois = generateCronFragment('schedulerCronWizardDaysOfMonth');
	joursdumois = createRange(joursdumois);
	mois = generateCronFragment('schedulerCronWizardMonths');
	mois = createRange(mois);
	joursdelasemaine = generateCronFragment('schedulerCronWizardDayOfWeek');
	joursdelasemaine = createRange(joursdelasemaine);
	
	if (joursdumois == "?" && joursdelasemaine == "?") {
		joursdumois = "*";
	}
	
	sCronResult = secondes + " " + minutes + " " + heures + " " + joursdumois + " " + mois + " " + joursdelasemaine
	//alert(sCronResult);
	getId("schedulerCron").value = sCronResult;
}

function generateCronFragment(source) {
	var currentSel = getId(source);
	var sResult = "";
	if (currentSel.options[0].selected) {
		sResult = currentSel.options[0].value;
	}
	else {
		for (var i=1; i<currentSel.options.length; i++) {
			if (currentSel.options[i].selected) {
				sResult += currentSel.options[i].value + ",";
			}
		}
	}
	
	return sResult.replace(/,$/, "");
}

function emptyAndSelectList(liste, sel) {
	var current = getId(liste);
	//alert("sel='" + sel + "'");
	for (var j=0; j<current.options.length; j++) {
		if (sel.indexOf("," + current.options[j].value + ",") != -1) {
			current.options[j].selected = true;
		}
		else {
			current.options[j].selected = false;
		}
	}
}

function parseCron() {
	var sCron = getId("schedulerCron").value;
	var bParseOK = true;
	var mycron = sCron.split(" ");
	//minutes
	var minutes = mycron[1].split(",");
	for (var k=0; k<minutes.length; k++) {
		//alert("minutes[k]='" + minutes[k] + "'");
		if (minutes[k].indexOf("-") != -1) {
			var tmpRange = minutes[k].split("-");
			var tmpValue = "";
			//alert("tmpRange[0]='" + tmpRange[0] + "', tmpRange[1]='" + tmpRange[1] + "'");
			for (var l=1*tmpRange[0]; l<=1*tmpRange[1]; l++) {
				//alert("l='" + l + "'");
				tmpValue += l + ",";
			}
			minutes[k] = tmpValue.replace(/,$/, "");
		}
	}
	//alert("minutes='" + minutes.toString() + "'");
	emptyAndSelectList('schedulerCronWizardMinutes',"," + minutes.toString() + ",");
	//heures
	var heures = mycron[2].split(",");
	for (var k=0; k<heures.length; k++) {
		if (heures[k].indexOf("-") != -1) {
			var tmpRange = heures[k].split("-");
			var tmpValue = "";
			for (var l=tmpRange[0]; l<=tmpRange[1]; l++) {
				tmpValue += l + ",";
			}
			heures[k] = tmpValue.replace(/,$/, "");
		}
	}
	emptyAndSelectList("schedulerCronWizardHours","," + heures.toString() + ",");
	//jours du mois
	mycron[3] = mycron[3].replace("*", "?");
	var joursdumois = mycron[3].split(",");
	for (var k=0; k<joursdumois.length; k++) {
		if (joursdumois[k].indexOf("-") != -1) {
			var tmpRange = joursdumois[k].split("-");
			var tmpValue = "";
			for (var l=tmpRange[0]; l<=tmpRange[1]; l++) {
				tmpValue += l + ",";
			}
			joursdumois[k] = tmpValue.replace(/,$/, "");
		}
	}
	emptyAndSelectList("schedulerCronWizardDaysOfMonth","," + joursdumois.toString() + ",");
	//mois
	var mois = mycron[4].split(",");
	for (var k=0; k<mois.length; k++) {
		if (mois[k].indexOf("-") != -1) {
			var tmpRange = mois[k].split("-");
			var tmpValue = "";
			for (var l=tmpRange[0]; l<=tmpRange[1]; l++) {
				tmpValue += l + ",";
			}
			mois[k] = tmpValue.replace(/,$/, "");
		}
	}
	emptyAndSelectList("schedulerCronWizardMonths","," + mois.toString() + ",");
	//jours de la semaine
	mycron[5] = mycron[5].replace("*", "?");
	var joursdelasemaine = mycron[5].split(",");
	for (var k=0; k<joursdelasemaine.length; k++) {
		if (joursdelasemaine[k].indexOf("-") != -1) {
			var tmpRange = joursdelasemaine[k].split("-");
			var tmpValue = "";
			for (var l=tmpRange[0]; l<=tmpRange[1]; l++) {
				tmpValue += l + ",";
			}
			joursdelasemaine[k] = tmpValue.replace(/,$/, "");
		}
	}
	emptyAndSelectList("schedulerCronWizardDayOfWeek","," + joursdelasemaine.toString() + ",");
}

