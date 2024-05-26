package com.github.tvbox.osc.api;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.beanry.InitBean;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.beanry.ReJieXiBean;
import com.github.tvbox.osc.beanry.SiteBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.AdBlocker;
import com.github.tvbox.osc.util.BaseR;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.VideoParseRuler;
import com.github.tvbox.osc.util.MMkvUtils;
import com.github.tvbox.osc.util.ToolUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class ApiConfig {
    private static final String TAG = "ApiConfig";
    private static ApiConfig instance;
    private LinkedHashMap<String, SourceBean> sourceBeanList;
    private SourceBean mHomeSource;
    private ParseBean mDefaultParse;
    private List<LiveChannelGroup> liveChannelGroupList;
    private List<ParseBean> parseBeanList;
    private List<String> vipParseFlags;
    private List<IJKCode> ijkCodes;
    private String spider = null;
    private SourceBean emptyHome = new SourceBean();
    private JarLoader jarLoader = new JarLoader();

    private ApiConfig() {
        sourceBeanList = new LinkedHashMap<>();
        liveChannelGroupList = new ArrayList<>();
        parseBeanList = new ArrayList<>();
    }

    public static ApiConfig get() {
        if (instance == null) {
            synchronized (ApiConfig.class) {
                if (instance == null) {
                    instance = new ApiConfig();
                }
            }
        }
        return instance;
    }

    public void loadConfig(boolean useCache, LoadConfigCallback callback, Activity activity) {
        Log.d("TAG", "loadConfig: "+Hawk.get(HawkConfig.JSON_URL, "http://w1.xyui.top:7001/api.json"));
        String apiUrl = Hawk.get(HawkConfig.API_URL, Hawk.get(HawkConfig.JSON_URL, "http://w1.xyui.top:7001/api.json"));
        if (apiUrl.isEmpty()) {
            callback.error("-1");
            return;
        }
        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/" + MD5.encode(apiUrl));
        if (useCache && cache.exists()) {
            try {
                parseJson(apiUrl, cache);
                callback.success();
                return;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        String apiFix = apiUrl;
        if (apiUrl.startsWith("clan://")) {
            apiFix = clanToAddress(apiUrl);
        }
        OkGo.<String>get(apiFix)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            String json = response.body();
                            parseJson(apiUrl, response.body());
                            try {
                                File cacheDir = cache.getParentFile();
                                assert cacheDir != null;
                                if (!cacheDir.exists()) cacheDir.mkdirs();
                                if (cache.exists()) cache.delete();
                                FileOutputStream fos = new FileOutputStream(cache);
                                fos.write(json.getBytes("UTF-8"));
                                fos.flush();
                                fos.close();
                            } catch (Throwable th) {
                                th.printStackTrace();
                            }
                            callback.success();
                        } catch (Throwable th) {
                            th.printStackTrace();
                            callback.error("解析配置失败");
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        if (cache.exists()) {
                            try {
                                parseJson(apiUrl, cache);
                                callback.success();
                                return;
                            } catch (Throwable th) {
                                th.printStackTrace();
                            }
                        }
                        callback.error("拉取配置失败\n" + (response.getException() != null ? response.getException().getMessage() : ""));
                    }

                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        String result = "";
                        if (response.body() == null) {
                            result = "";
                        } else {
                            result = response.body().string();
                        }
                        if (apiUrl.startsWith("clan")) {
                            result = clanContentFix(clanToAddress(apiUrl), result);
                        }
                        result = fixContentPath(apiUrl, result);
                        return result;
                    }
                });
    }


    public void loadJar(boolean useCache, String spider, LoadConfigCallback callback) {
        String[] urls = spider.split(";md5;");
        String jarUrl = urls[0];
        String md5 = urls.length > 1 ? urls[1].trim() : "";
        File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/csp.jar");

        if (!md5.isEmpty() || useCache) {
            if (cache.exists() && (useCache || MD5.getFileMd5(cache).equalsIgnoreCase(md5))) {
                if (jarLoader.load(cache.getAbsolutePath())) {
                    callback.success();
                } else {
                    callback.error("");
                }
                return;
            }
        }

        OkGo.<File>get(jarUrl).execute(new AbsCallback<File>() {

            @Override
            public File convertResponse(okhttp3.Response response) throws Throwable {
                File cacheDir = cache.getParentFile();
                if (!cacheDir.exists())
                    cacheDir.mkdirs();
                if (cache.exists())
                    cache.delete();
                FileOutputStream fos = new FileOutputStream(cache);
                fos.write(response.body().bytes());
                fos.flush();
                fos.close();
                return cache;
            }

            @Override
            public void onSuccess(Response<File> response) {
                if (response.body().exists()) {
                    if (jarLoader.load(response.body().getAbsolutePath())) {
                        callback.success();
                    } else {
                        callback.error("");
                    }
                } else {
                    callback.error("");
                }
            }

            @Override
            public void onError(Response<File> response) {
                super.onError(response);
                callback.error("");
            }
        });
    }

    private void parseJson(String apiUrl, File f) throws Throwable {
        System.out.println("从本地缓存加载" + f.getAbsolutePath());
        BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s = "";
        while ((s = bReader.readLine()) != null) {
            sb.append(s).append("\n");
        }
        bReader.close();
        parseJson(apiUrl, sb.toString());
    }

    private void parseJson(String apiUrl, String jsonStr) {

        JsonObject infoJson = new Gson().fromJson(jsonStr, JsonObject.class);
        spider = DefaultConfig.safeJsonString(infoJson, "spider", "");

        // 插入自定义站点
        SourceBean firstSite = null;
        InitBean initData = MMkvUtils.loadInitBean("");
        SiteBean siteData = MMkvUtils.loadSiteBean("");
        ReJieXiBean reJieXiBean = MMkvUtils.loadReJieXiBean("");

        if (siteData != null && siteData.msg.size() > 0) { //自定义站点
            for (int i = 0; i < siteData.msg.size(); i++) {
                SourceBean sbData = new SourceBean();
                String siteKey = siteData.msg.get(i).gname;
                sbData.setKey(siteKey);
                sbData.setName(siteKey);
                sbData.setType(siteData.msg.get(i).type);
                sbData.setApi(siteData.msg.get(i).gapiname);
                sbData.setSearchable(siteData.msg.get(i).searchable);
                sbData.setQuickSearch(siteData.msg.get(i).quicksearch);
                sbData.setFilterable(siteData.msg.get(i).filterable);
                sbData.setPlayerUrl(siteData.msg.get(i).parse);
                sbData.setExt(siteData.msg.get(i).extend);
                sbData.setCategories(DefaultConfig.safeJsonStringList(null, "categories"));
                if (initData == null || !ToolUtils.getIsEmpty(initData.msg.uiRemoversc) || !initData.msg.uiRemoversc.contains(siteKey)) {
                    sourceBeanList.put(siteKey, sbData);
                    if (firstSite == null) firstSite = sbData;
                }
            }
        }

        for (JsonElement opt : infoJson.get("sites").getAsJsonArray()) { // 远端站点源
            JsonObject obj = (JsonObject) opt;
            SourceBean sb = new SourceBean();
            String siteKey = obj.get("key").getAsString().trim();
            sb.setKey(siteKey);
            sb.setName(obj.get("name").getAsString().trim());
            sb.setType(obj.get("type").getAsInt());
            sb.setApi(obj.get("api").getAsString().trim());
            sb.setSearchable(DefaultConfig.safeJsonInt(obj, "searchable", 1));
            sb.setQuickSearch(DefaultConfig.safeJsonInt(obj, "quickSearch", 1));
            sb.setFilterable(DefaultConfig.safeJsonInt(obj, "filterable", 1));
            sb.setPlayerUrl(DefaultConfig.safeJsonString(obj, "playUrl", ""));
            sb.setExt(DefaultConfig.safeJsonString(obj, "ext", ""));
            sb.setCategories(DefaultConfig.safeJsonStringList(obj, "categories"));
            if (initData == null || !ToolUtils.getIsEmpty(initData.msg.uiRemoversc) || !initData.msg.uiRemoversc.contains(obj.get("name").getAsString().trim())) {
                sourceBeanList.put(siteKey, sb);
                if (firstSite == null) firstSite = sb;
            }
        }

        if (sourceBeanList != null && sourceBeanList.size() > 0) {
            String home = Hawk.get(HawkConfig.HOME_API, "");
            SourceBean sh = getSource(home);
            if (sh == null) {
                assert firstSite != null;
                setSourceBean(firstSite);
            } else {
                setSourceBean(sh);
            }
        }

        // 需要使用vip解析的flag
        vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags");

        // 解析地址
        for (JsonElement opt : infoJson.get("parses").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            ParseBean pb = new ParseBean();
            pb.setName(obj.get("name").getAsString().trim());
            pb.setUrl(obj.get("url").getAsString().trim());
            String ext = obj.has("ext") ? obj.get("ext").getAsJsonObject().toString() : "";
            Log.d(TAG, "parseJson1: "+ext);
            pb.setExt(ext);
            pb.setType(DefaultConfig.safeJsonInt(obj, "type", 0));
            if (initData == null || !ToolUtils.getIsEmpty(initData.msg.uiRemoveParses) || !initData.msg.uiRemoveParses.contains(obj.get("name").getAsString().trim())) {
                parseBeanList.add(pb);
            }
        }

        // 插入自定义解析
        if (reJieXiBean != null && reJieXiBean.msg.size() > 0) {
            for (int i = 0; i < reJieXiBean.msg.size(); i++) {
                if (!reJieXiBean.msg.get(i).url.contains(",")){ //单个解析接口
                    ParseBean pbb = new ParseBean();
                    pbb.setName(reJieXiBean.msg.get(i).name);
                    pbb.setUrl(reJieXiBean.msg.get(i).url);
                    String ext = reJieXiBean.msg.get(i).ext;
                    Log.d(TAG, "parseJson2: "+ext);
                    pbb.setExt(ext);
                    pbb.setType(reJieXiBean.msg.get(i).type);
                    parseBeanList.add(pbb);
                }else{ //一个接口里面存在多个解析接口
                    String[] jieXiData = reJieXiBean.msg.get(i).url.split(",");
                    for (int p = 0; p < jieXiData.length; p++) {
                        ParseBean pbs = new ParseBean();
                        pbs.setName(reJieXiBean.msg.get(i).name + p);
                        pbs.setUrl(jieXiData[i]);
                        String ext = reJieXiBean.msg.get(i).ext;
                        Log.d(TAG, "parseJson3: "+ext);
                        pbs.setExt(ext);
                        pbs.setType(reJieXiBean.msg.get(i).type);
                        parseBeanList.add(pbs);
                    }
                }
            }
        }

        // 获取默认解析
        if (parseBeanList != null && parseBeanList.size() > 0) {
            String defaultParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "");
            if (!TextUtils.isEmpty(defaultParse))
                for (ParseBean pb : parseBeanList) {
                    if (pb.getName().equals(defaultParse)) {
                        setDefaultParse(pb);
                    }
                }
            if (mDefaultParse == null) {
                setDefaultParse(parseBeanList.get(0));
            }
        }

        // 直播源
        liveChannelGroupList.clear();
        String liveURL = Hawk.get(HawkConfig.LIVE_URL, "");

        try {
            JsonObject livesOBJ = infoJson.get("lives").getAsJsonArray().get(0).getAsJsonObject();
            String lives = livesOBJ.toString();
            int index = lives.indexOf("proxy://");
            if (index != -1) {
                int endIndex = lives.lastIndexOf("\"");
                String url = lives.substring(index, endIndex);
                url = DefaultConfig.checkReplaceProxy(url);

                //clan
                String extUrl = Uri.parse(url).getQueryParameter("ext");
                if (extUrl != null && !extUrl.isEmpty()) {
                    String extUrlFix;
                    if (extUrl.startsWith("http") || extUrl.startsWith("clan://")) {
                        extUrlFix = extUrl;
                    } else {
                        extUrlFix = new String(Base64.decode(extUrl, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
                    }
                    if (extUrlFix.startsWith("clan://")) {
                        extUrlFix = clanContentFix(clanToAddress(apiUrl), extUrlFix);
                    }
                    System.out.println("Live URL :" + extUrlFix);
                    putLiveHistory(extUrlFix);
                    if (StringUtils.isBlank(liveURL)) {
                        Hawk.put(HawkConfig.LIVE_URL, extUrlFix);
                    } else {
                        extUrlFix = liveURL;
                    }
                    extUrlFix = Base64.encodeToString(extUrlFix.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
                    url = url.replace(extUrl, extUrlFix);
                }
                if (livesOBJ.has("epg")) {
                    String epg = livesOBJ.get("epg").getAsString();
                    String epgURL = Hawk.get(HawkConfig.EPG_URL, "");
                    if (StringUtils.isBlank(epgURL)) {
                        System.out.println("EPG URL :" + epg);
                        Hawk.put(HawkConfig.EPG_URL, epg);
                    }
                }
                LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
                liveChannelGroup.setGroupName(url);
                liveChannelGroupList.add(liveChannelGroup);

            } else {
                if (!lives.contains("type")) {
                    loadLives(infoJson.get("lives").getAsJsonArray());
                } else {
                    JsonObject fengMiLives = infoJson.get("lives").getAsJsonArray().get(0).getAsJsonObject();
                    String type = fengMiLives.get("type").getAsString();
                    if (type.equals("0")) {
                        String url = fengMiLives.get("url").getAsString();
                        if (fengMiLives.has("epg")) {
                            String epg = fengMiLives.get("epg").getAsString();
                            String epgURL = Hawk.get(HawkConfig.EPG_URL, "");
                            if (StringUtils.isBlank(epgURL)) {
                                System.out.println("EPG URL :" + epg);
                                Hawk.put(HawkConfig.EPG_URL, epg);
                            }
                        }
                        if (url.startsWith("http")) {
                            System.out.println("Live URL :" + url);
                            putLiveHistory(url);
                            if (StringUtils.isBlank(liveURL)) {
                                Hawk.put(HawkConfig.LIVE_URL, url);
                            } else {
                                url = liveURL;
                            }
                            url = Base64.encodeToString(url.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
                        }
                        url = "http://127.0.0.1:9978/proxy?do=live&type=txt&ext=" + url;
                        LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
                        liveChannelGroup.setGroupName(url);
                        liveChannelGroupList.add(liveChannelGroup);
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        if (infoJson.has("rules")) {
            VideoParseRuler.clearRule();
            for (JsonElement oneHostRule : infoJson.getAsJsonArray("rules")) {
                JsonObject obj = (JsonObject) oneHostRule;
                String host = obj.get("host").getAsString();
                if (obj.has("rule")) {
                    JsonArray ruleJsonArr = obj.getAsJsonArray("rule");
                    ArrayList<String> rule = new ArrayList<>();
                    for (JsonElement one : ruleJsonArr) {
                        String oneRule = one.getAsString();
                        rule.add(oneRule);
                    }
                    if (rule.size() > 0) {
                        VideoParseRuler.addHostRule(host, rule);
                    }
                }
                if (obj.has("filter")) {
                    JsonArray filterJsonArr = obj.getAsJsonArray("filter");
                    ArrayList<String> filter = new ArrayList<>();
                    for (JsonElement one : filterJsonArr) {
                        String oneFilter = one.getAsString();
                        filter.add(oneFilter);
                    }
                    if (filter.size() > 0) {
                        VideoParseRuler.addHostFilter(host, filter);
                    }
                }
            }
        }

        String defaultIJKADS = "{\"ijk\":[{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"overlay-format\",\"category\":4,\"value\":\"842225234\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"1\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"0\"},{\"name\":\"dns_cache_timeout\",\"category\":1,\"value\":\"600000000\"}],\"group\":\"软解码\"},{\"options\":[{\"name\":\"opensles\",\"category\":4,\"value\":\"0\"},{\"name\":\"overlay-format\",\"category\":4,\"value\":\"842225234\"},{\"name\":\"framedrop\",\"category\":4,\"value\":\"1\"},{\"name\":\"soundtouch\",\"category\":4,\"value\":\"1\"},{\"name\":\"start-on-prepared\",\"category\":4,\"value\":\"1\"},{\"name\":\"http-detect-rangeupport\",\"category\":1,\"value\":\"0\"},{\"name\":\"fflags\",\"category\":1,\"value\":\"fastseek\"},{\"name\":\"skip_loop_filter\",\"category\":2,\"value\":\"48\"},{\"name\":\"reconnect\",\"category\":4,\"value\":\"1\"},{\"name\":\"enable-accurate-seek\",\"category\":4,\"value\":\"0\"},{\"name\":\"mediacodec\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-auto-rotate\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-handle-resolution-change\",\"category\":4,\"value\":\"1\"},{\"name\":\"mediacodec-hevc\",\"category\":4,\"value\":\"1\"},{\"name\":\"dns_cache_timeout\",\"category\":1,\"value\":\"600000000\"}],\"group\":\"硬解码\"}],\"ads\":[\"mimg.0c1q0l.cn\",\"www.googletagmanager.com\",\"www.google-analytics.com\",\"mc.usihnbcq.cn\",\"mg.g1mm3d.cn\",\"mscs.svaeuzh.cn\",\"cnzz.hhttm.top\",\"tp.vinuxhome.com\",\"cnzz.mmstat.com\",\"www.baihuillq.com\",\"s23.cnzz.com\",\"z3.cnzz.com\",\"c.cnzz.com\",\"stj.v1vo.top\",\"z12.cnzz.com\",\"img.mosflower.cn\",\"tips.gamevvip.com\",\"ehwe.yhdtns.com\",\"xdn.cqqc3.com\",\"www.jixunkyy.cn\",\"sp.chemacid.cn\",\"hm.baidu.com\",\"s9.cnzz.com\",\"z6.cnzz.com\",\"um.cavuc.com\",\"mav.mavuz.com\",\"wofwk.aoidf3.com\",\"z5.cnzz.com\",\"xc.hubeijieshikj.cn\",\"tj.tianwenhu.com\",\"xg.gars57.cn\",\"k.jinxiuzhilv.com\",\"cdn.bootcss.com\",\"ppl.xunzhuo123.com\",\"xomk.jiangjunmh.top\",\"img.xunzhuo123.com\",\"z1.cnzz.com\",\"s13.cnzz.com\",\"xg.huataisangao.cn\",\"z7.cnzz.com\",\"xg.huataisangao.cn\",\"z2.cnzz.com\",\"s96.cnzz.com\",\"q11.cnzz.com\",\"thy.dacedsfa.cn\",\"xg.whsbpw.cn\",\"s19.cnzz.com\",\"z8.cnzz.com\",\"s4.cnzz.com\",\"f5w.as12df.top\",\"ae01.alicdn.com\",\"www.92424.cn\",\"k.wudejia.com\",\"vivovip.mmszxc.top\",\"qiu.xixiqiu.com\",\"cdnjs.hnfenxun.com\",\"cms.qdwght.com\"]}";
        JsonObject defaultJson = new Gson().fromJson(defaultIJKADS, JsonObject.class);

        // 广告地址
        if (AdBlocker.isEmpty()) {
//            AdBlocker.clear();
            //追加的广告拦截
            if (infoJson.has("ads")) {
                for (JsonElement host : infoJson.getAsJsonArray("ads")) {
                    AdBlocker.addAdHost(host.getAsString());
                }
            } else {
                //默认广告拦截
                for (JsonElement host : defaultJson.getAsJsonArray("ads")) {
                    AdBlocker.addAdHost(host.getAsString());
                }
            }
        }

        // IJK解码配置
        boolean foundOldSelect = false;
        String ijkCodec = Hawk.get(HawkConfig.IJK_CODEC, "");
        ijkCodes = new ArrayList<>();
        for (JsonElement opt : infoJson.get("ijk").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            String name = obj.get("group").getAsString();
            LinkedHashMap<String, String> baseOpt = new LinkedHashMap<>();
            for (JsonElement cfg : obj.get("options").getAsJsonArray()) {
                JsonObject cObj = (JsonObject) cfg;
                String key = cObj.get("category").getAsString() + "|" + cObj.get("name").getAsString();
                String val = cObj.get("value").getAsString();
                baseOpt.put(key, val);
            }
            IJKCode codec = new IJKCode();
            codec.setName(name);
            codec.setOption(baseOpt);
            if (name.equals(ijkCodec) || TextUtils.isEmpty(ijkCodec)) {
                codec.selected(true);
                ijkCodec = name;
                foundOldSelect = true;
            } else {
                codec.selected(false);
            }
            ijkCodes.add(codec);
        }
        if (!foundOldSelect && ijkCodes.size() > 0) {
            ijkCodes.get(0).selected(true);
        }
    }

    private void putLiveHistory(String url) {
        if (!url.isEmpty()) {
            ArrayList<String> liveHistory = Hawk.get(HawkConfig.LIVE_HISTORY, new ArrayList<String>());
            if (!liveHistory.contains(url))
                liveHistory.add(0, url);
            if (liveHistory.size() > 20)
                liveHistory.remove(20);
            Hawk.put(HawkConfig.LIVE_HISTORY, liveHistory);
        }
    }


    public void loadLives(JsonArray livesArray) {
        liveChannelGroupList.clear();
        int groupIndex = 0;
        int channelIndex = 0;
        int channelNum = 0;
        for (JsonElement groupElement : livesArray) {
            LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
            liveChannelGroup.setLiveChannels(new ArrayList<LiveChannelItem>());
            liveChannelGroup.setGroupIndex(groupIndex++);
            String groupName = ((JsonObject) groupElement).get("group").getAsString().trim();
            String[] splitGroupName = groupName.split("_", 2);
            liveChannelGroup.setGroupName(splitGroupName[0]);
            if (splitGroupName.length > 1)
                liveChannelGroup.setGroupPassword(splitGroupName[1]);
            else
                liveChannelGroup.setGroupPassword("");
            channelIndex = 0;
            for (JsonElement channelElement : ((JsonObject) groupElement).get("channels").getAsJsonArray()) {
                JsonObject obj = (JsonObject) channelElement;
                LiveChannelItem liveChannelItem = new LiveChannelItem();
                liveChannelItem.setChannelName(obj.get("name").getAsString().trim());
                liveChannelItem.setChannelIndex(channelIndex++);
                liveChannelItem.setChannelNum(++channelNum);
                ArrayList<String> urls = DefaultConfig.safeJsonStringList(obj, "urls");
                ArrayList<String> sourceNames = new ArrayList<>();
                ArrayList<String> sourceUrls = new ArrayList<>();
                int sourceIndex = 1;
                for (String url : urls) {
                    String[] splitText = url.split("\\$", 2);
                    sourceUrls.add(splitText[0]);
                    if (splitText.length > 1)
                        sourceNames.add(splitText[1]);
                    else
                        sourceNames.add("源" + Integer.toString(sourceIndex));
                    sourceIndex++;
                }
                liveChannelItem.setChannelSourceNames(sourceNames);
                liveChannelItem.setChannelUrls(sourceUrls);
                liveChannelGroup.getLiveChannels().add(liveChannelItem);
            }
            liveChannelGroupList.add(liveChannelGroup);
        }
    }

    public String getSpider() {
        return spider;
    }

    public Spider getCSP(SourceBean sourceBean) {
        return jarLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt());
    }

    public Object[] proxyLocal(Map param) {
        return jarLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public interface LoadConfigCallback {
        void success();

        void retry();

        void error(String msg);
    }

    public interface FastParseCallback {
        void success(boolean parse, String url, Map<String, String> header);

        void fail(int code, String msg);
    }

    public SourceBean getSource(String key) {
        if (!sourceBeanList.containsKey(key))
            return null;
        return sourceBeanList.get(key);
    }

    public void setSourceBean(SourceBean sourceBean) {
        this.mHomeSource = sourceBean;
        Hawk.put(HawkConfig.HOME_API, sourceBean.getKey());
    }

    public void setDefaultParse(ParseBean parseBean) {
        if (this.mDefaultParse != null)
            this.mDefaultParse.setDefault(false);
        this.mDefaultParse = parseBean;
        Hawk.put(HawkConfig.DEFAULT_PARSE, parseBean.getName());
        parseBean.setDefault(true);
    }

    public ParseBean getDefaultParse() {
        return mDefaultParse;
    }

    public List<SourceBean> getSourceBeanList() {
        return new ArrayList<>(sourceBeanList.values());
    }

    public List<ParseBean> getParseBeanList() {
        return parseBeanList;
    }

    public List<String> getVipParseFlags() {
        return vipParseFlags;
    }

    public SourceBean getHomeSourceBean() {
        return mHomeSource == null ? emptyHome : mHomeSource;
    }

    public List<LiveChannelGroup> getChannelGroupList() {
        return liveChannelGroupList;
    }

    public List<IJKCode> getIjkCodes() {
        return ijkCodes;
    }

    public IJKCode getCurrentIJKCode() {
        String codeName = Hawk.get(HawkConfig.IJK_CODEC, "");
        return getIJKCodec(codeName);
    }

    public IJKCode getIJKCodec(String name) {
        for (IJKCode code : ijkCodes) {
            if (code.getName().equals(name))
                return code;
        }
        return ijkCodes.get(0);
    }

    String clanToAddress(String lanLink) {
        if (lanLink.startsWith("clan://localhost/")) {
            return lanLink.replace("clan://localhost/", ControlManager.get().getAddress(true) + "file/");
        } else {
            String link = lanLink.substring(7);
            int end = link.indexOf('/');
            return "http://" + link.substring(0, end) + "/file/" + link.substring(end + 1);
        }
    }

    String clanContentFix(String lanLink, String content) {
        String fix = lanLink.substring(0, lanLink.indexOf("/file/") + 6);
        return content.replace("clan://", fix);
    }

    String fixContentPath(String url, String content) {
        if (content.contains("\"./")) {
            if (!url.startsWith("http") && !url.startsWith("clan://")) {
                url = "http://" + url;
            }
            if (url.startsWith("clan://")) url = clanToAddress(url);
            content = content.replace("./", url.substring(0, url.lastIndexOf("/") + 1));
        }
        return content;
    }

    String miTV(String url) {
        if (url.startsWith("p") || url.startsWith("mitv")) {

        }
        return url;
    }
}