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

import sdmxdl.*;
import com.google.common.collect.ImmutableList;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.db.DbAccessor;
import ec.tss.tsproviders.db.DbSeries;
import ec.tss.tsproviders.db.DbSetId;
import ec.tstoolkit.design.VisibleForTesting;
import internal.sdmx.SdmxQueryUtil;
import java.io.IOException;
import java.util.List;

import sdmxdl.ext.SdmxCubeUtil;
import sdmxdl.web.SdmxWebManager;

/**
 *
 * @author Philippe Charles
 */
@Deprecated
final class DotStatAccessor extends DbAccessor.Abstract<DotStatBean> {

    private final SdmxWebManager manager;
    private final Languages languages = Languages.ANY;

    DotStatAccessor(DotStatBean dbBean, SdmxWebManager manager) {
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
        try (Connection conn = manager.getConnection(dbBean.getDbName(), languages)) {
            return getAllSeries(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected List<DbSeries> getAllSeriesWithData(DbSetId ref) throws Exception {
        try (Connection conn = manager.getConnection(dbBean.getDbName(), languages)) {
            return getAllSeriesWithData(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected DbSeries getSeriesWithData(DbSetId ref) throws Exception {
        try (Connection conn = manager.getConnection(dbBean.getDbName(), languages)) {
            return getSeriesWithData(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    protected List<String> getChildren(DbSetId ref) throws Exception {
        try (Connection conn = manager.getConnection(dbBean.getDbName(), languages)) {
            return getChildren(conn, dbBean.getFlowRef(), ref);
        }
    }

    @Override
    public DbAccessor<DotStatBean> memoize() {
        return DbAccessor.BulkAccessor.from(this, dbBean.getCacheDepth(), DbAccessor.BulkAccessor.newTtlCache(dbBean.getCacheTtl()));
    }

    private static List<DbSetId> getAllSeries(Connection conn, DataflowRef flow, DbSetId node) throws IOException {
        KeyConverter converter = KeyConverter.of(conn.getStructure(flow), node);

        try (TsCursor<Key> cursor = SdmxQueryUtil.getAllSeries(conn, flow, converter.toKey(node), SdmxQueryUtil.NO_LABEL)) {
            ImmutableList.Builder<DbSetId> result = ImmutableList.builder();
            while (cursor.nextSeries()) {
                result.add(converter.fromKey(cursor.getSeriesId()));
            }
            return result.build();
        }
    }

    private static List<DbSeries> getAllSeriesWithData(Connection conn, DataflowRef flow, DbSetId node) throws IOException {
        KeyConverter converter = KeyConverter.of(conn.getStructure(flow), node);

        try (TsCursor<Key> cursor = SdmxQueryUtil.getAllSeriesWithData(conn, flow, converter.toKey(node), SdmxQueryUtil.NO_LABEL)) {
            ImmutableList.Builder<DbSeries> result = ImmutableList.builder();
            while (cursor.nextSeries()) {
                result.add(new DbSeries(converter.fromKey(cursor.getSeriesId()), cursor.getSeriesData()));
            }
            return result.build();
        }
    }

    private static DbSeries getSeriesWithData(Connection conn, DataflowRef flow, DbSetId leaf) throws IOException {
        KeyConverter converter = KeyConverter.of(conn.getStructure(flow), leaf);

        try (TsCursor<Key> cursor = SdmxQueryUtil.getSeriesWithData(conn, flow, converter.toKey(leaf), SdmxQueryUtil.NO_LABEL)) {
            return new DbSeries(leaf, cursor.nextSeries() ? cursor.getSeriesData() : SdmxQueryUtil.MISSING_DATA);
        }
    }

    private static List<String> getChildren(Connection conn, DataflowRef flow, DbSetId node) throws IOException {
        DataStructure dsd = conn.getStructure(flow);
        KeyConverter converter = KeyConverter.of(dsd, node);
        String dimensionId = node.getColumn(node.getLevel());
        int dimensionIndex = SdmxCubeUtil.getDimensionIndexById(dsd, dimensionId).orElseThrow(RuntimeException::new);
        return SdmxQueryUtil.getChildren(conn, flow, converter.toKey(node), dimensionIndex);
    }

    @VisibleForTesting
    static Key getKey(DataStructure dsd, DbSetId ref) {
        if (ref.isRoot()) {
            return Key.ALL;
        }
        String[] result = new String[ref.getMaxLevel()];
        for (int i = 0; i < result.length; i++) {
            String id = ref.getColumn(i);
            String value = i < ref.getLevel() ? ref.getValue(i) : "";
            int index = SdmxCubeUtil.getDimensionIndexById(dsd, id).orElseThrow(RuntimeException::new);
            result[index] = value;
        }
        return Key.of(result);
    }

    @lombok.RequiredArgsConstructor
    static final class KeyConverter {

        static KeyConverter of(DataStructure dsd, DbSetId ref) {
            return new KeyConverter(dsd, new DbSetIdBuilder(dsd, ref));
        }

        final DataStructure dsd;
        final DbSetIdBuilder builder;

        public Key toKey(DbSetId a) {
            return getKey(dsd, a);
        }

        public DbSetId fromKey(Key b) {
            return builder.getId(b);
        }
    }

    private static final class DbSetIdBuilder {

        private final DbSetId ref;
        private final int[] indices;
        private final String[] dimValues;

        DbSetIdBuilder(DataStructure dsd, DbSetId ref) {
            this.ref = ref;
            String[] selectColumns = ref.selectColumns();
            this.indices = new int[selectColumns.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = SdmxCubeUtil.getDimensionIndexById(dsd, selectColumns[i]).orElseThrow(RuntimeException::new);
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
}
