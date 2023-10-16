package internal.jd3;

import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceProvider;
import ec.tss.tsproviders.TsProviders;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public final class TsManager3 {

    public static TsManager3 get() {
        return INSTANCE;
    }

    private static final TsManager3 INSTANCE = new TsManager3();

    private TsManager3() {
    }

    public <T extends IDataSourceProvider> @NonNull Optional<T> getProvider(@NonNull Class<T> type) {
        return TsProviders.all()
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    public <T extends IDataSourceProvider> @NonNull Optional<T> getProvider(@NonNull Class<T> type, @NonNull DataSource dataSource) {
        return TsProviders.lookup(type, dataSource)
                .toJavaUtil();
    }
}
