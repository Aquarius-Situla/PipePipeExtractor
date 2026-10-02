package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliStreamLinkHandlerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BilibiliStreamPageParserTest {
    @Test
    void selectsRequestedSecondPartFromCanonicalBilibiliUrl()
            throws Exception {
        final LinkHandler handler = new BilibiliStreamLinkHandlerFactory().fromUrl(
                "https://www.bilibili.com/video/BV1ex411J7GE?p=2");
        assertEquals("https://www.bilibili.com/video/BV1ex411J7GE?p=2", handler.getUrl());

        final JsonObject watch = JsonParser.object().from("""
                {"pages":[{"page":1,"cid":66445301,"part":"Part one"},
                          {"page":2,"cid":35039663,"part":"Part two"},
                          {"page":3,"cid":35039678,"part":"Part three"}]}
                """);
        assertEquals(66445301, BilibiliStreamPageParser.pageForUrl(watch,
                "https://www.bilibili.com/video/BV1ex411J7GE?p=1").getLong("cid"));
        final JsonObject selected = BilibiliStreamPageParser.pageForUrl(watch, handler.getUrl());
        assertEquals(2, selected.getInt("page"));
        assertEquals(35039663, selected.getLong("cid"));
        assertEquals("Part two", selected.getString("part"));
        assertEquals(35039678, BilibiliStreamPageParser.pageForUrl(watch,
                "https://www.bilibili.com/video/BV1ex411J7GE?p=3").getLong("cid"));
    }

    @Test
    void defaultsToFirstPartWhenUrlHasNoPageParameter() throws Exception {
        final JsonObject watch = JsonParser.object().from("""
                {"pages":[{"page":1,"cid":10},{"page":2,"cid":20}]}
                """);
        assertEquals(10, BilibiliStreamPageParser.pageForUrl(watch,
                "https://www.bilibili.com/video/BV1ex411J7GE").getLong("cid"));
    }
}
