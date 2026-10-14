package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliPlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;

public class BilibiliPlaylistExtractor extends PlaylistExtractor {
    public JsonObject data;
    public String type;
    private String currentMid = "";
    private String firstPageApiUrl = "";
    private JsonObject userData;

    public BilibiliPlaylistExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        BilibiliPlaylistLinkHandlerFactory.PlaylistTarget target;
        try {
            target = BilibiliPlaylistLinkHandlerFactory.parseTarget(getLinkHandler().getUrl());
        } catch (ParsingException e) {
            target = BilibiliPlaylistLinkHandlerFactory.parseTarget(getOriginalUrl());
        }
        type = target.type;
        currentMid = target.mid;

        if ("partition".equals(type)) {
            try {
                data = JsonParser.object().from(downloader.get(getLinkHandler().getUrl(), getHeaders(getOriginalUrl())).responseBody());
            } catch (JsonParserException e) {
                throw new ExtractionException("Failed to parse partition response", e);
            }
            return;
        }

        String midParam = (!currentMid.isEmpty()) ? "mid=" + currentMid + "&" : "";
        if ("seasons_archives".equals(type)) {
            firstPageApiUrl = "https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?"
                    + midParam + "season_id=" + target.id + "&sort_reverse=false&page_num=1&page_size=30";
        } else {
            firstPageApiUrl = "https://api.bilibili.com/x/series/archives?"
                    + midParam + "series_id=" + target.id + "&only_normal=true&sort=desc&pn=1&ps=30";
        }

