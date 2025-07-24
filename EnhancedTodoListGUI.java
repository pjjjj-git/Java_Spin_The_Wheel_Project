package vibecodingLOL;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class EnhancedTodoListGUI extends JFrame {
    private static final String FILE_NAME = "enhanced_todolist.dat";
    private static final String CONFIG_FILE = "todolist_config.dat";
    private static final Color MAIN_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_COLOR = new Color(100, 149, 237);
    private static final Color ACCENT_COLOR = new Color(255, 215, 0);
    
    private ArrayList<Task> tasks;
    private TaskTableModel tableModel;
    private JTable taskTable;
    private JTextField taskField;
    private JFormattedTextField dueDateField;
    private JTextField searchField;
    private JComboBox<Task.Priority> priorityComboBox;
    private JComboBox<String> categoryComboBox;
    private ArrayList<String> categories;
    private JComboBox<String> filterComboBox;
    private JLabel statsLabel;
    private boolean darkMode = false;
    
    private static class Task implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String description;
        private LocalDate dueDate;
        private boolean completed;
        private Priority priority;
        private String category;
        private LocalDate creationDate;
        
        public enum Priority {
            LOW("Low", new Color(144, 238, 144)), 
            MEDIUM("Medium", new Color(255, 255, 150)), 
            HIGH("High", new Color(255, 182, 193));
            
            private final String displayName;
            private final Color color;
            
            Priority(String displayName, Color color) {
                this.displayName = displayName;
                this.color = color;
            }
            
            @Override
            public String toString() {
                return displayName;
            }
            
            public Color getColor() {
                return color;
            }
        }
        
        public Task(String description, LocalDate dueDate, Priority priority, String category) {
            this.description = description;
            this.dueDate = dueDate;
            this.completed = false;
            this.priority = priority;
            this.category = category;
            this.creationDate = LocalDate.now();
        }
        
        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public LocalDate getCreationDate() { return creationDate; }
        
        public boolean isOverdue() {
            return !completed && dueDate != null && dueDate.isBefore(LocalDate.now());
        }
    }
    
    private class TaskTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columnNames = {"✓", "Description", "Due Date", "Priority", "Category", "Status"};
        private ArrayList<Task> taskList;
        
        public TaskTableModel(ArrayList<Task> taskList) {
            this.taskList = taskList;
        }
        
        @Override
        public int getRowCount() {
            return taskList.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1 || columnIndex == 2 || columnIndex == 3 || columnIndex == 4;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Task task = taskList.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return task.isCompleted();
                case 1: return task.getDescription();
                case 2: return task.getDueDate() != null ? 
                    task.getDueDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "No date";
                case 3: return task.getPriority().toString();
                case 4: return task.getCategory();
                case 5: 
                    if (task.isCompleted()) return "Completed";
                    if (task.isOverdue()) return "Overdue";
                    if (task.getDueDate() != null && task.getDueDate().equals(LocalDate.now())) return "Due Today";
                    if (task.getDueDate() != null && task.getDueDate().equals(LocalDate.now().plusDays(1))) return "Due Tomorrow";
                    return "Pending";
                default: return null;
            }
        }
        
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            Task task = taskList.get(rowIndex);
            
            try {
                switch (columnIndex) {
                    case 0: 
                        task.setCompleted((Boolean) value);
                        break;
                    case 1: 
                        task.setDescription((String) value);
                        break;
                    case 2: 
                        if (((String) value).isEmpty()) {
                            task.setDueDate(null);
                        } else {
                            task.setDueDate(LocalDate.parse((String) value, 
                                DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                        }
                        break;
                    case 3: 
                        task.setPriority(Task.Priority.valueOf(((String) value).toUpperCase()));
                        break;
                    case 4: 
                        task.setCategory((String) value);
                        if (!categories.contains((String) value)) {
                            categories.add((String) value);
                            categoryComboBox.addItem((String) value);
                        }
                        break;
                }
                fireTableCellUpdated(rowIndex, columnIndex);
                updateStats();
                saveTasksToFile();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(EnhancedTodoListGUI.this, 
                    "Invalid input: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        public void addTask(Task task) {
            taskList.add(task);
            fireTableRowsInserted(taskList.size() - 1, taskList.size() - 1);
            updateStats();
        }
        
        public void removeTask(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < taskList.size()) {
                taskList.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
                updateStats();
            }
        }
        
        public ArrayList<Task> getTasks() {
            return taskList;
        }
    }
    
    public EnhancedTodoListGUI() {
        super("Enhanced To-Do List Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Load configuration
        loadConfig();
        
        // Initialize data
        tasks = new ArrayList<>();
        categories = new ArrayList<>(Arrays.asList("Personal", "Work", "Shopping", "Other"));
        
        // Load existing tasks
        loadTasksFromFile();
        
        // Create components
        createComponents();
        
        // Add window closing listener to save tasks
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveTasksToFile();
                saveConfig();
            }
        });
        
        // Set application icon
        try {
            setIconImage(new ImageIcon(getClass().getResource("/todo_icon.png")).getImage());
        } catch (Exception e) {
            System.out.println("Icon not found, using default");
        }
        
        // Display the window
        setVisible(true);
    }
    
    private void createComponents() {
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        applyTheme(mainPanel);
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel titleLabel = new JLabel("Enhanced To-Do List", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(darkMode ? Color.WHITE : MAIN_COLOR);
        
        // Stats panel
        statsLabel = new JLabel(" ", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateStats();
        
        // Search and filter panel
        JPanel searchFilterPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Search Tasks"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTasks(); }
            public void removeUpdate(DocumentEvent e) { filterTasks(); }
            public void changedUpdate(DocumentEvent e) { filterTasks(); }
        });
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Filter:"));
        
        String[] filterOptions = {"All Tasks", "Active Tasks", "Completed Tasks", "Overdue Tasks", "Due Today", "Due Tomorrow"};
        filterComboBox = new JComboBox<>(filterOptions);
        filterComboBox.addActionListener(e -> filterTasks());
        
        JButton clearFilterButton = new JButton("Clear");
        clearFilterButton.addActionListener(e -> {
            searchField.setText("");
            filterComboBox.setSelectedIndex(0);
        });
        
        filterPanel.add(filterComboBox);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(clearFilterButton);
        
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(filterPanel, BorderLayout.EAST);
        
        searchFilterPanel.add(searchPanel);
        searchFilterPanel.add(statsLabel);
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(searchFilterPanel, BorderLayout.CENTER);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Task table
        tableModel = new TaskTableModel(tasks);
        taskTable = new JTable(tableModel);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setRowHeight(30);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(30);
        taskTable.getColumnModel().getColumn(5).setMaxWidth(100);
        
        // Set custom renderer for tasks
        taskTable.setDefaultRenderer(Object.class, new TaskRenderer());
        
        // Enable tooltips
        ToolTipManager.sharedInstance().registerComponent(taskTable);
        
        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel for adding tasks
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Add New Task"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        applyTheme(inputPanel);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Task description
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        inputPanel.add(createStyledLabel("Description:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        taskField = new JTextField(20);
        inputPanel.add(taskField, gbc);
        
        // Due date
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        inputPanel.add(createStyledLabel("Due Date:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        dueDateField = new JFormattedTextField(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        dueDateField.setColumns(10);
        inputPanel.add(dueDateField, gbc);
        
        // Calendar button
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton calendarButton = new JButton("Pick Date");
        calendarButton.addActionListener(e -> showDatePicker());
        inputPanel.add(calendarButton, gbc);
        
        // Priority
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        inputPanel.add(createStyledLabel("Priority:"), gbc);
        
        gbc.gridx = 4;
        gbc.weightx = 0.5;
        priorityComboBox = new JComboBox<>(Task.Priority.values());
        inputPanel.add(priorityComboBox, gbc);
        
        // Category
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        inputPanel.add(createStyledLabel("Category:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        categoryComboBox = new JComboBox<>(categories.toArray(new String[0]));
        categoryComboBox.setEditable(true);
        inputPanel.add(categoryComboBox, gbc);
        
        // Add button
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton addButton = createStyledButton("Add Task", MAIN_COLOR);
        addButton.addActionListener(e -> addTask());
        inputPanel.add(addButton, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton removeButton = createStyledButton("Remove Selected", new Color(220, 80, 60));
        removeButton.addActionListener(e -> removeTask());
        buttonPanel.add(removeButton);
        
        JButton completeButton = createStyledButton("Toggle Completion", new Color(60, 179, 113));
        completeButton.addActionListener(e -> toggleCompletion());
        buttonPanel.add(completeButton);
        
        JButton saveButton = createStyledButton("Save Tasks", SECONDARY_COLOR);
        saveButton.addActionListener(e -> {
            saveTasksToFile();
            JOptionPane.showMessageDialog(this, 
                "Tasks saved successfully!", 
                "Save Successful", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(saveButton);
        
        JButton clearButton = createStyledButton("Clear All", new Color(220, 80, 60));
        clearButton.addActionListener(e -> clearAllTasks());
        buttonPanel.add(clearButton);
        
        // Combine panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add the main panel to the frame
        add(mainPanel);
        
        // Set up menu bar
        setupMenuBar();
        
        // Set up keyboard shortcuts
        setupKeyboardShortcuts();
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(darkMode ? Color.WHITE : Color.BLACK);
        return label;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void applyTheme(Container container) {
        Color bgColor = darkMode ? new Color(60, 63, 65) : Color.WHITE;
        Color fgColor = darkMode ? Color.WHITE : Color.BLACK;
        
        container.setBackground(bgColor);
        
        for (Component comp : container.getComponents()) {
            if (comp instanceof Container) {
                applyTheme((Container) comp);
            }
            
            comp.setBackground(bgColor);
            
            if (comp instanceof JLabel || comp instanceof JCheckBox) {
                comp.setForeground(fgColor);
            }
            
            if (comp instanceof JTextField || comp instanceof JFormattedTextField || comp instanceof JComboBox) {
                comp.setBackground(darkMode ? new Color(69, 73, 74) : Color.WHITE);
                comp.setForeground(fgColor);
            }
            
            if (comp instanceof JButton) {
                // Keep button colors as they are specially styled
            }
        }
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        applyTheme(menuBar);
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveTasksToFile());
        
        JMenuItem exportItem = new JMenuItem("Export to CSV", KeyEvent.VK_E);
        exportItem.addActionListener(e -> exportToCSV());
        
        JMenuItem importItem = new JMenuItem("Import from CSV", KeyEvent.VK_I);
        importItem.addActionListener(e -> importFromCSV());
        
        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> {
            saveTasksToFile();
            saveConfig();
            System.exit(0);
        });
        
        fileMenu.add(saveItem);
        fileMenu.add(exportItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        
        JMenuItem addItem = new JMenuItem("Add Task", KeyEvent.VK_A);
        addItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        addItem.addActionListener(e -> addTask());
        
        JMenuItem removeItem = new JMenuItem("Remove Task", KeyEvent.VK_R);
        removeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        removeItem.addActionListener(e -> removeTask());
        
        JMenuItem completeItem = new JMenuItem("Toggle Completion", KeyEvent.VK_T);
        completeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        completeItem.addActionListener(e -> toggleCompletion());
        
        JMenuItem clearItem = new JMenuItem("Clear All Tasks", KeyEvent.VK_C);
        clearItem.addActionListener(e -> clearAllTasks());
        
        editMenu.add(addItem);
        editMenu.add(removeItem);
        editMenu.add(completeItem);
        editMenu.addSeparator();
        editMenu.add(clearItem);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        
        JMenuItem sortPriorityItem = new JMenuItem("Sort by Priority", KeyEvent.VK_P);
        sortPriorityItem.addActionListener(e -> sortTasks("priority"));
        
        JMenuItem sortCategoryItem = new JMenuItem("Sort by Category", KeyEvent.VK_C);
        sortCategoryItem.addActionListener(e -> sortTasks("category"));
        
        JMenuItem sortDueDateItem = new JMenuItem("Sort by Due Date", KeyEvent.VK_D);
        sortDueDateItem.addActionListener(e -> sortTasks("duedate"));
        
        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode", darkMode);
        darkModeItem.addActionListener(e -> toggleDarkMode());
        
        viewMenu.add(sortPriorityItem);
        viewMenu.add(sortCategoryItem);
        viewMenu.add(sortDueDateItem);
        viewMenu.addSeparator();
        viewMenu.add(darkModeItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        
        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> showAboutDialog());
        
        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts", KeyEvent.VK_K);
        shortcutsItem.addActionListener(e -> showShortcutsDialog());
        
        helpMenu.add(aboutItem);
        helpMenu.add(shortcutsItem);
        
        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    @SuppressWarnings("serial")
	private void setupKeyboardShortcuts() {
        // Add task on Enter key in task field
        taskField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addTask();
                }
            }
        });
        
        // Global shortcuts
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        
        // Add task shortcut
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "addTask");
        actionMap.put("addTask", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTask();
            }
        });
        
        // Remove task shortcut
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeTask");
        actionMap.put("removeTask", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeTask();
            }
        });
    }
    
    private void addTask() {
        String description = taskField.getText().trim();
        
        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a task description!", 
                "Empty Description", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Parse due date
        LocalDate dueDate = null;
        try {
            String dateText = dueDateField.getText().trim();
            if (!dateText.isEmpty()) {
                dueDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid date format! Please use MM/DD/YYYY", 
                "Invalid Date", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the selected priority
        Task.Priority priority = (Task.Priority) priorityComboBox.getSelectedItem();
        
        // Get the selected or entered category
        String category = (String) categoryComboBox.getSelectedItem();
        if (category != null && !category.isEmpty() && !categories.contains(category)) {
            categories.add(category);
            categoryComboBox.addItem(category);
        }
        
        // Create and add the new task
        Task newTask = new Task(description, dueDate, priority, category);
        tableModel.addTask(newTask);
        
        // Reset input fields
        taskField.setText("");
        dueDateField.setValue(null);
        priorityComboBox.setSelectedIndex(0);
        categoryComboBox.setSelectedIndex(0);
        
        // Save tasks
        saveTasksToFile();
        
        // Set focus back to task field
        taskField.requestFocus();
    }
    
    private void removeTask() {
        int selectedRow = taskTable.getSelectedRow();
        
        if (selectedRow >= 0) {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove this task?", 
                "Confirm Removal", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                tableModel.removeTask(selectedRow);
                saveTasksToFile();
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select a task to remove!", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void clearAllTasks() {
        if (tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "The task list is already empty!", 
                "No Tasks", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove ALL tasks? This cannot be undone.", 
            "Confirm Clear All", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            tasks.clear();
            tableModel.fireTableDataChanged();
            updateStats();
            saveTasksToFile();
        }
    }
    
    private void toggleCompletion() {
        int selectedRow = taskTable.getSelectedRow();
        
        if (selectedRow >= 0) {
            Task task = tasks.get(selectedRow);
            task.setCompleted(!task.isCompleted());
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
            updateStats();
            saveTasksToFile();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select a task to toggle completion!", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void filterTasks() {
        String searchText = searchField.getText().toLowerCase();
        String filterOption = (String) filterComboBox.getSelectedItem();
        
        ArrayList<Task> filteredTasks = new ArrayList<>();
        
        for (Task task : tasks) {
            boolean matchesSearch = task.getDescription().toLowerCase().contains(searchText) ||
                                   (task.getCategory() != null && task.getCategory().toLowerCase().contains(searchText));
            
            boolean matchesFilter = true;
            
            switch (filterOption) {
                case "Active Tasks":
                    matchesFilter = !task.isCompleted();
                    break;
                case "Completed Tasks":
                    matchesFilter = task.isCompleted();
                    break;
                case "Overdue Tasks":
                    matchesFilter = task.isOverdue();
                    break;
                case "Due Today":
                    matchesFilter = task.getDueDate() != null && 
                                  task.getDueDate().equals(LocalDate.now()) && 
                                  !task.isCompleted();
                    break;
                case "Due Tomorrow":
                    matchesFilter = task.getDueDate() != null && 
                                  task.getDueDate().equals(LocalDate.now().plusDays(1)) && 
                                  !task.isCompleted();
                    break;
            }
            
            if (matchesSearch && matchesFilter) {
                filteredTasks.add(task);
            }
        }
        
        tableModel = new TaskTableModel(filteredTasks);
        taskTable.setModel(tableModel);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(30);
        taskTable.getColumnModel().getColumn(5).setMaxWidth(100);
        taskTable.setDefaultRenderer(Object.class, new TaskRenderer());
        
        updateStats();
    }
    
    private void sortTasks(String sortBy) {
        ArrayList<Task> tasksCopy = new ArrayList<>(tasks);
        
        switch (sortBy) {
            case "priority":
                Collections.sort(tasksCopy, (t1, t2) -> t2.getPriority().compareTo(t1.getPriority()));
                break;
            case "category":
                Collections.sort(tasksCopy, (t1, t2) -> {
                    if (t1.getCategory() == null && t2.getCategory() == null) return 0;
                    if (t1.getCategory() == null) return 1;
                    if (t2.getCategory() == null) return -1;
                    return t1.getCategory().compareTo(t2.getCategory());
                });
                break;
            case "duedate":
                Collections.sort(tasksCopy, (t1, t2) -> {
                    if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                    if (t1.getDueDate() == null) return 1;
                    if (t2.getDueDate() == null) return -1;
                    return t1.getDueDate().compareTo(t2.getDueDate());
                });
                break;
        }
        
        tasks = tasksCopy;
        tableModel = new TaskTableModel(tasks);
        taskTable.setModel(tableModel);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(30);
        taskTable.getColumnModel().getColumn(5).setMaxWidth(100);
        taskTable.setDefaultRenderer(Object.class, new TaskRenderer());
    }
    
    private void updateStats() {
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(Task::isCompleted).count();
        int overdue = (int) tasks.stream().filter(t -> !t.isCompleted() && t.isOverdue()).count();
        int dueToday = (int) tasks.stream().filter(t -> 
            !t.isCompleted() && 
            t.getDueDate() != null && 
            t.getDueDate().equals(LocalDate.now())
        ).count();
        
        String stats = String.format(
            "Total: %d | Completed: %d (%.0f%%) | Overdue: %d | Due Today: %d",
            total, 
            completed, 
            total > 0 ? (completed * 100.0 / total) : 0,
            overdue,
            dueToday
        );
        
        statsLabel.setText(stats);
    }
    
    private void showDatePicker() {
        JDialog dateDialog = new JDialog(this, "Select Due Date", true);
        dateDialog.setSize(300, 300);
        dateDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JMonthPicker monthPicker = new JMonthPicker();
        panel.add(monthPicker, BorderLayout.CENTER);
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            LocalDate selectedDate = monthPicker.getSelectedDate();
            if (selectedDate != null) {
                dueDateField.setText(selectedDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            }
            dateDialog.dispose();
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dateDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        dateDialog.add(panel);
        dateDialog.setVisible(true);
    }
    
    private void toggleDarkMode() {
        darkMode = !darkMode;
        applyTheme(getContentPane());
        applyTheme(getJMenuBar());
        repaint();
    }
    
    private void showAboutDialog() {
        String aboutText = "<html><center><h2>Enhanced To-Do List</h2>" +
            "<p>Version 2.0</p>" +
            "<p>Created with Java Swing</p>" +
            "<p>© 2023 Todo App Inc.</p>" +
            "<p>All rights reserved</p></center></html>";
        
        JOptionPane.showMessageDialog(this, aboutText, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showShortcutsDialog() {
        String shortcuts = "<html><h3>Keyboard Shortcuts</h3>" +
            "<table>" +
            "<tr><td><b>Ctrl+N</b></td><td>Add new task</td></tr>" +
            "<tr><td><b>Delete</b></td><td>Remove selected task</td></tr>" +
            "<tr><td><b>Space</b></td><td>Toggle completion</td></tr>" +
            "<tr><td><b>Ctrl+S</b></td><td>Save tasks</td></tr>" +
            "<tr><td><b>Enter</b></td><td>Add task (when in description field)</td></tr>" +
            "</table></html>";
        
        JOptionPane.showMessageDialog(this, shortcuts, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Tasks to CSV");
        fileChooser.setSelectedFile(new File("tasks_export.csv"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(fileToSave)) {
                // Write header
                writer.println("Description,Due Date,Priority,Category,Completed,Creation Date");
                
                // Write tasks
                for (Task task : tasks) {
                    writer.println(String.format("\"%s\",%s,%s,%s,%s,%s",
                        task.getDescription(),
                        task.getDueDate() != null ? 
                            task.getDueDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "",
                        task.getPriority(),
                        task.getCategory() != null ? task.getCategory() : "",
                        task.isCompleted() ? "Yes" : "No",
                        task.getCreationDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                    ));
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Tasks exported successfully to " + fileToSave.getName(), 
                    "Export Successful", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error exporting tasks: " + e.getMessage(), 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void importFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Tasks from CSV");
        
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileToOpen))) {
                // Skip header
                reader.readLine();
                
                ArrayList<Task> importedTasks = new ArrayList<>();
                String line;
                int lineNum = 1;
                
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    try {
                        // Simple CSV parsing (won't handle commas in quoted fields)
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        
                        if (parts.length < 5) {
                            throw new Exception("Invalid number of fields on line " + lineNum);
                        }
                        
                        // Parse description (remove quotes if present)
                        String description = parts[0].trim();
                        if (description.startsWith("\"") && description.endsWith("\"")) {
                            description = description.substring(1, description.length() - 1);
                        }
                        
                        // Parse due date
                        LocalDate dueDate = null;
                        if (!parts[1].trim().isEmpty()) {
                            dueDate = LocalDate.parse(parts[1].trim(), 
                                DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                        }
                        
                        // Parse priority
                        Task.Priority priority = Task.Priority.MEDIUM;
                        for (Task.Priority p : Task.Priority.values()) {
                            if (p.toString().equalsIgnoreCase(parts[2].trim())) {
                                priority = p;
                                break;
                            }
                        }
                        
                        // Parse category
                        String category = parts[3].trim();
                        if (!category.isEmpty() && !categories.contains(category)) {
                            categories.add(category);
                            categoryComboBox.addItem(category);
                        }
                        
                        // Parse completed status
                        boolean completed = parts[4].trim().equalsIgnoreCase("Yes");
                        
                        // Create task
                        Task task = new Task(description, dueDate, priority, category);
                        task.setCompleted(completed);
                        
                        // Creation date is optional
                        if (parts.length > 5 && !parts[5].trim().isEmpty()) {
                            // We can't modify creation date directly, but in a real app
                            // we would have a proper constructor or setter for this
                        }
                        
                        importedTasks.add(task);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, 
                            "Error parsing line " + lineNum + ": " + e.getMessage(), 
                            "Import Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                // Confirm import
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Import " + importedTasks.size() + " tasks? This will replace your current tasks.", 
                    "Confirm Import", 
                    JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    tasks = importedTasks;
                    tableModel = new TaskTableModel(tasks);
                    taskTable.setModel(tableModel);
                    taskTable.getColumnModel().getColumn(0).setMaxWidth(30);
                    taskTable.getColumnModel().getColumn(5).setMaxWidth(100);
                    taskTable.setDefaultRenderer(Object.class, new TaskRenderer());
                    updateStats();
                    saveTasksToFile();
                    
                    JOptionPane.showMessageDialog(this, 
                        "Imported " + tasks.size() + " tasks successfully!", 
                        "Import Successful", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error reading file: " + e.getMessage(), 
                    "Import Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadTasksFromFile() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                tasks = (ArrayList<Task>) ois.readObject();
                updateStats();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading tasks: " + e.getMessage(), 
                    "Load Error", 
                    JOptionPane.ERROR_MESSAGE);
                tasks = new ArrayList<>();
            }
        } else {
            tasks = new ArrayList<>();
        }
    }
    
    private void saveTasksToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(tasks);
            updateStats();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error saving tasks: " + e.getMessage(), 
                "Save Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                darkMode = ois.readBoolean();
            } catch (Exception e) {
                System.out.println("Error loading config: " + e.getMessage());
            }
        }
    }
    
    private void saveConfig() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            oos.writeBoolean(darkMode);
        } catch (IOException e) {
            System.out.println("Error saving config: " + e.getMessage());
        }
    }
    
    private class TaskRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (row < tasks.size()) {
                Task task = tableModel.getTasks().get(row);
                
                // Set tooltip
                String tooltip = "<html>" +
                    "<b>Description:</b> " + task.getDescription() + "<br>" +
                    "<b>Priority:</b> " + task.getPriority() + "<br>" +
                    "<b>Category:</b> " + task.getCategory() + "<br>" +
                    "<b>Due Date:</b> " + (task.getDueDate() != null ? 
                        task.getDueDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "None") + "<br>" +
                    "<b>Created:</b> " + task.getCreationDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) +
                    "</html>";
                setToolTipText(tooltip);
                
                // Set background color based on priority if not selected
                if (!isSelected) {
                    if (task.isCompleted()) {
                        comp.setBackground(darkMode ? new Color(40, 80, 40) : new Color(220, 255, 220));
                        comp.setForeground(darkMode ? new Color(150, 150, 150) : new Color(100, 100, 100));
                    } else if (task.isOverdue()) {
                        comp.setBackground(darkMode ? new Color(80, 40, 40) : new Color(255, 200, 200));
                        comp.setForeground(darkMode ? Color.WHITE : Color.BLACK);
                    } else {
                        Color priorityColor = task.getPriority().getColor();
                        // Adjust color for dark mode
                        if (darkMode) {
                            priorityColor = new Color(
                                Math.max(priorityColor.getRed() - 100, 0),
                                Math.max(priorityColor.getGreen() - 100, 0),
                                Math.max(priorityColor.getBlue() - 100, 0)
                            );
                        }
                        comp.setBackground(priorityColor);
                        comp.setForeground(darkMode ? Color.WHITE : Color.BLACK);
                    }
                }
                
                // Strike through completed tasks
                if (task.isCompleted() && column == 1) {
                    setText("<html><strike>" + value + "</strike></html>");
                }
            }
            
            return comp;
        }
    }
    
    // Custom month picker component
    private class JMonthPicker extends JPanel {
        private static final long serialVersionUID = 1L;
        
        private JComboBox<String> monthComboBox;
        private JComboBox<Integer> yearComboBox;
        private JButton[][] dayButtons;
        private JPanel daysPanel;
        private final String[] monthNames = {
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        };
        private LocalDate currentDate;
        private LocalDate selectedDate;
        
        public JMonthPicker() {
            setLayout(new BorderLayout(5, 5));
            
            // Initialize with current date
            currentDate = LocalDate.now();
            selectedDate = null;
            
            // Month and year selection panel
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            
            // Month combo box
            monthComboBox = new JComboBox<>(monthNames);
            monthComboBox.setSelectedIndex(currentDate.getMonthValue() - 1);
            monthComboBox.addActionListener(e -> updateCalendar());
            
            // Year combo box
            Integer[] years = new Integer[11];
            int currentYear = currentDate.getYear();
            for (int i = 0; i < years.length; i++) {
                years[i] = currentYear - 5 + i;
            }
            yearComboBox = new JComboBox<>(years);
            yearComboBox.setSelectedItem(currentYear);
            yearComboBox.addActionListener(e -> updateCalendar());
            
            headerPanel.add(monthComboBox);
            headerPanel.add(yearComboBox);
            
            // Days of week panel
            JPanel weekdaysPanel = new JPanel(new GridLayout(1, 7));
            String[] weekdays = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                weekdaysPanel.add(label);
            }
            
            // Days panel
            daysPanel = new JPanel(new GridLayout(6, 7));
            dayButtons = new JButton[6][7];
            
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    final int r = row;
                    final int c = col;
                    JButton dayButton = new JButton();
                    dayButton.addActionListener(e -> selectDate(r, c));
                    dayButtons[row][col] = dayButton;
                    daysPanel.add(dayButton);
                }
            }
            
            add(headerPanel, BorderLayout.NORTH);
            add(weekdaysPanel, BorderLayout.CENTER);
            add(daysPanel, BorderLayout.SOUTH);
            
            updateCalendar();
        }
        
        private void updateCalendar() {
            int month = monthComboBox.getSelectedIndex() + 1;
            int year = (Integer) yearComboBox.getSelectedItem();
            
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;
            int daysInMonth = firstDayOfMonth.lengthOfMonth();
            
            // Reset all buttons
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    dayButtons[row][col].setText("");
                    dayButtons[row][col].setEnabled(false);
                }
            }
            
            // Fill the buttons with dates
            int day = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    if (row == 0 && col < startDayOfWeek) {
                        // Empty cells before the first day of the month
                        continue;
                    }
                    if (day > daysInMonth) {
                        // Empty cells after the last day of the month
                        break;
                    }
                    
                    dayButtons[row][col].setText(String.valueOf(day));
                    dayButtons[row][col].setEnabled(true);
                    
                    // Highlight today
                    LocalDate date = LocalDate.of(year, month, day);
                    if (date.equals(LocalDate.now())) {
                        dayButtons[row][col].setBackground(ACCENT_COLOR);
                        dayButtons[row][col].setForeground(Color.BLACK);
                    } else {
                        dayButtons[row][col].setBackground(null);
                        dayButtons[row][col].setForeground(null);
                    }
                    
                    // Highlight selected date
                    if (selectedDate != null && 
                        date.getYear() == selectedDate.getYear() && 
                        date.getMonth() == selectedDate.getMonth() && 
                        date.getDayOfMonth() == selectedDate.getDayOfMonth()) {
                        dayButtons[row][col].setBackground(MAIN_COLOR);
                        dayButtons[row][col].setForeground(Color.WHITE);
                    }
                    
                    day++;
                }
            }
        }
        
        private void selectDate(int row, int col) {
            if (dayButtons[row][col].isEnabled()) {
                int day = Integer.parseInt(dayButtons[row][col].getText());
                int month = monthComboBox.getSelectedIndex() + 1;
                int year = (Integer) yearComboBox.getSelectedItem();
                
                selectedDate = LocalDate.of(year, month, day);
                updateCalendar();
            }
        }
        
        public LocalDate getSelectedDate() {
            return selectedDate;
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new EnhancedTodoListGUI());
    }
}