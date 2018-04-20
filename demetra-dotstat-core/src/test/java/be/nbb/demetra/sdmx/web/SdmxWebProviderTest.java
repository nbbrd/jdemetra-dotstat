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
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.Frequency;
import be.nbb.sdmx.facade.repo.SdmxRepository;
import be.nbb.sdmx.facade.Obs;
import be.nbb.sdmx.facade.repo.SdmxRepositoryManager;
import be.nbb.sdmx.facade.Series;
import ec.tss.TsMoniker;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static ec.tss.tsproviders.Assertions.assertThat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import be.nbb.sdmx.facade.SdmxManager;

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
        result.setDbName("world");
        result.setTableName("CONJ");
        result.setDimColumns("REGION, SECTOR");
        return o.encodeBean(result);
    }

    private static SdmxManager getCustomManager() {
        return SdmxRepositoryManager.builder().repository(getCustomRepo()).build();
    }

    private static SdmxRepository getCustomRepo() {
        DataStructure conjStruct = DataStructure.builder()
                .ref(DataStructureRef.of("NBB", "RES1", "1.0"))
                .dimension(Dimension.builder().id("REGION").position(1).label("Region").code("BE", "Belgium").code("FR", "France").build())
                .dimension(Dimension.builder().id("SECTOR").position(2).label("Sector").code("IND", "Industry").code("XXX", "Other").build())
                .label("hello")
                .timeDimensionId("time")
                .primaryMeasureId("value")
                .build();
        Dataflow conj = Dataflow.of(DataflowRef.parse("CONJ"), conjStruct.getRef(), "Conjoncture");

        return SdmxRepository.builder()
                .name("world")
                .structure(conjStruct)
                .flow(conj)
                .data(conj.getRef(), Series.builder().key(Key.of("BE", "IND")).freq(Frequency.MONTHLY).obs(toSeries(TsFrequency.Monthly, 1)).build())
                .data(conj.getRef(), Series.builder().key(Key.of("BE", "XXX")).freq(Frequency.MONTHLY).obs(toSeries(TsFrequency.Monthly, 2)).build())
                .data(conj.getRef(), Series.builder().key(Key.of("FR", "IND")).freq(Frequency.MONTHLY).obs(toSeries(TsFrequency.Monthly, 3)).build())
                .build();
    }

    private static List<Obs> toSeries(TsFrequency freq, int seed) {
        return TsData.random(freq, seed).stream()
                .map(o -> Obs.of(LocalDateTime.ofInstant(o.getPeriod().middle().toInstant(), ZoneId.systemDefault()), o.getValue()))
                .collect(Collectors.toList());
    }
}
