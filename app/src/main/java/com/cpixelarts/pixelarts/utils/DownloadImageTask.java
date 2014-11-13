package com.cpixelarts.pixelarts.utils;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.cpixelarts.pixelarts.GalleryActivity;

/**
 * Created by vincent on 03/11/14.
 */
public class DownloadImageTask extends AsyncTask<Integer, Void, Bitmap> {
    ImageView bmImage;
    int imageId;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(Integer... ids) {
        imageId = ids[0];
        int size = 128;
        String url = GalleryActivity.BASE_URL + "/gallery/drawing-" + imageId + "_" + size + ".png";

        Bitmap bitmap = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            //Log.e("DownloadImageTask", e.getMessage());
            //e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    protected void onPostExecute(Bitmap result) {
        if (result == null)
            return;

        bmImage.setImageBitmap(result);
        ImageManager.getInstance().addBitmapToMemoryCache(imageId, result);
    }
}
