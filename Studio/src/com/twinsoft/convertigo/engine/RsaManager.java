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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;

public class RsaManager implements AbstractManager {
	private final static Pattern findTimestamp = Pattern.compile("^ts=(\\d+)&(.*$)");
	private final static int keyLength = 512;
	private final static int chunckSize = keyLength / 4;
	private final static SecurityException securityException = new SecurityException();
	
	static {
		securityException.setStackTrace(new StackTraceElement[0]);	
	}
	
	private enum key{cipher, publickey, rsaTimestamp};
	private KeyPairGenerator kpg;

	public void destroy() throws EngineException {
	}

	public void init() throws EngineException {
		try {
        	kpg = KeyPairGenerator.getInstance("RSA");
        	kpg.initialize(keyLength);
		} catch (NoSuchAlgorithmException e) {
			kpg = null;
			Engine.logEngine.error("(RsaManager) Failed to initialize RSA KeyPairGenerator", e);
		}
	}

    public String decrypt(String encrypted, HttpSession session) {
    	Cipher dec = (Cipher) session.getAttribute(key.cipher.toString());
    	
    	if (dec == null) {
    		Engine.logEngine.warn("(RsaManager) No cipher is session " + session.getId());
    		throw securityException;
    	}
    	
        StringBuffer result = new StringBuffer();
        try {
            for (int i = 0; i < encrypted.length(); i += chunckSize) {
                byte[] data = hexStringToByteArray(encrypted.substring(i, i + chunckSize));
                byte[] decryptedBlock = dec.doFinal(data);
                try {
                	int offset = 0;
                	while (offset < decryptedBlock.length && decryptedBlock[offset] == 0) {
                		offset++;
                	}
					result.append(new String(decryptedBlock, offset, decryptedBlock.length-offset, "utf-8"));
				} catch (UnsupportedEncodingException e) {
					Engine.logEngine.warn("(RsaManager) Failed to decode decryptedBlock", e);
				}
            }
        } catch (GeneralSecurityException e) {
        	Engine.logEngine.warn("(RsaManager) Failed to decrypt a message", e);
            throw securityException;
        }
        
        Engine.logEngine.debug("(RsaManager) Request decrypted for session " + session.getId());
        String query = result.toString();
        
    	synchronized (session) {
	        Long exTs = (Long) session.getAttribute(key.rsaTimestamp.toString());
	    	Matcher findTS = findTimestamp.matcher(query);
	    	
	    	if (findTS.matches()) {
	    		try {
	    			Long newTs = Long.parseLong(findTS.group(1));
	    			
	    			if (exTs == null || newTs > exTs) {
	    				Engine.logEngine.debug("(RsaManager) Update timestamp for session " + session.getId());
	    				session.setAttribute(key.rsaTimestamp.toString(), newTs);
	        			return findTS.group(2);
	        		}
	    		} catch (NumberFormatException e) {
	    		}
	    		Engine.logEngine.info("(RsaManager) Invalid timestamp for session " + session.getId());
	    		throw securityException;
	    	} else if (exTs != null) {
	    		Engine.logEngine.info("(RsaManager) No timestamp for session " + session.getId());
	    		throw securityException;
	    	}
		}
    	
        return query;
    }
    
    public String getPublicKey(HttpSession session) {
    	String publicKey = (String) session.getAttribute(key.publickey.toString());
    	Cipher dec = (Cipher) session.getAttribute(key.cipher.toString());
    	
    	if (publicKey == null || dec == null) {
    		try {
	    		KeyPair kp = kpg.generateKeyPair();
	    		dec = Cipher.getInstance("RSA/ECB/NOPADDING");
	    		dec.init(Cipher.DECRYPT_MODE, kp.getPrivate());
	
	    		RSAPublicKey pk = (RSAPublicKey) kp.getPublic();
	    		publicKey = pk.getPublicExponent().toString(16) + '|' + pk.getModulus().toString(16) + '|' + getMaxDigits(keyLength);
	    		
	    		session.setAttribute(key.publickey.toString(), publicKey);
	    		session.setAttribute(key.cipher.toString(), dec);
	    	} catch (Exception e) {
	    		Engine.logEngine.warn("Can't create publicKey for session " + session.getId(), e);
	    	}
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