package com.rajesh.acetcse.ui.dashboard;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.*;
import com.rajesh.acetcse.R;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class AttendanceViewActivity extends AppCompatActivity {

    private Spinner spinnerClass;
    private EditText editDate;
    private LinearLayout attendanceListContainer;
    private Button btnLoad, btnDownload;

    private String selectedDateForDB = "";
    private final String DATABASE_URL = "https://security-systems-ecda3-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendance_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerClass = findViewById(R.id.spinnerClass);
        editDate = findViewById(R.id.editDate);
        btnLoad = findViewById(R.id.btnLoad);
        btnDownload = findViewById(R.id.btnDownload);
        attendanceListContainer = findViewById(R.id.attendanceDataText);

        // Spinner class options
        String[] classList = {"II-A", "II-B", "III-A", "III-B", "IV-A", "IV-B"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        editDate.setOnClickListener(v -> showDatePicker());
        btnLoad.setOnClickListener(v -> loadFilteredAttendance());
        btnDownload.setOnClickListener(v -> exportAttendanceToExcel(
                spinnerClass.getSelectedItem().toString(),
                editDate.getText().toString()
        ));
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dd = String.format("%02d", dayOfMonth);
                    String mm = String.format("%02d", month + 1);
                    String yyyy = String.valueOf(year);

                    editDate.setText(dd + "-" + mm + "-" + yyyy);
                    selectedDateForDB = dd + "-" + mm + "-" + yyyy;
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void loadFilteredAttendance() {
        String className = spinnerClass.getSelectedItem().toString();

        if (selectedDateForDB.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        attendanceListContainer.removeAllViews();

        DatabaseReference attendanceRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("students/attendance")
                .child(selectedDateForDB)
                .child(className);

        DatabaseReference studentListRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("students")
                .child(className);

        studentListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot studentListSnapshot) {
                attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot attendanceSnapshot) {
                        if (!attendanceSnapshot.exists()) {
                            TextView tv = new TextView(AttendanceViewActivity.this);
                            tv.setText("No records found for " + className + " on " + editDate.getText().toString());
                            tv.setPadding(16, 16, 16, 16);
                            attendanceListContainer.addView(tv);
                            return;
                        }

                        for (DataSnapshot student : attendanceSnapshot.getChildren()) {
                            String roll = student.getKey();
                            String status = student.getValue(String.class);

                            String name = "Unknown";
                            if (studentListSnapshot.hasChild(roll) && studentListSnapshot.child(roll).hasChild("name")) {
                                name = studentListSnapshot.child(roll).child("name").getValue(String.class);
                            }

                            TextView row = new TextView(AttendanceViewActivity.this);
                            row.setText(roll + " - " + name + ": " + status);
                            row.setTextSize(16);
                            row.setPadding(20, 12, 20, 12);

                            if ("Present".equalsIgnoreCase(status)) {
                                row.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            } else {
                                row.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            }

                            attendanceListContainer.addView(row);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AttendanceViewActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AttendanceViewActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportAttendanceToExcel(String className, String date) {
        if (attendanceListContainer.getChildCount() == 0) {
            Toast.makeText(this, "No attendance data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Attendance");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Roll No");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Status");

            for (int i = 0; i < attendanceListContainer.getChildCount(); i++) {
                TextView rowView = (TextView) attendanceListContainer.getChildAt(i);
                String[] parts = rowView.getText().toString().split(" - |: ");
                if (parts.length == 3) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(parts[0]);
                    row.createCell(1).setCellValue(parts[1]);
                    row.createCell(2).setCellValue(parts[2]);
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            workbook.close();

            String fileName = "Attendance_" + className + "_" + date + ".xlsx";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // API 29+ → Scoped Storage
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri fileUri = getContentResolver().insert(collection, values);

                try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                    os.write(bos.toByteArray());
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(fileUri, values, null, null);

            } else {
                // API 26-28 → Legacy storage (needs permission)
                java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                );
                java.io.File file = new java.io.File(downloadsDir, fileName);
                try (OutputStream os = new java.io.FileOutputStream(file)) {
                    os.write(bos.toByteArray());
                }
            }

            Toast.makeText(this, "Excel saved to Downloads", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
