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
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import static ec.tss.tsproviders.IDataSourceLoaderAssert.assertThat;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.io.IOException;
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
        assertThat(getProvider())
                .isEquivalentTo(getPreviousProvider(), DotStatProvider2Test::getSampleDataSource);
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(DotStatProvider2Test::getProvider, o -> {
            return o.newBean();
        });
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
