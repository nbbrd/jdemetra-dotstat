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
package be.nbb.demetra.sdmx.web;

import be.nbb.demetra.dotstat.DotStatProvider;
import ec.tss.TsMoniker;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static ec.tss.tsproviders.Assertions.assertThat;
import java.util.EnumSet;
import sdmxdl.Feature;
import sdmxdl.web.SdmxWebManager;
import tests.sdmxdl.api.RepoSamples;
import tests.sdmxdl.web.spi.MockedDriver;

/**
 *
 * @author Philippe Charles
 */
public class SdmxWebProviderTest {

    @Test
    public void testEquivalence() throws IOException {
        IDataSourceLoaderAssert.assertThat(getProvider())
                .isEquivalentTo(getPreviousProvider(), SdmxWebProviderTest::getSampleDataSource);
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(SdmxWebProviderTest::getProvider, o -> {
            return o.newBean();
        });
    }

    @Test
    public void testMonikerUri() {
        String uri = "demetra://tsprovider/DOTSTAT/20150203/SERIES?cacheDepth=2&cacheTtl=360000&dbName=ECB&dimColumns=CURRENCY%2CCURRENCY_DENOM%2CEXR_SUFFIX%2CEXR_TYPE%2CFREQ&tableName=ECB%2CEXR%2C1.0#CURRENCY=CHF&CURRENCY_DENOM=EUR&EXR_SUFFIX=A&EXR_TYPE=SP00&FREQ=M";

        SdmxWebBean bean = new SdmxWebBean();
        bean.setSource("ECB");
        bean.setFlow("ECB,EXR,1.0");
        bean.setDimensions(Arrays.asList("CURRENCY", "CURRENCY_DENOM", "EXR_SUFFIX", "EXR_TYPE", "FREQ"));
        bean.setCacheDepth(2);
        bean.setCacheTtl(Duration.ofMinutes(6));

        DataSource.Builder dataSource = DataSource.builder("DOTSTAT", "20150203");
        new SdmxWebParam.V1().set(dataSource, bean);
        DataSet expected = DataSet.builder(dataSource.build(), DataSet.Kind.SERIES)
                .put("CURRENCY", "CHF")
                .put("CURRENCY_DENOM", "EUR")
                .put("EXR_SUFFIX", "A")
                .put("EXR_TYPE", "SP00")
                .put("FREQ", "M")
                .build();

        try (SdmxWebProvider p = new SdmxWebProvider()) {
            assertThat(p.toDataSet(new TsMoniker("DOTSTAT", uri))).isEqualTo(expected);
        }
    }

    private static SdmxWebProvider getProvider() {
        SdmxWebProvider result = new SdmxWebProvider();
        result.setSdmxManager(getCustomManager());
        return result;
    }

    private static DotStatProvider getPreviousProvider() {
        DotStatProvider result = new DotStatProvider();
        result.setSdmxManager(getCustomManager());
        return result;
    }

    private static DataSource getSampleDataSource(DotStatProvider o) {
        be.nbb.demetra.dotstat.DotStatBean result = o.newBean();
        result.setDbName(RepoSamples.REPO.getName());
        result.setTableName(RepoSamples.FLOW_REF.toString());
        result.setDimColumns("FREQ, REGION, SECTOR");
        return o.encodeBean(result);
    }

    private static SdmxWebManager getCustomManager() {
        return SdmxWebManager
                .builder()
                .driver(MockedDriver
                        .builder()
                        .id("demo")
                        .repo(RepoSamples.REPO, EnumSet.allOf(Feature.class))
                        .build())
                .build();
    }
}
