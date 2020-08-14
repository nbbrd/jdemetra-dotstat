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

import sdmxdl.DataStructure;
import sdmxdl.Dimension;
import sdmxdl.DataflowRef;
import sdmxdl.Key;
import sdmxdl.SdmxConnection;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.db.DbAccessor;
import ec.tss.tsproviders.db.DbSeries;
import ec.tss.tsproviders.db.DbSetId;
import ec.tstoolkit.design.VisibleForTesting;
import internal.sdmx.SdmxQueryUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import sdmxdl.SdmxManager;

/**
 *
 * @author Philippe Charles
 */
@Deprecated
final class DotStatAccessor extends DbAccessor.Abstract<DotStatBean> {

    private final SdmxManager manager;

    DotStatAccessor(DotStatBean dbBean, SdmxManager manager) {
        super(dbBean);
        this.manager = manager;
    }

    @Override
    public Exception testDbBean() {
        Exception result = super.testDbBean();
        if (result != null) {
            return result;
        }
        try {
            dbBean.getFlowRef();
        } catch (IllegalArgumentException ex) {
            return ex;
        }
        return null;
    }

    @Override
    protected List<DbSetId> getAllSeries(DbSetId ref) throws Exception {
        try (SdmxConnection conn = manager.getConnection(dbBean.getDbName())) {
            return getAllSeries(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected List<DbSeries> getAllSeriesWithData(DbSetId ref) throws Exception {
        try (SdmxConnection conn = manager.getConnection(dbBean.getDbName())) {
            return getAllSeriesWithData(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected DbSeries getSeriesWithData(DbSetId ref) throws Exception {
        try (SdmxConnection conn = manager.getConnection(dbBean.getDbName())) {
            return getSeriesWithData(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected List<String> getChildren(DbSetId ref) throws Exception {
        try (SdmxConnection conn = manager.getConnection(dbBean.getDbName())) {
            return getChildren(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    public DbAccessor<DotStatBean> memoize() {
        return DbAccessor.BulkAccessor.from(this, dbBean.getCacheDepth(), DbAccessor.BulkAccessor.newTtlCache(dbBean.getCacheTtl()));
    }

    private static List<DbSetId> getAllSeries(SdmxConnection conn, DataflowRef flowRef, DbSetId ref) throws IOException {
        Converter<DbSetId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key colKey = converter.convert(ref);
        try (TsCursor<Key> cursor = SdmxQueryUtil.getAllSeries(conn, flowRef, colKey, SdmxQueryUtil.NO_LABEL)) {
            ImmutableList.Builder<DbSetId> result = ImmutableList.builder();
            while (cursor.nextSeries()) {
                result.add(converter.reverse().convert(cursor.getSeriesId()));
            }
            return result.build();
        }
    }

    private static List<DbSeries> getAllSeriesWithData(SdmxConnection conn, DataflowRef flowRef, DbSetId ref) throws IOException {
        Converter<DbSetId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key colKey = converter.convert(ref);
        try (TsCursor<Key> cursor = SdmxQueryUtil.getAllSeriesWithData(conn, flowRef, colKey, SdmxQueryUtil.NO_LABEL)) {
            ImmutableList.Builder<DbSeries> result = ImmutableList.builder();
            while (cursor.nextSeries()) {
                result.add(new DbSeries(converter.reverse().convert(cursor.getSeriesId()), cursor.getSeriesData()));
            }
            return result.build();
        }
    }

    private static DbSeries getSeriesWithData(SdmxConnection conn, DataflowRef flowRef, DbSetId ref) throws IOException {
        Converter<DbSetId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key seriesKey = converter.convert(ref);
        try (TsCursor<Key> cursor = SdmxQueryUtil.getSeriesWithData(conn, flowRef, seriesKey, SdmxQueryUtil.NO_LABEL)) {
            return new DbSeries(ref, cursor.nextSeries() ? cursor.getSeriesData() : SdmxQueryUtil.MISSING_DATA);
        }
    }

    private static List<String> getChildren(SdmxConnection conn, DataflowRef flowRef, DbSetId ref) throws IOException {
        DataStructure dsd = conn.getStructure(flowRef);
        Converter<DbSetId, Key> converter = getConverter(dsd, ref);
        int dimensionPosition = dimensionById(dsd).get(ref.getColumn(ref.getLevel())).getPosition();
        return SdmxQueryUtil.getChildren(conn, flowRef, converter.convert(ref), dimensionPosition);
    }

    @VisibleForTesting
    static Key getKey(Map<String, Dimension> dimensionById, DbSetId ref) {
        if (ref.isRoot()) {
            return Key.ALL;
        }
        String[] result = new String[ref.getMaxLevel()];
        for (int i = 0; i < result.length; i++) {
            result[dimensionById.get(ref.getColumn(i)).getPosition() - 1] = i < ref.getLevel() ? ref.getValue(i) : "";
        }
        return Key.of(result);
    }

    static Converter<DbSetId, Key> getConverter(DataStructure ds, DbSetId ref) {
        final Map<String, Dimension> dimensionById = dimensionById(ds);
        final DbSetIdBuilder builder = new DbSetIdBuilder(dimensionById, ref);
        return new Converter<DbSetId, Key>() {
            @Override
            protected Key doForward(DbSetId a) {
                return getKey(dimensionById, a);
            }

            @Override
            protected DbSetId doBackward(Key b) {
                return builder.getId(b);
            }
        };
    }

    private static final class DbSetIdBuilder {

        private final DbSetId ref;
        private final int[] indices;
        private final String[] dimValues;

        DbSetIdBuilder(Map<String, Dimension> dimensionById, DbSetId ref) {
            this.ref = ref;
            String[] selectColumns = ref.selectColumns();
            this.indices = new int[selectColumns.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = dimensionById.get(selectColumns[i]).getPosition() - 1;
            }
            this.dimValues = new String[selectColumns.length];
        }

        public DbSetId getId(Key o) {
            for (int i = 0; i < indices.length; i++) {
                dimValues[i] = o.get(indices[i]);
            }
            return ref.child(dimValues);
        }
    }

    @VisibleForTesting
    static Map<String, Dimension> dimensionById(DataStructure ds) {
        return Maps.uniqueIndex(ds.getDimensions(), Dimension::getId);
    }
}
