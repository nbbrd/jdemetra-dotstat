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
package internal.web.drivers;

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataQuery;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.ResourceRef;
import be.nbb.sdmx.facade.parser.DataFactory;
import be.nbb.sdmx.facade.util.SdmxExceptions;
import static be.nbb.sdmx.facade.util.SdmxMediaType.*;
import static be.nbb.sdmx.facade.xml.stream.SdmxXmlStreams.*;
import ioutil.IO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import internal.util.rest.RestClient;
import internal.util.rest.RestQueryBuilder;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
class AbstractSdmx21 extends AbstractRestClient {

    protected final URL endpoint;
    protected final LanguagePriorityList langs;
    protected final RestClient executor;
    protected final boolean seriesKeysOnlySupported;
    protected final DataFactory dialect;

    @Override
    protected URL getFlowsQuery() throws IOException {
        return getFlowsQuery(endpoint).build();
    }

    @Override
    protected List<Dataflow> getFlows(URL url) throws IOException {
        return flow21(langs)
                .parseStream(calling(url, XML));
    }

    @Override
    protected URL getFlowQuery(DataflowRef ref) throws IOException {
        return getFlowQuery(endpoint, ref).build();
    }

    @Override
    protected Dataflow getFlow(URL url, DataflowRef ref) throws IOException {
        return flow21(langs)
                .parseStream(calling(url, XML))
                .stream()
                .filter(ref::containsRef)
                .findFirst()
                .orElseThrow(() -> SdmxExceptions.missingFlow(ref));
    }

    @Override
    protected URL getStructureQuery(DataStructureRef ref) throws IOException {
        return getStructureQuery(endpoint, ref).build();
    }

    @Override
    protected DataStructure getStructure(URL url, DataStructureRef ref) throws IOException {
        return struct21(langs)
                .parseStream(calling(url, STRUCTURE_21))
                .stream()
                .filter(ref::equalsRef)
                .findFirst()
                .orElseThrow(() -> SdmxExceptions.missingStructure(ref));
    }

    @Override
    protected URL getDataQuery(DataflowRef flowRef, DataQuery query) throws IOException {
        return getDataQuery(endpoint, flowRef, query).build();
    }

    @Override
    protected DataCursor getData(DataStructure dsd, URL url) throws IOException {
        return compactData21(dsd, dialect)
                .parseStream(calling(url, STRUCTURE_SPECIFIC_DATA_21));
    }

    @Override
    public boolean isSeriesKeysOnlySupported() throws IOException {
        return seriesKeysOnlySupported;
    }

    @Override
    public DataStructureRef peekStructureRef(DataflowRef flowRef) throws IOException {
        return null;
    }

    private IO.Supplier<? extends InputStream> calling(URL query, String mediaType) throws IOException {
        return () -> executor.openStream(query, mediaType, langs.toString());
    }

    @Nonnull
    static RestQueryBuilder onMeta(@Nonnull URL endpoint, @Nonnull String resourceType, @Nonnull ResourceRef ref) {
        return RestQueryBuilder.of(endpoint)
                .path(resourceType)
                .path(ref.getAgency())
                .path(ref.getId())
                .path(ref.getVersion());
    }

    @Nonnull
    static RestQueryBuilder onData(@Nonnull URL endpoint, @Nonnull DataflowRef flowRef, @Nonnull Key key) {
        return RestQueryBuilder.of(endpoint)
                .path(DATA_RESOURCE)
                .path(flowRef.toString())
                .path(key.toString())
                .path(DEFAULT_PROVIDER_REF);
    }

    @Nonnull
    static RestQueryBuilder getFlowsQuery(@Nonnull URL endpoint) throws IOException {
        return onMeta(endpoint, DATAFLOW_RESOURCE, FLOWS);
    }

    @Nonnull
    static RestQueryBuilder getFlowQuery(@Nonnull URL endpoint, @Nonnull DataflowRef ref) throws IOException {
        return onMeta(endpoint, DATAFLOW_RESOURCE, ref);
    }

    @Nonnull
    static RestQueryBuilder getStructureQuery(@Nonnull URL endpoint, @Nonnull DataStructureRef ref) throws IOException {
        return onMeta(endpoint, DATASTRUCTURE_RESOURCE, ref).param(REFERENCES_PARAM, "children");
    }

    @Nonnull
    static RestQueryBuilder getDataQuery(@Nonnull URL endpoint, @Nonnull DataflowRef flowRef, @Nonnull DataQuery query) throws IOException {
        RestQueryBuilder result = onData(endpoint, flowRef, query.getKey());
        switch (query.getDetail()) {
            case SERIES_KEYS_ONLY:
                result.param(DETAIL_PARAM, "serieskeysonly");
                break;
        }
        return result;
    }

    static final DataflowRef FLOWS = DataflowRef.of("all", "all", "latest");

    static final String DATAFLOW_RESOURCE = "dataflow";
    static final String DATASTRUCTURE_RESOURCE = "datastructure";
    static final String DATA_RESOURCE = "data";

    static final String DEFAULT_PROVIDER_REF = "all";

    static final String REFERENCES_PARAM = "references";
    static final String DETAIL_PARAM = "detail";
}
