package be.nbb.demetra.sdmx.web.actions;

import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.nbdemetra.ui.tsproviders.ProviderNode;
import ec.tss.tsproviders.IDataSourceProvider;
import ec.ui.ExtAction;
import internal.jd3.AbilityNodeAction3;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import org.openide.windows.TopComponent;
import sdmxdl.web.SdmxWebManager;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Stream;

@ActionID(category = "Edit", id = ListSourcesAction.ID)
@ActionRegistration(displayName = "#CTL_ListSourcesAction", lazy = false)
@Messages("CTL_ListSourcesAction=List sources")
@ActionReference(path = ProviderNode.ACTION_PATH, position = 1530, separatorBefore = 1500, id = @ActionID(category = "Edit", id = ListSourcesAction.ID))
public final class ListSourcesAction extends AbilityNodeAction3<IDataSourceProvider> implements Presenter.Popup {

    static final String ID = "be.nbb.demetra.sdmx.web.actions.ListSourcesAction";

    public ListSourcesAction() {
        super(IDataSourceProvider.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ExtAction.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<IDataSourceProvider> items) {
        items.map(SdmxWebProvider.class::cast).forEach(item -> {
            createComponent("SdmxWebSource", item.getSdmxManager());
        });
    }

    private static TopComponent createComponent(String name, SdmxWebManager sdmxManager) {
        JSdmxWebSourcePanel main = new JSdmxWebSourcePanel();
        main.setSdmxManager(sdmxManager);

        TopComponent c = new TopComponent() {
            @Override
            public int getPersistenceType() {
                return TopComponent.PERSISTENCE_NEVER;
            }
        };
        c.setName(name);
        c.setLayout(new BorderLayout());
        c.add(main, BorderLayout.CENTER);
        c.open();
        return c;
    }

    @Override
    protected boolean enable(Stream<IDataSourceProvider> items) {
        return items.anyMatch(SdmxWebProvider.class::isInstance);
    }

    @Override
    public String getName() {
        return Bundle.CTL_ListSourcesAction();
    }
}
