package com.classtrack;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/** Class Attendance App - Java Swing, no external libraries needed. */
public class AttendanceApp extends JFrame {

    // ---------- Colors ----------
    static final Color PURPLE = new Color(103, 58, 183);
    static final Color TEAL = new Color(0, 150, 136);
    static final Color RED = new Color(239, 83, 80);
    static final Color ORANGE = new Color(255, 167, 38);
    static final Color GREEN = new Color(102, 187, 106);
    static final Color BLUE = new Color(66, 165, 245);
    static final Color BG = new Color(243, 240, 250);
    static final Color WEEKEND = new Color(224, 224, 224);
    static final String[] STATUS = {"Not marked", "Present", "Absent", "Late", "Excused"};

    // ---------- Data (saved to attendance.dat) ----------
    static class Student implements Serializable {
        String id, name;
        Student(String id, String name) { this.id = id; this.name = name; }
    }

    static class Event implements Serializable {
        LocalDate start, end;
        String label;
        boolean vacation;
        Event(LocalDate s, LocalDate e, String l, boolean v) { start = s; end = e; label = l; vacation = v; }
        boolean covers(LocalDate d) { return !d.isBefore(start) && !d.isAfter(end); }
    }

    static class Data implements Serializable {
        String course = "My Course";
        List<Student> students = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        Map<LocalDate, Map<String, String>> attendance = new HashMap<>();
    }

    private static final File FILE = new File("attendance.dat");
    private Data data = load();
    private LocalDate selected = LocalDate.now();
    private YearMonth month = YearMonth.now();

