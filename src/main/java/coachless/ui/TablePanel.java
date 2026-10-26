package coachless.ui;

import coachless.data.Names;
import coachless.data.ProgressSave;
import coachless.data.SaveFileManager;
 
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class TablePanel extends JPanel {
    private final SaveFileManager saves;
    private final DefaultTableModel model;
    private final BiFunction<List<ProgressSave>, String, List<Object[]>> rows;
    private final JComboBox<String> characterBox;   // null when there is no character filter
    private final JLabel status = new JLabel();
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField search = new JTextField(18);
    
    protected TablePanel(SaveFileManager saves, String title, String subtitle, boolean characterFilter,
        int sortColumn, String[] columns, Class<?>[] types, BiFunction<List<ProgressSave>, String, List<Object[]>> rows) {
        
        super(new BorderLayout(0, 12));
        this.saves = saves;
        this.rows = rows;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
 
        model = new DefaultTableModel(columns, 0) {
            @Override public Class<?> getColumnClass(int c) { return types[c]; }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(sortColumn, SortOrder.DESCENDING)));
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.max(table.getRowHeight(), 28));
        table.setShowVerticalLines(false);
        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            { setHorizontalAlignment(RIGHT); }
            @Override protected void setValue(Object v) {
                double d = (Double) v;
                setText(Double.isNaN(d) ? "-" : String.format("%.1f", d));
            }
        });
 
        // header
        JLabel titleLabel = new JLabel(title);
        titleLabel.putClientProperty("FlatLaf.styleClass", "h1");
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(titleLabel);
        header.add(sub);
 
        // controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search...");
        controls.add(search);
        if (characterFilter) {
            List<String> labels = new ArrayList<>();
            labels.add("All characters");
            for (String id : ProgressSave.CharacterIDs) labels.add(Names.PrettyPrint(id));
            characterBox = new JComboBox<>(labels.toArray(new String[0]));
            characterBox.addActionListener(e -> refresh());
            controls.add(characterBox);
        } else {
            characterBox = null;
        }
        status.setForeground(UIManager.getColor("Label.disabledForeground"));
        controls.add(status);
 
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applySearch(); }
            @Override public void removeUpdate(DocumentEvent e) { applySearch(); }
            @Override public void changedUpdate(DocumentEvent e) { applySearch(); }
        });
 
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);
 
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
 
        saves.addListener(this::refresh);
        refresh();
    }
 
    private void refresh() {
        String character = null;
        if (characterBox != null && characterBox.getSelectedIndex() > 0) {
            character = ProgressSave.CharacterIDs.get(characterBox.getSelectedIndex() - 1);
        }
        List<ProgressSave> valid = saves.validSaves();
        model.setRowCount(0);
        for (Object[] r : rows.apply(valid, character)) model.addRow(r);
        status.setText(valid.isEmpty()
                ? "No data yet. Add your progress.save file above."
                : model.getRowCount() + " rows");
    }
 
    private void applySearch() {
        String text = search.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0));
    }
}