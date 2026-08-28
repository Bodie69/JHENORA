package com.campus.campusmate.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.campus.campusmate.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

/**
 * Base Activity class that simplifies ViewBinding and provides utility methods.
 *
 * @param <VB> The ViewBinding type.
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    protected VB binding;
    protected SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = inflateBinding(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        setupViews();
        setupObservers();
    }

    /**
     * Set up the toolbar/action bar with a back button.
     * @param toolbar The toolbar instance.
     */
    protected void setupBackButton(androidx.appcompat.widget.Toolbar toolbar) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     * Shows a Snackbar with the given message.
     */
    protected void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Hides the software keyboard.
     */
    protected void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    protected abstract VB inflateBinding(LayoutInflater inflater);
    protected abstract void setupViews();
    protected abstract void setupObservers();
}
