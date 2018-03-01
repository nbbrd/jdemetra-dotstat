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
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.parser.spi.SdmxDialect;
import be.nbb.sdmx.facade.web.SdmxWebManager;
import be.nbb.sdmx.facade.web.SdmxWebEntryPoint;
import be.nbb.sdmx.facade.util.UnexpectedIOException;
import com.google.common.base.Strings;
import ec.util.completion.AutoCompletionSource;
import static ec.util.completion.AutoCompletionSource.Behavior.ASYNC;
import static ec.util.completion.AutoCompletionSource.Behavior.NONE;
import static ec.util.completion.AutoCompletionSource.Behavior.SYNC;
import ec.util.completion.ExtAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.ListCellRenderer;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxAutoCompletion {

    public AutoCompletionSource onDialects() {
        return ExtAutoCompletionSource
                .builder(o -> StreamSupport.stream(ServiceLoader.load(SdmxDialect.class).spliterator(), false).collect(Collectors.toList()))
                .behavior(AutoCompletionSource.Behavior.SYNC)
                .postProcessor(SdmxAutoCompletion::filterAndSortDialects)
                .valueToString(SdmxDialect::getName)
                .build();
    }

    private List<SdmxDialect> filterAndSortDialects(List<SdmxDialect> allValues, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return allValues.stream()
                .filter(o -> filter.test(o.getDescription()) || filter.test(o.getName()))
                .sorted(Comparator.comparing(SdmxDialect::getDescription))
                .collect(Collectors.toList());
    }

    public ListCellRenderer getDialectRenderer() {
        return CustomListCellRenderer.of(SdmxDialect::getName, SdmxDialect::getDescription);
    }
    
    public AutoCompletionSource onEntryPoints(SdmxWebManager manager) {
        return ExtAutoCompletionSource
                .builder(o -> manager.getEntryPoints())
                .behavior(AutoCompletionSource.Behavior.SYNC)
                .postProcessor(SdmxAutoCompletion::filterAndSortEntryPoints)
                .valueToString(SdmxWebEntryPoint::getName)
                .build();
    }

    public ListCellRenderer getEntryPointsRenderer() {
        return CustomListCellRenderer.of(SdmxWebEntryPoint::getDescription, SdmxWebEntryPoint::getName);
    }

    public AutoCompletionSource onFlows(SdmxConnectionSupplier supplier, LanguagePriorityList languages, Supplier<String> source, ConcurrentMap cache) {
        return ExtAutoCompletionSource
                .builder(o -> loadFlows(supplier, languages, source))
                .behavior(o -> canLoadFlows(source) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortFlows)
                .valueToString(o -> o.getRef().toString())
                .cache(cache, o -> getFlowCacheKey(source, languages), SYNC)
                .build();
    }

    public ListCellRenderer getFlowsRenderer() {
        return CustomListCellRenderer.of(Dataflow::getLabel, o -> o.getRef().toString());
    }

    public AutoCompletionSource onDimensions(SdmxConnectionSupplier supplier, LanguagePriorityList languages, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache) {
        return ExtAutoCompletionSource
                .builder(o -> loadDimensions(supplier, languages, source, flow))
                .behavior(o -> canLoadDimensions(source, flow) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortDimensions)
                .valueToString(Dimension::getId)
                .cache(cache, o -> getDimensionCacheKey(source, flow, languages), SYNC)
                .build();
    }

    public ListCellRenderer getDimensionsRenderer() {
        return CustomListCellRenderer.of(Dimension::getId, Dimension::getLabel);
    }

    public String getDefaultDimensionsAsString(SdmxConnectionSupplier supplier, LanguagePriorityList languages, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache, CharSequence delimiter) throws Exception {
        String key = getDimensionCacheKey(source, flow, languages);
        List<Dimension> result = (List<Dimension>) cache.get(key);
        if (result == null) {
            result = loadDimensions(supplier, languages, source, flow);
            cache.put(key, result);
        }
        return result.stream()
                .sorted(Comparator.comparingInt(Dimension::getPosition))
                .map(Dimension::getId)
                .collect(Collectors.joining(delimiter));
    }

    private List<SdmxWebEntryPoint> filterAndSortEntryPoints(List<SdmxWebEntryPoint> allValues, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return allValues.stream()
                .filter(o -> filter.test(o.getDescription()) || filter.test(o.getUri().toString()))
                .sorted(Comparator.comparing(SdmxWebEntryPoint::getDescription))
                .collect(Collectors.toList());
    }

    private boolean canLoadFlows(Supplier<String> source) {
        return !Strings.isNullOrEmpty(source.get());
    }

    private List<Dataflow> loadFlows(SdmxConnectionSupplier supplier, LanguagePriorityList languages, Supplier<String> source) throws IOException {
        try (SdmxConnection c = supplier.getConnection(source.get(), languages)) {
            return new ArrayList<>(c.getFlows());
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    private List<Dataflow> filterAndSortFlows(List<Dataflow> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getLabel()) || filter.test(o.getRef().getId()))
                .sorted(Comparator.comparing(Dataflow::getLabel))
                .collect(Collectors.toList());
    }

    private String getFlowCacheKey(Supplier<String> source, LanguagePriorityList languages) {
        return source.get() + languages.toString();
    }

    private boolean canLoadDimensions(Supplier<String> source, Supplier<String> flow) {
        return canLoadFlows(source) && !Strings.isNullOrEmpty(flow.get());
    }

    private List<Dimension> loadDimensions(SdmxConnectionSupplier supplier, LanguagePriorityList languages, Supplier<String> source, Supplier<String> flow) throws IOException {
        try (SdmxConnection c = supplier.getConnection(source.get(), languages)) {
            return new ArrayList<>(c.getStructure(DataflowRef.parse(flow.get())).getDimensions());
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    private List<Dimension> filterAndSortDimensions(List<Dimension> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getId()) || filter.test(o.getLabel()) || filter.test(String.valueOf(o.getPosition())))
                .sorted(Comparator.comparing(Dimension::getId))
                .collect(Collectors.toList());
    }

    private String getDimensionCacheKey(Supplier<String> source, Supplier<String> flow, LanguagePriorityList languages) {
        return source.get() + "/" + flow.get() + languages.toString();
    }
}
