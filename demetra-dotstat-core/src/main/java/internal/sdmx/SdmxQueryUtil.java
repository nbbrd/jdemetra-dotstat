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

import sdmxdl.DataflowRef;
import sdmxdl.Key;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.OptionalTsData;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import sdmxdl.Connection;
import sdmxdl.Series;
import sdmxdl.ext.SdmxCubeUtil;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxQueryUtil {

    public final String NO_LABEL = null;
    public final OptionalTsData MISSING_DATA = OptionalTsData.absent("No results matching the query");

    @NonNull
    public TsCursor<Key> getAllSeries(Connection conn, DataflowRef flow, Key node, @Nullable String labelAttribute) throws IOException {
        Stream<Series> result = SdmxCubeUtil.getAllSeries(conn, flow, node);
        return new SdmxDataAdapter(node, result, labelAttribute);
    }

    @NonNull
    public TsCursor<Key> getAllSeriesWithData(Connection conn, DataflowRef flow, Key node, @Nullable String labelAttribute) throws IOException {
        Stream<Series> result = SdmxCubeUtil.getAllSeriesWithData(conn, flow, node);
        return new SdmxDataAdapter(node, result, labelAttribute);
    }

    @NonNull
    public TsCursor<Key> getSeriesWithData(Connection conn, DataflowRef flow, Key leaf, @Nullable String labelAttribute) throws IOException {
        Optional<Series> result = SdmxCubeUtil.getSeriesWithData(conn, flow, leaf);
        return new SdmxDataAdapter(leaf, result.map(Stream::of).orElse(Stream.empty()), labelAttribute);
    }

    @NonNull
    public List<String> getChildren(Connection conn, DataflowRef flow, Key node, int dimensionPosition) throws IOException {
        Stream<String> result = SdmxCubeUtil.getChildren(conn, flow, node, dimensionPosition);
        return result.sorted().collect(Collectors.toList());
    }
}
