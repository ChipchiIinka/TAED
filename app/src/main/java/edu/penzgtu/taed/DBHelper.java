package edu.penzgtu.taed;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "taed.db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE journals (journal_id INTEGER PRIMARY KEY AUTOINCREMENT, journal_name TEXT, page_count INTEGER, grade_count INTEGER, student_count INTEGER)");
        db.execSQL("CREATE TABLE pages (page_id INTEGER PRIMARY KEY AUTOINCREMENT, journal_id INTEGER, page_name TEXT)");
        db.execSQL("CREATE TABLE students (student_id INTEGER PRIMARY KEY AUTOINCREMENT, page_id INTEGER, student_number INTEGER, student_name TEXT)");
        db.execSQL("CREATE TABLE grades (grade_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, grade_index INTEGER, grade_value TEXT)");
        db.execSQL("CREATE TABLE exams (exam_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, credit TEXT, cp TEXT, exam TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS journals");
        db.execSQL("DROP TABLE IF EXISTS pages");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS grades");
        db.execSQL("DROP TABLE IF EXISTS exams");
        onCreate(db);
    }

    public long createJournal(String journalName, int pageCount, int gradeCount, int studentCount) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("journal_name", journalName);
        values.put("page_count", pageCount);
        values.put("grade_count", gradeCount);
        values.put("student_count", studentCount);
        long journalId = db.insert("journals", null, values);

        for (int i = 0; i < pageCount; i++) {
            ContentValues pageValues = new ContentValues();
            pageValues.put("journal_id", journalId);
            pageValues.put("page_name", "Группа " + (i + 1));
            long pageId = db.insert("pages", null, pageValues);

            for (int j = 0; j < studentCount; j++) {
                ContentValues studentValues = new ContentValues();
                studentValues.put("page_id", pageId);
                studentValues.put("student_number", j + 1);
                studentValues.put("student_name", "Студент " + (j + 1));
                long studentId = db.insert("students", null, studentValues);

                for (int k = 0; k < gradeCount; k++) {
                    ContentValues gradeValues = new ContentValues();
                    gradeValues.put("student_id", studentId);
                    gradeValues.put("grade_index", k);
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
        }
        return journalId;
    }

    public Cursor getLastJournal() {
        return getReadableDatabase().rawQuery("SELECT * FROM journals ORDER BY journal_id DESC LIMIT 1", null);
    }

    public Cursor getPages(long journalId) {
        return getReadableDatabase().rawQuery("SELECT * FROM pages WHERE journal_id = ?", new String[]{String.valueOf(journalId)});
    }

    public Cursor getStudents(long pageId) {
        return getReadableDatabase().rawQuery("SELECT * FROM students WHERE page_id = ? ORDER BY student_number", new String[]{String.valueOf(pageId)});
    }

    public Cursor getGrades(long studentId) {
        return getReadableDatabase().rawQuery("SELECT * FROM grades WHERE student_id = ? ORDER BY grade_index", new String[]{String.valueOf(studentId)});
    }

    public Cursor getExams(long studentId) {
        return getReadableDatabase().rawQuery("SELECT * FROM exams WHERE student_id = ?", new String[]{String.valueOf(studentId)});
    }

    public Cursor getJournal(long journalId) {
        return getReadableDatabase().rawQuery("SELECT * FROM journals WHERE journal_id = ?", new String[]{String.valueOf(journalId)});
    }

    public void updateStudent(long studentId, String name) {
        ContentValues values = new ContentValues();
        values.put("student_name", name);
        getWritableDatabase().update("students", values, "student_id = ?", new String[]{String.valueOf(studentId)});
    }

    public void updateGrade(long studentId, int index, String value) {
        ContentValues values = new ContentValues();
        values.put("grade_value", value);
        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update("grades", values, "student_id = ? AND grade_index = ?", new String[]{String.valueOf(studentId), String.valueOf(index)});
        if (updated == 0) {
            values.put("student_id", studentId);
            values.put("grade_index", index);
            db.insert("grades", null, values);
        }
    }

    public void updateExams(long studentId, String credit, String cp, String exam) {
        ContentValues values = new ContentValues();
        values.put("credit", credit);
        values.put("cp", cp);
        values.put("exam", exam);
        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update("exams", values, "student_id = ?", new String[]{String.valueOf(studentId)});
        if (updated == 0) {
            values.put("student_id", studentId);
            db.insert("exams", null, values);
        }
    }

    public void deleteJournal(long journalId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("grades", "student_id IN (SELECT student_id FROM students WHERE page_id IN (SELECT page_id FROM pages WHERE journal_id=?))", new String[]{String.valueOf(journalId)});
        db.delete("exams", "student_id IN (SELECT student_id FROM students WHERE page_id IN (SELECT page_id FROM pages WHERE journal_id=?))", new String[]{String.valueOf(journalId)});
        db.delete("students", "page_id IN (SELECT page_id FROM pages WHERE journal_id=?)", new String[]{String.valueOf(journalId)});
        db.delete("pages", "journal_id=?", new String[]{String.valueOf(journalId)});
        db.delete("journals", "journal_id=?", new String[]{String.valueOf(journalId)});
    }

    public String getJournalDataForExport() {
        StringBuilder csv = new StringBuilder();
        Cursor journalCursor = getLastJournal();
        if (!journalCursor.moveToFirst()) {
            journalCursor.close();
            return "";
        }

        long journalId = journalCursor.getLong(journalCursor.getColumnIndexOrThrow("journal_id"));
        String journalName = journalCursor.getString(journalCursor.getColumnIndexOrThrow("journal_name"));
        int gradeCount = journalCursor.getInt(journalCursor.getColumnIndexOrThrow("grade_count"));
        journalCursor.close();

        csv.append("Journal Name,").append(escapeCsv(journalName)).append("\n\n");

        Cursor pagesCursor = getPages(journalId);
        while (pagesCursor.moveToNext()) {
            long pageId = pagesCursor.getLong(pagesCursor.getColumnIndexOrThrow("page_id"));
            String pageName = pagesCursor.getString(pagesCursor.getColumnIndexOrThrow("page_name"));

            csv.append("Page,").append(escapeCsv(pageName)).append("\n");
            csv.append("Student Number,Student Name");

            for (int i = 0; i < gradeCount; i++) {
                csv.append(",Grade ").append(i + 1);
            }
            csv.append(",Credit,CP,Exam\n");

            Cursor studentsCursor = getStudents(pageId);
            while (studentsCursor.moveToNext()) {
                long studentId = studentsCursor.getLong(studentsCursor.getColumnIndexOrThrow("student_id"));
                int studentNumber = studentsCursor.getInt(studentsCursor.getColumnIndexOrThrow("student_number"));
                String studentName = studentsCursor.getString(studentsCursor.getColumnIndexOrThrow("student_name"));

                csv.append(studentNumber).append(",").append(escapeCsv(studentName));

                Cursor gradesCursor = getGrades(studentId);
                for (int i = 0; i < gradeCount; i++) {
                    String gradeValue = "-";
                    if (gradesCursor.moveToPosition(i)) {
                        gradeValue = gradesCursor.getString(gradesCursor.getColumnIndexOrThrow("grade_value"));
                    }
                    csv.append(",").append(escapeCsv(gradeValue));
                }
                gradesCursor.close();

                Cursor examsCursor = getExams(studentId);
                String credit = "-", cp = "-", exam = "-";
                if (examsCursor.moveToFirst()) {
                    credit = examsCursor.getString(examsCursor.getColumnIndexOrThrow("credit"));
                    cp = examsCursor.getString(examsCursor.getColumnIndexOrThrow("cp"));
                    exam = examsCursor.getString(examsCursor.getColumnIndexOrThrow("exam"));
                }
                examsCursor.close();
                csv.append(",").append(escapeCsv(credit)).append(",").append(escapeCsv(cp)).append(",").append(escapeCsv(exam)).append("\n");
            }
            studentsCursor.close();
            csv.append("\n");
        }
        pagesCursor.close();
        return csv.toString();
    }

    public long importJournalFromCsv(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String journalName = null;
            int gradeCount = 0, studentCount = 0;
            List<String> pageNames = new ArrayList<>();
            List<List<String[]>> pagesData = new ArrayList<>();
            List<String[]> currentPageData = null;
            boolean isHeader = false;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = parseCsvLine(line);
                if (parts.length == 0) continue;

                if (parts[0].equals("Journal Name")) {
                    journalName = parts.length > 1 ? parts[1] : "";
                } else if (parts[0].equals("Page")) {
                    pageNames.add(parts.length > 1 ? parts[1] : "Группа " + (pageNames.size() + 1));
                    currentPageData = new ArrayList<>();
                    pagesData.add(currentPageData);
                    isHeader = true;
                } else if (isHeader && parts[0].equals("Student Number")) {
                    gradeCount = parts.length - 5;
                    isHeader = false;
                } else if (currentPageData != null && parts[0].matches("\\d+")) {
                    currentPageData.add(parts);
                    studentCount = Math.max(studentCount, currentPageData.size());
                }
            }
            reader.close();

            if (journalName == null || pageNames.isEmpty() || gradeCount <= 0 || studentCount <= 0) {
                return -1;
            }

            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues journalValues = new ContentValues();
                journalValues.put("journal_name", journalName);
                journalValues.put("page_count", pageNames.size());
                journalValues.put("grade_count", gradeCount);
                journalValues.put("student_count", studentCount);
                long journalId = db.insert("journals", null, journalValues);

                for (int i = 0; i < pageNames.size(); i++) {
                    ContentValues pageValues = new ContentValues();
                    pageValues.put("journal_id", journalId);
                    pageValues.put("page_name", pageNames.get(i));
                    long pageId = db.insert("pages", null, pageValues);

                    List<String[]> pageData = pagesData.get(i);
                    for (String[] studentData : pageData) {
                        int studentNumber = Integer.parseInt(studentData[0]);
                        String studentName = studentData[1];

                        ContentValues studentValues = new ContentValues();
                        studentValues.put("page_id", pageId);
                        studentValues.put("student_number", studentNumber);
                        studentValues.put("student_name", studentName);
                        long studentId = db.insert("students", null, studentValues);

                        for (int j = 0; j < gradeCount; j++) {
                            String gradeValue = j + 2 < studentData.length ? studentData[j + 2] : "-";
                            ContentValues gradeValues = new ContentValues();
                            gradeValues.put("student_id", studentId);
                            gradeValues.put("grade_index", j);
                            gradeValues.put("grade_value", gradeValue);
                            db.insert("grades", null, gradeValues);
                        }

                        String credit = studentData.length > gradeCount + 2 ? studentData[gradeCount + 2] : "-";
                        String cp = studentData.length > gradeCount + 3 ? studentData[gradeCount + 3] : "-";
                        String exam = studentData.length > gradeCount + 4 ? studentData[gradeCount + 4] : "-";
                        ContentValues examValues = new ContentValues();
                        examValues.put("student_id", studentId);
                        examValues.put("credit", credit);
                        examValues.put("cp", cp);
                        examValues.put("exam", exam);
                        db.insert("exams", null, examValues);
                    }
                }

                db.setTransactionSuccessful();
                return journalId;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Процедура обработки данных: Выполняет поиск студентов по имени в указанном журнале.
     * @param query Поисковый запрос.
     * @param journalId Идентификатор текущего журнала.
     * @return Список студентов, соответствующих запросу.
     */
    public List<SearchResult.StudentData> searchStudentsByName(String query, long journalId) {
        List<SearchResult.StudentData> results = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT s.student_id, s.student_name, p.page_name " +
                "FROM students s " +
                "JOIN pages p ON s.page_id = p.page_id " +
                "WHERE s.student_name LIKE ? AND p.journal_id = ? " +
                "ORDER BY s.student_name";
        Cursor cursor = db.rawQuery(sql, new String[]{"%" + query + "%", String.valueOf(journalId)});
        while (cursor.moveToNext()) {
            long studentId = cursor.getLong(cursor.getColumnIndexOrThrow("student_id"));
            String studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name"));
            String pageName = cursor.getString(cursor.getColumnIndexOrThrow("page_name"));
            results.add(new SearchResult.StudentData(studentId, studentName, pageName));
        }
        cursor.close();
        return results;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}