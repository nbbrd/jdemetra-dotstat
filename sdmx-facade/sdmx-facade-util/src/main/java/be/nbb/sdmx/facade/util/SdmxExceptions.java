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

import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.DataflowRef;
import java.io.IOException;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public final class SdmxExceptions {

    @Nonnull
    public IOException connectionClosed() {
        return new IOException("Connection already closed");
    }

    @Nonnull
    public IOException missingFlow(@Nonnull DataflowRef ref) {
        return new IOException("Missing dataflow '" + ref + "'");
    }

    @Nonnull
    public IOException missingStructure(@Nonnull DataStructureRef ref) {
        return new IOException("Missing datastructure '" + ref + "'");
    }

    @Nonnull
    public IOException missingData(@Nonnull DataflowRef ref) {
        return new IOException("Missing data '" + ref + "'");
    }
}
