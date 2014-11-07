package com.cpixelarts.pixelarts.model;

import java.util.Date;

/**
 * Created by vincent on 03/11/14.
 */
public class PixelArt {
    public int id = -1;
    public String title;
    public String titleCanonical;
    public int width;
    public int height;
    public Date createdAt;
    public boolean locked;
    public int[] pixels;
}
