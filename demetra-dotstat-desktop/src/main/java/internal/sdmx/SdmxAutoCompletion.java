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

import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import com.google.common.base.Strings;
import ec.tstoolkit.utilities.GuavaCaches;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.ExtAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import internal.util.DialectLoader;
import nbbrd.desktop.favicon.DomainName;
import nbbrd.desktop.favicon.FaviconRef;
import nbbrd.desktop.favicon.FaviconSupport;
import nbbrd.desktop.favicon.URLConnectionFactory;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import sdmxdl.*;
import sdmxdl.ext.spi.Dialect;
import sdmxdl.web.Network;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import shaded.dotstat.nbbrd.io.WrappedIOException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ec.util.completion.AutoCompletionSource.Behavior.*;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxAutoCompletion {

    public AutoCompletionSource onDialects() {
        return ExtAutoCompletionSource
                .builder(o -> new DialectLoader().get())
                .behavior(AutoCompletionSource.Behavior.SYNC)
                .postProcessor(SdmxAutoCompletion::filterAndSortDialects)
                .valueToString(Dialect::getName)
                .build();
    }

    private List<Dialect> filterAndSortDialects(List<Dialect> allValues, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return allValues.stream()
                .filter(o -> filter.test(o.getDescription()) || filter.test(o.getName()))
                .sorted(Comparator.comparing(Dialect::getDescription))
                .collect(Collectors.toList());
    }

    public ListCellRenderer getDialectRenderer() {
        return CustomListCellRenderer.of(Dialect::getName, Dialect::getDescription);
    }

    public AutoCompletionSource onSources(SdmxWebManager manager) {
        return ExtAutoCompletionSource
                .builder(term -> getAllSources(manager))
                .behavior(AutoCompletionSource.Behavior.SYNC)
                .postProcessor((values, term) -> filterAndSortSources(values, term, manager.getLanguages()))
                .valueToString(SdmxWebSource::getName)
                .build();
    }

    private List<SdmxWebSource> getAllSources(SdmxWebManager manager) {
        return manager
                .getSources()
                .values()
                .stream()
                .filter(source -> !source.isAlias())
                .collect(Collectors.toList());
    }

    public static ImageIcon getDefaultIcon() {
        return ImageUtilities.loadImageIcon("be/nbb/demetra/dotstat/sdmx-logo.png", false);
    }

    public static Icon getFavicon(URL website) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), getDefaultIcon())
                : getDefaultIcon();
    }

    public static Icon getFavicon(URL website, Runnable callback) {
        return website != null
                ? FAVICONS.getOrDefault(FaviconRef.of(DomainName.of(website), 16), callback, getDefaultIcon())
                : getDefaultIcon();
    }

    public static final FaviconSupport FAVICONS = FaviconSupport
            .ofServiceLoader()
            .toBuilder()
            .cache(GuavaCaches.ttlCacheAsMap(Duration.ofHours(1)))
            .client(new ClientOverCustomNetwork())
            .build();

    private static final class ClientOverCustomNetwork implements URLConnectionFactory {

        @Override
        public URLConnection openConnection(URL url) throws IOException {
            Network network = getNetwork();
            Proxy proxy = selectProxy(network, url);
            URLConnection result = network.getURLConnectionFactory().openConnection(url, proxy);
            applyHttps(result, network);
            return result;
        }

        private static void applyHttps(URLConnection result, Network network) {
            if (result instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) result;
                https.setHostnameVerifier(network.getHostnameVerifier());
                https.setSSLSocketFactory(network.getSSLSocketFactory());
            }
        }

        private static Proxy selectProxy(Network network, URL url) throws IOException {
            try {
                return network.getProxySelector().select(url.toURI()).stream().findFirst().orElse(Proxy.NO_PROXY);
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }

        private static Network getNetwork() {
            return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class))
                    .map(provider -> provider.getSdmxManager().getNetwork())
                    .orElseGet(Network::getDefault);
        }
    }

    public ListCellRenderer getSourceRenderer(SdmxWebManager manager) {
        return new CustomListCellRenderer<SdmxWebSource>() {
            @Override
            protected String getValueAsString(SdmxWebSource value) {
                return getNameAndDescription(value, manager.getLanguages());
            }

            @Override
            protected Icon toIcon(String term, JList list, SdmxWebSource value, int index, boolean isSelected, boolean cellHasFocus) {
                return getFavicon(value.getWebsite(), list::repaint);
            }
        };
    }

    private String getNameAndDescription(SdmxWebSource o, LanguagePriorityList langs) {
        return o.getName() + ": " + langs.select(o.getDescriptions());
    }

    public AutoCompletionSource onFlows(SdmxWebManager manager, Supplier<String> source, ConcurrentMap cache) {
        return ExtAutoCompletionSource
                .builder(o -> loadFlows(manager, source))
                .behavior(o -> canLoadFlows(source) ? ASYNC : NONE)
                .postProcessor(SdmxAutoCompletion::filterAndSortFlows)
                .valueToString(o -> o.getRef().toString())
                .cache(cache, o -> getFlowCacheKey(source, manager.getLanguages()), SYNC)
                .build();
    }

    public ListCellRenderer getFlowsRenderer() {
        return CustomListCellRenderer.of(Dataflow::getName, o -> o.getRef().toString());
    }

    public AutoCompletionSource onDimensions(SdmxWebManager manager, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache) {
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

    public String getDefaultDimensionsAsString(SdmxWebManager manager, Supplier<String> source, Supplier<String> flow, ConcurrentMap cache, CharSequence delimiter) throws Exception {
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

    private List<SdmxWebSource> filterAndSortSources(List<SdmxWebSource> allValues, String term, LanguagePriorityList langs) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return allValues
                .stream()
                .filter(source -> filterSource(source, filter, langs))
                .collect(Collectors.toList());
    }

    private static boolean filterSource(SdmxWebSource source, Predicate<String> filter, LanguagePriorityList langs) {
        return filter.test(langs.select(source.getDescriptions()))
                || filter.test(source.getName())
                || source.getAliases().stream().anyMatch(filter);
    }

    private boolean canLoadFlows(Supplier<String> source) {
        return !Strings.isNullOrEmpty(source.get());
    }

    private List<Dataflow> loadFlows(SdmxWebManager manager, Supplier<String> source) throws IOException {
        try (Connection c = manager.getConnection(source.get())) {
            return new ArrayList<>(c.getFlows());
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(ex);
        }
    }

    private List<Dataflow> filterAndSortFlows(List<Dataflow> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getName()) || filter.test(o.getRef().getId()) || filter.test(o.getDescription()))
                .sorted(Comparator.comparing(Dataflow::getName))
                .collect(Collectors.toList());
    }

    private String getFlowCacheKey(Supplier<String> source, LanguagePriorityList languages) {
        return source.get() + languages.toString();
    }

    private boolean canLoadDimensions(Supplier<String> source, Supplier<String> flow) {
        return canLoadFlows(source) && !Strings.isNullOrEmpty(flow.get());
    }

    private List<Dimension> loadDimensions(SdmxWebManager manager, Supplier<String> source, Supplier<String> flow) throws IOException {
        try (Connection c = manager.getConnection(source.get())) {
            return new ArrayList<>(c.getStructure(DataflowRef.parse(flow.get())).getDimensions());
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(ex);
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
