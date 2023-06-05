package internal.sdmx.web;

import ec.nbdemetra.ui.notification.MessageUtil;
import internal.http.curl.CurlHttpURLConnection;
import lombok.NonNull;
import nbbrd.net.proxy.SystemProxySelector;
import nl.altindag.ssl.SSLFactory;
import org.openide.awt.StatusDisplayer;
import sdmxdl.DataRepository;
import sdmxdl.ext.Cache;
import sdmxdl.format.FileFormat;
import sdmxdl.format.spi.FileFormatProvider;
import sdmxdl.format.spi.FileFormatProviderLoader;
import sdmxdl.provider.ext.FileCache;
import sdmxdl.provider.ext.VerboseCache;
import sdmxdl.web.MonitorReports;
import sdmxdl.web.Network;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.URLConnectionFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.ProxySelector;
import java.util.function.BiConsumer;

@lombok.experimental.UtilityClass
public class SdmxWebFactory {

    public static SdmxWebManager createManager(boolean curlBackend) {
        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .eventListener((src, msg) -> StatusDisplayer.getDefault().setStatusText(msg))
                .network(getNetworkFactory(curlBackend))
                .cache(getCache())
                .build();
    }

    private static Network getNetworkFactory(boolean curlBackend) {
        SSLFactory sslFactory = SSLFactory
                .builder()
                .withDefaultTrustMaterial()
                .withSystemTrustMaterial()
                .build();

        return new Network() {
            @Override
            public HostnameVerifier getHostnameVerifier() {
                return sslFactory.getHostnameVerifier();
            }

            @Override
            public @NonNull URLConnectionFactory getURLConnectionFactory() {
                return curlBackend ? CurlHttpURLConnection::of : URLConnectionFactory.getDefault();
            }

            @Override
            public ProxySelector getProxySelector() {
                return SystemProxySelector.ofServiceLoader();
            }

            @Override
            public SSLSocketFactory getSSLSocketFactory() {
                return sslFactory.getSslSocketFactory();
            }
        };
    }

    private static Cache getCache() {
        FileCache fileCache = getFileCache(false);
        return getVerboseCache(fileCache, true);
    }

    private static FileCache getFileCache(boolean noCacheCompression) {
        FileFormatProvider formatProvider = FileFormatProviderLoader.load().stream().findFirst().orElseThrow(RuntimeException::new);
        FileFormat<DataRepository> repositoryFormat = formatProvider.getDataRepositoryFormat();
        FileFormat<MonitorReports> monitorFormat = formatProvider.getMonitorReportsFormat();
        return FileCache
                .builder()
                .repositoryFormat(noCacheCompression ? repositoryFormat : FileFormat.gzip(repositoryFormat))
                .monitorFormat(noCacheCompression ? monitorFormat : FileFormat.gzip(monitorFormat))
                .onIOException(MessageUtil::showException)
                .build();
    }

    private static Cache getVerboseCache(Cache delegate, boolean verbose) {
        if (verbose) {
            BiConsumer<String, Boolean> listener = (key, hit) -> StatusDisplayer.getDefault().setStatusText((hit ? "Hit " : "Miss ") + key);
            return new VerboseCache(delegate, listener, listener);
        }
        return delegate;
    }
}
