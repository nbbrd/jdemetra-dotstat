package be.nbb.demetra.sdmx.web;

import com.google.common.base.Converter;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.notification.MessageUtil;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import internal.sdmx.CustomNetwork;
import internal.sdmx.SdmxIcons;
import lombok.NonNull;
import nbbrd.io.text.Parser;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Sheet;
import sdmxdl.Languages;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.web.spi.Networking;
import sdmxdl.web.spi.WebCaching;
import standalone_sdmxdl.internal.util.WebCachingLoader;
import standalone_sdmxdl.sdmxdl.format.xml.XmlWebSource;
import standalone_sdmxdl.sdmxdl.provider.web.SingleNetworkingSupport;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@lombok.Data
public class SdmxWebConfiguration {

    private static final String SOURCES_PROPERTY = "sources";
    private static final File DEFAULT_SOURCES = null;
    private File sources = DEFAULT_SOURCES;

    private static final String LANGUAGES_PROPERTY = "languages";
    private static final String DEFAULT_LANGUAGES = null;
    private String languages = DEFAULT_LANGUAGES;

    private static final String CURL_BACKEND_PROPERTY = "curlBackend";
    private static final boolean DEFAULT_CURL_BACKEND = false;
    private boolean curlBackend = DEFAULT_CURL_BACKEND;

    private static final String NO_CACHE_PROPERTY = "noCache";
    private static final boolean DEFAULT_NO_CACHE = false;
    private boolean noCache = DEFAULT_NO_CACHE;

    private static final String AUTO_PROXY_PROPERTY = "autoProxy";
    private static final boolean DEFAULT_AUTO_PROXY = false;
    private boolean autoProxy = DEFAULT_AUTO_PROXY;

    private static final String NO_DEFAULT_SSL_PROPERTY = "noDefaultSSL";
    private static final boolean DEFAULT_NO_DEFAULT_SSL = false;
    private boolean noDefaultSSL = DEFAULT_NO_DEFAULT_SSL;

    private static final String NO_SYSTEM_SSL_PROPERTY = "noSystemSSL";
    private static final boolean DEFAULT_NO_SYSTEM_SSL = false;
    private boolean noSystemSSL = DEFAULT_NO_SYSTEM_SSL;

    private static final String DISPLAY_CODES_PROPERTY = "displayCodes";
    private static final boolean DEFAULT_DISPLAY_CODES = false;
    private boolean displayCodes = DEFAULT_DISPLAY_CODES;

    public static SdmxWebConfiguration copyOf(SdmxWebConfiguration bean) {
        SdmxWebConfiguration result = new SdmxWebConfiguration();
        result.sources = bean.sources;
        result.languages = bean.languages;
        result.curlBackend = bean.curlBackend;
        result.noCache = bean.noCache;
        result.autoProxy = bean.autoProxy;
        result.noDefaultSSL = bean.noDefaultSSL;
        result.noSystemSSL = bean.noSystemSSL;
        result.displayCodes = bean.displayCodes;
        return result;
    }

    public SdmxWebManager toSdmxWebManager() {
        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .onEvent(this::reportEvent)
                .onError(this::reportError)
                .caching(toCaching())
                .networking(toNetworking())
                .customSources(toSources())
                .build();
    }

    public Languages toLanguages() {
        return Parser.of(Languages::parse)
                .parseValue(languages)
                .orElse(Languages.ANY);
    }

    private void reportEvent(SdmxWebSource source, String marker, CharSequence message) {
        StatusDisplayer.getDefault().setStatusText(message.toString());
    }

    private void reportError(SdmxWebSource source, String marker, CharSequence message, IOException error) {
        NotificationDisplayer.getDefault().notify(message.toString(), SdmxIcons.getDefaultIcon(), "", null);
    }

    private WebCaching toCaching() {
        if (noCache) {
            return WebCaching.noOp();
        }
        return WebCachingLoader.load();
    }

    private Networking toNetworking() {
        CustomNetwork x = CustomNetwork
                .builder()
                .curlBackend(curlBackend)
                .autoProxy(autoProxy)
                .defaultTrustMaterial(!noDefaultSSL)
                .systemTrustMaterial(!noSystemSSL)
                .build();
        return SingleNetworkingSupport
                .builder()
                .id("DRY")
                .proxySelector(x::getProxySelector)
                .sslFactory(x::getSSLFactory)
                .urlConnectionFactory(x::getURLConnectionFactory)
                .build();
    }

    private List<SdmxWebSource> toSources() {
        if (sources != null && sources.exists()) {
            try {
                return XmlWebSource.getParser().parseFile(sources);
            } catch (IOException ex) {
                MessageUtil.showException("Cannot load custom sources", ex);
            }
        }
        return Collections.emptyList();
    }

