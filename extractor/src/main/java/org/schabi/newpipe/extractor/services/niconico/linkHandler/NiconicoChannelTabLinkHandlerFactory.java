package org.schabi.newpipe.extractor.services.niconico.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

public class NiconicoChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private final NiconicoUserLinkHandlerFactory userLinkHandlerFactory =
            new NiconicoUserLinkHandlerFactory();

    @Override
    public String getId(final String url) throws ParsingException {
        return userLinkHandlerFactory.getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return userLinkHandlerFactory.onAcceptUrl(url);
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) {
        return id;
    }
}
