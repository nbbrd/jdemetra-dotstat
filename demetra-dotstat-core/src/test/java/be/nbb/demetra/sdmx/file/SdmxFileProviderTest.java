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

import be.nbb.sdmx.facade.samples.SdmxSource;
import ec.tss.TsCollectionInformation;
import ec.tss.TsInformationType;
import ec.tss.TsMoniker;
import static ec.tss.tsproviders.Assertions.assertThat;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxFileProviderTest {

    private static File sampleFile;

    @BeforeClass
    public static void beforeClass() throws IOException {
        sampleFile = File.createTempFile("sdmx", ".xml");
        sampleFile.deleteOnExit();
        SdmxSource.NBB_DATA.copyTo(sampleFile.toPath());
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(SdmxFileProviderTest::getProvider, o -> {
            SdmxFileBean result = o.newBean();
            result.setFile(sampleFile);
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
        File structureFile = File.createTempFile("sdmx", ".xml");
        structureFile.deleteOnExit();
        SdmxSource.NBB_DATA_STRUCTURE.copyTo(structureFile.toPath());

        SdmxFileBean bean = new SdmxFileBean();
        bean.setFile(sampleFile);
        bean.setStructureFile(structureFile);

        try (SdmxFileProvider p = new SdmxFileProvider()) {
            DataSource dataSource = p.encodeBean(bean);
            Assertions.assertThat(p.open(dataSource)).isTrue();
            TsCollectionInformation info = new TsCollectionInformation(p.toMoniker(dataSource), TsInformationType.All);
            Assertions.assertThat(p.get(info)).isTrue();
            Assertions.assertThat(info.items)
                    .hasSize(1)
                    .element(0)
                    .satisfies(o -> {
                        Assertions.assertThat(o.name).isEqualTo("LOCSTL04.AUS.M");
                        Assertions.assertThat(new HashMap(o.metaData)).hasSize(1).containsEntry("TIME_FORMAT", "P1M");
                        Assertions.assertThat(o.data).isNotNull();
                    });
        }
    }
}