    // ---------- UI ----------
    private final JTextField courseField = new JTextField();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel dateInfo = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 4, 4));
    private final JLabel tableTitle = new JLabel();
    private final StudentModel model = new StudentModel();
    private final JTable table = new JTable(model);

    public AttendanceApp() {
        super("Class Attendance");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCalendar(), BorderLayout.WEST);
        add(buildTablePanel(), BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { save(); }
        });
        refreshAll();
    }

    // ---------- Header ----------
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(PURPLE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel lbl = new JLabel("Course:");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        courseField.setText(data.course);
        courseField.setFont(new Font("SansSerif", Font.BOLD, 22));
        courseField.setBackground(new Color(237, 231, 246));
        courseField.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        courseField.addActionListener(e -> { data.course = courseField.getText(); save(); });
        p.add(lbl, BorderLayout.WEST);
        p.add(courseField, BorderLayout.CENTER);
        return p;
    }

    // ---------- Calendar ----------
    private JPanel buildCalendar() {
        JPanel p = new JPanel(new BorderLayout(5, 8));
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(380, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 0),
                BorderFactory.createLineBorder(PURPLE, 2, true)));

        JButton prev = styledButton("<", PURPLE), next = styledButton(">", PURPLE);
        prev.addActionListener(e -> { month = month.minusMonths(1); refreshCalendar(); });
        next.addActionListener(e -> { month = month.plusMonths(1); refreshCalendar(); });
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        monthLabel.setForeground(PURPLE);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.add(prev, BorderLayout.WEST);
        top.add(monthLabel, BorderLayout.CENTER);
        top.add(next, BorderLayout.EAST);

        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // legend + holiday buttons
        JPanel bottom = new JPanel(new GridLayout(0, 1, 4, 4));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        dateInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel legend = new JLabel("<html><span style='color:#EF5350'>■ Holiday</span> &nbsp; "
                + "<span style='color:#FFA726'>■ Vacation</span> &nbsp; "
                + "<span style='color:#9E9E9E'>■ Weekend</span> &nbsp; "
                + "<span style='color:#673AB7'>■ Selected</span></html>", SwingConstants.CENTER);
        JButton addEv = styledButton("+ Add Holiday / Vacation", ORANGE);
        JButton delEv = styledButton("Remove Holiday / Vacation", RED);
        addEv.addActionListener(e -> addEvent());
        delEv.addActionListener(e -> removeEvent());
        bottom.add(dateInfo);
        bottom.add(legend);
        bottom.add(addEv);
        bottom.add(delEv);

        p.add(top, BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void refreshCalendar() {
        monthLabel.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        grid.removeAll();
        for (String d : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(new Font("SansSerif", Font.BOLD, 12));
            l.setForeground(TEAL);
            grid.add(l);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) grid.add(new JLabel());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            JButton b = new JButton(String.valueOf(day));
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setFont(new Font("SansSerif", Font.BOLD, 13));
            Event ev = eventOn(date);
            Color bg = new Color(232, 245, 253), fg = Color.DARK_GRAY;
            if (isWeekend(date)) bg = WEEKEND;
            if (ev != null) { bg = ev.vacation ? ORANGE : RED; fg = Color.WHITE; }
            if (date.equals(selected)) { bg = PURPLE; fg = Color.WHITE; }
            b.setBackground(bg);
            b.setForeground(fg);
            b.setBorder(date.equals(LocalDate.now())
                    ? BorderFactory.createLineBorder(GREEN, 3)
                    : BorderFactory.createLineBorder(Color.WHITE, 1));
            b.addActionListener(e -> { selected = date; refreshAll(); });
            grid.add(b);
        }
        grid.revalidate();
        grid.repaint();

        Event ev = eventOn(selected);
        String info = selected.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy"));
        if (ev != null) info += "  -  " + (ev.vacation ? "Vacation: " : "Holiday: ") + ev.label;
        else if (isWeekend(selected)) info += "  -  Weekend";
        dateInfo.setText(info);
    }

    // ---------- Students table ----------
    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(5, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));

        tableTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        tableTitle.setForeground(PURPLE);

        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 15));
        table.setGridColor(new Color(224, 218, 240));
        table.setSelectionBackground(new Color(209, 196, 233));
        table.setSelectionForeground(Color.BLACK);
        JTableHeader_style();
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(STATUS)));
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);

        JButton add = styledButton("+ Add Student", GREEN);
        JButton remove = styledButton("- Remove Student", RED);
        JButton allPresent = styledButton("Mark All Present", BLUE);
        JButton saveBtn = styledButton("Save", TEAL);
        add.addActionListener(e -> addStudent());
        remove.addActionListener(e -> removeStudent());
        allPresent.addActionListener(e -> markAllPresent());
        saveBtn.addActionListener(e -> { data.course = courseField.getText(); save();
            JOptionPane.showMessageDialog(this, "Saved!"); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(add); buttons.add(remove); buttons.add(allPresent); buttons.add(saveBtn);

        p.add(tableTitle, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private void JTableHeader_style() {
        var h = table.getTableHeader();
        h.setBackground(TEAL);
        h.setForeground(Color.WHITE);
        h.setFont(new Font("SansSerif", Font.BOLD, 15));
        h.setPreferredSize(new Dimension(0, 36));
    }

    private void addStudent() {
        JTextField id = new JTextField(), name = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Student ID:")); form.add(id);
        form.add(new JLabel("Student Name:")); form.add(name);
        if (JOptionPane.showConfirmDialog(this, form, "Add Student",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String sid = id.getText().trim(), sname = name.getText().trim();
        if (sid.isEmpty() || sname.isEmpty()) { warn("Please enter both ID and name."); return; }
        for (Student s : data.students) if (s.id.equalsIgnoreCase(sid)) { warn("That ID already exists."); return; }
        data.students.add(new Student(sid, sname));
        save();
        model.fireTableDataChanged();
    }

    private void removeStudent() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a student first."); return; }
        Student s = data.students.get(table.convertRowIndexToModel(row));
        if (JOptionPane.showConfirmDialog(this, "Remove " + s.name + " (" + s.id + ")?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        data.students.remove(s);
        data.attendance.values().forEach(m -> m.remove(s.id));
        save();
        model.fireTableDataChanged();
    }

    private void markAllPresent() {
        if (!isSchoolDay(selected)) { warn("This day is a weekend, holiday or vacation."); return; }
        Map<String, String> day = data.attendance.computeIfAbsent(selected, k -> new HashMap<>());
        for (Student s : data.students) day.put(s.id, "Present");
        save();
        model.fireTableDataChanged();
    }

    // ---------- Holidays / vacations ----------
    private void addEvent() {
        JTextField label = new JTextField();
        JComboBox<String> type = new JComboBox<>(new String[]{"Holiday", "Vacation"});
        JTextField start = new JTextField(selected.toString()), end = new JTextField(selected.toString());
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Name (e.g. Winter Break):")); form.add(label);
        form.add(new JLabel("Type:")); form.add(type);
        form.add(new JLabel("Start date (yyyy-MM-dd):")); form.add(start);
        form.add(new JLabel("End date (yyyy-MM-dd):")); form.add(end);
        if (JOptionPane.showConfirmDialog(this, form, "Add Holiday / Vacation",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            LocalDate s = LocalDate.parse(start.getText().trim()), e = LocalDate.parse(end.getText().trim());
            if (e.isBefore(s)) { warn("End date is before start date."); return; }
            String name = label.getText().trim().isEmpty() ? (String) type.getSelectedItem() : label.getText().trim();
            data.events.add(new Event(s, e, name, type.getSelectedIndex() == 1));
            save();
            refreshAll();
        } catch (DateTimeParseException ex) {
            warn("Please use the date format yyyy-MM-dd, e.g. 2026-12-25.");
        }
    }

    private void removeEvent() {
        Event ev = eventOn(selected);
        if (ev == null) { warn("Select a holiday or vacation day on the calendar first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Remove \"" + ev.label + "\" (" + ev.start + " to " + ev.end + ")?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        data.events.remove(ev);
        save();
        refreshAll();
    }

    // ---------- Helpers ----------
    private Event eventOn(LocalDate d) {
        for (Event e : data.events) if (e.covers(d)) return e;
        return null;
    }
    private boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
    private boolean isSchoolDay(LocalDate d) { return !isWeekend(d) && eventOn(d) == null; }

    private void refreshAll() {
        refreshCalendar();
        tableTitle.setText("Attendance for " + selected.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")));
        model.fireTableDataChanged();
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE); }

    private static JButton styledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- Table model ----------
    class StudentModel extends AbstractTableModel {
        final String[] cols = {"ID", "Student Name", "Status", "Attendance %"};
        public int getRowCount() { return data.students.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public boolean isCellEditable(int r, int c) { return c == 2 && isSchoolDay(selected); }

        public Object getValueAt(int r, int c) {
            Student s = data.students.get(r);
            switch (c) {
                case 0: return s.id;
                case 1: return s.name;
                case 2:
                    if (!isSchoolDay(selected)) {
                        Event ev = eventOn(selected);
                        return ev != null ? ev.label : "Weekend";
                    }
                    return data.attendance.getOrDefault(selected, Map.of()).getOrDefault(s.id, "Not marked");
                default: return percent(s);
            }
        }

        public void setValueAt(Object v, int r, int c) {
            Student s = data.students.get(r);
            Map<String, String> day = data.attendance.computeIfAbsent(selected, k -> new HashMap<>());
            if ("Not marked".equals(v)) day.remove(s.id); else day.put(s.id, (String) v);
            save();
            fireTableRowsUpdated(r, r);
        }

        private String percent(Student s) {
            int total = 0, ok = 0;
            for (Map<String, String> day : data.attendance.values()) {
                String st = day.get(s.id);
                if (st == null || st.equals("Excused")) continue;
                total++;
                if (st.equals("Present") || st.equals("Late")) ok++;
            }
            return total == 0 ? "-" : Math.round(100.0 * ok / total) + "%";
        }
    }

    static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setHorizontalAlignment(CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            String s = String.valueOf(v);
            Color bg = Color.WHITE;
            switch (s) {
                case "Present": bg = new Color(200, 230, 201); break;
                case "Absent": bg = new Color(255, 205, 210); break;
                case "Late": bg = new Color(255, 236, 179); break;
                case "Excused": bg = new Color(187, 222, 251); break;
                case "Not marked": bg = Color.WHITE; break;
                default: bg = new Color(238, 238, 238);
            }
            setBackground(bg);
            setForeground(Color.DARK_GRAY);
            return this;
        }
    }

    // ---------- Save / load ----------
    private void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            data.course = courseField.getText();
            out.writeObject(data);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static Data load() {
        if (FILE.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
                return (Data) in.readObject();
            } catch (Exception e) { e.printStackTrace(); }
        }
        return new Data();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AttendanceApp().setVisible(true));
    }
}