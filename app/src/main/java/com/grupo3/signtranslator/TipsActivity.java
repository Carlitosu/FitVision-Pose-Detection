package com.grupo3.signtranslator;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;

import com.grupo3.signtranslator.databinding.ActivityTipsBinding;
import com.grupo3.signtranslator.databinding.ItemTipBinding;

/** Consejos de postura: una tarjeta por recomendación, numeradas. */
@SuppressWarnings("SpellCheckingInspection")
public class TipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTipsBinding binding = ActivityTipsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String[] tips = getResources().getStringArray(R.array.posture_tips);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < tips.length; i++) {
            ItemTipBinding item = ItemTipBinding.inflate(inflater, binding.tipsContainer, false);
            item.tvTipNumber.setText(String.valueOf(i + 1));
            item.tvTipText.setText(tips[i]);
            binding.tipsContainer.addView(item.getRoot());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}