package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.StringUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.beanry.InitBean;
import com.github.tvbox.osc.beanry.ReJieXiBean;
import com.github.tvbox.osc.beanry.ReLevelBean;
import com.github.tvbox.osc.beanry.ReUserBean;
import com.github.tvbox.osc.beanry.SiteBean;
import com.github.tvbox.osc.util.BaseR;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MMkvUtils;
import com.github.tvbox.osc.util.MacUtils;
import com.github.tvbox.osc.util.ToolUtils;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

public class StartActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private LinearLayout ll_ok_tiao;

    private String Mac;
    private int start_time = 5;
    private boolean isLogin = true;
    private boolean isClosed = false;
    private boolean isCloseds = false;
    private final Handler handler = new Handler();
    private static final String TAG = "StartActivity";

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            setTime(start_time);
            start_time -= 1;
            if (start_time >= 0 && !isClosed) {
                handler.postDelayed(runnable, 1000);
            } else {
                goMain();
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            goMain();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (HawkConfig.APP_ID.contains("demo")){
            HawkConfig.APP_ID = HawkConfig.APP_ID.replaceAll("demo","");
        }

        if (Hawk.get(HawkConfig.HOME_REC, 8) == 8){ //是否设置过首页数据
            Hawk.put(HawkConfig.HOME_REC, 0); //设置首页默认数据
        }
        Log.d(TAG, "HawkConfig.APP_ID: "+HawkConfig.APP_ID);
        textView = findViewById(R.id.tv_start);
        imageView = findViewById(R.id.iv_image);
        ll_ok_tiao = findViewById(R.id.ll_ok_tiao);

        Mac = MacUtils.getMac(true);
        if (Build.VERSION.SDK_INT >= 26 || Mac == null || Mac.contains("00:00")) {
            Mac = ToolUtils.getAndroidId(StartActivity.this);
        }

        findViewById(R.id.tv_TTime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goMain();
            }
        });
        getSite();
        getJieXi();
        getAppIni();
        getMyBanner();
    }

    private void goMain() {
        start_time = 0;
        isClosed = true;
        handler.removeCallbacks(runnable);
        if (!isCloseds) {
            isCloseds = true;
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setTime(int i) {
        if (ll_ok_tiao.getVisibility() == View.GONE) {
            ll_ok_tiao.setVisibility(View.VISIBLE);
        }
        runOnUiThread(() -> {
            if (textView != null) {
                textView.setText(StringUtils.getString(R.string.skip, i));
            }
        });
    }

    private void getSite() { //获取自定义站点
        new Thread(() -> {
            OkGo.<String>post(ToolUtils.setApi("site"))
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("null"))
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onStart(Request<String, ? extends Request> request) {
                            Log.d(TAG, "onStart: " + request.getCacheKey());
                        }

                        @Override
                        public void onSuccess(Response<String> response) {
                            if (ToolUtils.iniData(response, StartActivity.this)) {
                                SiteBean siteDta = new Gson().fromJson(BaseR.decry_R(response.body()), SiteBean.class);
                                MMkvUtils.saveSiteBean(siteDta);
                            }
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private void getJieXi() { //获取解析接口
        new Thread(() -> {
            OkGo.<String>post(ToolUtils.setApi("exten"))
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("null"))
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            if (ToolUtils.iniData(response, StartActivity.this)) {
                                ReJieXiBean reJieXiBean = new Gson().fromJson(BaseR.decry_R(response.body()), ReJieXiBean.class);
                                MMkvUtils.saveReJieXiBean(reJieXiBean);
                            }
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private void getMyBanner() {
        new Thread(() -> {
            OkGo.<String>post(ToolUtils.setApi("level"))
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("null"))
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            if (ToolUtils.iniData(response, StartActivity.this)) {
                                ReLevelBean LevelBean = new Gson().fromJson(BaseR.decry_R(response.body()), ReLevelBean.class);
                                MMkvUtils.saveReLevelBean(LevelBean);
                            }
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private void getAppIni() { //获取应用配置
        new Thread(() -> {
            OkGo.<String>post(ToolUtils.setApi("ini") + "&pay")
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("pay"))
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            if (ToolUtils.iniData(response, StartActivity.this)) {
                                InitBean initData = new Gson().fromJson(BaseR.decry_R(response.body()), InitBean.class);
                                if (initData.code == 200) {
                                    String apiJson = initData.msg.appJson;
                                    if (!ToolUtils.getIsEmpty(apiJson) && ToolUtils.getIsEmpty(initData.msg.appJsonb)) {
                                        apiJson = initData.msg.appJsonb;
                                    }
                                    Hawk.put(HawkConfig.JSON_URL, apiJson); //保存聚合接口

                                    String startupAd = initData.msg.uiStartad;
                                    if (ToolUtils.getIsEmpty(startupAd)) {
                                        setQiDong(startupAd);
                                    }

                                    if (ToolUtils.getIsEmpty(initData.msg.logonWay)) {
                                        switch (initData.msg.logonWay) {
                                            case "0":
                                                if (!MMkvUtils.loadUser().equals("") && !MMkvUtils.loadPasswd().equals("")) {
                                                    reLoginReg(MMkvUtils.loadUser(), MMkvUtils.loadPasswd());
                                                }
                                                break;
                                            case "1":
                                                Log.d(TAG, "卡密登录");
                                                break;
                                            case "2":
                                                if (!MMkvUtils.loadUser().equals("") && !MMkvUtils.loadPasswd().equals("")) {
                                                    reLoginReg(MMkvUtils.loadUser(), MMkvUtils.loadPasswd());
                                                } else {
                                                    reLoginReg(Mac, "12345678");
                                                }
                                                break;
                                        }
                                    }

                                    MMkvUtils.saveInitBean(initData);
                                    handler.postDelayed(runnable, 1000);
                                }
                            }
                        }

                        @Override
                        public void onError(final Response<String> error) {
                            handler.postDelayed(runnable, 1000);
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private void reLoginReg(String user, String passwd) { //登录注册
        new Thread(() -> {
            String act;
            if (!isLogin) {
                act = "user_reg";
            } else {
                act = "user_logon";
            }
            Log.d(TAG, "reLoginReg: " + act);
            OkGo.<String>post(ToolUtils.setApi(act))
                    .params("user", user)
                    .params("account", user)
                    .params("password", passwd)
                    .params("markcode", ToolUtils.getAndroidId(StartActivity.this))
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("user=" + user + "&account=" + user + "&password=" + passwd + "&markcode=" + ToolUtils.getAndroidId(StartActivity.this)))
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            try {
                                JSONObject jo = new JSONObject(BaseR.decry_R(response.body()));
                                if (jo.getInt("code") == 200) { //成功
                                    if (isLogin) { //登录成功
                                        MMkvUtils.saveUser(user);
                                        MMkvUtils.savePasswd(passwd);
                                        Log.d(TAG, "reLoginReg: " + BaseR.decry_R(response.body()));
                                        ReUserBean userData = new Gson().fromJson(BaseR.decry_R(response.body()), ReUserBean.class);
                                        MMkvUtils.saveReUserBean(userData);
                                    } else { //注册成功
                                        isLogin = true;
                                        reLoginReg(user, passwd);
                                        MMkvUtils.saveReUserBean(null);
                                        Log.d(TAG, "reLoginReg: " + BaseR.decry_R(response.body()));
                                    }
                                } else {
                                    if (isLogin) { //登录失败
                                        if (jo.getInt("code") == 114){ //账号被禁用
                                            Toast.makeText(StartActivity.this, jo.getString("msg"), Toast.LENGTH_SHORT).show();
                                        }else{ //账号或密码错误，去注册
                                            isLogin = false;
                                            reLoginReg(user, passwd);
                                        }
                                    } else { //注册失败
                                        Log.d(TAG, "reLoginReg: " + BaseR.decry_R(response.body()));
                                        Toast.makeText(StartActivity.this, jo.getString("msg"), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private void setQiDong(String imgUrl) {
        imageView.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imgUrl)
                .centerCrop()
                .override(0, 0) //默认淡入淡出动画
                .transition(DrawableTransitionOptions.withCrossFade()) //缓存策略,跳过内存缓存【此处应该设置为false，否则列表刷新时会闪一下】
                .skipMemoryCache(false) //缓存策略,硬盘缓存-仅仅缓存最终的图像，即降低分辨率后的（或者是转换后的）
                .diskCacheStrategy(DiskCacheStrategy.ALL) //设置图片加载的优先级
                .priority(Priority.HIGH)
                .into(imageView);
    }
}

