package com.rajesh.acetcse.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import com.rajesh.acetcse.R;

public class SearchFragment extends Fragment {

    private EditText editSearch;
    private Button btnSearch;
    private TableLayout tableLayoutResults, tableLayoutSummary;
    private TextView studentName, studentRoll, studentClass, presentCount, absentCount, totalDays;

    private final String DATABASE_URL = "https://security-systems-ecda3-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);

        editSearch = root.findViewById(R.id.editSearch);
        btnSearch = root.findViewById(R.id.btnSearch);
        tableLayoutResults = root.findViewById(R.id.tableLayoutResults);
        tableLayoutSummary = root.findViewById(R.id.tableLayoutSummary);
        studentName = root.findViewById(R.id.studentName);
        studentRoll = root.findViewById(R.id.studentRoll);
        studentClass = root.findViewById(R.id.studentClass);
        presentCount = root.findViewById(R.id.presentCount);
        absentCount = root.findViewById(R.id.absentCount);
        totalDays = root.findViewById(R.id.totalDays);

        btnSearch.setOnClickListener(v -> searchStudent());

        return root;
    }

    private void searchStudent() {
        String query = editSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Enter Roll No or Name", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference studentsRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;

                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.getKey();

                    for (DataSnapshot studentSnapshot : classSnapshot.getChildren()) {
                        String rollNo = studentSnapshot.getKey();
                        String name = studentSnapshot.child("name").getValue(String.class);
                        // Match by roll no or name
                        if (rollNo.equalsIgnoreCase(query) || (name != null && name.equalsIgnoreCase(query))) {
                            found = true;
                            studentName.setText("Name: " + name);
                            studentRoll.setText("Roll No: " + rollNo);
                            studentClass.setText("Class: " + className);

                            loadAttendance(rollNo, name, className);
                            break;
                        }
                    }
                    if (found) break;
                }

                if (!found) {
                    Toast.makeText(getContext(), "No student found", Toast.LENGTH_SHORT).show();
                    clearTables();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAttendance(String rollNo, String name, String className) {
        DatabaseReference attendanceRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("students").child("attendance");

        // clear tables except headers
        tableLayoutResults.removeViews(1, Math.max(0, tableLayoutResults.getChildCount() - 1));

        final int[] present = {0};
        final int[] absent = {0};

        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    if (dateSnapshot.hasChild(className) && dateSnapshot.child(className).hasChild(rollNo)) {
                        String status = dateSnapshot.child(className).child(rollNo).getValue(String.class);
                        String date = dateSnapshot.getKey();

                        TableRow row = new TableRow(getContext());
                        row.setPadding(12, 12, 12, 12);

                        TextView dateCol = new TextView(getContext());
                        dateCol.setText(date);
                        dateCol.setPadding(24, 12, 24, 12);

                        TextView statusCol = new TextView(getContext());
                        statusCol.setText(status);
                        statusCol.setPadding(24, 12, 24, 12);
                        if ("Present".equalsIgnoreCase(status)) {
                            statusCol.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            present[0]++;
                        } else {
                            statusCol.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            absent[0]++;
                        }

                        row.addView(dateCol);
                        row.addView(statusCol);
                        tableLayoutResults.addView(row);
                    }
                }

                int total = present[0] + absent[0];
                presentCount.setText(String.valueOf(present[0]));
                absentCount.setText(String.valueOf(absent[0]));
                totalDays.setText(String.valueOf(total));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearTables() {
        tableLayoutResults.removeViews(1, Math.max(0, tableLayoutResults.getChildCount() - 1));
        presentCount.setText("0");
        absentCount.setText("0");
        totalDays.setText("0");
    }
}
