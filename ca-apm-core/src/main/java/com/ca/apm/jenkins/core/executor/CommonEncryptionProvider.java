/*
 * Copyright (c) 2018 CA. All rights reserved.
 * 
 * This software and all information contained therein is confidential and proprietary and
 * shall not be duplicated, used, disclosed or disseminated in any way except as authorized
 * by the applicable license agreement, without the express written permission of CA. All
 * authorized reproductions must be marked with this language.
 * 
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW, CA PROVIDES THIS SOFTWARE WITHOUT WARRANTY
 * OF ANY KIND, INCLUDING WITHOUT LIMITATION, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL CA BE
 * LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR DAMAGE, DIRECT OR
 * INDIRECT, FROM THE USE OF THIS SOFTWARE, INCLUDING WITHOUT LIMITATION, LOST
 * PROFITS, BUSINESS INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF CA IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
 */

package com.ca.apm.jenkins.core.executor;

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


/**
 * This object provides all the encryption / decryption support required for
 * agent as well as manager. This current implementation may be changed in
 * future as long as the public interface is not changed.
 * 
 * @author batas04
 *
 */
public class CommonEncryptionProvider
{

    protected boolean           encryptionEnabled = false;

    private static final String SECRET_KEY        = "PBEWithMD5AndDES";
    
    public static final String       APM_ENCRYPTION_PREFIX     = "ENC(";

    //
    // code characters for values 0..63
    //
    private static char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
                                           .toCharArray();

    //
    // lookup table for converting base64 characters to value in range 0..63
    //
    private static byte[] codes    = new byte[256];
    
