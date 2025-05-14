package edu.penzgtu.taed;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.List;

public class JournalService extends Service {

    private static final String TAG = "JournalService";
    private final IBinder binder = new LocalBinder();
    private DBHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        dbHelper = new DBHelper(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service onUnbind");
        return true; // Разрешаем повторное подключение через onRebind
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "Service onRebind");
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        super.onDestroy();
        dbHelper.close();
    }

    /**
     * Процедура обработки данных: Выполняет поиск студентов по запросу в указанном журнале.
     * @param query Поисковый запрос.
     * @param journalId Идентификатор текущего журнала.
     * @return Результат поиска.
     */
    public SearchResult searchStudents(String query, long journalId) {
        Log.d(TAG, "searchStudents called with query: " + query + ", journalId: " + journalId);
        List<SearchResult.StudentData> students = dbHelper.searchStudentsByName(query, journalId);
        return new SearchResult(students);
    }

    public class LocalBinder extends Binder {
        JournalService getService() {
            return JournalService.this;
        }
    }
}