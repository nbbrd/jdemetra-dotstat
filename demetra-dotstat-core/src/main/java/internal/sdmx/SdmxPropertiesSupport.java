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
import lombok.AccessLevel;
import lombok.NonNull;
import standalone_sdmxdl.nbbrd.io.function.IORunnable;
import sdmxdl.Languages;
import sdmxdl.SdmxManager;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @param <M>
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SdmxPropertiesSupport<M extends SdmxManager<?>> implements HasSdmxProperties<M> {

    public static <M extends SdmxManager<?>> HasSdmxProperties<M> of(Supplier<M> defaultManager, Runnable onManagerChange) {
        return new SdmxPropertiesSupport<>(
                defaultManager, new AtomicReference<>(defaultManager.get()), onManagerChange,
                () -> Languages.ANY, new AtomicReference<>(Languages.ANY), IORunnable.noOp().asUnchecked()
        );
    }

    private final Supplier<M> defaultManager;
    private final AtomicReference<M> manager;
    private final Runnable onManagerChange;

    private final Supplier<Languages> defaultLanguages;
    private final AtomicReference<Languages> languages;
    private final Runnable onLanguagesChange;

    @Override
    public @NonNull M getSdmxManager() {
        return manager.get();
    }

    @Override
    public void setSdmxManager(M manager) {
        M old = this.manager.get();
        if (this.manager.compareAndSet(old, manager != null ? manager : defaultManager.get())) {
            onManagerChange.run();
        }
    }

    @Override
    public @NonNull Languages getLanguages() {
        return languages.get();
    }

    @Override
    public void setLanguages(Languages languages) {
        Languages old = this.languages.get();
        if (this.languages.compareAndSet(old, languages != null ? languages : defaultLanguages.get())) {
            onLanguagesChange.run();
        }
    }
}
