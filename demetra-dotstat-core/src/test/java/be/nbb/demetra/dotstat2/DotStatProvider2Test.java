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
package be.nbb.demetra.dotstat2;

import be.nbb.demetra.dotstat.DotStatProvider;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.TimeFormat;
import be.nbb.sdmx.facade.util.MemSdmxRepository;
import be.nbb.sdmx.facade.util.MemSdmxRepository.Obs;
import be.nbb.sdmx.facade.util.MemSdmxRepository.Series;
import ec.tss.TsMoniker;
import static ec.tss.tsproviders.Assertions.assertThat;
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

/**
 *
 * @author Philippe Charles
 */
public class DotStatProvider2Test {

    @Test
    public void testEquivalence() throws IOException {
        IDataSourceLoaderAssert.assertThat(getProvider())
                .isEquivalentTo(getPreviousProvider(), DotStatProvider2Test::getSampleDataSource);
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(DotStatProvider2Test::getProvider, o -> {
            return o.newBean();
        });
    }

    @Test
    public void testMonikerUri() {
        String uri = "demetra://tsprovider/DOTSTAT/20150203/SERIES?cacheDepth=2&cacheTtl=360000&dbName=ECB&dimColumns=CURRENCY%2CCURRENCY_DENOM%2CEXR_SUFFIX%2CEXR_TYPE%2CFREQ&tableName=ECB%2CEXR%2C1.0#CURRENCY=CHF&CURRENCY_DENOM=EUR&EXR_SUFFIX=A&EXR_TYPE=SP00&FREQ=M";

        DotStatBean2 bean = new DotStatBean2();
        bean.setDbName("ECB");
        bean.setDimensionIds(Arrays.asList("CURRENCY", "CURRENCY_DENOM", "EXR_SUFFIX", "EXR_TYPE", "FREQ"));
        bean.setFlowRef("ECB,EXR,1.0");
        bean.setCacheDepth(2);
        bean.setCacheTtl(Duration.ofMinutes(6));

        DataSource.Builder dataSource = DataSource.builder("DOTSTAT", "20150203");
        new DotStatParam.V1().set(dataSource, bean);
        DataSet expected = DataSet.builder(dataSource.build(), DataSet.Kind.SERIES)
                .put("CURRENCY", "CHF")
                .put("CURRENCY_DENOM", "EUR")
                .put("EXR_SUFFIX", "A")
                .put("EXR_TYPE", "SP00")
                .put("FREQ", "M")
                .build();

        try (DotStatProvider2 p = new DotStatProvider2()) {
            assertThat(p.toDataSet(new TsMoniker("DOTSTAT", uri))).isEqualTo(expected);
        }
    }

    private static DotStatProvider2 getProvider() {
        DotStatProvider2 result = new DotStatProvider2();
        result.setConnectionSupplier(getCustomSupplier());
        return result;
    }

    private static DotStatProvider getPreviousProvider() {
        DotStatProvider result = new DotStatProvider();
        result.setConnectionSupplier(getCustomSupplier());
        return result;
    }

    private static DataSource getSampleDataSource(DotStatProvider o) {
        be.nbb.demetra.dotstat.DotStatBean result = o.newBean();
        result.setDbName("world");
        result.setTableName("CONJ");
        result.setDimColumns("REGION, SECTOR");
        return o.encodeBean(result);
    }

    private static SdmxConnectionSupplier getCustomSupplier() {
        DataStructure conjStruct = DataStructure.builder()
                .ref(DataStructureRef.of("NBB", "RES1", "1.0"))
                .dimension(Dimension.builder().id("REGION").position(1).label("Region").code("BE", "Belgium").code("FR", "France").build())
                .dimension(Dimension.builder().id("SECTOR").position(2).label("Sector").code("IND", "Industry").code("XXX", "Other").build())
                .label("hello")
                .timeDimensionId("time")
                .primaryMeasureId("value")
                .build();
        Dataflow conj = Dataflow.of(DataflowRef.parse("CONJ"), conjStruct.getRef(), "Conjoncture");

        return MemSdmxRepository.builder()
                .name("world")
                .dataStructure(conjStruct)
                .dataflow(conj)
                .series(Series.of(conj.getFlowRef(), Key.of("BE", "IND"), TimeFormat.MONTHLY, toSeries(TsFrequency.Monthly, 1)))
                .series(Series.of(conj.getFlowRef(), Key.of("BE", "XXX"), TimeFormat.MONTHLY, toSeries(TsFrequency.Monthly, 2)))
                .series(Series.of(conj.getFlowRef(), Key.of("FR", "IND"), TimeFormat.MONTHLY, toSeries(TsFrequency.Monthly, 3)))
                .build()
                .asConnectionSupplier();
    }

    private static List<Obs> toSeries(TsFrequency freq, int seed) {
        return TsData.random(freq, seed).stream()
                .map(o -> Obs.of(o.getPeriod().middle(), o.getValue()))
                .collect(Collectors.toList());
    }
}
