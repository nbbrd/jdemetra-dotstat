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
package be.nbb.demetra.dotstat;

import static be.nbb.demetra.dotstat.DotStatAccessor.getKey;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.connectors.TestResource;
import be.nbb.sdmx.facade.repo.SdmxRepositorySupplier;
import com.google.common.base.Joiner;
import ec.tss.tsproviders.db.DbAccessor;
import ec.tss.tsproviders.db.DbSeries;
import ec.tss.tsproviders.db.DbSetId;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.Map;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class DotStatAccessorTest {

    private final SdmxConnectionSupplier supplier = SdmxRepositorySupplier.builder()
            .repository(TestResource.nbb())
            .repository(TestResource.ecb())
            .build();

    private static DotStatBean nbbBean() {
        DotStatBean result = new DotStatBean();
        result.setDbName("NBB");
        result.setFlowRef(DataflowRef.of("NBB", "TEST_DATASET", null));
        result.setDimColumns(Joiner.on(',').join(new String[]{"SUBJECT", "LOCATION", "FREQUENCY"}));
        return result;
    }

    private static DbSetId nbbRoot() {
        return DbSetId.root("SUBJECT", "LOCATION", "FREQUENCY");
    }

    private static DotStatBean ecbBean() {
        String[] dimensions = {"FREQ", "AME_REF_AREA", "AME_TRANSFORMATION",
            "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM"};

        DotStatBean result = new DotStatBean();
        result.setDbName("ECB");
        result.setFlowRef(DataflowRef.parse("ECB,AME,1.0"));
        result.setDimColumns(Joiner.on(',').join(dimensions));
        return result;
    }

    private static DbSetId ecbRoot() {
        return DbSetId.root("FREQ", "AME_REF_AREA", "AME_TRANSFORMATION",
                "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM");
    }

    @Test
    public void testGetKey() throws Exception {
        DataStructure dfs = supplier.getConnection("NBB", LanguagePriorityList.ANY).getDataStructure(DataflowRef.of("NBB", "TEST_DATASET", null));
        Map<String, Dimension> dimById = DotStatAccessor.dimensionById(dfs);

        // default ordering of dimensions
        DbSetId r1 = DbSetId.root("SUBJECT", "LOCATION", "FREQUENCY");
        assertThat(getKey(dimById, r1.child("LOCSTL04", "AUS", "M"))).isEqualTo(Key.parse("LOCSTL04.AUS.M"));
        assertThat(getKey(dimById, r1.child("LOCSTL04", "AUS"))).isEqualTo(Key.parse("LOCSTL04.AUS."));
        assertThat(getKey(dimById, r1.child("LOCSTL04"))).isEqualTo(Key.parse("LOCSTL04.."));
        assertThat(getKey(dimById, r1)).isEqualTo(Key.ALL);

        // custom ordering of dimensions
        DbSetId r2 = DbSetId.root("FREQUENCY", "LOCATION", "SUBJECT");
        assertThat(getKey(dimById, r2.child("M", "AUS", "LOCSTL04"))).isEqualTo(Key.parse("LOCSTL04.AUS.M"));
        assertThat(getKey(dimById, r2.child("M", "AUS"))).isEqualTo(Key.parse(".AUS.M"));
        assertThat(getKey(dimById, r2.child("M"))).isEqualTo(Key.parse("..M"));
        assertThat(getKey(dimById, r2)).isEqualTo(Key.ALL);
    }

    @Test
    public void testGetKeyFromTs() throws Exception {
        try (DataCursor o = supplier.getConnection("NBB", LanguagePriorityList.ANY).getData(DataflowRef.of("NBB", "TEST_DATASET", null), Key.ALL, true)) {
            o.nextSeries();
            assertThat(o.getSeriesKey()).isEqualTo(Key.parse("LOCSTL04.AUS.M"));
        }
    }

    @Test
    public void testGetAllSeries20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier, LanguagePriorityList.ANY);

        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");

        assertThat(accessor.getAllSeries()).containsExactly(single);
        assertThat(accessor.getAllSeries("LOCSTL04")).containsExactly(single);
        assertThat(accessor.getAllSeries("LOCSTL04", "AUS")).containsExactly(single);
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier, LanguagePriorityList.ANY);

        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");
        Consumer<DbSeries> singleCheck = o -> {
            assertThat(o.getId()).isEqualTo(single);
            assertThat(o.getData().get().getLength()).isEqualTo(55);
        };

        assertThat(accessor.getAllSeriesWithData()).hasSize(1).first().satisfies(singleCheck);
        assertThat(accessor.getAllSeriesWithData("LOCSTL04")).hasSize(1).first().satisfies(singleCheck);
        assertThat(accessor.getAllSeriesWithData("LOCSTL04", "AUS")).hasSize(1).first().satisfies(singleCheck);
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier, LanguagePriorityList.ANY);

        DbSeries series = accessor.getSeriesWithData("LOCSTL04", "AUS", "M");
        assertThat(series.getId()).isEqualTo(nbbRoot().child("LOCSTL04", "AUS", "M"));

        TsData o = series.getData().get();
        assertThat(o.getStart()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1966, 1));
        assertThat(o.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1970, 7));
        assertThat(o.getLength()).isEqualTo(55);
        assertThat(o.getObsCount()).isEqualTo(54);
        assertThat(o.isMissing(50)).isTrue(); // 1970-04
        assertThat(o.get(0)).isEqualTo(98.68823);
        assertThat(o.get(54)).isEqualTo(101.1945);
    }

    @Test
    public void testGetChildren20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier, LanguagePriorityList.ANY);

        assertThat(accessor.getChildren()).containsExactly("LOCSTL04");
        assertThat(accessor.getChildren("LOCSTL04")).containsExactly("AUS");
        assertThat(accessor.getChildren("LOCSTL04", "AUS")).containsExactly("M");
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier, LanguagePriorityList.ANY);

        DbSetId root = ecbRoot();

        assertThat(accessor.getAllSeries())
                .hasSize(120)
                .contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE"))
                .contains(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN"))
                .allMatch(DbSetId::isSeries);

        assertThat(accessor.getAllSeries("A"))
                .hasSize(120)
                .allMatch(DbSetId::isSeries);

        assertThat(accessor.getAllSeries("A", "DEU"))
                .hasSize(4)
                .contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE"))
                .doesNotContain(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN"))
                .allMatch(DbSetId::isSeries);
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier, LanguagePriorityList.ANY);

        DbSetId item = ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE");

        assertThat(accessor.getAllSeriesWithData())
                .hasSize(120)
                .filteredOn(o -> o.getId().equals(item))
                .first()
                .satisfies(o -> assertThat(o.getData().get().getLength()).isEqualTo(25));

        assertThat(accessor.getAllSeriesWithData("A", "DEU"))
                .hasSize(4)
                .filteredOn(o -> o.getId().equals(item))
                .first()
                .satisfies(o -> assertThat(o.getData().get().getLength()).isEqualTo(25));
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier, LanguagePriorityList.ANY);

        DbSeries series = accessor.getSeriesWithData("A", "DEU", "1", "0", "319", "0", "UBLGE");
        assertThat(series.getId()).isEqualTo(ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE"));

        TsData o = series.getData().get();
        assertThat(o.getStart()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 1991, 0));
        assertThat(o.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 2015, 0));
        assertThat(o.getLength()).isEqualTo(25);
        assertThat(o.getObsCount()).isEqualTo(25);
        assertThat(o.get(0)).isEqualTo(-2.8574221);
        assertThat(o.get(24)).isEqualTo(-0.1420473);
    }

    @Test
    public void testGetChildren21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier, LanguagePriorityList.ANY);

        assertThat(accessor.getChildren())
                .hasSize(1)
                .contains("A");

        assertThat(accessor.getChildren("A"))
                .hasSize(30)
                .contains("BEL", "POL");

        assertThat(accessor.getChildren("A", "BEL"))
                .hasSize(1)
                .contains("1");

        assertThat(accessor.getChildren("hello"))
                .isEmpty();
    }
}
