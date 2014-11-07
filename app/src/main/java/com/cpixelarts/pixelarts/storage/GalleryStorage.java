package com.cpixelarts.pixelarts.storage;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by vincent on 06/11/14.
 */
public class GalleryStorage {
    public static final String FILENAME = "cpixelarts_gallery.json";
    public static final String ENCODING = "UTF-8";

    protected static File getFile(Context context) {
        return new File(context.getCacheDir(), FILENAME);
    }
    /**
     * Store gallery json
     *
     * @param context
     * @param jsonResults
     * @return
     */
    public static boolean storeGallery(Context context, String jsonResults) {
        try {
            File file = getFile(context);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonResults.getBytes(ENCODING));
            fos.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    /**
     * Load gallery json
     *
     * @param context
     * @return
     */
    public static String loadGallery(Context context) {
        try {
            File file = getFile(context);
            if (!file.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader( new InputStreamReader(fis, ENCODING));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null ) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }
}