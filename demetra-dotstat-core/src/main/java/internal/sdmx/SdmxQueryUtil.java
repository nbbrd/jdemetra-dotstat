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

import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.ObsCharacteristics;
import ec.tss.tsproviders.utils.ObsGathering;
import ec.tss.tsproviders.utils.OptionalTsData;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import standalone_sdmxdl.nbbrd.io.IOIterator;
import standalone_sdmxdl.nbbrd.io.function.IORunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import sdmxdl.*;
import sdmxdl.ext.SdmxCubeUtil;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class SdmxQueryUtil {

    public final String NO_LABEL = null;
    public final OptionalTsData MISSING_DATA = OptionalTsData.absent("No results matching the query");

    @NonNull
    public TsCursor<Key> getAllSeries(Connection conn, FlowRef flow, Key node, @Nullable String labelAttribute) throws IOException {
        Stream<Series> result = SdmxCubeUtil.getAllSeries(conn, flow, node);
        return new SdmxDataAdapter(node, result, labelAttribute);
    }

    @NonNull
    public TsCursor<Key> getAllSeriesWithData(Connection conn, FlowRef flow, Key node, @Nullable String labelAttribute) throws IOException {
        Stream<Series> result = SdmxCubeUtil.getAllSeriesWithData(conn, flow, node);
        return new SdmxDataAdapter(node, result, labelAttribute);
    }

    @NonNull
    public TsCursor<Key> getSeriesWithData(Connection conn, FlowRef flow, Key leaf, @Nullable String labelAttribute) throws IOException {
        Optional<Series> result = SdmxCubeUtil.getSeriesWithData(conn, flow, leaf);
        return new SdmxDataAdapter(leaf, result.map(Stream::of).orElse(Stream.empty()), labelAttribute);
    }

    @NonNull
    public List<String> getChildren(Connection conn, FlowRef flow, Key node, int dimensionPosition) throws IOException {
        Stream<String> result = SdmxCubeUtil.getChildren(conn, flow, node, dimensionPosition);
        return result.sorted().collect(Collectors.toList());
    }

    private static final class SdmxDataAdapter implements TsCursor<Key> {

        private final Key ref;
        private final IOIterator<Series> cursor;
        private final Closeable closeable;
        private final String labelAttribute;
        private boolean closed;
        private Series currentSeries;

        SdmxDataAdapter(@NonNull Key ref, @NonNull Stream<Series> cursor, @Nullable String labelAttribute) {
            this.ref = ref;
            this.cursor = IOIterator.checked(cursor.iterator());
            this.closeable = IORunnable.checked(cursor::close).asCloseable();
            this.labelAttribute = labelAttribute;
            this.closed = false;
            this.currentSeries = null;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public @lombok.NonNull Map<String, String> getMetaData() {
            return Collections.emptyMap();
        }

        @Override
        public boolean nextSeries() throws IOException {
            while (cursor.hasNextWithIO()) {
                currentSeries = cursor.nextWithIO();
                if (currentSeries != null && currentSeries.getKey().isSeries() && ref.contains(currentSeries.getKey())) {
                    return true;
                }
            }
            currentSeries = null;
            return false;
        }

        @Override
        public @lombok.NonNull Key getSeriesId() {
            return currentSeries.getKey();
        }

        @Override
        public @lombok.NonNull String getSeriesLabel() throws IllegalStateException {
            if (labelAttribute != null && !labelAttribute.isEmpty()) {
                String result = currentSeries.getMeta().get(labelAttribute);
                if (result != null) {
                    return result;
                }
            }
            return currentSeries.getKey().toString();
        }

        @Override
        public @lombok.NonNull OptionalTsData getSeriesData() {
            return toDataByLocalDate();
        }

        @Override
        public @lombok.NonNull Map<String, String> getSeriesMetaData() {
            return currentSeries.getMeta();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closeable.close();
        }

        private OptionalTsData toDataByLocalDate() {
            switch (currentSeries.getObs().size()) {
                case 0:
                    return OptionalTsData.absent("No data");
                case 1:
                    Obs singleObs = currentSeries.getObs().first();
                    return OptionalTsData
                            .builderByLocalDate(getSingleGathering(singleObs), ObsCharacteristics.ORDERED)
                            .addAll(currentSeries.getObs().stream(), SdmxDataAdapter::toLocalDate, Obs::getValue)
                            .build();
                default:
                    return OptionalTsData
                            .builderByLocalDate(DEFAULT_GATHERING, ObsCharacteristics.ORDERED)
                            .addAll(currentSeries.getObs().stream(), SdmxDataAdapter::toLocalDate, Obs::getValue)
                            .build();
            }
        }

        private static LocalDate toLocalDate(Obs obs) {
            return obs.getPeriod().getStart().toLocalDate();
        }

        private static ObsGathering getSingleGathering(Obs singleObs) {
            return ObsGathering.includingMissingValues(getTsFrequency(singleObs), TsAggregationType.None);
        }

        private static final ObsGathering DEFAULT_GATHERING = ObsGathering.includingMissingValues(TsFrequency.Undefined, TsAggregationType.None);

        private static TsFrequency getTsFrequency(Obs obs) {
            switch (obs.getPeriod().getDuration().toString()) {
                case "P6M":
                    return TsFrequency.HalfYearly;
                case "P4M":
                    return TsFrequency.QuadriMonthly;
                case "P3M":
                    return TsFrequency.Quarterly;
                case "P2M":
                    return TsFrequency.BiMonthly;
                case "P1M":
                    return TsFrequency.Monthly;
                default:
                    return TsFrequency.Yearly;
            }
        }
    }
}
