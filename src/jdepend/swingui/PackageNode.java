package jdepend.swingui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageComparator;

/**
 * The <code>PackageNode</code> class defines the default behavior for tree
 * nodes representing Java packages.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public abstract class PackageNode {

    private PackageNode parent;

    private JavaPackage jPackage;

    private List<PackageNode> children;

    private static NumberFormat formatter;
    static {
        formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(2);
    }

    /**
     * Constructs a <code>PackageNode</code> with the specified package and
     * its collection of dependent packages.
     * 
     * @param parent Parent package node.
     * @param jPackage Java package.
     */
    public PackageNode(PackageNode parent, JavaPackage jPackage) {
        this.parent = parent;
        this.jPackage = jPackage;
        children = null;
    }

    /**
     * Returns the Java package represented in this node.
     * 
     * @return Java package.
     */
    public JavaPackage getPackage() {
        return jPackage;
    }

    /**
     * Returns the parent of this package node.
     * 
     * @return Parent package node.
     */
    public PackageNode getParent() {
        return parent;
    }

    /**
     * Indicates whether this node is a leaf node.
     * 
     * @return <code>true</code> if this node is a leaf; <code>false</code>
     *         otherwise.
     */
    public boolean isLeaf() {
        if (getCoupledPackages().size() > 0) {
            return false;
        }

        return true;
    }

    /**
     * Creates and returns a <code>PackageNode</code> with the specified
     * parent node and Java package.
     * 
     * @param parent Parent package node.
     * @param jPackage Java package.
     * @return A non-null <code>PackageNode</code.
     */
    protected abstract PackageNode makeNode(PackageNode parent,
            JavaPackage jPackage);

    /**
     * Returns the collection of Java packages coupled to the package
     * represented in this node.
     * 
     * @return Collection of coupled packages.
     */
    protected abstract Collection<JavaPackage> getCoupledPackages();

    /**
     * Indicates whether the specified package should be displayed as a child of
     * this node.
     * 
     * @param jPackage Package to test.
     * @return <code>true</code> to display the package; <code>false</code>
     *         otherwise.
     */
    public boolean isChild(JavaPackage jPackage) {
        return true;
    }

    /**
     * Returns the child package nodes of this node.
     * 
     * @return List of child package nodes.
     */
    public List<PackageNode> getChildren() {

        if (children == null) {

            children = new ArrayList<PackageNode>();
            ArrayList<JavaPackage> packages = new ArrayList<JavaPackage>(getCoupledPackages());
            Collections.sort(packages, new PackageComparator(PackageComparator.byName()));
            for (JavaPackage pkg : packages) {
                if (isChild(pkg)) {
                    PackageNode childNode = makeNode(this, pkg);
                    children.add(childNode);
                }
            }
        }

        return children;
    }

    /**
     * Returns the string representation of this node's metrics.
     * 
     * @return Metrics string.
     */
    public String toMetricsString() {
        StringBuffer label = new StringBuffer(83);
        label.append(getPackage().getName())
             .append("  (CC: ").append(getPackage().getConcreteClassCount())
             .append("  AC: ").append(getPackage().getAbstractClassCount())
             .append("  Ca: ").append(getPackage().afferentCoupling())
             .append("  Ce: ").append(getPackage().efferentCoupling())
             .append("  A: ").append(format(getPackage().abstractness()))
             .append("  I: ").append(format(getPackage().instability()))
             .append("  D: ").append(format(getPackage().distance()))
             .append("  V: ").append(getPackage().getVolatility());
        if (getPackage().containsCycle()) {
            label.append(" Cyclic");
        }

        label.append(')');

        return label.toString();
    }

    /**
     * Returns the string representation of this node in it's current tree
     * context.
     * 
     * @return Node label.
     */
    @Override
    public String toString() {

        if (getParent().getParent() == null) {
            return toMetricsString();
        }

        return getPackage().getName();
    }

    /*
     * Returns the specified number in a displayable format. @param number
     * Number to format. @return Formatted number.
     */
    private static String format(float f) {
        return formatter.format(f);
    }
}

