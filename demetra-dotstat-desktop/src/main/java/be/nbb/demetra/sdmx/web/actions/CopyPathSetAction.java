package be.nbb.demetra.sdmx.web.actions;

import be.nbb.demetra.sdmx.web.SdmxWebBean;
import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.nbdemetra.ui.tsproviders.CollectionNode;
import ec.nbdemetra.ui.tsproviders.SeriesNode;
import ec.tss.tsproviders.DataSet;
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
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.Connection;
import sdmxdl.FlowRef;
import sdmxdl.Key;
import sdmxdl.Structure;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ActionID(category = "Edit", id = CopyPathSetAction.ID)
@ActionRegistration(displayName = "#CTL_CopyPathSetAction", lazy = false)
@Messages("CTL_CopyPathSetAction=Copy Path/Reference...")
@ActionReferences({
        @ActionReference(path = CollectionNode.ACTION_PATH, position = 1422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID)),
        @ActionReference(path = SeriesNode.ACTION_PATH, position = 1422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID))
})
public final class CopyPathSetAction extends AbilityNodeAction3<DataSet> implements Presenter.Popup {

    static final String ID = "be.nbb.demetra.sdmx.web.actions.CopyPathSetAction";

    public CopyPathSetAction() {
        super(DataSet.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ExtAction.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<DataSet> items) {
        DataSet item = single(items).orElseThrow(NoSuchElementException::new);
        SdmxWebProvider provider = providerOf(item.getDataSource()).orElseThrow(NoSuchElementException::new);
        SdmxWebBean bean = provider.decodeBean(item.getDataSource());
        FlowRef flowRef = FlowRef.parse(bean.getFlow());
        Key key = getKey(provider, bean.getSource(), flowRef, item);
        CatalogRef catalog = CatalogRef.NO_CATALOG;
        new OnDemandMenuBuilder()
                .copyToClipboard("SDMX-DL URI", SdmxURI.dataSetURI(bean.getSource(), flowRef, key, catalog))
                .copyToClipboard("Source", bean.getSource())
                .copyToClipboard("Flow", flowRef.toString())
                .copyToClipboard("Key", key.toString())
                .addSeparator()
                .copyToClipboard("Fetch data command", SdmxCommand.fetchData(catalog, bean.getSource(), flowRef.toString(), key))
                .copyToClipboard("Fetch meta command", SdmxCommand.fetchMeta(catalog, bean.getSource(), flowRef.toString(), key))
                .copyToClipboard("Fetch keys command", SdmxCommand.fetchKeys(catalog, bean.getSource(), flowRef.toString(), key))
                .showMenuAsPopup(null);
    }

    @Override
    protected boolean enable(Stream<DataSet> items) {
        Optional<DataSet> item = single(items);
        return item.isPresent() && providerOf(item.get().getDataSource()).isPresent();
    }

    @Override
    public String getName() {
        return Bundle.CTL_CopyPathSetAction();
    }

    private static Optional<SdmxWebProvider> providerOf(DataSource dataSource) {
        return TsManager3.get().getProvider(SdmxWebProvider.class, dataSource);
    }

    private static <T> Optional<T> single(Stream<T> items) {
        List<T> list = items.collect(Collectors.toList());
        return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
    }

    private static Key getKey(SdmxWebProvider provider, String source, FlowRef flowRef, DataSet dataSet) {
        try (Connection connection = provider.getSdmxManager().getConnection(source, provider.getLanguages())) {
            Structure structure = connection.getStructure(flowRef);
            Key.Builder result = Key.builder(structure);
            structure.getDimensions().forEach(dimension -> result.put(dimension.getId(), dataSet.get(dimension.getId())));
            return result.build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
