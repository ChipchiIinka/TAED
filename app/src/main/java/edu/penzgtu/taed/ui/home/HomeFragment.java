package edu.penzgtu.taed.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
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

import edu.penzgtu.taed.R;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout1;
    private TableLayout tableLayout3;
    private TableLayout tableLayout4;
    private TableLayout tableLayout5;
    private int tabCounter = 0;
    private int currentTabPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация компонентов
        tabLayout1 = view.findViewById(R.id.tabLayout1);
        view.findViewById(R.id.disciplineEditText);
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

        // Динамическое добавление вкладок
        addNewTab("ПОНЕДЕЛЬНИК", 0);

        // Обработчик кнопки добавления вкладки
        addTabButton.setOnClickListener(v -> addNewTab("Вкладка " + (++tabCounter), tabLayout1.getTabCount()));

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
                    String newName = input.getText().toString();
                    if (!newName.isEmpty()) {
                        tab.setText(newName);
                    }
                });
                builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

                builder.show();
            }
        });

        // Обработчик долгого нажатия для каждой вкладки
        for (int i = 0; i < tabLayout1.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout1.getTabAt(i);
            if (tab != null) {
                tab.view.setOnLongClickListener(v -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Удалить вкладку");
                    builder.setMessage("Вы уверены, что хотите удалить вкладку \"" + tab.getText() + "\"?");
                    builder.setPositiveButton("Да", (dialog, which) -> {
                        tabLayout1.removeTab(tab);
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
            }
        }

        // Обработчик добавления новой строки
        addRowButton.setOnClickListener(v -> addNewRow());

        // Инициализация содержимого для первой вкладки
        updateTableContent();

        return view;
    }

    // Метод для добавления новой вкладки
    private void addNewTab(String tabName, int position) {
        TabLayout.Tab newTab = tabLayout1.newTab();
        newTab.setText(tabName);
        tabLayout1.addTab(newTab, position);
        newTab.view.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удалить вкладку");
            builder.setMessage("Вы уверены, что хотите удалить вкладку \"" + newTab.getText() + "\"?");
            builder.setPositiveButton("Да", (dialog, which) -> {
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
        int rowCount = tableLayout3.getChildCount() + 1;

        // Добавление строки в tableLayout3 (№ и ФИО)
        TableRow row3 = new TableRow(getContext());

        // Кнопка удаления
        ImageButton deleteButton = getImageDelButton(row3);
        row3.addView(deleteButton);

        EditText number = getEditText(String.valueOf(rowCount), 0);
        row3.addView(number);

        EditText name = getEditText("", 1);
        row3.addView(name);

        tableLayout3.addView(row3);

        // Добавление строки в tableLayout4 (оценки)
        TableRow row4 = new TableRow(getContext());
        Space space = new Space(getContext());
        space.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
        row4.addView(space);

        for (int i = 0; i < 25; i++) {
            EditText grade = getEditText("-", i);
            row4.addView(grade);
        }
        tableLayout4.addView(row4);
        tableLayout4.addView(createSpacer());

        // Добавление строки в tableLayout5 (Зач КП ЭКЗ)
        TableRow row5 = new TableRow(getContext());
        Space space5 = new Space(getContext());
        space5.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
        row5.addView(space5);

        EditText credit = getEditText("-", 0);
        row5.addView(credit);

        EditText cp = getEditText("-", 1);
        row5.addView(cp);

        EditText exam = getEditText("-", 2);
        row5.addView(exam);

        tableLayout5.addView(row5);
        tableLayout5.addView(createSpacer());
    }

    // Удаление строки
    private void deleteRow(TableRow rowToDelete) {
        int rowIndex = tableLayout3.indexOfChild(rowToDelete);
        if (rowIndex >= 0) {
            tableLayout3.removeViewAt(rowIndex);
            tableLayout4.removeViewAt(rowIndex * 2); // Удаляем строку
            tableLayout4.removeViewAt(rowIndex * 2); // Удаляем разделитель
            tableLayout5.removeViewAt(rowIndex * 2); // Удаляем строку
            tableLayout5.removeViewAt(rowIndex * 2); // Удаляем разделитель
            renumberRows();
        }
    }

    // Перенумерация строк
    private void renumberRows() {
        for (int i = 0; i < tableLayout3.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout3.getChildAt(i);
            EditText numberEditText = (EditText) row.getChildAt(1); // Индекс 1, так как 0 — кнопка
            numberEditText.setText(String.valueOf(i + 1));
        }
    }

    // Обновление содержимого таблиц в зависимости от вкладки
    private void updateTableContent() {
        clearTableContent();

        String tabName = Objects.requireNonNull(Objects.requireNonNull(tabLayout1.getTabAt(currentTabPosition))
                .getText()).toString();

        if (tabName.equals("ПОНЕДЕЛЬНИК")) {
            for (int i = 1; i <= 25; i++) {
                // Добавление строки в tableLayout3 (№ и ФИО)
                TableRow row3 = new TableRow(getContext());

                ImageButton deleteButton = getImageDelButton(row3);
                row3.addView(deleteButton);

                EditText number = getEditText(String.valueOf(i), 0);
                row3.addView(number);

                EditText name = getEditText("Фамилия Имя Отчество", 1);
                row3.addView(name);

                tableLayout3.addView(row3);

                // Добавление строки в tableLayout4 (оценки)
                TableRow row4 = new TableRow(getContext());
                Space space = new Space(getContext());
                space.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
                row4.addView(space);

                for (int j = 0; j < 25; j++) {
                    EditText grade = getEditText(String.valueOf((i + j) % 10), j);
                    row4.addView(grade);
                }
                tableLayout4.addView(row4);
                tableLayout4.addView(createSpacer());

                // Добавление строки в tableLayout5 (Зач КП ЭКЗ)
                TableRow row5 = new TableRow(getContext());
                Space space5 = new Space(getContext());
                space5.setLayoutParams(new TableRow.LayoutParams(48, TableRow.LayoutParams.WRAP_CONTENT));
                row5.addView(space5);

                EditText credit = getEditText("4", 0);
                row5.addView(credit);

                EditText cp = getEditText("4", 1);
                row5.addView(cp);

                EditText exam = getEditText("4", 2);
                row5.addView(exam);

                tableLayout5.addView(row5);
                tableLayout5.addView(createSpacer());
            }
        }
    }

    @NonNull
    private ImageButton getImageDelButton(TableRow row3) {
        ImageButton deleteButton = new ImageButton(getContext());
        deleteButton.setImageResource(android.R.drawable.ic_delete);
        deleteButton.setBackgroundColor(0x00FFFFFF);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        deleteButton.setOnClickListener(v -> deleteRow(row3));
        return deleteButton;
    }

    @NonNull
    private EditText getEditText(String bio, int x) {
        EditText name = new EditText(getContext());
        name.setText(bio);
        name.setTextColor(0xFF000000);
        name.setPadding(8, 8, 8, 8);
        name.setBackgroundColor(x % 2 == 0 ? 0xFFE0F7FA : 0xFFFFFFFF);
        name.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        name.setSingleLine(true);
        return name;
    }
}