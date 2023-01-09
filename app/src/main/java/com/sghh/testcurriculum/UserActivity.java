package com.sghh.testcurriculum;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserActivity extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 戻り値の変数を初期値trueで用意
        boolean returnVal = true;
        // 選択されたメニューのIDを取得
        int itemId = item.getItemId();
        // 選択されたメニューが[戻る]の場合、アクティビティを終了
        if (itemId == android.R.id.home) {
            finish();
        }

        return returnVal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // アクションバーを取得
        ActionBar actionBar = getSupportActionBar();
        // アクションバーの[戻る]メニューを有効に設定
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView textView = findViewById(R.id.userText);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // emailを取得
            String email = user.getEmail();
            textView.setText(email);
        }

        Button deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ダイアログ表示
                new AlertDialog.Builder(UserActivity.this)
                        .setTitle("ユーザーを削除しますか？")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // OKが押された場合、ユーザーを削除
                            delete();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(UserActivity.this, StartActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        });
    }

    public void delete() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UserActivity", "User account deleted.");
                    }
                });
    }
}