    Sheet toSheet() {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();

        b.withFile()
                .select(this, SOURCES_PROPERTY)
                .display("Sources")
                .description("File that provides data source definitions")
                .filterForSwing(new FileNameExtensionFilter("XML file", "xml"))
                .directories(false)
                .add();
        b.withAutoCompletion()
                .select(this, LANGUAGES_PROPERTY)
                .servicePath(Locale.class.getName())
                .separator(",")
                .display("Languages")
                .description("Language priority list")
                .add();
        result.put(b.build());

        b.reset("Network");
        b.withBoolean()
                .select(this, CURL_BACKEND_PROPERTY)
                .display("Curl backend")
                .description("Use curl backend instead of JDK")
                .add();
        b.withBoolean()
                .select(this, NO_CACHE_PROPERTY)
                .display("No cache")
                .description("Disable caching")
                .add();
        b.withBoolean()
                .select(this, AUTO_PROXY_PROPERTY)
                .display("Auto proxy")
                .description("Enable automatic proxy detection")
                .add();
        b.withBoolean()
                .select(this, NO_DEFAULT_SSL_PROPERTY)
                .display("No default SSL")
                .description("Disable default truststore")
                .add();
        b.withBoolean()
                .select(this, NO_SYSTEM_SSL_PROPERTY)
                .display("No system SSL")
                .description("Disable system truststore")
                .add();
        result.put(b.build());

        return result;
    }

    static final Configurator<SdmxWebProviderBuddy> CONFIGURATOR = new ConfigHandler()
            .toConfigurator(new ConfigConverter(), SdmxWebConfiguration::editConfiguration);

    private static boolean editConfiguration(Object bean) {
        return bean instanceof SdmxWebConfiguration && new PropertySheetDialogBuilder().title("Configure").editSheet(((SdmxWebConfiguration) bean).toSheet());
    }

    private static final class ConfigHandler extends BeanHandler<SdmxWebConfiguration, SdmxWebProviderBuddy> {

        @Override
        public @NonNull SdmxWebConfiguration loadBean(SdmxWebProviderBuddy resource) {
            return SdmxWebConfiguration.copyOf(resource.getConfiguration());
        }

        @Override
        public void storeBean(SdmxWebProviderBuddy resource, @NonNull SdmxWebConfiguration bean) {
            resource.setConfiguration(bean);
        }
    }

    private static final class ConfigConverter extends Converter<SdmxWebConfiguration, Config> {

        private final IParam<Config, File> sources = Params.onFile(new File(""), SOURCES_PROPERTY);
        private final IParam<Config, String> languages = Params.onString("", LANGUAGES_PROPERTY);
        private final IParam<Config, Boolean> curlBackend = Params.onBoolean(DEFAULT_CURL_BACKEND, CURL_BACKEND_PROPERTY);
        private final IParam<Config, Boolean> noCache = Params.onBoolean(DEFAULT_NO_CACHE, NO_CACHE_PROPERTY);
        private final IParam<Config, Boolean> autoProxy = Params.onBoolean(DEFAULT_AUTO_PROXY, AUTO_PROXY_PROPERTY);
        private final IParam<Config, Boolean> noDefaultSSL = Params.onBoolean(DEFAULT_NO_DEFAULT_SSL, NO_DEFAULT_SSL_PROPERTY);
        private final IParam<Config, Boolean> noSystemSSL = Params.onBoolean(DEFAULT_NO_SYSTEM_SSL, NO_SYSTEM_SSL_PROPERTY);
        private final IParam<Config, Boolean> displayCodes = Params.onBoolean(DEFAULT_DISPLAY_CODES, DISPLAY_CODES_PROPERTY);

        @Override
        protected Config doForward(SdmxWebConfiguration a) {
            Config.Builder result = Config.builder(SdmxWebConfiguration.class.getName(), "INSTANCE", "20230717");
            sources.set(result, a.getSources());
            languages.set(result, a.getLanguages());
            curlBackend.set(result, a.isCurlBackend());
            noCache.set(result, a.isNoCache());
            autoProxy.set(result, a.isAutoProxy());
            noDefaultSSL.set(result, a.isNoDefaultSSL());
            noSystemSSL.set(result, a.isNoSystemSSL());
            displayCodes.set(result, a.isDisplayCodes());
            return result.build();
        }

        @Override
        protected SdmxWebConfiguration doBackward(Config b) {
            SdmxWebConfiguration result = new SdmxWebConfiguration();
            result.setSources(sources.get(b));
            result.setLanguages(languages.get(b));
            result.setCurlBackend(curlBackend.get(b));
            result.setNoCache(noCache.get(b));
            result.setAutoProxy(autoProxy.get(b));
            result.setNoDefaultSSL(noDefaultSSL.get(b));
            result.setNoSystemSSL(noSystemSSL.get(b));
            result.setDisplayCodes(displayCodes.get(b));
            return result;
        }
    }
}
