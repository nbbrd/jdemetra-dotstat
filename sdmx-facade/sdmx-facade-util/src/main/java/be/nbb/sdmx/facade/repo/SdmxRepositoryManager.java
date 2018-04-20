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
package be.nbb.sdmx.facade.repo;

import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnection;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import be.nbb.sdmx.facade.SdmxManager;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder")
public final class SdmxRepositoryManager implements SdmxManager {

    private final AtomicReference<LanguagePriorityList> languages = new AtomicReference<>(LanguagePriorityList.ANY);

    @lombok.NonNull
    @lombok.Singular
    List<SdmxRepository> repositories;

    @Override
    public SdmxConnection getConnection(String name) throws IOException {
        Objects.requireNonNull(name);

        return repositories.stream()
                .filter(o -> o.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IOException("Cannot find '" + name + "'"))
                .asConnection();
    }

    @Override
    public LanguagePriorityList getLanguages() {
        return languages.get();
    }

    @Override
    public void setLanguages(LanguagePriorityList languages) {
        this.languages.set(languages != null ? languages : LanguagePriorityList.ANY);
    }
}
