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

package com.twinsoft.convertigo.engine.util;

import com.twinsoft.util.UnsignedLong;

public class Crypto {
	
	private static final byte DES_ENCRYPT = 1;
	private static final byte DES_DECRYPT = 0;
	private static final byte NUM_WEAK_KEY = 16;
	private static final byte ITERATIONS = 16;

	/**
	 * The private key for DES.
	 */
	private final static byte[] defaultPrivateKey = { (byte)'B', (byte)'z', (byte)'K', (byte)'Y', (byte)'w', (byte)'o', (byte)'m', (byte)'t' };

	/*public static final void main(String[] args) {
	    String sample = "twinsoft", message;
	    //System.out.println("Clé               : " + skey1 + skey2 + skey3);
	    System.out.println("Message original  : [" + sample + "]");
	    System.out.println("Message chiffré   : [" + (message = encodeToHexString(sample)) + "]");
	    System.out.println("Message déchiffré : [" + (message = decodeFromHexString(message)) + "]");
	}*/

	private static final String skey1 = "0606f60909f60606";
	private static final String skey2 = "f6f606090906f6f6";
	private static final String skey3 = "0606f60909f60606";
	private static final byte[] key1 = HexUtils.fromHexString(skey1);
	private static final byte[] key2 = HexUtils.fromHexString(skey2);
	private static final byte[] key3 = HexUtils.fromHexString(skey3);

