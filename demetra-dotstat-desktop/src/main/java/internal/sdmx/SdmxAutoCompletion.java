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
package internal.sdmx;

import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import com.google.common.base.Strings;
import ec.tstoolkit.utilities.GuavaCaches;
import ec.util.completion.AutoCompletionSource;
import static ec.util.completion.AutoCompletionSource.Behavior.ASYNC;
import static ec.util.completion.AutoCompletionSource.Behavior.NONE;
import static ec.util.completion.AutoCompletionSource.Behavior.SYNC;
import ec.util.completion.ExtAutoCompletionSource;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxAutoCompletion {

    public AutoCompletionSource onFlows(SdmxConnectionSupplier supplier, Supplier<String> source) {
        return ExtAutoCompletionSource
                .builder(o -> loadFlows(supplier, source))
                .behavior(o -> canLoadFlows(source) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortFlows)
                .valueToString(o -> o.getFlowRef().toString())
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> getFlowCacheKey(source), SYNC)
                .build();
    }

    public AutoCompletionSource onDimensions(SdmxConnectionSupplier supplier, Supplier<String> source, Supplier<String> flow) {
        return ExtAutoCompletionSource
                .builder(o -> loadDimensions(supplier, source, flow))
                .behavior(o -> canLoadDimensions(source, flow) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortDimensions)
                .valueToString(Dimension::getId)
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> getDimensionCacheKey(source, flow), SYNC)
                .build();
    }

    public String getDefaultDimensionsAsString(SdmxConnectionSupplier supplier, Supplier<String> source, Supplier<String> flow, CharSequence delimiter) throws Exception {
        return onDimensions(supplier, source, flow).getValues("").stream().map(o -> ((Dimension) o).getId()).collect(Collectors.joining(delimiter));
    }

    private boolean canLoadFlows(Supplier<String> source) {
        return !Strings.isNullOrEmpty(source.get());
    }

    private List<Dataflow> loadFlows(SdmxConnectionSupplier supplier, Supplier<String> source) throws IOException {
        try (SdmxConnection c = supplier.getConnection(source.get())) {
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

    private String getFlowCacheKey(Supplier<String> source) {
        return source.get();
    }

    private boolean canLoadDimensions(Supplier<String> source, Supplier<String> flow) {
        return canLoadFlows(source) && !Strings.isNullOrEmpty(flow.get());
    }

    private List<Dimension> loadDimensions(SdmxConnectionSupplier supplier, Supplier<String> source, Supplier<String> flow) throws IOException {
        try (SdmxConnection c = supplier.getConnection(source.get())) {
            return new ArrayList<>(c.getDataStructure(DataflowRef.parse(flow.get())).getDimensions());
        }
    }

    private List<Dimension> filterAndSortDimensions(List<Dimension> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getId()) || filter.test(o.getLabel()) || filter.test(String.valueOf(o.getPosition())))
                .sorted(Comparator.comparing(Dimension::getId))
                .collect(Collectors.toList());
    }

    private String getDimensionCacheKey(Supplier<String> source, Supplier<String> flow) {
        return source.get() + "/" + flow.get();
    }
}
