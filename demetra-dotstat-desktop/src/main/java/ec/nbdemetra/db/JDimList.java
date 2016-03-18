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
package ec.nbdemetra.db;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.AutoCompletionSources;
import ec.util.list.swing.DefaultOrderingListModel;
import ec.util.list.swing.ListSelectionModels;
import ec.util.list.swing.JOrderingList;
import ec.util.list.swing.JOrderingListCommands;
import ec.util.list.swing.LabelListCellRenderer;
import ec.util.various.swing.BasicSwingLauncher;
import ec.util.various.swing.FontAwesome;
import ec.util.various.swing.JCommand;
import ec.util.various.swing.OverlayRenderer;
import ec.util.various.swing.ext.FontAwesomeUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.LayerUI;
import org.openide.util.Exceptions;

/**
 *
 * @author Philippe Charles
 */
final class JDimList extends JComponent {

    public static void main(String[] args) {
        new BasicSwingLauncher()
                .content(new Callable<Component>() {
                    @Override
                    public Component call() throws Exception {
                        JDimList result = new JDimList();
                        result.setDimensions("one,two,three");
                        result.setAutoCompletionSource(new AutoCompletionSource() {
                            @Override
                            public AutoCompletionSource.Behavior getBehavior(String term) {
                                return Behavior.ASYNC;
                            }

                            @Override
                            public String toString(Object value) {
                                return value.toString();
                            }

                            @Override
                            public List<?> getValues(String term) throws Exception {
                                Thread.sleep(2000);
                                return Arrays.asList("one", "two", "three");
                            }
                        });
                        return result;
                    }
                })
                .size(400, 300)
                .logLevel(Level.FINE)
                .launch();
    }

    public static final String DIMENSIONS_PROPERTY = "dimensions";
    public static final String AUTO_COMPLETION_SOURCE_PROPERTY = "autoCompletionSource";
    public static final String RUNNING_PROPERTY = "running";

    private final Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
    private final Joiner joiner = Joiner.on(',');

    private final DefaultOrderingListModel<String> model;
    private final JOrderingList<String> list;
    private String dimensions;
    private AutoCompletionSource autoCompletionSource;
    private boolean running;
    private boolean updating = false;

