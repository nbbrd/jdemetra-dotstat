package internal.sdmx;

import lombok.NonNull;
import nbbrd.net.proxy.SystemProxySelector;
import nl.altindag.ssl.SSLFactory;
import org.openide.awt.StatusDisplayer;
import sdmxdl.provider.web.SingleNetworkingSupport;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.spi.Networking;
import sdmxdl.web.spi.URLConnectionFactory;
import shaded.dotstat.nbbrd.io.curl.CurlHttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

@lombok.experimental.UtilityClass
public class SdmxWebFactory {

    public static SdmxWebManager createManager(boolean curlBackend) {
        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .onEvent((src, marker, msg) -> StatusDisplayer.getDefault().setStatusText(msg.toString()))
                .networking(getNetworking(curlBackend))
                .build();
    }

    private static Networking getNetworking(boolean curlBackend) {
        SystemProxySelector systemProxySelector = SystemProxySelector.ofServiceLoader();

        sdmxdl.web.spi.SSLFactory sslFactory = new SSLFactoryAdapter(SSLFactory
                .builder()
                .withDefaultTrustMaterial()
                .withSystemTrustMaterial()
                .build());

        URLConnectionFactory urlConnectionFactory = curlBackend ? CurlHttpURLConnection::of : URLConnectionFactory.getDefault();

        return SingleNetworkingSupport
                .builder()
                .id("DRY")
                .proxySelector(() -> systemProxySelector)
                .sslFactory(() -> sslFactory)
                .urlConnectionFactory(() -> urlConnectionFactory)
                .build();
    }

    @lombok.AllArgsConstructor
    private static final class SSLFactoryAdapter implements sdmxdl.web.spi.SSLFactory {

        private final @NonNull SSLFactory delegate;

        @Override
        public @NonNull SSLSocketFactory getSSLSocketFactory() {
            return delegate.getSslSocketFactory();
        }

        @Override
        public @NonNull HostnameVerifier getHostnameVerifier() {
            return delegate.getHostnameVerifier();
        }
    }
}
