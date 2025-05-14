package edu.penzgtu.taed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private ActivityResultLauncher<Intent> loadFileLauncher;
    private DBHelper dbHelper;

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
            Toast.makeText(this, "Поиск", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_about) {
            Toast.makeText(this, "О программе", Toast.LENGTH_SHORT).show();
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
        // Добавляем альтернативные MIME-типы для совместимости
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
                // Обновление HomeFragment
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
}