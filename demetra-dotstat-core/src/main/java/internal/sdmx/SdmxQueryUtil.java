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
package internal.sdmx;

import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.DataQuery;
import be.nbb.sdmx.facade.SdmxConnection;
import com.google.common.collect.ImmutableList;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.OptionalTsData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxQueryUtil {

    public final String NO_LABEL = null;
    public final OptionalTsData MISSING_DATA = OptionalTsData.absent("No results matching the query");

    @Nonnull
    public TsCursor<Key> getAllSeries(SdmxConnection conn, DataflowRef flowRef, Key ref, @Nullable String labelAttribute) throws IOException {
        return conn.isSeriesKeysOnlySupported()
                ? request(conn, flowRef, ref, labelAttribute, true)
                : computeKeys(conn, flowRef, ref);
    }

    @Nonnull
    public TsCursor<Key> getAllSeriesWithData(SdmxConnection conn, DataflowRef flowRef, Key ref, @Nullable String labelAttribute) throws IOException {
        return conn.isSeriesKeysOnlySupported()
                ? request(conn, flowRef, ref, labelAttribute, false)
                : computeKeysAndRequestData(conn, flowRef, ref);
    }

    @Nonnull
    public TsCursor<Key> getSeriesWithData(SdmxConnection conn, DataflowRef flowRef, Key ref, @Nullable String labelAttribute) throws IOException {
        return request(conn, flowRef, ref, labelAttribute, false);
    }

    @Nonnull
    public List<String> getChildren(SdmxConnection conn, DataflowRef flowRef, Key ref, int dimensionPosition) throws IOException {
        if (conn.isSeriesKeysOnlySupported()) {
            try (TsCursor<Key> cursor = request(conn, flowRef, ref, NO_LABEL, true)) {
                int index = dimensionPosition - 1;
                TreeSet<String> result = new TreeSet<>();
                while (cursor.nextSeries()) {
                    result.add(cursor.getSeriesId().get(index));
                }
                return ImmutableList.copyOf(result);
            }
        }

        return computeAllPossibleChildren(dimensionByIndex(conn.getStructure(flowRef)), dimensionPosition);
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private TsCursor<Key> request(SdmxConnection conn, DataflowRef flowRef, Key key, String labelAttribute, boolean seriesKeysOnly) throws IOException {
        return new SdmxDataAdapter(key, conn.getCursor(flowRef, DataQuery.of(key, seriesKeysOnly)), labelAttribute);
    }

    private TsCursor<Key> computeKeys(SdmxConnection conn, DataflowRef flowRef, Key key) throws IOException {
        List<Key> list = computeAllPossibleSeries(dimensionByIndex(conn.getStructure(flowRef)), key);
        return TsCursor.from(list.iterator());
    }

    private TsCursor<Key> computeKeysAndRequestData(SdmxConnection conn, DataflowRef flowRef, Key key) throws IOException {
        List<Key> list = computeAllPossibleSeries(dimensionByIndex(conn.getStructure(flowRef)), key);
        Map<Key, OptionalTsData> dataByKey = dataByKey(conn, flowRef, key);
        return TsCursor.from(list.iterator(), o -> dataByKey.getOrDefault(o, MISSING_DATA));
    }

    private Map<Key, OptionalTsData> dataByKey(SdmxConnection conn, DataflowRef flowRef, Key key) throws IOException {
        Map<Key, OptionalTsData> dataByKey = new HashMap<>();
        try (TsCursor<Key> cursor = request(conn, flowRef, key, NO_LABEL, false)) {
            while (cursor.nextSeries()) {
                dataByKey.put(cursor.getSeriesId(), cursor.getSeriesData());
            }
        }
        return dataByKey;
    }

    private Dimension[] dimensionByIndex(DataStructure ds) {
        Dimension[] result = new Dimension[ds.getDimensions().size()];
        ds.getDimensions().forEach(o -> result[o.getPosition() - 1] = o);
        return result;
    }

    private List<Key> computeAllPossibleSeries(Dimension[] dimensionByIndex, Key ref) {
        List<Key> result = new ArrayList<>();
        String[] stack = new String[dimensionByIndex.length];
        computeAllPossibleSeries(i -> getCodeList(dimensionByIndex, ref, i), 0, stack, result);
        return result;
    }

    private Set<String> getCodeList(Dimension[] dimensionByIndex, Key ref, int idx) {
        return Key.ALL.equals(ref) || ref.isWildcard(idx)
                ? dimensionByIndex[idx].getCodes().keySet()
                : Collections.singleton(ref.get(idx));
    }

    private void computeAllPossibleSeries(IntFunction<Set<String>> codeLists, int idx, String[] stack, List<Key> result) {
        codeLists.apply(idx).forEach(code -> {
            stack[idx] = code;
            if (idx == stack.length - 1) {
                result.add(Key.of(stack));
            } else {
                computeAllPossibleSeries(codeLists, idx + 1, stack, result);
            }
        });
    }

    private List<String> computeAllPossibleChildren(Dimension[] dimensionByPosition, int dimensionPosition) {
        Dimension dimension = dimensionByPosition[dimensionPosition - 1];
        return dimension.getCodes().keySet().stream().sorted().collect(Collectors.toList());
    }
    //</editor-fold>
}
