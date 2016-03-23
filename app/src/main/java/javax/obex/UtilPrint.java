package javax.obex;

import android.util.Log;

/**
 * Created by lcom75 on 23/3/16.
 */
public class UtilPrint {
    private static final String TAG = "UtilPrint";

    public static void print(byte[] bytes) {
        Log.d(TAG, "bytes lendth:" + bytes.length);
        int length = (bytes.length > 37 ? 37 : bytes.length);
        for (int i = 0; i < length; i++) {
            Log.d(TAG, "byte [" + i + "]" + bytes[i]);
        }
    }
}
