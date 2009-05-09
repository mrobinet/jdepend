package jdepend.framework;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>ClassFileParser</code> class is responsible for 
 * parsing a Java class file to create a <code>JavaClass</code> 
 * instance.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class ClassFileParser extends AbstractParser {

    public static final int JAVA_MAGIC = 0xCAFEBABE;
    public static final int CONSTANT_UTF8 = 1;
    public static final int CONSTANT_UNICODE = 2;
    public static final int CONSTANT_INTEGER = 3;
    public static final int CONSTANT_FLOAT = 4;
    public static final int CONSTANT_LONG = 5;
    public static final int CONSTANT_DOUBLE = 6;
    public static final int CONSTANT_CLASS = 7;
    public static final int CONSTANT_STRING = 8;
    public static final int CONSTANT_FIELD = 9;
    public static final int CONSTANT_METHOD = 10;
    public static final int CONSTANT_INTERFACEMETHOD = 11;
    public static final int CONSTANT_NAMEANDTYPE = 12;
    public static final int CONSTANT_METHOD_HANDLE = 15;
    public static final int CONSTANT_METHOD_TYPE = 16;
    public static final int CONSTANT_INVOKEDYNAMIC = 18;

    public static final char CLASS_DESCRIPTOR = 'L';
    public static final int ACC_INTERFACE = 0x200;
    public static final int ACC_ABSTRACT = 0x400;
    
    private String fileName;
    private String className;
    private String superClassName;
    private String interfaceNames[];
    private boolean isAbstract;
    private JavaClass jClass;
    private Constant[] constantPool;
    private FieldOrMethodInfo[] fields;
    private FieldOrMethodInfo[] methods;
    private AttributeInfo[] attributes;
    private DataInputStream in;

    
    public ClassFileParser() {
        this(new PackageFilter());
    }

    public ClassFileParser(PackageFilter filter) {
        super(filter);
        reset();
    }

    private void reset() {
        className = null;
        superClassName = null;
        interfaceNames = new String[0];
        isAbstract = false;

        jClass = null;
        constantPool = new Constant[1];
        fields = new FieldOrMethodInfo[0];
        methods = new FieldOrMethodInfo[0];
        attributes = new AttributeInfo[0];
    }

    /**
     * Registered parser listeners are informed that the resulting
     * <code>JavaClass</code> was parsed.
     * @param classFile class file to be parsed
     * @return the Java class
     * @throws IOException if I/O exception occurs while parsing
     */
    public JavaClass parse(File classFile) throws IOException {

        this.fileName = classFile.getCanonicalPath();

        debug("\nParsing " + fileName + "...");

        InputStream inputStream = null;

        try {

            inputStream = new BufferedInputStream(new FileInputStream(classFile));

            return parse(inputStream);

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    public JavaClass parse(InputStream is) throws IOException {

        reset();

        jClass = new JavaClass("Unknown");

        in = new DataInputStream(is);

        parseMagic();

        parseMinorVersion();
        parseMajorVersion();

        constantPool = parseConstantPool();

        parseAccessFlags();

        className = parseClassName();

        superClassName = parseSuperClassName();

        interfaceNames = parseInterfaces();

        fields = parseFields();

        methods = parseMethods();

        parseAttributes();

        addClassConstantReferences();

        addAnnotationsReferences();

        onParsedJavaClass(jClass);

        return jClass;
    }

    private int parseMagic() throws IOException {
        int magic = in.readInt();
        if (magic != JAVA_MAGIC) {
            throw new IOException("Invalid class file: " + fileName);
        }

        return magic;
    }

    private int parseMinorVersion() throws IOException {
        return in.readUnsignedShort();
    }

    private int parseMajorVersion() throws IOException {
        return in.readUnsignedShort();
    }

    private Constant[] parseConstantPool() throws IOException {
        int constantPoolSize = in.readUnsignedShort();

        Constant[] pool = new Constant[constantPoolSize];

        for (int i = 1; i < constantPoolSize; i++) {

            Constant constant = parseNextConstant();

            pool[i] = constant;

            //
            // 8-byte constants use two constant pool entries
            //
            if (constant.getTag() == CONSTANT_DOUBLE
                    || constant.getTag() == CONSTANT_LONG) {
                i++;
            }
        }

        return pool;
    }

    private void parseAccessFlags() throws IOException {
        int accessFlags = in.readUnsignedShort();

        boolean isAbst = ((accessFlags & ACC_ABSTRACT) != 0);
        boolean isInterface = ((accessFlags & ACC_INTERFACE) != 0);

        this.isAbstract = isAbst || isInterface;
        jClass.isAbstract(this.isAbstract);

        debug("Parser: abstract = " + this.isAbstract);
    }

    private String parseClassName() throws IOException {
        int entryIndex = in.readUnsignedShort();
        String clazzName = getClassConstantName(entryIndex);
        jClass.setName(clazzName);
        jClass.setPackageName(getPackageName(clazzName));

        debug("Parser: class name = " + clazzName);
        debug("Parser: package name = " + getPackageName(clazzName));
        
        return clazzName;
    }

    private String parseSuperClassName() throws IOException {
        int entryIndex = in.readUnsignedShort();
        String superClazzName = getClassConstantName(entryIndex);
        addImport(getPackageName(superClazzName));

        debug("Parser: super class name = " + superClazzName);
        
        return superClazzName;
    }

    private String[] parseInterfaces() throws IOException {
        int interfacesCount = in.readUnsignedShort();
        String[] ifcNames = new String[interfacesCount];
        for (int i = 0; i < interfacesCount; i++) {
            int entryIndex = in.readUnsignedShort();
            ifcNames[i] = getClassConstantName(entryIndex);
            addImport(getPackageName(ifcNames[i]));

            debug("Parser: interface = " + ifcNames[i]);
        }

        return ifcNames;
    }

    private FieldOrMethodInfo[] parseFields() throws IOException {
        int fieldsCount = in.readUnsignedShort();
        FieldOrMethodInfo[] infos = new FieldOrMethodInfo[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            infos[i] = parseFieldOrMethodInfo();
            String descriptor = toUTF8(infos[i].getDescriptorIndex());
            debug("Parser: field descriptor = " + descriptor);
            String[] types = descriptorToTypes(descriptor);
            for (String type : types) {
                addImport(getPackageName(type));
                debug("Parser: field type = " + type);
            }
        }

        return infos;
    }

    private FieldOrMethodInfo[] parseMethods() throws IOException {
        int methodsCount = in.readUnsignedShort();
        FieldOrMethodInfo[] infos = new FieldOrMethodInfo[methodsCount];
        for (int i = 0; i < methodsCount; i++) {
            infos[i] = parseFieldOrMethodInfo();
            String descriptor = toUTF8(infos[i].getDescriptorIndex());
            debug("Parser: method descriptor = " + descriptor);
            String[] types = descriptorToTypes(descriptor);
            for (String type : types) {
                if (type.length() > 0) {
                    addImport(getPackageName(type));
                    debug("Parser: method type = " + type);
                }
            }
        }

        return infos;
    }

    private Constant parseNextConstant() throws IOException {

        Constant result;

        byte tag = in.readByte();

        switch (tag) {

        case ClassFileParser.CONSTANT_CLASS:
        case ClassFileParser.CONSTANT_STRING:
        case ClassFileParser.CONSTANT_METHOD_TYPE:
            result = new Constant(tag, in.readUnsignedShort());
            break;
        case ClassFileParser.CONSTANT_FIELD:
        case ClassFileParser.CONSTANT_METHOD:
        case ClassFileParser.CONSTANT_INTERFACEMETHOD:
        case ClassFileParser.CONSTANT_NAMEANDTYPE:
        case ClassFileParser.CONSTANT_INVOKEDYNAMIC:
            result = new Constant(tag, in.readUnsignedShort(), in
                    .readUnsignedShort());
            break;
        case ClassFileParser.CONSTANT_INTEGER:
            result = new Constant(tag, in.readInt());
            break;
        case ClassFileParser.CONSTANT_FLOAT:
            result = new Constant(tag, in.readFloat());
            break;
        case ClassFileParser.CONSTANT_LONG:
            result = new Constant(tag, in.readLong());
            break;
        case ClassFileParser.CONSTANT_DOUBLE:
            result = new Constant(tag, in.readDouble());
            break;
        case ClassFileParser.CONSTANT_UTF8:
            result = new Constant(tag, in.readUTF());
            break;
        case ClassFileParser.CONSTANT_METHOD_HANDLE:
            result = new Constant(tag, in.readByte(), in.readUnsignedShort());
            break;
        default:
            throw new IOException("Unknown constant: " + tag);
        }

        return result;
    }

    private FieldOrMethodInfo parseFieldOrMethodInfo() throws IOException {

        FieldOrMethodInfo result = new FieldOrMethodInfo(
                in.readUnsignedShort(), in.readUnsignedShort(), in
                        .readUnsignedShort());

        int attributesCount = in.readUnsignedShort();
        for (int a = 0; a < attributesCount; a++) {
        	AttributeInfo attribute = parseAttribute();
        	if ("RuntimeVisibleAnnotations".equals(attribute.name)) {
        		result.runtimeVisibleAnnotations = attribute;
        	}
        }

        return result;
    }

    private void parseAttributes() throws IOException {
        int attributesCount = in.readUnsignedShort();
        attributes = new AttributeInfo[attributesCount];

        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = parseAttribute();

            // Section 4.7.7 of VM Spec - Class File Format
            if (attributes[i].getName() != null && attributes[i].getName().equals("SourceFile")) {
                byte[] b = attributes[i].getValue();
                int b0 = b[0] < 0 ? b[0] + 256 : b[0];
                int b1 = b[1] < 0 ? b[1] + 256 : b[1];
                int pe = b0 * 256 + b1;

                String descriptor = toUTF8(pe);
                jClass.setSourceFile(descriptor);
            }
        }
    }

    private AttributeInfo parseAttribute() throws IOException {
        AttributeInfo result = new AttributeInfo();

        int nameIndex = in.readUnsignedShort();
        if (nameIndex != -1) {
            result.setName(toUTF8(nameIndex));
        }

        int attributeLength = in.readInt();
        byte[] value = new byte[attributeLength];
        for (int b = 0; b < attributeLength; b++) {
            value[b] = in.readByte();
        }

        result.setValue(value);
        return result;
    }

    private Constant getConstantPoolEntry(int entryIndex) throws IOException {

        if (entryIndex < 0 || entryIndex >= constantPool.length) {
            throw new IOException("Illegal constant pool index : " + entryIndex);
        }

        return constantPool[entryIndex];
    }

    private void addClassConstantReferences() throws IOException {
        for (int j = 1; j < constantPool.length; j++) {
            if (constantPool[j].getTag() == CONSTANT_CLASS) {
                String name = toUTF8(constantPool[j].getNameIndex());
                addImport(getPackageName(name));

                debug("Parser: class type = " + slashesToDots(name));
            }
            
            if (constantPool[j].getTag() == CONSTANT_DOUBLE
                    || constantPool[j].getTag() == CONSTANT_LONG) {
                j++;
            }
        }
    }

    private void addAnnotationsReferences() throws IOException {
        for (int j = 1; j < attributes.length; j++) {
            if ("RuntimeVisibleAnnotations".equals(attributes[j].name)) {
                addAnnotationReferences(attributes[j]);
            }
        }
        for (int j = 1; j < fields.length; j++) {
        	if (fields[j].runtimeVisibleAnnotations != null) {
        		addAnnotationReferences(fields[j].runtimeVisibleAnnotations);
        	}
        }
        for (int j = 1; j < methods.length; j++) {
        	if (methods[j].runtimeVisibleAnnotations != null) {
        		addAnnotationReferences(methods[j].runtimeVisibleAnnotations);
        	}
        }
    }

    private void addAnnotationReferences(AttributeInfo annotation) throws IOException {
    	// JVM Spec 4.8.15
    	byte[] data = annotation.value;
    	int numAnnotations = u2(data, 0);
    	int annotationIndex = 2;
    	addAnnotationReferences(data, annotationIndex, numAnnotations);
    }

    private int addAnnotationReferences(byte[] data, int index, int numAnnotations) throws IOException {
    	int visitedAnnotations = 0;
		while (visitedAnnotations < numAnnotations) {
	    	int typeIndex = u2(data, index);
	    	int numElementValuePairs = u2(data, index = index + 2);
	        addImport(getPackageName(toUTF8(typeIndex).substring(1)));
	        int visitedElementValuePairs = 0;
	        index += 2;
	        while (visitedElementValuePairs < numElementValuePairs) {
	        	index = addAnnotationElementValueReferences(data, index = index + 2);
	        	visitedElementValuePairs++;
	        }
	        visitedAnnotations++;
    	}
		return index;
	}
    
    private int addAnnotationElementValueReferences(byte[] data, int index) throws IOException {
    	byte tag = data[index];
    	index += 1;
    	switch (tag) {
        	case 'B':
        	case 'C':
        	case 'D':
        	case 'F':
        	case 'I':
        	case 'J':
        	case 'S':
    		case 'Z':
    		case 's':
    			index += 2;
    			break;
    			
    		case 'e':
    			int enumTypeIndex = u2(data, index);
    			addImport(getPackageName(toUTF8(enumTypeIndex).substring(1)));
    			index += 4;
    			break;
    			
    		case 'c':
    			int classInfoIndex = u2(data, index);
    			addImport(getPackageName(toUTF8(classInfoIndex).substring(1)));
    			index += 2;
    			break;
    			
    		case '@':
    			index = addAnnotationReferences(data, index, 1);
    			break;
    			
    		case '[':
    			int numValues = u2(data, index);
    			index = index + 2;
    			for (int i = 0; i < numValues; i++) {
    				index = addAnnotationElementValueReferences(data, index);
    			}
    			break;
    	}
    	return index;
    }

	private int u2(byte[] data, int index) {
		return (data[index] << 8 & 0xFF00)  | (data[index+1] & 0xFF);
	}

	private String getClassConstantName(int entryIndex) throws IOException {

        Constant entry = getConstantPoolEntry(entryIndex);
        if (entry == null) {
            return "";
        }
        return slashesToDots(toUTF8(entry.getNameIndex()));
    }

    private String toUTF8(int entryIndex) throws IOException {
        Constant entry = getConstantPoolEntry(entryIndex);
        if (entry.getTag() == CONSTANT_UTF8) {
            return (String) entry.getValue();
        }

        throw new IOException("Constant pool entry is not a UTF8 type: "
                + entryIndex);
    }

    private void addImport(String importPackage) {
        if ((importPackage != null) && (getFilter().accept(importPackage))) {
            jClass.addImportedPackage(new JavaPackage(importPackage));
        }
    }

    private String slashesToDots(String s) {
        return s.replace('/', '.');
    }

    private String getPackageName(String s) {
        String sVal = s;
        if ((sVal.length() > 0) && (sVal.charAt(0) == '[')) {
            String types[] = descriptorToTypes(sVal);
            if (types.length == 0) {
                return null; // primitives
            }

            sVal = types[0];
        }

        sVal = slashesToDots(sVal);
        int index = sVal.lastIndexOf('.');
        if (index > 0) {
            return sVal.substring(0, index);
        }

        return "Default";
    }

    private String[] descriptorToTypes(String descriptor) {

        int typesCount = 0;
        for (int index = 0; index < descriptor.length(); index++) {
            if (descriptor.charAt(index) == ';') {
                typesCount++;
            }
        }

        String types[] = new String[typesCount];

        int typeIndex = 0;
        for (int index = 0; index < descriptor.length(); index++) {

            int startIndex = descriptor.indexOf(CLASS_DESCRIPTOR, index);
            if (startIndex < 0) {
                break;
            }

            index = descriptor.indexOf(';', startIndex + 1);
            types[typeIndex++] = descriptor.substring(startIndex + 1, index);
        }

        return types;
    }

    class Constant {

        private byte tag;

        private int nameIndex;

        private int typeIndex;

        private Object value;

        Constant(byte t, int nameIndex) {
            this(t, nameIndex, -1);
        }

        Constant(byte t, Object val) {
            this(t, -1, -1);
            value = val;
        }

        Constant(byte t, int nameIdx, int typeIdx) {
            tag = t;
            nameIndex = nameIdx;
            typeIndex = typeIdx;
            value = null;
        }

        byte getTag() {
            return tag;
        }

        int getNameIndex() {
            return nameIndex;
        }

        int getTypeIndex() {
            return typeIndex;
        }

        Object getValue() {
            return value;
        }

        @Override
        public String toString() {

            StringBuilder s = new StringBuilder(51);

            s.append("tag: ").append(getTag());

            if (getNameIndex() > -1) {
                s.append(" nameIndex: ").append(getNameIndex());
            }

            if (getTypeIndex() > -1) {
                s.append(" typeIndex: ").append(getTypeIndex());
            }

            if (getValue() != null) {
                s.append(" value: ").append(getValue());
            }

            return s.toString();
        }
    }

    class FieldOrMethodInfo {

        private final int accessFlags;

        private final int nameIndex;

        private final int descriptorIndex;
        
        private AttributeInfo runtimeVisibleAnnotations;

        FieldOrMethodInfo(int accessFlgs, int nameIdx, int descriptorIdx) {

            accessFlags = accessFlgs;
            nameIndex = nameIdx;
            descriptorIndex = descriptorIdx;
        }

        int getAccessFlags() {
            return accessFlags;
        }

        int getNameIndex() {
            return nameIndex;
        }

        int getDescriptorIndex() {
            return descriptorIndex;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder(83);

            try {

                s.append("\n    name (#").append(getNameIndex()).append(") = ").append(toUTF8(getNameIndex()))
                 .append("\n    signature (#").append(getDescriptorIndex()).append(") = ")
                        .append(toUTF8(getDescriptorIndex()));

                String[] types = descriptorToTypes(toUTF8(getDescriptorIndex()));
                for (String type : types) {
                    s.append("\n        type = ").append(type);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return s.toString();
        }
    }

    class AttributeInfo {

        private String name;

        private byte[] value;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }

        public byte[] getValue() {
            return this.value;
        }
    }

    /**
     * Returns a string representation of this object.
     * 
     * @return String representation.
     */
    @Override
    public String toString() {

        StringBuilder s = new StringBuilder(137);

        try {

            s.append('\n').append(className).append(":\n\nConstants:\n");
            for (int i = 1; i < constantPool.length; i++) {
                Constant entry = getConstantPoolEntry(i);
                s.append("    ").append(i).append(". ").append(entry.toString()).append('\n');
                if (entry.getTag() == CONSTANT_DOUBLE
                        || entry.getTag() == CONSTANT_LONG) {
                    i++;
                }
            }

            s.append("\nClass Name: ").append(className)
             .append("\nSuper Name: ").append(superClassName)
             .append("\n\n")
             .append(interfaceNames.length).append(" interfaces\n");

            for (String interfaceName : interfaceNames) {
                s.append("    ").append(interfaceName).append('\n');
            }

            s.append('\n').append(fields.length).append(" fields\n");
            for (FieldOrMethodInfo field : fields) {
                s.append(field.toString()).append('\n');
            }

            s.append('\n').append(methods.length).append(" methods\n");
            for (FieldOrMethodInfo method : methods) {
                s.append(method.toString()).append('\n');
            }

            s.append("\nDependencies:\n");
            for (JavaPackage jPackage : jClass.getImportedPackages()) {
                s.append("    ").append(jPackage.getName()).append('\n');
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return s.toString();
    }

    /**
     * Test main.
     * @param args Single argument of file path.
     */
    public static void main(String args[]) {
        try {

            ClassFileParser.debugFlag = true;

            if (args.length <= 0) {
                System.err.println("usage: ClassFileParser <class-file>");
                System.exit(0);
            }

            ClassFileParser parser = new ClassFileParser();

            parser.parse(new File(args[0]));

            System.err.println(parser.toString());

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
