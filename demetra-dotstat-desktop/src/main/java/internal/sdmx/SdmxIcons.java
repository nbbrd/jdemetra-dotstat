package internal.sdmx;

import ec.tstoolkit.utilities.GuavaCaches;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import nbbrd.desktop.favicon.DomainName;
import nbbrd.desktop.favicon.FaviconRef;
import nbbrd.desktop.favicon.FaviconSupport;
import nbbrd.desktop.favicon.URLConnectionFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openide.util.ImageUtilities;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.web.spi.Network;
import sdmxdl.web.spi.Networking;
import sdmxdl.web.spi.SSLFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;

@lombok.experimental.UtilityClass
public class SdmxIcons {

    public static @NonNull ImageIcon getDefaultIcon() {
        return ImageUtilities.loadImageIcon("be/nbb/demetra/dotstat/sdmx-logo.png", false);
    }

    public static @NonNull Icon getFavicon(@NonNull Networking networking, @Nullable URL website) {
        return website != null
                ? getFavicons(networking).getOrDefault(FaviconRef.of(DomainName.of(website), 16), getDefaultIcon())
                : getDefaultIcon();
    }

    public static @NonNull Icon getFavicon(@NonNull Networking networking, @Nullable URL website, @NonNull Runnable callback) {
        return website != null
                ? getFavicons(networking).getOrDefault(FaviconRef.of(DomainName.of(website), 16), callback, getDefaultIcon())
                : getDefaultIcon();
    }

    private static FaviconSupport getFavicons(Networking networking) {
        return FAVICONS
                .toBuilder()
                .client(new FaviconClientOverCustomNetworking(networking))
                .build();
    }

    private static final FaviconSupport FAVICONS = FaviconSupport
            .ofServiceLoader()
            .toBuilder()
            .cache(GuavaCaches.ttlCacheAsMap(Duration.ofHours(1)))
            .build();

    @AllArgsConstructor
    private static final class FaviconClientOverCustomNetworking implements URLConnectionFactory {

        private final @NonNull Networking networking;

        @Override
        public @NonNull URLConnection openConnection(@NonNull URL url) throws IOException {
            Network network = networking.getNetwork(asSource(url));
            Proxy proxy = selectProxy(network, url);
            URLConnection result = network.getURLConnectionFactory().openConnection(url, proxy);
            applyHttps(result, network);
            return result;
        }

        private static SdmxWebSource asSource(URL url) throws IOException {
            try {
                return SdmxWebSource.builder().id("").endpoint(url.toURI()).driver("").build();
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }

        private static void applyHttps(URLConnection result, Network network) {
            if (result instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) result;
                SSLFactory sslFactory = network.getSSLFactory();
                https.setHostnameVerifier(sslFactory.getHostnameVerifier());
                https.setSSLSocketFactory(sslFactory.getSSLSocketFactory());
            }
        }

        private static Proxy selectProxy(Network network, URL url) throws IOException {
            try {
                return network.getProxySelector().select(url.toURI()).stream().findFirst().orElse(Proxy.NO_PROXY);
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }
    }
}
