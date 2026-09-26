package org.schabi.newpipe.extractor.services.niconico.linkHandler;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NiconicoChannelTabLinkHandlerFactoryTest {
    @Test
    void createsLivestreamTabFromUserUrl() throws Exception {
        final NiconicoChannelTabLinkHandlerFactory factory = new NiconicoChannelTabLinkHandlerFactory();
        final String channelId = "https://www.nicovideo.jp/user/123";

        final ListLinkHandler handler = factory.fromQuery(
                channelId,
                Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ChannelTabs.LIVESTREAMS)),
                null);

        assertEquals(channelId, handler.getUrl());
        assertEquals(ChannelTabs.LIVESTREAMS, handler.getContentFilters().get(0).getName());
    }
}
