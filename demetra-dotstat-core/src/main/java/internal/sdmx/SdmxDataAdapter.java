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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 *
 * @author Philippe Charles
 */
final class SdmxDataAdapter implements TsCursor<Key> {

    private final Calendar calendar;
    private final Key group;
    private final DataCursor cursor;
    private boolean closed;
    private Key currentKey;

    public SdmxDataAdapter(Key group, DataCursor cursor) {
        this.calendar = new GregorianCalendar();
        this.group = group;
        this.cursor = cursor;
        this.closed = false;
        this.currentKey = null;
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
    public String getSeriesLabel() throws IOException, IllegalStateException {
        return currentKey.toString();
    }

    @Override
    public OptionalTsData getSeriesData() throws IOException {
        return toData(cursor);
    }

    @Override
    public Map<String, String> getSeriesMetaData() throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        cursor.close();
    }

    private OptionalTsData toData(DataCursor cursor) throws IOException {
        OptionalTsData.Builder2 data = OptionalTsData.builderByDate(calendar, getObsGathering(cursor.getTimeFormat()));
        while (cursor.nextObs()) {
            Date period = cursor.getPeriod();
            Number value = period != null ? cursor.getValue() : null;
            data.add(period, value);
        }
        return data.build();
    }

    private ObsGathering getObsGathering(TimeFormat format) {
        switch (format) {
            case YEARLY:
                return ObsGathering.includingMissingValues(TsFrequency.Yearly, TsAggregationType.None);
            case HALF_YEARLY:
                return ObsGathering.includingMissingValues(TsFrequency.HalfYearly, TsAggregationType.None);
            case QUADRI_MONTHLY:
                return ObsGathering.includingMissingValues(TsFrequency.QuadriMonthly, TsAggregationType.None);
            case QUARTERLY:
                return ObsGathering.includingMissingValues(TsFrequency.Quarterly, TsAggregationType.None);
            case MONTHLY:
                return ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.None);
            case WEEKLY:
                return ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last);
            case DAILY:
                return ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last);
            case HOURLY:
                return ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last);
            case MINUTELY:
                return ObsGathering.includingMissingValues(TsFrequency.Monthly, TsAggregationType.Last);
            default:
                return ObsGathering.includingMissingValues(TsFrequency.Undefined, TsAggregationType.None);
        }
    }
}
