package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

//One layout for the car form and the job form.
public class FormRows {

    private FormRows() {
    }

    public static JPanel fields() {
        JPanel panel = new JPanel(new GridBagLayout());
        //Let this block grow and shrink with the window.
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    public static void addRow(JPanel panel, int row, String label, JTextField field) {
        //Label stays on the left and does not stretch.
        GridBagConstraints labelCell = new GridBagConstraints();
        labelCell.gridx = 0;
        labelCell.gridy = row;
        labelCell.weightx = 0;
        labelCell.anchor = GridBagConstraints.LINE_START;
        labelCell.fill = GridBagConstraints.NONE;
        labelCell.insets = new Insets(4, 4, 4, 12);

        //Left padding keeps the first letter from being clipped.
        JLabel text = new JLabel(label);
        text.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        panel.add(text, labelCell);

        //Text field takes whatever width is left, and can shrink again.
        int rowHeight = field.getPreferredSize().height;
        field.setMinimumSize(new Dimension(80, rowHeight));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));

        GridBagConstraints fieldCell = new GridBagConstraints();
        fieldCell.gridx = 1;
        fieldCell.gridy = row;
        fieldCell.weightx = 1;
        fieldCell.anchor = GridBagConstraints.LINE_START;
        fieldCell.fill = GridBagConstraints.HORIZONTAL;
        fieldCell.insets = new Insets(4, 0, 4, 4);
        panel.add(field, fieldCell);
    }

    public static JPanel footer(String hint, JButton button) {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(new JLabel(hint), BorderLayout.NORTH);
        footer.add(button, BorderLayout.EAST);
        return footer;
    }
}