        try {
            String responseBody = downloader.get(firstPageApiUrl, getHeaders(getOriginalUrl())).responseBody();
            JsonObject root = JsonParser.object().from(responseBody);
            data = root.getObject("data");
            if (data != null && data.has("meta")) {
                long m = data.getObject("meta").getLong("mid");
                if (m > 0) currentMid = String.valueOf(m);
            }
        } catch (JsonParserException e) {
            throw new ExtractionException("Failed to parse playlist response", e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        if ("partition".equals(type)) {
            if (getLinkHandler().getUrl().contains("name=")) {
                try {
                    return URLDecoder.decode(getLinkHandler().getUrl().split("name=")[1].split("&")[0], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return getLinkHandler().getUrl().split("name=")[1].split("&")[0];
                }
            }
            return "Partition";
        }
        if (data != null && data.has("meta")) {
            JsonObject meta = data.getObject("meta");
            if (meta.has("name") && !meta.getString("name").isEmpty()) {
                return meta.getString("name");
            }
            if (meta.has("title") && !meta.getString("title").isEmpty()) {
                return meta.getString("title");
            }
        }
        if (getLinkHandler().getUrl().contains("name=")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("name=")[1].split("&")[0], "UTF-8");
            } catch (Exception ignored) {}
        }
        return "Bilibili Playlist";
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        if ("partition".equals(type)) {
            InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> collector = new StreamInfoItemsCollector(getServiceId());
            JsonArray relatedArray = data.getArray("data");
            for (int i = 0; i < relatedArray.size(); i++) {
                collector.commit(
                        new BilibiliRelatedInfoItemExtractor(
                                relatedArray.getObject(i), getLinkHandler().getUrl().split("bvid=")[1].split("&")[0],
                                URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8"),
                                String.valueOf(i + 1), getUploaderName(), null));
            }
            if (ServiceList.BiliBili.getFilterTypes().contains("playlists")) {
                collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
            }
            return new InfoItemsPage<>(collector, null);
        }
        return getPage(new Page(firstPageApiUrl, getDefaultCookies()));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        if (!"partition".equals(type) && !page.getUrl().equals(firstPageApiUrl)) {
            try {
                data = JsonParser.object().from(getDownloader().get(page.getUrl(), getHeaders(getOriginalUrl())).responseBody()).getObject("data");
            } catch (JsonParserException e) {
                throw new ExtractionException("Failed to parse playlist page response", e);
            }
        }
        if (data == null) {
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        JsonArray results = data.getArray("archives");
        if (results == null || results.isEmpty()) {
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        String uploaderName = getUploaderName();
        String uploaderAvatar = getUploaderAvatarUrl();
        for (int i = 0; i < results.size(); i++) {
            collector.commit(new BilibiliChannelInfoItemWebAPIExtractor(results.getObject(i), uploaderName, uploaderAvatar));
        }
        if (ServiceList.BiliBili.getFilterTypes().contains("playlists")) {
            collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
        }

        Page nextPage = null;
        long total = getStreamCount();
        String pageParam = "seasons_archives".equals(type) ? "page_num" : "pn";
        int currentPageNum = 1;
        if (page.getUrl().contains(pageParam + "=")) {
            try {
                currentPageNum = Integer.parseInt(page.getUrl().split(pageParam + "=")[1].split("&")[0]);
            } catch (Exception ignored) {}
        }
        int pageSize = 30;
        if ((long) currentPageNum * pageSize < total) {
            nextPage = new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), pageParam, 1), getDefaultCookies());
        }
        return new InfoItemsPage<>(collector, nextPage);
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if ("partition".equals(type)) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderUrl=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        String uid = null;
        if (data != null && data.has("meta")) {
            long midLong = data.getObject("meta").getLong("mid");
            if (midLong > 0) uid = String.valueOf(midLong);
            else uid = data.getObject("meta").getString("mid");
        }
        if (uid == null || uid.isEmpty()) {
            uid = currentMid;
        }
        if (uid == null || uid.isEmpty()) {
            uid = utils.getMidFromRecordApiUrl(getLinkHandler().getUrl());
        }
        return BilibiliChannelLinkHandlerFactory.baseUrl + (uid != null ? uid : "");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if ("partition".equals(type)) {
            if (getLinkHandler().getUrl().contains("uploaderName=")) {
                try {
                    return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderName=")[1].split("&")[0], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return getLinkHandler().getUrl().split("uploaderName=")[1].split("&")[0];
                }
            }
            return "[Unknown]";
        }
        return getUserInfoLazily().getObject("data").getObject("card").getString("name", "[Unknown]");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if ("partition".equals(type)) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderAvatar=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        String face = getUserInfoLazily().getObject("data").getObject("card").getString("face");
        return face != null ? face.replace("http:", "https:") : "";
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        if ("partition".equals(type)) {
            return data.getArray("data").size();
        }
        if (data != null && data.has("page")) {
            long total = data.getObject("page").getLong("total");
            if (total > 0) return total;
        }
        if (data != null && data.has("meta")) {
            return data.getObject("meta").getLong("total");
        }
        return 0;
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        if ("partition".equals(type)) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
        if (data != null && data.has("meta")) {
            String cover = data.getObject("meta").getString("cover");
            if (cover != null && !cover.isEmpty()) {
                return cover.replace("http:", "https:");
            }
        }
        if (data != null && data.has("archives")) {
            JsonArray archives = data.getArray("archives");
            if (archives != null && !archives.isEmpty()) {
                String pic = archives.getObject(0).getString("pic");
                if (pic != null && !pic.isEmpty()) {
                    return pic.replace("http:", "https:");
                }
            }
        }
        return "";
    }

    // Get user metadata JSON lazily, without possible NPE.
    private JsonObject getUserInfoLazily() {
        if (userData != null) return userData;
        try {
            userData = JsonParser.object().from(getDownloader().get(getUserInfoRequestUrl(), getHeaders(getOriginalUrl())).responseBody());
        } catch (Exception e) {
            userData = new JsonObject();
        }
        return userData;
    }

    private String getUserInfoRequestUrl() {
        String uid = null;
        if (data != null && data.has("meta")) {
            long midLong = data.getObject("meta").getLong("mid");
            if (midLong > 0) uid = String.valueOf(midLong);
            else uid = data.getObject("meta").getString("mid");
        }
        if (uid == null || uid.isEmpty()) {
            uid = currentMid;
        }
        if (uid == null || uid.isEmpty()) {
            uid = utils.getMidFromRecordApiUrl(getLinkHandler().getUrl());
        }
        return QUERY_USER_INFO_URL + (uid != null ? uid : "");
    }
}
