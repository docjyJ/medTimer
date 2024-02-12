package com.futsch1.medtimer.medicine;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_COLOR;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_ID;
import static com.futsch1.medtimer.ActivityCodes.EXTRA_INDEX;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.ColorPicker;
import com.futsch1.medtimer.MedicineViewModel;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.helpers.SwipeHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.util.List;

public class EditMedicine extends AppCompatActivity {

    MedicineViewModel medicineViewModel;
    EditText editMedicineName;
    int medicineId;
    HandlerThread thread;
    ReminderViewAdapter adapter;
    private SwipeHelper swipeHelper;
    private SwitchMaterial enableColor;
    private Button colorButton;
    private int color;
    ActivityResultLauncher<Intent> getColor = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() == RESULT_OK) {
                color = o.getData() != null ? o.getData().getIntExtra(EXTRA_COLOR, Color.DKGRAY) : Color.DKGRAY;
                colorButton.setBackgroundColor(color);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.thread = new HandlerThread("DeleteMedicine");
        this.thread.start();

        setContentView(R.layout.activity_edit_medicine);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);
        medicineId = getIntent().getIntExtra(EXTRA_ID, 0);

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        adapter = new ReminderViewAdapter(new ReminderViewAdapter.ReminderDiff(), EditMedicine.this::deleteItem);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        editMedicineName = findViewById(R.id.editMedicineName);
        enableColor = findViewById(R.id.enableColor);

        colorButton = findViewById(R.id.selectColor);

        final Observer<List<MedicineWithReminders>> nameObserver = newList -> {
            if (newList != null) {
                Medicine medicine = newList.get(getIntent().getIntExtra(EXTRA_INDEX, 0)).medicine;
                editMedicineName.setText(medicine.name);

                enableColor.setChecked(medicine.useColor);
                enableColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    medicine.useColor = isChecked;
                    colorButton.setVisibility(medicine.useColor ? View.VISIBLE : View.GONE);
                });

                colorButton.setBackgroundColor(medicine.color);
                colorButton.setVisibility(medicine.useColor ? View.VISIBLE : View.GONE);
                color = medicine.color;
                colorButton.setOnClickListener(v -> {
                    Intent i = new Intent(getApplicationContext(), ColorPicker.class);
                    i.putExtra(EXTRA_COLOR, color);
                    getColor.launch(i);
                });
            }
        };

        // Swipe to delete
        swipeHelper = new SwipeHelper(Color.RED, android.R.drawable.ic_menu_delete, this) {
            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    EditMedicine.this.deleteItem(EditMedicine.this, viewHolder.getItemId(), viewHolder.getAdapterPosition());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        ExtendedFloatingActionButton fab = findViewById(R.id.addReminder);
        fab.setOnClickListener(view -> {
            TextInputLayout textInputLayout = new TextInputLayout(this);
            TextInputEditText editText = new TextInputEditText(this);
            editText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            editText.setHint(R.string.amount);
            editText.setSingleLine();
            textInputLayout.addView(editText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(textInputLayout);
            builder.setTitle(R.string.add_reminder);
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                Editable e = editText.getText();
                if (e != null) {
                    String amount = e.toString();
                    Reminder reminder = new Reminder(medicineId);
                    reminder.amount = amount;
                    reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000;

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this, (tpView, hourOfDay, minute) -> {
                        reminder.timeInMinutes = hourOfDay * 60 + minute;
                        medicineViewModel.insertReminder(reminder);
                    }, 8, 0, DateFormat.is24HourFormat(view.getContext()));
                    timePickerDialog.show();

                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        medicineViewModel.getMedicines().observe(this, nameObserver);
        medicineViewModel.getReminders(medicineId).observe(this, adapter::submitList);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPref.getString("delete_items", "0").equals("0")) {
            swipeHelper.setDefaultSwipeDirs(ItemTouchHelper.LEFT);
        } else {
            swipeHelper.setDefaultSwipeDirs(0);
        }
    }

    private void deleteItem(Context context, long itemId, int adapterPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm);
        builder.setMessage(R.string.are_you_sure_delete_reminder);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            final Handler handler = new Handler(thread.getLooper());
            handler.post(() -> {
                Reminder reminder = medicineViewModel.getReminder((int) itemId);
                medicineViewModel.deleteReminder(reminder);
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
            });
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        String word = editMedicineName.getText().toString();
        Medicine medicine = new Medicine(word, medicineId);
        medicine.useColor = enableColor.isChecked();
        medicine.color = color;
        medicineViewModel.updateMedicine(medicine);

        RecyclerView recyclerView = findViewById(R.id.reminderList);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            ReminderViewHolder viewHolder = (ReminderViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));

            medicineViewModel.updateReminder(viewHolder.reminder);
        }

        thread.quitSafely();
    }
}