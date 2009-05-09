package jdepend.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageComparator;
import jdepend.framework.PackageFilter;
import jdepend.framework.ParserListener;

/**
 * The <code>JDepend</code> class analyzes directories of Java class files,
 * generates metrics for each Java package, and reports the metrics in a Swing
 * tree.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class JDepend implements ParserListener {

    private jdepend.framework.JDepend analyzer;

    private JFrame frame;

    private StatusPanel statusPanel;

    private JTextField statusField;

    private JProgressBar progressBar;

    private DependTree afferentTree;

    private DependTree efferentTree;

    private Map<String, String> resourceStrings;

    private Map<String, Action> actions;

    private static final Font BOLD_FONT = new Font("dialog", Font.BOLD, 12);

    /**
     * Constructs a <code>JDepend</code> instance.
     */
    public JDepend() {

        analyzer = new jdepend.framework.JDepend();

        analyzer.addParseListener(this);

        //
        // Force the cross platform L&F.
        //
        try {
            UIManager.setLookAndFeel(UIManager
                    .getCrossPlatformLookAndFeelClassName());
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //
        // Install the resource string table.
        //
        resourceStrings = new HashMap<String, String>();
        resourceStrings.put("menubar", "File");
        resourceStrings.put("File", "About Exit");

        //
        // Install the action table.
        //
        actions = new HashMap<String, Action>();
        actions.put("About", new AboutAction());
        actions.put("Exit", new ExitAction());
    }

    /**
     * Adds the specified directory name to the collection of directories to be
     * analyzed.
     * 
     * @param name Directory name.
     * @throws IOException If the directory does not exist.
     */
    public void addDirectory(String name) throws IOException {
        analyzer.addDirectory(name);
    }

    /**
     * Sets the package filter.
     * 
     * @param filter Package filter.
     */
    public void setFilter(PackageFilter filter) {
        analyzer.setFilter(filter);
    }

    /**
     * Sets the comma-separated list of components.
     * @param components list of components
     */
    public void setComponents(String components) {
        analyzer.setComponents(components);
    }
    
    /**
     * Analyzes the registered directories, generates metrics for each Java
     * package, and reports the metrics in a graphical format.
     */
    public void analyze() {

        display();

        startProgressMonitor(analyzer.countClasses());

        ArrayList<JavaPackage> packages = new ArrayList<JavaPackage>(analyzer.analyze());

        Collections.sort(packages, new PackageComparator(PackageComparator.byName()));

        stopProgressMonitor();

        updateTree(packages);
    }

    /**
     * Called whenever a Java source file is parsed into the specified
     * <code>JavaClass</code> instance.
     * 
     * @param jClass Parsed Java class.
     */
    public void onParsedJavaClass(final JavaClass jClass) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                getProgressBar().setValue(getProgressBar().getValue() + 1);
            }
        });
    }

    private void display() {
        frame = createUI();
        frame.setVisible(true);
    }

    private void updateTree(List<JavaPackage> packages) {

        JavaPackage jPackage = new JavaPackage("root");
        jPackage.setAfferents(packages);
        jPackage.setEfferents(packages);

        AfferentNode ah = new AfferentNode(null, jPackage);
        getAfferentTree().setModel(new DependTreeModel(ah));

        EfferentNode eh = new EfferentNode(null, jPackage);
        getEfferentTree().setModel(new DependTreeModel(eh));
    }

    private void startProgressMonitor(final int maxValue) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                getProgressBar().setMinimum(0);
                getProgressBar().setMaximum(maxValue);
                getStatusPanel().setStatusComponent(getProgressBar());
            }
        });
    }

    private void stopProgressMonitor() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                getStatusPanel().setStatusComponent(getStatusField());
                int classCount = analyzer.countClasses();
                int packageCount = analyzer.countPackages();
                showStatusMessage("Analyzed " + packageCount + " packages ("
                        + classCount + " classes).");
            }
        });
    }

    private JFrame createUI() {

        JFrame jFrame = createFrame("JDepend");

        JMenuBar menuBar = createMenubar();
        jFrame.setJMenuBar(menuBar);

        JPanel treePanel = createTreePanel();
        StatusPanel status = getStatusPanel();

        jFrame.getContentPane().add("Center", treePanel);
        jFrame.getContentPane().add("South", status);
        jFrame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = 700;
        int height = 500;
        int x = (screenSize.width - width) / 2;
        int y = (screenSize.height - height) / 2;
        jFrame.setBounds(x, y, width, height);
        jFrame.setSize(width, height);

        return jFrame;
    }

    private JFrame createFrame(String title) {

        JFrame jFrame = new JFrame(title);

        jFrame.getContentPane().setLayout(new BorderLayout());
        jFrame.setBackground(SystemColor.control);

        jFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                new ExitAction().actionPerformed(null);
            }
        });

        return jFrame;
    }

    private JPanel createTreePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 1));
        panel.add(getEfferentTree());
        panel.add(getAfferentTree());

        /*
         * panel.setLayout(new GridLayout(1,1)); JSplitPane splitPane = new
         * JSplitPane(JSplitPane.VERTICAL_SPLIT);
         * splitPane.setOneTouchExpandable(true);
         * splitPane.setTopComponent(getEfferentTree());
         * splitPane.setBottomComponent(getAfferentTree());
         * panel.add(splitPane);
         */

        return panel;
    }

    private StatusPanel createStatusPanel() {
        StatusPanel panel = new StatusPanel();
        panel.setStatusComponent(getStatusField());

        return panel;
    }

    private JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);

        return bar;
    }

    private JTextField createStatusField() {
        JTextField field = new JTextField();
        field.setFont(BOLD_FONT);
        field.setEditable(false);
        field.setForeground(Color.black);
        field.setBorder(BorderFactory
                .createBevelBorder(BevelBorder.LOWERED));

        Insets insets = new Insets(5, 5, 5, 5);
        field.setMargin(insets);

        return field;
    }

    private JMenuBar createMenubar() {

        JMenuBar menuBar = new JMenuBar();

        String[] menuKeys = tokenize(resourceStrings.get("menubar"));
        for (int i = 0; i < menuKeys.length; i++) {
            JMenu m = createMenu(menuKeys[i]);
            if (m != null) {
                menuBar.add(m);
            }
        }

        return menuBar;
    }

    private JMenu createMenu(String key) {

        String[] itemKeys = tokenize(resourceStrings.get(key));
        JMenu menu = new JMenu(key);
        for (int i = 0; i < itemKeys.length; i++) {
            if (itemKeys[i].equals("-")) {
                menu.addSeparator();
            } else {
                JMenuItem mi = createMenuItem(itemKeys[i]);
                menu.add(mi);
            }
        }

        char mnemonic = key.charAt(0);
        menu.setMnemonic(mnemonic);

        return menu;
    }

    private JMenuItem createMenuItem(String key) {

        JMenuItem mi = new JMenuItem(key);

        char mnemonic = key.charAt(0);
        mi.setMnemonic(mnemonic);

        char accelerator = key.charAt(0);
        mi.setAccelerator(KeyStroke.getKeyStroke(accelerator,
                java.awt.Event.CTRL_MASK));

        String actionString = key;
        mi.setActionCommand(actionString);

        Action a = getActionForCommand(actionString);
        if (a != null) {
            mi.addActionListener(a);
            mi.setEnabled(a.isEnabled());
        } else {
            mi.setEnabled(false);
        }

        return mi;
    }

    private void showStatusMessage(final String message) {
        getStatusField().setFont(BOLD_FONT);
        getStatusField().setForeground(Color.black);
        getStatusField().setText(" " + message);
    }

    private void showStatusError(final String message) {
        getStatusField().setFont(BOLD_FONT);
        getStatusField().setForeground(Color.red);
        getStatusField().setText(" " + message);
    }

    private DependTree getAfferentTree() {
        if (afferentTree == null) {
            afferentTree = new DependTree();
            afferentTree.addTreeSelectionListener(new TreeListener());
        }

        return afferentTree;
    }

    private DependTree getEfferentTree() {
        if (efferentTree == null) {
            efferentTree = new DependTree();
            efferentTree.addTreeSelectionListener(new TreeListener());
        }

        return efferentTree;
    }

    private StatusPanel getStatusPanel() {
        if (statusPanel == null) {
            statusPanel = createStatusPanel();
        }
        return statusPanel;
    }

    private JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = createProgressBar();
        }

        return progressBar;
    }

    private JTextField getStatusField() {
        if (statusField == null) {
            statusField = createStatusField();
        }
        return statusField;
    }

    private Action getActionForCommand(String command) {
        return actions.get(command);
    }

    /*
     * Parses the specified string into an array of strings on whitespace
     * boundaries. @param input String to tokenize. @return Strings.
     */
    private String[] tokenize(String input) {

        ArrayList<String> v = new ArrayList<String>();
        StringTokenizer t = new StringTokenizer(input);

        while (t.hasMoreTokens()) {
            v.add(t.nextToken());
        }

        return v.toArray(new String[v.size()]);
    }

    //
    // Tree selection handler.
    //
    private class TreeListener implements TreeSelectionListener {

        /**
         * Constructs a <code>TreeListener</code> instance.
         */
        TreeListener() {
        }

        /**
         * Callback method triggered whenever the value of the tree selection
         * changes.
         * 
         * @param te Event that characterizes the change.
         */
        public void valueChanged(TreeSelectionEvent te) {

            TreePath path = te.getNewLeadSelectionPath();

            if (path != null) {
                PackageNode node = (PackageNode) path.getLastPathComponent();
                showStatusMessage(node.toMetricsString());
            }
        }
    }

    //
    // About action handler.
    //
    private class AboutAction extends AbstractAction {

        private static final long serialVersionUID = -1;

        /**
         * Constructs an <code>AboutAction</code> instance.
         */
        AboutAction() {
            super("About");
        }

        /**
         * Handles the action.
         */
        public void actionPerformed(ActionEvent e) {
            AboutDialog d = new AboutDialog(frame);
            d.setModal(true);
            d.setLocation(300, 300);
            d.setVisible(true);
        }
    }

    //
    // Exit action handler.
    //
    private class ExitAction extends AbstractAction {

        private static final long serialVersionUID = -1;

        /**
         * Constructs an <code>ExitAction</code> instance.
         */
        ExitAction() {
            super("Exit");
        }

        /**
         * Handles the action.
         */
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
            System.exit(0);
        }
    }

    private void usage(String message) {
        if (message != null) {
            System.err.println("\n" + message);
        }

        String baseUsage = "\nJDepend ";

        System.err.println("");
        System.err.println("usage: ");
        System.err.println(baseUsage + "-components <components> " +
            "<directory> [directory2 [directory 3] ...]");
        System.exit(1);
    }

    private void instanceMain(String[] args) {

        if (args.length < 1) {
            usage("Must specify at least one directory.");
        }

        int directoryCount = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equalsIgnoreCase("-components")) {
                    if (args.length <= i + 1) {
                        usage("Components not specified.");
                    }
                    setComponents(args[++i]);
                } else {
                    usage("Invalid argument: " + args[i]);
                }
            } else {
                try {
                    addDirectory(args[i]);
                    directoryCount++;
                } catch (IOException ioe) {
                    usage("Directory does not exist: " + args[i]);
                }
            }
        }
        
        if (directoryCount == 0) {
            usage("Must specify at least one directory.");
        }

        analyze();
    }

    public static void main(String[] args) {
        new JDepend().instanceMain(args);
    }
}