    public JDimList() {
        this.model = new DefaultOrderingListModel<>();
        this.list = new JOrderingList<>();
        this.dimensions = "";
        this.autoCompletionSource = AutoCompletionSources.empty();
        this.running = false;

        list.setModel(model);
        list.setCellRenderer(new LabelListCellRenderer<String>());
        list.setComponentPopupMenu(buildMenu().getPopupMenu());

        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                if (!updating) {
                    setDimensions(joiner.join(new AbstractList<String>() {
                        @Override
                        public String get(int index) {
                            return list.getModel().getElementAt(index);
                        }

                        @Override
                        public int size() {
                            return list.getModel().getSize();
                        }
                    }));
                }
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                intervalAdded(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                intervalAdded(e);
            }
        });

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case DIMENSIONS_PROPERTY:
                        onDimensionsChange();
                        break;
                    case AUTO_COMPLETION_SOURCE_PROPERTY:
                        onAutoCompletionSourceChange();
                        break;
                    case RUNNING_PROPERTY:
                        onRunningChange();
                        break;
                }
            }
        });

        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setLayout(new BorderLayout());
        add(new JLayer<>(list, new LoadingUI()));
        add(buildToolBar(), BorderLayout.EAST);
    }

    //<editor-fold defaultstate="collapsed" desc="Events handlers">
    private void onDimensionsChange() {
        updating = true;
        model.clear();
        model.addAll(splitter.splitToList(dimensions));
        updating = false;
    }

    private void onAutoCompletionSourceChange() {
    }

    private void onRunningChange() {
        list.setEnabled(!running);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter/Setters">
    public void setDimensions(String dimensions) {
        String old = this.dimensions;
        this.dimensions = dimensions != null ? dimensions : "";
        firePropertyChange(DIMENSIONS_PROPERTY, old, this.dimensions);
    }

    public String getDimensions() {
        return dimensions;
    }

    public AutoCompletionSource getAutoCompletionSource() {
        return autoCompletionSource;
    }

    public void setAutoCompletionSource(AutoCompletionSource contentProvider) {
        AutoCompletionSource old = this.autoCompletionSource;
        this.autoCompletionSource = contentProvider;
        firePropertyChange(AUTO_COMPLETION_SOURCE_PROPERTY, old, this.autoCompletionSource);
    }

    public boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        boolean old = this.running;
        this.running = running;
        firePropertyChange(RUNNING_PROPERTY, old, this.running);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private JMenu buildMenu() {
        JMenu result = new JMenu();
        result.add(JOrderingListCommands.moveUp().toAction(list)).setText("Move up");
        result.add(JOrderingListCommands.moveDown().toAction(list)).setText("Move down");
        return result;
    }

    private JToolBar buildToolBar() {
        JToolBar result = new JToolBar();
        result.setOrientation(JToolBar.VERTICAL);
        result.setFloatable(false);

        JButton button;

        button = result.add(new Magic().toAction(this));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_MAGIC, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Retrieve the default dimensions");
        button = result.add(new Add().toAction(this));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_PLUS, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Add a new dimension");
        button = result.add(new Remove().toAction(this));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_MINUS, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Remove the selected dimensions");

        result.addSeparator();
        button = result.add(JOrderingListCommands.moveUp().toAction(list));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_CARET_UP, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Move up the selected dimension");
        button = result.add(JOrderingListCommands.moveDown().toAction(list));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_CARET_DOWN, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Move down the selected dimension");

        result.addSeparator();
        button = result.add(new Help().toAction(this));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_QUESTION, BeanInfo.ICON_MONO_16x16));
        button.setToolTipText("Show help");

        return result;
    }

    private static final class Add extends JCommand<JDimList> {

        @Override
        public void execute(JDimList c) throws Exception {
            String value = JOptionPane.showInputDialog("Value");
            if (value != null && value.length() > 0) {
                c.model.add(value);
                int idx = c.model.getSize() - 1;
                c.list.getSelectionModel().setSelectionInterval(idx, idx);
            }
        }

        @Override
        public boolean isEnabled(JDimList c) {
            return !c.isRunning();
        }

        @Override
        public ActionAdapter toAction(JDimList c) {
            return super.toAction(c).withWeakPropertyChangeListener(c);
        }
    }

    private static final class Remove extends JCommand<JDimList> {

        @Override
        public void execute(JDimList c) throws Exception {
            int[] indices = ListSelectionModels.getSelectedIndices(c.list.getSelectionModel());
            for (int i = indices.length - 1; i >= 0; i--) {
                c.model.remove(indices[i]);
            }
            c.list.getSelectionModel().clearSelection();
        }

        @Override
        public boolean isEnabled(JDimList c) {
            return !c.isRunning() && !c.list.getSelectionModel().isSelectionEmpty();
        }

        @Override
        public JCommand.ActionAdapter toAction(JDimList c) {
            return super.toAction(c)
                    .withWeakPropertyChangeListener(c)
                    .withWeakListSelectionListener(c.list.getSelectionModel());
        }
    }

    private static final class Help extends JCommand<JDimList> {

        @Override
        public void execute(JDimList c) throws Exception {
        }

        @Override
        public boolean isEnabled(JDimList component) {
            return false;
        }
    }

    private static final class Magic extends JCommand<JDimList> {

        @Override
        public void execute(final JDimList c) throws Exception {
            final AutoCompletionSource acs = c.getAutoCompletionSource();
            switch (acs.getBehavior("")) {
                case ASYNC:
                    c.setRunning(true);
                    new SwingWorker<List<String>, Void>() {
                        @Override
                        protected List<String> doInBackground() throws Exception {
                            return getValuesAsStrings(acs, "");
                        }

                        @Override
                        protected void done() {
                            try {
                                c.setDimensions(c.joiner.join(get()));
                            } catch (InterruptedException | ExecutionException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                            c.setRunning(false);
                        }
                    }.execute();
                    break;
                case SYNC:
                    c.setDimensions(c.joiner.join(getValuesAsStrings(acs, "")));
                    break;
                case NONE:
                    break;
            }
        }

        @Override
        public boolean isEnabled(JDimList c) {
            return c.getAutoCompletionSource().getBehavior("") != AutoCompletionSource.Behavior.NONE;
        }

        @Override
        public ActionAdapter toAction(JDimList c) {
            return super.toAction(c)
                    .withWeakPropertyChangeListener(c, AUTO_COMPLETION_SOURCE_PROPERTY);
        }

        private static List<String> getValuesAsStrings(final AutoCompletionSource source, String term) throws Exception {
            return Lists.transform(source.getValues(term), new Function<Object, String>() {
                @Override
                public String apply(Object input) {
                    return source.toString(input);
                }
            });
        }
    }

    private static final class LoadingUI extends LayerUI<JOrderingList> implements Icon {

        private final OverlayRenderer.Label<JOrderingList> r = new OverlayRenderer.Label<>();
        private Icon iconStrongRef;

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            JOrderingList view = ((JLayer<JOrderingList>) c).getView();
            if (!view.isEnabled()) {
                if (iconStrongRef == null) {
                    iconStrongRef = FontAwesome.FA_SPINNER.getSpinningIcon(view, view.getForeground(), 24f);
                }
                JLabel renderer = r.getOverlayRenderer(view);
                renderer.setBackground(SwingColorSchemeSupport.withAlpha(view.getBackground(), 200));
                renderer.setText("<html><center><b>Loading");
                renderer.setIcon(this);
                renderer.paint(g);
            } else {
                if (iconStrongRef != null) {
                    iconStrongRef = null;
                }
                if (view.getModel().getSize() == 0) {
                    JLabel renderer = r.getOverlayRenderer(view);
                    renderer.setText("<html><center><b>No dimension defined</b><br>Use the toolbar on the right to add new values");
                    renderer.paint(g);
                } else {
                    JLabel renderer = r.getOverlayRenderer(view);
                    renderer.setIcon(FontAwesome.FA_ARROW_DOWN.getIcon(SwingColorSchemeSupport.withAlpha(Color.LIGHT_GRAY, 30), renderer.getFont().getSize2D() * 10));
                    renderer.setOpaque(false);
                    renderer.paint(g);
                }
            }
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (iconStrongRef != null) {
                iconStrongRef.paintIcon(c, g, x, y);
            }
        }

        @Override
        public int getIconWidth() {
            return iconStrongRef != null ? iconStrongRef.getIconWidth() : 0;
        }

        @Override
        public int getIconHeight() {
            return iconStrongRef != null ? iconStrongRef.getIconHeight() : 0;
        }
    }
    //</editor-fold>
}
