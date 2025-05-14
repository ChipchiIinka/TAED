package edu.penzgtu.taed;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import edu.penzgtu.taed.databinding.ActivityMainBinding;
import edu.penzgtu.taed.ui.home.HomeFragment;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private ActivityResultLauncher<Intent> loadFileLauncher;
    private DBHelper dbHelper;
    private JournalService journalService;
    private boolean isServiceBound = false;
    private ServiceConnection serviceConnection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler();
    private long currentJournalId = -1; // Храним ID текущего журнала

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        drawerLayout = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Инициализация DBHelper
        dbHelper = new DBHelper(this);

        // Настройка ActionBarDrawerToggle для кнопки "три тире"
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Динамическое добавление пунктов меню в Navigation Drawer
        Menu menu = navigationView.getMenu();
        String[] dynamicItems = {"Динамический пункт 1", "Динамический пункт 2", "Динамический пункт 3"};
        for (int i = 0; i < dynamicItems.length; i++) {
            menu.add(R.id.nav_dynamic_group, Menu.NONE, i, dynamicItems[i])
                    .setIcon(R.drawable.ic_menu_dynamic);
        }

        // Настройка навигации
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_journal)
                .setOpenableLayout(drawerLayout)
                .build();

        // Получение NavController через NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        } else {
            throw new IllegalStateException("NavHostFragment not found");
        }

        // Обработка кликов по пунктам Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                navController.navigate(R.id.nav_home);
            } else if (item.getItemId() == R.id.nav_journal) {
                navController.navigate(R.id.nav_journal);
            } else {
                Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Инициализация SAF для сохранения файла
        saveFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    saveJournalToCsv(uri);
                }
            }
        });

        // Инициализация SAF для загрузки файла
        loadFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    loadJournalFromCsv(uri);
                } else {
                    Toast.makeText(this, "Не удалось получить файл", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Выбор файла отменен", Toast.LENGTH_SHORT).show();
            }
        });

        // Инициализация ServiceConnection
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Service connected");
                JournalService.LocalBinder binder = (JournalService.LocalBinder) service;
                journalService = binder.getService();
                isServiceBound = true;
                Toast.makeText(MainActivity.this, "Подключено к службе", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Service disconnected");
                isServiceBound = false;
                journalService = null;
            }
        };

        // Устанавливаем текущий journalId (например, последний загруженный журнал)
        updateCurrentJournalId();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int itemId = item.getItemId();
        Log.d(TAG, "Menu item selected: " + itemId);
        if (itemId == R.id.action_create_journal) {
            navController.navigate(R.id.nav_journal);
            return true;
        } else if (itemId == R.id.action_save_file) {
            startSaveFile();
            return true;
        } else if (itemId == R.id.action_load_file) {
            startLoadFile();
            return true;
        } else if (itemId == R.id.action_edit_file) {
            Toast.makeText(this, "Редактировать файл", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_create_schedule) {
            Toast.makeText(this, "Создать расписание", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_edit_schedule) {
            Toast.makeText(this, "Редактировать расписание", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_load_students) {
            Toast.makeText(this, "Загрузить студентов", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_share_email) {
            Toast.makeText(this, "Поделиться по email", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_search) {
            Log.d(TAG, "Search menu item triggered");
            startSearch();
            return true;
        } else if (itemId == R.id.action_about) {
            stopService();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void startSaveFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "journal_export.csv");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            saveFileLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при открытии диалога сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveJournalToCsv(Uri uri) {
        try {
            String csvContent = dbHelper.getJournalDataForExport();
            if (csvContent.isEmpty()) {
                Toast.makeText(this, "Журнал пуст, сохранение невозможно", Toast.LENGTH_SHORT).show();
                return;
            }
            getContentResolver().openOutputStream(uri).write(csvContent.getBytes());
            getContentResolver().openOutputStream(uri).close();
            Toast.makeText(this, "Журнал сохранен", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startLoadFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/*", "*/*"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            loadFileLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при открытии диалога выбора файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadJournalFromCsv(Uri uri) {
        try {
            long journalId = dbHelper.importJournalFromCsv(getContentResolver().openInputStream(uri));
            if (journalId != -1) {
                currentJournalId = journalId; // Обновляем текущий journalId
                NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_content_main);
                if (navHostFragment != null) {
                    HomeFragment homeFragment = (HomeFragment) navHostFragment.getChildFragmentManager()
                            .getFragments().stream()
                            .filter(fragment -> fragment instanceof HomeFragment)
                            .findFirst()
                            .orElse(null);
                    if (homeFragment != null) {
                        homeFragment.setJournal(journalId);
                    }
                    navController.navigate(R.id.nav_home);
                }
                Toast.makeText(this, "Журнал загружен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ошибка формата файла или данные отсутствуют", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Нет доступа к файлу: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при загрузке файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Обновляет currentJournalId, выбирая последний загруженный или созданный журнал.
     */
    private void updateCurrentJournalId() {
        Cursor cursor = dbHelper.getLastJournal();
        if (cursor.moveToFirst()) {
            currentJournalId = cursor.getLong(cursor.getColumnIndexOrThrow("journal_id"));
            Log.d(TAG, "Current journalId updated: " + currentJournalId);
        }
        cursor.close();
    }

    /**
     * Процедура ввода данных: Открывает диалог для ввода имени студента и инициирует поиск.
     */
    private void startSearch() {
        Log.d(TAG, "startSearch called");
        if (currentJournalId == -1) {
            Toast.makeText(this, "Нет активного журнала", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Поиск студента");
        final EditText input = new EditText(this);
        input.setHint("Введите имя студента");
        builder.setView(input);

        builder.setPositiveButton("ОК", (dialog, which) -> {
            String query = input.getText().toString().trim();
            Log.d(TAG, "Search query: " + query);
            if (query.isEmpty()) {
                Toast.makeText(this, "Введите запрос для поиска", Toast.LENGTH_SHORT).show();
                return;
            }

            // Запускаем и подключаемся к службе
            Intent intent = new Intent(this, JournalService.class);
            Log.d(TAG, "Calling startService");
            startService(intent);
            Log.d(TAG, "Calling bindService");
            boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "bindService result: " + bound);

            // Выполняем поиск в фоновом потоке
            executor.execute(() -> {
                int retries = 5;
                while (!isServiceBound && retries > 0) {
                    try {
                        Thread.sleep(200);
                        retries--;
                        Log.d(TAG, "Waiting for service connection, retries left: " + retries);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for service", e);
                    }
                }

                mainHandler.post(() -> {
                    if (isServiceBound && journalService != null) {
                        Log.d(TAG, "Service ready, performing search");
                        performSearch(query);
                    } else {
                        Log.e(TAG, "Service not connected after retries");
                        Toast.makeText(MainActivity.this, "Не удалось подключиться к службе", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Процедура обработки данных: Выполняет поиск студентов через службу.
     */
    private void performSearch(String query) {
        Log.d(TAG, "performSearch called with query: " + query);
        try {
            SearchResult result = journalService.searchStudents(query, currentJournalId);
            displaySearchResults(result);
        } catch (Exception e) {
            Log.e(TAG, "Search error", e);
            Toast.makeText(this, "Ошибка при поиске: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Процедура вывода данных: Отображает результаты поиска в диалоговом окне с прокручиваемым списком.
     */
    private void displaySearchResults(SearchResult result) {
        Log.d(TAG, "displaySearchResults called, results: " + result.getStudents().size());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Результаты поиска");

        if (result.getStudents().isEmpty()) {
            builder.setMessage("Студенты не найдены");
            builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
        } else {
            // Создаем ListView для отображения результатов
            ListView listView = new ListView(this);
            ArrayList<String> resultList = new ArrayList<>();
            for (SearchResult.StudentData student : result.getStudents()) {
                resultList.add(student.getStudentName() + " (" + student.getPageName() + ")");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultList);
            listView.setAdapter(adapter);
            builder.setView(listView);
            builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
        }

        builder.setCancelable(false); // Диалог нельзя закрыть кнопкой "Назад"
        builder.show();
    }

    /**
     * Процедура остановки службы: Отключает и останавливает службу.
     */
    private void stopService() {
        Log.d(TAG, "stopService called");
        if (isServiceBound) {
            Log.d(TAG, "Unbinding service");
            unbindService(serviceConnection);
            isServiceBound = false;
            journalService = null;
        }
        Intent intent = new Intent(this, JournalService.class);
        Log.d(TAG, "Stopping service");
        stopService(intent);
        Toast.makeText(this, "Служба остановлена", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}