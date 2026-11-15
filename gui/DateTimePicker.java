package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/** A calendar date button with hour and minute dropdowns. */
public final class DateTimePicker extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-uuuu");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM uuuu");
    private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final JButton dateButton = new JButton("Choose date");
    private final JComboBox<String> hours = new JComboBox<>();
    private final JComboBox<String> minutes = new JComboBox<>();
    private final JPopupMenu calendar = new JPopupMenu();
    private LocalDate selectedDate;
    private YearMonth displayedMonth = YearMonth.now();

    public DateTimePicker() {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (int hour = 0; hour < 24; hour++) {
            hours.addItem(String.format("%02d", hour));
        }
        for (int minute = 0; minute < 60; minute++) {
            minutes.addItem(String.format("%02d", minute));
        }
        hours.setSelectedIndex(-1);
        minutes.setSelectedIndex(-1);
        hours.setToolTipText("Hour (00–23)");
        minutes.setToolTipText("Minute (00–59)");
        dateButton.setToolTipText("Open calendar to choose a date");
        dateButton.addActionListener(event -> showCalendar());

        add(dateButton);
        add(new JLabel("Time:"));
        add(hours);
        add(new JLabel(":"));
        add(minutes);
    }

    public LocalDateTime getSelectedDateTime() {
        if (selectedDate == null || hours.getSelectedIndex() < 0 || minutes.getSelectedIndex() < 0) {
            return null;
        }
        return selectedDate.atTime(hours.getSelectedIndex(), minutes.getSelectedIndex());
    }

    public void clearSelection() {
        selectedDate = null;
        dateButton.setText("Choose date");
        hours.setSelectedIndex(-1);
        minutes.setSelectedIndex(-1);
    }

    private void showCalendar() {
        displayedMonth = YearMonth.from(selectedDate == null ? LocalDate.now() : selectedDate);
        rebuildCalendar();
        calendar.show(dateButton, 0, dateButton.getHeight());
    }

    private void rebuildCalendar() {
        calendar.removeAll();
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton previous = new JButton("<");
        previous.setToolTipText("Previous month");
        previous.addActionListener(event -> changeMonth(-1));
        JButton next = new JButton(">");
        next.setToolTipText("Next month");
        next.addActionListener(event -> changeMonth(1));
        JLabel month = new JLabel(displayedMonth.format(MONTH_FORMAT), SwingConstants.CENTER);
        JPanel heading = new JPanel(new BorderLayout());
        heading.add(previous, BorderLayout.WEST);
        heading.add(month, BorderLayout.CENTER);
        heading.add(next, BorderLayout.EAST);
        content.add(heading, BorderLayout.NORTH);

        JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
        for (String weekday : WEEKDAYS) {
            days.add(new JLabel(weekday, SwingConstants.CENTER));
        }
        int leadingDays = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingDays; i++) {
            days.add(new JLabel());
        }
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            JButton dayButton = new JButton(Integer.toString(day));
            dayButton.setToolTipText(date.format(DATE_FORMAT));
            if (date.equals(selectedDate)) {
                dayButton.setBorder(BorderFactory.createLineBorder(dayButton.getForeground(), 2));
            }
            dayButton.addActionListener(event -> {
                selectedDate = date;
                dateButton.setText(date.format(DATE_FORMAT));
                calendar.setVisible(false);
            });
            days.add(dayButton);
        }
        content.add(days, BorderLayout.CENTER);
        calendar.add(content);
        calendar.pack();
        calendar.revalidate();
        calendar.repaint();
    }

    private void changeMonth(int months) {
        displayedMonth = displayedMonth.plusMonths(months);
        rebuildCalendar();
    }
}
