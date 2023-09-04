package be.nbb.demetra.sdmx.web;

import be.nbb.demetra.sdmx.Toggle;
import com.google.common.base.Converter;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import internal.sdmx.SdmxIcons;
import lombok.NonNull;
import nbbrd.io.text.Parser;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Sheet;
import sdmxdl.Languages;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import standalone_sdmxdl.sdmxdl.provider.ri.caching.RiCaching;
import standalone_sdmxdl.sdmxdl.provider.ri.drivers.SourceProperties;
import standalone_sdmxdl.sdmxdl.provider.ri.networking.RiNetworking;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@lombok.Data
public class SdmxWebConfiguration {

    private static final String SOURCES_PROPERTY = "sources";
    private static final File DEFAULT_SOURCES = null;
    private File sources = DEFAULT_SOURCES;

    private static final String LANGUAGES_PROPERTY = "languages";
    private static final String DEFAULT_LANGUAGES = null;
    private String languages = DEFAULT_LANGUAGES;

    private static final String CURL_BACKEND_PROPERTY = "curlBackend";
    private static final Toggle DEFAULT_CURL_BACKEND = Toggle.DEFAULT;
    private Toggle curlBackend = DEFAULT_CURL_BACKEND;

    private static final String NO_CACHE_PROPERTY = "noCache";
    private static final Toggle DEFAULT_NO_CACHE = Toggle.DEFAULT;
    private Toggle noCache = DEFAULT_NO_CACHE;

    private static final String AUTO_PROXY_PROPERTY = "autoProxy";
    private static final Toggle DEFAULT_AUTO_PROXY = Toggle.DEFAULT;
    private Toggle autoProxy = DEFAULT_AUTO_PROXY;

    private static final String NO_DEFAULT_SSL_PROPERTY = "noDefaultSSL";
    private static final Toggle DEFAULT_NO_DEFAULT_SSL = Toggle.DEFAULT;
    private Toggle noDefaultSSL = DEFAULT_NO_DEFAULT_SSL;

    private static final String NO_SYSTEM_SSL_PROPERTY = "noSystemSSL";
    private static final Toggle DEFAULT_NO_SYSTEM_SSL = Toggle.DEFAULT;
    private Toggle noSystemSSL = DEFAULT_NO_SYSTEM_SSL;

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
        Properties properties = System.getProperties();

        curlBackend.applyTo(properties, RiNetworking.CURL_BACKEND_PROPERTY);
        noCache.applyTo(properties, RiCaching.NO_CACHE_PROPERTY);
        autoProxy.applyTo(properties, RiNetworking.AUTO_PROXY_PROPERTY);
        noDefaultSSL.applyTo(properties, RiNetworking.NO_DEFAULT_SSL_PROPERTY);
        noSystemSSL.applyTo(properties, RiNetworking.NO_SYSTEM_SSL_PROPERTY);

        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .onEvent(this::reportEvent)
                .onError(this::reportError)
                .customSources(getCustomSources())
                .build();
    }

    private static List<SdmxWebSource> getCustomSources() {
        try {
            return SourceProperties.loadCustomSources();
        } catch (IOException e) {
            return Collections.emptyList();
        }
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
        b.withEnum(Toggle.class)
                .select(this, CURL_BACKEND_PROPERTY)
                .display("Curl backend")
                .description("Use curl backend instead of JDK")
                .add();
        b.withEnum(Toggle.class)
                .select(this, NO_CACHE_PROPERTY)
                .display("No cache")
                .description("Disable caching")
                .add();
        b.withEnum(Toggle.class)
                .select(this, AUTO_PROXY_PROPERTY)
                .display("Auto proxy")
                .description("Enable automatic proxy detection")
                .add();
        b.withEnum(Toggle.class)
                .select(this, NO_DEFAULT_SSL_PROPERTY)
                .display("No default SSL")
                .description("Disable default truststore")
                .add();
        b.withEnum(Toggle.class)
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
        private final IParam<Config, Toggle> curlBackend = Params.onEnum(DEFAULT_CURL_BACKEND, CURL_BACKEND_PROPERTY);
        private final IParam<Config, Toggle> noCache = Params.onEnum(DEFAULT_NO_CACHE, NO_CACHE_PROPERTY);
        private final IParam<Config, Toggle> autoProxy = Params.onEnum(DEFAULT_AUTO_PROXY, AUTO_PROXY_PROPERTY);
        private final IParam<Config, Toggle> noDefaultSSL = Params.onEnum(DEFAULT_NO_DEFAULT_SSL, NO_DEFAULT_SSL_PROPERTY);
        private final IParam<Config, Toggle> noSystemSSL = Params.onEnum(DEFAULT_NO_SYSTEM_SSL, NO_SYSTEM_SSL_PROPERTY);
        private final IParam<Config, Boolean> displayCodes = Params.onBoolean(DEFAULT_DISPLAY_CODES, DISPLAY_CODES_PROPERTY);

        @Override
        protected Config doForward(SdmxWebConfiguration a) {
            Config.Builder result = Config.builder(SdmxWebConfiguration.class.getName(), "INSTANCE", "20230717");
            sources.set(result, a.getSources());
            languages.set(result, a.getLanguages());
            curlBackend.set(result, a.getCurlBackend());
            noCache.set(result, a.getNoCache());
            autoProxy.set(result, a.getAutoProxy());
            noDefaultSSL.set(result, a.getNoDefaultSSL());
            noSystemSSL.set(result, a.getNoSystemSSL());
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
