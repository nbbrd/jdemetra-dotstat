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
package be.nbb.demetra.dotstat;

import ec.tss.tsproviders.utils.OptionalTsData;
import java.io.IOException;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
abstract class TsCursor<K, EX extends Throwable> implements AutoCloseable {

    abstract public boolean nextSeries() throws EX;

    @Nonnull
    abstract public K getKey() throws EX;

    @Nonnull
    abstract public OptionalTsData getData() throws EX;

    @Override
    public void close() throws IOException {
    }

}
