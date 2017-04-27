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
package be.nbb.demetra.sdmx.webservice;

import internal.desktop.PropertyAdapter;
import be.nbb.demetra.dotstat.SdmxWsAutoCompletionService;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import ec.nbdemetra.ui.properties.DhmsPropertyEditor;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.tstoolkit.utilities.GuavaCaches;
import ec.util.completion.AutoCompletionSource;
import static ec.util.completion.AutoCompletionSource.Behavior.ASYNC;
import static ec.util.completion.AutoCompletionSource.Behavior.NONE;
import static ec.util.completion.AutoCompletionSource.Behavior.SYNC;
import ec.util.completion.ExtAutoCompletionSource;
import internal.desktop.AutoCompletionRenderers;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
class Util {

    Sheet createSheet(SdmxWebServiceBean bean, SdmxConnectionSupplier supplier) {
        Sheet result = new Sheet();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.put(withSource(b.reset("Source"), bean, supplier).build());
        result.put(withCache(b.reset("Cache").description("Mechanism used to improve performance."), bean).build());
        return result;
    }

    @NbBundle.Messages({
        "bean.source.display=REST endpoint name",
        "bean.flow.display=Dataset",
        "bean.dimensions.display=Dimensions",
        "bean.labelAttribute.display=Label attribute"
    })
    private NodePropertySetBuilder withSource(NodePropertySetBuilder b, SdmxWebServiceBean bean, SdmxConnectionSupplier supplier) {
        b.withAutoCompletion()
                .select(bean, "source")
                .servicePath(SdmxWsAutoCompletionService.PATH)
                .display(Bundle.bean_source_display())
                .add();
        b.withAutoCompletion()
                .select(bean, "flow")
                .source(getFlowCompletion(bean, supplier))
                .cellRenderer(AutoCompletionRenderers.of(Dataflow::getLabel, o -> o.getFlowRef().toString()))
                .display(Bundle.bean_flow_display())
                .add();
        b.withAutoCompletion()
                .select(dimensionsProperty(bean))
                .source(getDimensionCompletion(bean, supplier))
                .separator(",")
                .defaultValueSupplier(() -> getDefaultDimensionsAsString(bean, supplier, ","))
                .cellRenderer(AutoCompletionRenderers.of(Dimension::getId, Dimension::getLabel))
                .display(Bundle.bean_dimensions_display())
                .add();
        b.withAutoCompletion()
                .select(bean, "labelAttribute")
                .display(Bundle.bean_labelAttribute_display())
                .add();
        return b;
    }

    @NbBundle.Messages({
        "bean.cacheDepth.display=Depth",
        "bean.cacheDepth.description=The data retrieval depth. It is always more performant to get one big chunk of data instead of several smaller parts. The downside of it is the increase of memory usage. Setting this value to zero disables the cache.",
        "bean.cacheTtl.display=Time to live",
        "bean.cacheTtl.description=The lifetime of the data stored in the cache. Setting this value to zero disables the cache."})
    private NodePropertySetBuilder withCache(NodePropertySetBuilder b, SdmxWebServiceBean bean) {
        b.withInt()
                .select(bean, "cacheDepth")
                .display(Bundle.bean_cacheDepth_display())
                .description(Bundle.bean_cacheDepth_description())
                .min(0)
                .add();
        b.with(long.class)
                .select(durationProperty(bean))
                .editor(DhmsPropertyEditor.class)
                .display(Bundle.bean_cacheTtl_display())
                .description(Bundle.bean_cacheTtl_description())
                .add();
        return b;
    }

    private AutoCompletionSource getFlowCompletion(SdmxWebServiceBean bean, SdmxConnectionSupplier supplier) {
        return ExtAutoCompletionSource
                .builder(o -> loadFlows(supplier, bean))
                .behavior(o -> canLoadFlows(bean) ? ASYNC : NONE)
                .postProcessor(Util::filterAndSortFlows)
                .valueToString(o -> o.getFlowRef().toString())
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> bean.getSource(), SYNC)
                .build();
    }

    private boolean canLoadFlows(SdmxWebServiceBean bean) {
        return !Strings.isNullOrEmpty(bean.getSource());
    }

    private List<Dataflow> loadFlows(SdmxConnectionSupplier supplier, SdmxWebServiceBean bean) throws IOException {
        try (SdmxConnection c = supplier.getConnection(bean.getSource())) {
            return new ArrayList<>(c.getDataflows());
        }
    }

    private List<Dataflow> filterAndSortFlows(List<Dataflow> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getLabel()) || filter.test(o.getFlowRef().getId()))
                .sorted(Comparator.comparing(Dataflow::getLabel))
                .collect(Collectors.toList());
    }

    private Node.Property<String> dimensionsProperty(SdmxWebServiceBean bean) {
        return PropertyAdapter.of(bean, "dimensions", List.class, String.class, Joiner.on(',')::join, Splitter.on(',').trimResults().omitEmptyStrings()::splitToList);
    }

    private AutoCompletionSource getDimensionCompletion(SdmxWebServiceBean bean, SdmxConnectionSupplier supplier) {
        return ExtAutoCompletionSource
                .builder(o -> loadDimensions(supplier, bean))
                .behavior(o -> canLoadDimensions(bean) ? ASYNC : NONE)
                .postProcessor(Util::filterAndSortDimensions)
                .valueToString(Dimension::getId)
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> bean.getSource() + "/" + bean.getFlow(), SYNC)
                .build();
    }

    private boolean canLoadDimensions(SdmxWebServiceBean bean) {
        return !Strings.isNullOrEmpty(bean.getSource()) && !Strings.isNullOrEmpty(bean.getFlow());
    }

    private List<Dimension> loadDimensions(SdmxConnectionSupplier supplier, SdmxWebServiceBean bean) throws IOException {
        try (SdmxConnection c = supplier.getConnection(bean.getSource())) {
            return new ArrayList<>(c.getDataStructure(DataflowRef.parse(bean.getFlow())).getDimensions());
        }
    }

    private List<Dimension> filterAndSortDimensions(List<Dimension> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getId()) || filter.test(o.getLabel()) || filter.test(String.valueOf(o.getPosition())))
                .sorted(Comparator.comparing(Dimension::getId))
                .collect(Collectors.toList());
    }

    private String getDefaultDimensionsAsString(SdmxWebServiceBean bean, SdmxConnectionSupplier supplier, CharSequence delimiter) throws Exception {
        return getDimensionCompletion(bean, supplier).getValues("").stream().map(o -> ((Dimension) o).getId()).collect(Collectors.joining(delimiter));
    }

    private Node.Property<Long> durationProperty(SdmxWebServiceBean bean) {
        return PropertyAdapter.of(bean, "cacheTtl", Duration.class, long.class, Duration::toMillis, Duration::ofMillis);
    }
}
