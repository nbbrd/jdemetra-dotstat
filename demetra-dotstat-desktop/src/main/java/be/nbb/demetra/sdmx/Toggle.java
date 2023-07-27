package be.nbb.demetra.sdmx;

import java.util.Properties;

public enum Toggle {

    DEFAULT, DISABLE, ENABLE;

    public void applyTo(Properties properties, CharSequence key) {
        switch (this) {
            case DEFAULT:
                properties.remove(key.toString());
                break;
            case DISABLE:
                properties.setProperty(key.toString(), Boolean.toString(false));
                break;
            case ENABLE:
                properties.setProperty(key.toString(), Boolean.toString(true));
                break;
        }
    }
}
