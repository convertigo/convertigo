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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/engine/util/Crypto.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.engine.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class Crypto2 {

	private Cipher ecipher;
	private Cipher dcipher;

	// 8-byte salt
	private static final byte[] SALT = { (byte) 'a', (byte) 'M', (byte) 'f', (byte) 's', (byte) 'B',
			(byte) 'x', (byte) 'P', (byte) 'L' };

	private static final String CIPHERING_ALGORITHM = "PBEWithMD5AndTripleDES";

	private Crypto2(String passPhrase) throws InvalidKeySpecException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// Create the key
		PBEKeySpec pbeKeySpec = new PBEKeySpec(passPhrase.toCharArray());
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, 20);

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(CIPHERING_ALGORITHM);
		SecretKey pbeKey = secretKeyFactory.generateSecret(pbeKeySpec);

		ecipher = Cipher.getInstance(CIPHERING_ALGORITHM);
		dcipher = Cipher.getInstance(CIPHERING_ALGORITHM);

		ecipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
		dcipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
	}

	private byte[] encrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
		byte[] ciphered = ecipher.doFinal(data);
		return ciphered;
	}

	private byte[] decrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
		byte[] result = dcipher.doFinal(data);
		return result;
	}

	// Marker for new crypto lib (this character is outside the set of
	// hexadecimal characters used by encodeToHexString()).
	private static final char NEW_CRYPTO_MARKER = 'x';

	/**
	 * Encrypts a string using the DES algorithm.
	 * 
	 * @param passPhrase
	 *            the passphrase used for cipher/decipher
	 * @param sData
	 *            the string to encrypt.
	 * 
	 * @returns the encrypted string; the script is of hexadecimal string
	 *          format, i.e. it contains only hexadecimal (printable)
	 *          characters, or <code>null</code> if any error occurs.
	 * 
	 * @see #decodeFromHexString
	 */
	public static String encodeToHexString(String passphrase, String sData) {
		try {
			Crypto2 crypto = new Crypto2(passphrase);
			byte[] data = sData.getBytes("UTF-8");
			byte[] ciphered = crypto.encrypt(data);
			String sCiphered = NEW_CRYPTO_MARKER + HexUtils.toHexString(ciphered);
			return sCiphered;
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			logError("Unable to convert string '" + sData + "' to UTF-8 byte array", e);
			return null;
		} catch (Exception e) {
			// Should never happen
			logError("Unable to encode to hex string: '" + sData + "'", e);
			return null;
		}
	}

	/**
	 * Decrypts a string using the DES algorithm.
	 * 
	 * @param passPhrase
	 *            the passphrase used for cipher/decipher
	 * @param ciphered
	 *            the string to decrypt; this string must have been encoded by
	 *            the <code>encodeToHexString()</code> function in order to stay
	 *            meaningfull.
	 * 
	 * @returns the decrypted string, or <code>null</code> if any error occurs.
	 * 
	 * @see #encodeToHexString
	 */
	public static String decodeFromHexString(String passphrase, String ciphered) {
		try {
			Crypto2 crypto = new Crypto2(passphrase);
			// Remove the new crypto marker before getting the ciphered data
			String cipheredOnlyData = ciphered.substring(1);
			byte[] data = HexUtils.fromHexString(cipheredOnlyData);
			byte[] deciphered = crypto.decrypt(data);
			return new String(deciphered, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen
			logError("Unable to convert byte array to UTF-8 string", e);
			return null;
		} catch (Exception e) {
			// Should never happen
			logError("Unable to decode from hex string: '" + ciphered + "'", e);
			return null;
		}
	}

	public static String encodeToHexString(String data) {
		return Crypto2.encodeToHexString(EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE),
				data);
	}

	@SuppressWarnings("deprecation")
	private static String decodeFromHexString(String ciphered, boolean bTripleDES) {
		logDebug("Decoding: '" + ciphered + "'; bTripleDES (for old crypto): " + bTripleDES);
		// New crypto lib
		if (ciphered.charAt(0) == NEW_CRYPTO_MARKER) {
			return Crypto2
					.decodeFromHexString(EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE),
							ciphered);
		}
		// Old crypto lib
		else {
			logDebug("Old crypto lib detected");
			
			String decipheredValue;

			if (bTripleDES)
				decipheredValue = com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString3(ciphered);
			else
				decipheredValue = com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString(ciphered);

			if (decipheredValue == null) {
				throw new IllegalArgumentException("Unable to decode value '" + ciphered + "'");
			}

			return decipheredValue;
		}
	}

	// Helper routine for backward compatibility
	public static String decodeFromHexString(String ciphered) {
		return decodeFromHexString(ciphered, false);
	}

	// Helper routine for backward compatibility
	public static String decodeFromHexString3(String ciphered) {
		return decodeFromHexString(ciphered, true);
	}
	
	private static void logError(String message, Exception e) {
		if (Engine.logEngine != null) {
			Engine.logEngine.error(message, e);
		}
		else {
			System.out.println(message);
			e.printStackTrace();
		}
	}
	
	private static void logDebug(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.debug(message);
		}
		else {
			System.out.println(message);
		}
	}
}
