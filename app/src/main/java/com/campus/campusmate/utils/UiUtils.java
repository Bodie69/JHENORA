package com.campus.campusmate.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.campus.campusmate.R;
import com.campus.campusmate.models.Announcement;
import com.google.android.material.snackbar.Snackbar;

/**
 * Utility class for common UI operations.
 * Provides helper methods for showing Toasts, Snackbars, visibility toggles, and animations.
 */
public final class UiUtils {

    private UiUtils() {}

    /**
     * Shows a short-duration Toast message.
     *
     * @param context The application context.
     * @param message The message to display.
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a long-duration Toast message.
     *
     * @param context The application context.
     * @param message The message to display.
     */
    public static void showLongToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a short-duration Snackbar.
     *
     * @param root    The root view to anchor the Snackbar.
     * @param message The message to display.
     */
    public static void showSnackbar(View root, String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a long-duration Snackbar with an action button.
     *
     * @param root     The root view to anchor the Snackbar.
     * @param message  The message to display.
     * @param action   The text for the action button.
     * @param listener The click listener for the action button.
     */
    public static void showSnackbarAction(View root, String message, String action, View.OnClickListener listener) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG)
                .setAction(action, listener)
                .show();
    }

    /**
     * Fades in a view and sets its visibility to VISIBLE.
     *
     * @param view The view to animate.
     */
    public static void fadeIn(View view) {
        if (view == null) return;
        view.setVisibility(View.VISIBLE);
        view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in));
    }

    /**
     * Fades out a view and sets its visibility to GONE.
     *
     * @param view The view to animate.
     */
    public static void fadeOut(View view) {
        if (view == null) return;
        view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_out));
        view.setVisibility(View.GONE);
    }

    /**
     * Sets the view visibility to VISIBLE.
     *
     * @param view The view to show.
     */
    public static void show(View view) {
        if (view != null) view.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the view visibility to GONE.
     *
     * @param view The view to hide.
     */
    public static void hide(View view) {
        if (view != null) view.setVisibility(View.GONE);
    }

    /**
     * Sets the view visibility to INVISIBLE.
     *
     * @param view The view to make invisible.
     */
    public static void invisible(View view) {
        if (view != null) view.setVisibility(View.INVISIBLE);
    }

    /**
     * Hides the soft keyboard from the focused view.
     */
    public static void hideKeyboard(View view) {
        if (view == null) return;
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Wires a toggle button to show/hide the text of a password field.
     *
     * @param toggle    The button that toggles visibility (shows an eye / crossed-out eye icon).
     * @param editText  The password EditText to reveal or mask.
     */
    public static void setupPasswordToggle(android.widget.ImageButton toggle, android.widget.EditText editText) {
        if (toggle == null || editText == null) return;
        android.graphics.Typeface typeface = editText.getTypeface();
        toggle.setOnClickListener(v -> {
            boolean isVisible = Boolean.TRUE.equals(toggle.getTag());
            int selection = editText.getSelectionEnd();
            if (isVisible) {
                editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggle.setImageResource(R.drawable.ic_eye);
            } else {
                editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggle.setImageResource(R.drawable.ic_eye_off);
            }
            editText.setTypeface(typeface);
            if (selection >= 0) editText.setSelection(selection);
            toggle.setTag(!isVisible);
        });
    }

    /**
     * Applies a priority badge (text, background, and color) to a TextView.
     *
     * @param badge    The TextView to use as a badge.
     * @param priority The priority level (e.g., URGENT, HIGH).
     */
    public static void applyPriorityBadge(TextView badge, String priority) {
        if (badge == null) return;
        switch (priority) {
            case Announcement.PRIORITY_URGENT:
                badge.setText(R.string.priority_urgent);
                badge.setBackgroundResource(R.drawable.bg_priority_urgent);
                badge.setTextColor(badge.getContext().getColor(R.color.danger));
                badge.setVisibility(View.VISIBLE);
                break;
            case Announcement.PRIORITY_HIGH:
                badge.setText(R.string.priority_high);
                badge.setBackgroundResource(R.drawable.bg_priority_high);
                badge.setTextColor(badge.getContext().getColor(R.color.warning));
                badge.setVisibility(View.VISIBLE);
                break;
            default:
                badge.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Applies a status badge (text, background, and color) to a TextView.
     *
     * @param badge  The TextView to use as a badge.
     * @param status The status level (e.g., APPROVED, REJECTED, PENDING).
     */
    public static void applyStatusBadge(TextView badge, String status) {
        if (badge == null) return;
        switch (status) {
            case Announcement.STATUS_APPROVED:
                badge.setText(R.string.status_approved);
                badge.setBackgroundResource(R.drawable.bg_status_approved);
                badge.setTextColor(badge.getContext().getColor(R.color.success));
                break;
            case Announcement.STATUS_REJECTED:
                badge.setText(R.string.status_rejected);
                badge.setBackgroundResource(R.drawable.bg_status_rejected);
                badge.setTextColor(badge.getContext().getColor(R.color.danger));
                break;
            default:
                badge.setText(R.string.status_pending);
                badge.setBackgroundResource(R.drawable.bg_status_pending);
                badge.setTextColor(badge.getContext().getColor(R.color.warning));
                break;
        }
    }
}
