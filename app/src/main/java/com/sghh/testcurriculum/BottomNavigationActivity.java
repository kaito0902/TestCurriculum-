package com.sghh.testcurriculum;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.sghh.testcurriculum.databinding.ActivityBottomNavigationBinding;

public class BottomNavigationActivity extends AppCompatActivity {

    private ActivityBottomNavigationBinding binding;
    public String prefectureItem = "";

    // 戻るボタン押下時の処理
    @Override
    public void onBackPressed(){
        // ダイアログ表示
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_dialog_title)
                .setPositiveButton("OK", (dialog, which) -> {
                    // アプリの終了処理
                    Intent intent = new Intent(BottomNavigationActivity.this, StartActivity.class);
                    startActivity(intent);
                    FirebaseAuth.getInstance().signOut();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBottomNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search, R.id.navigation_setting)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_navigation);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        prefectureItem = getIntent().getStringExtra("prefecture");
    }
}