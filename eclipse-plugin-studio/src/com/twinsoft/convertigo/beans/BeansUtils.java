package com.twinsoft.convertigo.beans;

public class BeansUtils {

	public static String cleanDescription(String description, boolean bHtml) {
		String cleanDescription = description;
		// Replace first space
		if (cleanDescription.charAt(0) == ' ') {
			cleanDescription = cleanDescription.substring(1);
		}

		// replace orangetwinsoft class by text color style
		cleanDescription = cleanDescription.replace("class=\"orangetwinsoft\"", (bHtml ? "style=\"color=#FC870A;\"" : ""));

		// replace computer class by new font
		cleanDescription = cleanDescription.replace("class=\"computer\"", (bHtml ? "style=\"font-family: lucida Console;\"" : ""));

		// Double BR tags
		cleanDescription = cleanDescription.replaceAll("<br/>(?:<br/>)?", (bHtml ? "<br/><br/>" : ""));

		return cleanDescription;
	}

}
