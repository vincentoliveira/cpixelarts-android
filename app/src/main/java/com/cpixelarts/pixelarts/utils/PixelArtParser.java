package com.cpixelarts.pixelarts.utils;

/**
 * Created by vincent on 03/11/14.
 */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.cpixelarts.pixelarts.model.PixelArt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PixelArtParser {
    public static int count = 0;

    public static List<PixelArt> parseList(String jsonStr) {
        List<PixelArt> list = new ArrayList<PixelArt>();

        // try parse the string to a JSON object
        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONArray jsonList = json.getJSONArray("results");

            for (int i = 0; i < jsonList.length(); i++) {
                PixelArt pixelArt = parsePixelArtsJson(jsonList.getJSONObject(i));
                if (pixelArt != null) {
                    list.add(pixelArt);
                }
            }
            count = json.getInt("count");
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing pixel art list  " + e.toString());
        }

        return list;
    }


    public static PixelArt parsePixelArt(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            return parsePixelArtsJson(json.getJSONObject("drawing"));
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing pixel art " + e.toString());
            return null;
        }
    }

    public static PixelArt parsePixelArtsJson(JSONObject json) {
        PixelArt pixelArt = new PixelArt();
        try {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

            pixelArt.id = json.getInt("id");
            pixelArt.title = json.isNull("title") ? null : json.getString("title");
            pixelArt.titleCanonical = json.isNull("title_canonical") ? null : json.getString("title_canonical");
            pixelArt.width = json.getInt("width");
            pixelArt.height = json.getInt("height");
            pixelArt.locked = json.getBoolean("is_locked");
            pixelArt.createdAt = formatter.parse(json.getString("created_at"));

            if (json.has("pixels")) {
                pixelArt.pixels = new int[pixelArt.width * pixelArt.height];
                Arrays.fill(pixelArt.pixels, -1);
                JSONArray jsonPixels = json.getJSONArray("pixels");
                for (int i = 0; i < jsonPixels.length(); i++) {
                    JSONObject jsonPixel = jsonPixels.getJSONObject(i);
                    int position = jsonPixel.getInt("position");
                    int color = jsonPixel.getInt("color");
                    pixelArt.pixels[position] = color;
                }
            }

            return pixelArt;
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing pixel art " + e.toString());
            return null;
        } catch (ParseException e) {
            Log.e("JSON Parser", "Error parsing pixel art " + e.toString());
            return null;
        }
    }

}