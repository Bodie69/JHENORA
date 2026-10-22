package com.campus.campusmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies department logos are trimmed and centred so they fill a circular frame. */
@RunWith(AndroidJUnit4.class)
public class SupabaseStorageLogoTest {

    private static final int SIZE = 512;

    /** A red disc of {@code diameter} at ({@code cx}, {@code cy}) on a {@code w} x {@code h} canvas. */
    private static Bitmap disc(int w, int h, int cx, int cy, int diameter, int background) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(background);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.RED);
        c.drawCircle(cx, cy, diameter / 2f, p);
        return b;
    }

    /** Bounding box [left, top, right, bottom] of reddish pixels. */
    private static int[] redBounds(Bitmap b) {
        int left = b.getWidth(), top = b.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                int px = b.getPixel(x, y);
                if (Color.alpha(px) > 128 && Color.red(px) > 200 && Color.green(px) < 80) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        return new int[]{left, top, right, bottom};
    }

    private static void assertFillsAndCentred(Bitmap out) {
        assertEquals(SIZE, out.getWidth());
        assertEquals(SIZE, out.getHeight());
        int[] r = redBounds(out);
        // Reaches (within anti-aliasing) all four edges of the square...
        assertTrue("left " + r[0], r[0] <= 3);
        assertTrue("top " + r[1], r[1] <= 3);
        assertTrue("right " + r[2], r[2] >= SIZE - 4);
        assertTrue("bottom " + r[3], r[3] >= SIZE - 4);
        // ...and is centred.
        float cx = (r[0] + r[2]) / 2f;
        float cy = (r[1] + r[3]) / 2f;
        assertEquals(SIZE / 2f, cx, 3f);
        assertEquals(SIZE / 2f, cy, 3f);
    }

    @Test
    public void whiteMarginsAreTrimmedAndOffCentreLogoIsCentred() {
        // Small disc tucked into the top-left of a big white image.
        Bitmap src = disc(1000, 800, 250, 220, 300, Color.WHITE);
        assertFillsAndCentred(SupabaseStorage.squareLogo(src, SIZE));
    }

    @Test
    public void transparentMarginsAreTrimmedAndStayTransparent() {
        Bitmap src = disc(900, 900, 600, 640, 400, Color.TRANSPARENT);
        Bitmap out = SupabaseStorage.squareLogo(src, SIZE);
        assertFillsAndCentred(out);
        assertEquals("corner stays transparent", 0, Color.alpha(out.getPixel(0, 0)));
    }

    @Test
    public void wideLogoIsPaddedNotCropped() {
        // A 2:1 red bar on white: the whole bar must survive, centred vertically.
        Bitmap src = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(src);
        c.drawColor(Color.WHITE);
        Paint p = new Paint();
        p.setColor(Color.RED);
        c.drawRect(100, 200, 700, 500, p); // 600 x 300
        Bitmap out = SupabaseStorage.squareLogo(src, SIZE);
        int[] r = redBounds(out);
        assertTrue("full width kept", r[0] <= 1 && r[2] >= SIZE - 2);
        int height = r[3] - r[1] + 1;
        assertEquals("aspect kept (half height)", SIZE / 2f, height, 3f);
        assertEquals("vertically centred", SIZE / 2f, (r[1] + r[3]) / 2f, 3f);
        assertEquals("padding uses logo background", Color.WHITE, out.getPixel(SIZE / 2, 5));
    }
}
