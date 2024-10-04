package be.nbb.demetra.sdmx;

import java.util.Properties;

public enum Toggle {

    DEFAULT, DISABLE, ENABLE;

    public void applyTo(Properties properties, CharSequence key) {
        applyTo(properties, key, "false", "true");
    }

    public void applyTo(Properties properties, CharSequence key, String disableValue, String enableValue) {
        switch (this) {
            case DEFAULT:
                properties.remove(key.toString());
                break;
            case DISABLE:
                properties.setProperty(key.toString(), disableValue);
                break;
            case ENABLE:
                properties.setProperty(key.toString(), enableValue);
                break;
        }
    }
}
