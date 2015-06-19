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
package be.nbb.sdmx.bancaditalia;

import be.nbb.sdmx.DataCursor;
import be.nbb.sdmx.Key;
import be.nbb.sdmx.TimeFormat;
import be.nbb.sdmx.util.ObsParser;
import ec.tss.tsproviders.utils.Parsers;
import ec.tss.tsproviders.utils.Parsers.Parser;
import static ec.tss.tsproviders.utils.StrangeParsers.yearFreqPosParser;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
final class DataCursorAdapter extends DataCursor {

    private final Iterator<PortableTimeSeries> data;
    private final ObsParser obs;
    private PortableTimeSeries current;
    private int index;

    public DataCursorAdapter(List<PortableTimeSeries> data) {
        this.data = data.iterator();
        this.obs = new ObsParser();
    }

    @Override
    public boolean nextSeries() throws IOException {
        boolean result = data.hasNext();
        if (result) {
            current = data.next();
            obs.timeFormat(getTimeFormat(current));
            index = -1;
        }
        return result;
    }

    @Override
    public boolean nextObs() throws IOException {
        index++;
        return index < current.getObservations().size();
    }

    @Override
    public Key getKey() throws IOException {
        return parseKey(current);
    }

    @Override
    public TimeFormat getTimeFormat() throws IOException {
        return obs.getTimeFormat();
    }

    @Override
    public Date getPeriod() throws IOException {
        return obs.withPeriod(current.getTimeSlots().get(index)).getPeriod();
    }

    @Override
    public Double getValue() throws IOException {
        return current.getObservations().get(index);
    }

    @Override
    public void close() throws IOException {
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static TimeFormat getTimeFormat(PortableTimeSeries input) {
        String value = splitKeyValue(input.getAttributes()).get("TIME_FORMAT");
        if (value != null) {
            return TimeFormat.parseByTimeFormat(value);
        }
        if (input.getFrequency() != null) {
            return TimeFormat.parseByFrequencyCodeId(input.getFrequency());
        }
        return TimeFormat.UNDEFINED;
    }
    
    private static Map<String, String> splitKeyValue(List<String> input) {
        Map<String, String> result = new HashMap<>();
        for (String o : input) {
            String[] items = o.split("=");
            result.put(items[0], items[1]);
        }
        return result;
    }
    
    private static Parser<Date> getParser(TimeFormat format) {
        switch (format) {
            case YEARLY:
                return onStrictDatePattern("yyyy").or(onStrictDatePattern("yyyy'-01'")).or(onStrictDatePattern("yyyy'-A1'"));
            case HALF_YEARLY:
                return yearFreqPosParser().or(onStrictDatePattern("yyyy-MM"));
            case QUADRI_MONTHLY:
                return yearFreqPosParser().or(onStrictDatePattern("yyyy-MM"));
            case QUARTERLY:
                return yearFreqPosParser().or(onStrictDatePattern("yyyy-MM"));
            case MONTHLY:
                return yearFreqPosParser().or(onStrictDatePattern("yyyy-MM"));
            case WEEKLY:
                return onStrictDatePattern("yyyy-MM-dd");
            case DAILY:
                return onStrictDatePattern("yyyy-MM-dd");
            case HOURLY:
                return onStrictDatePattern("yyyy-MM-dd");
            case MINUTELY:
                return onStrictDatePattern("yyyy-MM-dd");
            default:
                return onStrictDatePattern("yyyy-MM");
        }
    }
    
    private static Parsers.Parser<Date> onStrictDatePattern(String datePattern) {
        final DateFormat dateFormat = new SimpleDateFormat(datePattern, Locale.ROOT);
        dateFormat.setLenient(false);
        return new Parsers.FailSafeParser<Date>() {
            @Override
            protected Date doParse(CharSequence input) throws Exception {
                String inputAsString = input.toString();
                Date result = dateFormat.parse(inputAsString);
                return result != null && inputAsString.equals(dateFormat.format(result)) ? result : null;
            }
        };
    }
    
    @Nonnull
    private static Key parseKey(PortableTimeSeries ts) {
        List<String> dimensions = ts.getDimensions();
        if (dimensions.isEmpty()) {
            return Key.ALL;
        }
        String[] result = new String[dimensions.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = dimensions.get(i).split("=")[1];
        }
        return Key.valueOf(result);
    }
    //</editor-fold>
}
