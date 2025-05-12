package edu.penzgtu.taed;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import edu.penzgtu.taed.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        edu.penzgtu.taed.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Динамическое добавление пунктов меню в Navigation Drawer
        Menu menu = navigationView.getMenu();
        String[] dynamicItems = {"Динамический пункт 1", "Динамический пункт 2", "Динамический пункт 3"};
        for (int i = 0; i < dynamicItems.length; i++) {
            menu.add(R.id.nav_dynamic_group, Menu.NONE, i, dynamicItems[i])
                    .setIcon(R.drawable.ic_menu_dynamic);
        }

        // Обработка кликов по пунктам Navigation Drawer
        navigationView.setNavigationItemSelectedListener(item -> {
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            drawer.closeDrawers();
            return true;
        });

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Обработка кликов по пунктам главного меню
        Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                    || super.onSupportNavigateUp();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return super.onSupportNavigateUp();
        }
    }
}