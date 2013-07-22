/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

import com.twinsoft.convertigo.engine.util.Crypto2;

public class InstallCerts {

	private static String currentDir;
	
	public static void main(String[] args) {
//		args = new String[2];
//		args[0] = "9901000001A.pfx";
//		args[1] = "0d5f84b0-9b31-064a-04ab-9f8fc5";
//		args[0] = "-f";
//		args[1] = "liste.txt";
		
		currentDir = System.getProperty("user.dir");
		System.out.println("Repertoire courant : " + currentDir);
		
		if (!checkArgs(args)) {
			printCommandSyntax();
			return;
		}

		try {
			if ("-f".equals(args[0])) {
				BufferedReader br = new BufferedReader(new FileReader(currentDir + "/" + args[1]));

				String fileName;
				String password;
				
				String line;
				StringTokenizer st;
				int i = 0;
				while ((line = br.readLine()) != null) {
					i++;
					line = line.trim();
					if (line.length() > 0) {
						st = new StringTokenizer(line, " \t");
						
						if (st.countTokens() != 2) {
							System.out.println("\n> (!) Format invalide (ligne " + i + ") : '" + line + "'");
							continue;
						}
						
						fileName = st.nextToken();
						System.out.println("\n> Installation du certificat " + fileName);

						password = st.nextToken();
						if ((password == null) || (password.length() == 0)) {
							System.out.println("  (!) Mot de passe non precise pour le certificat '" + fileName + "'");
						}
						else {
							installCertificate(fileName, password);
						}
					}
				}
			}
			else {
				String fileName = args[0];
				String password = args[1];
				installCertificate(fileName, password);
			}
		}
		catch(FileNotFoundException e) {
			System.out.println("Erreur : " + e.getMessage());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean checkArgs(String[] args) {
		if (args.length != 2) return false;
		
		return true;
	}
	
	private static void installCertificate(String fileName, String password) {
		File file = new File(currentDir + "/" + fileName);
		if (!file.exists()) {
			System.out.println("  (!) Le fichier de certificat '" + fileName + "' n'existe pas");
			return;
		}

		if (!CertificateManager.checkCertificatePassword("client", file.getPath(), password)) {
			System.out.println("  (!) Mot de passe invalide");
			return;
		}
		System.out.println("  Mot de passe : " + password);

		try {
			String propertiesFileName = currentDir + "/" + CertificateManager.STORES_PROPERTIES_FILE_NAME;
			File propertiesFile = new File(propertiesFileName);
			Properties properties = new Properties();
			properties.load(new FileInputStream(propertiesFile));
			properties.setProperty(fileName, Crypto2.encodeToHexString(password));
			properties.setProperty(fileName + ".type", "client");
			properties.store(new FileOutputStream(propertiesFile), null);
		}
		catch(IOException e) {
			System.out.println("  (!) Erreur lors de l'acces au fichier de proprietes de certificats : " + e.getMessage());
			return;
		}
		
		System.out.println("  Certificat installe");
	}
	
	public static void printCommandSyntax() {
		System.out.println("InstallCerts installe ou renouvele des certificats clients dans");
		System.out.println("la base de certificats clients de Convertigo.");
		System.out.println("");
		System.out.println("InstallCerts <nom du certificat> <mot de passe du certificat>");
		System.out.println("");
		System.out.println("   - Le nom du certificat est le nom du fichier contenant le");
		System.out.println("     certificat. Ce fichier doit se trouver dans le repertoire");
		System.out.println("     courant.");
		System.out.println("   - Le mot de passe du certificat est le mot de passe associe");
		System.out.println("     au certificat. Ce mot de passe est verifie avant la mise a");
		System.out.println("     jour de la base de certificats de Convertigo.");
		System.out.println("");
		System.out.println("InstallCerts -f <fichier de liste de certificats>");
		System.out.println("");
		System.out.println("   - Le fichier de liste de certificats est un fichier contenant.");
		System.out.println("     une liste de couples <nom du certificat> <mot de passe du certificat>");
		System.out.println("     (chaque couple est ecrit sur une ligne, et le nom est separe");
		System.out.println("     du mot de passe par les caracteres espace ou tabulation).");
		System.out.println("");
		System.out.println("     Exemple :");
		System.out.println("");
		System.out.println("certificat1.pfx   mot_de_passe");
		System.out.println("certificat2.pfx   toto");
		System.out.println("cert              alzkerhflazkjefh");
		System.out.println("990121110.pfx     0a1g2-hgg1x-d017f-65a1b");
		System.out.println("");
		System.out.println("     Les noms de fichier des certificats ne doivent pas comporter");
		System.out.println("     d'espaces.");
		System.out.println("     Les lignes mal formattees seront ignorees.");
		System.out.println("     Les mots de passe sont verifies avant la mise a jour de la");
		System.out.println("     base de certificats clients de Convertigo.");
	}
	
}
