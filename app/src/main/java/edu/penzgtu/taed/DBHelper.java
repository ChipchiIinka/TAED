package edu.penzgtu.taed;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "JournalDB";
    private static final int DATABASE_VERSION = 1;

    // Таблица для журналов
    private static final String TABLE_JOURNALS = "journals";
    private static final String COLUMN_JOURNAL_ID = "journal_id";
    private static final String COLUMN_JOURNAL_NAME = "journal_name";
    private static final String COLUMN_PAGE_COUNT = "page_count";
    private static final String COLUMN_GRADE_COUNT = "grade_count";
    private static final String COLUMN_STUDENT_COUNT = "student_count";

    // Таблица для вкладок (страниц журнала)
    private static final String TABLE_PAGES = "pages";
    private static final String COLUMN_PAGE_ID = "page_id";
    private static final String COLUMN_PAGE_NAME = "page_name";

    // Таблица для студентов
    private static final String TABLE_STUDENTS = "students";
    private static final String COLUMN_STUDENT_ID = "student_id";
    private static final String COLUMN_STUDENT_NAME = "student_name";
    private static final String COLUMN_STUDENT_NUMBER = "student_number";

    // Таблица для оценок
    private static final String TABLE_GRADES = "grades";
    private static final String COLUMN_GRADE_ID = "grade_id";
    private static final String COLUMN_GRADE_VALUE = "grade_value";
    private static final String COLUMN_GRADE_INDEX = "grade_index";

    // Таблица для зачетов, КП и экзаменов
    private static final String TABLE_EXAMS = "exams";
    private static final String COLUMN_EXAM_ID = "exam_id";
    private static final String COLUMN_CREDIT = "credit";
    private static final String COLUMN_CP = "cp";
    private static final String COLUMN_EXAM = "exam";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы журналов
        String createJournalsTable = "CREATE TABLE " + TABLE_JOURNALS + " (" +
                COLUMN_JOURNAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_JOURNAL_NAME + " TEXT, " +
                COLUMN_PAGE_COUNT + " INTEGER, " +
                COLUMN_GRADE_COUNT + " INTEGER, " +
                COLUMN_STUDENT_COUNT + " INTEGER)";
        db.execSQL(createJournalsTable);

        // Создание таблицы вкладок
        String createPagesTable = "CREATE TABLE " + TABLE_PAGES + " (" +
                COLUMN_PAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_JOURNAL_ID + " INTEGER, " +
                COLUMN_PAGE_NAME + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_JOURNAL_ID + ") REFERENCES " + TABLE_JOURNALS + "(" + COLUMN_JOURNAL_ID + "))";
        db.execSQL(createPagesTable);

        // Создание таблицы студентов
        String createStudentsTable = "CREATE TABLE " + TABLE_STUDENTS + " (" +
                COLUMN_STUDENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PAGE_ID + " INTEGER, " +
                COLUMN_STUDENT_NUMBER + " INTEGER, " +
                COLUMN_STUDENT_NAME + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_PAGE_ID + ") REFERENCES " + TABLE_PAGES + "(" + COLUMN_PAGE_ID + "))";
        db.execSQL(createStudentsTable);

        // Создание таблицы оценок
        String createGradesTable = "CREATE TABLE " + TABLE_GRADES + " (" +
                COLUMN_GRADE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_STUDENT_ID + " INTEGER, " +
                COLUMN_GRADE_INDEX + " INTEGER, " +
                COLUMN_GRADE_VALUE + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_STUDENT_ID + ") REFERENCES " + TABLE_STUDENTS + "(" + COLUMN_STUDENT_ID + "))";
        db.execSQL(createGradesTable);

        // Создание таблицы зачетов, КП и экзаменов
        String createExamsTable = "CREATE TABLE " + TABLE_EXAMS + " (" +
                COLUMN_EXAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_STUDENT_ID + " INTEGER, " +
                COLUMN_CREDIT + " TEXT, " +
                COLUMN_CP + " TEXT, " +
                COLUMN_EXAM + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_STUDENT_ID + ") REFERENCES " + TABLE_STUDENTS + "(" + COLUMN_STUDENT_ID + "))";
        db.execSQL(createExamsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXAMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRADES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JOURNALS);
        onCreate(db);
    }

    // Создание нового журнала
    public long createJournal(String journalName, int pageCount, int gradeCount, int studentCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_JOURNAL_NAME, journalName);
        values.put(COLUMN_PAGE_COUNT, pageCount);
        values.put(COLUMN_GRADE_COUNT, gradeCount);
        values.put(COLUMN_STUDENT_COUNT, studentCount);
        long journalId = db.insert(TABLE_JOURNALS, null, values);

        // Создание вкладок
        for (int i = 0; i < pageCount; i++) {
            String pageName = journalName + "_Page_" + (i + 1);
            ContentValues pageValues = new ContentValues();
            pageValues.put(COLUMN_JOURNAL_ID, journalId);
            pageValues.put(COLUMN_PAGE_NAME, pageName);
            long pageId = db.insert(TABLE_PAGES, null, pageValues);

            // Создание студентов
            for (int j = 0; j < studentCount; j++) {
                ContentValues studentValues = new ContentValues();
                studentValues.put(COLUMN_PAGE_ID, pageId);
                studentValues.put(COLUMN_STUDENT_NUMBER, j + 1);
                studentValues.put(COLUMN_STUDENT_NAME, "Студент " + (j + 1));
                long studentId = db.insert(TABLE_STUDENTS, null, studentValues);

                // Создание оценок
                for (int k = 0; k < gradeCount; k++) {
                    ContentValues gradeValues = new ContentValues();
                    gradeValues.put(COLUMN_STUDENT_ID, studentId);
                    gradeValues.put(COLUMN_GRADE_INDEX, k);
                    gradeValues.put(COLUMN_GRADE_VALUE, "-");
                    db.insert(TABLE_GRADES, null, gradeValues);
                }

                // Создание зачетов, КП и экзаменов
                ContentValues examValues = new ContentValues();
                examValues.put(COLUMN_STUDENT_ID, studentId);
                examValues.put(COLUMN_CREDIT, "-");
                examValues.put(COLUMN_CP, "-");
                examValues.put(COLUMN_EXAM, "-");
                db.insert(TABLE_EXAMS, null, examValues);
            }
        }
        db.close();
        return journalId;
    }

    // Получение данных журнала
    public Cursor getJournal(long journalId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_JOURNALS, null, COLUMN_JOURNAL_ID + "=?", new String[]{String.valueOf(journalId)}, null, null, null);
    }

    // Получение всех вкладок журнала
    public Cursor getPages(long journalId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PAGES, null, COLUMN_JOURNAL_ID + "=?", new String[]{String.valueOf(journalId)}, null, null, null);
    }

    // Получение всех студентов для вкладки
    public Cursor getStudents(long pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_STUDENTS, null, COLUMN_PAGE_ID + "=?", new String[]{String.valueOf(pageId)}, null, null, null);
    }

    // Получение всех оценок студента
    public Cursor getGrades(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_GRADES, null, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)}, null, null, COLUMN_GRADE_INDEX + " ASC");
    }

    // Получение зачетов, КП и экзаменов студента
    public Cursor getExams(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EXAMS, null, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)}, null, null, null);
    }

    // Обновление данных студента
    public void updateStudent(long studentId, String studentName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_NAME, studentName);
        db.update(TABLE_STUDENTS, values, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
        db.close();
    }

    // Обновление оценки
    public void updateGrade(long studentId, int gradeIndex, String gradeValue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_GRADE_VALUE, gradeValue);
        db.update(TABLE_GRADES, values, COLUMN_STUDENT_ID + "=? AND " + COLUMN_GRADE_INDEX + "=?",
                new String[]{String.valueOf(studentId), String.valueOf(gradeIndex)});
        db.close();
    }

    // Обновление зачетов, КП и экзаменов
    public void updateExams(long studentId, String credit, String cp, String exam) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CREDIT, credit);
        values.put(COLUMN_CP, cp);
        values.put(COLUMN_EXAM, exam);
        db.update(TABLE_EXAMS, values, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
        db.close();
    }

    // Удаление журнала
    public void deleteJournal(long journalId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Удаление связанных данных
        Cursor pages = getPages(journalId);
        while (pages.moveToNext()) {
            long pageId = pages.getLong(pages.getColumnIndexOrThrow(COLUMN_PAGE_ID));
            Cursor students = getStudents(pageId);
            while (students.moveToNext()) {
                long studentId = students.getLong(students.getColumnIndexOrThrow(COLUMN_STUDENT_ID));
                db.delete(TABLE_GRADES, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
                db.delete(TABLE_EXAMS, COLUMN_STUDENT_ID + "=?", new String[]{String.valueOf(studentId)});
            }
            students.close();
            db.delete(TABLE_STUDENTS, COLUMN_PAGE_ID + "=?", new String[]{String.valueOf(pageId)});
        }
        pages.close();
        db.delete(TABLE_PAGES, COLUMN_JOURNAL_ID + "=?", new String[]{String.valueOf(journalId)});
        db.delete(TABLE_JOURNALS, COLUMN_JOURNAL_ID + "=?", new String[]{String.valueOf(journalId)});
        db.close();
    }

    // Получение последнего созданного журнала
    public Cursor getLastJournal() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_JOURNALS, null, null, null, null, null, COLUMN_JOURNAL_ID + " DESC", "1");
    }
}