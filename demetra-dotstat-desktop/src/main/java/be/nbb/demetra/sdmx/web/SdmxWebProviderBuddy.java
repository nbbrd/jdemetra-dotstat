/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.demetra.sdmx.web;

import internal.sdmx.BuddyEventListener;
import be.nbb.demetra.dotstat.DotStatOptionsPanelController;
import be.nbb.demetra.dotstat.DotStatProviderBuddy.BuddyConfig;
import be.nbb.demetra.dotstat.SdmxWsAutoCompletionService;
import sdmxdl.LanguagePriorityList;
import sdmxdl.ext.SdmxCache;
import sdmxdl.web.SdmxWebManager;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import ec.nbdemetra.db.DbIcon;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.notification.MessageUtil;
import ec.nbdemetra.ui.properties.DhmsPropertyEditor;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.DataSet;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxAutoCompletion;
import internal.sdmx.SdmxPropertiesSupport;
import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import sdmxdl.SdmxManager;
import java.net.ProxySelector;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;
import nbbrd.net.proxy.SystemProxySelector;
import nl.altindag.ssl.SSLFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Lookup;
import sdmxdl.kryo.KryoSerialization;
import sdmxdl.sys.SdmxSystemUtil;
import sdmxdl.util.ext.FileCache;
import sdmxdl.util.ext.Serializer;
import sdmxdl.web.SdmxWebAuthenticator;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.xml.XmlWebSource;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class, supersedes = "be.nbb.demetra.dotstat.DotStatProviderBuddy")
public final class SdmxWebProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    private final Configurator<SdmxWebProviderBuddy> configurator;
    private final ConcurrentMap autoCompletionCache;

    private File customSources;

    @lombok.Getter
    private SdmxWebManager webManager;

    public SdmxWebProviderBuddy() {
        this.configurator = createConfigurator();
        this.autoCompletionCache = GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1));
        this.customSources = new File("");
        this.webManager = createManager();
        lookupProvider().ifPresent(o -> o.setSdmxManager(webManager));
    }

    @Override
    public String getProviderName() {
        return SdmxWebProvider.NAME;
    }

    @Override
    public Image getIcon(int type, boolean opened) {
        return DbIcon.DATABASE.getImageIcon().getImage();
    }

    @Override
    public Image getIcon(DataSet dataSet, int type, boolean opened) {
        switch (dataSet.getKind()) {
            case COLLECTION:
                return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/folder.png", true);
            case SERIES:
                return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/chart_line.png", true);
            case DUMMY:
                return null;
        }
        return IDataSourceProviderBuddy.super.getIcon(dataSet, type, opened);
    }

    @Override
    public Image getIcon(IOException ex, int type, boolean opened) {
        return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/exclamation-red.png", true);
    }

    @Override
    public boolean editBean(String title, Object bean) throws IntrospectionException {
        if (bean instanceof SdmxWebBean) {
            Optional<SdmxWebProvider> provider = lookupProvider();
            if (provider.isPresent()) {
                SdmxWebProvider o = provider.get();
                return new PropertySheetDialogBuilder()
                        .title(title)
                        .icon(getIcon(BeanInfo.ICON_COLOR_16x16, false))
                        .editSheet(createSheet((SdmxWebBean) bean, o.getSdmxManager(), autoCompletionCache));
            }
        }
        return IDataSourceProviderBuddy.super.editBean(title, bean);
    }

    @Override
    public Config getConfig() {
        return configurator.getConfig(this);
    }

    @Override
    public void setConfig(Config config) throws IllegalArgumentException {
        configurator.setConfig(this, config);
    }

    @Override
    public Config editConfig(Config config) throws IllegalArgumentException {
        OptionsDisplayer.getDefault().open(DotStatOptionsPanelController.ID);
        return config;
    }

    @Deprecated
    public void setProxySelector(@Nullable ProxySelector proxySelector) {
    }

    @Deprecated
    public void setSSLSocketFactory(@Nullable SSLSocketFactory sslSocketFactory) {
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static Optional<SdmxWebProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class));
    }

    private static Configurator<SdmxWebProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(BuddyConfig.converter());
    }

    private static SdmxWebManager createManager() {
        SSLFactory sslFactory = SSLFactory
                .builder()
                .withDefaultTrustMaterial()
                .withSystemTrustMaterial()
                .build();

        return SdmxWebManager.ofServiceLoader()
                .toBuilder()
                .eventListener(BuddyEventListener.INSTANCE)
                .proxySelector(SystemProxySelector.ofServiceLoader())
                .sslSocketFactory(sslFactory.getSslSocketFactory())
                .hostnameVerifier(sslFactory.getHostnameVerifier())
                .cache(getCache())
                .authenticator(getNetBeansAuthenticator())
                .build();
    }

    private static SdmxCache getCache() {
        return FileCache
                .builder()
                .serializer(Serializer.gzip(new KryoSerialization()))
                .onIOException(MessageUtil::showException)
                .build();
    }

    private static SdmxWebAuthenticator getNetBeansAuthenticator() {
        PasswordAuthentication noUser = new PasswordAuthentication(null, new char[0]);
        SdmxWebAuthenticator result = SdmxSystemUtil.getAuthenticatorOrNull(noUser, MessageUtil::showException);
        return result != null ? result : SdmxWebAuthenticator.noOp();
    }

    private static List<SdmxWebSource> loadSources(File file) {
        if (file.exists()) {
            try {
                return XmlWebSource.getParser().parseFile(file);
            } catch (IOException ex) {
                StatusDisplayer.getDefault().setStatusText(ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, SdmxWebProviderBuddy> {

        @Override
        public BuddyConfig loadBean(SdmxWebProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            result.setCustomSources(resource.customSources);
            result.setPreferredLanguage(resource.webManager.getLanguages().toString());
            lookupProvider().ifPresent(provider -> {
                result.setDisplayCodes(provider.isDisplayCodes());
            });
            return result;
        }

        @Override
        public void storeBean(SdmxWebProviderBuddy resource, BuddyConfig bean) {
            resource.customSources = bean.getCustomSources();
            resource.webManager = resource.webManager
                    .toBuilder()
                    .customSources(loadSources(resource.customSources))
                    .languages(SdmxPropertiesSupport.tryParseLangs(bean.getPreferredLanguage()).orElse(LanguagePriorityList.ANY))
                    .build();
            lookupProvider().ifPresent(provider -> {
                provider.setDisplayCodes(bean.isDisplayCodes());
                provider.setSdmxManager(resource.webManager);
            });
        }
    }

    @NbBundle.Messages({
        "bean.cache.description=Mechanism used to improve performance."})
    private static Sheet createSheet(SdmxWebBean bean, SdmxManager manager, ConcurrentMap cache) {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.put(withSource(b.reset("Source"), bean, manager, cache).build());
        result.put(withOptions(b.reset("Options"), bean, manager, cache).build());
        result.put(withCache(b.reset("Cache").description(Bundle.bean_cache_description()), bean).build());
        return result;
    }

    @NbBundle.Messages({
        "bean.source.display=Provider",
        "bean.source.description=The identifier of the service that provides data.",
        "bean.flow.display=Dataflow",
        "bean.flow.description=The identifier of a specific dataflow.",})
    private static NodePropertySetBuilder withSource(NodePropertySetBuilder b, SdmxWebBean bean, SdmxManager manager, ConcurrentMap cache) {
        b.withAutoCompletion()
                .select(bean, "source")
                .servicePath(SdmxWsAutoCompletionService.PATH)
                .display(Bundle.bean_source_display())
                .description(Bundle.bean_source_description())
                .add();
        b.withAutoCompletion()
                .select(bean, "flow")
                .source(SdmxAutoCompletion.onFlows(manager, bean::getSource, cache))
                .cellRenderer(SdmxAutoCompletion.getFlowsRenderer())
                .display(Bundle.bean_flow_display())
                .description(Bundle.bean_flow_description())
                .add();
        return b;
    }

    @NbBundle.Messages({
        "bean.dimensions.display=Dataflow dimensions",
        "bean.dimensions.description=An optional comma-separated list of dimensions that defines the order used to hierarchise time series.",
        "bean.labelAttribute.display=Series label attribute",
        "bean.labelAttribute.description=An optional attribute that carries the label of time series."
    })
    private static NodePropertySetBuilder withOptions(NodePropertySetBuilder b, SdmxWebBean bean, SdmxManager manager, ConcurrentMap cache) {
        b.withAutoCompletion()
                .select(bean, "dimensions", List.class, Joiner.on(',')::join, Splitter.on(',').trimResults().omitEmptyStrings()::splitToList)
                .source(SdmxAutoCompletion.onDimensions(manager, bean::getSource, bean::getFlow, cache))
                .separator(",")
                .defaultValueSupplier(() -> SdmxAutoCompletion.getDefaultDimensionsAsString(manager, bean::getSource, bean::getFlow, cache, ","))
                .cellRenderer(SdmxAutoCompletion.getDimensionsRenderer())
                .display(Bundle.bean_dimensions_display())
                .description(Bundle.bean_dimensions_description())
                .add();
        b.withAutoCompletion()
                .select(bean, "labelAttribute")
                .display(Bundle.bean_labelAttribute_display())
                .description(Bundle.bean_labelAttribute_description())
                .add();
        return b;
    }

    @NbBundle.Messages({
        "bean.cacheDepth.display=Depth",
        "bean.cacheDepth.description=The data retrieval depth. It is always more performant to get one big chunk of data instead of several smaller parts. The downside of it is the increase of memory usage. Setting this value to zero disables the cache.",
        "bean.cacheTtl.display=Time to live",
        "bean.cacheTtl.description=The lifetime of the data stored in the cache. Setting this value to zero disables the cache."})
    private static NodePropertySetBuilder withCache(NodePropertySetBuilder b, SdmxWebBean bean) {
        b.withInt()
                .select(bean, "cacheDepth")
                .display(Bundle.bean_cacheDepth_display())
                .description(Bundle.bean_cacheDepth_description())
                .min(0)
                .add();
        b.with(long.class)
                .select(bean, "cacheTtl", Duration.class, Duration::toMillis, Duration::ofMillis)
                .editor(DhmsPropertyEditor.class)
                .display(Bundle.bean_cacheTtl_display())
                .description(Bundle.bean_cacheTtl_description())
                .add();
        return b;
    }
    //</editor-fold>
}
