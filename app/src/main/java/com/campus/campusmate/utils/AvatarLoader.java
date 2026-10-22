package com.campus.campusmate.utils;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import timber.log.Timber;

/**
 * Loads a user's profile photo over their initials avatar. The initials view stays visible
 * underneath, so it shows while the photo loads and remains as the fallback when there is no
 * photo or the photo fails to load. Safe to call from RecyclerView binds: Glide cancels the
 * view's previous request first.
 */
public final class AvatarLoader {

    private AvatarLoader() {}

    /**
     * @param photo    image view stacked above {@code fallback} (e.g. in a FrameLayout)
     * @param fallback initials avatar shown when there is no usable photo
     * @param url      public photo URL; {@code null} or empty for none
     */
    public static void load(@NonNull ImageView photo, @NonNull View fallback, @Nullable String url) {
        fallback.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(url)) {
            Glide.with(photo).clear(photo);
            photo.setVisibility(View.GONE);
            return;
        }
        photo.setVisibility(View.VISIBLE);
        Glide.with(photo)
                .load(url)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        Timber.w(e, "Profile photo failed to load");
                        photo.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(photo);
    }
}
