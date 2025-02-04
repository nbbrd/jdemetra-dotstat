package internal.sdmx;

import lombok.AccessLevel;
import lombok.NonNull;

// FIXME: temporary code; to be removed when the real implementation is available in next release of sdmx-dl
@lombok.Value
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CatalogRef {

    @NonNull
    String id;

    @Override
    public String toString() {
        return id;
    }

    public static @NonNull CatalogRef parse(@NonNull CharSequence input) throws IllegalArgumentException {
        return new CatalogRef(input.toString());
    }

    public static final CatalogRef NO_CATALOG = CatalogRef.parse("");
}
