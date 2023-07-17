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

import be.nbb.demetra.sdmx.HasSdmxProperties;
import be.nbb.demetra.sdmx.file.SdmxFileBean;
import be.nbb.demetra.sdmx.file.SdmxFileProvider;
import be.nbb.demetra.sdmx.web.SdmxWebBean;
import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.ExtAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import lombok.NonNull;
import sdmxdl.*;
import sdmxdl.file.SdmxFileSource;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static ec.util.completion.AutoCompletionSource.Behavior.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Philippe Charles
 */
public abstract class SdmxAutoCompletion {

    public abstract @NonNull AutoCompletionSource getSource();

    public abstract @NonNull ListCellRenderer<?> getRenderer();

    public static @NonNull SdmxAutoCompletion onWebSource(@NonNull SdmxWebProvider provider) {
        return new WebSourceCompletion(provider);
    }

    public static @NonNull SdmxAutoCompletion onDataflow(@NonNull SdmxWebProvider provider, @NonNull SdmxWebBean bean, @NonNull ConcurrentMap<Object, Object> cache) {
        return new DataflowCompletion<>(provider, () -> getWebSourceOrNull(bean, provider), cache);
    }

    public static @NonNull SdmxAutoCompletion onDimension(@NonNull SdmxWebProvider provider, @NonNull SdmxWebBean bean, @NonNull ConcurrentMap<Object, Object> cache) {
        return new DimensionCompletion<>(provider, () -> getWebSourceOrNull(bean, provider), () -> getDataflowRefOrNull(bean), cache);
    }

    public static @NonNull SdmxAutoCompletion onDimension(@NonNull SdmxFileProvider provider, @NonNull SdmxFileBean bean, @NonNull ConcurrentMap<Object, Object> cache) {
        return new DimensionCompletion<>(provider, () -> getFileSource(bean, provider).orElse(null), () -> getFileSource(bean, provider).map(SdmxFileSource::asDataflowRef).orElse(null), cache);
    }

    public static @NonNull SdmxAutoCompletion onAttribute(@NonNull SdmxWebProvider provider, @NonNull SdmxWebBean bean, @NonNull ConcurrentMap<Object, Object> cache) {
        return new AttributeCompletion<>(provider, () -> getWebSourceOrNull(bean, provider), () -> getDataflowRefOrNull(bean), cache);
    }

    public static @NonNull SdmxAutoCompletion onAttribute(@NonNull SdmxFileProvider provider, @NonNull SdmxFileBean bean, @NonNull ConcurrentMap<Object, Object> cache) {
        return new AttributeCompletion<>(provider, () -> getFileSource(bean, provider).orElse(null), () -> getFileSource(bean, provider).map(SdmxFileSource::asDataflowRef).orElse(null), cache);
    }

    @lombok.AllArgsConstructor
    private static final class WebSourceCompletion extends SdmxAutoCompletion {

        private final @NonNull HasSdmxProperties<SdmxWebManager> provider;

        @Override
        public @NonNull AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(SYNC)
                    .postProcessor(this::filterAndSort)
                    .valueToString(SdmxWebSource::getId)
                    .build();
        }

        @Override
        public @NonNull ListCellRenderer<?> getRenderer() {
            return new CustomListCellRenderer<SdmxWebSource>() {
                @Override
                protected @NonNull String getValueAsString(@NonNull SdmxWebSource value) {
                    return value.getId() + ": " + provider.getLanguages().select(value.getNames());
                }

                @Override
                protected Icon toIcon(String term, JList list, SdmxWebSource value, int index, boolean isSelected, boolean cellHasFocus) {
                    return SdmxIcons.getFavicon(provider.getSdmxManager().getNetworking(), value.getWebsite(), list::repaint);
                }
            };
        }

        private List<SdmxWebSource> load(String term) {
            return provider
                    .getSdmxManager()
                    .getSources()
                    .values()
                    .stream()
                    .filter(source -> !source.isAlias())
                    .collect(toList());
        }

        private List<SdmxWebSource> filterAndSort(List<SdmxWebSource> list, String term) {
            return list.stream().filter(getFilter(term)).collect(toList());
        }

