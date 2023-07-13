package internal.sdmx;

import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import lombok.NonNull;
import nbbrd.desktop.favicon.DomainName;
import nbbrd.desktop.favicon.FaviconRef;
import nbbrd.desktop.favicon.FaviconSupport;
import nbbrd.desktop.favicon.URLConnectionFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
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
import java.util.HashMap;
import java.util.Optional;

@lombok.experimental.UtilityClass
public class SdmxIcons {

    public static final FaviconSupport FAVICONS = FaviconSupport
            .ofServiceLoader()
            .toBuilder()
            .client(new FaviconClientOverCustomNetwork())
            .cache(new HashMap<>())
            //            .cache(IOCacheFactoryLoader.get().ofTtl(Duration.ofHours(1)))
            .build();

    public static ImageIcon getDefaultIcon() {
        return ImageUtilities.loadImageIcon("be/nbb/demetra/dotstat/sdmx-logo.png", false);
    }

    public static Icon getFavicon(URL website) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), getDefaultIcon())
                : getDefaultIcon();
    }

    public static Icon getFavicon(URL website, Runnable callback) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), callback, getDefaultIcon())
                : getDefaultIcon();
    }

    private static final class FaviconClientOverCustomNetwork implements URLConnectionFactory {

        @Override
        public @NonNull URLConnection openConnection(@NonNull URL url) throws IOException {
            Network network = getNetworking().getNetwork(asSource(url));
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

        private static Networking getNetworking() {
            return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class))
                    .map(provider -> provider.getSdmxManager().getNetworking())
                    .orElseGet(Networking::getDefault);
        }
    }
}
