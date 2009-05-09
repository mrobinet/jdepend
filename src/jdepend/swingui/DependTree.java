package jdepend.swingui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import jdepend.framework.JavaPackage;

/**
 * The <code>DependTree</code> class defines the graphical tree for displaying
 * the packages and their hierarchical dependencies.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class DependTree extends JPanel implements TreeSelectionListener {

    private static final long serialVersionUID = -1;

    private JTree tree;

    /**
     * Constructs a <code>DependTree</code> with an empty tree model.
     */
    public DependTree() {
        this(new DependTreeModel(new AfferentNode(null, new JavaPackage(""))));
    }

    /**
     * Constructs a <code>DependTree</code> with the specified tree model.
     * 
     * @param model Depend tree model.
     */
    public DependTree(DependTreeModel model) {

        setBorder(BorderFactory.createTitledBorder(model.getRoot().toString()));

        setModel(model);

        setLayout(new BorderLayout());

        JScrollPane pane = createScrollPane();

        add(pane, "Center");
    }

    /**
     * Sets the tree model.
     * 
     * @param model Tree model.
     */
    public void setModel(DependTreeModel model) {
        setBorder(BorderFactory.createTitledBorder(model.getRoot().toString()));
        getTree().setModel(model);

    }

    /**
     * Returns the tree model.
     * 
     * @return Tree model.
     */
    public DependTreeModel getModel() {
        return (DependTreeModel) getTree().getModel();
    }

    /**
     * Registers the specified listener with this tree.
     * 
     * @param l Tree selection listener.
     */
    public void addTreeSelectionListener(TreeSelectionListener l) {
        getTree().addTreeSelectionListener(l);
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
            path.getLastPathComponent();
        }
    }

    /**
     * Creates and returns a scroll pane.
     * 
     * @return Scroll pane.
     */
    private JScrollPane createScrollPane() {
        return new JScrollPane(getTree());
    }

    /**
     * Creates and returns a peered tree.
     * 
     * @return Tree.
     */
    private JTree createTree() {

        JTree jTree = new JTree();
        jTree.setShowsRootHandles(false);
        jTree.setFont(new Font("Dialog", Font.PLAIN, 12));
        jTree.addTreeSelectionListener(this);
        jTree.setRootVisible(false);
        jTree.setLargeModel(true);

        return jTree;
    }

    /*
     * Returns the peered tree. @return A non-null tree.
     */
    private JTree getTree() {
        if (tree == null) {
            tree = createTree();
        }

        return tree;
    }
}

