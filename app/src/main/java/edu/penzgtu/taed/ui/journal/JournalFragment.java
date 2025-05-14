package edu.penzgtu.taed.ui.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import edu.penzgtu.taed.DBHelper;
import edu.penzgtu.taed.R;
import edu.penzgtu.taed.ui.home.HomeFragment;

public class JournalFragment extends Fragment {

    private EditText journalNameEditText;
    private EditText pageCountEditText;
    private EditText gradeCountEditText;
    private EditText studentCountEditText;
    private DBHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        // Инициализация компонентов
        journalNameEditText = view.findViewById(R.id.journalNameEditText);
        pageCountEditText = view.findViewById(R.id.pageCountEditText);
        gradeCountEditText = view.findViewById(R.id.gradeCountEditText);
        studentCountEditText = view.findViewById(R.id.studentCountEditText);
        Button createJournalButton = view.findViewById(R.id.createJournalButton);

        // Инициализация базы данных
        dbHelper = new DBHelper(requireContext());

        // Обработчик кнопки создания журнала
        createJournalButton.setOnClickListener(v -> createJournal());

        return view;
    }

    private void createJournal() {
        String journalName = journalNameEditText.getText().toString().trim();
        String pageCountStr = pageCountEditText.getText().toString().trim();
        String gradeCountStr = gradeCountEditText.getText().toString().trim();
        String studentCountStr = studentCountEditText.getText().toString().trim();

        // Валидация полей
        if (journalName.isEmpty() || pageCountStr.isEmpty() || gradeCountStr.isEmpty() || studentCountStr.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        int pageCount, gradeCount, studentCount;
        try {
            pageCount = Integer.parseInt(pageCountStr);
            gradeCount = Integer.parseInt(gradeCountStr);
            studentCount = Integer.parseInt(studentCountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Введите корректные числовые значения", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pageCount <= 0 || gradeCount <= 0 || studentCount <= 0) {
            Toast.makeText(getContext(), "Значения должны быть больше 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создание журнала в базе данных
        long journalId = dbHelper.createJournal(journalName, pageCount, gradeCount, studentCount);

        // Переход к HomeFragment с новым журналом
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putLong("journalId", journalId);
        navController.navigate(R.id.nav_home, bundle);

        Toast.makeText(getContext(), "Журнал создан", Toast.LENGTH_SHORT).show();
    }
}