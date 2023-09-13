package be.nbb.demetra.sdmx.web.actions;

import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.nbdemetra.ui.tsproviders.DataSourceNode;
import ec.tss.tsproviders.DataSource;
import ec.ui.ExtAction;
import ec.util.desktop.Desktop;
import ec.util.desktop.DesktopManager;
import internal.jd3.AbilityNodeAction3;
import internal.jd3.TsManager3;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.web.WebSource;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

@ActionID(category = "Edit", id = OpenWebsiteAction.ID)
@ActionRegistration(displayName = "#CTL_OpenWebsiteAction", lazy = false)
@Messages("CTL_OpenWebsiteAction=Open web site")
@ActionReference(path = DataSourceNode.ACTION_PATH, position = 1720, separatorBefore = 1700, id = @ActionID(category = "Edit", id = OpenWebsiteAction.ID))
public final class OpenWebsiteAction extends AbilityNodeAction3<DataSource> implements Presenter.Popup {

    static final String ID = "be.nbb.demetra.sdmx.web.actions.OpenWebsiteAction";

    public OpenWebsiteAction() {
        super(DataSource.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ExtAction.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<DataSource> items) {
        items.forEach(item -> {
            try {
                DesktopManager.get().browse(getWebsite(item).toURI());
            } catch (IOException | URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }

    @Override
    protected boolean enable(Stream<DataSource> items) {
        return DesktopManager.get().isSupported(Desktop.Action.BROWSE)
                && items.anyMatch(item -> getWebsite(item) != null);
    }

    private URL getWebsite(DataSource dataSource) {
        return TsManager3.get()
                .getProvider(SdmxWebProvider.class, dataSource)
                .map(provider -> getWebsite(provider, dataSource))
                .orElse(null);
    }

    private URL getWebsite(SdmxWebProvider provider, DataSource dataSource) {
        WebSource source = provider.getSdmxManager().getSources().get(provider.decodeBean(dataSource).getSource());
        return source != null ? source.getWebsite() : null;
    }

    @Override
    public String getName() {
        return Bundle.CTL_OpenWebsiteAction();
    }
}
