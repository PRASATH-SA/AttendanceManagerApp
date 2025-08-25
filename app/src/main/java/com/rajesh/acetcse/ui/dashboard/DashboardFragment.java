package com.rajesh.acetcse.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.rajesh.acetcse.ui.dashboard.AttendanceViewActivity;
import com.rajesh.acetcse.R;

public class DashboardFragment extends Fragment {

    private Spinner spinnerClass;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        spinnerClass = root.findViewById(R.id.spinnerClass);
        Button btnViewAttendance = root.findViewById(R.id.btnViewAttendance);

        // Class list
        String[] classOptions = {"II-A", "II-B", "III-A", "III-B", "IV-A", "IV-B"};

        // Set spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                classOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        // Button action
        btnViewAttendance.setOnClickListener(v -> {
            String selectedClass = spinnerClass.getSelectedItem().toString();

            if (selectedClass.isEmpty()) {
                Toast.makeText(getContext(), "Please select a class", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(getActivity(), AttendanceViewActivity.class);
                intent.putExtra("className", selectedClass);
                startActivity(intent);
            }
        });

        return root;
    }
}
