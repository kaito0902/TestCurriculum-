package com.sghh.testcurriculum.ui.setting;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.sghh.testcurriculum.R;
import com.sghh.testcurriculum.StartActivity;
import com.sghh.testcurriculum.UserActivity;

public class SettingFragment extends Fragment {

    private Intent intent;
    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setting, container, false);

        initView(v);
        listView.setOnItemClickListener(new ListItemClickListener());

        return v;
    }

    private void initView(View v) {
        listView = v.findViewById(R.id.menuList);
    }

    // リストがタップされたときの処理が記述されたメンバクラス
    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // タップされたListViewの文字列を取得
            String item = (String) adapterView.getItemAtPosition(i);

            switch (item) {
                case "ユーザー情報":
                    intent = new Intent(getContext(), UserActivity.class);
                    startActivity(intent);
                    break;
                case "ログアウト":
                    // ダイアログ表示
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.logout_dialog_title)
                            .setPositiveButton("OK", (dialog, which) -> {
                                // アプリの終了処理
                                Intent intent = new Intent(getContext(), StartActivity.class);
                                startActivity(intent);
                                FirebaseAuth.getInstance().signOut();
                            })
                            .setNegativeButton("キャンセル", null)
                            .show();
                    break;
                case "利用規約":
                    /// ブラウザ起動でページ表示
                    intent = new Intent( Intent.ACTION_VIEW );
                    intent.setData( Uri.parse("https://kaito0902.github.io/PrivacyHtml/tos.html") );
                    startActivity( intent );
                    break;
                case "プライバシーポリシー":
                    /// ブラウザ起動でページ表示
                    intent = new Intent( Intent.ACTION_VIEW );
                    intent.setData( Uri.parse("https://kaito0902.github.io/PrivacyHtml/privacy.html") );
                    startActivity( intent );
                    break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}