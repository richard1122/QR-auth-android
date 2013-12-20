package com.richard1993.qrauth.android;

/**
 Copyright (c) 2011 IETF Trust and the persons identified as
 authors of the code. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, is permitted pursuant to, and subject to the license
 terms contained in, the Simplified BSD License set forth in Section
 4.c of the IETF Trust's Legal Provisions Relating to IETF Documents
 (http://trustee.ietf.org/license-info).
 */
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

/**
 * This is an example implementation of the OATH TOTP algorithm. Visit
 * www.openauthentication.org for more information.
 * 
 * @author Johan Rydell, PortWise, Inc.
 */
public class TOTP {
	private static final int[] DIGITS_POWER
	// 0 1 2 3 4 5 6 7 8
	= { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000 };

	private TOTP() {
	}

	/**
	 * This method uses the JCE to provide the crypto algorithm. HMAC computes a
	 * Hashed Message Authentication Code with the crypto hash algorithm as a
	 * parameter.
	 * 
	 * @param crypto
	 *            : the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
	 * @param keyBytes
	 *            : the bytes to use for the HMAC key
	 * @param text
	 *            : the message or text to be authenticated
	 */
	public static byte[] hmac_sha(String crypto, byte[] keyBytes, byte[] text) {
		try {
			Mac hmac;
			hmac = Mac.getInstance(crypto);

			SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
			hmac.init(macKey);

			return hmac.doFinal(text);
		} catch (GeneralSecurityException gse) {
			throw new UndeclaredThrowableException(gse);
		}
	}
	
	
	/**
	 * ʹ��SHA512�㷨����ժҪ
	 * @param data
	 * @return
	 */
	public static String sha512(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(data);
			byte[] message = md.digest();

			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < message.length; i++) {
				stringBuffer.append(Integer.toString(
						(message[i] & 0xff) + 0x100, 16).substring(1));
			}
			return stringBuffer.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This method converts a HEX string to Byte[]
	 * 
	 * @param hex
	 *            : the HEX string
	 * 
	 * @return: a byte array
	 */
	private static byte[] hexStr2Bytes(String hex) {
		// Adding one byte to get the right conversion
		// Values starting with "0" can be converted
		byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

		// Copy all the REAL bytes, not the "first"
		byte[] ret = new byte[bArray.length - 1];

		for (int i = 0; i < ret.length; i++)
			ret[i] = bArray[i + 1];

		return ret;
	}

	/**
	 * This method generates a TOTP value for the given set of parameters.
	 * 
	 * @param key
	 *            : the shared secret, HEX encoded
	 * @param time
	 *            : a value that reflects a time
	 * @param returnDigits
	 *            : number of digits to return
	 * 
	 * @return: a numeric String in base 10 that includes
	 *          {@link truncationDigits} digits
	 */
	public static String generateTOTP(String key, String time,
			String returnDigits) {
		return generateTOTP(key, time, returnDigits, "HmacSHA1");
	}

	/**
	 * This method generates a TOTP value for the given set of parameters.
	 * 
	 * @param key
	 *            : the shared secret, HEX encoded
	 * @param time
	 *            : a value that reflects a time
	 * @param returnDigits
	 *            : number of digits to return
	 * 
	 * @return: a numeric String in base 10 that includes
	 *          {@link truncationDigits} digits
	 */
	public static String generateTOTP256(String key, String time,
			String returnDigits) {
		return generateTOTP(key, time, returnDigits, "HmacSHA256");
	}

	/**
	 * This method generates a TOTP value for the given set of parameters.
	 * 
	 * @param key
	 *            : the shared secret, HEX encoded
	 * @param time
	 *            : a value that reflects a time
	 * @param returnDigits
	 *            : number of digits to return
	 * 
	 * @return: a numeric String in base 10 that includes
	 *          {@link truncationDigits} digits
	 */
	public static String generateTOTP512(String key, String time,
			String returnDigits) {
		return generateTOTP(key, time, returnDigits, "HmacSHA512");
	}

	/**
	 * This method generates a TOTP value for the given set of parameters.
	 * 
	 * @param key
	 *            : the shared secret, HEX encoded
	 * @param time
	 *            : a value that reflects a time
	 * @param returnDigits
	 *            : number of digits to return
	 * @param crypto
	 *            : the crypto function to use
	 * 
	 * @return: a numeric String in base 10 that includes
	 *          {@link truncationDigits} digits
	 */
	public static String generateTOTP(String key, String time,
			String returnDigits, String crypto) {
		int codeDigits = Integer.decode(returnDigits).intValue();
		String result = null;

		// Using the counter
		// First 8 bytes are for the movingFactor
		// Compliant with base RFC 4226 (HOTP)
		while (time.length() < 16)
			time = "0" + time;

		// Get the HEX in a Byte[]
		byte[] msg = hexStr2Bytes(time);
		byte[] k = hexStr2Bytes(key);
		byte[] hash = hmac_sha(crypto, k, msg);
		
		Log.i("Keys in Bytes", new String(k));

		// put selected bytes into result int
		int offset = hash[hash.length - 1] & 0xf;

		int binary = ((hash[offset] & 0x7f) << 24)
				| ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

		int otp = binary % DIGITS_POWER[codeDigits];

		result = Integer.toString(otp);

		while (result.length() < codeDigits) {
			result = "0" + result;
		}

		return result;
	}

	public static void main(String[] args) {
		// Seed for HMAC-SHA1 - 20 bytes
		String seed = "3132333435363738393031323334353637383930";

		// Seed for HMAC-SHA256 - 32 bytes
		String seed32 = "3132333435363738393031323334353637383930"
				+ "313233343536373839303132";

		// Seed for HMAC-SHA512 - 64 bytes
		String seed64 = "3132333435363738393031323334353637383930"
				+ "3132333435363738393031323334353637383930"
				+ "3132333435363738393031323334353637383930" + "31323334";
		long T0 = 0;
		long X = 30;
		long[] testTime = { 59L, 1111111109L, 1111111111L, 1234567890L,
				2000000000L, 20000000000L };

		String steps = "0";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));

		try {
			System.out.println("+---------------+-----------------------+"
					+ "------------------+--------+--------+");
			System.out.println("|  Time(sec)    |   Time (UTC format)   "
					+ "| Value of T(Hex)  |  TOTP  | Mode   |");
			System.out.println("+---------------+-----------------------+"
					+ "------------------+--------+--------+");

			for (int i = 0; i < testTime.length; i++) {
				long T = (testTime[i] - T0) / X;
				steps = Long.toHexString(T).toUpperCase();

				while (steps.length() < 16)
					steps = "0" + steps;

				String fmtTime = String.format("%1$-11s", testTime[i]);
				String utcTime = df.format(new Date(testTime[i] * 1000));
				System.out.print("|  " + fmtTime + "  |  " + utcTime + "  | "
						+ steps + " |");
				System.out.println(generateTOTP(seed, steps, "8", "HmacSHA1")
						+ "| SHA1   |");
				System.out.print("|  " + fmtTime + "  |  " + utcTime + "  | "
						+ steps + " |");
				System.out.println(generateTOTP(seed32, steps, "8",
						"HmacSHA256") + "| SHA256 |");
				System.out.print("|  " + fmtTime + "  |  " + utcTime + "  | "
						+ steps + " |");
				System.out.println(generateTOTP(seed64, steps, "8",
						"HmacSHA512") + "| SHA512 |");

				System.out.println("+---------------+-----------------------+"
						+ "------------------+--------+--------+");
			}
		} catch (final Exception e) {
			System.out.println("Error : " + e);
		}
	}
}