package com.sghh.testcurriculum;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private InputMethodManager mInputMethodManager;
    private ConstraintLayout mMainLayout;

    private FirebaseAuth mAuth;

    private EditText mEmailText;
    private EditText mPassText;

    // キーボードを隠すメソッド
    public void onHideTouchEvent() {
        //キーボードを隠す
        mInputMethodManager.hideSoftInputFromWindow(mMainLayout.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    // EditText編集時に背景をタップしたら呼ばれる
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onHideTouchEvent();
        //背景にフォーカスを移す
        mMainLayout.requestFocus();
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mMainLayout = findViewById(R.id.mainLayout);
        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mEmailText = findViewById(R.id.editLoginEmailAddress);
        mPassText = findViewById(R.id.editLoginPassword);

        // アクションバーを取得
        ActionBar actionBar = getSupportActionBar();
        // アクションバーの[戻る]メニューを有効に設定
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();

        pushSubmitButton();
    }

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

    // EditTextにエラーをセットするメソッド
    public boolean[] setError() {
        boolean[] result = {false, false};
        if (mEmailText.getText().toString().isEmpty()) {
            mEmailText.setError("文字を入力してください");
        } else if (!mEmailText.getText().toString().contains("@")) {
            mEmailText.setError("@を含める必要があります");
        } else {
            result[0] = true;
        }

        if (mPassText.getText().toString().isEmpty()) {
            mPassText.setError("文字を入力してください");
        } else if (mPassText.getText().toString().length() < 6) {
            mPassText.setError("6文字以上入力してください");
        } else {
            result[1] = true;
        }
        return result;
    }

    // 同意して登録ボタンを押下時の処理メソッド
    private void pushSubmitButton() {
        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(view -> {
            boolean[] errorArray = setError();
            if (errorArray[0] && errorArray[1]) {
                String email = mEmailText.getText().toString();
                String password = mPassText.getText().toString();
                signIn(email, password);

                Intent intent = new Intent(LoginActivity.this, BottomNavigationActivity.class);
                startActivity(intent);
            }
        });
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    showDialog(user.getUid());
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "signInWithEmail:failure", task.getException());
                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    showDialog("mmm");
                }
            }
        });
    }

    private void showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("閉じる", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }
}