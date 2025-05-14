package edu.penzgtu.taed.ui.home;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import edu.penzgtu.taed.DBHelper;
import edu.penzgtu.taed.R;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout1;
    private TableLayout tableLayout3;
    private TableLayout tableLayout4;
    private TableLayout tableLayout5;
    private EditText disciplineEditText;
    private DBHelper dbHelper;
    private long currentJournalId = -1;
    private int currentTabPosition = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация компонентов
        tabLayout1 = view.findViewById(R.id.tabLayout1);
        disciplineEditText = view.findViewById(R.id.disciplineEditText);
        view.findViewById(R.id.tableLayout2);
        view.findViewById(R.id.mainScrollView);
        view.findViewById(R.id.horizontalScrollView3);
        tableLayout3 = view.findViewById(R.id.tableLayout3);
        view.findViewById(R.id.tableLayout4Header);
        view.findViewById(R.id.horizontalScrollView4);
        tableLayout4 = view.findViewById(R.id.tableLayout4);
        view.findViewById(R.id.tableLayout5Header);
        view.findViewById(R.id.horizontalScrollView5);
        tableLayout5 = view.findViewById(R.id.tableLayout5);
        Button addTabButton = view.findViewById(R.id.addTabButton);
        Button addRowButton = view.findViewById(R.id.addRowButton);

        // Загрузка последнего журнала
        loadLastJournal();

        // Обработчик кнопки добавления вкладки
        addTabButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Новая вкладка");
            final EditText input = new EditText(getContext());
            input.setHint("Название вкладки");
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                String tabName = input.getText().toString().trim();
                if (!tabName.isEmpty()) {
                    addNewTab(tabName);
                }
            });
            builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // Обработчик выбора вкладки
        tabLayout1.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                updateTableContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                clearTableContent();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Переименовать вкладку");
                final EditText input = new EditText(getContext());
                input.setText(tab.getText());
                builder.setView(input);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        tab.setText(newName);
                        long pageId = (long) tab.view.getTag();
                        dbHelper.getWritableDatabase().execSQL(
                                "UPDATE pages SET page_name = ? WHERE page_id = ?",
                                new Object[]{newName, pageId}
                        );
                    }
                });
                builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
                builder.show();
            }
        });

        // Обработчик добавления новой строки
        addRowButton.setOnClickListener(v -> addNewRow());

        return view;
    }

    // Добавление новой вкладки
    private void addNewTab(String tabName) {
        if (currentJournalId == -1) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("journal_id", currentJournalId);
        values.put("page_name", tabName);
        long pageId = db.insert("pages", null, values);

        // Добавление студентов из текущего журнала
        Cursor journalCursor = dbHelper.getJournal(currentJournalId);
        int studentCount = 0, gradeCount = 0;
        if (journalCursor.moveToFirst()) {
            studentCount = journalCursor.getInt(journalCursor.getColumnIndexOrThrow("student_count"));
            gradeCount = journalCursor.getInt(journalCursor.getColumnIndexOrThrow("grade_count"));
        }
        journalCursor.close();

        for (int i = 0; i < studentCount; i++) {
            ContentValues studentValues = new ContentValues();
            studentValues.put("page_id", pageId);
            studentValues.put("student_number", i + 1);
            studentValues.put("student_name", "Студент " + (i + 1));
            long studentId = db.insert("students", null, studentValues);

            for (int j = 0; j < gradeCount; j++) {
                ContentValues gradeValues = new ContentValues();
                gradeValues.put("student_id", studentId);
                gradeValues.put("grade_index", j);
                gradeValues.put("grade_value", "-");
                db.insert("grades", null, gradeValues);
            }

            ContentValues examValues = new ContentValues();
            examValues.put("student_id", studentId);
            examValues.put("credit", "-");
            examValues.put("cp", "-");
            examValues.put("exam", "-");
            db.insert("exams", null, examValues);
        }

        TabLayout.Tab newTab = tabLayout1.newTab();
        newTab.setText(tabName);
        newTab.view.setTag(pageId);
        tabLayout1.addTab(newTab);
        newTab.view.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удалить вкладку");
            builder.setMessage("Вы уверены, что хотите удалить вкладку \"" + newTab.getText() + "\"?");
            builder.setPositiveButton("Да", (dialog, which) -> {
                long pageIdToDelete = (long) newTab.view.getTag();
                db.delete("grades", "student_id IN (SELECT student_id FROM students WHERE page_id=?)", new String[]{String.valueOf(pageIdToDelete)});
                db.delete("exams", "student_id IN (SELECT student_id FROM students WHERE page_id=?)", new String[]{String.valueOf(pageIdToDelete)});
                db.delete("students", "page_id=?", new String[]{String.valueOf(pageIdToDelete)});
                db.delete("pages", "page_id=?", new String[]{String.valueOf(pageIdToDelete)});
                tabLayout1.removeTab(newTab);
                if (tabLayout1.getTabCount() > 0) {
                    Objects.requireNonNull(tabLayout1.getTabAt(0)).select();
                } else {
                    clearTableContent();
                }
            });
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.cancel());
            builder.show();
            return true;
        });
        db.close();
    }

    // Загрузка последнего журнала
    private void loadLastJournal() {
        Cursor journalCursor = dbHelper.getLastJournal();
        if (journalCursor.moveToFirst()) {
            currentJournalId = journalCursor.getLong(journalCursor.getColumnIndexOrThrow("journal_id"));
            String journalName = journalCursor.getString(journalCursor.getColumnIndexOrThrow("journal_name"));
            disciplineEditText.setText(journalName);
            loadTabs();
            updateTableContent();
        }
        journalCursor.close();
    }

    // Загрузка вкладок журнала
    private void loadTabs() {
        tabLayout1.removeAllTabs();
        Cursor pagesCursor = dbHelper.getPages(currentJournalId);
        while (pagesCursor.moveToNext()) {
            long pageId = pagesCursor.getLong(pagesCursor.getColumnIndexOrThrow("page_id"));
            String pageName = pagesCursor.getString(pagesCursor.getColumnIndexOrThrow("page_name"));
            TabLayout.Tab newTab = tabLayout1.newTab();
            newTab.setText(pageName);
            newTab.view.setTag(pageId);
            tabLayout1.addTab(newTab);
            newTab.view.setOnLongClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Удалить вкладку");
                builder.setMessage("Вы уверены, что хотите удалить вкладку \"" + newTab.getText() + "\"?");
                builder.setPositiveButton("Да", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("grades", "student_id IN (SELECT student_id FROM students WHERE page_id=?)", new String[]{String.valueOf(pageId)});
                    db.delete("exams", "student_id IN (SELECT student_id FROM students WHERE page_id=?)", new String[]{String.valueOf(pageId)});
                    db.delete("students", "page_id=?", new String[]{String.valueOf(pageId)});
                    db.delete("pages", "page_id=?", new String[]{String.valueOf(pageId)});
                    tabLayout1.removeTab(newTab);
                    if (tabLayout1.getTabCount() > 0) {
                        Objects.requireNonNull(tabLayout1.getTabAt(0)).select();
                    } else {
                        clearTableContent();
                    }
                    db.close();
                });
                builder.setNegativeButton("Нет", (dialog, which) -> dialog.cancel());
                builder.show();
                return true;
            });
        }
        pagesCursor.close();
        if (tabLayout1.getTabCount() > 0) {
            Objects.requireNonNull(tabLayout1.getTabAt(0)).select();
        }
    }

    // Очистка содержимого таблиц
    private void clearTableContent() {
        tableLayout3.removeAllViews();
        tableLayout4.removeAllViews();
        tableLayout5.removeAllViews();
    }

    // Создание разделителя
    private View createSpacer() {
        View spacer = new View(getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                (int) (8 * getResources().getDisplayMetrics().density)
        );
        spacer.setLayoutParams(params);
        return spacer;
    }

    // Добавление новой строки
    private void addNewRow() {
        if (currentJournalId == -1 || tabLayout1.getTabCount() == 0) return;

        long pageId = (long) Objects.requireNonNull(tabLayout1.getTabAt(currentTabPosition)).view.getTag();
        Cursor journalCursor = dbHelper.getJournal(currentJournalId);
        int gradeCount = 0;
        if (journalCursor.moveToFirst()) {
            gradeCount = journalCursor.getInt(journalCursor.getColumnIndexOrThrow("grade_count"));
        }
        journalCursor.close();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int studentNumber = tableLayout3.getChildCount() + 1;
        ContentValues studentValues = new ContentValues();
        studentValues.put("page_id", pageId);
        studentValues.put("student_number", studentNumber);
        studentValues.put("student_name", "Студент " + studentNumber);
        long studentId = db.insert("students", null, studentValues);

        for (int i = 0; i < gradeCount; i++) {
            ContentValues gradeValues = new ContentValues();
            gradeValues.put("student_id", studentId);
            gradeValues.put("grade_index", i);
            gradeValues.put("grade_value", "-");
            db.insert("grades", null, gradeValues);
        }

        ContentValues examValues = new ContentValues();
        examValues.put("student_id", studentId);
        examValues.put("credit", "-");
        examValues.put("cp", "-");
        examValues.put("exam", "-");
        db.insert("exams", null, examValues);

        // Добавление строки в tableLayout3 (№ и ФИО)
        TableRow row3 = new TableRow(getContext());
        ImageButton deleteButton = getImageDelButton(row3, studentId);
        row3.addView(deleteButton);

        EditText number = getEditText(String.valueOf(studentNumber), 0);
        row3.addView(number);

        EditText name = getEditText("Студент " + studentNumber, 1);
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                dbHelper.updateStudent(studentId, s.toString());
            }
        });
        row3.addView(name);
        tableLayout3.addView(row3);

        // Добавление строки в tableLayout4 (оценки)
        TableRow row4 = new TableRow(getContext());
        Space space = new Space(getContext());
        space.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
        row4.addView(space);

        for (int i = 0; i < gradeCount; i++) {
            EditText grade = getEditText("-", i);
            final int index = i;
            grade.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    dbHelper.updateGrade(studentId, index, s.toString());
                }
            });
            row4.addView(grade);
        }
        tableLayout4.addView(row4);
        tableLayout4.addView(createSpacer());

        // Добавление строки в tableLayout5 (Зач КП ЭКЗ)
        TableRow row5 = new TableRow(getContext());
        Space space5 = new Space(getContext());
        space5.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
        row5.addView(space5);

        EditText creditEdit = getEditText("-", 0);
        creditEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Cursor exams = dbHelper.getExams(studentId);
                if (exams.moveToFirst()) {
                    dbHelper.updateExams(studentId, s.toString(),
                            exams.getString(exams.getColumnIndexOrThrow("cp")),
                            exams.getString(exams.getColumnIndexOrThrow("exam")));
                }
                exams.close();
            }
        });
        row5.addView(creditEdit);

        EditText cpEdit = getEditText("-", 1);
        cpEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Cursor exams = dbHelper.getExams(studentId);
                if (exams.moveToFirst()) {
                    dbHelper.updateExams(studentId,
                            exams.getString(exams.getColumnIndexOrThrow("credit")),
                            s.toString(),
                            exams.getString(exams.getColumnIndexOrThrow("exam")));
                }
                exams.close();
            }
        });
        row5.addView(cpEdit);

        EditText examEdit = getEditText("-", 2);
        examEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Cursor exams = dbHelper.getExams(studentId);
                if (exams.moveToFirst()) {
                    dbHelper.updateExams(studentId,
                            exams.getString(exams.getColumnIndexOrThrow("credit")),
                            exams.getString(exams.getColumnIndexOrThrow("cp")),
                            s.toString());
                }
                exams.close();
            }
        });
        row5.addView(examEdit);

        tableLayout5.addView(row5);
        tableLayout5.addView(createSpacer());
        db.close();
    }

    // Удаление строки
    private void deleteRow(long studentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("grades", "student_id=?", new String[]{String.valueOf(studentId)});
        db.delete("exams", "student_id=?", new String[]{String.valueOf(studentId)});
        db.delete("students", "student_id=?", new String[]{String.valueOf(studentId)});
        db.close();
        updateTableContent();
    }

    // Обновление содержимого таблиц
    private void updateTableContent() {
        clearTableContent();
        if (currentJournalId == -1 || tabLayout1.getTabCount() == 0) return;

        long pageId = (long) Objects.requireNonNull(tabLayout1.getTabAt(currentTabPosition)).view.getTag();
        Cursor studentsCursor = dbHelper.getStudents(pageId);
        Cursor journalCursor = dbHelper.getJournal(currentJournalId);
        int gradeCount = 0;
        if (journalCursor.moveToFirst()) {
            gradeCount = journalCursor.getInt(journalCursor.getColumnIndexOrThrow("grade_count"));
        }
        journalCursor.close();

        while (studentsCursor.moveToNext()) {
            long studentId = studentsCursor.getLong(studentsCursor.getColumnIndexOrThrow("student_id"));
            int studentNumber = studentsCursor.getInt(studentsCursor.getColumnIndexOrThrow("student_number"));
            String studentName = studentsCursor.getString(studentsCursor.getColumnIndexOrThrow("student_name"));

            // Добавление строки в tableLayout3 (№ и ФИО)
            TableRow row3 = new TableRow(getContext());
            ImageButton deleteButton = getImageDelButton(row3, studentId);
            row3.addView(deleteButton);

            EditText number = getEditText(String.valueOf(studentNumber), 0);
            row3.addView(number);

            EditText name = getEditText(studentName, 1);
            name.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    dbHelper.updateStudent(studentId, s.toString());
                }
            });
            row3.addView(name);
            tableLayout3.addView(row3);

            // Добавление строки в tableLayout4 (оценки)
            TableRow row4 = new TableRow(getContext());
            Space space = new Space(getContext());
            space.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
            row4.addView(space);

            Cursor gradesCursor = dbHelper.getGrades(studentId);
            for (int i = 0; i < gradeCount; i++) {
                String gradeValue = "-";
                if (gradesCursor.moveToPosition(i)) {
                    gradeValue = gradesCursor.getString(gradesCursor.getColumnIndexOrThrow("grade_value"));
                }
                EditText grade = getEditText(gradeValue, i);
                final int index = i;
                grade.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        dbHelper.updateGrade(studentId, index, s.toString());
                    }
                });
                row4.addView(grade);
            }
            gradesCursor.close();
            tableLayout4.addView(row4);
            tableLayout4.addView(createSpacer());

            // Добавление строки в tableLayout5 (Зач КП ЭКЗ)
            TableRow row5 = new TableRow(getContext());
            Space space5 = new Space(getContext());
            space5.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
            row5.addView(space5);

            Cursor examsCursor = dbHelper.getExams(studentId);
            String credit = "-", cp = "-", exam = "-";
            if (examsCursor.moveToFirst()) {
                credit = examsCursor.getString(examsCursor.getColumnIndexOrThrow("credit"));
                cp = examsCursor.getString(examsCursor.getColumnIndexOrThrow("cp"));
                exam = examsCursor.getString(examsCursor.getColumnIndexOrThrow("exam"));
            }
            examsCursor.close();

            EditText creditEdit = getEditText(credit, 0);
            creditEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    Cursor exams = dbHelper.getExams(studentId);
                    if (exams.moveToFirst()) {
                        dbHelper.updateExams(studentId, s.toString(),
                                exams.getString(exams.getColumnIndexOrThrow("cp")),
                                exams.getString(exams.getColumnIndexOrThrow("exam")));
                    }
                    exams.close();
                }
            });
            row5.addView(creditEdit);

            EditText cpEdit = getEditText(cp, 1);
            cpEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    Cursor exams = dbHelper.getExams(studentId);
                    if (exams.moveToFirst()) {
                        dbHelper.updateExams(studentId,
                                exams.getString(exams.getColumnIndexOrThrow("credit")),
                                s.toString(),
                                exams.getString(exams.getColumnIndexOrThrow("exam")));
                    }
                    exams.close();
                }
            });
            row5.addView(cpEdit);

            EditText examEdit = getEditText(exam, 2);
            examEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    Cursor exams = dbHelper.getExams(studentId);
                    if (exams.moveToFirst()) {
                        dbHelper.updateExams(studentId,
                                exams.getString(exams.getColumnIndexOrThrow("credit")),
                                exams.getString(exams.getColumnIndexOrThrow("cp")),
                                s.toString());
                    }
                    exams.close();
                }
            });
            row5.addView(examEdit);

            tableLayout5.addView(row5);
            tableLayout5.addView(createSpacer());
        }
        studentsCursor.close();
    }

    @NonNull
    private ImageButton getImageDelButton(TableRow row3, long studentId) {
        ImageButton deleteButton = new ImageButton(getContext());
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        deleteButton.setBackgroundColor(0x00FFFFFF);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удалить строку");
            builder.setMessage("Вы уверены, что хотите удалить эту строку?");
            builder.setPositiveButton("Да", (dialog, which) -> deleteRow(studentId));
            builder.setNegativeButton("Нет", (dialog, which) -> dialog.cancel());
            builder.show();
        });
        return deleteButton;
    }

    @NonNull
    private EditText getEditText(String text, int index) {
        EditText editText = new EditText(getContext());
        editText.setText(text);
        editText.setTextColor(0xFF000000);
        editText.setPadding(8, 8, 8, 8);
        editText.setBackgroundColor(index % 2 == 0 ? 0xFFE0F7FA : 0xFFFFFFFF);
        editText.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        editText.setSingleLine(true);
        return editText;
    }

    public void setJournal(long journalId) {
        if (currentJournalId != -1) {
            dbHelper.deleteJournal(currentJournalId);
        }
        currentJournalId = journalId;
        loadLastJournal();
    }
}