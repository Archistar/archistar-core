package at.archistar.bft.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestHelper {
	
	public static MessageDigest createMd() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
			return null;
		}
	}
}
