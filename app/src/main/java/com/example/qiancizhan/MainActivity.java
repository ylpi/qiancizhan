package com.example.qiancizhan;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_tower).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CardStackActivity.start(MainActivity.this, "tower");
            }
        });
        findViewById(R.id.tv_poker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CardStackActivity.start(MainActivity.this, "poker");
            }
        });
    }

}