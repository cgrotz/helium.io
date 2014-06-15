package io.helium.common;

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Created by Christoph Grotz on 15.06.14.
 */
public class PasswordHelper {
    private static final MessageDigest md;
    static {
        try {
            md = MessageDigest.getInstance("SHA-512");
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String encode(String password) {
        try {
            byte[] encoded = md.digest(password.getBytes("UTF-8"));
            return convertToHex(encoded);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean comparePassword(String encodedPassword, String password) {
        if(Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(encodedPassword)) {
            throw new IllegalStateException("password cannot be null");
        }
        return encodedPassword.equals(encode(password));
    }

    private static String convertToHex(byte[] data)
    {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < data.length; i++)
        {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do
            {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            }
            while(two_halfs++ < 1);
        }
        return buf.toString();
    }
}
