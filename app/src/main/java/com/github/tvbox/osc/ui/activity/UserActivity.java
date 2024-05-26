package com.github.tvbox.osc.ui.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.beanry.ReLevelBean;
import com.github.tvbox.osc.beanry.ReUserBean;
import com.github.tvbox.osc.beanry.InitBean;
import com.github.tvbox.osc.beanry.UserInfoBean;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.ui.dialog.WeiXinDialog;
import com.github.tvbox.osc.util.BaseR;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.MMkvUtils;
import com.github.tvbox.osc.util.ToolUtils;
import com.github.tvbox.osc.util.WiFiDialog;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class UserActivity extends BaseActivity {
    private static final String TAG = "UserActivity";
    private TextView tvUserMac, tvUserPoints, tvUserEndTime;
    private LinearLayout llUserRefresh;
    private ImageView lvUserRefresh, ll_User_ads, user_activity_pic;
    private TextView tv_Button_name, lluserRefreshtext;
    private boolean KOJAK = true;
    @SuppressLint("HandlerLeak")
    private Handler Screensaver = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (KOJAK) {
                KOJAK = false;
                ReLevelBean LevelBean = MMkvUtils.loadReLevelBean("");
                if (LevelBean != null && LevelBean.msg.size() > 0){
                    jumpActivity(MyBanner.class);
                }
            }
            Screensaver.removeMessages(1);
        }
    };

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        Screensaver.removeMessages(1);
        Screensaver.sendEmptyMessageDelayed(1, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Screensaver.removeCallbacksAndMessages(null);
    }

    public void setNetworkBitmap(String url) {
        Glide.with(this)
                .load(url)
                .error(R.drawable.button3)
                .into(ll_User_ads);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void init() {
        KOJAK = true;
        Screensaver.sendEmptyMessageDelayed(1, 60000);
        tvUserMac = findViewById(R.id.llUserMac);
        tvUserPoints = findViewById(R.id.llUserPrice);
        tvUserEndTime = findViewById(R.id.llUserEndTime);
        lvUserRefresh = findViewById(R.id.lv_user_Refresh);
        llUserRefresh = findViewById(R.id.ll_user_Refresh);
        lluserRefreshtext = findViewById(R.id.ll_user_Refresh_text);
        tv_Button_name = findViewById(R.id.tv_Button_name);
        ll_User_ads = findViewById(R.id.ll_user_ads);
        user_activity_pic = findViewById(R.id.user_activity_pic);
        initData();
        onClickM();
    }

    private InitBean initData;
    private ReUserBean userData;

    @SuppressLint("SetTextI18n")
    private void initData() {
        initData = MMkvUtils.loadInitBean("");
        if (initData != null) {
            if (ToolUtils.getIsEmpty(initData.msg.uiCommunity)) {
                if (initData.msg.uiCommunity.contains("|")) {
                    String[] buttonTitle = initData.msg.uiCommunity.split("\\|");
                    tv_Button_name.setText(buttonTitle[0]);
                } else {
                    tv_Button_name.setText(initData.msg.uiCommunity);
                }
            } else {
                tv_Button_name.setVisibility(View.GONE);
            }
            if (ToolUtils.getIsEmpty(initData.msg.uiButton3backg)) {
                setNetworkBitmap(initData.msg.uiButton3backg);
            }
        }

        userData = MMkvUtils.loadReUserBean("");
        if (userData != null && ToolUtils.getIsEmpty(userData.msg.token)) {
            lluserRefreshtext.setText("刷新");
            lvUserRefresh.setVisibility(View.VISIBLE);
            findViewById(R.id.fl_user_in).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_user_pic).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_user_logins).setVisibility(View.GONE);
            if (ToolUtils.getIsEmpty(MMkvUtils.loadUser())) {
                tvUserMac.setText("用户：" + MMkvUtils.loadUser());
            }else{
                tvUserMac.setText("用户：" + userData.msg.info.name);
            }
            tvUserPoints.setText("积分：" + userData.msg.info.fen);
            tvUserEndTime.setText("SVIP：" + ToolUtils.stampToDate(userData.msg.info.vip));
            getUserInfo(userData.msg.token, false);

            Glide.with(this)
                    .load(userData.msg.info.pic)
                    .error(R.drawable.channel_user_avatar_default)
                    .into(user_activity_pic);
        } else {
            findViewById(R.id.fl_user_in).setVisibility(View.GONE);
            findViewById(R.id.ll_user_pic).setVisibility(View.GONE);
            findViewById(R.id.ll_user_logins).setVisibility(View.VISIBLE);
            findViewById(R.id.user_fragment_Logout).setVisibility(View.GONE);
            lluserRefreshtext.setText("登录");
        }
    }

    @SuppressLint("SetTextI18n")
    private void onClickM() {
        findViewById(R.id.ll_user_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserDialog();
            }
        });
        findViewById(R.id.ll_user_Refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lluserRefreshtext.getText().toString().equals("刷新")) {
                    if (userData !=null && ToolUtils.getIsEmpty(userData.msg.token)){
                        lvUserRefresh.setVisibility(View.VISIBLE);
                        getUserInfo(userData.msg.token, true);
                    }else{
                        ToolUtils.showToast(mContext, "已是最新数据", R.drawable.toast_smile);
                    }
                } else {
                    showUserDialog();
                }
            }
        });
        findViewById(R.id.cashCoupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpActivity(KamActivity.class);
            }
        });
        findViewById(R.id.pointsMall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolUtils.showToast(mContext, "暂未开放", R.drawable.toast_smile);
            }
        });
        findViewById(R.id.ll_user_openVip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (initData != null) {
                    if (initData.msg.pay.state.equals("y")) {
                        jumpActivity(VipActivity.class);
                    } else {
                        jumpActivity(VipCardActivity.class);
                    }
                } else {
                    ToolUtils.showToast(mContext, "暂未开放", R.drawable.toast_err);
                }
            }
        });
        ll_User_ads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (initData != null && ToolUtils.getIsEmpty(initData.msg.uiButtonadimg)) {
                    WeiXinDialog dialog = new WeiXinDialog(mContext);
                    dialog.show();
                }else{
                    ToolUtils.showToast(mContext, "敬请期待", R.drawable.toast_err);
                }
            }
        });
        findViewById(R.id.user_System_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                jumpActivity(SettingActivity.class);
            }
        });
        findViewById(R.id.user_fragment_Feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                AboutDialog dialog = new AboutDialog(mContext);
                dialog.show();
            }
        });
        findViewById(R.id.user_fragment_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                jumpActivity(AboutActivity.class);
            }
        });
        findViewById(R.id.user_fragment_Logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (userData != null && ToolUtils.getIsEmpty(userData.msg.token)) {
                    findViewById(R.id.lv_user_Refresh).setVisibility(View.VISIBLE);
                    MMkvUtils.saveUser(null);
                    MMkvUtils.savePasswd(null);
                    MMkvUtils.saveReUserBean(null);
                    Toast.makeText(UserActivity.this, "退出登录", Toast.LENGTH_SHORT).show();
                    jumpActivity(HomeActivity.class);
                } else {
                    MMkvUtils.saveReUserBean(null);
                    Toast.makeText(UserActivity.this, "状态错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 显示用户登录注册的dialog
     */
    private void showUserDialog() {
        WiFiDialog.Builder builder = new WiFiDialog.Builder(mContext);
        View mView = View.inflate(mContext, R.layout.user_form, null);
        final EditText user_name_et = (EditText) mView.findViewById(R.id.user_name_et);
        final EditText user_pass_et = (EditText) mView.findViewById(R.id.user_pass_et);
        builder.setContentView(mView);

        builder.setPositiveButton("登录", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestServer("user_logon", user_name_et, user_pass_et);
            }
        });

        builder.setNeutralButton("注册", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestServer("user_reg", user_name_et, user_pass_et);
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    /**
     * 请求服务器
     *
     * @param uNameET    用户名输入框
     * @param uPassET    密码输入框
     */
    private void requestServer(String act, EditText uNameET, EditText uPassET) {
        //获取数据
        final String userName = uNameET.getText().toString().trim();
        final String userPassWord = uPassET.getText().toString().trim();
        //非空判断
        if (TextUtils.isEmpty(userName)) {
            ToolUtils.showToast(mContext, "您还没输入账号", R.drawable.toast_err);
            return;
        }
        if (userName.length() < 5) {
            ToolUtils.showToast(mContext, "输入的账号小于6位", R.drawable.toast_err);
            return;
        }
        if (TextUtils.isEmpty(userPassWord)) {
            ToolUtils.showToast(mContext, "您还没输入密码", R.drawable.toast_err);
            return;
        }
        if (act.equals("user_reg")) {
            ToolUtils.loadingShow_tv(mContext, R.string.is_registing);
        } else {
            ToolUtils.loadingShow_tv(mContext, R.string.is_loading);
        }
        LoginRegs(act, userName, userPassWord);
    }

    private void LoginRegs(String act, String user, String passwd) { //注册登录
        new Thread(() -> {
            OkGo.<String>post(ToolUtils.setApi(act))
                    .params("user", user)
                    .params("account", user)
                    .params("password", passwd)
                    .params("markcode", ToolUtils.getAndroidId(UserActivity.this))
                    .params("t", System.currentTimeMillis() / 1000)
                    .params("sign", ToolUtils.setSign("user="+user+"&account="+user+"&password="+passwd+"&markcode="+ToolUtils.getAndroidId(this)))
                    .execute(new AbsCallback<String>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(Response<String> response) {
                            try {
                                JSONObject jo = new JSONObject(BaseR.decry_R(response.body()));
                                if (jo.getInt("code") == 200) { //成功
                                    if (act.equals("user_logon")) {
                                        MMkvUtils.saveUser(user);
                                        MMkvUtils.savePasswd(passwd);
                                        ToolUtils.showToast(mContext, "登录成功", R.drawable.toast_smile);
                                        ReUserBean userData = new Gson().fromJson(BaseR.decry_R(response.body()), ReUserBean.class);
                                        if (ToolUtils.getIsEmpty(MMkvUtils.loadUser())) {
                                            tvUserMac.setText("用户：" + MMkvUtils.loadUser());
                                        }else{
                                            tvUserMac.setText("用户：" + userData.msg.info.name);
                                        }
                                        tvUserPoints.setText("积分：" + userData.msg.info.fen);
                                        tvUserEndTime.setText("SVIP：" + ToolUtils.stampToDate(userData.msg.info.vip));
                                        MMkvUtils.saveReUserBean(userData);
                                        lluserRefreshtext.setText("刷新");
                                        findViewById(R.id.fl_user_in).setVisibility(View.VISIBLE);
                                        findViewById(R.id.ll_user_pic).setVisibility(View.VISIBLE);
                                        findViewById(R.id.ll_user_logins).setVisibility(View.GONE);
                                        findViewById(R.id.user_fragment_Logout).setVisibility(View.VISIBLE);
                                        if (mDialog != null && mDialog.isShowing()) {
                                            mDialog.dismiss();
                                        }
                                    } else {
                                        MMkvUtils.saveReUserBean(null);
                                        LoginRegs("user_logon", user, passwd);
                                    }
                                    findViewById(R.id.lv_user_Refresh).setVisibility(View.GONE);
                                } else {
                                    MMkvUtils.saveReUserBean(null);
                                    ToolUtils.showToast(mContext, jo.getString("msg"), R.drawable.toast_err);
                                }
                            } catch (JSONException e) {
                                MMkvUtils.saveReUserBean(null);
                                e.printStackTrace();
                            }
                            ToolUtils.loadingClose_Tv();
                        }

                        public void onError(Response<String> error) {
                            ToolUtils.loadingClose_Tv();
                            ToolUtils.showToast(mContext, "未知错误", R.drawable.toast_err);
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            assert response.body() != null;
                            return response.body().string();
                        }
                    });
        }).start();
    }

    private Dialog mDialog;

    @SuppressLint("SetTextI18n")
    private void getUserInfo(String token, boolean i) {
        OkGo.<String>post(ToolUtils.setApi("get_info"))
                .params("token", token)
                .params("t", System.currentTimeMillis() / 1000)
                .params("sign", ToolUtils.setSign("token="+token))
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject jo = new JSONObject(BaseR.decry_R(response.body()));
                                    if (jo.getInt("code") == 200){
                                        UserInfoBean userInfoData = new Gson().fromJson(BaseR.decry_R(response.body()), UserInfoBean.class);
                                        userData.msg.info.vip = userInfoData.msg.vip;
                                        userData.msg.info.fen = userInfoData.msg.fen;
                                        userData.msg.info.name = userInfoData.msg.name;
                                        if (ToolUtils.getIsEmpty(MMkvUtils.loadUser())) {
                                            tvUserMac.setText("用户：" + MMkvUtils.loadUser());
                                        }else{
                                            tvUserMac.setText("用户：" + userData.msg.info.name);
                                        }
                                        tvUserPoints.setText("积分：" + userInfoData.msg.fen);
                                        tvUserEndTime.setText("SVIP：" + ToolUtils.stampToDate(userInfoData.msg.vip));
                                        MMkvUtils.saveReUserBean(userData);
                                        findViewById(R.id.user_fragment_Logout).setVisibility(View.VISIBLE);
                                        if (i){
                                            Toast.makeText(UserActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
                                        }
                                        findViewById(R.id.lv_user_Refresh).setVisibility(View.GONE);
                                    }else{
                                        if (i){
                                            Toast.makeText(UserActivity.this, "您的账号在其他设备登录！您已被迫下线", Toast.LENGTH_SHORT).show();
                                        }
                                        lluserRefreshtext.setText("登录");
                                        MMkvUtils.saveReUserBean(null);
                                        findViewById(R.id.fl_user_in).setVisibility(View.GONE);
                                        findViewById(R.id.ll_user_pic).setVisibility(View.GONE);
                                        findViewById(R.id.ll_user_logins).setVisibility(View.VISIBLE);
                                        findViewById(R.id.user_fragment_Logout).setVisibility(View.GONE);
                                    }
                                } catch (JSONException e) {
                                    if (i){
                                        Toast.makeText(UserActivity.this, "刷新失败", Toast.LENGTH_SHORT).show();
                                    }
                                    MMkvUtils.saveReUserBean(null);
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Response<String> error) {
                        Toast.makeText(UserActivity.this, "未知错误", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        assert response.body() != null;
                        return response.body().string();
                    }
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_user;
    }
}
