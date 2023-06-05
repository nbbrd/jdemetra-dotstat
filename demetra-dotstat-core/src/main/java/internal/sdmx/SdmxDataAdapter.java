/*
 * Copyright 2017 National Bank of Belgium
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
import nbbrd.io.IOIterator;
import nbbrd.io.function.IORunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import sdmxdl.Key;
import sdmxdl.Obs;
import sdmxdl.Series;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
final class SdmxDataAdapter implements TsCursor<Key> {

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
    public boolean isClosed() throws IOException {
        return closed;
    }

    @Override
    public Map<String, String> getMetaData() throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public boolean nextSeries() throws IOException {
        while (cursor.hasNextWithIO()) {
            currentSeries = cursor.nextWithIO();
            if (currentSeries.getKey().isSeries() && ref.contains(currentSeries.getKey())) {
                return true;
            }
        }
        currentSeries = null;
        return false;
    }

    @Override
    public Key getSeriesId() throws IOException {
        return currentSeries.getKey();
    }

    @Override
    public String getSeriesLabel() throws IOException, IllegalStateException {
        if (labelAttribute != null && !labelAttribute.isEmpty()) {
            String result = currentSeries.getMeta().get(labelAttribute);
            if (result != null) {
                return result;
            }
        }
        return currentSeries.getKey().toString();
    }

    @Override
    public OptionalTsData getSeriesData() throws IOException {
        return toDataByLocalDate();
    }

    @Override
    public Map<String, String> getSeriesMetaData() throws IOException {
        return currentSeries.getMeta();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        closeable.close();
    }

    private OptionalTsData toDataByLocalDate() throws IOException {
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
