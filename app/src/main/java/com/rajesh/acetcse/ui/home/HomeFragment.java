package com.rajesh.acetcse.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.rajesh.acetcse.databinding.FragmentHomeBinding;
import com.rajesh.acetcse.IIAActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.button.setOnClickListener(v -> openClass("II-A"));
        binding.button2.setOnClickListener(v -> openClass("II-B"));
        binding.button3.setOnClickListener(v -> openClass("III-A"));
        binding.button4.setOnClickListener(v -> openClass("III-B"));
        binding.button5.setOnClickListener(v -> openClass("IV-A"));
        binding.button6.setOnClickListener(v -> openClass("IV-B"));

        return root;
    }

    private void openClass(String className) {
        Intent intent = new Intent(getActivity(), IIAActivity.class);
        intent.putExtra("className", className);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
