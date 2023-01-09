package com.sghh.testcurriculum;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.os.HandlerCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private InputMethodManager mInputMethodManager;
    private ConstraintLayout mMainLayout;

    private EditText mEmailText;
    private EditText mPassText;
    private EditText mPostCodeText;
    private EditText mAddressText;

    private ExecutorService executorService;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // アクションバーを取得
        ActionBar actionBar = getSupportActionBar();
        // アクションバーの[戻る]メニューを有効に設定
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPostCodeText = findViewById(R.id.postalCodeText);
        mAddressText = findViewById(R.id.addressText);
        mMainLayout = findViewById(R.id.mainLayout);
        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mEmailText = findViewById(R.id.editTextTextEmailAddress);
        mPassText = findViewById(R.id.editTextTextPassword);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        getPrivacyText();
        getAddress();
        pushSubmitButton();
    }

    // アクションバーの戻るボタン押下時の処理を書いたメソッド
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

    // privacyTextの文字列を作成するメソッド
    private void getPrivacyText() {
        TextView privacyText = findViewById(R.id.privacyText);

        // HTML文字列作成
        String html = "[同意して登録]をタップすると、<font color=\"#4992FF\"><a href=\"https://kaito0902.github.io/PrivacyHtml/privacy.html\">プライバシーポリシー</a></font>と<font color=\"#4992FF\"><a href=\"https://kaito0902.github.io/PrivacyHtml/tos.html\">利用規約</a></font>に同意したことになります。";
        // CharSequenceオブジェクト作成
        CharSequence csHTML = Html.fromHtml(html);
        // TextViewにテキストを設定
        privacyText.setText(csHTML);

        // リンクがタップされたときの処理
        privacyText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // 郵便番号で住所を検索するボタン押下時のメソッド
    private void getAddress() {
        // Mainスレッドを戻り値として取得
        Looper mainLooper = Looper.getMainLooper();
        // スレッド間の通信を行ってくれるオブジェクト
        Handler handler = HandlerCompat.createAsync(mainLooper);

        Button button = findViewById(R.id.getAddressButton);

        button.setOnClickListener(view -> {
            onHideTouchEvent();
            // postCodeTextが7桁ならtrue
            if (mPostCodeText.length() == 7) {
                // BackgroundTaskをインスタンス化
                BackgroundTask backgroundTask = new BackgroundTask(handler);
                // シングルスレッドを作成
                executorService = Executors.newSingleThreadExecutor();
                // BackgroundTaskの処理をシングルスレッドで実行
                executorService.submit(backgroundTask);
            } else {
                // 7桁まで入力してくださいのエラートースト表示
                Toast.makeText(getApplicationContext(), R.string.input_warning, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 非同期処理クラス
    private class BackgroundTask implements Runnable {
        // Handlerオブジェクト
        private final Handler _handler;

        // コンストラクト
        public BackgroundTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            // editText欄にある数値を取得
            Editable getText = mPostCodeText.getText();
            // getTextを文字列に変換し格納
            String postalCode = getText.toString();

            // 郵便番号検索APIから取得したJSON文字列を格納する変数
            String addressJSON = getAddress(postalCode);
            // addressJSONを住所の形に加工した文字列を格納する変数
            String processingJSON = getJSONProcessing(addressJSON);

            // UiInfoTaskをインスタンス化
            UiInfoTask uiInfoTask = new UiInfoTask(processingJSON);

            // Handlerオブジェクトを生成した元スレッドで画面描画の処理を行わせる
            _handler.post(uiInfoTask);
        }
    }

    // 非同期処理クラスのデータをUIスレッドに反映するクラス
    private class UiInfoTask implements Runnable {
        // 取得した住所情報のJSON文字列
        private final String _result;

        // コンストラクタ
        public UiInfoTask(String result) {
            _result = result;
        }

        @UiThread
        @Override
        public void run() {
            mAddressText.setText(_result);
        }
    }

    // 郵便番号検索APIから住所を取得するメソッド
    private String getAddress(String urlSt) {
        // リクエストURL
        String requestURL = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=" + urlSt;

        // 郵便番号検索APIから取得したJSON文字列を格納する
        String result = null;
        try {
            // URLオブジェクトを生成
            URL url = new URL(requestURL);
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // データ取得に使っても良い時間を設定
            urlConnection.setReadTimeout(5000);
            // 接続に使っても良い時間を設定
            urlConnection.setConnectTimeout(5000);

            // リクエストメソッド
            urlConnection.setRequestMethod("GET");

            // 接続
            urlConnection.connect();

            // レスポンスデータを取得
            try (InputStream inputStream = urlConnection.getInputStream()) {
                // レスポンスデータであるInputStreamオブジェクトを文字列に変換
                result = isString(inputStream);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // HttpURLConnectionオブジェクトを開放
            urlConnection.disconnect();

        }
        catch (SocketException e) {
            Log.d("SocketException", "通信タイムアウト", e);
        }
        catch (IOException e) {
            Log.d("IOException", "通信失敗", e);
            e.printStackTrace();
        }

        return result;
    }

    // 郵便番号検索APIから取得したJSONデータを加工するメソッド
    private String getJSONProcessing(String _result) {
        // 住所
        String address = "";
        try {
            // JSONObjectオブジェクトを_resultを引数に生成
            JSONObject jsonObject = new JSONObject(_result);
            // 配列データをgetJSONArray()で取得
            JSONArray arrayJSON = jsonObject.getJSONArray("results");
            // 配列データを取り出すためgetJSONObject()で1番目のデータを取得
            JSONObject addressJSON = arrayJSON.getJSONObject(0);

            // 都道府県名を取得
            String prefectureName = addressJSON.getString("address1");
            // 市区町村名を取得
            String cityName = addressJSON.getString("address2");
            // 町名を取得
            String townName = addressJSON.getString("address3");

            // 住所を定義
            address = prefectureName + cityName + townName;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return address;
    }

    // InputStreamオブジェクトを文字列に変換するメソッド
    private String isString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] b = new char[1024];
        int line;
        while (0 <= (line = reader.read(b))) {
            sb.append(b, 0, line);
        }
        return sb.toString();
    }

    // 同意して登録ボタンを押下時の処理メソッド
    private void pushSubmitButton() {
        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(view -> {
            boolean[] errorArray = setError();
            // setErrorの値がtrue,trueだった場合画面遷移
            if (errorArray[0] && errorArray[1]) {
                Editable getAddress = mAddressText.getText();
                String addressText = getAddress.toString();
                int addressIndexOf;
                if (addressText.indexOf("道") > 0) {
                    addressIndexOf = addressText.indexOf("道");
                } else if (addressText.indexOf("府") > 0) {
                    addressIndexOf = addressText.indexOf("府");
                } else if (addressText.indexOf("都") > 0) {
                    addressIndexOf = addressText.indexOf("都");
                } else {
                    addressIndexOf = addressText.indexOf("県");
                }
                // 都道府県のみを格納
                String prefecture = addressText.substring(0, addressIndexOf + 1);

                String email = ((TextView)findViewById(R.id.editTextTextEmailAddress)).getText().toString();
                String password = ((TextView)findViewById(R.id.editTextTextPassword)).getText().toString();
                createUser(email, password);

                Intent intent = new Intent(RegisterActivity.this, BottomNavigationActivity.class);
                intent.putExtra("prefecture", prefecture);
                startActivity(intent);
            }
        });
    }

    // FirebaseUser登録処理
    private void createUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                assert user != null;
                showDialog(user.getUid());
            }
        });
    }

    private void showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("閉じる", (dialog, which) -> dialog.cancel());
        Dialog dialog = builder.create();
        dialog.show();
    }
}