        private Predicate<SdmxWebSource> getFilter(String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return value -> filter.test(provider.getLanguages().select(value.getNames()))
                    || filter.test(value.getId())
                    || value.getAliases().stream().anyMatch(filter);
        }
    }

    @lombok.AllArgsConstructor
    private static final class DataflowCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        private final @NonNull HasSdmxProperties<? extends SdmxManager<S>> provider;

        private final @NonNull Supplier<S> source;

        private final @NonNull ConcurrentMap<Object, Object> cache;

        @Override
        public @NonNull AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(o -> o.getRef().toString())
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public @NonNull ListCellRenderer<?> getRenderer() {
            return CustomListCellRenderer.<Dataflow>of(flow -> flow.getRef() + "<br><i>" + flow.getName(), flow -> flow.getRef().toString());
        }

        private List<Dataflow> load(String term) throws Exception {
            try (Connection c = provider.getSdmxManager().getConnection(source.get(), provider.getLanguages())) {
                return new ArrayList<>(c.getFlows());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null ? ASYNC : NONE;
        }

        private List<Dataflow> filterAndSort(List<Dataflow> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getName()) || filter.test(o.getRef().getId()) || filter.test(o.getDescription()))
                    .sorted(Comparator.comparing(Dataflow::getName))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Dataflow" + source.get() + provider.getLanguages();
        }
    }

    @lombok.AllArgsConstructor
    private static final class DimensionCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        private final @NonNull HasSdmxProperties<? extends SdmxManager<S>> provider;

        private final @NonNull Supplier<S> source;

        private final @NonNull Supplier<DataflowRef> flowRef;

        private final @NonNull ConcurrentMap<Object, Object> cache;

        @Override
        public @NonNull AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(Dimension::getId)
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public @NonNull ListCellRenderer<?> getRenderer() {
            return CustomListCellRenderer.of(Dimension::getId, Dimension::getName);
        }

        private List<Dimension> load(String term) throws Exception {
            try (Connection c = provider.getSdmxManager().getConnection(source.get(), provider.getLanguages())) {
                return new ArrayList<>(c.getStructure(flowRef.get()).getDimensions());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null && flowRef.get() != null ? ASYNC : NONE;
        }

        private List<Dimension> filterAndSort(List<Dimension> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getId()) || filter.test(o.getName()) || filter.test(String.valueOf(o.getPosition())))
                    .sorted(Comparator.comparing(Dimension::getId))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Dimension" + source.get() + flowRef.get() + provider.getLanguages();
        }
    }

    @lombok.AllArgsConstructor
    private static final class AttributeCompletion<S extends SdmxSource> extends SdmxAutoCompletion {

        private final @NonNull HasSdmxProperties<? extends SdmxManager<S>> provider;

        private final @NonNull Supplier<S> source;

        private final @NonNull Supplier<DataflowRef> flowRef;

        private final @NonNull ConcurrentMap<Object, Object> cache;

        @Override
        public @NonNull AutoCompletionSource getSource() {
            return ExtAutoCompletionSource
                    .builder(this::load)
                    .behavior(this::getBehavior)
                    .postProcessor(this::filterAndSort)
                    .valueToString(Attribute::getId)
                    .cache(cache, this::getCacheKey, SYNC)
                    .build();
        }

        @Override
        public @NonNull ListCellRenderer<?> getRenderer() {
            return CustomListCellRenderer.of(Attribute::getId, Attribute::getName);
        }

        private List<Attribute> load(String term) throws Exception {
            try (Connection c = provider.getSdmxManager().getConnection(source.get(), provider.getLanguages())) {
                return new ArrayList<>(c.getStructure(flowRef.get()).getAttributes());
            }
        }

        private AutoCompletionSource.Behavior getBehavior(String term) {
            return source.get() != null && flowRef.get() != null ? ASYNC : NONE;
        }

        private List<Attribute> filterAndSort(List<Attribute> values, String term) {
            Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
            return values.stream()
                    .filter(o -> filter.test(o.getId()) || filter.test(o.getName()))
                    .sorted(Comparator.comparing(Attribute::getId))
                    .collect(toList());
        }

        private String getCacheKey(String term) {
            return "Attribute" + source.get() + flowRef.get() + provider.getLanguages();
        }
    }

    private static SdmxWebSource getWebSourceOrNull(SdmxWebBean bean, SdmxWebProvider provider) {
        return provider.getSdmxManager().getSources().get(bean.getSource());
    }

    private static DataflowRef getDataflowRefOrNull(SdmxWebBean bean) {
        try {
            return DataflowRef.parse(bean.getFlow());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Optional<SdmxFileSource> getFileSource(SdmxFileBean bean, SdmxFileProvider provider) {
        try {
            return Optional.of(SdmxCubeItems.resolveFileSet(provider, bean));
        } catch (FileNotFoundException ex) {
            return Optional.empty();
        }
    }
}