	private static final int[] shifts2 = { 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0 };
	private static final byte[][] weakKeys = {
			// weak keys
			{(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01 },
			{ (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE },
			{ (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F },
			{ (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0 },
			// semi-weak keys
			{(byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE },
			{ (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01 },
			{ (byte)0x1F, (byte)0xE0, (byte)0x1F, (byte)0xE0, (byte)0x0E, (byte)0xF1, (byte)0x0E, (byte)0xF1 },
			{ (byte)0xE0, (byte)0x1F, (byte)0xE0, (byte)0x1F, (byte)0xF1, (byte)0x0E, (byte)0xF1, (byte)0x0E },
			{ (byte)0x01, (byte)0xE0, (byte)0x01, (byte)0xE0, (byte)0x01, (byte)0xF1, (byte)0x01, (byte)0xF1 },
			{ (byte)0xE0, (byte)0x01, (byte)0xE0, (byte)0x01, (byte)0xF1, (byte)0x01, (byte)0xF1, (byte)0x01 },
			{ (byte)0x1F, (byte)0xFE, (byte)0x1F, (byte)0xFE, (byte)0x0E, (byte)0xFE, (byte)0x0E, (byte)0xFE },
			{ (byte)0xFE, (byte)0x1F, (byte)0xFE, (byte)0x1F, (byte)0xFE, (byte)0x0E, (byte)0xFE, (byte)0x0E },
			{ (byte)0x01, (byte)0x1F, (byte)0x01, (byte)0x1F, (byte)0x01, (byte)0x0E, (byte)0x01, (byte)0x0E },
			{ (byte)0x1F, (byte)0x01, (byte)0x1F, (byte)0x01, (byte)0x0E, (byte)0x01, (byte)0x0E, (byte)0x01 },
			{ (byte)0xE0, (byte)0xFE, (byte)0xE0, (byte)0xFE, (byte)0xF1, (byte)0xFE, (byte)0xF1, (byte)0xFE },
			{ (byte)0xFE, (byte)0xE0, (byte)0xFE, (byte)0xE0, (byte)0xFE, (byte)0xF1, (byte)0xFE, (byte)0xF1 }
		};
	private static final byte[] oddParity = {
			1, 1, 2, 2, 4, 4, 7, 7, 8, 8, 11, 11, 13, 13, 14, 14, 16, 16, 19, 19, 21, 21, 22, 22, 25, 25, 26, 26, 28, 28, 31, 31,
			32, 32, 35, 35, 37, 37, 38, 38, 41, 41, 42, 42, 44, 44, 47, 47, 49, 49, 50, 50, 52, 52, 55, 55, 56, 56, 59, 59, 61,
			61, 62, 62, 64, 64, 67, 67, 69, 69, 70, 70, 73, 73, 74, 74, 76, 76, 79, 79, 81, 81, 82, 82, 84, 84, 87, 87, 88, 88,
			91, 91, 93, 93, 94, 94, 97, 97, 98, 98, 100, 100, 103, 103, 104, 104, 107, 107, 109, 109, 110, 110, 112, 112, 115,
			115, 117, 117, 118, 118, 121, 121, 122, 122, 124, 124, 127, 127, (byte)128, (byte)128, (byte)131, (byte)131,
			(byte)133, (byte)133, (byte)134, (byte)134, (byte)137, (byte)137, (byte)138, (byte)138, (byte)140, (byte)140,
			(byte)143, (byte)143, (byte)145, (byte)145, (byte)146, (byte)146, (byte)148, (byte)148, (byte)151, (byte)151,
			(byte)152, (byte)152, (byte)155, (byte)155, (byte)157, (byte)157, (byte)158, (byte)158, (byte)161, (byte)161,
			(byte)162, (byte)162, (byte)164, (byte)164, (byte)167, (byte)167, (byte)168, (byte)168, (byte)171, (byte)171,
			(byte)173, (byte)173, (byte)174, (byte)174, (byte)176, (byte)176, (byte)179, (byte)179, (byte)181, (byte)181,
			(byte)182, (byte)182, (byte)185, (byte)185, (byte)186, (byte)186, (byte)188, (byte)188, (byte)191, (byte)191,
			(byte)193, (byte)193, (byte)194, (byte)194, (byte)196, (byte)196, (byte)199, (byte)199, (byte)200, (byte)200,
			(byte)203, (byte)203, (byte)205, (byte)205, (byte)206, (byte)206, (byte)208, (byte)208, (byte)211, (byte)211,
			(byte)213, (byte)213, (byte)214, (byte)214, (byte)217, (byte)217, (byte)218, (byte)218, (byte)220, (byte)220,
			(byte)223, (byte)223, (byte)224, (byte)224, (byte)227, (byte)227, (byte)229, (byte)229, (byte)230, (byte)230,
			(byte)233, (byte)233, (byte)234, (byte)234, (byte)236, (byte)236, (byte)239, (byte)239, (byte)241, (byte)241,
			(byte)242, (byte)242, (byte)244, (byte)244, (byte)247, (byte)247, (byte)248, (byte)248, (byte)251, (byte)251,
			(byte)253, (byte)253, (byte)254, (byte)254
		};
	private static final long[][] SPtrans = {
			{
				0x00820200L, 0x00020000L, 0x80800000L, 0x80820200L, 0x00800000L, 0x80020200L, 0x80020000L, 0x80800000L,
				0x80020200L, 0x00820200L, 0x00820000L, 0x80000200L, 0x80800200L, 0x00800000L, 0x00000000L, 0x80020000L,
				0x00020000L, 0x80000000L, 0x00800200L, 0x00020200L, 0x80820200L, 0x00820000L, 0x80000200L, 0x00800200L,
				0x80000000L, 0x00000200L, 0x00020200L, 0x80820000L, 0x00000200L, 0x80800200L, 0x80820000L, 0x00000000L,
				0x00000000L, 0x80820200L, 0x00800200L, 0x80020000L, 0x00820200L, 0x00020000L, 0x80000200L, 0x00800200L,
				0x80820000L, 0x00000200L, 0x00020200L, 0x80800000L, 0x80020200L, 0x80000000L, 0x80800000L, 0x00820000L,
				0x80820200L, 0x00020200L, 0x00820000L, 0x80800200L, 0x00800000L, 0x80000200L, 0x80020000L, 0x00000000L,
				0x00020000L, 0x00800000L, 0x80800200L, 0x00820200L, 0x80000000L, 0x80820000L, 0x00000200L, 0x80020200L,
			},
			{
				0x10042004L, 0x00000000L, 0x00042000L, 0x10040000L, 0x10000004L, 0x00002004L, 0x10002000L, 0x00042000L,
				0x00002000L, 0x10040004L, 0x00000004L, 0x10002000L, 0x00040004L, 0x10042000L, 0x10040000L, 0x00000004L,
				0x00040000L, 0x10002004L, 0x10040004L, 0x00002000L, 0x00042004L, 0x10000000L, 0x00000000L, 0x00040004L,
				0x10002004L, 0x00042004L, 0x10042000L, 0x10000004L, 0x10000000L, 0x00040000L, 0x00002004L, 0x10042004L,
				0x00040004L, 0x10042000L, 0x10002000L, 0x00042004L, 0x10042004L, 0x00040004L, 0x10000004L, 0x00000000L,
				0x10000000L, 0x00002004L, 0x00040000L, 0x10040004L, 0x00002000L, 0x10000000L, 0x00042004L, 0x10002004L,
				0x10042000L, 0x00002000L, 0x00000000L, 0x10000004L, 0x00000004L, 0x10042004L, 0x00042000L, 0x10040000L,
				0x10040004L, 0x00040000L, 0x00002004L, 0x10002000L, 0x10002004L, 0x00000004L, 0x10040000L, 0x00042000L,
			},
			{
				0x41000000L, 0x01010040L, 0x00000040L, 0x41000040L, 0x40010000L, 0x01000000L, 0x41000040L, 0x00010040L,
				0x01000040L, 0x00010000L, 0x01010000L, 0x40000000L, 0x41010040L, 0x40000040L, 0x40000000L, 0x41010000L,
				0x00000000L, 0x40010000L, 0x01010040L, 0x00000040L, 0x40000040L, 0x41010040L, 0x00010000L, 0x41000000L,
				0x41010000L, 0x01000040L, 0x40010040L, 0x01010000L, 0x00010040L, 0x00000000L, 0x01000000L, 0x40010040L,
				0x01010040L, 0x00000040L, 0x40000000L, 0x00010000L, 0x40000040L, 0x40010000L, 0x01010000L, 0x41000040L,
				0x00000000L, 0x01010040L, 0x00010040L, 0x41010000L, 0x40010000L, 0x01000000L, 0x41010040L, 0x40000000L,
				0x40010040L, 0x41000000L, 0x01000000L, 0x41010040L, 0x00010000L, 0x01000040L, 0x41000040L, 0x00010040L,
				0x01000040L, 0x00000000L, 0x41010000L, 0x40000040L, 0x41000000L, 0x40010040L, 0x00000040L, 0x01010000L,
			},
			{
				0x00100402L, 0x04000400L, 0x00000002L, 0x04100402L, 0x00000000L, 0x04100000L, 0x04000402L, 0x00100002L,
				0x04100400L, 0x04000002L, 0x04000000L, 0x00000402L, 0x04000002L, 0x00100402L, 0x00100000L, 0x04000000L,
				0x04100002L, 0x00100400L, 0x00000400L, 0x00000002L, 0x00100400L, 0x04000402L, 0x04100000L, 0x00000400L,
				0x00000402L, 0x00000000L, 0x00100002L, 0x04100400L, 0x04000400L, 0x04100002L, 0x04100402L, 0x00100000L,
				0x04100002L, 0x00000402L, 0x00100000L, 0x04000002L, 0x00100400L, 0x04000400L, 0x00000002L, 0x04100000L,
				0x04000402L, 0x00000000L, 0x00000400L, 0x00100002L, 0x00000000L, 0x04100002L, 0x04100400L, 0x00000400L,
				0x04000000L, 0x04100402L, 0x00100402L, 0x00100000L, 0x04100402L, 0x00000002L, 0x04000400L, 0x00100402L,
				0x00100002L, 0x00100400L, 0x04100000L, 0x04000402L, 0x00000402L, 0x04000000L, 0x04000002L, 0x04100400L,
			},
			{
				0x02000000L, 0x00004000L, 0x00000100L, 0x02004108L, 0x02004008L, 0x02000100L, 0x00004108L, 0x02004000L,
				0x00004000L, 0x00000008L, 0x02000008L, 0x00004100L, 0x02000108L, 0x02004008L, 0x02004100L, 0x00000000L,
				0x00004100L, 0x02000000L, 0x00004008L, 0x00000108L, 0x02000100L, 0x00004108L, 0x00000000L, 0x02000008L,
				0x00000008L, 0x02000108L, 0x02004108L, 0x00004008L, 0x02004000L, 0x00000100L, 0x00000108L, 0x02004100L,
				0x02004100L, 0x02000108L, 0x00004008L, 0x02004000L, 0x00004000L, 0x00000008L, 0x02000008L, 0x02000100L,
				0x02000000L, 0x00004100L, 0x02004108L, 0x00000000L, 0x00004108L, 0x02000000L, 0x00000100L, 0x00004008L,
				0x02000108L, 0x00000100L, 0x00000000L, 0x02004108L, 0x02004008L, 0x02004100L, 0x00000108L, 0x00004000L,
				0x00004100L, 0x02004008L, 0x02000100L, 0x00000108L, 0x00000008L, 0x00004108L, 0x02004000L, 0x02000008L,
			},
			{
				0x20000010L, 0x00080010L, 0x00000000L, 0x20080800L, 0x00080010L, 0x00000800L, 0x20000810L, 0x00080000L,
				0x00000810L, 0x20080810L, 0x00080800L, 0x20000000L, 0x20000800L, 0x20000010L, 0x20080000L, 0x00080810L,
				0x00080000L, 0x20000810L, 0x20080010L, 0x00000000L, 0x00000800L, 0x00000010L, 0x20080800L, 0x20080010L,
				0x20080810L, 0x20080000L, 0x20000000L, 0x00000810L, 0x00000010L, 0x00080800L, 0x00080810L, 0x20000800L,
				0x00000810L, 0x20000000L, 0x20000800L, 0x00080810L, 0x20080800L, 0x00080010L, 0x00000000L, 0x20000800L,
				0x20000000L, 0x00000800L, 0x20080010L, 0x00080000L, 0x00080010L, 0x20080810L, 0x00080800L, 0x00000010L,
				0x20080810L, 0x00080800L, 0x00080000L, 0x20000810L, 0x20000010L, 0x20080000L, 0x00080810L, 0x00000000L,
				0x00000800L, 0x20000010L, 0x20000810L, 0x20080800L, 0x20080000L, 0x00000810L, 0x00000010L, 0x20080010L,
			},
			{
				0x00001000L, 0x00000080L, 0x00400080L, 0x00400001L, 0x00401081L, 0x00001001L, 0x00001080L, 0x00000000L,
				0x00400000L, 0x00400081L, 0x00000081L, 0x00401000L, 0x00000001L, 0x00401080L, 0x00401000L, 0x00000081L,
				0x00400081L, 0x00001000L, 0x00001001L, 0x00401081L, 0x00000000L, 0x00400080L, 0x00400001L, 0x00001080L,
				0x00401001L, 0x00001081L, 0x00401080L, 0x00000001L, 0x00001081L, 0x00401001L, 0x00000080L, 0x00400000L,
				0x00001081L, 0x00401000L, 0x00401001L, 0x00000081L, 0x00001000L, 0x00000080L, 0x00400000L, 0x00401001L,
				0x00400081L, 0x00001081L, 0x00001080L, 0x00000000L, 0x00000080L, 0x00400001L, 0x00000001L, 0x00400080L,
				0x00000000L, 0x00400081L, 0x00400080L, 0x00001080L, 0x00000081L, 0x00001000L, 0x00401081L, 0x00400000L,
				0x00401080L, 0x00000001L, 0x00001001L, 0x00401081L, 0x00400001L, 0x00401080L, 0x00401000L, 0x00001001L,
			},
			{
				0x08200020L, 0x08208000L, 0x00008020L, 0x00000000L, 0x08008000L, 0x00200020L, 0x08200000L, 0x08208020L,
				0x00000020L, 0x08000000L, 0x00208000L, 0x00008020L, 0x00208020L, 0x08008020L, 0x08000020L, 0x08200000L,
				0x00008000L, 0x00208020L, 0x00200020L, 0x08008000L, 0x08208020L, 0x08000020L, 0x00000000L, 0x00208000L,
				0x08000000L, 0x00200000L, 0x08008020L, 0x08200020L, 0x00200000L, 0x00008000L, 0x08208000L, 0x00000020L,
				0x00200000L, 0x00008000L, 0x08000020L, 0x08208020L, 0x00008020L, 0x08000000L, 0x00000000L, 0x00208000L,
				0x08200020L, 0x08008020L, 0x08008000L, 0x00200020L, 0x08208000L, 0x00000020L, 0x00200020L, 0x08008000L,
				0x08208020L, 0x00200000L, 0x08200000L, 0x08000020L, 0x00208000L, 0x00008020L, 0x08008020L, 0x08200000L,
				0x00000020L, 0x08208000L, 0x00208020L, 0x00000000L, 0x08000000L, 0x08200020L, 0x00008000L, 0x00208020L,
			}
		};
	private static final long[][] skb = {
			{
				0x00000000L, 0x00000010L, 0x20000000L, 0x20000010L, 0x00010000L, 0x00010010L, 0x20010000L, 0x20010010L,
				0x00000800L, 0x00000810L, 0x20000800L, 0x20000810L, 0x00010800L, 0x00010810L, 0x20010800L, 0x20010810L,
				0x00000020L, 0x00000030L, 0x20000020L, 0x20000030L, 0x00010020L, 0x00010030L, 0x20010020L, 0x20010030L,
				0x00000820L, 0x00000830L, 0x20000820L, 0x20000830L, 0x00010820L, 0x00010830L, 0x20010820L, 0x20010830L,
				0x00080000L, 0x00080010L, 0x20080000L, 0x20080010L, 0x00090000L, 0x00090010L, 0x20090000L, 0x20090010L,
				0x00080800L, 0x00080810L, 0x20080800L, 0x20080810L, 0x00090800L, 0x00090810L, 0x20090800L, 0x20090810L,
				0x00080020L, 0x00080030L, 0x20080020L, 0x20080030L, 0x00090020L, 0x00090030L, 0x20090020L, 0x20090030L,
				0x00080820L, 0x00080830L, 0x20080820L, 0x20080830L, 0x00090820L, 0x00090830L, 0x20090820L, 0x20090830L,
			},
			{
				0x00000000L, 0x02000000L, 0x00002000L, 0x02002000L, 0x00200000L, 0x02200000L, 0x00202000L, 0x02202000L,
				0x00000004L, 0x02000004L, 0x00002004L, 0x02002004L, 0x00200004L, 0x02200004L, 0x00202004L, 0x02202004L,
				0x00000400L, 0x02000400L, 0x00002400L, 0x02002400L, 0x00200400L, 0x02200400L, 0x00202400L, 0x02202400L,
				0x00000404L, 0x02000404L, 0x00002404L, 0x02002404L, 0x00200404L, 0x02200404L, 0x00202404L, 0x02202404L,
				0x10000000L, 0x12000000L, 0x10002000L, 0x12002000L, 0x10200000L, 0x12200000L, 0x10202000L, 0x12202000L,
				0x10000004L, 0x12000004L, 0x10002004L, 0x12002004L, 0x10200004L, 0x12200004L, 0x10202004L, 0x12202004L,
				0x10000400L, 0x12000400L, 0x10002400L, 0x12002400L, 0x10200400L, 0x12200400L, 0x10202400L, 0x12202400L,
				0x10000404L, 0x12000404L, 0x10002404L, 0x12002404L, 0x10200404L, 0x12200404L, 0x10202404L, 0x12202404L,
			},
			{
				0x00000000L, 0x00000001L, 0x00040000L, 0x00040001L, 0x01000000L, 0x01000001L, 0x01040000L, 0x01040001L,
				0x00000002L, 0x00000003L, 0x00040002L, 0x00040003L, 0x01000002L, 0x01000003L, 0x01040002L, 0x01040003L,
				0x00000200L, 0x00000201L, 0x00040200L, 0x00040201L, 0x01000200L, 0x01000201L, 0x01040200L, 0x01040201L,
				0x00000202L, 0x00000203L, 0x00040202L, 0x00040203L, 0x01000202L, 0x01000203L, 0x01040202L, 0x01040203L,
				0x08000000L, 0x08000001L, 0x08040000L, 0x08040001L, 0x09000000L, 0x09000001L, 0x09040000L, 0x09040001L,
				0x08000002L, 0x08000003L, 0x08040002L, 0x08040003L, 0x09000002L, 0x09000003L, 0x09040002L, 0x09040003L,
				0x08000200L, 0x08000201L, 0x08040200L, 0x08040201L, 0x09000200L, 0x09000201L, 0x09040200L, 0x09040201L,
				0x08000202L, 0x08000203L, 0x08040202L, 0x08040203L, 0x09000202L, 0x09000203L, 0x09040202L, 0x09040203L,
			},
			{
				0x00000000L, 0x00100000L, 0x00000100L, 0x00100100L, 0x00000008L, 0x00100008L, 0x00000108L, 0x00100108L,
				0x00001000L, 0x00101000L, 0x00001100L, 0x00101100L, 0x00001008L, 0x00101008L, 0x00001108L, 0x00101108L,
				0x04000000L, 0x04100000L, 0x04000100L, 0x04100100L, 0x04000008L, 0x04100008L, 0x04000108L, 0x04100108L,
				0x04001000L, 0x04101000L, 0x04001100L, 0x04101100L, 0x04001008L, 0x04101008L, 0x04001108L, 0x04101108L,
				0x00020000L, 0x00120000L, 0x00020100L, 0x00120100L, 0x00020008L, 0x00120008L, 0x00020108L, 0x00120108L,
				0x00021000L, 0x00121000L, 0x00021100L, 0x00121100L, 0x00021008L, 0x00121008L, 0x00021108L, 0x00121108L,
				0x04020000L, 0x04120000L, 0x04020100L, 0x04120100L, 0x04020008L, 0x04120008L, 0x04020108L, 0x04120108L,
				0x04021000L, 0x04121000L, 0x04021100L, 0x04121100L, 0x04021008L, 0x04121008L, 0x04021108L, 0x04121108L,
			},
			{
				0x00000000L, 0x10000000L, 0x00010000L, 0x10010000L, 0x00000004L, 0x10000004L, 0x00010004L, 0x10010004L,
				0x20000000L, 0x30000000L, 0x20010000L, 0x30010000L, 0x20000004L, 0x30000004L, 0x20010004L, 0x30010004L,
				0x00100000L, 0x10100000L, 0x00110000L, 0x10110000L, 0x00100004L, 0x10100004L, 0x00110004L, 0x10110004L,
				0x20100000L, 0x30100000L, 0x20110000L, 0x30110000L, 0x20100004L, 0x30100004L, 0x20110004L, 0x30110004L,
				0x00001000L, 0x10001000L, 0x00011000L, 0x10011000L, 0x00001004L, 0x10001004L, 0x00011004L, 0x10011004L,
				0x20001000L, 0x30001000L, 0x20011000L, 0x30011000L, 0x20001004L, 0x30001004L, 0x20011004L, 0x30011004L,
				0x00101000L, 0x10101000L, 0x00111000L, 0x10111000L, 0x00101004L, 0x10101004L, 0x00111004L, 0x10111004L,
				0x20101000L, 0x30101000L, 0x20111000L, 0x30111000L, 0x20101004L, 0x30101004L, 0x20111004L, 0x30111004L,
			},
			{
				0x00000000L, 0x08000000L, 0x00000008L, 0x08000008L, 0x00000400L, 0x08000400L, 0x00000408L, 0x08000408L,
				0x00020000L, 0x08020000L, 0x00020008L, 0x08020008L, 0x00020400L, 0x08020400L, 0x00020408L, 0x08020408L,
				0x00000001L, 0x08000001L, 0x00000009L, 0x08000009L, 0x00000401L, 0x08000401L, 0x00000409L, 0x08000409L,
				0x00020001L, 0x08020001L, 0x00020009L, 0x08020009L, 0x00020401L, 0x08020401L, 0x00020409L, 0x08020409L,
				0x02000000L, 0x0A000000L, 0x02000008L, 0x0A000008L, 0x02000400L, 0x0A000400L, 0x02000408L, 0x0A000408L,
				0x02020000L, 0x0A020000L, 0x02020008L, 0x0A020008L, 0x02020400L, 0x0A020400L, 0x02020408L, 0x0A020408L,
				0x02000001L, 0x0A000001L, 0x02000009L, 0x0A000009L, 0x02000401L, 0x0A000401L, 0x02000409L, 0x0A000409L,
				0x02020001L, 0x0A020001L, 0x02020009L, 0x0A020009L, 0x02020401L, 0x0A020401L, 0x02020409L, 0x0A020409L,
			},
			{
				0x00000000L, 0x00000100L, 0x00080000L, 0x00080100L, 0x01000000L, 0x01000100L, 0x01080000L, 0x01080100L,
				0x00000010L, 0x00000110L, 0x00080010L, 0x00080110L, 0x01000010L, 0x01000110L, 0x01080010L, 0x01080110L,
				0x00200000L, 0x00200100L, 0x00280000L, 0x00280100L, 0x01200000L, 0x01200100L, 0x01280000L, 0x01280100L,
				0x00200010L, 0x00200110L, 0x00280010L, 0x00280110L, 0x01200010L, 0x01200110L, 0x01280010L, 0x01280110L,
				0x00000200L, 0x00000300L, 0x00080200L, 0x00080300L, 0x01000200L, 0x01000300L, 0x01080200L, 0x01080300L,
				0x00000210L, 0x00000310L, 0x00080210L, 0x00080310L, 0x01000210L, 0x01000310L, 0x01080210L, 0x01080310L,
				0x00200200L, 0x00200300L, 0x00280200L, 0x00280300L, 0x01200200L, 0x01200300L, 0x01280200L, 0x01280300L,
				0x00200210L, 0x00200310L, 0x00280210L, 0x00280310L, 0x01200210L, 0x01200310L, 0x01280210L, 0x01280310L,
			},
			{
				0x00000000L, 0x04000000L, 0x00040000L, 0x04040000L, 0x00000002L, 0x04000002L, 0x00040002L, 0x04040002L,
				0x00002000L, 0x04002000L, 0x00042000L, 0x04042000L, 0x00002002L, 0x04002002L, 0x00042002L, 0x04042002L,
				0x00000020L, 0x04000020L, 0x00040020L, 0x04040020L, 0x00000022L, 0x04000022L, 0x00040022L, 0x04040022L,
				0x00002020L, 0x04002020L, 0x00042020L, 0x04042020L, 0x00002022L, 0x04002022L, 0x00042022L, 0x04042022L,
				0x00000800L, 0x04000800L, 0x00040800L, 0x04040800L, 0x00000802L, 0x04000802L, 0x00040802L, 0x04040802L,
				0x00002800L, 0x04002800L, 0x00042800L, 0x04042800L, 0x00002802L, 0x04002802L, 0x00042802L, 0x04042802L,
				0x00000820L, 0x04000820L, 0x00040820L, 0x04040820L, 0x00000822L, 0x04000822L, 0x00040822L, 0x04040822L,
				0x00002820L, 0x04002820L, 0x00042820L, 0x04042820L, 0x00002822L, 0x04002822L, 0x00042822L, 0x04042822L,
			}
		};
	
	private static final int DES_KEY_SZ = 8;
	
	private boolean checkKey = false;

	public Crypto() {
	}

	/**
	 * Encrypts a string using the DES algorithm.
	 *
	 * @param s the string to encrypt.
	 *
	 * @returns the encrypted string; the script is of hexadecimal
	 * string format, i.e. it contains only hexadecimal (printable)
	 * characters, or <code>null</code> if any error occurs.
	 *
	 * @see #decodeFromHexString
	 */
	public static String encodeToHexString(String s) {
		try {
			if (s.equals(""))
				return "";

			Crypto crypto = new Crypto();
			UnsignedLong[] signedKey = new UnsignedLong[32];
			crypto.keySched(defaultPrivateKey, signedKey);

			int modulo = s.length() % 8;
			int dl = ((modulo != 0) ? (8 - modulo) : 0);
			byte[] data = new byte[s.length() + dl];

			for (int i = 0; i < data.length; i++)
				data[i] = (byte)0;

			String encrypted = "";
			byte[] out = new byte[8];
			byte[] in = new byte[8];

			System.arraycopy(s.getBytes(), 0, data, 0, s.length());

			for (int i = 0; i < data.length; i += 8) {
				System.arraycopy(data, i, in, 0, 8);
				crypto.des_ecb_encrypt(in, out, signedKey, Crypto.DES_ENCRYPT);
				//encrypted += Crypto.hexString(out).substring(0, 16).toUpperCase();
				encrypted += HexUtils.toHexString(out);
			}

			return encrypted;
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	/**
	 * Decrypts a string using the DES algorithm.
	 *
	 * @param s the string to decrypt; this string must have been
	 * encoded by the <code>encodeToHexString()</code> function in
	 * order to stay meaningfull.
	 *
	 * @returns the decrypted string, or <code>null</code> if any
	 * error occurs.
	 *
	 * @see #encodeToHexString
	 */
	public static String decodeFromHexString(String s) {
		try {
			if (s.equals(""))
				return "";

			Crypto crypto = new Crypto();
			UnsignedLong[] signedKey = new UnsignedLong[32];
			crypto.keySched(defaultPrivateKey, signedKey);

			String decrypted = "";

			byte[] out = new byte[8];
			byte[] in = new byte[8];
			byte[] data = s.getBytes();
			byte[] data2 = new byte[16];

			for (int i = 0; i < data.length; i += 16) {
				System.arraycopy(data, i, data2, 0, 16);

				String tmp = new String(data2);
				in = HexUtils.fromHexString(tmp);
				crypto.des_ecb_encrypt(in, out, signedKey, Crypto.DES_DECRYPT);
				decrypted += new String(out);
			}

			int posNullChar = decrypted.indexOf(0);

			if (posNullChar != -1)
				return decrypted.substring(0, posNullChar);
			else

				return decrypted;
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	/**
	 * Encrypts a string using the DES algorithm.
	 *
	 * @param s the string to encrypt.
	 *
	 * @returns the encrypted string; the script is of hexadecimal
	 * string format, i.e. it contains only hexadecimal (printable)
	 * characters, or <code>null</code> if any error occurs.
	 *
	 * @see #decodeFromHexString
	 */
	public static String encodeToHexString3(String s) {
		try {
			if (s.equals(""))
				return "";

			Crypto crypto = new Crypto();
			UnsignedLong[] ulkey1 = new UnsignedLong[32];
			crypto.keySched(key1, ulkey1);

			UnsignedLong[] ulkey2 = new UnsignedLong[32];
			crypto.keySched(key2, ulkey2);

			UnsignedLong[] ulkey3 = new UnsignedLong[32];
			crypto.keySched(key3, ulkey3);

			int modulo = s.length() % 8;
			int dl = ((modulo != 0) ? (8 - modulo) : 0);
			byte[] data = new byte[s.length() + dl];

			for (int i = 0; i < data.length; i++)
				data[i] = (byte)0;

			String encrypted = "";
			byte[] out = new byte[8];
			byte[] in = new byte[8];

			System.arraycopy(s.getBytes(), 0, data, 0, s.length());

			for (int i = 0; i < data.length; i += 8) {
				System.arraycopy(data, i, in, 0, 8);
				crypto.des_ecb3_encrypt(in, out, ulkey1, ulkey2, ulkey3, Crypto.DES_ENCRYPT);
				//encrypted += Crypto.hexString(out).substring(0, 16).toUpperCase();
				encrypted += HexUtils.toHexString(out);
			}

			return encrypted;
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	/**
	 * Decrypts a string using the DES algorithm.
	 *
	 * @param s the string to decrypt; this string must have been
	 * encoded by the <code>encodeToHexString()</code> function in
	 * order to stay meaningfull.
	 *
	 * @returns the decrypted string, or <code>null</code> if any
	 * error occurs.
	 *
	 * @see #encodeToHexString
	 */
	public static String decodeFromHexString3(String s) {
		try {
			if (s.equals(""))
				return "";

			Crypto crypto = new Crypto();
			UnsignedLong[] ulkey1 = new UnsignedLong[32];
			crypto.keySched(key1, ulkey1);

			UnsignedLong[] ulkey2 = new UnsignedLong[32];
			crypto.keySched(key2, ulkey2);

			UnsignedLong[] ulkey3 = new UnsignedLong[32];
			crypto.keySched(key3, ulkey3);

			String decrypted = "";

			byte[] out = new byte[8];
			byte[] in = new byte[8];
			byte[] data = s.getBytes();
			byte[] data2 = new byte[16];

			for (int i = 0; i < data.length; i += 16) {
				System.arraycopy(data, i, data2, 0, 16);

				String tmp = new String(data2);
				in = HexUtils.fromHexString(tmp);
				crypto.des_ecb3_encrypt(in, out, ulkey1, ulkey2, ulkey3, Crypto.DES_DECRYPT);
				decrypted += new String(out);
			}

			int posNullChar = decrypted.indexOf(0);

			if (posNullChar != -1)
				return decrypted.substring(0, posNullChar);
			else

				return decrypted;
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	private void D_ENCRYPT(UnsignedLong Q, UnsignedLong R, int S, UnsignedLong[] ks) {
		UnsignedLong u;
		UnsignedLong v;
		UnsignedLong t;

		u = R.xor(ks[S]);
		t = R.xor(ks[S + 1]);
		t = t.shiftRight(4).or(t.shiftLeft(28));

		v = UnsignedLong.valueOf(SPtrans[1][t.and(UnsignedLong.valueOf(0x3F)).intValue()]);
		v = v.or(UnsignedLong.valueOf(SPtrans[3][t.shiftRight(8).and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[5][t.shiftRight(16).and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[7][t.shiftRight(24).and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[0][u.and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[2][u.shiftRight(8).and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[4][u.shiftRight(16).and(UnsignedLong.valueOf(0x3F)).intValue()]));
		v = v.or(UnsignedLong.valueOf(SPtrans[6][u.shiftRight(24).and(UnsignedLong.valueOf(0x3F)).intValue()]));

		//Q = Q.xor(v);
		Q.xorEqual(v);
	}

	/**
	 * Convert a buffer from big endian to little endian
	 */
	private byte[] toIntel(byte[] src) {
		byte c = src[3];
		src[3] = src[0];
		src[0] = c;

		c = src[2];
		src[2] = src[1];
		src[1] = c;

		c = src[7];
		src[7] = src[4];
		src[4] = c;

		c = src[6];
		src[6] = src[5];
		src[5] = c;

		return (src);
	}

	private void c2l(byte[] in, UnsignedLong[] ll) {
		int i;
		byte[] tmp = new byte[4];

		for (i = 0; i < 4; i++)
			tmp[i] = in[3 - i];

		ll[0] = new UnsignedLong(tmp);

		for (i = 0; i < 4; i++)
			tmp[i] = in[7 - i];

		ll[1] = new UnsignedLong(tmp);
	}

	private void l2c(UnsignedLong[] ll, byte[] out) {
		int i;
		byte[] tmp;

		tmp = ll[0].toByteArray();

		for (i = 0; i < tmp.length; i++)
			out[3 - i] = tmp[i];

		tmp = ll[1].toByteArray();

		for (i = 0; i < tmp.length; i++)
			out[7 - i] = tmp[i];
	}

	private byte[] des_ecb_encrypt(byte[] input, byte[] output, UnsignedLong[] ks, int encrypt) {
		UnsignedLong[] l = new UnsignedLong[2];

		if (encrypt == DES_DECRYPT)
			toIntel(input);

		c2l(input, l);
		des_encrypt(l, ks, encrypt);
		l2c(l, output);

		if (encrypt == DES_ENCRYPT)
			toIntel(output);

		return (output);
	}

	private byte[] des_ecb3_encrypt(byte[] input, byte[] output, UnsignedLong[] ks1, UnsignedLong[] ks2, UnsignedLong[] ks3,
	    int encrypt) {
		UnsignedLong[] ll = new UnsignedLong[2];

		if (encrypt == DES_DECRYPT)
			toIntel(input);

		c2l(input, ll);

		if (encrypt == DES_ENCRYPT)
			des_encrypt3(ll, ks1, ks2, ks3);
		else
			des_decrypt3(ll, ks1, ks2, ks3);

		l2c(ll, output);

		if (encrypt == DES_ENCRYPT)
			toIntel(output);

		return (output);
	}

	private void FP(UnsignedLong l, UnsignedLong r) {
		PERM_OP(l, r, 1, UnsignedLong.valueOf(0x55555555));
		PERM_OP(r, l, 8, UnsignedLong.valueOf(0x00FF00FF));
		PERM_OP(l, r, 2, UnsignedLong.valueOf(0x33333333));
		PERM_OP(r, l, 16, UnsignedLong.valueOf(0x0000FFFF));
		PERM_OP(l, r, 4, UnsignedLong.valueOf(0x0F0F0F0F));
	}

	private void IP(UnsignedLong l, UnsignedLong r) {
		PERM_OP(r, l, 4, UnsignedLong.valueOf(0x0F0F0F0F));
		PERM_OP(l, r, 16, UnsignedLong.valueOf(0x0000FFFF));
		PERM_OP(r, l, 2, UnsignedLong.valueOf(0x33333333));
		PERM_OP(l, r, 8, UnsignedLong.valueOf(0x00FF00FF));
		PERM_OP(r, l, 1, UnsignedLong.valueOf(0x55555555));
	}

	private void PERM_OP(UnsignedLong a, UnsignedLong b, int n, UnsignedLong m) {
		UnsignedLong t = null;

		t = a.shiftRight(n).xor(b).and(m);
		//b = b.xor(t);
		b.xorEqual(t);
		//a = a.xor(t.shiftLeft(n));
		a.xorEqual(t.shiftLeft(n));
	}

	private void HPERM_OP(UnsignedLong a, int n, UnsignedLong m) {
		UnsignedLong t = null;

		t = a.shiftLeft(16 - n).xor(a).and(m);
		//a = a.xor(t).xor(t.shiftRight(16 - n));
		a.xorEqual(t.xor(t.shiftRight(16 - n)));
	}

	private boolean checkParity(byte[] key) {
		int i;

		for (i = 0; i < DES_KEY_SZ; i++) {
			if (key[i] != oddParity[(int)key[i]])
				return false;
		}

		return true;
	}

	private void des_encrypt(UnsignedLong[] data, UnsignedLong[] ks, int encrypt) {
		UnsignedLong l;
		UnsignedLong r;
		UnsignedLong u;
		int i;

		u = data[0];
		r = data[1];

		IP(u, r);

		/* Things have been modified so that the initial rotate is
		 * done outside the loop.  This required the
		 * des_SPtrans values in sp.h to be rotated 1 bit to the right.
		 * One perl script later and things have a 5% speed up on a sparc2.
		 * Thanks to Richard Outerbridge <71755.204@CompuServe.COM>
		 * for pointing this out.
		 */
		l = r.shiftLeft(1).or(r.shiftRight(31));
		r = u.shiftLeft(1).or(u.shiftRight(31));

		/* clear the top bits on machines with 8byte longs */
		l = l.and(UnsignedLong.valueOf(0xFFFFFFFF));
		r = r.and(UnsignedLong.valueOf(0xFFFFFFFF));

		/* I don't know if it is worth the effort of loop unrolling the inner loop */
		if (encrypt == DES_ENCRYPT) {
			for (i = 0; i < 32; i += 4) {
				D_ENCRYPT(l, r, i + 0, ks);
				D_ENCRYPT(r, l, i + 2, ks);
			}
		} else {
			for (i = 30; i > 0; i -= 4) {
				D_ENCRYPT(l, r, i - 0, ks);
				D_ENCRYPT(r, l, i - 2, ks);
			}
		}

		l = l.shiftRight(1).or(l.shiftLeft(31));
		r = r.shiftRight(1).or(r.shiftLeft(31));

		/* clear the top bits on machines with 8byte longs */
		l = l.and(UnsignedLong.valueOf(0xFFFFFFFF));
		r = r.and(UnsignedLong.valueOf(0xFFFFFFFF));

		FP(r, l);

		data[0] = l;
		data[1] = r;
	}

	private void des_encrypt2(UnsignedLong[] data, UnsignedLong[] ks, int encrypt) {
		UnsignedLong l;
		UnsignedLong r;
		UnsignedLong u;
		int i;

		u = data[0];
		r = data[1];

		/* Things have been modified so that the initial rotate is
		 * done outside the loop.  This required the
		 * des_SPtrans values in sp.h to be rotated 1 bit to the right.
		 * One perl script later and things have a 5% speed up on a sparc2.
		 * Thanks to Richard Outerbridge <71755.204@CompuServe.COM>
		 * for pointing this out.
		 */
		l = r.shiftLeft(1).or(r.shiftRight(31));
		r = u.shiftLeft(1).or(u.shiftRight(31));

		/* clear the top bits on machines with 8byte longs */
		l = l.and(UnsignedLong.valueOf(0xFFFFFFFF));
		r = r.and(UnsignedLong.valueOf(0xFFFFFFFF));

		/* I don't know if it is worth the effort of loop unrolling the inner loop */
		if (encrypt == DES_ENCRYPT) {
			for (i = 0; i < 32; i += 4) {
				D_ENCRYPT(l, r, i + 0, ks);
				D_ENCRYPT(r, l, i + 2, ks);
			}
		} else {
			for (i = 30; i > 0; i -= 4) {
				D_ENCRYPT(l, r, i - 0, ks);
				D_ENCRYPT(r, l, i - 2, ks);
			}
		}

		l = l.shiftRight(1).or(l.shiftLeft(31));
		r = r.shiftRight(1).or(r.shiftLeft(31));

		/* clear the top bits on machines with 8byte longs */
		l = l.and(UnsignedLong.valueOf(0xFFFFFFFF));
		r = r.and(UnsignedLong.valueOf(0xFFFFFFFF));

		data[0] = l;
		data[1] = r;
	}

	private void des_encrypt3(UnsignedLong[] data, UnsignedLong[] ks1, UnsignedLong[] ks2, UnsignedLong[] ks3) {
		UnsignedLong l;
		UnsignedLong r;

		l = data[0];
		r = data[1];
		IP(l, r);
		data[0] = l;
		data[1] = r;
		des_encrypt2(data, ks1, DES_ENCRYPT);
		des_encrypt2(data, ks2, DES_DECRYPT);
		des_encrypt2(data, ks3, DES_ENCRYPT);
		l = data[0];
		r = data[1];
		FP(r, l);
		data[0] = l;
		data[1] = r;
	}

	private void des_decrypt3(UnsignedLong[] data, UnsignedLong[] ks1, UnsignedLong[] ks2, UnsignedLong[] ks3) {
		UnsignedLong l;
		UnsignedLong r;

		l = data[0];
		r = data[1];
		IP(l, r);
		data[0] = l;
		data[1] = r;
		des_encrypt2(data, ks3, DES_DECRYPT);
		des_encrypt2(data, ks2, DES_ENCRYPT);
		des_encrypt2(data, ks1, DES_DECRYPT);
		l = data[0];
		r = data[1];
		FP(r, l);
		data[0] = l;
		data[1] = r;
	}

	private boolean isWeakKey(byte[] key) {
		for (int i = 0; i < NUM_WEAK_KEY; i++)
			for (int j = 0; j < key.length; j++)
				if (weakKeys[i][j] == key[j])
					return (true);

		return (false);
	}

	private int keySched(byte[] key, UnsignedLong[] schedule) {
		return (setKey(key, schedule));
	}

	private int setKey(byte[] key, UnsignedLong[] schedule) {
		UnsignedLong c;
		UnsignedLong d;
		UnsignedLong s;
		UnsignedLong t;
		int i;
		int k;
		UnsignedLong[] ll = new UnsignedLong[2];

		if (checkKey) {
			if (!checkParity(key))
				return (-1);

			if (isWeakKey(key))
				return (-2);
		}

		c2l(key, ll);
		c = ll[0];
		d = ll[1];

		// I now do it in 47 simple operations :-)
		// Thanks to John Fletcher (john_fletcher@lccmail.ocf.llnl.gov)
		// for the inspiration. :-)
		PERM_OP(d, c, 4, UnsignedLong.valueOf(0x0F0F0F0F));
		HPERM_OP(c, -2, UnsignedLong.valueOf(0xCCCC0000));
		HPERM_OP(d, -2, UnsignedLong.valueOf(0xCCCC0000));
		PERM_OP(d, c, 1, UnsignedLong.valueOf(0x55555555));
		PERM_OP(c, d, 8, UnsignedLong.valueOf(0x00FF00FF));
		PERM_OP(d, c, 1, UnsignedLong.valueOf(0x55555555));

		t = d.and(UnsignedLong.valueOf(0x000000FF)).shiftLeft(16);
		t = t.or(d.and(UnsignedLong.valueOf(0x0000FF00)));
		t = t.or(d.and(UnsignedLong.valueOf(0x00FF0000)).shiftRight(16));
		d = t.or(c.and(UnsignedLong.valueOf(0xF0000000)).shiftRight(4));
		c = c.and(UnsignedLong.valueOf(0x0FFFFFFF));

		for (k = 0, i = 0; i < ITERATIONS; i++) {
			if (shifts2[i] != 0) {
				c = c.shiftRight(2).or(c.shiftLeft(26));
				d = d.shiftRight(2).or(d.shiftLeft(26));
			} else {
				c = c.shiftRight(1).or(c.shiftLeft(27));
				d = d.shiftRight(1).or(d.shiftLeft(27));
			}

			c = c.and(UnsignedLong.valueOf(0x0FFFFFFF));
			d = d.and(UnsignedLong.valueOf(0x0FFFFFFF));

			// could be a few less shifts but I am to lazy
			// at this point in time to investigate
			s = UnsignedLong.valueOf(skb[0][c.and(UnsignedLong.valueOf(0x3F)).intValue()]);
			s = s.or(UnsignedLong.valueOf(
				        skb[1][c.shiftRight(6).and(UnsignedLong.valueOf(0x03))
				                .or(c.shiftRight(7).and(UnsignedLong.valueOf(0x3C))).intValue()]));
			s = s.or(UnsignedLong.valueOf(
				        skb[2][c.shiftRight(13).and(UnsignedLong.valueOf(0x0F))
				                .or(c.shiftRight(14).and(UnsignedLong.valueOf(0x30))).intValue()]));
			s = s.or(UnsignedLong.valueOf(
				        skb[3][c.shiftRight(20).and(UnsignedLong.valueOf(0x01))
				                .or(c.shiftRight(21).and(UnsignedLong.valueOf(0x06)))
				                .or(c.shiftRight(22).and(UnsignedLong.valueOf(0x38))).intValue()]));

			t = UnsignedLong.valueOf(skb[4][d.and(UnsignedLong.valueOf(0x3F)).intValue()]);
			t = t.or(UnsignedLong.valueOf(
				        skb[5][d.shiftRight(7).and(UnsignedLong.valueOf(0x03))
				                .or(d.shiftRight(8).and(UnsignedLong.valueOf(0x3C))).intValue()]));
			t = t.or(UnsignedLong.valueOf(skb[6][d.shiftRight(15).and(UnsignedLong.valueOf(0x3F)).intValue()]));
			t = t.or(UnsignedLong.valueOf(
				        skb[7][d.shiftRight(21).and(UnsignedLong.valueOf(0x0F))
				                .or(d.shiftRight(22).and(UnsignedLong.valueOf(0x30))).intValue()]));

			// table contained 0213 4657
			schedule[k++] = t.shiftLeft(16).or(s.and(UnsignedLong.valueOf(0x0000FFFF))).and(UnsignedLong.valueOf(0xFFFFFFFF));
			s = s.shiftRight(16).or(t.and(UnsignedLong.valueOf(0xFFFF0000)));

			s = s.shiftLeft(4).or(s.shiftRight(28));
			schedule[k++] = s.and(UnsignedLong.valueOf(0xFFFFFFFF));
		}

		return (0);
	}
}
