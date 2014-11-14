package com.cpixelarts.pixelarts.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cpixelarts.pixelarts.R;
import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.utils.ImageManager;

import java.util.List;

/**
 * Created by vincent on 03/11/14.
 */
public class PixelArtAdapter extends BaseAdapter {
    private Context context;
    private List<PixelArt> list;

    public PixelArtAdapter(Context context, List<PixelArt> list) {
        this.context = context;
        this.list = list;
    }

    public int getCount() {
        return list.size();
    }

    public PixelArt getItem(int position) {
        return list.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.pixel_art_item, null);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        ImageView lockImageView = (ImageView) convertView.findViewById(R.id.lockImageView);
        TextView textView = (TextView) convertView.findViewById(R.id.textView);

        PixelArt pixelArt = this.getItem(position);
        if (pixelArt.title != null) {
            textView.setText(pixelArt.title);
        } else if (pixelArt.id > 0) {
            textView.setText("#" + pixelArt.id);
        }

        if (pixelArt.locked) {
            lockImageView.setVisibility(View.VISIBLE);
        } else {
            lockImageView.setVisibility(View.INVISIBLE);
        }

        ImageManager.getInstance().loadBitmap(pixelArt.id, imageView);

        return convertView;
    }
}
