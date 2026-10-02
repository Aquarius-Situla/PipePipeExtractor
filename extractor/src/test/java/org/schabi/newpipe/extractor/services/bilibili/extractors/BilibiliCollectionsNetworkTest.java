package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.schabi.newpipe.extractor.stream.StreamCollectionInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "bilibili.networkTests", matches = "true")
class BilibiliCollectionsNetworkTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Test
    void extractsLiveCollectionAndMultipageVideoMetadata() throws Exception {
        final JsonObject collectionVideo = fetchVideo("BV1Tb421b7mi");
        final List<StreamCollectionInfo> collections = BilibiliCollectionParser.parse(collectionVideo);
        assertEquals(1, collections.size());
        assertEquals("2974525", collections.get(0).getId());
        assertEquals("楚汉传奇", collections.get(0).getTitle());
        assertEquals("正片", collections.get(0).getSections().get(0).getTitle());
        final StreamCollectionInfo.Episode firstEpisode = collections.get(0).getSections()
                .get(0).getEpisodes().get(0);
        assertEquals("BV1Tb421b7mi", firstEpisode.getVideoId());
        assertEquals("https://www.bilibili.com/video/BV1Tb421b7mi", firstEpisode.getUrl());
        assertEquals(1541093346, firstEpisode.getContentId());
        assertTrue(collections.get(0).getSections().get(0).getEpisodes().size() > 1);

        final JsonObject uncollectedVideo = fetchVideo("BV1ex411J7GE");
        assertTrue(BilibiliCollectionParser.parse(uncollectedVideo).isEmpty());

        final JsonArray pages = uncollectedVideo.getArray("pages");
        assertTrue(pages.size() >= 3);
        for (int pageNumber = 1; pageNumber <= 3; pageNumber++) {
            final JsonObject selectedPage = BilibiliStreamPageParser.pageForUrl(uncollectedVideo,
                    "https://www.bilibili.com/video/BV1ex411J7GE?p=" + pageNumber);
            final JsonObject expectedPage = pages.getObject(pageNumber - 1);
            assertEquals(pageNumber, selectedPage.getInt("page"));
            assertEquals(expectedPage.getLong("cid"), selectedPage.getLong("cid"));
        }
        assertEquals(35039663, BilibiliStreamPageParser.pageForUrl(uncollectedVideo,
                "https://www.bilibili.com/video/BV1ex411J7GE?p=2").getLong("cid"));
    }

    private static JsonObject fetchVideo(final String bvid) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "TypeType-PipePipeExtractor-test/1.0")
                .GET()
                .build();
        final HttpResponse<String> response = CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new AssertionError("BiliBili returned HTTP " + response.statusCode());
        }
        final JsonObject root = JsonParser.object().from(response.body());
        if (root.getInt("code") != 0) {
            throw new AssertionError("BiliBili API error: " + root.getString("message"));
        }
        return root.getObject("data");
    }
}
