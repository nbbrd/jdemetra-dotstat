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

import be.nbb.sdmx.facade.samples.ByteSource;
import be.nbb.sdmx.facade.samples.SdmxSource;
import static be.nbb.sdmx.facade.util.FreqUtil.TIME_FORMAT_CONCEPT;
import ec.tss.TsCollectionInformation;
import ec.tss.TsInformationType;
import ec.tss.TsMoniker;
import static ec.tss.tsproviders.Assertions.assertThat;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

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
        bytes.copyTo(result.toPath());
        return result;
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        NO_XML = File.createTempFile("sdmx_empty", ".xml");
        NO_XML.deleteOnExit();
        BLANK = new File("");
        GENERIC20 = createTemp(SdmxSource.NBB_DATA, "sdmx_generic20", ".xml");
        STRUCT20 = createTemp(SdmxSource.NBB_DATA_STRUCTURE, "sdmx_struct20", ".xml");
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

        try (SdmxFileProvider p = new SdmxFileProvider()) {
            assertThat(p.toDataSet(new TsMoniker("sdmx-file", uri))).isEqualTo(expected);
        }
    }

    private static SdmxFileProvider getProvider() {
        SdmxFileProvider result = new SdmxFileProvider();
        return result;
    }

    @Test
    public void testContent() throws IOException {
        try (SdmxFileProvider p = new SdmxFileProvider()) {
            Assertions.assertThat(newRequest(p, GENERIC20, STRUCT20)).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isTrue();
                Assertions.assertThat(info.items)
                        .hasSize(1)
                        .element(0)
                        .satisfies(o -> {
                            Assertions.assertThat(o.name).isEqualTo("LOCSTL04.AUS.M");
                            Assertions.assertThat(new HashMap(o.metaData)).hasSize(1).containsEntry(TIME_FORMAT_CONCEPT, "P1M");
                            Assertions.assertThat(o.data).isNotNull();
                            Assertions.assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("Monthly");
                        });
            });

            Assertions.assertThat(newRequest(p, GENERIC20, BLANK)).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isTrue();
                Assertions.assertThat(info.items).hasSize(1).element(0).satisfies(o -> Assertions.assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("M"));
            });

            Assertions.assertThat(newRequest(p, GENERIC20, BLANK, "SUBJECT", "LOCATION", "FREQUENCY")).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isTrue();
                Assertions.assertThat(info.items).hasSize(1).element(0).satisfies(o -> Assertions.assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("M"));
            });

            Assertions.assertThat(newRequest(p, GENERIC20, BLANK, "LOCATION", "FREQUENCY", "SUBJECT")).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isTrue();
                Assertions.assertThat(info.items).hasSize(1).element(0).satisfies(o -> Assertions.assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("LOCSTL04"));
            });

            Assertions.assertThat(newRequest(p, GENERIC20, STRUCT20, "LOCATION", "FREQUENCY", "SUBJECT")).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isTrue();
                Assertions.assertThat(info.items).hasSize(1).element(0).satisfies(o -> Assertions.assertThat(p.getDisplayNodeName(p.toDataSet(o.moniker))).isEqualTo("Amplitude adjusted (CLI)"));
            });

            Assertions.assertThat(newRequest(p, NO_XML, STRUCT20)).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isFalse();
                Assertions.assertThat(info.invalidDataCause).contains("XMLStreamException");
            });

            Assertions.assertThat(newRequest(p, GENERIC20, NO_XML)).satisfies(info -> {
                Assertions.assertThat(p.get(info)).isFalse();
                Assertions.assertThat(info.invalidDataCause).contains("XMLStreamException");
            });
        }
    }

    private static TsCollectionInformation newRequest(SdmxFileProvider p, File data, File structure, String... dims) {
        SdmxFileBean bean = new SdmxFileBean();
        bean.setFile(data);
        bean.setStructureFile(structure);
        bean.setDimensions(Arrays.asList(dims));

        DataSource dataSource = p.encodeBean(bean);
        Assertions.assertThat(p.open(dataSource)).isTrue();
        return new TsCollectionInformation(p.toMoniker(dataSource), TsInformationType.All);
    }
}
