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
package internal.web.sdmx21;

import be.nbb.sdmx.facade.LanguagePriorityList;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@FunctionalInterface
public interface RestExecutor {

    @Nonnull
    InputStream execute(@Nonnull URL query, @Nonnull String mediaType, @Nonnull LanguagePriorityList langs) throws IOException;

    static RestExecutor getDefault(int readTimeout, int connectTimeout) {
        return RestExecutorImpl.of(readTimeout, connectTimeout);
    }
}
