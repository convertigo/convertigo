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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpSession;

public class RsaManager implements AbstractManager {
	private final static int keyLenght = 512;
	private enum key{cipher, publickey};
	private KeyPairGenerator kpg;

	public void destroy() throws EngineException {
	}

	public void init() throws EngineException {
		try {
			
        	kpg = KeyPairGenerator.getInstance("RSA");
        	kpg.initialize(keyLenght);
        	
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public String decrypt(String encrypted, HttpSession session) {
    	Cipher dec = (Cipher) session.getAttribute(key.cipher.toString());
    	if(dec==null) throw new RuntimeException("no cipher");
        String[] blocks = encrypted.split("\\s");
        StringBuffer result = new StringBuffer();
        try {
            for ( int i = blocks.length-1; i >= 0; i-- ) {
                byte[] data = hexStringToByteArray(blocks[i]);
                byte[] decryptedBlock = dec.doFinal(data);
                try {
                	int offset = 0;
                	while(offset<decryptedBlock.length && decryptedBlock[offset]==0)
                		offset++;
					result.append(new String(decryptedBlock, offset, decryptedBlock.length-offset, "utf-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decrypt error",e);
        }
        return result.reverse().toString();
    }
    
    public String getPublicKey(HttpSession session) {
    	String publicKey = (String) session.getAttribute(key.publickey.toString());
    	Cipher dec = (Cipher) session.getAttribute(key.cipher.toString());
    	if(publicKey == null || dec == null) try {
    		KeyPair kp = kpg.generateKeyPair();


    		dec = Cipher.getInstance("RSA/ECB/NOPADDING");

    		dec.init(Cipher.DECRYPT_MODE, kp.getPrivate());

    		RSAPublicKey pk = (RSAPublicKey)kp.getPublic();
    		publicKey = pk.getPublicExponent().toString(16)+'|'+pk.getModulus().toString(16)+'|'+getMaxDigits(keyLenght);
    		
    		session.setAttribute(key.publickey.toString(), publicKey);
    		session.setAttribute(key.cipher.toString(), dec);
    	} catch (NoSuchAlgorithmException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (NoSuchPaddingException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return publicKey;
    }

//    /**
//     * Parse url string (Todo - better parsing algorithm)
//     * @param url value to parse
//     * @param encoding encoding value
//     * @return Map with param name, value pairs
//     */
//    public static Map<String, String> parse(String url,String encoding) {
//        try {
//            String urlToParse = URLDecoder.decode(url,encoding);
//            String[] params = urlToParse.split("&");
//            Map<String, String> parsed = new HashMap<String, String>(params.length);
//            for (int i = 0; i<params.length; i++ )  {
//                String[] p = params[i].split("=");
//                String name = p[0];
//                String value = (p.length==2)?p[1]:null;
//                parsed.put(name, value);
//            }
//            return parsed;
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException("Unknown encoding.",e);
//        }
//    }
//
//    /**
//     * Return public RSA key modulus
//     * @param keyPair RSA keys
//     * @return modulus value as hex string
//     */
//    public static String getPublicKeyModulus( KeyPair keyPair ) {
//        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
//        return publicKey.getModulus().toString(16);
//    }
//
//    /**
//     * Return public RSA key exponent
//     * @param keyPair RSA keys
//     * @return public exponent value as hex string
//     */
//    public static String getPublicKeyExponent( KeyPair keyPair ) {
//        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
//        return publicKey.getPublicExponent().toString(16);
//    }
//
//    /**
//     * Max block size with given key length
//     * @param keyLength length of key
//     * @return numeber of digits
//     */
    private static int getMaxDigits(int keyLength)   {
        return ((keyLength *2)/16)+3;
    }
//
//    /**
//     * Convert byte array to hex string
//     * @param bytes input byte array
//     * @return Hex string representation
//     */
//    private static String byteArrayToHexString(byte[] bytes) {
//        StringBuffer result = new StringBuffer();
//        for (int i=0; i < bytes.length; i++) {
//            result.append( Integer.toString( ( bytes[i] & 0xff ) + 0x100, 16).substring( 1 ) );
//        }
//        return result.toString();
//    }

    /**
     * Convert hex string to byte array
     * @param data input string data
     * @return bytes
     */
    private static byte[] hexStringToByteArray(String data) {
        int k = 0;
        byte[] results = new byte[data.length() / 2];
        for (int i = 0; i < data.length();) {
            results[k] = (byte) (Character.digit(data.charAt(i++), 16) << 4);
            results[k] += (byte) (Character.digit(data.charAt(i++), 16));
            k++;
        }
        return results;
    }
	
}