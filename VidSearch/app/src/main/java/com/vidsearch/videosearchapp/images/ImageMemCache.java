package com.vidsearch.videosearchapp.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.core.graphics.BitmapCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class ImageMemCache {
    private static final String TAG = "ImageMemCache";

    private int mMemCacheSize;
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private Set<SoftReference<Bitmap>> mReusableBitmaps;
    private static ImageMemCache sInstance = null;

    /*
     Create a new ImageMemCache. Use getInstance to get to an ImageMemCache instance
     */
    private ImageMemCache(float percent) {
        if (percent < 0.01f || percent > 0.8f) {
            throw new IllegalArgumentException("Illegal mem cache percent");
        }
        mMemCacheSize = Math.round(percent*Runtime.getRuntime().maxMemory()/1024);
        Log.d(TAG, "Mem cache created: " + mMemCacheSize + " KB");

        mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

        mMemoryCache = new LruCache<String, BitmapDrawable>(mMemCacheSize) {

            @Override
            protected void entryRemoved(
                    boolean evicted,
                    @NonNull String key,
                    @NonNull BitmapDrawable oldValue,
                    BitmapDrawable newValue) {
                Log.d(TAG, "Memcache: Bitmap removed: " + key + " obj: " + oldValue + " new: " + newValue);
                // Add the bitmap to the SoftReference set for possible use with inBitmap later
                mReusableBitmaps.add(new SoftReference<Bitmap>(oldValue.getBitmap()));
            }

            /*
             Measure item size in KB rather than units; more practical for a bitmap cache
             */
            @Override
            protected int sizeOf(@NonNull String key, @NonNull BitmapDrawable value) {
                // get the size of a bitmap in a BitmapDrawable; returns allocated memory size
                // which may be larger than the actual bitmap data byte count (in case it was reused)
                final int bitmapSize = BitmapCompat.getAllocationByteCount(value.getBitmap())/1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };
    }

    /*
     Return the singleton ImageCache instance.
     */
    public static ImageMemCache getInstance(float percent) {
        if (sInstance == null) {
            sInstance = new ImageMemCache(percent);
        }
        return sInstance;
    }

    /*
     Add a bitmap to the memory cache
     */
    public void addBitmapToMemCache(String key, BitmapDrawable bitmap) {
        Log.d(TAG, "addImageToCache: " + key + "; obj: " + bitmap);
        mMemoryCache.put(key, bitmap);
    }

    /*
     Get the bitmap from the memory cache
     */
    public BitmapDrawable getBitmapFromMemCache(String key) {
        //Log.d(TAG, "getImageFromMemCache: " + key);
        BitmapDrawable bitmap = mMemoryCache.get(key);
        if (bitmap != null) {
            Log.d(TAG, "Memory cache hit: " + key);
        }
        return bitmap;
    }

    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        Bitmap bitmap = null;
        synchronized (mReusableBitmaps) {
            if (!mReusableBitmaps.isEmpty()) {
                final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
                Bitmap item;
                while (iterator.hasNext()) {
                    item = iterator.next().get();
                    if (item == null) {
                        Log.d(TAG, "getBitmapReusable has null");
                    } else {
                        Log.d(TAG, "getBitmapReusable: " + item.isMutable());
                    }
                    if (null != item && item.isMutable()) {
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;
                            // remove from the reusable set so it can't be used again
                            iterator.remove();
                            break;
                        }
                    } else {
                        // remove from the set if the reference has been cleared
                        iterator.remove();
                    }
                } // while (iterator.hasNext())
            } // if (!mReusableBitmaps.isEmpty())
        }
        return bitmap;
    }

    public void clearMemCache() {
        // TODO: add a menu option to clear cache!
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
    }

    private static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
            return candidate.getWidth() == targetOptions.outWidth &&
                    candidate.getHeight() == targetOptions.outHeight &&
                    targetOptions.inSampleSize == 1;
        }
        // From Android 4.4 (Kitkat) onward we can re-use if the byte size of the new bitmap is
        // smaller than the reusable bitmap candidate allocation byte count
        int width = targetOptions.outWidth / targetOptions.inSampleSize;
        int height = targetOptions.outHeight / targetOptions.inSampleSize;
        int bytes = width * height * getBytesPerPixel(candidate.getConfig());
        return bytes <= candidate.getAllocationByteCount();
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }
}
