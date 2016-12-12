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

import static be.nbb.sdmx.facade.connectors.SdmxTestResources.ECB_DATA;
import static be.nbb.sdmx.facade.connectors.SdmxTestResources.ECB_DATAFLOWS;
import static be.nbb.sdmx.facade.connectors.SdmxTestResources.ECB_DATA_STRUCTURE;
import static be.nbb.sdmx.facade.connectors.SdmxTestResources.NBB_DATA;
import static be.nbb.sdmx.facade.connectors.SdmxTestResources.NBB_DATAFLOWS;
import static be.nbb.sdmx.facade.connectors.SdmxTestResources.NBB_DATA_STRUCTURE;
import be.nbb.sdmx.facade.FlowRef;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.ResourceRef;
import be.nbb.sdmx.facade.util.MemSdmxConnection;
import be.nbb.sdmx.facade.util.XMLStreamGenericDataCursor20;
import be.nbb.sdmx.facade.util.XMLStreamGenericDataCursor21;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Philippe Charles
 */
public final class TestResource {

    @Nonnull
    public static final MemSdmxConnection nbb() {
        try {
            MemSdmxConnection.Builder result = MemSdmxConnection.builder();
            List<DataStructure> dataStructures;
            try (InputStreamReader r = open(NBB_DATA_STRUCTURE)) {
                dataStructures = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.parse(r))
                        .transform(toDataStructure())
                        .toList();
                result.dataStructures(dataStructures);
            }
            FlowRef flowRef = FlowRef.of("NBB", "TEST_DATASET", null);
            try (InputStreamReader r = open(NBB_DATAFLOWS)) {
                result.dataflows(FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.parse(r))
                        .transform(convertToDataflow())
                        .filter(Predicates.compose(Predicates.equalTo(flowRef), toFlowRef())));
            }
            DataStructure dfs = Iterables.find(dataStructures, Predicates.compose(Predicates.equalTo(ResourceRef.of("NBB", "TEST_DATASET", null)), toResourceRef()));
            try (DataCursor cursor = XMLStreamGenericDataCursor20.genericData20(XMLInputFactory.newInstance(), open(NBB_DATA), dfs)) {
                result.data(flowRef, MemSdmxConnection.copyOf(cursor));
            }
            result.seriesKeysOnlySupported(false);
            return result.build();
        } catch (IOException | XMLStreamException | SdmxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    public static final MemSdmxConnection ecb() {
        try {
            MemSdmxConnection.Builder result = MemSdmxConnection.builder();
            List<DataStructure> dataStructures;
            try (InputStreamReader r = open(ECB_DATA_STRUCTURE)) {
                dataStructures = FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v21.DataStructureParser.parse(r))
                        .transform(toDataStructure())
                        .toList();
                result.dataStructures(dataStructures);
            }
            FlowRef flowRef = FlowRef.of("ECB", "AME", "1.0");
            try (InputStreamReader r = open(ECB_DATAFLOWS)) {
                result.dataflows(FluentIterable
                        .from(it.bancaditalia.oss.sdmx.parser.v21.DataflowParser.parse(r))
                        .transform(toflow())
                        .filter(Predicates.compose(Predicates.equalTo(flowRef), toFlowRef())));
            }
            DataStructure dfs = Iterables.find(dataStructures, Predicates.compose(Predicates.equalTo(ResourceRef.of("ECB", "ECB_AME1", "1.0")), toResourceRef()));
            try (DataCursor cursor = XMLStreamGenericDataCursor21.genericData21(XMLInputFactory.newInstance(), open(ECB_DATA), dfs)) {
                result.data(flowRef, MemSdmxConnection.copyOf(cursor));
            }
            result.seriesKeysOnlySupported(true);
            return result.build();
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
                return Dataflow.of(
                        FlowRef.of(input.getAgency(), input.getId(), input.getVersion()),
                        ResourceRef.of(input.getAgency(), input.getId(), input.getVersion()),
                        input.getName()
                );
            }
        };
    }

    private static Function<Dataflow, FlowRef> toFlowRef() {
        return new Function<Dataflow, FlowRef>() {
            @Override
            public FlowRef apply(Dataflow input) {
                return input.getFlowRef();
            }
        };
    }
    //</editor-fold>
}
