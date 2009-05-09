package jdepend.framework;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>JavaClass</code> class represents a Java 
 * class or interface.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class JavaClass {

    private String className;
    private String packageName;
    private boolean isAbst;
    private Map<String, JavaPackage> imports;
    private String sourceFile;


    public JavaClass(String name) {
        className = name;
        packageName = "default";
        isAbst = false;
        imports = new HashMap<String, JavaPackage>();
        sourceFile = "Unknown";
    }

    public void setName(String name) {
        className = name;
    }

    public String getName() {
        return className;
    }

    public void setPackageName(String name) {
        packageName = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setSourceFile(String name) {
        sourceFile = name;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Collection<JavaPackage> getImportedPackages() {
        return imports.values();
    }

    public void addImportedPackage(JavaPackage jPackage) {
        if (!jPackage.getName().equals(getPackageName())) {
            imports.put(jPackage.getName(), jPackage);
        }
    }

    public boolean isAbstract() {
        return isAbst;
    }

    public void isAbstract(boolean isAbstract) {
        isAbst = isAbstract;
    }

    @Override
    public boolean equals(Object other) {

        if (other instanceof JavaClass) {
            JavaClass otherClass = (JavaClass) other;
            return otherClass.getName().equals(getName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public static class ClassComparator implements Comparator<JavaClass> {

        public int compare(JavaClass c1, JavaClass c2) {
            return c1.getName().compareTo(c2.getName());
        }
    }
}
