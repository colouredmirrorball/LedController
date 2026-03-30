package be.cmbsoft.ledcontrol.ui;

import be.cmbsoft.ledcontrol.output.LedStrip;
import be.cmbsoft.ledcontrol.output.LedStripConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Swing window that lists the configured {@link LedStrip} objects and lets the
 * user add, edit and delete them.  Changes are persisted via {@link LedStripConfig}.
 *
 * <p>Call {@link #setOnStripsChanged} to receive a notification whenever the list
 * is modified so the main sketch can rebuild its outputs.
 */
public class StripConfigPanel extends JFrame {

    private final LedStripConfig config;
    private final StripTableModel tableModel;
    private final JTable table;
    private Consumer<List<LedStrip>> onStripsChanged = ignored -> {
    };

    public StripConfigPanel(LedStripConfig config) {
        super("LED Strip Configuration");
        this.config = config;
        this.tableModel = new StripTableModel(config.getStrips());
        this.table = new JTable(tableModel);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(900, 400);
        setLocationByPlatform(true);

        buildUi();
    }

    private static LedStrip copyOf(LedStrip src) {
        return new LedStrip(src.getName(), src.getStartX(), src.getStartY(),
                src.getAngleDegrees(), src.getLedCount(), src.getLedSpacingPixels(),
                src.getRemoteIp(), src.getRemotePort(), src.getSubnet(), src.getUniverse());
    }

    private static void applyTo(LedStrip src, LedStrip dst) {
        dst.setName(src.getName());
        dst.setStartX(src.getStartX());
        dst.setStartY(src.getStartY());
        dst.setAngleDegrees(src.getAngleDegrees());
        dst.setLedCount(src.getLedCount());
        dst.setLedSpacingPixels(src.getLedSpacingPixels());
        dst.setRemoteIp(src.getRemoteIp());
        dst.setRemotePort(src.getRemotePort());
        dst.setSubnet(src.getSubnet());
        dst.setUniverse(src.getUniverse());
    }

    // ---- private helpers ----

    /**
     * Register a listener called whenever the strip list changes.
     */
    public void setOnStripsChanged(Consumer<List<LedStrip>> listener) {
        this.onStripsChanged = listener;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton saveBtn = new JButton("Save");
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        buttons.add(Box.createHorizontalStrut(16));
        buttons.add(saveBtn);
        root.add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0)
                openEditor(config.getStrips().get(row));
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                config.getStrips().remove(row);
                tableModel.fireTableRowsDeleted(row, row);
                notifyChanged();
            }
        });
        saveBtn.addActionListener(e -> {
            config.save();
            JOptionPane.showMessageDialog(this, "Strip configuration saved.");
        });

        setContentPane(root);
    }

    private void openEditor(LedStrip existing) {
        LedStrip copy = existing == null ? newDefaultStrip() : copyOf(existing);
        StripEditorDialog dialog = new StripEditorDialog(this, copy);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            if (existing == null) {
                config.getStrips().add(copy);
                tableModel.fireTableRowsInserted(config.getStrips().size() - 1, config.getStrips().size() - 1);
            } else {
                int idx = config.getStrips().indexOf(existing);
                applyTo(copy, existing);
                tableModel.fireTableRowsUpdated(idx, idx);
            }
            notifyChanged();
        }
    }

    private LedStrip newDefaultStrip() {
        LedStrip newStrip = new LedStrip();
        List<LedStrip> strips = config.getStrips();
        if (!strips.isEmpty()) {
            LedStrip lastStrip = strips.get(strips.size() - 1);
            newStrip.setName(lastStrip.getName());
            newStrip.setStartX(lastStrip.getStartX() + 20);
            newStrip.setStartY(lastStrip.getStartY() + 20);
            newStrip.setRemoteIp(lastStrip.getRemoteIp());
            newStrip.setRemotePort(lastStrip.getRemotePort());
            newStrip.setSubnet(lastStrip.getSubnet());
            newStrip.setUniverse(lastStrip.getUniverse() + 1);
            newStrip.setLedCount(lastStrip.getLedCount());
            newStrip.setLedSpacingPixels(lastStrip.getLedSpacingPixels());
        }
        return newStrip;
    }

    private void notifyChanged() {
        onStripsChanged.accept(config.getStrips());
    }

    /**
     * Refreshes the table to reflect any external changes to the strip list
     * (e.g. positions updated by dragging handles in the Processing canvas).
     */
    public void refreshTable() {
        tableModel.fireTableDataChanged();
    }

    // ---- inner classes ----

    /**
     * Simple dialog for editing / creating one {@link LedStrip}.
     */
    static class StripEditorDialog extends JDialog {

        private boolean confirmed = false;

        StripEditorDialog(Frame owner, LedStrip strip) {
            super(owner, "Edit Strip", true);
            setSize(420, 380);
            setLocationRelativeTo(owner);

            JTextField nameField = new JTextField(strip.getName(), 18);
            JTextField startXField = new JTextField(String.valueOf(strip.getStartX()), 8);
            JTextField startYField = new JTextField(String.valueOf(strip.getStartY()), 8);
            JTextField angleField = new JTextField(String.valueOf(strip.getAngleDegrees()), 8);
            JTextField countField = new JTextField(String.valueOf(strip.getLedCount()), 8);
            JTextField spacingField = new JTextField(String.valueOf(strip.getLedSpacingPixels()), 8);
            JTextField ipField = new JTextField(strip.getRemoteIp(), 18);
            JTextField portField = new JTextField(String.valueOf(strip.getRemotePort()), 8);
            JTextField subnetField = new JTextField(String.valueOf(strip.getSubnet()), 4);
            JTextField universeField = new JTextField(String.valueOf(strip.getUniverse()), 4);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints lc = new GridBagConstraints();
            lc.anchor = GridBagConstraints.WEST;
            lc.insets = new Insets(3, 3, 3, 6);
            GridBagConstraints fc = new GridBagConstraints();
            fc.fill = GridBagConstraints.HORIZONTAL;
            fc.weightx = 1;
            fc.insets = new Insets(3, 0, 3, 3);
            fc.gridwidth = GridBagConstraints.REMAINDER;

            addRow(form, lc, fc, 0, "Name", nameField);
            addRow(form, lc, fc, 1, "Start X", startXField);
            addRow(form, lc, fc, 2, "Start Y", startYField);
            addRow(form, lc, fc, 3, "Angle (°)", angleField);
            addRow(form, lc, fc, 4, "LED count", countField);
            addRow(form, lc, fc, 5, "LED spacing (px)", spacingField);
            addRow(form, lc, fc, 6, "Remote IP", ipField);
            addRow(form, lc, fc, 7, "Remote port", portField);
            addRow(form, lc, fc, 8, "Subnet", subnetField);
            addRow(form, lc, fc, 9, "Universe", universeField);

            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btns.add(ok);
            btns.add(cancel);

            ok.addActionListener(e -> {
                try {
                    strip.setName(nameField.getText().trim());
                    strip.setStartX(Double.parseDouble(startXField.getText().trim()));
                    strip.setStartY(Double.parseDouble(startYField.getText().trim()));
                    strip.setAngleDegrees(Double.parseDouble(angleField.getText().trim()));
                    strip.setLedCount(Integer.parseInt(countField.getText().trim()));
                    strip.setLedSpacingPixels(Double.parseDouble(spacingField.getText().trim()));
                    strip.setRemoteIp(ipField.getText().trim());
                    strip.setRemotePort(Integer.parseInt(portField.getText().trim()));
                    strip.setSubnet(Integer.parseInt(subnetField.getText().trim()));
                    strip.setUniverse(Integer.parseInt(universeField.getText().trim()));
                    confirmed = true;
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number: " + ex.getMessage());
                }
            });
            cancel.addActionListener(e -> dispose());

            JPanel root = new JPanel(new BorderLayout());
            root.add(form, BorderLayout.CENTER);
            root.add(btns, BorderLayout.SOUTH);
            setContentPane(root);
        }

        private static void addRow(JPanel p, GridBagConstraints lc, GridBagConstraints fc,
                                   int row, String label, JComponent field) {
            lc.gridy = fc.gridy = row;
            lc.gridx = 0;
            p.add(new JLabel(label + ":"), lc);
            fc.gridx = 1;
            p.add(field, fc);
        }

        boolean isConfirmed() {
            return confirmed;
        }
    }

    /**
     * Table model that exposes the key properties of each {@link LedStrip}.
     */
    static class StripTableModel extends AbstractTableModel {

        private static final String[] COLS = {
                "Name", "Start X", "Start Y", "Angle°", "LEDs", "Spacing", "IP", "Subnet", "Universe"
        };

        private final List<LedStrip> strips;

        StripTableModel(List<LedStrip> strips) {
            this.strips = strips;
        }

        @Override
        public int getRowCount() {
            return strips.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            LedStrip s = strips.get(row);
            return switch (col) {
                case 0 -> s.getName();
                case 1 -> s.getStartX();
                case 2 -> s.getStartY();
                case 3 -> s.getAngleDegrees();
                case 4 -> s.getLedCount();
                case 5 -> s.getLedSpacingPixels();
                case 6 -> s.getRemoteIp();
                case 7 -> s.getSubnet();
                case 8 -> s.getUniverse();
                default -> "";
            };
        }
    }
}

