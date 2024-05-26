package com.github.tvbox.osc.ui.fragment;

import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.activity.AboutActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.VipActivity;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.ui.dialog.ApiDialog;
import com.github.tvbox.osc.ui.dialog.BackupDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.XWalkInitDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class ModelSettingFragment extends BaseLazyFragment {
    private TextView tvDebugOpen;
    private TextView tvMediaCodec;
    private TextView tvParseWebView;
    private TextView tvRender;
    private TextView tvApi;
    private TextView tvHomeApi;
    private TextView tvDns;
    private TextView tvSearchView;
    private TextView tvShowPreviewText;//小窗预览
    private TextView tvFastSearchText;

    public static ModelSettingFragment newInstance() {
        return new ModelSettingFragment().setArguments();
    }

    public ModelSettingFragment setArguments() {
        return this;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_model;
    }

    @Override
    protected void init() {
        tvShowPreviewText = findViewById(R.id.showPreviewText);
        tvShowPreviewText.setText(Hawk.get(HawkConfig.SHOW_PREVIEW, true) ? "开启" : "关闭");
        tvDebugOpen = findViewById(R.id.tvDebugOpen);
        tvParseWebView = findViewById(R.id.tvParseWebView);
        tvMediaCodec = findViewById(R.id.tvMediaCodec);
        tvRender = findViewById(R.id.tvRenderType);
        tvApi = findViewById(R.id.tvApi);
        tvHomeApi = findViewById(R.id.tvHomeApi);
        tvDns = findViewById(R.id.tvDns);
        tvSearchView = findViewById(R.id.tvSearchView);
        tvMediaCodec.setText(Hawk.get(HawkConfig.IJK_CODEC, ""));
        tvDebugOpen.setText(Hawk.get(HawkConfig.DEBUG_OPEN, false) ? "【开】" : "【关】");
        tvParseWebView.setText(Hawk.get(HawkConfig.PARSE_WEBVIEW, true) ? "系统自带" : "XWalkView");
        tvApi.setText(Hawk.get(HawkConfig.API_URL, ""));
        tvDns.setText(OkGoHelper.dnsHttpsList.get(Hawk.get(HawkConfig.DOH_URL, 0)));
        tvSearchView.setText(getSearchView(Hawk.get(HawkConfig.SEARCH_VIEW, 0)));
        tvHomeApi.setText(ApiConfig.get().getHomeSourceBean().getName());
        tvRender.setText(PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0)));
        findViewById(R.id.llDebug).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                Hawk.put(HawkConfig.DEBUG_OPEN, !Hawk.get(HawkConfig.DEBUG_OPEN, false));
                tvDebugOpen.setText(Hawk.get(HawkConfig.DEBUG_OPEN, false) ? "【开】" : "【关】");
            }
        });
        findViewById(R.id.llParseWebVew).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                boolean useSystem = !Hawk.get(HawkConfig.PARSE_WEBVIEW, true);
                Hawk.put(HawkConfig.PARSE_WEBVIEW, useSystem);
                tvParseWebView.setText(Hawk.get(HawkConfig.PARSE_WEBVIEW, true) ? "系统自带" : "XWalkView");
                if (!useSystem) {
                    Toast.makeText(mContext, "注意: XWalkView只适用于部分低Android版本，Android5.0以上推荐使用系统自带", Toast.LENGTH_LONG).show();
                    XWalkInitDialog dialog = new XWalkInitDialog(mContext);
                    dialog.setOnListener(new XWalkInitDialog.OnListener() {
                        @Override
                        public void onchange() {
                        }
                    });
                    dialog.show();
                }
            }
        });
        findViewById(R.id.llBackup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                BackupDialog dialog = new BackupDialog(mActivity);
                dialog.show();
            }
        });
        findViewById(R.id.llAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                jumpActivity(AboutActivity.class);
            }
        });
        findViewById(R.id.llHomeApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                List<SourceBean> sites = ApiConfig.get().getSourceBeanList();
                if (sites.size() > 0) {
                    SelectDialog<SourceBean> dialog = new SelectDialog<>(mActivity);
                    dialog.setTip("请选择首页数据源");
                    dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
                        @Override
                        public void click(SourceBean value, int pos) {
                            ApiConfig.get().setSourceBean(value);
                            tvHomeApi.setText(ApiConfig.get().getHomeSourceBean().getName());
                        }

                        @Override
                        public String getDisplay(SourceBean val) {
                            return val.getName();
                        }
                    }, new DiffUtil.ItemCallback<SourceBean>() {
                        @Override
                        public boolean areItemsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                            return oldItem == newItem;
                        }

                        @Override
                        public boolean areContentsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                            return oldItem.getKey().equals(newItem.getKey());
                        }
                    }, sites, sites.indexOf(ApiConfig.get().getHomeSourceBean()));
                    dialog.show();
                }
            }
        });
        findViewById(R.id.llDns).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int dohUrl = Hawk.get(HawkConfig.DOH_URL, 0);

                SelectDialog<String> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择安全DNS");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
                    @Override
                    public void click(String value, int pos) {
                        tvDns.setText(OkGoHelper.dnsHttpsList.get(pos));
                        Hawk.put(HawkConfig.DOH_URL, pos);
                        String url = OkGoHelper.getDohUrl(pos);
                        OkGoHelper.dnsOverHttps.setUrl(url.isEmpty() ? null : HttpUrl.get(url));
                        IjkMediaPlayer.toggleDotPort(pos > 0);
                    }

                    @Override
                    public String getDisplay(String val) {
                        return val;
                    }
                }, new DiffUtil.ItemCallback<String>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                        return oldItem.equals(newItem);
                    }
                }, OkGoHelper.dnsHttpsList, dohUrl);
                dialog.show();
            }
        });
        findViewById(R.id.llApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                ApiDialog dialog = new ApiDialog(mActivity);
                EventBus.getDefault().register(dialog);
                dialog.setOnListener(new ApiDialog.OnListener() {
                    @Override
                    public void onchange(String api) {
                        Hawk.put(HawkConfig.API_URL, api);
                        tvApi.setText(api);
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ((BaseActivity) mActivity).hideSysBar();
                        EventBus.getDefault().unregister(dialog);
                    }
                });
                dialog.show();
            }
        });
        findViewById(R.id.llMediaCodec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<IJKCode> ijkCodes = ApiConfig.get().getIjkCodes();
                if (ijkCodes == null || ijkCodes.size() == 0)
                    return;
                FastClickCheckUtil.check(v);

                int defaultPos = 0;
                String ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "");
                for (int j = 0; j < ijkCodes.size(); j++) {
                    if (ijkSel.equals(ijkCodes.get(j).getName())) {
                        defaultPos = j;
                        break;
                    }
                }

                SelectDialog<IJKCode> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择IJK解码");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<IJKCode>() {
                    @Override
                    public void click(IJKCode value, int pos) {
                        value.selected(true);
                        tvMediaCodec.setText(value.getName());
                    }

                    @Override
                    public String getDisplay(IJKCode val) {
                        return val.getName();
                    }
                }, new DiffUtil.ItemCallback<IJKCode>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                        return oldItem == newItem;
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                        return oldItem.getName().equals(newItem.getName());
                    }
                }, ijkCodes, defaultPos);
                dialog.show();
            }
        });

        switch (Hawk.get(HawkConfig.PLAY_SCALE, 0)) {
            case 0:
                findViewById(R.id.iv_setting_playeratio_default_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                break;
            case 1:
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                break;
            case 2:
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                break;
            case 3:
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                break;
            case 4:
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                break;
            case 5:
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setFocusable(true);
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.VISIBLE);
                break;
        }

        switch (Hawk.get(HawkConfig.PLAY_TYPE, 1)) {
            case 0:
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.GONE);
                break;
            case 1:
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.GONE);
                break;
            case 2:
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.VISIBLE);
                break;
        }

        switch (Hawk.get(HawkConfig.HOME_REC, 1)) {
            case 0:
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.GONE);
                break;
            case 1:
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.GONE);
                break;
            case 2:
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.VISIBLE);
                break;
        }

        findViewById(R.id.setting_llHomeRec_douBan_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.HOME_REC, 0);
            }
        });

        findViewById(R.id.setting_llHomeRec_recommend_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.HOME_REC, 1);
            }
        });

        findViewById(R.id.setting_llHomeRec_history_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_llHomeRec_douBan_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_recommend_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_llHomeRec_history_linear).setVisibility(View.VISIBLE);
                Hawk.put(HawkConfig.HOME_REC, 2);
            }
        });

        findViewById(R.id.setting_playercore_system_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_TYPE, 0);
            }
        });

        findViewById(R.id.setting_playercore_ijk_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_TYPE, 1);
            }
        });
        findViewById(R.id.setting_playercore_exo_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playercore_system_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_ijk_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playercore_exo_linear).setVisibility(View.VISIBLE);
                Hawk.put(HawkConfig.PLAY_TYPE, 2);
            }
        });

        findViewById(R.id.setting_playeratio_default_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_SCALE, 0);
            }
        });
        findViewById(R.id.setting_playeratio_16_9_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_SCALE, 1);
            }
        });
        findViewById(R.id.setting_playeratio_4_3_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_SCALE, 2);
            }
        });
        findViewById(R.id.setting_playeratio_tianchong_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_SCALE, 3);
            }
        });
        findViewById(R.id.setting_playeratio_yuanshi_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.VISIBLE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.GONE);
                Hawk.put(HawkConfig.PLAY_SCALE, 4);
            }
        });
        findViewById(R.id.setting_playeratio_caijian_linear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.iv_setting_playeratio_default_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_16_9_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_4_3_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_tianchong_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_yuanshi_linear).setVisibility(View.GONE);
                findViewById(R.id.iv_setting_playeratio_caijian_linear).setVisibility(View.VISIBLE);
                Hawk.put(HawkConfig.PLAY_SCALE, 5);
            }
        });

        findViewById(R.id.llRender).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0);
                ArrayList<Integer> renders = new ArrayList<>();
                renders.add(0);
                renders.add(1);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择默认渲染方式");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.PLAY_RENDER, value);
                        tvRender.setText(PlayerHelper.getRenderName(value));
                        PlayerHelper.init();
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return PlayerHelper.getRenderName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, renders, defaultPos);
                dialog.show();
            }
        });

        findViewById(R.id.llSearchView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.SEARCH_VIEW, 0);
                ArrayList<Integer> types = new ArrayList<>();
                types.add(0);
                types.add(1);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择搜索视图");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.SEARCH_VIEW, value);
                        tvSearchView.setText(getSearchView(value));
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return getSearchView(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, types, defaultPos);
                dialog.show();
            }
        });
        SettingActivity.callback = new SettingActivity.DevModeCallback() {
            @Override
            public void onChange() {
                findViewById(R.id.llDebug).setVisibility(View.VISIBLE);
            }
        };
        findViewById(R.id.showPreview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                Hawk.put(HawkConfig.SHOW_PREVIEW, !Hawk.get(HawkConfig.SHOW_PREVIEW, true));
                tvShowPreviewText.setText(Hawk.get(HawkConfig.SHOW_PREVIEW, true) ? "开启" : "关闭");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SettingActivity.callback = null;
    }

    String getSearchView(int type) {
        if (type == 0) {
            return "缩略图";
        } else {
            return "文字列表";
        }
    }
}