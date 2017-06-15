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

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.TimeFormat;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.ObsGathering;
import ec.tss.tsproviders.utils.OptionalTsData;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
final class SdmxDataAdapter implements TsCursor<Key> {

    private final Key group;
    private final DataCursor cursor;
    private final String labelAttribute;
    private boolean closed;
    private Key currentKey;
    private Calendar calendar;
    private ZoneId zoneId;

    SdmxDataAdapter(@Nonnull Key group, @Nonnull DataCursor cursor, @Nullable String labelAttribute) {
        this.group = group;
        this.cursor = cursor;
        this.labelAttribute = labelAttribute;
        this.closed = false;
        this.currentKey = null;
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
        while (cursor.nextSeries()) {
            currentKey = cursor.getSeriesKey();
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
    public String getSeriesLabel() throws IOException, IllegalStateException {
        if (labelAttribute != null && !labelAttribute.isEmpty()) {
            String result = cursor.getSeriesAttribute(labelAttribute);
            if (result != null) {
                return result;
            }
        }
        return currentKey.toString();
    }

    @Override
    public OptionalTsData getSeriesData() throws IOException {
        return hasTime(cursor.getSeriesTimeFormat()) ? toDataByDate(cursor) : toDataByLocalDate(cursor);
    }

    @Override
    public Map<String, String> getSeriesMetaData() throws IOException {
        return cursor.getSeriesAttributes();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        cursor.close();
    }

    private OptionalTsData toDataByDate(DataCursor cursor) throws IOException {
        if (calendar == null || zoneId == null) {
            calendar = new GregorianCalendar();
            zoneId = ZoneId.systemDefault();
        }
        OptionalTsData.Builder2<Date> result = OptionalTsData.builderByDate(calendar, GATHERINGS.get(cursor.getSeriesTimeFormat()));
        while (cursor.nextObs()) {
            LocalDateTime period = cursor.getObsPeriod();
            if (period != null) {
                result.add(Date.from(period.atZone(zoneId).toInstant()), cursor.getObsValue());
            }
        }
        return result.build();
    }

    private OptionalTsData toDataByLocalDate(DataCursor cursor) throws IOException {
        OptionalTsData.Builder2<LocalDate> result = OptionalTsData.builderByLocalDate(GATHERINGS.get(cursor.getSeriesTimeFormat()));
        while (cursor.nextObs()) {
            LocalDateTime period = cursor.getObsPeriod();
            if (period != null) {
                result.add(period.toLocalDate(), cursor.getObsValue());
            }
        }
        return result.build();
    }

    private static boolean hasTime(TimeFormat format) {
        switch (format) {
            case HOURLY:
            case MINUTELY:
                return true;
            default:
                return false;
        }
    }

    private static final Map<TimeFormat, ObsGathering> GATHERINGS = initGatherings();

    private static Map<TimeFormat, ObsGathering> initGatherings() {
        Map<TimeFormat, ObsGathering> result = new EnumMap<>(TimeFormat.class);
        result.put(TimeFormat.YEARLY, ObsGathering.includingMissingValues(TsFrequency.Yearly, TsAggregationType.None));
        result.put(TimeFormat.HALF_YEARLY, ObsGathering.includingMissingValues(TsFrequency.HalfYearly, TsAggregationType.None));
        result.put(TimeFormat.QUADRI_MONTHLY, ObsGathering.includingMissingValues(TsFrequency.QuadriMonthly, TsAggregationType.None));
        result.put(TimeFormat.QUARTERLY, ObsGathering.includingMissingValues(TsFrequency.Quarterly, TsAggregationType.None));
        result.put(TimeFormat.MONTHLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.None));
        result.put(TimeFormat.WEEKLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(TimeFormat.DAILY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(TimeFormat.HOURLY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(TimeFormat.MINUTELY, ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last));
        result.put(TimeFormat.UNDEFINED, ObsGathering.includingMissingValues(TsFrequency.Undefined, TsAggregationType.None));
        return result;
    }
}
