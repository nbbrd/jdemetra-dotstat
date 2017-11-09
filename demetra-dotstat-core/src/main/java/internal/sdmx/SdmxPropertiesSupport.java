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

import be.nbb.demetra.sdmx.HasSdmxProperties;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.AccessLevel;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SdmxPropertiesSupport implements HasSdmxProperties {

    public static SdmxPropertiesSupport of(Supplier<SdmxConnectionSupplier> defaultSupplier, Runnable onSupplierChange, Supplier<LanguagePriorityList> defaultLanguages, Runnable onLanguagesChange) {
        return new SdmxPropertiesSupport(
                defaultSupplier,
                new AtomicReference<>(defaultSupplier.get()),
                onSupplierChange,
                defaultLanguages,
                new AtomicReference<>(defaultLanguages.get()),
                onLanguagesChange);
    }

    private final Supplier<SdmxConnectionSupplier> defaultSupplier;
    private final AtomicReference<SdmxConnectionSupplier> supplier;
    private final Runnable onSupplierChange;

    private final Supplier<LanguagePriorityList> defaultLanguages;
    private final AtomicReference<LanguagePriorityList> languages;
    private final Runnable onLanguagesChange;

    @Override
    public SdmxConnectionSupplier getConnectionSupplier() {
        return supplier.get();
    }

    @Override
    public void setConnectionSupplier(SdmxConnectionSupplier connectionSupplier) {
        SdmxConnectionSupplier old = this.supplier.get();
        if (this.supplier.compareAndSet(old, connectionSupplier != null ? connectionSupplier : defaultSupplier.get())) {
            onSupplierChange.run();
        }
    }

    @Override
    public LanguagePriorityList getLanguages() {
        return languages.get();
    }

    @Override
    public void setLanguages(LanguagePriorityList languages) {
        LanguagePriorityList old = this.languages.get();
        if (this.languages.compareAndSet(old, languages != null ? languages : defaultLanguages.get())) {
            onLanguagesChange.run();
        }
    }
}
