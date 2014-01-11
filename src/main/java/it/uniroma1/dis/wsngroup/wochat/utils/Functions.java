package it.uniroma1.dis.wsngroup.wochat.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.log4j.Logger;

public class Functions {
	
	private static final Logger logger = Logger.getLogger(Functions.class);
	
	public static String byteArrayToHex (byte buf[]) {
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		for (int i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}
		return strbuf.toString();
	}
	
	public static String hashCode(String stringToEncrypt) {
		String encryptedString = "";
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] hash = messageDigest.digest(stringToEncrypt.getBytes("UTF-8"));
			encryptedString = byteArrayToHex(hash);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			logger.warn(e.getMessage(), e);
			encryptedString = stringToEncrypt;
		}
		return encryptedString;
	}
	
	public static int randInt(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt(max - min) + min;
	    return randomNum;
	}
	
	public static String readFile(String path, Charset encoding) throws Exception {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
	
}
