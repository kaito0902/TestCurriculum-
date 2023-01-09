package com.sghh.testcurriculum.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.sghh.testcurriculum.BottomNavigationActivity;
import com.sghh.testcurriculum.R;

public class SearchFragment extends Fragment {

    private ListView listView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        initView(v);
        listView.setOnItemClickListener(new SearchFragment.ListItemClickListener());

        return v;
    }

    private void initView(View v) {
        listView = v.findViewById(R.id.prefectureList);
    }

    // リストがタップされたときの処理が記述されたメンバクラス
    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // タップされた都道府県名を取得
            String item = (String) adapterView.getItemAtPosition(i);
            // ダイアログ表示
            new AlertDialog.Builder(getContext())
                    .setTitle(item + "の天気を検索しますか？")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // アプリの終了処理
                        Intent intent = new Intent(getContext(), BottomNavigationActivity.class);
                        intent.putExtra("prefecture", item);
                        startActivity(intent);
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}