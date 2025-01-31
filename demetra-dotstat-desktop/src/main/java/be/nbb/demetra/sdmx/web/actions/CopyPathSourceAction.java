package be.nbb.demetra.sdmx.web.actions;

import be.nbb.demetra.sdmx.web.SdmxWebBean;
import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.nbdemetra.ui.tsproviders.DataSourceNode;
import ec.tss.tsproviders.DataSource;
import ec.ui.ExtAction;
import internal.jd3.AbilityNodeAction3;
import internal.jd3.TsManager3;
import internal.sdmx.CatalogRef;
import internal.sdmx.OnDemandMenuBuilder;
import internal.sdmx.SdmxCommand;
import internal.sdmx.SdmxURI;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.FlowRef;
import sdmxdl.Key;

import javax.swing.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ActionID(category = "Edit", id = CopyPathSourceAction.ID)
@ActionRegistration(displayName = "#CTL_CopyPathSourceAction", lazy = false)
@Messages("CTL_CopyPathSourceAction=Copy Path/Reference...")
@ActionReference(path = DataSourceNode.ACTION_PATH, position = 1412, id = @ActionID(category = "Edit", id = CopyPathSourceAction.ID))
public final class CopyPathSourceAction extends AbilityNodeAction3<DataSource> implements Presenter.Popup {

    static final String ID = "be.nbb.demetra.sdmx.web.actions.CopyPathSourceAction";

    public CopyPathSourceAction() {
        super(DataSource.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ExtAction.hideWhenDisabled(new JMenuItem(this));
    }

    @SuppressWarnings("resource")
    @Override
    protected void performAction(Stream<DataSource> items) {
        DataSource item = single(items).orElseThrow(NoSuchElementException::new);
        SdmxWebProvider provider = providerOf(item).orElseThrow(NoSuchElementException::new);
        SdmxWebBean bean = provider.decodeBean(item);
        FlowRef flowRef = FlowRef.parse(bean.getFlow());
        CatalogRef catalog = CatalogRef.NO_CATALOG;
        new OnDemandMenuBuilder()
                .copyToClipboard("SDMX-DL URI", SdmxURI.dataSourceURI(bean.getSource(), flowRef, catalog))
                .copyToClipboard("Source", bean.getSource())
                .copyToClipboard("Flow", flowRef.toString())
                .addSeparator()
                .copyToClipboard("List dimensions command", SdmxCommand.listDimensions(catalog, bean.getSource(), flowRef))
                .copyToClipboard("List attributes command", SdmxCommand.listAttributes(catalog, bean.getSource(), flowRef))
                .copyToClipboard("Fetch all keys command", SdmxCommand.fetchKeys(catalog, bean.getSource(), bean.getFlow(), Key.ALL))
                .showMenuAsPopup(null);
    }

    @Override
    protected boolean enable(Stream<DataSource> items) {
        Optional<DataSource> item = single(items);
        return item.isPresent() && providerOf(item.get()).isPresent();
    }

    @Override
    public String getName() {
        return Bundle.CTL_CopyPathSourceAction();
    }

    private static Optional<SdmxWebProvider> providerOf(DataSource dataSource) {
        return TsManager3.get().getProvider(SdmxWebProvider.class, dataSource);
    }

    private static <T> Optional<T> single(Stream<T> items) {
        List<T> list = items.collect(Collectors.toList());
        return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
    }
}
