package com.sghh.testcurriculum.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.sghh.testcurriculum.BottomNavigationActivity;
import com.sghh.testcurriculum.R;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    
    private final String API_TOKEN = "8030b855c64c79f786ccf60006a2df08";
    private String mCityName = "";

    // スレッドUI操作ハンドラ
    private Handler mHandler = new Handler();
    // テキストオブジェクト
    private Runnable searchText;

    /* 現在の天気 */
    // 日付
    private TextView mNowDateText;
    // 都道府県名
    private TextView mPrefectureText;
    // 天気
    private TextView mWeatherText;
    // 天気Lottieアニメーション
    private LottieAnimationView mLottieAnimationView;
    // 最高気温
    private TextView mHighestTemperatureText;
    // 最低気温
    private TextView mLowestTemperatureText;
    // 風速
    private TextView mWindSpeedText;
    // 湿度
    private TextView mHumidityText;

    /* 3時間毎の天気List */
    private ListView mListView;

    /**
     * 現在日時をyyyy/MM/dd HH:mm:ss形式で取得する
     */
    public static String getNowDate(){
        final DateFormat df = new SimpleDateFormat("MM/dd HH:mm");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        initView(v);
        initEvent();
        return v;
    }
    
    private void initView(View v) {
        mNowDateText = v.findViewById(R.id.dateText);
        mPrefectureText = v.findViewById(R.id.prefectureText);
        mWeatherText = v.findViewById(R.id.weatherText);
        mLottieAnimationView = v.findViewById(R.id.weatherIcon);
        mHighestTemperatureText = v.findViewById(R.id.highestTemperatureCharacter);
        mLowestTemperatureText = v.findViewById(R.id.lowestTemperatureCharacter);
        mWindSpeedText = v.findViewById(R.id.windSpeedCharacter);
        mHumidityText = v.findViewById(R.id.humidityCharacter);
        mListView = v.findViewById(R.id.listview);
    }

    private void initEvent() {
        Runnable progressRunnable = () -> mLottieAnimationView.setAnimation(R.raw.progress);
        mHandler.post(progressRunnable);

        BottomNavigationActivity bottomNavigationActivity = (BottomNavigationActivity) getActivity();
        Runnable runnable = () -> {
            mCityName = bottomNavigationActivity.prefectureItem;
            try {
                if (!mCityName.equals("")) {
                    getWeather();
                }
            } catch (NullPointerException e) {
            }
        };
        mHandler.postDelayed(runnable, 1000);

        searchText = () -> {
            try {
                if (mCityName.equals("")) {
                    mWeatherText.setText("検索から都道府県を選んでください");
                    mLottieAnimationView.setAnimation(R.raw.search);
                    mLottieAnimationView.playAnimation();
                }
            } catch (NullPointerException e) {
                mWeatherText.setText("検索から都道府県を選んでください");
                mLottieAnimationView.setAnimation(R.raw.search);
                mLottieAnimationView.playAnimation();
            }
        };
        mHandler.postDelayed(searchText, 5000);
    }

    /**
     * 天気予報の検索を開始するメソッド
     */
    private void getWeather() {
        // Mainスレッドを戻り値として取得
        Looper mainLooper = Looper.getMainLooper();
        // スレッド間の通信を行ってくれるオブジェクト
        Handler handler = HandlerCompat.createAsync(mainLooper);
        // BackgroundTaskをインスタンス化
        HomeFragment.BackgroundTask backgroundTask = new HomeFragment.BackgroundTask(handler);
        // シングルスレッドを作成
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // BackgroundTaskの処理をシングルスレッドで実行
        executorService.submit(backgroundTask);
    }

    // 非同期処理クラス
    private class BackgroundTask implements Runnable {
        // Handlerオブジェクト
        private final Handler _handler;

        public BackgroundTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            /* 現在の天気 */
            // 天気予報APIから取得したJSON文字列を格納する変数
            String weatherJSON = getJSON(mCityName);
            // weatherJSONを住所の形に加工した文字列を格納する変数
            String[] processingJSON = getJSONProcessing(weatherJSON);

            // UiInfoTaskをインスタンス化
            HomeFragment.UiInfoTask uiInfoTask = new HomeFragment.UiInfoTask(processingJSON);

            // Handlerオブジェクトを生成した元スレッドで画面描画の処理を行わせる
            _handler.post(uiInfoTask);

            /* 5日間の天気 */
            // 天気予報APIから取得したJSON文字列を格納する変数
            String listWeatherJSON = get5WeatherJSON(mCityName);
            // listWeatherJSONを各Textに格納する形に加工した文字列を格納する変数
            List<Map<String, Object>> machiningJSON = getJSONMachining(listWeatherJSON);
            // UiInfoTaskをインスタンス化
            HomeFragment.UiInfoTask2 uiInfoTask2 = new HomeFragment.UiInfoTask2(machiningJSON);
            // Handlerオブジェクトを生成した元スレッドで画面描画の処理を行わせる
            _handler.post(uiInfoTask2);
        }
    }

    // 非同期処理クラスのデータをUIスレッドに反映するクラス
    private class UiInfoTask implements Runnable {
        // 取得した天気情報の配列
        String[] _result = new String[5];

        // コンストラクタ
        public UiInfoTask(String[] result) {
            _result = result;
        }

        @UiThread
        @Override
        public void run() {
            mNowDateText.setText(getNowDate());
            mPrefectureText.setText(mCityName);
            mHighestTemperatureText.setText(_result[1]);
            mLowestTemperatureText.setText(_result[2]);
            mWindSpeedText.setText(_result[3]);
            mHumidityText.setText(_result[4]);

            getWeatherIcon(_result[0]);
        }
    }

    /**
     * OpenWeatherAPIから現在の天気をJSON形式で取得するメソッド
     * @param cityName
     * @return String
     */
    private String getJSON(String cityName) {
        // リクエストURL
        String weatherURL = "https://api.openweathermap.org/data/2.5/weather?lang=ja&q=" + cityName + "&appid=" + API_TOKEN;

        // OpenWeatherAPIから取得したJSON文字列を格納する
        String result = null;
        try {
            // URLオブジェクトを生成
            URL url = new URL(weatherURL);
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

    /**
     * OpenWeatherAPIから取得したJSONデータを加工するメソッド
     * @param _result
     * @return String[]
     */
    private String[] getJSONProcessing(String _result) {
        // 返り値
        String[] result = new String[5];
        try {
            // JSONObjectオブジェクトを_resultを引数に生成
            JSONObject jsonObject = new JSONObject(_result);
            // 配列データを取得
            JSONArray jsonArray = jsonObject.getJSONArray("weather");
            // 配列データを取り出すためgetJSONObject()で1番目のデータを取得
            JSONObject weatherJson = jsonArray.getJSONObject(0);
            JSONObject mainJSON = jsonObject.getJSONObject("main");
            JSONObject windJSON = jsonObject.getJSONObject("wind");

            // 天気を取得
            result[0] = weatherJson.getString("main");
            // 最高気温を取得
            result[1] = getTemp(mainJSON.getString("temp_max"));
            // 最低気温を取得
            result[2] = getTemp(mainJSON.getString("temp_min"));
            // 風速を取得
            result[3] = windJSON.getString("speed") + "m/s";
            // 湿度を取得
            result[4] = mainJSON.getString("humidity") + "%";
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 天気アイコンと天気を取得するメソッド
     * @param weather
     */
    private void getWeatherIcon(String weather) {
        switch (weather) {
            case "Clear":
                mWeatherText.setText("晴れ");
                mLottieAnimationView.setAnimation(R.raw.weather_sunny);
                mLottieAnimationView.playAnimation();
                break;
            case "Rain":
            case "Drizzle":
                mWeatherText.setText("雨");
                mLottieAnimationView.setAnimation(R.raw.weather_rain);
                mLottieAnimationView.playAnimation();
                break;
            case "Snow":
                mWeatherText.setText("雪");
                mLottieAnimationView.setAnimation(R.raw.weather_snow);
                mLottieAnimationView.playAnimation();
                break;
            case "Thunderstorm":
                mWeatherText.setText("雷雨");
                mLottieAnimationView.setAnimation(R.raw.weather_storm);
                mLottieAnimationView.playAnimation();
                break;
            default:
                mWeatherText.setText("曇り");
                mLottieAnimationView.setAnimation(R.raw.weather_windy);
                mLottieAnimationView.playAnimation();
                break;
        }
    }

    /**
     * ５日間の天気予報の検索をするメソッド
     * https://openweathermap.org/forecast5
     * @param cityName
     * @return String
     */
    private String get5WeatherJSON(String cityName) {
        // リクエストURL
        String weatherURL = "https://api.openweathermap.org/data/2.5/forecast?lang=ja&q=" + cityName + "&appid=" + API_TOKEN;

        // OpenWeatherAPIから取得したJSON文字列を格納する
        String result = null;
        try {
            // URLオブジェクトを生成
            URL url = new URL(weatherURL);
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

    /**
     * OpenWeatherAPIから取得したJSONデータを加工するメソッド
     * @param _result
     * @return List<Map<String, Object>>
     */
    private List<Map<String, Object>> getJSONMachining(String _result) {
        // 返り値
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = null;

        JSONObject weatherJson;
        JSONObject mainJSON;
        JSONArray tempJSON;
        JSONObject weatherTempJson;

        try {
            // JSONObjectオブジェクトを_resultを引数に生成
            JSONObject jsonObject = new JSONObject(_result);
            // 配列データを取得
            JSONArray jsonListArray = jsonObject.getJSONArray("list");
            for (int i = 3; i < 15; i++) {
                // 配列データを取り出すためgetJSONObject()でi番目のデータを取得
                weatherJson = jsonListArray.getJSONObject(i);
                mainJSON = weatherJson.getJSONObject("main");
                tempJSON = weatherJson.getJSONArray("weather");
                weatherTempJson = tempJSON.getJSONObject(0);

                map = new HashMap<>();
                map.put("DATE", getDate(weatherJson.getString("dt_txt")));
                map.put("TIME", getTime(weatherJson.getString("dt_txt")));
                map.put("ICON", getListWeatherIcon(weatherTempJson.getString("main")));
                map.put("TEMPERATURE", getTemp(mainJSON.getString("temp")));
                list.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 非同期処理クラスのデータをUIスレッドに反映するクラス
     */
    private class UiInfoTask2 implements Runnable {
        // 取得した天気情報の配列
        List<Map<String, Object>> _result;

        // コンストラクタ
        public UiInfoTask2(List<Map<String, Object>> result) {
            _result = result;
        }

        @UiThread
        @Override
        public void run() {
            SimpleAdapter adapter = new SimpleAdapter(getContext(), _result, R.layout.weather_list_item,
                    new String[]{"DATE", "TIME", "ICON", "TEMPERATURE"},
                    new int[]{R.id.date, R.id.time, R.id.weatherIcon, R.id.temperature});

            mListView.setAdapter(adapter);
        }
    }

    /**
     * Listの天気アイコンを取得するメソッド
     * @param weather
     * @return int
     */
    private int getListWeatherIcon(String weather) {
        int weatherIcon;
        switch (weather) {
            case "Clear":
                weatherIcon = (R.drawable.outline_sunny_24);
                break;
            case "Rain":
            case "Drizzle":
                weatherIcon = (R.drawable.rainy_24);
                break;
            case "Snow":
                weatherIcon = (R.drawable.outline_snow_24);
                break;
            case "Thunderstorm":
                weatherIcon = (R.drawable.outline_storm_24);
                break;
            default:
                weatherIcon = (R.drawable.outline_cloudy_24);
                break;
        }

        return weatherIcon;
    }

    /**
     * 表示する日付の形に加工するメソッド
     * @param string
     * @return String
     */
    private String getDate(String string) {
        String date;
        // 曜日取得
        int y = Integer.parseInt(string.substring(0, 4));
        int m = Integer.parseInt(string.substring(5, 7)) - 1;
        int d = Integer.parseInt(string.substring(8, 10));
        String[] week_name = {"(日)", "(月)", "(火)", "(水)",
                "(木)", "(金)", "(土)"};
        Calendar calendar = Calendar.getInstance();
        calendar.set(y, m, d);
        int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        String step1 = string.substring(5, 10);
        String result = step1.replace("-", "/");
        date = result + week_name[week];

        return date;
    }

    /**
     * 表示したい時間の形に加工するメソッド
     * @param string
     * @return String
     */
    private String getTime(String string) {
        String time = string.substring(11, 16);

        return time;
    }

    /**
     * 気温をケルビンから摂氏へ変換するメソッド
     * @param temp
     * @return String
     */
    private String getTemp(String temp) {
        int b = temp.indexOf(".");
        String c = temp.substring(0, b);
        String d = Integer.parseInt(c) - 273 + "℃";

        return d;
    }

    /**
     * InputStreamオブジェクトを文字列に変換するメソッド
     * @param is
     * @return String
     * @throws IOException
     */
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}