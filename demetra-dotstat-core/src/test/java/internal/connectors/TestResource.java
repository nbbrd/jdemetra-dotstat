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
package internal.connectors;

import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.samples.SdmxSource;
import be.nbb.sdmx.facade.repo.SdmxRepository;
import be.nbb.sdmx.facade.samples.ByteSource;
import be.nbb.sdmx.facade.xml.stream.SdmxXmlStreams;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.client.Parser;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.util.LanguagePriorityList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLEventReader;
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
            Map<DataStructureRef, DataStructure> dataStructures = toDataStructures(parse(SdmxSource.NBB_DATA_STRUCTURE, l, new it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser()));
            result.dataStructures(dataStructures.values());
            DataflowRef flowRef = DataflowRef.of("NBB", "TEST_DATASET", null);
            result.dataflows(parse(SdmxSource.NBB_DATAFLOWS, l, new it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser())
                    .stream()
                    .map(TestResource::toDataFlow)
                    .filter(o -> o.getRef().equals(flowRef))
                    .collect(Collectors.toList()));
            DataStructure dsd = dataStructures.get(DataStructureRef.of("NBB", "TEST_DATASET", null));
            try (DataCursor cursor = SdmxXmlStreams.genericData20(dsd).get(SdmxSource.XIF, SdmxSource.NBB_DATA.openReader())) {
                result.copyOf(flowRef, cursor);
            }
            return result
                    .name("NBB")
                    .seriesKeysOnlySupported(false)
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    public static final SdmxRepository ecb() {
        try {
            LanguagePriorityList l = LanguagePriorityList.parse("en");
            SdmxRepository.Builder result = SdmxRepository.builder();
            Map<DataStructureRef, DataStructure> dataStructures = toDataStructures(parse(SdmxSource.ECB_DATA_STRUCTURE, l, new it.bancaditalia.oss.sdmx.parser.v21.DataStructureParser()));
            result.dataStructures(dataStructures.values());
            DataflowRef flowRef = DataflowRef.of("ECB", "AME", "1.0");
            result.dataflows(parse(SdmxSource.ECB_DATAFLOWS, l, new it.bancaditalia.oss.sdmx.parser.v21.DataflowParser()).stream()
                    .map(Util::toFlow)
                    .filter(o -> o.getRef().equals(flowRef))
                    .collect(Collectors.toList()));
            DataStructure dfs = dataStructures.get(DataStructureRef.of("ECB", "ECB_AME1", "1.0"));
            try (DataCursor cursor = SdmxXmlStreams.genericData21(dfs).get(SdmxSource.XIF, SdmxSource.ECB_DATA.openReader())) {
                result.copyOf(flowRef, cursor);
            }
            return result
                    .name("ECB")
                    .seriesKeysOnlySupported(true)
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<DataStructureRef, DataStructure> toDataStructures(List<DataFlowStructure> input) {
        return input.stream()
                .map(Util::toStructure)
                .collect(Collectors.toMap(DataStructure::getRef, Function.identity()));
    }

    private static Dataflow toDataFlow(DataFlowStructure input) {
        return Dataflow.of(DataflowRef.of(input.getAgency(), input.getId(), input.getVersion()),
                DataStructureRef.of(input.getAgency(), input.getId(), input.getVersion()),
                input.getName()
        );
    }

    private static <T> T parse(ByteSource xml, LanguagePriorityList l, Parser<T> parser) throws IOException {
        XMLEventReader r = null;
        try {
            r = xml.openXmlEvent(SdmxSource.XIF);
            return parser.parse(r, l);
        } catch (XMLStreamException | SdmxException ex) {
            throw new IOException(ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (XMLStreamException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}
