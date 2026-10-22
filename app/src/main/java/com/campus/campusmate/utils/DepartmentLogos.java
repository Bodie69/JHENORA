package com.campus.campusmate.utils;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.campus.campusmate.R;
import com.campus.campusmate.repositories.DepartmentRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shows a department's uploaded logo as a small circular icon at the start of a department
 * label (feed tags, detail screens, live display, profile, admin lists). Labels for
 * departments without a logo are left as plain text, so the tag looks exactly as before.
 * <p>
 * The logo lookup is fetched in the background, so a label may be bound before it arrives
 * (first launch after install, or a logo just changed by an admin). Bound labels are tracked
 * weakly and rebound whenever {@link DepartmentRepository#getLogoUpdates()} fires.
 */
public final class DepartmentLogos {

    /** Icon diameter relative to the label's text size. */
    private static final float ICON_SCALE = 1.4f;
    private static final int ICON_PADDING_DP = 4;

    /** Labels bound so far; weak keys so recycled/destroyed views can be collected. Main thread only. */
    private static final Set<TextView> boundLabels =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean observingUpdates;

    private DepartmentLogos() {}

    /**
     * Binds {@code department}'s logo to the start of {@code label}, or clears it when the
     * department is {@code null} or has no logo. General posts pass
     * {@link Constants#DEPT_GENERAL} to show the General logo. Safe to call from
     * RecyclerView binds: a recycled view's previous load is cancelled first.
     */
    @MainThread
    public static void bind(@NonNull TextView label, @Nullable String department) {
        observeUpdates();
        label.setTag(R.id.tag_department_logo, department);
        boundLabels.add(label);
        int size = Math.round(label.getTextSize() * ICON_SCALE);
        String url = DepartmentRepository.getInstance().getLogoUrl(department);
        if (url == null) {
            Glide.with(label).clear(new StartIconTarget(label, size));
            setIcon(label, null, size);
            return;
        }
        Glide.with(label)
                .load(url)
                .override(size)
                .circleCrop()
                .into(new StartIconTarget(label, size));
    }

    /** Rebinds every tracked label still on screen when the logo lookup changes. */
    private static void observeUpdates() {
        if (observingUpdates) return;
        observingUpdates = true;
        DepartmentRepository.getInstance().getLogoUpdates().observeForever(logos -> {
            for (TextView label : new ArrayList<>(boundLabels)) {
                // Detached views may belong to a destroyed activity, where Glide would throw;
                // RecyclerView rebinds them anyway when they come back.
                if (!label.isAttachedToWindow()) continue;
                bind(label, (String) label.getTag(R.id.tag_department_logo));
            }
        });
    }

    private static void setIcon(TextView label, @Nullable Drawable icon, int size) {
        if (icon != null) icon.setBounds(0, 0, size, size);
        label.setCompoundDrawablesRelative(icon, null, null, null);
        label.setCompoundDrawablePadding(icon != null
                ? Math.round(ICON_PADDING_DP * label.getResources().getDisplayMetrics().density)
                : 0);
    }

    /** Glide target that renders into the label's start compound drawable. */
    private static final class StartIconTarget extends CustomViewTarget<TextView, Drawable> {
        private final int size;

        StartIconTarget(TextView label, int size) {
            super(label);
            this.size = size;
        }

        @Override
        public void onResourceReady(@NonNull Drawable resource,
                                    @Nullable Transition<? super Drawable> transition) {
            setIcon(view, resource, size);
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            setIcon(view, null, size);
        }

        @Override
        protected void onResourceCleared(@Nullable Drawable placeholder) {
            setIcon(view, null, size);
        }
    }
}
