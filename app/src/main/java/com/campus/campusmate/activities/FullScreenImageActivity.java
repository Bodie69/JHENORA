package com.campus.campusmate.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityFullscreenImageBinding;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.UiUtils;

/**
 * Full-screen, pinch-to-zoom viewer for an announcement/event's cover image. Opened by tapping
 * the cover image on {@link AnnouncementDetailActivity} or {@link EventDetailActivity}.
 */
public class FullScreenImageActivity extends BaseActivity<ActivityFullscreenImageBinding> {

    /** Starts the viewer for {@code imageUrl}. No-ops if the url is null/empty. */
    public static void start(Context context, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putExtra(Constants.EXTRA_IMAGE_URL, imageUrl);
        context.startActivity(intent);
    }

    @Override
    protected ActivityFullscreenImageBinding inflateBinding(LayoutInflater inflater) {
        return ActivityFullscreenImageBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        String imageUrl = getIntent().getStringExtra(Constants.EXTRA_IMAGE_URL);
        if (imageUrl == null || imageUrl.isEmpty()) {
            finish();
            return;
        }

        binding.btnClose.setOnClickListener(v -> finish());
        binding.ivFullscreen.setOnClickListener(v -> finish());

        Glide.with(this)
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e,
                                                 Object model, Target<Drawable> target, boolean isFirstResource) {
                        binding.progressBar.setVisibility(View.GONE);
                        UiUtils.showToast(FullScreenImageActivity.this, "Failed to load image");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                    DataSource dataSource, boolean isFirstResource) {
                        binding.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(binding.ivFullscreen);
    }

    @Override
    protected void setupObservers() {
        // No ViewModel — this screen only displays the image passed via intent extra.
    }
}
