/*
 * Copyright 2016 National Bank of Belgium
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

import internal.util.HasCacheSupport;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
public interface HasCache {

    @Nonnull
    ConcurrentMap getCache();

    void setCache(@Nullable ConcurrentMap cache);

    @Nonnull
    static HasCache of(@Nonnull Supplier<ConcurrentMap> defaultCache) {
        return of(defaultCache, (oldObj, newObj) -> {
        });
    }

    @Nonnull
    static HasCache of(@Nonnull Supplier<ConcurrentMap> defaultCache, @Nonnull BiConsumer<ConcurrentMap, ConcurrentMap> onCacheChange) {
        ConcurrentMap cache = defaultCache.get();
        onCacheChange.accept(null, cache);
        return new HasCacheSupport(defaultCache, new AtomicReference<>(cache), onCacheChange);
    }
}
