package com.campus.campusmate.activities;

import android.content.Intent;
import android.view.LayoutInflater;

import com.campus.campusmate.base.BaseActivity;
import com.campus.campusmate.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends BaseActivity<ActivityWelcomeBinding> {

    @Override
    protected ActivityWelcomeBinding inflateBinding(LayoutInflater inflater) {
        return ActivityWelcomeBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        binding.btnGetStarted.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        binding.btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    @Override
    protected void setupObservers() {
        // No ViewModel state on this screen.
    }
}
