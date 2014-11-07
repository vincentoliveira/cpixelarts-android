package com.cpixelarts.pixelarts.storage;

import android.content.Context;
import android.util.Log;

import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.utils.PixelArtParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vincent on 06/11/14.
 */
public class PixelArtStorage {
    public static final String FILENAME = "cpixelarts_{id}.json";
    public static final String FILENAME_PATTERN = ".*/cpixelarts_[\\d]+\\.json$";
    public static final String ENCODING = "UTF-8";

    protected static File getDirectory(Context context) {
        return context.getCacheDir();
    }

    protected static File getFile(Context context, int pixelArtID) {
        String filename = FILENAME.replace("{id}", Integer.toString(pixelArtID));
        return new File(getDirectory(context), filename);
    }

    /**
     * Store gallery json
     *
     * @param context
     * @param id
     * @param jsonResults
     * @return
     */
    public static boolean storePixelArt(Context context, int id, String jsonResults) {
        try {
            File file = getFile(context, id);
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
     * @param id
     * @return PixelArt
     */
    public static PixelArt loadPixelArt(Context context, int id) {
        try {
            File file = getFile(context, id);
            if (!file.exists()) {
                return null;
            }

            String json = readFile(file);
            return PixelArtParser.parsePixelArt(json);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    private static String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, ENCODING));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null ) {
            sb.append(line);
        }

        return sb.toString();
    }

    public static List<PixelArt> loadRecentPixelArts(Context context) {
        List<PixelArt> pixelArts = new ArrayList<PixelArt>();

        File directory = getDirectory(context);
        File[] files = directory.listFiles();
        for (File inFile : files) {
            if (inFile.isFile() && inFile.getAbsolutePath().matches(FILENAME_PATTERN)) {
                try {
                    String json = readFile(inFile);
                    PixelArt pixelArt = PixelArtParser.parsePixelArt(json);
                    if (pixelArt != null) {
                        pixelArts.add(pixelArt);
                    }
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        return pixelArts;
    }
}