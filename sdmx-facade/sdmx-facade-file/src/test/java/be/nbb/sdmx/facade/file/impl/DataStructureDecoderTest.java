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
package be.nbb.sdmx.facade.file.impl;

import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import org.junit.Test;
import be.nbb.sdmx.facade.file.SdmxDecoder;
import be.nbb.sdmx.facade.samples.SdmxSource;
import static org.assertj.core.api.Assertions.assertThat;
import static be.nbb.sdmx.facade.file.impl.CustomDataStructureBuilder.dimension;
import java.io.Reader;

/**
 *
 * @author Philippe Charles
 */
public class DataStructureDecoderTest {

    private final XMLInputFactory factory = XMLInputFactory.newInstance();

    @Test
    public void testDecodeGeneric20() throws IOException {
        DataStructure ds = DataStructure.builder()
                .ref(DataStructureRef.of(null, "BIS_JOINT_DEBT", null))
                .dimension(dimension("FREQ", 1, "A", "M"))
                .dimension(dimension("JD_TYPE", 2, "P"))
                .dimension(dimension("JD_CATEGORY", 3, "A"))
                .dimension(dimension("VIS_CTY", 4, "MX"))
                .label("BIS_JOINT_DEBT")
                .timeDimensionId("TIME_PERIOD")
                .primaryMeasureId("OBS_VALUE")
                .build();

        try (Reader stream = SdmxSource.OTHER_GENERIC20.openReader()) {
            assertThat(DataStructureDecoder.decodeDataStructure(SdmxDecoder.DataType.GENERIC20, factory, stream)).isEqualTo(ds);
        }
    }

    @Test
    public void testDecodeCompact20() throws IOException {
        DataStructure ds = DataStructure.builder()
                .ref(DataStructureRef.of(null, "TODO", null))
                .dimension(dimension("FREQ", 1, "A", "M"))
                .dimension(dimension("COLLECTION", 2, "B"))
                .dimension(dimension("VIS_CTY", 3, "MX"))
                .dimension(dimension("JD_TYPE", 4, "P"))
                .dimension(dimension("JD_CATEGORY", 5, "A", "B"))
                .label("TODO")
                .timeDimensionId("TIME_PERIOD")
                .primaryMeasureId("OBS_VALUE")
                .build();

        try (Reader stream = SdmxSource.OTHER_COMPACT20.openReader()) {
            assertThat(DataStructureDecoder.decodeDataStructure(SdmxDecoder.DataType.COMPACT20, factory, stream)).isEqualTo(ds);
        }
    }

    @Test
    public void testDecodeGeneric21() throws IOException {
        DataStructure ds = DataStructure.builder()
                .ref(DataStructureRef.of(null, "ECB_AME1", null))
                .dimension(dimension("FREQ", 1, "A"))
                .dimension(dimension("AME_REF_AREA", 2, "BEL"))
                .dimension(dimension("AME_TRANSFORMATION", 3, "1"))
                .dimension(dimension("AME_AGG_METHOD", 4, "0"))
                .dimension(dimension("AME_UNIT", 5, "0"))
                .dimension(dimension("AME_REFERENCE", 6, "0"))
                .dimension(dimension("AME_ITEM", 7, "OVGD"))
                .label("ECB_AME1")
                .timeDimensionId("TIME_PERIOD")
                .primaryMeasureId("OBS_VALUE")
                .build();

        try (Reader stream = SdmxSource.OTHER_GENERIC21.openReader()) {
            assertThat(DataStructureDecoder.decodeDataStructure(SdmxDecoder.DataType.GENERIC21, factory, stream)).isEqualTo(ds);
        }
    }

    @Test
    public void testDecodeCompact21() throws IOException {
        DataStructure ds = DataStructure.builder()
                .ref(DataStructureRef.of(null, "ECB_AME1", null))
                .dimension(dimension("FREQ", 1, "A"))
                .dimension(dimension("AME_REF_AREA", 2, "BEL"))
                .dimension(dimension("AME_TRANSFORMATION", 3, "1"))
                .dimension(dimension("AME_AGG_METHOD", 4, "0"))
                .dimension(dimension("AME_UNIT", 5, "0"))
                .dimension(dimension("AME_REFERENCE", 6, "0"))
                .dimension(dimension("AME_ITEM", 7, "OVGD"))
                .label("ECB_AME1")
                .timeDimensionId("TIME_PERIOD")
                .primaryMeasureId("OBS_VALUE")
                .build();

        try (Reader stream = SdmxSource.OTHER_COMPACT21.openReader()) {
            assertThat(DataStructureDecoder.decodeDataStructure(SdmxDecoder.DataType.COMPACT21, factory, stream)).isEqualTo(ds);
        }
    }
}
