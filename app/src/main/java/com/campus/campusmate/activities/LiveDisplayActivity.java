package com.campus.campusmate.activities;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityLiveDisplayBinding;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.viewmodels.LiveDisplayViewModel;

import java.util.ArrayList;
import java.util.List;

public class LiveDisplayActivity extends BaseActivity<ActivityLiveDisplayBinding> {

    private LiveDisplayViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<Announcement> announcements = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private int announcementIndex = 0;
    private int eventIndex = 0;
    private boolean toggleFlag = false;

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
            binding.tvClock.setText(DateUtils.formatTime(System.currentTimeMillis()));
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

    private final Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            showNextSlide();
            mainHandler.postDelayed(this, Constants.LIVE_DISPLAY_SLIDE_INTERVAL_MS);
        }
    };

    private void startSlideShow() {
        mainHandler.postDelayed(slideRunnable, 2000);
    }

    private void showNextSlide() {
        if (announcements.isEmpty() && events.isEmpty()) {
            binding.tvNoContent.setVisibility(View.VISIBLE);
            binding.layoutSlide.setVisibility(View.GONE);
            binding.ivBackground.setImageResource(0);
            return;
        }

        binding.tvNoContent.setVisibility(View.GONE);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(500);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                int currAnn = announcements.size();
                int currEv = events.size();

                if (currAnn == 0 && currEv == 0) {
                    binding.tvNoContent.setVisibility(View.VISIBLE);
                    binding.layoutSlide.setVisibility(View.GONE);
                    return;
                }

                updateSlideContent(currAnn, currEv);

                Animation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(500);
                binding.layoutSlide.startAnimation(fadeIn);
                binding.layoutSlide.setVisibility(View.VISIBLE);
            }
        });
        binding.layoutSlide.startAnimation(fadeOut);
    }

    private void updateSlideContent(int totalAnn, int totalEv) {
        if (totalAnn > 0 && totalEv > 0) {
            if (toggleFlag) {
                showAnnouncementSlide(announcements.get(announcementIndex % totalAnn));
                announcementIndex++;
            } else {
                showEventSlide(events.get(eventIndex % totalEv));
                eventIndex++;
            }
            toggleFlag = !toggleFlag;
        } else if (totalAnn > 0) {
            showAnnouncementSlide(announcements.get(announcementIndex % totalAnn));
            announcementIndex++;
        } else if (totalEv > 0) {
            showEventSlide(events.get(eventIndex % totalEv));
            eventIndex++;
        }
    }

    private void showAnnouncementSlide(Announcement a) {
        binding.tvSlideType.setText("ANNOUNCEMENT");
        binding.tvSlideTitle.setText(a.getTitle());
        binding.tvSlideContent.setText(a.getWhat() != null ? a.getWhat() : "");
        binding.tvSlideAuthor.setText("By " + (a.getAuthorName() != null ? a.getAuthorName() : "Unknown"));
        binding.tvSlideDept.setText(Announcement.VISIBILITY_GENERAL.equals(a.getVisibility())
                ? "General" : a.getDepartment() != null ? a.getDepartment() : "");
        binding.tvSlideDate.setText(DateUtils.formatDate(a.getCreatedAt()));

        if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(a.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivBackground);
        } else {
            binding.ivBackground.setImageResource(0);
        }
    }

    private void showEventSlide(Event e) {
        binding.tvSlideType.setText("EVENT");
        binding.tvSlideTitle.setText(e.getTitle());
        binding.tvSlideContent.setText(e.getWhat() != null ? e.getWhat() : "");
        binding.tvSlideAuthor.setText("By " + (e.getAuthorName() != null ? e.getAuthorName() : "Unknown"));
        binding.tvSlideDept.setText(Event.VISIBILITY_GENERAL.equals(e.getVisibility())
                ? "General" : e.getDepartment() != null ? e.getDepartment() : "");
        binding.tvSlideDate.setText(e.getStartDate() > 0 ? DateUtils.formatDate(e.getStartDate()) : "");

        if (e.getImageUrl() != null && !e.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(e.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivBackground);
        } else {
            binding.ivBackground.setImageResource(0);
        }
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
    }
}
