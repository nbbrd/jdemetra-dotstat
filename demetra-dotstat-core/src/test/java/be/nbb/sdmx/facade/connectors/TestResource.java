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

import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.samples.SdmxSource;
import be.nbb.sdmx.facade.repo.SdmxRepository;
import be.nbb.sdmx.facade.xml.stream.SdmxXmlStreams;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.util.LanguagePriorityList;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Philippe Charles
 */
public final class TestResource {

    @Nonnull
    public static final SdmxRepository nbb() {
        try {
            LanguagePriorityList l = LanguagePriorityList.parse("en");
            SdmxRepository.Builder result = SdmxRepository.builder();
            Map<DataStructureRef, DataStructure> dataStructures;
            try (InputStreamReader r = SdmxSource.NBB_DATA_STRUCTURE.openReader()) {
                dataStructures = toDataStructures(new it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser().parse(r, l));
                result.dataStructures(dataStructures.values());
            }
            DataflowRef flowRef = DataflowRef.of("NBB", "TEST_DATASET", null);
            try (InputStreamReader r = SdmxSource.NBB_DATAFLOWS.openReader()) {
                result.dataflows(new it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser().parse(r, l).stream()
                        .map(TestResource::toDataFlow)
                        .filter(o -> o.getFlowRef().equals(flowRef))
                        .collect(Collectors.toList()));
            }
            DataStructure dfs = dataStructures.get(DataStructureRef.of("NBB", "TEST_DATASET", null));
            try (DataCursor cursor = SdmxXmlStreams.genericData20(dfs).get(XMLInputFactory.newInstance(), SdmxSource.NBB_DATA.openReader())) {
                result.copyOf(flowRef, cursor);
            }
            return result
                    .name("NBB")
                    .seriesKeysOnlySupported(false)
                    .build();
        } catch (IOException | XMLStreamException | SdmxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    public static final SdmxRepository ecb() {
        try {
            LanguagePriorityList l = LanguagePriorityList.parse("en");
            SdmxRepository.Builder result = SdmxRepository.builder();
            Map<DataStructureRef, DataStructure> dataStructures;
            try (InputStreamReader r = SdmxSource.ECB_DATA_STRUCTURE.openReader()) {
                dataStructures = toDataStructures(new it.bancaditalia.oss.sdmx.parser.v21.DataStructureParser().parse(r, l));
                result.dataStructures(dataStructures.values());
            }
            DataflowRef flowRef = DataflowRef.of("ECB", "AME", "1.0");
            try (InputStreamReader r = SdmxSource.ECB_DATAFLOWS.openReader()) {
                result.dataflows(new it.bancaditalia.oss.sdmx.parser.v21.DataflowParser().parse(r, l).stream()
                        .map(Util::toDataflow)
                        .filter(o -> o.getFlowRef().equals(flowRef))
                        .collect(Collectors.toList()));
            }
            DataStructure dfs = dataStructures.get(DataStructureRef.of("ECB", "ECB_AME1", "1.0"));
            try (DataCursor cursor = SdmxXmlStreams.genericData21(dfs).get(XMLInputFactory.newInstance(), SdmxSource.ECB_DATA.openReader())) {
                result.copyOf(flowRef, cursor);
            }
            return result
                    .name("ECB")
                    .seriesKeysOnlySupported(true)
                    .build();
        } catch (IOException | XMLStreamException | SdmxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<DataStructureRef, DataStructure> toDataStructures(List<DataFlowStructure> input) {
        return input.stream()
                .map(Util::toDataStructure)
                .collect(Collectors.toMap(DataStructure::getRef, Function.identity()));
    }

    private static Dataflow toDataFlow(DataFlowStructure input) {
        return Dataflow.of(DataflowRef.of(input.getAgency(), input.getId(), input.getVersion()),
                DataStructureRef.of(input.getAgency(), input.getId(), input.getVersion()),
                input.getName()
        );
    }
}
