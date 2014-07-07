package io.helium.common;

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 *
 * Helper for hashing passwords
 *
 * Created by Christoph Grotz on 15.06.14.
 */
public class PasswordHelper {

    private static PasswordHelper instance = null;
    public static PasswordHelper get() {
        if ( instance == null ) {
            instance = new PasswordHelper();
        }
        return instance;
    }

    private final MessageDigest md;
    private PasswordHelper() {
        try {
            md = MessageDigest.getInstance("SHA-512");
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }


    public String encode(String password) {
        try {
            byte[] encoded = md.digest(password.getBytes("UTF-8"));
            return convertToHex(encoded);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean comparePassword(String encodedPassword, String password) {
        if(Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(encodedPassword)) {
            throw new IllegalStateException("password cannot be null");
        }
        return encodedPassword.equals(encode(password));
    }

    private String convertToHex(byte[] data)
    {
        StringBuilder buf = new StringBuilder();
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = aData & 0x0F;
            }
            while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
