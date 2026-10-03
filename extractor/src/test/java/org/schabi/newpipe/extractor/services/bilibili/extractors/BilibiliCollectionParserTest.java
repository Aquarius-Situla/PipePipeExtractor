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
}
