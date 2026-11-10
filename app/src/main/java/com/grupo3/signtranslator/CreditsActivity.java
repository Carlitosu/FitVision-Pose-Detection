package com.grupo3.signtranslator;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.grupo3.signtranslator.databinding.ActivityCreditsBinding;

/** Créditos: grupo, curso, creador principal y tecnologías usadas. */
public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCreditsBinding binding = ActivityCreditsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}