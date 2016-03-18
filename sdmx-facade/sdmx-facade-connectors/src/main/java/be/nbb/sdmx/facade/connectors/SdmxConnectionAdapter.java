/*
 * Copyright 2015 National Bank of Belgium
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
package be.nbb.sdmx.facade.connectors;

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.FlowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnection;
import com.google.common.collect.ImmutableSet;
import it.bancaditalia.oss.sdmx.api.DSDIdentifier;
import it.bancaditalia.oss.sdmx.api.GenericSDMXClient;
import it.bancaditalia.oss.sdmx.client.custom.DotStat;
import it.bancaditalia.oss.sdmx.util.SdmxException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
class SdmxConnectionAdapter extends SdmxConnection {

    private final GenericSDMXClient client;

    public SdmxConnectionAdapter(GenericSDMXClient client) {
        this.client = client;
    }

    @Override
    final public Set<Dataflow> getDataflows() throws IOException {
        ImmutableSet.Builder<Dataflow> result = ImmutableSet.builder();
        for (it.bancaditalia.oss.sdmx.api.Dataflow o : loadDataFlows().values()) {
            result.add(Util.toDataflow(o));
        }
        return result.build();
    }

    @Override
    final public Dataflow getDataflow(FlowRef flowRef) throws IOException {
        return Util.toDataflow(loadDataflow(flowRef));
    }

    @Override
    final public DataStructure getDataStructure(FlowRef flowRef) throws IOException {
        return Util.toDataStructure(loadDataStructure(flowRef));
    }

    @Override
    final public DataCursor getData(FlowRef flowRef, Key key, boolean serieskeysonly) throws IOException {
        return loadData(flowRef, key, serieskeysonly);
    }

    @Override
    public boolean isSeriesKeysOnlySupported() {
        return client instanceof RestSdmxClientWithCursor
                && ((RestSdmxClientWithCursor) client).getConfig().isSeriesKeysOnlySupported();
    }

    @Nonnull
    protected Map<String, it.bancaditalia.oss.sdmx.api.Dataflow> loadDataFlows() throws IOException {
        try {
            return client.getDataflows();
        } catch (SdmxException ex) {
            throw new IOException("While getting dataflows", ex);
        }
    }

    @Nonnull
    protected it.bancaditalia.oss.sdmx.api.Dataflow loadDataflow(FlowRef flowRef) throws IOException {
        try {
            return client.getDataflow(flowRef.getFlowId(), flowRef.getAgencyId(), flowRef.getVersion());
        } catch (SdmxException ex) {
            throw new IOException("While getting dataflow '" + flowRef + "'", ex);
        }
    }

    @Nonnull
    protected it.bancaditalia.oss.sdmx.api.DataFlowStructure loadDataStructure(FlowRef flowRef) throws IOException {
        try {
            it.bancaditalia.oss.sdmx.api.DSDIdentifier dsd = client instanceof DotStat
                    ? new DSDIdentifier(flowRef.getFlowId(), flowRef.getAgencyId(), flowRef.getVersion())
                    : loadDataflow(flowRef).getDsdIdentifier();
            return client.getDataFlowStructure(dsd, true);
        } catch (SdmxException ex) {
            throw new IOException("While getting datastructure for '" + flowRef + "'", ex);
        }
    }

    @Nonnull
    protected DataCursor loadData(FlowRef flowRef, Key key, boolean serieskeysonly) throws IOException {
        if (serieskeysonly && !isSeriesKeysOnlySupported()) {
            throw new IllegalStateException("serieskeysonly not supported");
        }
        it.bancaditalia.oss.sdmx.api.Dataflow dataflow = loadDataflow(flowRef);
        it.bancaditalia.oss.sdmx.api.DataFlowStructure dfs = loadDataStructure(flowRef);
        try {
            return client instanceof RestSdmxClientWithCursor
                    ? ((RestSdmxClientWithCursor) client).getDataCursor(dataflow, dfs, key, serieskeysonly)
                    : new DataCursorAdapter(client.getTimeSeries(dataflow, dfs, key.toString(), null, null, serieskeysonly, null, false));
        } catch (SdmxException ex) {
            if (isNoResultMatchingQuery(ex)) {
                return DataCursor.noOp();
            }
            throw new IOException("While getting data for '" + flowRef + "' at '" + key + "'", ex);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static boolean isNoResultMatchingQuery(SdmxException ex) {
        return ex.getMessage().contains("SDMX meaning: No results matching the query.");
    }
    //</editor-fold>
}
