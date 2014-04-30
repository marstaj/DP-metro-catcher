package cz.marstaj.metrocatcher.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by mastajner on 01/04/14.
 */
public class Util {

    public static String readFromAssets(Context context, String path) throws IOException {
        StringBuilder buf = new StringBuilder();
        InputStream stream = context.getAssets().open(path);
        BufferedReader in =
                new BufferedReader(new InputStreamReader(stream));
        String str;
        while ((str = in.readLine()) != null) {
            buf.append(str);
        }
        in.close();
        return buf.toString();
    }
}
