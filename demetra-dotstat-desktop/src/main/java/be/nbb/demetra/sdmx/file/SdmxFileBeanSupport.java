package be.nbb.demetra.sdmx.file;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import ec.nbdemetra.ui.properties.FileLoaderFileFilter;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.tss.tsproviders.IFileLoader;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxAutoCompletion;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;
import sdmxdl.Dimension;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@lombok.experimental.UtilityClass
class SdmxFileBeanSupport {

    @NbBundle.Messages({
            "bean.cache.description=Mechanism used to improve performance."})
    public static Sheet newSheet(SdmxFileBean bean, SdmxFileProvider provider) {
        ConcurrentMap<Object, Object> autoCompletionCache = GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1));

        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.put(withSource(b.reset("Source"), bean, provider).build());
        result.put(withOptions(b.reset("Options"), bean, provider, autoCompletionCache).build());
        return result;
    }

    @NbBundle.Messages({
            "bean.file.display=Data file",
            "bean.file.description=The path to the sdmx data file.",})
    private static NodePropertySetBuilder withSource(NodePropertySetBuilder b, SdmxFileBean bean, IFileLoader loader) {
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

    private static NodePropertySetBuilder withOptions(NodePropertySetBuilder b, SdmxFileBean bean, SdmxFileProvider provider, ConcurrentMap<Object, Object> autoCompletionCache) {
        b.withFile()
                .select(bean, "structureFile")
                .display(Bundle.bean_structureFile_display())
                .description(Bundle.bean_structureFile_description())
                .filterForSwing(new FileLoaderFileFilter(provider))
                .paths(provider.getPaths())
                .directories(false)
                .add();

        SdmxAutoCompletion dimension = SdmxAutoCompletion.onDimension(provider, bean, autoCompletionCache);

        b.withAutoCompletion()
                .select(bean, "dimensions", java.util.List.class,
                        Joiner.on(',')::join, Splitter.on(',').trimResults().omitEmptyStrings()::splitToList)
                .source(dimension.getSource())
                .cellRenderer(dimension.getRenderer())
                .separator(",")
                .defaultValueSupplier(() -> dimension.getSource().getValues("").stream().map(sdmxdl.Dimension.class::cast).sorted(Comparator.comparingInt(sdmxdl.Dimension::getPosition)).map(Dimension::getId).collect(Collectors.joining(",")))
                .display(Bundle.bean_dimensions_display())
                .description(Bundle.bean_dimensions_description())
                .add();

        SdmxAutoCompletion attribute = SdmxAutoCompletion.onAttribute(provider, bean, autoCompletionCache);

        b.withAutoCompletion()
                .select(bean, "labelAttribute")
                .source(attribute.getSource())
                .cellRenderer(attribute.getRenderer())
                .display(Bundle.bean_labelAttribute_display())
                .description(Bundle.bean_labelAttribute_description())
                .add();
        return b;
    }
}
