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
package be.nbb.sdmx.util;

import be.nbb.sdmx.TimeFormat;
import ec.tss.tsproviders.utils.Parsers;
import static ec.tss.tsproviders.utils.StrangeParsers.yearFreqPosParser;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
public final class ObsParser {

    private Parsers.Parser<Date> periodParser;
    private TimeFormat timeFormat;
    private String period;
    private String value;

    public ObsParser() {
        timeFormat(TimeFormat.UNDEFINED);
    }

    @Nonnull
    public ObsParser timeFormat(@Nonnull TimeFormat timeFormat) {
        if (this.timeFormat != timeFormat) {
            this.timeFormat = timeFormat;
            this.periodParser = getParser(timeFormat);
        }
        return this;
    }

    @Nonnull
    public ObsParser clear() {
        this.period = null;
        this.value = null;
        return this;
    }

    @Nonnull
    public ObsParser withPeriod(@Nullable String period) {
        this.period = period;
        return this;
    }

    @Nonnull
    public ObsParser withValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    @Nonnull
    public TimeFormat getTimeFormat() {
        return timeFormat;
    }

    @Nullable
    public Date getPeriod() {
        return period != null ? periodParser.parse(period) : null;
    }

    @Nullable
    public Double getValue() {
        return value != null ? Double.parseDouble(value) : null;
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    @Nonnull
    private static Parsers.Parser<Date> getParser(@Nonnull TimeFormat format) {
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

    @Nonnull
    private static Parsers.Parser<Date> onStrictDatePattern(@Nonnull String datePattern) {
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
    //</editor-fold>
}
