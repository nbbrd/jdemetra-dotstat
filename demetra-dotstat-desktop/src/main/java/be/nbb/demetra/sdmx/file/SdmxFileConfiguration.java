package be.nbb.demetra.sdmx.file;

import com.google.common.base.Converter;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxIcons;
import lombok.NonNull;
import nbbrd.io.text.Parser;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Sheet;
import sdmxdl.Languages;
import sdmxdl.file.FileSource;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.spi.FileCaching;
import standalone_sdmxdl.sdmxdl.format.MemCachingSupport;

import java.io.IOException;
import java.util.Locale;

@lombok.Data
public class SdmxFileConfiguration {

    private static final String LANGUAGES_PROPERTY = "languages";
    private static final String DEFAULT_LANGUAGES = null;
    private String languages = DEFAULT_LANGUAGES;

    private static final String NO_CACHE_PROPERTY = "noCache";
    private static final boolean DEFAULT_NO_CACHE = false;
    private boolean noCache = DEFAULT_NO_CACHE;

    public static SdmxFileConfiguration copyOf(SdmxFileConfiguration bean) {
        SdmxFileConfiguration result = new SdmxFileConfiguration();
        result.languages = bean.languages;
        result.noCache = bean.noCache;
        return result;
    }

    public SdmxFileManager toSdmxFileManager() {
        return SdmxFileManager.ofServiceLoader()
                .toBuilder()
                .onEvent(this::reportEvent)
                .onError(this::reportError)
                .caching(toCaching())
                .build();
    }

    public Languages toLanguages() {
        return Parser.of(Languages::parse)
                .parseValue(languages)
                .orElse(Languages.ANY);
    }


    private void reportEvent(FileSource source, String marker, CharSequence message) {
        StatusDisplayer.getDefault().setStatusText(message.toString());
    }

    private void reportError(FileSource source, String marker, CharSequence message, IOException error) {
        NotificationDisplayer.getDefault().notify(message.toString(), SdmxIcons.getDefaultIcon(), "", null);
    }

    private FileCaching toCaching() {
        return noCache
                ? FileCaching.noOp()
                : MemCachingSupport
                .builder()
                .id("SHARED_SOFT_MEM")
                .repositoriesOf(GuavaCaches.softValuesCacheAsMap())
                .webMonitorsOf(GuavaCaches.softValuesCacheAsMap())
                .build();
    }

    Sheet toSheet() {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();

        b.withAutoCompletion()
                .select(this, LANGUAGES_PROPERTY)
                .servicePath(Locale.class.getName())
                .separator(",")
                .display("Languages")
                .description("Language priority list")
                .add();
        result.put(b.build());

        b.reset("Other");
        b.withBoolean()
                .select(this, NO_CACHE_PROPERTY)
                .display("No cache")
                .description("Disable caching")
                .add();
        result.put(b.build());

        return result;
    }

    static final Configurator<SdmxFileProviderBuddy> CONFIGURATOR = new ConfigHandler()
            .toConfigurator(new ConfigConverter(), SdmxFileConfiguration::editConfiguration);

    private static boolean editConfiguration(Object bean) {
        return bean instanceof SdmxFileConfiguration && new PropertySheetDialogBuilder().title("Configure").editSheet(((SdmxFileConfiguration) bean).toSheet());
    }

    private static final class ConfigHandler extends BeanHandler<SdmxFileConfiguration, SdmxFileProviderBuddy> {

        @Override
        public @NonNull SdmxFileConfiguration loadBean(@NonNull SdmxFileProviderBuddy resource) {
            return SdmxFileConfiguration.copyOf(resource.getConfiguration());
        }

        @Override
        public void storeBean(@NonNull SdmxFileProviderBuddy resource, @NonNull SdmxFileConfiguration bean) {
            resource.setConfiguration(bean);
        }
    }

    private static final class ConfigConverter extends Converter<SdmxFileConfiguration, Config> {

        private final IParam<Config, String> languages = Params.onString("", LANGUAGES_PROPERTY);
        private final IParam<Config, Boolean> noCache = Params.onBoolean(DEFAULT_NO_CACHE, NO_CACHE_PROPERTY);

        @Override
        protected Config doForward(SdmxFileConfiguration a) {
            Config.Builder result = Config.builder(SdmxFileConfiguration.class.getName(), "INSTANCE", "20230717");
            languages.set(result, a.getLanguages());
            noCache.set(result, a.isNoCache());
            return result.build();
        }

        @Override
        protected SdmxFileConfiguration doBackward(Config b) {
            SdmxFileConfiguration result = new SdmxFileConfiguration();
            result.setLanguages(languages.get(b));
            result.setNoCache(noCache.get(b));
            return result;
        }
    }
}
