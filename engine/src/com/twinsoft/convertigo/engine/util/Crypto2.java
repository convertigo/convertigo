/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.util.DESKey;

public class Crypto2 {

	private Cipher ecipher;
	private Cipher dcipher;

	private Crypto2(String passPhrase) throws InvalidKeySpecException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
		/* Ciphering options:
			Mode = CipherMode.CBC,-( Cipher-block chaining)
			Padding = PaddingMode.PKCS7 or PKCS5,
			KeySize = 128,
			BlockSize = 128,
			Key = keyBytes - password,
			IV = keyBytes  - password
		*/
		
		// Create the key
		byte[] bytesOfMessage = passPhrase.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] bytesPassphrase = md.digest(bytesOfMessage);
		SecretKeySpec key = new SecretKeySpec(bytesPassphrase, "AES");
		
	    // Parameter specific algorithm
	    AlgorithmParameterSpec paramSpec = new IvParameterSpec(bytesPassphrase); 

		ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
		dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	}

	private byte[] encrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
		byte[] ciphered = ecipher.doFinal(data);
		return ciphered;
	}

	private byte[] decrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
		byte[] result = dcipher.doFinal(data);
		return result;
	}

	// Marker for crypto v2 lib (this character is outside the set of
	// hexadecimal characters used by encodeToHexString()).
	private static final char CRYPTO_V2_MARKER = 'x';

	// Marker for crypto v1 lib (this character is outside the set of
	// hexadecimal characters used by encodeToHexString()).
	private static final char CRYPTO_V1_MARKER = 'y';

	// Marker for old twinsoft DESKey crypto lib (this character is outside the set of
	// hexadecimal characters used by encodeToHexString()).
	private static final char CRYPTO_TWINSOFT_DESKEY_MARKER = 'z';

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
			String sCiphered = CRYPTO_V2_MARKER + HexUtils.toHexString(ciphered);
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
	 * Encrypts a byte array using the DES algorithm.
	 * 
	 * @param passPhrase
	 *            the passphrase used for cipher/decipher
	 * @param data
	 *            the byte array to encrypt.
	 * 
	 * @returns the encrypted byte array, or <code>null</code> if any error occurs.
	 * 
	 * @see #decodeFromHexString
	 */
	public static byte[] encodeToByteArray(String passphrase, byte[] data) {
		try {
			Crypto2 crypto = new Crypto2(passphrase);
			byte[] ciphered = crypto.encrypt(data);
			return ciphered;
		} catch (Exception e) {
			// Should never happen
			logError("Unable to encode bytes", e);
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

	/**
	 * Decrypts a byte array using the DES algorithm.
	 * 
	 * @param passPhrase
	 *            the passphrase used for cipher/decipher
	 * @param ciphered
	 *            the ciphered byte array to decrypt.
	 * 
	 * @returns the decrypted byte array, or <code>null</code> if any error occurs.
	 * 
	 * @see #decodeFromHexString
	 */
	public static byte[] decodeFromByteArray(String passphrase, byte[] ciphered) {
		try {
			Crypto2 crypto = new Crypto2(passphrase);
			byte[] deciphered = crypto.decrypt(ciphered);
			return deciphered;
		} catch (Exception e) {
			// Should never happen
			logError("Unable to decode bytes: '" + ciphered + "'", e);
			return null;
		}
	}
	
	public static String encodeToHexString(String data) {
		return Crypto2.encodeToHexString(
				EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE),
				data);
	}

	private static String decodeFromHexString(String ciphered, boolean bTripleDES) {
		logDebug("Decoding: '" + ciphered + "'; bTripleDES (for old crypto): " + bTripleDES);
		// Crypto v2 lib
		if (ciphered.charAt(0) == CRYPTO_V2_MARKER) {
			logDebug("Crypto v2 detected");
			return Crypto2
					.decodeFromHexString(
							EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE),
							ciphered);
		}
		// Crypto v1 lib
		else if (ciphered.charAt(0) == CRYPTO_V1_MARKER) {
			ciphered = ciphered.substring(1);
			logDebug("Crypto v1 detected");
			if (bTripleDES) {
				return com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString3(ciphered);
			}
			else {
				return com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString(ciphered);
			}
		}
		// Crypto twinsoft DESKey
		else if (ciphered.charAt(0) == CRYPTO_TWINSOFT_DESKEY_MARKER) {
			ciphered = ciphered.substring(1);
			logDebug("Crypto twinsoft DESKey detected");
			if (bTripleDES) {
				return DESKey.decodeFromHexString3(ciphered);
			}
			else {
				return DESKey.decodeFromHexString(ciphered);
			}
		}
		// Fallback: try to detect crypto lib
		else {
			logDebug("Trying to detect crypto lib...");
			
			String decipheredValue;
			String recipheredValue;

			// Crypto v1 from C8O?
			if (bTripleDES) {
				decipheredValue = com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString3(ciphered);
				recipheredValue = com.twinsoft.convertigo.engine.util.Crypto.encodeToHexString3(decipheredValue);
			}
			else {
				decipheredValue = com.twinsoft.convertigo.engine.util.Crypto.decodeFromHexString(ciphered);
				recipheredValue = com.twinsoft.convertigo.engine.util.Crypto.encodeToHexString(decipheredValue);
			}

			// Crypto from TWS lib (DESKey)?
			if (ciphered.equals(recipheredValue)) {
				logDebug("Crypto v1 detected");
			} else {
				if (bTripleDES) {
					decipheredValue = DESKey.decodeFromHexString3(ciphered);
					recipheredValue = DESKey.encodeToHexString3(decipheredValue);
				}
				else {
					decipheredValue = DESKey.decodeFromHexString(ciphered);
					recipheredValue = DESKey.encodeToHexString(decipheredValue);
				}
				
				if (!ciphered.equals(recipheredValue)) {
					throw new IllegalArgumentException("Unable to decode value '" + ciphered + "' with any known ciphering methods");
				}
				
				logDebug("Crypto twinsoft DESKey detected");
			}
			
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
			Engine.logEngine.trace(message);
		}
		else {
			System.out.println(message);
		}
	}

}
