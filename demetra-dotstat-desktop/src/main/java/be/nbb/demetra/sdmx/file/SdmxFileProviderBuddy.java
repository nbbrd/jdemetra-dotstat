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
package be.nbb.demetra.sdmx.file;

import be.nbb.demetra.dotstat.DotStatOptionsPanelController;
import nbbrd.io.function.IOFunction;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.SdmxFileSource;
import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.FileLoaderFileFilter;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.IFileLoader;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.BuddyEventListener;
import internal.sdmx.SdmxAutoCompletion;
import internal.sdmx.SdmxCubeItems;
import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import sdmxdl.SdmxManager;
import org.openide.util.Lookup;
import sdmxdl.util.ext.MapCache;
import sdmxdl.xml.XmlFileSource;

/**
 *
 * @author Philippe Charles
 * @since 2.2.1
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public final class SdmxFileProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    private final Configurator<SdmxFileProviderBuddy> configurator;
    private final ConcurrentMap autoCompletionCache;

    public SdmxFileProviderBuddy() {
        this.configurator = createConfigurator();
        this.autoCompletionCache = GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1));
        lookupProvider().ifPresent(o -> o.setSdmxManager(createManager()));
    }

    @Override
    public String getProviderName() {
        return SdmxFileProvider.NAME;
    }

    @Override
    public Image getIcon(int type, boolean opened) {
        return ImageUtilities.loadImage("ec/nbdemetra/sdmx/document-code.png", true);
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
        if (bean instanceof SdmxFileBean) {
            Optional<SdmxFileProvider> provider = lookupProvider();
            if (provider.isPresent()) {
                return editBean(title, (SdmxFileBean) bean, provider.get());
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

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static SdmxFileManager createManager() {
        return SdmxFileManager.ofServiceLoader()
                .toBuilder()
                .eventListener(BuddyEventListener.INSTANCE)
                .cache(getCache())
                .build();
    }

    private static MapCache getCache() {
        return MapCache.of(GuavaCaches.softValuesCacheAsMap(), Clock.systemDefaultZone());
    }

    private static Optional<SdmxFileProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxFileProvider.class));
    }

    private static Configurator<SdmxFileProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(new BuddyConfigConverter());
    }

    private static final class BuddyConfig {

    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, SdmxFileProviderBuddy> {

        @Override
        public BuddyConfig loadBean(SdmxFileProviderBuddy resource) {
            return new BuddyConfig();
        }

        @Override
        public void storeBean(SdmxFileProviderBuddy resource, BuddyConfig bean) {
        }
    }

    private static final class BuddyConfigConverter extends Converter<BuddyConfig, Config> {

        @Override
        protected Config doForward(BuddyConfig a) {
            return Config.builder(Void.class.getName(), "INSTANCE", "2017").build();
        }

        @Override
        protected BuddyConfig doBackward(Config b) {
            return new BuddyConfig();
        }
    }

    private boolean editBean(String title, SdmxFileBean bean, SdmxFileProvider o) {
        return new PropertySheetDialogBuilder()
                .title(title)
                .icon(getIcon(BeanInfo.ICON_COLOR_16x16, false))
                .editSheet(createSheet(bean, o, o.getSdmxManager()));
    }

    @NbBundle.Messages({
        "bean.cache.description=Mechanism used to improve performance."})
    private Sheet createSheet(SdmxFileBean bean, IFileLoader loader, SdmxManager manager) {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.put(withSource(b.reset("Source"), bean, loader).build());
        result.put(withOptions(b.reset("Options"), bean, loader, manager).build());
        return result;
    }

    @NbBundle.Messages({
        "bean.file.display=Data file",
        "bean.file.description=The path to the sdmx data file.",})
    private NodePropertySetBuilder withSource(NodePropertySetBuilder b, SdmxFileBean bean, IFileLoader loader) {
        b.withFile()
                .select(bean, "file")
                .display(Bundle.bean_file_display())
                .description(Bundle.bean_file_description())
                .filterForSwing(new FileLoaderFileFilter(loader))
                .paths(loader.getPaths())
                .directories(false)
                .add();
        return b;
    }

    @NbBundle.Messages({
        "bean.structureFile.display=Structure file",
        "bean.structureFile.description=The path to the sdmx structure file.",
        "bean.dialect.display=Dialect",
        "bean.dialect.description=The name of the dialect used to parse the sdmx data file.",
        "bean.dimensions.display=Dataflow dimensions",
        "bean.dimensions.description=An optional comma-separated list of dimensions that defines the order used to hierarchise time series.",
        "bean.labelAttribute.display=Series label attribute",
        "bean.labelAttribute.description=An optional attribute that carries the label of time series."
    })
    private NodePropertySetBuilder withOptions(NodePropertySetBuilder b, SdmxFileBean bean, IFileLoader loader, SdmxManager manager) {
        b.withFile()
                .select(bean, "structureFile")
                .display(Bundle.bean_structureFile_display())
                .description(Bundle.bean_structureFile_description())
                .filterForSwing(new FileLoaderFileFilter(loader))
                .paths(loader.getPaths())
                .directories(false)
                .add();

        b.withAutoCompletion()
                .select(bean, "dialect")
                .source(SdmxAutoCompletion.onDialects())
                .cellRenderer(SdmxAutoCompletion.getDialectRenderer())
                .display(Bundle.bean_dialect_display())
                .description(Bundle.bean_dialect_description())
                .add();

        Supplier<String> toSource = () -> SdmxCubeItems.tryResolveFileSet(loader, bean).map(IOFunction.unchecked(XmlFileSource.getFormatter()::formatToString)).orElse("");
        Supplier<String> toFlow = () -> SdmxCubeItems.tryResolveFileSet(loader, bean).map(SdmxFileSource::asDataflowRef).map(Object::toString).orElse("");

        b.withAutoCompletion()
                .select(bean, "dimensions", List.class, Joiner.on(',')::join, Splitter.on(',').trimResults().omitEmptyStrings()::splitToList)
                .source(SdmxAutoCompletion.onDimensions(manager, toSource, toFlow, autoCompletionCache))
                .separator(",")
                .defaultValueSupplier(() -> SdmxAutoCompletion.getDefaultDimensionsAsString(manager, toSource, toFlow, autoCompletionCache, ","))
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
    //</editor-fold>
}
