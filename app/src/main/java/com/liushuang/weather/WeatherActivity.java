package com.liushuang.weather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.PoiRegion;
import com.bumptech.glide.Glide;
import com.liushuang.weather.gson.Forecast;
import com.liushuang.weather.gson.Weather;
import com.liushuang.weather.service.AutoUpdateService;
import com.liushuang.weather.utils.HttpUtil;
import com.liushuang.weather.utils.Utility;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingImg;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private String mWeatherId;
    public DrawerLayout mDrawerLayout;
    private ImageButton mBtnHome;
    private LinearLayout mLlLocation;
    private String mCityName1;

    private LocationClient mLocationClient;
    private TextView mTvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        setContentView(R.layout.activity_weather);

        // 初始化各控件
        mLlLocation = findViewById(R.id.id_ll_location);
        mDrawerLayout = findViewById(R.id.id_dl_drawerLayout);
        mBtnHome = findViewById(R.id.id_nav_button);
        mSwipeRefreshLayout = findViewById(R.id.id_srl_swipeRefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.teal_700);
        bingImg = findViewById(R.id.id_iv_bingImg);
        weatherLayout = (ScrollView) findViewById(R.id.id_sv_weather);
        titleCity = (TextView) findViewById(R.id.id_tv_titleCity);
        titleUpdateTime = (TextView) findViewById(R.id.id_tv_updateTime);
        degreeText = (TextView) findViewById(R.id.id_tv_degree);
        weatherInfoText = (TextView) findViewById(R.id.id_tv_weatherInfo);
        forecastLayout = (LinearLayout) findViewById(R.id.id_ll_forecast);
        aqiText = (TextView) findViewById(R.id.id_tv_aqi);
        pm25Text = (TextView) findViewById(R.id.id_tv_pm25);
        comfortText = (TextView) findViewById(R.id.id_tv_comfort);
        carWashText = (TextView) findViewById(R.id.id_tv_carWash);
        sportText = (TextView) findViewById(R.id.id_tv_sport);
        mTvLocation = findViewById(R.id.id_tv_location);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = sharedPreferences.getString("bing_pic", null);
        String weatherString = sharedPreferences.getString("weather", null);
//        Log.d(TAG, "onCreate: weather = " + weatherString);

        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingImg);
        } else {
            loadBingPic();
        }
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        mLlLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLocation", true);
                editor.apply();
                requestLocation();
            }
        });
        mBtnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    private void requestLocation() {
        LocationClientOption option = new LocationClientOption();
//        option.setScanSpan(10*60*1000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    /**
     * 根据城市天气id请求城市天气信息
     * @param weatherId
     */
    public void requestWeather(String weatherId) {
        Log.d(TAG, "requestWeather: weatherId = " + weatherId);
        mWeatherId = weatherId;
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=cc317b887ced43a9a7d1231f47ec83b8";
        Log.d(TAG, "requestWeather: weatherUrl = " + weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: IOException = " + e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Weather weather = Utility.handleWeatherResponse(responseText);
                Log.d(TAG, "onResponse: weather = " + weather);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            Log.d(TAG, "run: weather = " + weather);
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    private void loadBingPic() {
        String urlBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(urlBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic1 = response.body().string();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("bing_pic", bingPic1);
                editor.apply();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic1).into(bingImg);
                    }
                });
            }
        });
    }

    private String getCurrentTime(){
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日 HH:mm");
        return simpleDateFormat.format(date);
    }
    /**
     * 处理并展示Weather实体类中的数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLocation = sharedPreferences.getBoolean("isLocation", false);
        if (isLocation){
            titleCity.setText(mCityName1);
            mTvLocation.setText(mCityName1);
        }else {
            titleCity.setText(cityName);
            mTvLocation.setText("获取定位");
        }
//        titleUpdateTime.setText(updateTime);
        titleUpdateTime.setText(getCurrentTime());
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.id_tv_date);
            TextView infoText = (TextView) view.findViewById(R.id.id_tv_info);
            TextView maxText = (TextView) view.findViewById(R.id.id_tv_max);
            TextView minText = (TextView) view.findViewById(R.id.id_tv_min);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();

    }
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            /*String locationType = null;
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation){
                locationType = "GPS";
            }else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){
                locationType = "网络";
            }

            String locationInfo = "维度：" + bdLocation.getLatitude() + "\n"
                    + "经度：" + bdLocation.getLongitude() + "\n"
                    + "定位方式：" + locationType;*/

//            StringBuilder currentPosition = new StringBuilder();
//            currentPosition.append("纬度：").append(bdLocation.getLatitude()).
//                    append("\n");
//            currentPosition.append("经线：").append(bdLocation.getLongitude()).
//                    append("\n");
//            currentPosition.append("国家：").append(bdLocation.getCountry()).
//                    append("\n");
//            currentPosition.append("省：").append(bdLocation.getProvince()).
//                    append("\n");
//            currentPosition.append("市：").append(bdLocation.getCity()).
//                    append("\n");
//            currentPosition.append("区：").append(bdLocation.getDistrict()).
//                    append("\n");
//            currentPosition.append("街道：").append(bdLocation.getStreet()).
//                    append("\n");
//            currentPosition.append("定位方式：");
//            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
//                currentPosition.append("GPS");
//            } else if (bdLocation.getLocType() ==
//                    BDLocation.TypeNetWorkLocation) {
//                currentPosition.append("网络");
//            }
//
////            Log.d(TAG, "onReceiveLocation: locationInfo = " + locationInfo);
//            Log.d(TAG, "onReceiveLocation: locationInfo = " + currentPosition.toString());


            requestWeather(mWeatherId);
            mCityName1 = bdLocation.getDistrict();

            Log.d(TAG, "onReceiveLocation: mCityName1 = " + mCityName1);
            mLocationClient.stop();
        }
    }
}