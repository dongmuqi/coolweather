package aappbartest.com.seven.coolweather;

import aappbartest.com.seven.coolweather.gson.Forecast;
import aappbartest.com.seven.coolweather.gson.Weather;
import aappbartest.com.seven.coolweather.util.HttpUtil;
import aappbartest.com.seven.coolweather.util.Utility;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

  @BindView(R.id.weather_layout) ScrollView mWeatherLayout;
  @BindView(R.id.txt_title_city) TextView mTitleCity;
  @BindView(R.id.txt_title_update_time) TextView mTitleDateTime;
  @BindView(R.id.txt_degree_text) TextView mDereeText;
  @BindView(R.id.txt_weather_info_text) TextView mWeatherInfoText;
  @BindView(R.id.forecast_layout) LinearLayout mForecastLayout;
  @BindView(R.id.txt_aqi_text) TextView mAqiText;
  @BindView(R.id.txt_pm25_text) TextView mPm25Text;
  @BindView(R.id.txt_comfort_text) TextView mComfortText;
  @BindView(R.id.txt_car_wash_text) TextView mCarWashText;
  @BindView(R.id.txt_sport_test) TextView mSportText;
  @BindView(R.id.bing_pic_img) ImageView bingPicImg;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //沉浸式
    if (Build.VERSION.SDK_INT>=21){
      View decorView=getWindow().getDecorView();
      decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
      getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    setContentView(R.layout.activity_weather);
    ButterKnife.bind(this);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String weatherString = prefs.getString("weather", null);
    if (weatherString != null) {
      //有缓存时直接解析天气
      Weather weather = Utility.handleWeatherResponse(weatherString);
      showWeatherInfo(weather);
    } else {
      //无缓存时去服务器查询天气
      String weatherId = getIntent().getStringExtra("weather_id");
      mWeatherLayout.setVisibility(View.INVISIBLE);
      requestWeather(weatherId);
    }
    String bingPic = prefs.getString("bing_pic", null);
    if (bingPic != null) {
      Glide.with(this).load(bingPic).into(bingPicImg);
    } else {
      loadBingPic();
    }
  }

  //根据天气id请求城市天气信息
  private void requestWeather(final String weatherId) {
    String weatherUrl = "http://guolin.tech/api/weather?cityid="
        + weatherId
        + "&key=bd565fc885ca4c22be71c619d6acc6c9";
    HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

      @Override public void onResponse(Call call, Response response) throws IOException {

        final String responseText = response.body().string();
        final Weather weather = Utility.handleWeatherResponse(responseText);
        runOnUiThread(new Runnable() {
          @Override public void run() {

            if (weather != null && "ok".equals(weather.status)) {
              SharedPreferences.Editor editor =
                  PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
              editor.putString("weather", responseText);
              editor.apply();
              showWeatherInfo(weather);
            } else {
              Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
            }
          }
        });
        loadBingPic();
      }

      @Override public void onFailure(Call call, IOException e) {
        e.printStackTrace();
        runOnUiThread(new Runnable() {
          @Override public void run() {
            Toast.makeText(WeatherActivity.this, "获取天气信息失败:202", Toast.LENGTH_SHORT).show();
          }
        });
      }
    });
  }

  //处理并展示weather实体类中的数据
  private void showWeatherInfo(Weather weather) {
    String cityName = weather.basic.cityName;
    String updateTime = weather.basic.update.updateTime.split(" ")[1];
    String degree = weather.now.tmperature + "`C";
    String weatherInfo = weather.now.more.info;

    mTitleCity.setText(cityName);
    mTitleDateTime.setText(updateTime);
    mDereeText.setText(degree);
    mWeatherInfoText.setText(weatherInfo);

    mForecastLayout.removeAllViews();
    for (Forecast forecast : weather.forecastList) {
      //将参数1的布局 填充到参数2的控件内
      View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastLayout, false);
      TextView dataText = (TextView) view.findViewById(R.id.txt_date_text);
      TextView infoText = (TextView) view.findViewById(R.id.txt_info_text);
      TextView maxText = (TextView) view.findViewById(R.id.txt_max_text);
      TextView minText = (TextView) view.findViewById(R.id.txt_min_text);
      dataText.setText(forecast.date);
      infoText.setText(forecast.more.info);
      maxText.setText(forecast.temperature.max);
      minText.setText(forecast.temperature.min);
      mForecastLayout.addView(view);
    }
    if (weather.aqi != null) {
      mAqiText.setText(weather.aqi.city.aqi);
      mPm25Text.setText(weather.aqi.city.pm25);
    }
    String comfor = "舒适度:" + weather.suggestion.comfort.info;
    String carWash = "洗车指数:" + weather.suggestion.carWash.info;
    String sport = "运动建议:" + weather.suggestion.sport.info;
    mCarWashText.setText(carWash);
    mSportText.setText(sport);
    mComfortText.setText(comfor);
    mWeatherLayout.setVisibility(View.VISIBLE);
  }

  private void loadBingPic() {

    String requestBingPic = "http://guolin.tech/api/bing_pic";
    HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
      @Override public void onFailure(Call call, IOException e) {
        e.printStackTrace();
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        final String bingPic = response.body().string();
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
        editor.putString("bing_pic", bingPic);
        editor.apply();
        runOnUiThread(new Runnable() {
          @Override public void run() {
            Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
          }
        });
      }
    });
  }
}
