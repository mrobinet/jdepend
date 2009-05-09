package jdepend.framework;

import java.util.Comparator;

/**
 * The <code>PackageComparator</code> class is a <code>Comparator</code>
 * used to compare two <code>JavaPackage</code> instances for order using a
 * sorting strategy.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class PackageComparator implements Comparator<JavaPackage> {

    private PackageComparator by;

    private static PackageComparator byNameComparator;
    static {
        byNameComparator = new PackageComparator();
    }

    public static PackageComparator byName() {
        return byNameComparator;
    }

    private PackageComparator() {
    }

    public PackageComparator(PackageComparator byWhat) {
        this.by = byWhat;
    }

    public PackageComparator byWhat() {
        return by;
    }

    public int compare(JavaPackage a, JavaPackage b) {

        if (byWhat() == byName()) {
            return a.getName().compareTo(b.getName());
        }

        return 0;
    }
}