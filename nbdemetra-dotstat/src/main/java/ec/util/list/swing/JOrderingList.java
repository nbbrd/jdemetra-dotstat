/*
 * Copyright 2015 National Bank of Belgium
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
package ec.util.list.swing;

import ec.util.various.swing.ModernUI;
import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.DefaultListSelectionModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.MOVE;

/**
 *
 * @author Philippe Charles
 */
public final class JOrderingList<T> extends JComponent {

    public static final String MODEL_PROPERTY = "model";
    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";
    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";

    private final JList<T> list;
    private OrderingListModel<T> model;
    private ListCellRenderer<? super T> cellRenderer;
    private ListSelectionModel selectionModel;

    public JOrderingList() {
        this.list = new JList<>();
        this.model = new DefaultOrderingListModel<>();
        this.cellRenderer = new LabelListCellRenderer<>();
        this.selectionModel = new DefaultListSelectionModel();

        list.setTransferHandler(new ListItemTransferHandler());
        list.setDropMode(DropMode.INSERT);
        list.setDragEnabled(true);

        setBackground(list.getBackground());
        setForeground(list.getForeground());
        setFont(list.getFont());

        onModelChange();
        onCellRendererChange();
        onSelectionModelChange();
        onEnabledChange();

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case MODEL_PROPERTY:
                        onModelChange();
                        break;
                    case CELL_RENDERER_PROPERTY:
                        onCellRendererChange();
                        break;
                    case SELECTION_MODEL_PROPERTY:
                        onSelectionModelChange();
                        break;
                    case "enabled":
                        onEnabledChange();
                        break;
                    case "componentPopupMenu":
                        onComponentPopupMenuChange();
                        break;
                }
            }
        });

        setLayout(new BorderLayout());
        add(ModernUI.withEmptyBorders(new JScrollPane(list)));
    }

    //<editor-fold defaultstate="collapsed" desc="Events handlers">
    private void onModelChange() {
        list.setModel(model);
    }

    private void onCellRendererChange() {
        list.setCellRenderer(cellRenderer);
    }

    private void onSelectionModelChange() {
        list.setSelectionModel(selectionModel);
    }

    private void onEnabledChange() {
        list.setEnabled(isEnabled());
    }

    private void onComponentPopupMenuChange() {
        list.setComponentPopupMenu(getComponentPopupMenu());
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getters/Setters">
    @Nonnull
    public OrderingListModel<T> getModel() {
        return model;
    }

    public void setModel(@Nullable OrderingListModel<T> model) {
        OrderingListModel<T> old = this.model;
        this.model = model != null ? model : new DefaultOrderingListModel<T>();
        firePropertyChange(MODEL_PROPERTY, old, this.model);
    }

    @Nonnull
    public ListCellRenderer<? super T> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(@Nullable ListCellRenderer<? super T> cellRenderer) {
        ListCellRenderer<? super T> old = this.cellRenderer;
        this.cellRenderer = (ListCellRenderer<? super T>) (cellRenderer != null ? cellRenderer : new LabelListCellRenderer<>());
        firePropertyChange(CELL_RENDERER_PROPERTY, old, this.cellRenderer);
    }

    @Nonnull
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(@Nullable ListSelectionModel selectionModel) {
        ListSelectionModel old = this.selectionModel;
        this.selectionModel = selectionModel != null ? selectionModel : new DefaultListSelectionModel();
        firePropertyChange(SELECTION_MODEL_PROPERTY, old, this.selectionModel);
    }
    //</editor-fold>

    /**
     * Scrolls the list within an enclosing viewport to make the specified cell
     * completely visible. This calls {@code scrollRectToVisible} with the
     * bounds of the specified cell. For this method to work, the {@code JList}
     * must be within a <code>JViewport</code>.
     * <p>
     * If the given index is outside the list's range of cells, this method
     * results in nothing.
     *
     * @param index the index of the cell to make visible
     */
    public void ensureIndexIsVisible(int index) {
        list.ensureIndexIsVisible(index);
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    //http://docs.oracle.com/javase/tutorial/uiswing/dnd/dropmodedemo.html
    private static final class ListItemTransferHandler extends TransferHandler {

        private final DataFlavor flavor;

        public ListItemTransferHandler() {
            flavor = new ActivationDataFlavor(int[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of indices");
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new DataHandler(((JList) c).getSelectedIndices(), flavor.getMimeType());
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return !(!info.isDrop() || !info.isDataFlavorSupported(flavor));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!canImport(info)) {
                return false;
            }
            JList target = (JList) info.getComponent();
            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            OrderingListModel<?> model = (OrderingListModel<?>) target.getModel();
            int index = dl.getIndex();
            try {
                int[] indices = (int[]) info.getTransferable().getTransferData(flavor);
                if (indices[0] < index) {
                    index = index - indices.length;
                }
                model.move(indices, index);
                target.getSelectionModel().setSelectionInterval(index, index + indices.length - 1);
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }
    //</editor-fold>
}
