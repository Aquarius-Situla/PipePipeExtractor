package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.stream.StreamCollectionInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilibiliCollectionParserTest {
    @Test
    void mapsSectionsAndEpisodesInSourceOrder() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {
                  "ugc_season": {
                    "id": 2974525,
                    "title": "楚汉传奇",
                    "sections": [{
                      "id": 3341804,
                      "title": "正片",
                      "episodes": [
                        {"aid": 1804383120, "cid": 1541093346, "title": "Episode one",
                         "arc": {"pic": "http://i0.hdslb.com/episode-one.jpg"}},
                        {"aid": 1004394994, "cid": 1542426326, "title": "Episode two"}
                      ]
                    }]
                  }
                }
                """);

        final List<StreamCollectionInfo> collections = BilibiliCollectionParser.parse(watch);
        assertEquals(1, collections.size());
        final StreamCollectionInfo collection = collections.get(0);
        assertEquals("2974525", collection.getId());
        assertEquals("楚汉传奇", collection.getTitle());
        assertEquals("3341804", collection.getSections().get(0).getId());
        assertEquals("正片", collection.getSections().get(0).getTitle());
        assertEquals("Episode one", collection.getSections().get(0).getEpisodes().get(0).getTitle());
        assertEquals("BV1Tb421b7mi",
                collection.getSections().get(0).getEpisodes().get(0).getVideoId());
        assertEquals("https://www.bilibili.com/video/BV1Tb421b7mi",
                collection.getSections().get(0).getEpisodes().get(0).getUrl());
        assertEquals(1541093346,
                collection.getSections().get(0).getEpisodes().get(0).getContentId());
        assertEquals("https://i0.hdslb.com/episode-one.jpg",
                collection.getSections().get(0).getEpisodes().get(0).getThumbnailUrl());
        assertEquals("Episode two", collection.getSections().get(0).getEpisodes().get(1).getTitle());
    }

    @Test
    void returnsNoCollectionsWhenSeasonIsAbsent() throws JsonParserException {
        assertTrue(BilibiliCollectionParser.parse(JsonParser.object().from("{\"pages\": []}")).isEmpty());
    }

    @Test
    void acceptsAnExplicitArchiveBvid() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {"ugc_season":{"id":1,"title":"Season","sections":[{"id":2,
                  "title":"Main","episodes":[{"aid":1,"cid":3,"title":"Episode",
                  "arc":{"bvid":"BV1explicit01"}}]}]}}
                """);

        assertEquals("BV1explicit01", BilibiliCollectionParser.parse(watch).get(0)
                .getSections().get(0).getEpisodes().get(0).getVideoId());
    }

    @Test
    void prefersEpisodeThumbnailAndDefaultsToEmptyWhenAbsent() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {"ugc_season":{"id":1,"title":"Season","sections":[{"id":2,
                  "title":"Main","episodes":[
                    {"aid":1,"cid":3,"title":"With cover","pic":"http://i0.hdslb.com/cover.jpg"},
                    {"aid":2,"cid":4,"title":"Without cover"}
                  ]}]}}
                """);

        final List<StreamCollectionInfo.Episode> episodes = BilibiliCollectionParser.parse(watch)
                .get(0).getSections().get(0).getEpisodes();
        assertEquals("https://i0.hdslb.com/cover.jpg", episodes.get(0).getThumbnailUrl());
        assertEquals("", episodes.get(1).getThumbnailUrl());
    }

    @Test
    void parsesMultiPageVideosIntoCollection() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {
                  "bvid": "BV1Eb411u7Fw",
                  "title": "高等数学",
                  "pic": "http://i0.hdslb.com/main.jpg",
                  "pages": [
                    {"page": 1, "cid": 101, "part": "1.1 映射", "first_frame": "http://i0.hdslb.com/p1.jpg"},
                    {"page": 2, "cid": 102, "part": "P2 包含与属于"},
                    {"page": 3, "cid": 103, "part": ""}
                  ]
                }
                """);

        final List<StreamCollectionInfo> collections = BilibiliCollectionParser.parse(watch);
        assertEquals(1, collections.size());
        final StreamCollectionInfo collection = collections.get(0);
        assertEquals("BV1Eb411u7Fw-parts", collection.getId());
        assertEquals("高等数学", collection.getTitle());
        assertEquals(1, collection.getSections().size());
        assertEquals("parts", collection.getSections().get(0).getId());
        assertEquals("分P (3)", collection.getSections().get(0).getTitle());

        final List<StreamCollectionInfo.Episode> episodes = collection.getSections().get(0).getEpisodes();
        assertEquals(3, episodes.size());

        // P1: default videoId, formatted title, custom thumbnail
        assertEquals("BV1Eb411u7Fw", episodes.get(0).getVideoId());
        assertEquals("P1 1.1 映射", episodes.get(0).getTitle());
        assertEquals("https://www.bilibili.com/video/BV1Eb411u7Fw", episodes.get(0).getUrl());
        assertEquals(101, episodes.get(0).getContentId());
        assertEquals("https://i0.hdslb.com/p1.jpg", episodes.get(0).getThumbnailUrl());

        // P2: videoId with ?p=2, preserves existing P2 prefix, falls back to main pic
        assertEquals("BV1Eb411u7Fw?p=2", episodes.get(1).getVideoId());
        assertEquals("P2 包含与属于", episodes.get(1).getTitle());
        assertEquals("https://www.bilibili.com/video/BV1Eb411u7Fw?p=2", episodes.get(1).getUrl());
        assertEquals(102, episodes.get(1).getContentId());
        assertEquals("https://i0.hdslb.com/main.jpg", episodes.get(1).getThumbnailUrl());

        // P3: videoId with ?p=3, part is empty so defaults to P3
        assertEquals("BV1Eb411u7Fw?p=3", episodes.get(2).getVideoId());
        assertEquals("P3", episodes.get(2).getTitle());
        assertEquals("https://www.bilibili.com/video/BV1Eb411u7Fw?p=3", episodes.get(2).getUrl());
    }

    @Test
    void ignoresSinglePageVideoWithoutSeason() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {
                  "bvid": "BV1single",
                  "title": "单P视频",
                  "pages": [
                    {"page": 1, "cid": 101, "part": "正片"}
                  ]
                }
                """);

        assertTrue(BilibiliCollectionParser.parse(watch).isEmpty());
    }

    @Test
    void parsesBothPagesAndSeasonWithPagesFirst() throws JsonParserException {
        final JsonObject watch = JsonParser.object().from("""
                {
                  "bvid": "BV1multiInSeason",
                  "title": "选修课",
                  "pages": [
                    {"page": 1, "cid": 101, "part": "第一讲"},
                    {"page": 2, "cid": 102, "part": "第二讲"}
                  ],
                  "ugc_season": {
                    "id": 999,
                    "title": "整个学期大合集",
                    "sections": [{
                      "id": 1,
                      "title": "第一单元",
                      "episodes": [{"aid": 1, "cid": 101, "title": "全集"}]
                    }]
                  }
                }
                """);

        final List<StreamCollectionInfo> collections = BilibiliCollectionParser.parse(watch);
        assertEquals(2, collections.size());
        assertEquals("BV1multiInSeason-parts", collections.get(0).getId());
        assertEquals("999", collections.get(1).getId());
    }
}
