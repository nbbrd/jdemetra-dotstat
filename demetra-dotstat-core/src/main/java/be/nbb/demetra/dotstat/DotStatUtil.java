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

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.TimeFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.OptionalTsData;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
class DotStatUtil {

    @Nonnull
    public static TsCursor<Key> getAllSeries(SdmxConnection conn, DataflowRef flowRef, Key ref) throws IOException {
        return conn.isSeriesKeysOnlySupported()
                ? request(conn, flowRef, ref, true)
                : computeKeys(conn, flowRef, ref);
    }

    @Nonnull
    public static TsCursor<Key> getAllSeriesWithData(SdmxConnection conn, DataflowRef flowRef, Key ref) throws IOException {
        return conn.isSeriesKeysOnlySupported()
                ? request(conn, flowRef, ref, false)
                : computeKeysAndRequestData(conn, flowRef, ref);
    }

    @Nonnull
    public static OptionalTsData getSeriesWithData(SdmxConnection conn, DataflowRef flowRef, Key ref) throws IOException {
        try (TsCursor<Key> cursor = request(conn, flowRef, ref, false)) {
            return cursor.nextSeries() ? cursor.getSeriesData() : MISSING_DATA;
        }
    }

    @Nonnull
    public static List<String> getChildren(SdmxConnection conn, DataflowRef flowRef, Key ref, int dimensionPosition) throws IOException {
        if (conn.isSeriesKeysOnlySupported()) {
            try (TsCursor<Key> cursor = request(conn, flowRef, ref, true)) {
                int index = dimensionPosition - 1;
                TreeSet<String> result = new TreeSet<>();
                while (cursor.nextSeries()) {
                    result.add(cursor.getSeriesId().getItem(index));
                }
                return ImmutableList.copyOf(result);
            }
        }

        return computeAllPossibleChildren(dimensionByIndex(conn.getDataStructure(flowRef)), dimensionPosition);
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static final OptionalTsData MISSING_DATA = OptionalTsData.absent("No results matching the query");

    private static final class Adapter implements TsCursor<Key> {

        private final Key group;
        private final DataCursor cursor;
        private Key currentKey;

        public Adapter(Key group, DataCursor cursor) {
            this.group = group;
            this.cursor = cursor;
            this.currentKey = null;
        }

        @Override
        public boolean nextSeries() throws IOException {
            while (cursor.nextSeries()) {
                currentKey = cursor.getKey();
                if (group.contains(currentKey)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Key getSeriesId() throws IOException {
            return currentKey;
        }

        @Override
        public OptionalTsData getSeriesData() throws IOException {
            return toData(cursor);
        }

        @Override
        public void close() throws IOException {
            cursor.close();
        }

        @Override
        public Map<String, String> getSeriesMetaData() throws IOException, IllegalStateException {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getMetaData() throws IOException, IllegalStateException {
            return Collections.emptyMap();
        }

        @Override
        public boolean isClosed() throws IOException {
            return false;
        }

        private static OptionalTsData toData(DataCursor cursor) throws IOException {
            OptionalTsData.Builder data = createDataBuilder(cursor.getTimeFormat());
            while (cursor.nextObs()) {
                Date period = cursor.getPeriod();
                Number value = period != null ? cursor.getValue() : null;
                data.add(period, value);
            }
            return data.build();
        }

        private static OptionalTsData.Builder createDataBuilder(TimeFormat format) {
            switch (format) {
                case YEARLY:
                    return new OptionalTsData.Builder(TsFrequency.Yearly, TsAggregationType.None);
                case HALF_YEARLY:
                    return new OptionalTsData.Builder(TsFrequency.HalfYearly, TsAggregationType.None);
                case QUADRI_MONTHLY:
                    return new OptionalTsData.Builder(TsFrequency.QuadriMonthly, TsAggregationType.None);
                case QUARTERLY:
                    return new OptionalTsData.Builder(TsFrequency.Quarterly, TsAggregationType.None);
                case MONTHLY:
                    return new OptionalTsData.Builder(TsFrequency.Monthly, TsAggregationType.None);
                case WEEKLY:
                    return new OptionalTsData.Builder(TsFrequency.Monthly, TsAggregationType.Last);
                case DAILY:
                    return new OptionalTsData.Builder(TsFrequency.Monthly, TsAggregationType.Last);
                case HOURLY:
                    return new OptionalTsData.Builder(TsFrequency.Monthly, TsAggregationType.Last);
                case MINUTELY:
                    return new OptionalTsData.Builder(TsFrequency.Monthly, TsAggregationType.Last);
                default:
                    return new OptionalTsData.Builder(TsFrequency.Undefined, TsAggregationType.None);
            }
        }
    }

    private static TsCursor<Key> request(SdmxConnection conn, DataflowRef flowRef, Key key, boolean seriesKeysOnly) throws IOException {
        return new Adapter(key, conn.getData(flowRef, key, seriesKeysOnly));
    }

    private static TsCursor<Key> computeKeys(SdmxConnection conn, DataflowRef flowRef, Key key) throws IOException {
        final List<Key> list = computeAllPossibleSeries(dimensionByIndex(conn.getDataStructure(flowRef)), key);
        return new TsCursor<Key>() {
            private int index = -1;

            @Override
            public boolean nextSeries() throws IOException {
                index++;
                return index < list.size();
            }

            @Override
            public Key getSeriesId() throws IOException {
                return list.get(index);
            }

            @Override
            public OptionalTsData getSeriesData() throws IOException {
                throw new RuntimeException();
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public Map<String, String> getSeriesMetaData() throws IOException, IllegalStateException {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> getMetaData() throws IOException, IllegalStateException {
                return Collections.emptyMap();
            }

            @Override
            public boolean isClosed() throws IOException {
                return false;
            }
        };
    }

    private static TsCursor<Key> computeKeysAndRequestData(SdmxConnection conn, DataflowRef flowRef, Key key) throws IOException {
        final List<Key> list = computeAllPossibleSeries(dimensionByIndex(conn.getDataStructure(flowRef)), key);
        final Map<Key, OptionalTsData> dataByKey = new HashMap<>();
        try (TsCursor<Key> cursor = request(conn, flowRef, key, false)) {
            while (cursor.nextSeries()) {
                dataByKey.put(cursor.getSeriesId(), cursor.getSeriesData());
            }
        }
        return new TsCursor<Key>() {
            private int index = -1;

            @Override
            public boolean nextSeries() throws IOException {
                index++;
                return index < list.size();
            }

            @Override
            public Key getSeriesId() throws IOException {
                return list.get(index);
            }

            @Override
            public OptionalTsData getSeriesData() throws IOException {
                OptionalTsData data = dataByKey.get(getSeriesId());
                return data != null ? data : MISSING_DATA;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public Map<String, String> getSeriesMetaData() throws IOException, IllegalStateException {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, String> getMetaData() throws IOException, IllegalStateException {
                return Collections.emptyMap();
            }

            @Override
            public boolean isClosed() throws IOException {
                return false;
            }
        };
    }

    private static Dimension[] dimensionByIndex(DataStructure ds) {
        Dimension[] result = new Dimension[ds.getDimensions().size()];
        for (Dimension o : ds.getDimensions()) {
            result[o.getPosition() - 1] = o;
        }
        return result;
    }

    private static List<Key> computeAllPossibleSeries(Dimension[] dimensionByIndex, Key ref) {
        String[][] codeLists = new String[dimensionByIndex.length][];
        for (int i = 0; i < codeLists.length; i++) {
            codeLists[i] = Key.ALL.equals(ref) || ref.isWildcard(i)
                    ? dimensionByIndex[i].getCodes().keySet().toArray(new String[0])
                    : new String[]{ref.getItem(i)};
        }

        List<Key> result = new ArrayList<>();
        String[] stack = new String[codeLists.length];
        computeAllPossibleSeries(codeLists, 0, stack, result);
        return result;
    }

    private static void computeAllPossibleSeries(String[][] data, int idx, String[] stack, List<Key> result) {
        for (String code : data[idx]) {
            stack[idx] = code;
            if (idx == data.length - 1) {
                result.add(Key.of(stack));
            } else {
                computeAllPossibleSeries(data, idx + 1, stack, result);
            }
        }
    }

    private static List<String> computeAllPossibleChildren(Dimension[] dimensionByPosition, int dimensionPosition) {
        Dimension dimension = dimensionByPosition[dimensionPosition - 1];
        return Ordering.natural().sortedCopy(dimension.getCodes().keySet());
    }
    //</editor-fold>
}
