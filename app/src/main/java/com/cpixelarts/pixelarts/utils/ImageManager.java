package com.cpixelarts.pixelarts.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.cpixelarts.pixelarts.R;
import com.cpixelarts.pixelarts.restclient.DownloadImageTask;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by vincent on 06/11/14.
 */
public class ImageManager {

    private LruCache<Integer, Bitmap> mMemoryCache;
    private HashMap<Integer, Date> mMemoryCacheAge;
    private static ImageManager instance = null;
    private boolean init;

    private ImageManager() {
        init = false;
    }

    public static ImageManager getInstance()
    {
        if (instance == null) {
            instance = new ImageManager();
        }

        return instance;
    }

    public boolean isInit() {
        return this.init;
    }

    public void init(int cacheSize) {
        if (this.init) {
            return;
        }

        mMemoryCache = new LruCache<Integer, Bitmap>(cacheSize) {
            //@Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        mMemoryCacheAge = new HashMap<Integer, Date>();
        this.init = true;
    }

    public void addBitmapToMemoryCache(int id, Bitmap bitmap) {
        if (mMemoryCache != null && getBitmapFromMemCache(id) == null) {
            mMemoryCache.put(id, bitmap);
            if (mMemoryCacheAge != null) {
                mMemoryCacheAge.put(id, new Date());
            }
        }
    }

    public Bitmap getBitmapFromMemCache(int id) {
        if (mMemoryCache == null) {
            return null;
        }

        return mMemoryCache.get(id);
    }

    public void loadBitmap(int imageId, ImageView imageView) {
        if (imageId <= 0) {
            return;
        }

        final Bitmap bitmap = getBitmapFromMemCache(imageId);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher);
        }

        Date imageCacheAge = mMemoryCacheAge.get(imageId);
        Date tenMinutesAgo = new Date();
        tenMinutesAgo.setTime(tenMinutesAgo.getTime() - 600000);
        if (bitmap == null || imageCacheAge == null || imageCacheAge.before(tenMinutesAgo)) {
            DownloadImageTask task = new DownloadImageTask(imageView);
            task.execute(imageId);
        }
    }
}