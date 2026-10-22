package com.campus.campusmate.activities;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityLiveDisplayBinding;
import com.campus.campusmate.databinding.ItemLiveDisplayCardBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.DepartmentLogos;
import com.campus.campusmate.viewmodels.LiveDisplayViewModel;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Kiosk digital-signage screen: a single always-visible card that rotates through a merged
 * Announcements+Events feed, alternating between the two types tick to tick. Each card's
 * tv_type badge names which kind of post is currently showing.
 *
 * <p>"Time bomb": neither list is trusted as-is on every tick — {@link #liveAnnouncements()}
 * and {@link #liveEvents()} drop anything out of date first (an announcement past its own
 * event_date, or one with no event_date set past {@link Constants#LIVE_DISPLAY_ANNOUNCEMENT_MAX_AGE_MS}
 * since it was posted; an event past its own end date), so stale posts age out of the
 * rotation on their own without any admin cleanup.
 *
 * <p>A description too long to fit its card auto-scrolls instead of being cut off (see
 * {@link #settleContentAndSchedule}), and that card's dwell time stretches to match, so a long
 * description is always fully readable rather than truncated to fit a fixed slide duration.
 */
public class LiveDisplayActivity extends BaseActivity<ActivityLiveDisplayBinding> {

    private LiveDisplayViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<Announcement> announcements = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private int announcementIndex = 0;
    private int eventIndex = 0;
    private int slideTick = 0;
    private ObjectAnimator timerAnimator;
    private ObjectAnimator scrollAnimator;

    @Override
    protected ActivityLiveDisplayBinding inflateBinding(LayoutInflater inflater) {
        return ActivityLiveDisplayBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(LiveDisplayViewModel.class);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyFullscreen();

        startClock();
        startDataRefresh();
        startSlideShow();
    }

    @Override
    protected void setupObservers() {
        viewModel.announcements.observe(this, list -> {
            if (list != null) announcements = list;
        });

        viewModel.events.observe(this, list -> {
            if (list != null) events = list;
        });
    }

    private void applyFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            binding.tvClock.setText(DateUtils.formatTime(now));
            binding.tvDate.setText(DateUtils.formatDate(now));
            mainHandler.postDelayed(this, 1000);
        }
    };

    private void startClock() {
        mainHandler.post(clockRunnable);
    }

    private void startDataRefresh() {
        Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                viewModel.loadData();
                mainHandler.postDelayed(this, Constants.LIVE_DISPLAY_REFRESH_INTERVAL_MS);
            }
        };
        mainHandler.post(refreshTask);
    }

    /** Not a fixed-interval loop: a card whose description needs to auto-scroll schedules its
     *  own next tick further out (see {@link #settleContentAndSchedule}) instead of always
     *  waiting {@link Constants#LIVE_DISPLAY_SLIDE_INTERVAL_MS}, so the reader always gets
     *  enough time to reach the bottom before the card rotates out. */
    private void startSlideShow() {
        mainHandler.postDelayed(this::updateCard, 2000);
    }

    /** The "time bomb" here: an announcement with a real event_date (set via
     *  CreateAnnouncementActivity's date/time picker) expires once that date passes — the
     *  same author-set-expiry logic as {@link #liveEvents()}. One without it (an old row from
     *  before event_date existed, or a general FYI with no specific date) instead ages out
     *  {@link Constants#LIVE_DISPLAY_ANNOUNCEMENT_MAX_AGE_MS} after it was posted. */
    private List<Announcement> liveAnnouncements() {
        long cutoff = System.currentTimeMillis() - Constants.LIVE_DISPLAY_ANNOUNCEMENT_MAX_AGE_MS;
        List<Announcement> live = new ArrayList<>();
        for (Announcement a : announcements) {
            boolean expired = a.getEventDate() > 0 ? a.isPast() : a.getCreatedAt() < cutoff;
            if (!expired) live.add(a);
        }
        return live;
    }

    /** Events past their own end date are the "time bomb" here — {@link Event#isPast()} is
     *  the author-set expiry, so no separate age threshold is needed. */
    private List<Event> liveEvents() {
        List<Event> live = new ArrayList<>();
        for (Event e : events) {
            if (!e.isPast()) live.add(e);
        }
        return live;
    }

    private void updateCard() {
        ItemLiveDisplayCardBinding card = binding.cardDisplay;
        List<Announcement> liveAnnouncements = liveAnnouncements();
        List<Event> liveEvents = liveEvents();

        if (liveAnnouncements.isEmpty() && liveEvents.isEmpty()) {
            showEmpty(card, "No announcements or events yet");
            if (timerAnimator != null) timerAnimator.cancel();
            if (scrollAnimator != null) scrollAnimator.cancel();
            mainHandler.postDelayed(this::updateCard, Constants.LIVE_DISPLAY_SLIDE_INTERVAL_MS);
            return;
        }

        // Alternate type tick to tick, falling back to whichever type actually has content.
        slideTick++;
        boolean preferAnnouncement = slideTick % 2 == 0;
        boolean useAnnouncement = preferAnnouncement ? !liveAnnouncements.isEmpty() : liveEvents.isEmpty();

        if (useAnnouncement) {
            int index = announcementIndex % liveAnnouncements.size();
            announcementIndex++;
            showAnnouncementCard(card, liveAnnouncements.get(index));
        } else {
            int index = eventIndex % liveEvents.size();
            eventIndex++;
            showEventCard(card, liveEvents.get(index));
        }
    }

    private void showAnnouncementCard(ItemLiveDisplayCardBinding card, Announcement a) {
        fadeSwap(card.layoutPopulated, () -> {
            card.layoutEmpty.setVisibility(View.GONE);
            card.tvType.setText("ANNOUNCEMENT");
            card.tvTitle.setText(a.getTitle());
            card.tvContent.setText(a.getWhat() != null ? a.getWhat() : "");
            card.tvAuthor.setText("By " + (a.getAuthorName() != null ? a.getAuthorName() : "Unknown"));
            setBadges(card, a.getDepartment(), Announcement.VISIBILITY_GENERAL.equals(a.getVisibility()));
            card.tvDate.setText(DateUtils.formatDate(a.getCreatedAt()));
            loadImage(card, a.getImageUrl());
            settleContentAndSchedule(card);
        });
    }

    private void showEventCard(ItemLiveDisplayCardBinding card, Event e) {
        fadeSwap(card.layoutPopulated, () -> {
            card.layoutEmpty.setVisibility(View.GONE);
            card.tvType.setText("EVENT");
            card.tvTitle.setText(e.getTitle());
            card.tvContent.setText(e.getWhat() != null ? e.getWhat() : "");
            card.tvAuthor.setText("By " + (e.getAuthorName() != null ? e.getAuthorName() : "Unknown"));
            setBadges(card, e.getDepartment(), Event.VISIBILITY_GENERAL.equals(e.getVisibility()));
            card.tvDate.setText(e.getStartDate() > 0 ? DateUtils.formatDate(e.getStartDate()) : "");
            loadImage(card, e.getImageUrl());
            settleContentAndSchedule(card);
        });
    }

    /** tv_dept always names the poster's actual department (BSIT/BTLED/EDUC — every post has
     *  one regardless of scope), hidden only if it's somehow unset. tv_scope adds a "General"
     *  badge alongside it for General-visibility posts, so a reader can tell both who posted
     *  it and whether it's campus-wide or department-only at a glance. */
    private void setBadges(ItemLiveDisplayCardBinding card, String department, boolean isGeneral) {
        boolean hasDept = department != null && !department.isEmpty();
        card.tvDept.setText(hasDept ? department : "");
        card.tvDept.setVisibility(hasDept ? View.VISIBLE : View.GONE);
        DepartmentLogos.bind(card.tvDept, department);
        card.tvScope.setVisibility(isGeneral ? View.VISIBLE : View.GONE);
    }

    /** The description sits in a scroll box whose height is whatever's left between the title
     *  and the author/date row, which varies with title length and screen size. Once laid out,
     *  this measures whether the full description actually fits: if not, it auto-scrolls from
     *  top to bottom (with a pause at each end to read) instead of ellipsizing, and — since
     *  that takes longer than a normal slide — extends this card's dwell time so the scroll
     *  can actually finish being read before the card rotates out. Either way, it's also what
     *  starts (or restarts) this card's progress-bar timer and schedules the next tick, since
     *  only here is the true dwell time known. */
    private void settleContentAndSchedule(ItemLiveDisplayCardBinding card) {
        card.scrollContent.scrollTo(0, 0);
        card.scrollContent.post(() -> {
            if (scrollAnimator != null) scrollAnimator.cancel();

            int overflow = card.tvContent.getHeight() - card.scrollContent.getHeight();
            long dwellMs = Constants.LIVE_DISPLAY_SLIDE_INTERVAL_MS;

            if (overflow > 0) {
                float pxPerSec = Constants.LIVE_DISPLAY_SCROLL_DP_PER_SEC
                        * getResources().getDisplayMetrics().density;
                long scrollDurationMs = (long) (overflow / pxPerSec * 1000);
                dwellMs = Math.max(dwellMs, Constants.LIVE_DISPLAY_SCROLL_HOLD_TOP_MS
                        + scrollDurationMs + Constants.LIVE_DISPLAY_SCROLL_HOLD_BOTTOM_MS);

                scrollAnimator = ObjectAnimator.ofInt(card.scrollContent, "scrollY", 0, overflow);
                scrollAnimator.setStartDelay(Constants.LIVE_DISPLAY_SCROLL_HOLD_TOP_MS);
                scrollAnimator.setDuration(scrollDurationMs);
                scrollAnimator.start();
            }

            restartTimer(card, dwellMs);
            mainHandler.postDelayed(this::updateCard, dwellMs);
        });
    }

    private void showEmpty(ItemLiveDisplayCardBinding card, String message) {
        card.tvEmpty.setText(message);
        card.layoutEmpty.setVisibility(View.VISIBLE);
        card.layoutPopulated.setVisibility(View.GONE);
    }

    private void loadImage(ItemLiveDisplayCardBinding card, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            // No image on this item: clear any previous slide's image rather than leaving it
            // showing stale, so an empty item never looks like a failed load for this one.
            Glide.with(this).clear(card.ivImage);
            card.ivImage.setImageResource(0);
            return;
        }
        Glide.with(this)
                .load(imageUrl)
                // The target ImageView sits several 0dp/layout_weight levels deep (card ->
                // <include> override -> card view -> image frame); its first-ever measured size
                // can be a stale/transient tiny value, which Glide would otherwise cache a
                // decode at permanently. Decoding at the image's real resolution sidesteps that.
                .override(Target.SIZE_ORIGINAL)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target,
                                                 boolean isFirstResource) {
                        Timber.w(e, "Live Display: failed to load image %s", imageUrl);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                    DataSource dataSource, boolean isFirstResource) {
                        Timber.d("Live Display: loaded image %s (%dx%d) from %s", imageUrl,
                                resource.getIntrinsicWidth(), resource.getIntrinsicHeight(), dataSource);
                        return false;
                    }
                })
                .into(card.ivImage);
    }

    /** Fades {@code target} out, runs {@code applyChanges} (swap in the new slide's content and
     *  make it visible if it was showing the empty state), then fades it back in. */
    private void fadeSwap(View target, Runnable applyChanges) {
        target.setVisibility(View.VISIBLE);

        Animation fadeOut = new AlphaAnimation(target.getAlpha(), 0);
        fadeOut.setDuration(300);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                applyChanges.run();
                Animation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(300);
                fadeIn.setFillAfter(true);
                target.startAnimation(fadeIn);
            }
        });
        target.startAnimation(fadeOut);
    }

    private void restartTimer(ItemLiveDisplayCardBinding card, long durationMs) {
        if (timerAnimator != null) timerAnimator.cancel();

        card.progressSlide.setProgress(0);
        timerAnimator = ObjectAnimator.ofInt(card.progressSlide, "progress", 0, 100);
        timerAnimator.setDuration(durationMs);
        timerAnimator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFullscreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (timerAnimator != null) timerAnimator.cancel();
        if (scrollAnimator != null) scrollAnimator.cancel();
    }
}
