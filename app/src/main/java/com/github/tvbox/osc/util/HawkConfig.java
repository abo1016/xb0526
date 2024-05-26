package com.github.tvbox.osc.util;

import android.util.Base64;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class HawkConfig {

    public static boolean FORCE_pause = false;
    public static String APP_ID = "10000"; //应用ID2
    public static String SEN_SUS = ""; //百度统计
    public static String APP_Channel = ""; //渠道随意
    public static final String BASE_URL_ENC = "aHR0cDovL2lwdHYuc2tp";
    public static String MMM_MMM = new String(Base64.decode(HawkConfig.BASE_URL_ENC.getBytes(), Base64.DEFAULT));
    public static String API_KEY = "2nx7mxAdP5ETXmZkB3d7s3cn5bhd3d8f"; //接口密钥

    public static final String DOH_URL = "doh_url";
    public static final String API_URL = "api_url";
    public static final String SHOW_PREVIEW = "show_preview";//视频小窗
    public static final String HOME_API = "home_api";
    public static final String JSON_URL = "json_url";
    public static final String IJK_CODEC = "ijk_codec";
    public static final String HOME_SHOW_SOURCE = "show_source";
    public static final String PLAY_TYPE = "play_type"; //0 系统 1 ijk 2 exo 10 MXPlayer
    public static final String DEBUG_OPEN = "debug_open";
    public static final String API_HISTORY = "api_history";
    public static final String EPG_URL = "epg_url";
    public static final String LIVE_URL = "live_url";
    public static final String LIVE_HISTORY = "live_history";
    public static final String DEFAULT_PARSE = "parse_default";
    public static final String PIC_IN_PIC = "pic_in_pic";
    public static final String PARSE_WEBVIEW = "parse_webview"; // true 系统 false xwalk
    public static final String PLAY_SCALE = "play_scale"; //0 texture 2
    public static final String PLAY_RENDER = "play_render"; //0 texture 2
    public static final String PLAY_TIME_STEP = "play_time_step"; //0 texture 2
    public static final String HOME_REC = "home_rec"; // 0 豆瓣热播 1 数据源推荐 2 历史
    public static final String SEARCH_VIEW = "search_view"; // 0 缩略图  1列表
    public static final String LIVE_CHANNEL = "last_live_channel_name";
    public static final String LIVE_CHANNEL_REVERSE = "live_channel_reverse";
    public static final String LIVE_CROSS_GROUP = "live_cross_group";
    public static final String LIVE_CONNECT_TIMEOUT = "live_connect_timeout";
    public static final String LIVE_SHOW_NET_SPEED = "live_show_net_speed";
    public static final String LIVE_SHOW_TIME = "live_show_time";
    public static boolean hotVodDelete;
    public static final String SUBTITLE_TEXT_SIZE = "subtitle_text_size";
    public static final String SUBTITLE_TIME_DELAY = "subtitle_time_delay";
}