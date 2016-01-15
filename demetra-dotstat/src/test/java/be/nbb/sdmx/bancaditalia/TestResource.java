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
package be.nbb.sdmx.bancaditalia;

import static be.nbb.sdmx.SdmxTestResources.ECB_DATA;
import static be.nbb.sdmx.SdmxTestResources.ECB_DATAFLOWS;
import static be.nbb.sdmx.SdmxTestResources.ECB_DATA_STRUCTURE;
import static be.nbb.sdmx.SdmxTestResources.NBB_DATA;
import static be.nbb.sdmx.SdmxTestResources.NBB_DATAFLOWS;
import static be.nbb.sdmx.SdmxTestResources.NBB_DATA_STRUCTURE;
import be.nbb.sdmx.FlowRef;
import be.nbb.sdmx.Key;
import be.nbb.sdmx.SdmxConnection;
import be.nbb.sdmx.DataCursor;
import be.nbb.sdmx.DataStructure;
import be.nbb.sdmx.Dataflow;
import be.nbb.sdmx.ResourceRef;
import be.nbb.sdmx.util.SdmxParser;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.io.ByteSource;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.util.SdmxException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Philippe Charles
 */
public final class TestResource {

    Set<Dataflow> dataflows;
    Map<ResourceRef, DataStructure> dataStructureByRef;
    Callable<DataCursor> dataSupplier;
    boolean seriesKeysOnlySupported;

    @Nonnull
    public SdmxConnection AsConnection() {
        return new FakeConnection(this);
    }

    @Nonnull
    public static final TestResource nbb() {
        try {
            TestResource result = new TestResource();
            try (InputStreamReader r = open(NBB_DATAFLOWS)) {
                result.dataflows = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.parse(r))
                        .transform(convertToDataflow())
                        .toSet();
            }
            try (InputStreamReader r = open(NBB_DATA_STRUCTURE)) {
                result.dataStructureByRef = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.parse(r))
                        .transform(toDataStructure())
                        .uniqueIndex(toResourceRef());
            }
            final DataStructure dfs = result.dataStructureByRef.get(new ResourceRef("NBB", "TEST_DATASET", null));
            result.dataSupplier = new Callable<DataCursor>() {
                @Override
                public DataCursor call() throws Exception {
                    return SdmxParser.getDefault().genericData20(open(NBB_DATA), dfs);
                }
            };
            result.seriesKeysOnlySupported = false;
            return result;
        } catch (IOException | XMLStreamException | SdmxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    public static final TestResource ecb() {
        try {
            TestResource result = new TestResource();
            try (InputStreamReader r = open(ECB_DATAFLOWS)) {
                result.dataflows = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v21.DataflowParser.parse(r))
                        .transform(toflow())
                        .toSet();
            }
            try (InputStreamReader r = open(ECB_DATA_STRUCTURE)) {
                result.dataStructureByRef = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v21.DataStructureParser.parse(r))
                        .transform(toDataStructure())
                        .uniqueIndex(toResourceRef());
            }
            final DataStructure dfs = result.dataStructureByRef.get(new ResourceRef("ECB", "ECB_AME1", "1.0"));
            result.dataSupplier = new Callable<DataCursor>() {
                @Override
                public DataCursor call() throws Exception {
                    return SdmxParser.getDefault().genericData21(open(ECB_DATA), dfs);
                }
            };
            result.seriesKeysOnlySupported = true;
            return result;
        } catch (IOException | XMLStreamException | SdmxException ex) {
            throw new RuntimeException(ex);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static InputStreamReader open(ByteSource byteSource) throws IOException {
        return new InputStreamReader(byteSource.openStream(), StandardCharsets.UTF_8);
    }

    private static Function<it.bancaditalia.oss.sdmx.api.Dataflow, Dataflow> toflow() {
        return new Function<it.bancaditalia.oss.sdmx.api.Dataflow, Dataflow>() {
            @Override
            public Dataflow apply(it.bancaditalia.oss.sdmx.api.Dataflow input) {
                return Util.toDataflow(input);
            }
        };
    }

    private static Function<it.bancaditalia.oss.sdmx.api.DataFlowStructure, DataStructure> toDataStructure() {
        return new Function<it.bancaditalia.oss.sdmx.api.DataFlowStructure, DataStructure>() {
            @Override
            public DataStructure apply(it.bancaditalia.oss.sdmx.api.DataFlowStructure input) {
                return Util.toDataStructure(input);
            }
        };
    }

    private static Function<DataStructure, ResourceRef> toResourceRef() {
        return new Function<DataStructure, ResourceRef>() {
            @Override
            public ResourceRef apply(DataStructure input) {
                return input.getDataStructureRef();
            }
        };
    }

    private static Function<DataFlowStructure, Dataflow> convertToDataflow() {
        return new Function<DataFlowStructure, Dataflow>() {
            @Override
            public Dataflow apply(DataFlowStructure input) {
                return new Dataflow(
                        FlowRef.valueOf(input.getAgency(), input.getId(), input.getVersion()),
                        new ResourceRef(input.getAgency(), input.getId(), input.getVersion()),
                        input.getName()
                );
            }
        };
    }

    private static final class FakeConnection extends SdmxConnection {

        private final TestResource resource;

        public FakeConnection(TestResource resource) {
            this.resource = resource;
        }

        @Override
        public Set<Dataflow> getDataflows() throws IOException {
            return resource.dataflows;
        }

        @Override
        public Dataflow getDataflow(FlowRef flowRef) throws IOException {
            for (Dataflow o : resource.dataflows) {
                if (o.getFlowRef().equals(flowRef)) {
                    return o;
                }
            }
            return null;
        }

        @Override
        public DataStructure getDataStructure(FlowRef flowRef) throws IOException {
            Dataflow dataflow = getDataflow(flowRef);
            if (dataflow != null) {
                DataStructure result = resource.dataStructureByRef.get(dataflow.getDataStructureRef());
                if (result != null) {
                    return result;
                }
            }
            throw new IOException("Not found");
        }

        @Override
        public DataCursor getData(FlowRef flowRef, Key key, boolean serieskeysonly) throws IOException {
            getDataStructure(flowRef);
            try {
                return resource.dataSupplier.call();
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public boolean isSeriesKeysOnlySupported() {
            return resource.seriesKeysOnlySupported;
        }
    }
    //</editor-fold>
}
