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

import sdmxdl.Key;
import sdmxdl.Frequency;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.ObsGathering;
import ec.tss.tsproviders.utils.OptionalTsData;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.stream.Stream;
import nbbrd.io.IOIterator;
import nbbrd.io.function.IORunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import sdmxdl.Series;

/**
 *
 * @author Philippe Charles
 */
final class SdmxDataAdapter implements TsCursor<Key> {

    private final Key ref;
    private final IOIterator<Series> cursor;
    private final Closeable closeable;
    private final String labelAttribute;
    private boolean closed;
    private Series currentSeries;
    private Calendar calendar;
    private ZoneId zoneId;

    SdmxDataAdapter(@NonNull Key ref, @NonNull Stream<Series> cursor, @Nullable String labelAttribute) {
        this.ref = ref;
        this.cursor = IOIterator.checked(cursor.iterator());
        this.closeable = IORunnable.checked(cursor::close).asCloseable();
        this.labelAttribute = labelAttribute;
        this.closed = false;
        this.currentSeries = null;
        this.calendar = null;
        this.zoneId = null;
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
        return currentSeries.getFreq().hasTime() ? toDataByDate() : toDataByLocalDate();
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

    private OptionalTsData toDataByDate() throws IOException {
        if (calendar == null || zoneId == null) {
            calendar = new GregorianCalendar();
            zoneId = ZoneId.systemDefault();
        }
        return OptionalTsData
                .builderByDate(calendar, GATHERINGS.get(currentSeries.getFreq()))
                .addAll(currentSeries.getObs().stream(), obs -> toDate(obs.getPeriod()), obs -> obs.getValue())
                .build();
    }

    private Date toDate(LocalDateTime dateTime) {
        return dateTime != null ? Date.from(dateTime.atZone(zoneId).toInstant()) : null;
    }

    private OptionalTsData toDataByLocalDate() throws IOException {
        return OptionalTsData
                .builderByLocalDate(GATHERINGS.get(currentSeries.getFreq()))
                .addAll(currentSeries.getObs().stream(), obs -> toLocalDate(obs.getPeriod()), obs -> obs.getValue())
                .build();
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    private static final Map<Frequency, ObsGathering> GATHERINGS = initGatherings();

    private static Map<Frequency, ObsGathering> initGatherings() {
        Map<Frequency, ObsGathering> result = new EnumMap<>(Frequency.class);
        result.put(Frequency.ANNUAL, ObsGathering.includingMissingValues(TsFrequency.Yearly, TsAggregationType.Last));
        result.put(Frequency.HALF_YEARLY, ObsGathering.includingMissingValues(TsFrequency.HalfYearly, TsAggregationType.Last));
        result.put(Frequency.QUARTERLY, ObsGathering.includingMissingValues(TsFrequency.Quarterly, TsAggregationType.Last));
        result.put(Frequency.MONTHLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.WEEKLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.DAILY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.HOURLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.DAILY_BUSINESS, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.MINUTELY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(Frequency.UNDEFINED, ObsGathering.includingMissingValues(TsFrequency.Undefined, TsAggregationType.None));
        return result;
    }
}
