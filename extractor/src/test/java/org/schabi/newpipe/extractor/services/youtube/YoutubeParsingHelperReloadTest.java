package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class YoutubeParsingHelperReloadTest {
    private Downloader previousDownloader;
    private Localization previousLocalization;
    private ContentCountry previousCountry;

    @Test
    void includesReloadTokenAndOmitsEmptyReloadContext() throws Exception {
        final Localization localization = new Localization("en", "US");
        final ContentCountry contentCountry = new ContentCountry("US");
        final byte[] withToken = YoutubeParsingHelper.createJsonPlayerBody(
                localization, contentCountry, "video-id", 123, "cpn", "MWEB", "agent",
                "reload-token");
        final JsonObject request = JsonParser.object().from(
                new String(withToken, StandardCharsets.UTF_8));

        assertEquals("reload-token", request.getObject("playbackContext")
                .getObject("reloadPlaybackContext")
                .getObject("reloadPlaybackParams")
                .getString("token"));

        final byte[] withoutToken = YoutubeParsingHelper.createJsonPlayerBody(
                localization, contentCountry, "video-id", 123, "cpn", "MWEB", "agent");
        final JsonObject requestWithoutToken = JsonParser.object().from(
                new String(withoutToken, StandardCharsets.UTF_8));

        assertFalse(requestWithoutToken.getObject("playbackContext")
                .containsKey("reloadPlaybackContext"));
    }

    @BeforeEach
    void setUp() {
        previousDownloader = NewPipe.getDownloader();
        previousLocalization = NewPipe.getPreferredLocalization();
        previousCountry = NewPipe.getPreferredContentCountry();
        YoutubeParsingHelper.resetClientVersion();
        NewPipe.init(new ClientVersionDownloader(), Localization.DEFAULT,
                ContentCountry.DEFAULT);
    }

    @AfterEach
    void tearDown() {
        YoutubeParsingHelper.resetClientVersion();
        NewPipe.init(previousDownloader, previousLocalization, previousCountry);
    }

    private static final class ClientVersionDownloader extends Downloader {
        @Override
        public Response execute(@Nonnull final Request request)
                throws IOException, ReCaptchaException {
            return new Response(200, "OK", Collections.emptyMap(),
                    "\"INNERTUBE_CONTEXT_CLIENT_VERSION\":\"2.20260101.00.00\"",
                    null, "https://www.youtube.com/sw.js");
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
