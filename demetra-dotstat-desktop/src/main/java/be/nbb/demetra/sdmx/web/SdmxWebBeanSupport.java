package be.nbb.demetra.sdmx.web;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import ec.nbdemetra.ui.properties.DhmsPropertyEditor;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxAutoCompletion;
import internal.sdmx.SdmxWebSourceService;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;
import sdmxdl.Dimension;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@lombok.experimental.UtilityClass
class SdmxWebBeanSupport {

    @NbBundle.Messages({
            "bean.cache.description=Mechanism used to improve performance."})
    public static Sheet newSheet(SdmxWebBean bean, SdmxWebProvider provider) {
        ConcurrentMap<Object, Object> autoCompletionCache = GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1));

        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.put(withSource(b.reset("Source"), bean, provider, autoCompletionCache).build());
        result.put(withOptions(b.reset("Options"), bean, provider, autoCompletionCache).build());
        result.put(withCache(b.reset("Cache").description(Bundle.bean_cache_description()), bean).build());
        return result;
    }

    @NbBundle.Messages({
            "bean.source.display=Provider",
            "bean.source.description=The identifier of the service that provides data.",
            "bean.flow.display=Dataflow",
            "bean.flow.description=The identifier of a specific dataflow.",})
    private static NodePropertySetBuilder withSource(NodePropertySetBuilder b, SdmxWebBean bean, SdmxWebProvider provider, ConcurrentMap<Object, Object> autoCompletionCache) {
        b.withAutoCompletion()
                .select(bean, "source")
                .servicePath(SdmxWebSourceService.PATH)
                .display(Bundle.bean_source_display())
                .description(Bundle.bean_source_description())
                .add();

        SdmxAutoCompletion dataflow = SdmxAutoCompletion.onDataflow(provider, bean, autoCompletionCache);

        b.withAutoCompletion()
                .select(bean, "flow")
                .source(dataflow.getSource())
                .cellRenderer(dataflow.getRenderer())
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
    private static NodePropertySetBuilder withOptions(NodePropertySetBuilder b, SdmxWebBean bean, SdmxWebProvider provider, ConcurrentMap<Object, Object> autoCompletionCache) {
        SdmxAutoCompletion dimension = SdmxAutoCompletion.onDimension(provider, bean, autoCompletionCache);

        b.withAutoCompletion()
                .select(bean, "dimensions", List.class,
                        Joiner.on(',')::join, Splitter.on(',').trimResults().omitEmptyStrings()::splitToList)
                .source(dimension.getSource())
                .separator(",")
                .defaultValueSupplier(() -> dimension.getSource().getValues("").stream().map(sdmxdl.Dimension.class::cast).sorted(Comparator.comparingInt(sdmxdl.Dimension::getPosition)).map(Dimension::getId).collect(Collectors.joining(",")))
                .cellRenderer(dimension.getRenderer())
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
                .select(bean, "cacheTtl", Duration.class,
                        Duration::toMillis, Duration::ofMillis)
                .editor(DhmsPropertyEditor.class)
                .display(Bundle.bean_cacheTtl_display())
                .description(Bundle.bean_cacheTtl_description())
                .add();
        return b;
    }
}
