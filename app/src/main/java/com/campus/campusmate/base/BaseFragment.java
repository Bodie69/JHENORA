package com.campus.campusmate.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.snackbar.Snackbar;

/**
 * Base Fragment class that handles ViewBinding lifecycle and common utilities.
 *
 * @param <VB> The ViewBinding type.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    protected VB binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        if (binding == null) {
            throw new IllegalStateException("Binding cannot be null. Ensure inflateBinding is implemented correctly.");
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        setupObservers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Inflates the ViewBinding.
     * Example: return FragmentHomeBinding.inflate(inflater, container, false);
     */
    protected abstract VB inflateBinding(LayoutInflater inflater, ViewGroup container);

    /**
     * Initialize UI components, listeners, etc.
     */
    protected abstract void setupViews();

    /**
     * Set up LiveData observers.
     */
    protected abstract void setupObservers();

    /**
     * Shows a Snackbar with the given message.
     */
    protected void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }
}
