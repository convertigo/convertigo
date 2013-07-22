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

import com.twinsoft.util.DESKey;
import com.twinsoft.convertigo.engine.util.SimpleSHA1;
import com.twinsoft.util.UnsignedLong;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class PseudoCertificate {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean bEncrypt = true, bInvalid = false, bSha1 = false;
		String filePath = null, key = "test", value = "";
		Properties props = new Properties();
		int len = args.length;
		
		for (int i=0; i< len; i++) {
			String arg = args[i];
			String next = "";
			
			if (i < len-1)
				next = args[i+1];
				
			if (arg.equals("-usage")) {
				bInvalid = true;
				break;					
			}
			else if (arg.equals("-encrypt")) {
				if (next.startsWith("-"))
					bEncrypt = true;
				else {
					System.out.println("Argument non valide :"+ arg);
					bInvalid = true;
					break;					
				}
			}
			else if (arg.equals("-decrypt")) {
				if (next.startsWith("-"))
					bEncrypt = false;
				else {
					System.out.println("Argument non valide :"+ arg);
					bInvalid = true;
					break;					
				}
			}
			else if (arg.equals("-filePath")) {
				if (!next.startsWith("-")) {
					filePath = next;
					i++;
				}
				else {
					System.out.println("Argument non valide :"+ arg);
					bInvalid = true;
					break;
				}
			}
			else if (arg.equals("-cryptKey")) {
				if (!next.startsWith("-")) {
					key = next;
					i++;
				}
				else {
					System.out.println("Argument non valide :"+ arg);
					bInvalid = true;
					break;
				}
			}
			else if (arg.equals("-encpwd")) {
				bSha1 = true;
			}
			else if (arg.startsWith("-")) {
				if (!next.startsWith("-")) {
					value = next;
					props.setProperty(arg.substring(1), value);
					i++;
				}
				else {
					System.out.println("Argument non valide :"+ arg);
					bInvalid = true;
					break;
				}
			}
		}
		
		if (bInvalid)
			usage();
		else
		{
			if (bEncrypt) {
				props.setProperty("comment", "TWINSOFT");
				if (bSha1) {
					value = props.getProperty("password");
					if (value != null) {
						try {
							props.setProperty("password",SimpleSHA1.SHA1(value));
						} catch (NoSuchAlgorithmException e) {
							System.out.println("Probleme lors de l'encodage SHA1-1.");
						} catch (UnsupportedEncodingException e) {
							System.out.println("Probleme lors de l'encodage SHA1-2.");
						}
					}
				}
				if (saveProperties(props, filePath))
					encrypt(filePath, filePath, key);
			}
			else {
				byte[] output = decrypt(filePath, key);
				System.out.println(new String(output));
			}
		}
			
	}

	static public boolean checkCleCrypt(String inputFilePath, String password) {
		byte[] buf = decrypt(inputFilePath, password);
		if (buf != null) {
			String decrypted = new String(buf);
			return (decrypted.indexOf("comment=TWINSOFT")!=-1);
		}
		return false;
	}
	
	static private boolean saveProperties(Properties props, String filePath) {
		int index = filePath.lastIndexOf(".");
		if (index != -1)
			filePath = filePath.substring(0, index+1)+ "udv";
		else
			filePath = filePath + ".udv";
		
		String fileName = filePath;
		index = fileName.lastIndexOf(System.getProperty("file.separator"));
		if (index != -1)
			fileName = fileName.substring(index+1);
		
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					System.out.println("Probleme lors de la creation du fichier.");
					return false;
				}
			} catch (Exception e) {
				System.out.println("Probleme lors de la creation du fichier: " + e.getMessage());
				return false;
			} 
		}
		
		try {
			if (!props.containsKey("certificate"))
				props.setProperty("certificate", fileName);
			
			FileOutputStream fos = new FileOutputStream(file);
			props.store(fos, "");
			fos.flush();
			fos.close();
		} catch (Exception e) {
			System.out.println("Erreur pendant l'archivage du fichier: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	static public void encrypt(String inputFilePath, String outputFilePath, String password) {
		try {
			byte[] clear = openfile(inputFilePath);
			String s = new String(clear);

			String twsKey = password;
			int len = twsKey.length();
			if (len>=8)
				twsKey = twsKey.substring(0, 8);
			else {
				while ((len = twsKey.length())<8) {
					twsKey += " ";
				}
			}
				
			
			DESKey desKey = new DESKey();
			UnsignedLong[] signedKey = new UnsignedLong[32];
			desKey.keySched(twsKey.getBytes(), signedKey);

			int modulo = s.length() % 8;
			int dl = ((modulo != 0) ? (8-modulo) : 0);
			byte data[] = new byte[s.length() + dl];
			for (int i = 0 ; i < data.length ; i++)
				data[i] = (byte) 0;
				
			String encrypted = "";
			byte[] out = new byte[8];
			byte[] in = new byte[8];

			System.arraycopy(s.getBytes(), 0, data, 0, s.length());
				 
			for (int i = 0 ; i < data.length ; i += 8) {
				System.arraycopy(data, i, in, 0, 8);
				desKey.des_ecb_encrypt(in, out, signedKey, DESKey.DES_ENCRYPT);
				encrypted += DESKey.hexString(out);
			}
			
			saveFile(outputFilePath, encrypted.getBytes());
			System.out.println("Pseudo-certificat \""+ inputFilePath +"\" créé.");
		}
		catch(Exception e) {
			System.out.println("Erreur lors de l'encryptage des donnees: " + e.getMessage());
		}
	}
	
	static public byte[] decrypt(String inputFilePath, String password) {
		try {
			byte[] encrypted = openfile(inputFilePath);
			String s = new String(encrypted);
		
			String twsKey = password;
			int len = twsKey.length();
			if (len>=8)
				twsKey = twsKey.substring(0, 8);
			else {
				while ((len = twsKey.length())<8) {
					twsKey += " ";
				}
			}
			
			DESKey desKey = new DESKey();
			UnsignedLong[] signedKey = new UnsignedLong[32];
			desKey.keySched(twsKey.getBytes(), signedKey);

			String decrypted = "";

			byte[] out = new byte[8];
			byte[] in = new byte[8];
			byte[] data = s.getBytes();
			byte[] data2 = new byte[16];

			for (int i = 0 ; i < data.length ; i += 16) {
				System.arraycopy(data, i, data2, 0, 16);
				String tmp = new String(data2);
				in = DESKey.fromHexString(tmp);
				desKey.des_ecb_encrypt(in, out, signedKey, DESKey.DES_DECRYPT);
				decrypted += new String(out);
			}
		
			int posNullChar = decrypted.indexOf(0);
			if (posNullChar != -1)
				decrypted = decrypted.substring(0, posNullChar);
			return decrypted.getBytes();
		}
		catch(Exception e) {
			System.out.println("Erreur lors du décryptage des donnees: " + e.getMessage());
			return null;
		}
	}
	
	static private byte[] openfile(String filename) {
		try {
			File file = new File(filename);
			byte[] result = new byte[(int) file.length()];
			FileInputStream in = new FileInputStream(filename);
			in.read(result);
			in.close();
			return result;
		}
		catch (Exception e) {
			System.out.println("Probleme lors de la lecture du fichier: " + e.getMessage());
			return null;
		}
	}

	static private void saveFile(String filename, byte[] data) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			out.write(data);
			out.close();
		}
		catch (Exception e) {
			System.out.println("Probleme lors de la sauvegarde du fichier: " + e.getMessage());
		}
	}
	
	public static void usage() {
		System.out.println("PseudoCertificate : encrypt data to file with given key.\n");
		System.out.println("Usage : PseudoCertificate -filePath value -cryptKey value -login value -password value -certificate value -encpwd");
		System.out.println("-filePath \tthe full path to file to create");
		System.out.println("-cryptKey \tthe key to use to encrypt file");
		System.out.println("-login \tthe login parameter for sign on");
		System.out.println("-password \tthe password parameter for sign on");
		System.out.println("-certificate \tthe client certificate to use for ssl request");
		System.out.println("-encpwd \tthe password will be transmited in SHA1");
	}	
}


