package com.idevel.dailyinspection.beacon.ble;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BleUtils {
    public static final int BIG_ENDIAN = 1;
    public static final int BIT16 = 16;
    public static final int BIT32 = 32;
    public static final int BIT8 = 8;
    private static final String HEXES = "0123456789ABCDEF";
    public static final int LITTLE_ENDIAN = 2;
    public static final int MCD_RECEIVE = 1;
    public static final int MCD_SEND = 0;
    private byte[] ivect = {0, 0, 0, 0, 0, 0, 0, 0};

    public static String toHexString(byte raw) {
        StringBuilder hex = new StringBuilder(2);
        hex.append(HEXES.charAt((raw & 240) >> 4)).append(HEXES.charAt(raw & 15));
        return hex.toString();
    }

    public static String toHexString(byte[] raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder hex = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            hex.append(HEXES.charAt((b & 240) >> 4)).append(HEXES.charAt(b & 15));
        }
        return hex.toString();
    }

    public static int convertByteToInt(byte[] b) {
        int value = 0;
        for (byte b2 : b) {
            value = (value << 8) | b2;
        }
        return value;
    }

    public static int convertByteToInt(byte b) {
        return b | 0;
    }

    public static int convertByteToUnsignedInt(byte b) {
        return b & 255;
    }

    public static int convertByteToUnsignedInt(byte[] b) {
        int value = 0;
        for (byte b2 : b) {
            value = (value << 8) + (b2 & 255);
        }
        return value;
    }

    public static byte[] toBytes(String digits, int radix) throws IllegalArgumentException, NumberFormatException {
        if (digits == null) {
            return null;
        }
        if (radix == 16 || radix == 10 || radix == 8) {
            int divLen = radix == 16 ? 2 : 3;
            int length = digits.length();
            if (length % divLen == 1) {
                throw new IllegalArgumentException("For input string: \"" + digits + "\"");
            }
            int length2 = length / divLen;
            byte[] bytes = new byte[length2];
            for (int i = 0; i < length2; i++) {
                int index = i * divLen;
                bytes[i] = (byte) Short.parseShort(digits.substring(index, index + divLen), radix);
            }
            return bytes;
        }
        throw new IllegalArgumentException("For input radix: \"" + radix + "\"");
    }

    public static byte[] changeByteOrder(byte[] value, int Order) {
        if (value == null || value.length <= 0) {
            return null;
        }
        int idx = value.length;
        byte[] Temp = new byte[idx];
        if (Order == 1) {
            return value;
        }
        if (Order != 2) {
            return Temp;
        }
        for (int i = 0; i < idx; i++) {
            Temp[i] = value[idx - (i + 1)];
        }
        return Temp;
    }

    public static int toByteArrayInt(byte[] src, int nBit) {
        if (nBit == 8) {
            return (src[0] & 255) << 0;
        }
        if (nBit == 16) {
            return ((src[0] & 255) << 8) + ((src[1] & 255) << 0);
        }
        return ((src[0] & 255) << 24) + ((src[1] & 255) << 16) + ((src[2] & 255) << 8) + ((src[3] & 255) << 0);
    }

    public static byte[] reverseOrder(byte[] data) {
        if (data.length != 4) {
            throw new IllegalArgumentException("Arguement should be length=4");
        }
        return new byte[]{data[3], data[2], data[1], data[0]};
    }

    public static byte[] rol(byte[] data) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length - 1; i++) {
            out[i] = data[i + 1];
        }
        out[out.length - 1] = data[0];
        return out;
    }

    public static byte[] xor(byte[] x, byte[] y) {
        byte[] out = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            out[i] = (byte) (x[i] ^ y[i]);
        }
        return out;
    }

    public byte[] encrypt(byte[] data, byte[] keyValue, int direction) throws Exception {
        byte[] ovect = new byte[16];
        if (direction != 0) {
            ovect = Arrays.copyOf(data, data.length);
        }
        IvParameterSpec ivParameterSpec = new IvParameterSpec(this.ivect);
        SecretKey key = new SecretKeySpec(keyValue, "DESede");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(1, key, ivParameterSpec);
        byte[] cipherText = cipher.doFinal(data);
        if (direction == 0) {
            this.ivect = Arrays.copyOf(cipherText, cipherText.length);
        } else {
            this.ivect = Arrays.copyOf(ovect, ovect.length);
        }
        return cipherText;
    }

    public byte[] decrypt(byte[] data, byte[] keyValue, int direction) throws Exception {
        byte[] ovect = new byte[16];
        if (direction != 0) {
            ovect = Arrays.copyOf(data, data.length);
        }
        IvParameterSpec ivParameterSpec = new IvParameterSpec(this.ivect);
        SecretKey key = new SecretKeySpec(keyValue, "DESede");
        Cipher decipher = Cipher.getInstance("DESede/CBC/NoPadding");
        decipher.init(2, key, ivParameterSpec);
        byte[] cipherText = decipher.doFinal(data);
        if (direction == 0) {
            this.ivect = Arrays.copyOf(cipherText, cipherText.length);
        } else {
            this.ivect = Arrays.copyOf(ovect, ovect.length);
        }
        return cipherText;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, String path) {
        try {
            return rotate(bitmap, exifOrientationToDegree(new ExifInterface(path).getAttributeInt("Orientation", 1)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int exifOrientationToDegree(int orientation) {
        if (orientation == 6) {
            return 90;
        }
        if (orientation == 3) {
            return 180;
        }
        if (orientation == 8) {
            return 270;
        }
        return 0;
    }

    private static Bitmap rotate(Bitmap bitmap, int degree) {
        Bitmap original = bitmap;
        if (!(degree == 0 || bitmap == null)) {
            Matrix m = new Matrix();
            m.setRotate((float) degree, ((float) bitmap.getWidth()) / 2.0f, ((float) bitmap.getHeight()) / 2.0f);
            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            } catch (OutOfMemoryError e) {
                Log.w("junho", "Out of memory");
                return original;
            }
        }
        return bitmap;
    }

    public static Bitmap stringToBitmap(String image) {
        try {
            byte[] encodeByte = Base64.decode(image, 0);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public static int intToSignedBits(int i, int size) {
        if (i < 0) {
            return (1 << (size - 1)) + (((1 << (size - 1)) - 1) & i);
        }
        return i;
    }

    public static byte calcChecksum(byte[] bytes) {
        byte CheckSum = 0;
        for (byte b : bytes) {
            CheckSum = (byte) (CheckSum + b);
        }
        Log.d("junho", ">> con bytes = " + bytes.length);
        return CheckSum;
    }

    public static byte calcSbbleChecksum(byte[] bytes) {
        byte CheckSum = 0;
        for (byte b : bytes) {
            CheckSum = (byte) (CheckSum + b);
        }
        Log.d("junho", ">> con bytes = " + bytes.length);
        return (byte) (CheckSum ^ -1);
    }

    public static final byte[] toIntByteArray(int value, int nBit) {
        if (nBit == 8) {
            return new byte[]{(byte) value};
        } else if (nBit == 16) {
            return new byte[]{(byte) (value >>> 8), (byte) value};
        } else {
            return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        }
    }
}

