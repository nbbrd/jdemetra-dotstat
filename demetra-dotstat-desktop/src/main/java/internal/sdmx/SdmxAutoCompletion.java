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
import be.nbb.sdmx.facade.parser.spi.SdmxDialect;
import be.nbb.sdmx.facade.web.SdmxWebManager;
import be.nbb.sdmx.facade.web.SdmxWebSource;
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
import be.nbb.sdmx.facade.SdmxManager;

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

    public AutoCompletionSource onSources(SdmxWebManager manager) {
        return ExtAutoCompletionSource
                .builder(o -> manager.getSources())
                .behavior(AutoCompletionSource.Behavior.SYNC)
                .postProcessor(SdmxAutoCompletion::filterAndSortSources)
                .valueToString(SdmxWebSource::getName)
                .build();
    }

    public ListCellRenderer getSourceRenderer() {
        return CustomListCellRenderer.of(SdmxAutoCompletion::getNameAndDescription, o -> null);
    }

    private String getNameAndDescription(SdmxWebSource o) {
        return o.getName() + ": " + o.getDescription();
    }

    public AutoCompletionSource onFlows(SdmxManager manager, Supplier<String> source, ConcurrentMap cache) {
        return ExtAutoCompletionSource
                .builder(o -> loadFlows(manager, source))
                .behavior(o -> canLoadFlows(source) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortFlows)
                .valueToString(o -> o.getRef().toString())
                .cache(cache, o -> getFlowCacheKey(source, manager.getLanguages()), SYNC)
                .build();
    }

    public ListCellRenderer getFlowsRenderer() {
        return CustomListCellRenderer.of(Dataflow::getLabel, o -> o.getRef().toString());
    }

    public AutoCompletionSource onDimensions(SdmxManager manager, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache) {
        return ExtAutoCompletionSource
                .builder(o -> loadDimensions(manager, source, flow))
                .behavior(o -> canLoadDimensions(source, flow) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortDimensions)
                .valueToString(Dimension::getId)
                .cache(cache, o -> getDimensionCacheKey(source, flow, manager.getLanguages()), SYNC)
                .build();
    }

    public ListCellRenderer getDimensionsRenderer() {
        return CustomListCellRenderer.of(Dimension::getId, Dimension::getLabel);
    }

    public String getDefaultDimensionsAsString(SdmxManager manager, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache, CharSequence delimiter) throws Exception {
        String key = getDimensionCacheKey(source, flow, manager.getLanguages());
        List<Dimension> result = (List<Dimension>) cache.get(key);
        if (result == null) {
            result = loadDimensions(manager, source, flow);
            cache.put(key, result);
        }
        return result.stream()
                .sorted(Comparator.comparingInt(Dimension::getPosition))
                .map(Dimension::getId)
                .collect(Collectors.joining(delimiter));
    }

    private List<SdmxWebSource> filterAndSortSources(List<SdmxWebSource> allValues, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        // need to filter out duplicates
        return allValues
                .stream()
                .collect(Collectors.groupingBy(SdmxWebSource::getName))
                .values()
                .stream()
                .flatMap(o -> o.stream().limit(1))
                .filter(o -> filter.test(o.getDescription()) || filter.test(o.getName()))
                .sorted(Comparator.comparing(SdmxWebSource::getDescription))
                .collect(Collectors.toList());
    }

    private boolean canLoadFlows(Supplier<String> source) {
        return !Strings.isNullOrEmpty(source.get());
    }

    private List<Dataflow> loadFlows(SdmxManager manager, Supplier<String> source) throws IOException {
        try (SdmxConnection c = manager.getConnection(source.get())) {
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

    private List<Dimension> loadDimensions(SdmxManager manager, Supplier<String> source, Supplier<String> flow) throws IOException {
        try (SdmxConnection c = manager.getConnection(source.get())) {
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
