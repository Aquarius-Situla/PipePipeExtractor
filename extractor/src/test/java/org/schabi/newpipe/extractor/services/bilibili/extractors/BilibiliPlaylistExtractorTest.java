package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliPlaylistLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliPlaylistExtractorTest {

    @BeforeAll
    static void setUp() {
        NewPipe.init(new Downloader() {
            @Override
            public Response execute(@Nonnull Request request) {
                return new Response(200, "OK", Collections.emptyMap(), "{\"code\":0,\"data\":{}}", new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(@Nonnull Request request, AsyncCallback callback) {
                return null;
            }
        });
    }

    @Test
    void extractsPlaylistInfoItemCanonicalUrl() throws ParsingException, JsonParserException {
        JsonObject metaJson = JsonParser.object().from("""
                {
                  "meta": {
                    "season_id": 649231,
                    "series_id": 12345,
                    "mid": 11545648,
                    "name": "高等数学基础合集",
                    "cover": "http://i0.hdslb.com/bfs/archive/cover.jpg",
                    "total": 42
                  }
                }
                """);

        BilibiliPlaylistInfoItemExtractor seasonExtractor =
                new BilibiliPlaylistInfoItemExtractor(metaJson, "seasons_archives");
        assertEquals("高等数学基础合集", seasonExtractor.getName());
        assertEquals("https://www.bilibili.com/medialist/play/11545648?business=space_collection&business_id=649231", seasonExtractor.getUrl());
        assertEquals("https://i0.hdslb.com/bfs/archive/cover.jpg", seasonExtractor.getThumbnailUrl());
        assertEquals(42, seasonExtractor.getStreamCount());

        BilibiliPlaylistInfoItemExtractor seriesExtractor =
                new BilibiliPlaylistInfoItemExtractor(metaJson, "archives");
        assertEquals("https://www.bilibili.com/medialist/play/11545648?business=space_series&business_id=12345", seriesExtractor.getUrl());
    }

    @Test
    void extractsNameAndThumbnailFromMockData() throws ParsingException, JsonParserException {
        JsonObject mockData = JsonParser.object().from("""
                {
                  "meta": {
                    "season_id": 649231,
                    "mid": 11545648,
                    "name": "合集测试标题",
                    "cover": "http://i0.hdslb.com/bfs/archive/season_cover.jpg",
                    "total": 2
                  },
                  "archives": [
                    {
                      "aid": 111,
                      "bvid": "BV1test1",
                      "title": "Video 1",
                      "pic": "http://i0.hdslb.com/bfs/archive/v1.jpg",
                      "duration": 120
                    }
                  ],
                  "page": {
                    "page_num": 1,
                    "page_size": 30,
                    "total": 2
                  }
                }
                """);

        BilibiliPlaylistLinkHandlerFactory factory = new BilibiliPlaylistLinkHandlerFactory();
        ListLinkHandler handler = factory.fromUrl("https://www.bilibili.com/medialist/play/11545648?business=space_collection&business_id=649231");
        BilibiliPlaylistExtractor extractor = new BilibiliPlaylistExtractor(ServiceList.BiliBili, handler);
        extractor.data = mockData;
        extractor.type = "seasons_archives";

        assertEquals("合集测试标题", extractor.getName());
        assertEquals("https://i0.hdslb.com/bfs/archive/season_cover.jpg", extractor.getThumbnailUrl());
        assertEquals(2, extractor.getStreamCount());
        assertEquals("https://space.bilibili.com/11545648", extractor.getUploaderUrl());
    }

    @Test
    void fallsBackToFirstVideoPicWhenCoverIsEmpty() throws ParsingException, JsonParserException {
        JsonObject mockData = JsonParser.object().from("""
                {
                  "meta": {
                    "season_id": 649231,
                    "mid": 11545648,
                    "name": "无封面合集",
                    "cover": "",
                    "total": 1
                  },
                  "archives": [
                    {
                      "aid": 111,
                      "bvid": "BV1test1",
                      "title": "Video 1",
                      "pic": "http://i0.hdslb.com/bfs/archive/v1.jpg",
                      "duration": 120
                    }
                  ]
                }
                """);

        BilibiliPlaylistLinkHandlerFactory factory = new BilibiliPlaylistLinkHandlerFactory();
        ListLinkHandler handler = factory.fromUrl("bilibili:season:11545648:649231");
        BilibiliPlaylistExtractor extractor = new BilibiliPlaylistExtractor(ServiceList.BiliBili, handler);
        extractor.data = mockData;
        extractor.type = "seasons_archives";

        assertEquals("无封面合集", extractor.getName());
        assertEquals("https://i0.hdslb.com/bfs/archive/v1.jpg", extractor.getThumbnailUrl());
    }
}
