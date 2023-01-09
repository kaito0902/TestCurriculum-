package com.sghh.testcurriculum;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class StartActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // 新規登録ボタンidを取得
        Button registerButton = findViewById(R.id.registerButton);
        // ログインボタンidを取得
        Button loginButton = findViewById(R.id.loginButton);

        registerButton.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(StartActivity.this, BottomNavigationActivity.class);
            startActivity(intent);
        }
    }

    // 戻るボタン押下時の処理
    @Override
    public void onBackPressed(){
        ArrayList<Activity> ActivityList = new ArrayList<>();
        // ダイアログ表示
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title)
            .setPositiveButton("OK", (dialog, which) -> {
            // OKが押された場合、アプリを終了
            moveTaskToBack(true);
        })

            .setNegativeButton("キャンセル", null)
            .show();
    }
}