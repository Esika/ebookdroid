package org.ebookdroid.common.bitmaps;

import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.core.PagePaint;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bitmaps {

    private static final LogContext LCTX = BitmapManager.LCTX;

    private static final Config DEF_BITMAP_TYPE = Bitmap.Config.RGB_565;

    private static boolean useDefaultBitmapType = true;

    public final Bitmap.Config config;
    public final int partSize;
    public Rect bounds;
    public int columns;
    public int rows;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private BitmapRef[] bitmaps;

    public Bitmaps(final String nodeId, final BitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        final Bitmap origBitmap = orig.getBitmap();

        this.partSize = SettingsManager.getAppSettings().getBitmapSize();
        this.bounds = bitmapBounds;
        this.columns = (int) Math.ceil(bounds.width() / (float) partSize);
        this.rows = (int) Math.ceil(bounds.height() / (float) partSize);
        this.config = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
        this.bitmaps = new BitmapRef[columns * rows];

        int top = 0;

        final RawBitmap rb = new RawBitmap(partSize, partSize, origBitmap.hasAlpha());

        for (int row = 0; row < rows; row++, top += partSize) {
            int left = 0;
            for (int col = 0; col < columns; col++, left += partSize) {
                final String name = nodeId + ":[" + row + ", " + col + "]";
                final BitmapRef b = BitmapManager.getBitmap(name, partSize, partSize, config);
                final Bitmap bmp = b.getBitmap();

                if (row == rows - 1 || col == columns - 1) {
                    final int right = Math.min(left + partSize, bounds.width());
                    final int bottom = Math.min(top + partSize, bounds.height());
                    rb.retrieve(origBitmap, left, top, right - left, bottom - top);
                } else {
                    rb.retrieve(origBitmap, left, top, partSize, partSize);
                }
                if (invert) {
                    rb.invert();
                }
                rb.toBitmap(bmp);

                final int index = row * columns + col;
                bitmaps[index] = b;
            }
        }
    }

    public boolean reuse(final String nodeId, final BitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        lock.writeLock().lock();
        try {
            final Bitmap origBitmap = orig.getBitmap();
            final Config cfg = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
            if (cfg != this.config) {
                return false;
            }
            final int size = SettingsManager.getAppSettings().getBitmapSize();
            if (size != this.partSize) {
                return false;
            }

            final BitmapRef[] oldBitmaps = this.bitmaps;

            this.bounds = bitmapBounds;
            this.columns = (int) Math.ceil(bitmapBounds.width() / (float) partSize);
            this.rows = (int) Math.ceil(bitmapBounds.height() / (float) partSize);
            this.bitmaps = new BitmapRef[columns * rows];

            final int newsize = this.columns * this.rows;

            int i = 0;
            for (; i < newsize; i++) {
                this.bitmaps[i] = i < oldBitmaps.length ? oldBitmaps[i] : null;
                if (this.bitmaps[i] == null || this.bitmaps[i].getBitmap() == null) {
                    BitmapManager.release(this.bitmaps[i]);
                    this.bitmaps[i] = BitmapManager.getBitmap(nodeId + ":reuse:" + i, partSize, partSize, config);
                } else {
                    if (LCTX.isDebugEnabled()) {
                        LCTX.d("Reuse  bitmap: " + this.bitmaps[i]);
                    }
                }
                this.bitmaps[i].getBitmap().eraseColor(Color.CYAN);
            }
            for (; i < oldBitmaps.length; i++) {
                BitmapManager.release(oldBitmaps[i]);
            }

            int top = 0;

            final RawBitmap rb = new RawBitmap(partSize, partSize, origBitmap.hasAlpha());

            for (int row = 0; row < rows; row++, top += partSize) {
                int left = 0;
                for (int col = 0; col < columns; col++, left += partSize) {
                    final int index = row * columns + col;
                    final BitmapRef b = bitmaps[index];
                    final Bitmap bmp = b.getBitmap();

                    if (row == rows - 1 || col == columns - 1) {
                        final int right = Math.min(left + partSize, bounds.width());
                        final int bottom = Math.min(top + partSize, bounds.height());
                        rb.retrieve(origBitmap, left, top, right - left, bottom - top);
                    } else {
                        rb.retrieve(origBitmap, left, top, partSize, partSize);
                    }
                    if (invert) {
                        rb.invert();
                    }
                    rb.toBitmap(bmp);
                }
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasBitmaps() {
        lock.readLock().lock();
        try {
            if (bitmaps == null) {
                return false;
            }
            for (int i = 0; i < bitmaps.length; i++) {
                if (bitmaps[i] == null) {
                    return false;
                }
                if (bitmaps[i].isRecycled()) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    BitmapRef[] clear() {
        lock.writeLock().lock();
        try {
            BitmapRef[] refs = this.bitmaps;
            this.bitmaps = null;
            return refs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        lock.writeLock().lock();
        try {
            if (bitmaps != null) {
                for (BitmapRef ref : bitmaps) {
                    BitmapManager.release(ref);
                }
                bitmaps = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean draw(final Canvas canvas, final PagePaint paint, final PointF vb, final RectF tr, final RectF cr) {
        lock.readLock().lock();
        try {
            if (this.bitmaps == null) {
                return false;
            }

            final Rect orig = canvas.getClipBounds();
            canvas.clipRect(cr.left - vb.x, cr.top - vb.y, cr.right - vb.x, cr.bottom - vb.y, Op.REPLACE);

            final float scaleX = tr.width() / bounds.width();
            final float scaleY = tr.height() / bounds.height();

            int top = 0;
            boolean res = true;
            for (int row = 0; row < rows; row++, top += partSize) {
                int left = 0;
                for (int col = 0; col < columns; col++, left += partSize) {
                    final Matrix m = new Matrix();
                    m.postTranslate(left, top);
                    m.postScale(scaleX, scaleY);
                    m.postTranslate(tr.left - vb.x, tr.top - vb.y);

                    final int index = row * columns + col;
                    if (this.bitmaps[index] != null && this.bitmaps[index].bitmap != null
                            && !this.bitmaps[index].bitmap.isRecycled()) {
                        canvas.drawBitmap(this.bitmaps[index].bitmap, m, paint.bitmapPaint);
                    } else {
                        res = false;
                    }
                }
            }
            canvas.clipRect(orig, Op.REPLACE);
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }
}