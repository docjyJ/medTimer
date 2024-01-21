package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_REPLY;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MedicineActivity extends AppCompatActivity {

    EditText editMedicineName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine);

        editMedicineName = findViewById(R.id.editMedicineName);

        final ImageButton button = findViewById(R.id.saveMedicineButton);
        button.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(editMedicineName.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
            } else {
                String word = editMedicineName.getText().toString();
                replyIntent.putExtra(EXTRA_REPLY, word);
                setResult(RESULT_OK, replyIntent);
            }
            finish();
        });

    }
}