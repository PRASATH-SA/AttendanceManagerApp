package com.rajesh.acetcse;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class IIAActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnSubmit;
    private TextView classTitle;

    private String className;
    private final ArrayList<String> studentNames = new ArrayList<>();
    private final ArrayList<String> studentRolls = new ArrayList<>();

    private final String DATABASE_URL = "https://security-systems-ecda3-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iiaactivity);

        className = getIntent().getStringExtra("className");
        if (className == null || className.isEmpty()) {
            Toast.makeText(this, "Class not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle("Attendance - " + className);
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        listView = findViewById(R.id.listViewStudents);
        btnSubmit = findViewById(R.id.btnSubmitAttendance);
        classTitle = findViewById(R.id.classTitle);
        classTitle.setText("Mark Attendance for "+date);

        loadStudentData();

        btnSubmit.setOnClickListener(v -> submitAttendance());
    }

    private void loadStudentData() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("students")
                .child(className);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentNames.clear();
                studentRolls.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    String roll = child.child("roll").getValue(String.class);
                    if (name != null && roll != null) {
                        studentNames.add(name + " (" + roll + ")");
                        studentRolls.add(roll);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        IIAActivity.this,
                        android.R.layout.simple_list_item_multiple_choice,
                        studentNames
                );
                listView.setAdapter(adapter);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(IIAActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAttendance() {
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        DatabaseReference dbRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("students/attendance")
                .child(date)
                .child(className);
        Toast.makeText(IIAActivity.this, "Success",Toast.LENGTH_SHORT).show();

        // Step 1: Check if attendance already exists
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Step 2: Show confirmation dialog to edit
                    new AlertDialog.Builder(IIAActivity.this)
                            .setTitle("Attendance Already Marked")
                            .setMessage("Attendance for today is already recorded.\nDo you want to overwrite it?")
                            .setPositiveButton("Yes", (dialog, which) -> saveAttendance(dbRef))
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    // No attendance yet – save directly
                    saveAttendance(dbRef);
                }
            }
            private void saveAttendance(DatabaseReference dbRef) {
                HashMap<String, String> attendanceMap = new HashMap<>();

                for (int i = 0; i < studentRolls.size(); i++) {
                    String roll = studentRolls.get(i);
                    boolean isPresent = listView.isItemChecked(i);
                    attendanceMap.put(roll, isPresent ? "Present" : "Absent");
                }

                dbRef.setValue(attendanceMap)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(IIAActivity.this, "Attendance Submitted", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(IIAActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(IIAActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
