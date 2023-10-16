package internal.jd3;

import ec.nbdemetra.ui.tsproviders.DataSourceProviderBuddySupport;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class DataSourceManager3 {

    public static DataSourceManager3 get() {
        return INSTANCE;
    }

    private static final DataSourceManager3 INSTANCE = new DataSourceManager3();

    private DataSourceManager3() {
    }

    @NonNull
    public BeanEditor3 getBeanEditor(@NonNull String providerName, @NonNull String title) {
        return bean -> DataSourceProviderBuddySupport.getDefault().get(providerName).editBean(title, bean);
    }
}
