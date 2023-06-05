/*
 * Copyright 2016 National Bank of Belgium
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
package be.nbb.demetra.sdmx.file;

import ec.tss.TsCollectionInformation;
import ec.tss.TsInformation;
import ec.tss.TsInformationType;
import ec.tss.TsMoniker;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tests.sdmxdl.api.ByteSource;
import tests.sdmxdl.format.xml.SdmxXmlSources;

/**
 *
 * @author Philippe Charles
 */
public class SdmxFileProviderTest {

    private static File NO_XML;
    private static File BLANK;
    private static File GENERIC20;
    private static File STRUCT20;

    private static File createTemp(ByteSource bytes, String prefix, String suffix) throws IOException {
        File result = File.createTempFile(prefix, suffix);
        result.deleteOnExit();
        bytes.copyTo(result);
        return result;
    }

    @BeforeAll
    public static void beforeClass() throws IOException {
        NO_XML = File.createTempFile("sdmx_empty", ".xml");
        NO_XML.deleteOnExit();
        BLANK = new File("");
        GENERIC20 = createTemp(SdmxXmlSources.NBB_DATA, "sdmx_generic20", ".xml");
        STRUCT20 = createTemp(SdmxXmlSources.NBB_DATA_STRUCTURE, "sdmx_struct20", ".xml");
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(SdmxFileProviderTest::getProvider, o -> {
            SdmxFileBean result = o.newBean();
            result.setFile(GENERIC20);
            return result;
        });
    }

    @Test
    public void testMonikerUri() {
        String uri = "demetra://tsprovider/sdmx-file/v1/SERIES?f=BLS.xml&l=TITLE#k=Q.AT.ALL.BC.E.LE.B3.ST.S.DINX";

        SdmxFileBean bean = new SdmxFileBean();
        bean.setFile(new File("BLS.xml"));
        bean.setLabelAttribute("TITLE");

        DataSource.Builder dataSource = DataSource.builder("sdmx-file", "v1");
        new SdmxFileParam.V1().set(dataSource, bean);
        DataSet expected = DataSet.builder(dataSource.build(), DataSet.Kind.SERIES)
                .put("k", "Q.AT.ALL.BC.E.LE.B3.ST.S.DINX")
                .build();

        try ( SdmxFileProvider p = new SdmxFileProvider()) {
            assertThat(p.toDataSet(new TsMoniker("sdmx-file", uri))).isEqualTo(expected);
        }
    }

    private static SdmxFileProvider getProvider() {
        SdmxFileProvider result = new SdmxFileProvider();
        return result;
    }

    @Test
    public void testContent() throws IOException {
        try ( SdmxFileProvider p = new SdmxFileProvider()) {

            AtomicReference<TsMoniker> single = new AtomicReference<>();
            assertThat(newColInfo(p, GENERIC20, STRUCT20)).satisfies(info -> {
                assertThat(p.get(info)).isTrue();
                assertThat(info.items)
                        .hasSize(1)
                        .element(0)
                        .satisfies(o -> {
                            assertThat(o.name).isEqualTo("LOCSTL04.AUS.M");
                            assertThat(new HashMap(o.metaData)).hasSize(1).containsEntry("TIME_FORMAT", "P1M");
                            assertThat(o.data).isNull();
                            assertThat(o.invalidDataCause).startsWith("Cannot guess").contains("duplicated");
                            assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("Monthly");
                            single.set(o.moniker);
                        });
            });

            p.clearCache();
            assertThat(new TsInformation("", single.get(), TsInformationType.All)).satisfies(o -> {
                assertThat(p.get(o)).isTrue();
                assertThat(o.name).isEqualTo("LOCSTL04.AUS.M");
                assertThat(new HashMap(o.metaData)).hasSize(1).containsEntry("TIME_FORMAT", "P1M");
                assertThat(o.data).isNull();
                assertThat(o.invalidDataCause).startsWith("Cannot guess").contains("duplicated");
                assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("Monthly");
                single.set(o.moniker);
            });

            assertThat(newColInfo(p, GENERIC20, BLANK)).satisfies(info -> {
                assertThat(p.get(info)).isTrue();
                assertThat(info.items).hasSize(1).element(0).satisfies(o -> assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("M"));
            });

            assertThat(newColInfo(p, GENERIC20, BLANK, "SUBJECT", "LOCATION", "FREQUENCY")).satisfies(info -> {
                assertThat(p.get(info)).isTrue();
                assertThat(info.items).hasSize(1).element(0).satisfies(o -> assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("M"));
            });

            assertThat(newColInfo(p, GENERIC20, BLANK, "LOCATION", "FREQUENCY", "SUBJECT")).satisfies(info -> {
                assertThat(p.get(info)).isTrue();
                assertThat(info.items).hasSize(1).element(0).satisfies(o -> assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("LOCSTL04"));
            });

            assertThat(newColInfo(p, GENERIC20, STRUCT20, "LOCATION", "FREQUENCY", "SUBJECT")).satisfies(info -> {
                assertThat(p.get(info)).isTrue();
                assertThat(info.items).hasSize(1).element(0).satisfies(o -> assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("Amplitude adjusted (CLI)"));
            });

            assertThat(newColInfo(p, NO_XML, STRUCT20)).satisfies(info -> {
                assertThat(p.get(info)).isFalse();
                assertThat(info.invalidDataCause).contains("end-of-file");
            });

            assertThat(newColInfo(p, GENERIC20, NO_XML)).satisfies(info -> {
                assertThat(p.get(info)).isFalse();
                assertThat(info.invalidDataCause).contains("end-of-file");
            });
        }
    }

    private static TsCollectionInformation newColInfo(SdmxFileProvider p, File data, File structure, String... dims) {
        DataSource dataSource = p.encodeBean(newBean(data, structure, dims));
        assertThat(p.open(dataSource)).isTrue();
        return new TsCollectionInformation(p.toMoniker(dataSource), TsInformationType.All);
    }

    private static SdmxFileBean newBean(File data, File structure, String... dims) {
        SdmxFileBean result = new SdmxFileBean();
        result.setFile(data);
        result.setStructureFile(structure);
        result.setDimensions(Arrays.asList(dims));
        return result;
    }
}
