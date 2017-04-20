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
package be.nbb.sdmx.facade.util;

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.TimeFormat;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
public final class NoOpCursor implements DataCursor {

    @Nonnull
    public static DataCursor noOp() {
        return INSTANCE;
    }

    private static final NoOpCursor INSTANCE = new NoOpCursor();

    @Override
    public boolean nextSeries() throws IOException {
        return false;
    }

    @Override
    public boolean nextObs() throws IOException {
        return false;
    }

    @Override
    public Key getSeriesKey() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public TimeFormat getSeriesTimeFormat() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public Map<String, String> getSeriesAttributes() throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Date getObsPeriod() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public Double getObsValue() throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException {
    }
}