    static
    {
        for (int i = 0; i < 256; i++)
            codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++)
            codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++)
            codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++)
            codes[i] = (byte) (52 + i - '0');
        codes['+'] = 62;
        codes['/'] = 63;
    }
    
    public CommonEncryptionProvider()
    {
        encryptionEnabled = true;

        // Validate that encryption will actually work. Sometimes
        // java.lang.SecurityException: Cannot set up certs for trusted CAs
        // will be thrown. In such cases we will not do encryption

        try
        {
            SecretKeyFactory.getInstance(SECRET_KEY);

        } catch (Exception exe)
        {

            encryptionEnabled = false;
        }
    }

    public String encrypt(String str)
        throws InvalidKeyException, NoSuchAlgorithmException,
        InvalidKeySpecException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException
    {
        if (!encryptionEnabled) return str;
        byte[] s1 = encryptLevel1(str.getBytes());
        char[] charArray = encode(s1);
        return new String(charArray);
    }

    public String decrypt(String str)
        throws InvalidKeyException, NoSuchAlgorithmException,
        InvalidKeySpecException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, BadPaddingException
    {
        if (!encryptionEnabled) return str;
        try
        {
            byte[] bytes = new byte[0];
            try {
                bytes = decode(str.toCharArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] s1 = decryptLevel1(bytes);
            return new String(s1);
        } catch (javax.crypto.IllegalBlockSizeException ibse)
        {
            // This is the case where a non encrypted String is requested to be
            // encrypted.
            return str;
        }

    }

    protected byte[] encryptLevel1(byte[] str)
        throws InvalidKeyException, NoSuchAlgorithmException,
        InvalidKeySpecException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException
    {
        return runPBECipher(true, str);
    }

    protected byte[] decryptLevel1(byte[] str)
        throws InvalidKeyException, NoSuchAlgorithmException,
        InvalidKeySpecException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException
    {
        return runPBECipher(false, str);
    }

    protected byte[] runPBECipher(boolean encrypt, byte[] str)
        throws NoSuchAlgorithmException, InvalidKeySpecException,
        NoSuchPaddingException, InvalidKeyException,
        InvalidAlgorithmParameterException, IllegalBlockSizeException,
        BadPaddingException
    {
        PBEKeySpec pbeKeySpec;
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac;

        // Salt
        byte[] salt = "CAUNCNTR".getBytes();
        // Iteration count
        int count = 20;

        // Create PBE parameter set.
        pbeParamSpec = new PBEParameterSpec(salt, count);
        pbeKeySpec = new PBEKeySpec("umj2ee".toCharArray());
        keyFac = SecretKeyFactory.getInstance(SECRET_KEY);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        // Create PBE cipher
        Cipher pbeCipher = Cipher.getInstance(SECRET_KEY);

        // Initialize PBE Cipher with key and parameters.
        if (encrypt)
        {
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        } else
        {
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        }

        return pbeCipher.doFinal(str);

    }

    /**
     * returns an array of base64-encoded characters to represent the passed
     * data array.
     * 
     * @param data
     *            the array of bytes to encode
     * @return base64-coded character array.
     */
    protected char[] encode(byte[] data)
    {
        char[] out = new char[((data.length + 2) / 3) * 4];

        //
        // 3 bytes encode to 4 chars. Output is always an even
        // multiple of 4 characters.
        //
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4)
        {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i + 1) < data.length)
            {
                val |= (0xFF & (int) data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length)
            {
                val |= (0xFF & (int) data[i + 2]);
                quad = true;
            }
            out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index + 0] = alphabet[val & 0x3F];
        }
        return out;
    }

    /**
     * Decodes a BASE-64 encoded stream to recover the original data. White
     * space before and after will be trimmed away, but no other manipulation of
     * the input will be performed.
     * 
     * As of version 1.2 this method will properly handle input containing junk
     * characters (newlines and the like) rather than throwing an error. It does
     * this by pre-parsing the input and generating from that a count of VALID
     * input characters.
     * 
     *
     * 
     * @throws Exception
     */
    protected byte[] decode(char[] data) throws Exception {
        // as our input could contain non-BASE64 data (newlines,
        // whitespace of any sort, whatever) we must first adjust
        // our count of USABLE data so that...
        // (a) we don't misallocate the output array, and
        // (b) think that we miscalculated our data length
        // just because of extraneous throw-away junk

        int tempLen = data.length;
        for (int ix = 0; ix < data.length; ix++)
        {
            if ((data[ix] > 255) || codes[data[ix]] < 0) --tempLen; // ignore
                                                                    // non-valid
                                                                    // chars and
                                                                    // padding
        }
        // calculate required length:
        // -- 3 bytes for every 4 valid base64 chars
        // -- plus 2 bytes if there are 3 extra base64 chars,
        // or plus 1 byte if there are 2 extra.

        int len = (tempLen / 4) * 3;
        if ((tempLen % 4) == 3) len += 2;
        if ((tempLen % 4) == 2) len += 1;

        byte[] out = new byte[len];

        int shift = 0; // # of excess bits stored in accum
        int accum = 0; // excess bits
        int index = 0;

        // we now go through the entire array (NOT using the 'tempLen' value)
        for (int ix = 0; ix < data.length; ix++)
        {
            int value = (data[ix] > 255) ? -1 : codes[data[ix]];

            if (value >= 0) // skip over non-code
            {
                accum <<= 6; // bits shift up by 6 each time thru
                shift += 6; // loop, with new bits being put in
                accum |= value; // at the bottom.
                if (shift >= 8) // whenever there are 8 or more shifted in,
                {
                    shift -= 8; // write them out (from the top, leaving any
                    out[index++] = // excess at the bottom for next iteration.
                    (byte) ((accum >> shift) & 0xff);
                }
            }
            /*
             * we will also have skipped processing a padding null byte ('=')
             * here; these are used ONLY for padding to an even length and do
             * not legally occur as encoded data. for this reason we can ignore
             * the fact that no index++ operation occurs in that special case:
             * the out[] array is initialized to all-zero bytes to start with
             * and that works to our advantage in this combination.
             */
        }

        // if there is STILL something wrong we just have to throw up now!
        if (index != out.length)
        {
            throw new Exception("Miscalculated data length (wrote "
                                     + index + " instead of " + out.length
                                     + ")");
        }

        return out;
    }

    public String getDecryptedPassword(String password)
        throws InvalidKeyException, NoSuchAlgorithmException,
        InvalidKeySpecException, NoSuchPaddingException,
        InvalidAlgorithmParameterException, BadPaddingException
    {
        if (password.startsWith(APM_ENCRYPTION_PREFIX))
        {
            password = decrypt(password.substring(4));
        }
        return password;
    }
}
