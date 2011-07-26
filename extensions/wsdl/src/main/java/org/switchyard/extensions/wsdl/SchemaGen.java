/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */
package org.switchyard.extensions.wsdl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.switchyard.common.type.Classes;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class SchemaGen {
    private static final Map<String, String> PRIMITIVE_TYPES = new HashMap<String, String>();
    private static final Map<String, String> PRIMITIVE_MAP = new HashMap<String, String>();
    private GeneratedClassLoader _loader;
    private Set<Class<?>> _generatedClasses = new HashSet<Class<?>>();

    static {
      PRIMITIVE_TYPES.put("int", "I");
      PRIMITIVE_TYPES.put("float", "F");
      PRIMITIVE_TYPES.put("long", "J");
      PRIMITIVE_TYPES.put("double", "D");
      PRIMITIVE_TYPES.put("short", "S");
      PRIMITIVE_TYPES.put("boolean", "Z");
      PRIMITIVE_TYPES.put("byte", "B");

      PRIMITIVE_MAP.put("java.lang.Integer", "int");
      PRIMITIVE_MAP.put("java.lang.Float", "float");
      PRIMITIVE_MAP.put("java.lang.Long", "long");
      PRIMITIVE_MAP.put("java.lang.Double", "double");
      PRIMITIVE_MAP.put("java.lang.Short", "short");
      PRIMITIVE_MAP.put("java.lang.Boolean", "boolean");
      PRIMITIVE_MAP.put("java.lang.Byte", "byte");
      PRIMITIVE_MAP.put("java.lang.String", "string");
    }

    /**
     * Construct a SchemaGen instance.
     */
    public SchemaGen() {
        _loader = getGeneratedClassLoader(Classes.getTCCL());
    }

    private GeneratedClassLoader getGeneratedClassLoader(final ClassLoader tccl) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<GeneratedClassLoader>() {
                public GeneratedClassLoader run() {
                    return new GeneratedClassLoader(tccl);
                }
            });
        } else {
            return new GeneratedClassLoader(tccl);
        }
    }

    /**
     * Generate request wrapper class.
     *
     * @param className the name of the wrapper class
     * @param namespace namespace of the class and wrapped types
     * @param requestType the request parameter Java type
     * @throws WSDLWriterException if the wrapper class cannot be generated
     */
    public void generateClass(final String className, final String namespace, final String requestType) throws WSDLWriterException {
        generateClass(className, namespace, requestType, null);
    }

    /**
     * Generate both request and response wrapper classes.
     *
     * @param className the name of the wrapper class
     * @param namespace namespace of the class and wrapped types
     * @param requestType the request parameter Java type
     * @param responseType the response parameter Java type
     * @throws WSDLWriterException if the wrapper class cannot be generated
     */
    public void generateClass(final String className, final String namespace, final String requestType, final String responseType) throws WSDLWriterException {
        String paramName = "arg0";
        String genClassName = className;
        if (!isPrimitive(requestType)) {
            int idx = requestType.lastIndexOf('/');
            paramName = Character.toLowerCase(requestType.charAt(idx + 1)) + requestType.substring(idx + 2);
        }
        Class<?> genClass = _loader.getGeneratedClass(genClassName, requestType, paramName, namespace);
        _generatedClasses.add(genClass);

        if (responseType != null) {
            paramName = "return";
            genClassName = className + WSDLWriter.VALUE_RESPONSE;
            if (!isPrimitive(responseType)) {
                int idx = responseType.lastIndexOf('/');
                paramName = Character.toLowerCase(responseType.charAt(idx + 1)) + responseType.substring(idx + 2);
            }
            genClass = _loader.getGeneratedClass(genClassName, responseType, paramName, namespace);
            _generatedClasses.add(genClass);
        }
    }

    /**
     * Generate the schema using JAXB and attach to the Node specified.
     *
     * @param schema the types Node in a WSDL document
     * @throws WSDLWriterException if the schema cannot be generated
     */
    public void generateSchema(final Node schema) throws WSDLWriterException {
        Node doc = null;
        Set<DOMResult> results = new HashSet<DOMResult>();
        try {
            int size = _generatedClasses.size();
            Class<?>[] classes = new Class<?>[size];
            int i = 0;
            for (Class<?> clz : _generatedClasses) {
                classes[i++] = clz;
            }
            JAXBContext context = JAXBContext.newInstance(classes, null);
            context.generateSchema(new MySchemaOutputResolver(schema, results));

            for (DOMResult dr : results) {
                Element n = (Element) dr.getNode();
                NodeList children = n.getElementsByTagName(WSDLWriter.ELEMENT_XS_IMPORT);
                int childCount = children.getLength();
                for (int j = 0; j < childCount; j++) {
                    Element imp = (Element) children.item(j);
                    imp.removeAttribute(WSDLWriter.ATTR_SCHEMA_LOCATION);
                }
            }
        } catch (JAXBException jbe) {
            throw new WSDLWriterException(jbe);
        } catch (IOException ioe) {
            throw new WSDLWriterException(ioe);
        }
    }

    private boolean isPrimitive(String type) throws WSDLWriterException {
        String name = type.replace('/','.');
        if (PRIMITIVE_MAP.get(name) != null) {
            return true;
        }
        Class<?> typeClass = Classes.forName(name);
        if (typeClass == null) {
            throw new WSDLWriterException("Class not found " + type);
        }
        return typeClass.isPrimitive();
    }

    private static byte[] generateWrapper(String className, String fieldType, String param, String namespace) {
        String name = className.substring(className.lastIndexOf('/') + 1);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        String primType = PRIMITIVE_TYPES.get(fieldType);
        String fieldSignature = null;
        if (primType != null) {
            fieldSignature = primType;
        } else {
            fieldSignature = "L" + fieldType + ";";
        }
        ByteArrayOutputStream byteCode = new ByteArrayOutputStream();

        DataOutputStream stream = new DataOutputStream(byteCode);
        try {
            writeClassFile(stream);
            stream.writeShort(23); // pool size
            // The constant pool
            // 1:UTF8 arg0/param
            stream.writeByte(1);
            stream.writeUTF(param);
            // 2:UTF8 fieldSignature
            stream.writeByte(1);
            stream.writeUTF(fieldSignature);
            // 3:UTF8 <init>
            stream.writeByte(1);
            stream.writeUTF("<init>");
            // 4:UTF8 ()V
            stream.writeByte(1);
            stream.writeUTF("()V");
            // 5:UTF8 Code
            stream.writeByte(1);
            stream.writeUTF("Code");
            // 6:UTF8 RuntimeVisibleAnnotations
            stream.writeByte(1);
            stream.writeUTF("RuntimeVisibleAnnotations");
            // 7:UTF8 Ljavax/xml/bind/annotation/XmlRootElement;
            stream.writeByte(1);
            stream.writeUTF("Ljavax/xml/bind/annotation/XmlRootElement;");
            // 8:UTF8 Ljavax/xml/bind/annotation/XmlRootElement;
            stream.writeByte(1);
            stream.writeUTF("Ljavax/xml/bind/annotation/XmlElement;");
            // 9:UTF8 Ljavax/xml/bind/annotation/XmlRootElement;
            stream.writeByte(1);
            stream.writeUTF("Ljavax/xml/bind/annotation/XmlType;");
            // 10:UTF8 Ljavax/xml/bind/annotation/XmlRootElement;
            stream.writeByte(1);
            //stream.writeUTF("Ljavax/xml/bind/annotation/XmlSeeAlso;");
            stream.writeUTF("Ljavax/xml/bind/annotation/XmlSchemaType;");
            //stream.writeUTF("generated by SwitchYard");
            // 11:UTF8 name
            stream.writeByte(1);
            stream.writeUTF("name");
            // 12:UTF8 namespace
            stream.writeByte(1);
            stream.writeUTF("namespace");
            // 13:UTF8 name value
            stream.writeByte(1);
            stream.writeUTF(name);
            // 14:UTF8 namespace value
            stream.writeByte(1);
            stream.writeUTF(namespace);
            // 15:UTF8 className
            stream.writeByte(1);
            stream.writeUTF(className);
            // 16:UTF8 java/lang/Object
            stream.writeByte(1);
            stream.writeUTF("java/lang/Object");
            // 17:NameAndType
            stream.writeByte(12);
            stream.writeShort(3);
            stream.writeShort(4);
            // 18:Method Ref
            stream.writeByte(10);
            stream.writeShort(19);
            stream.writeShort(17);
            // 19:Class Ref java/lang/Object
            stream.writeByte(7);
            stream.writeShort(16);
            // 20:Class Ref className
            stream.writeByte(7);
            stream.writeShort(15);
            // 21:UTF8 fieldName
            stream.writeByte(1);
            stream.writeUTF(param);
            // 22:UTF8 value
            stream.writeByte(1);
            stream.writeUTF("value");

            writeClass(stream);
            writeFields(stream);
            writeMethods(stream);
            writeClassAnnotations(stream);


        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            stream.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        byte[] bytes = byteCode.toByteArray();
        return bytes;
    }

    private static void writeClassFile(DataOutputStream stream) throws IOException {
        stream.writeInt(0xCAFEBABE); // magic
        stream.writeInt(0x32); // version
    }

    private static void writeClass(DataOutputStream stream) throws IOException {
        stream.writeShort(0x0020 | 0x0001); // Super | Public
        stream.writeShort(20); // Class index
        stream.writeShort(19); // Super Class index
        stream.writeShort(0); // Interfaces count
    }

    private static void writeFields(DataOutputStream stream) throws IOException {
        // Fields - we have one
        stream.writeShort(1); // field count
        stream.writeShort(0x0001); // Public
        //stream.writeShort(0x0002); // Private
        stream.writeShort(1); // Name index
        stream.writeShort(2); // Type index
        // Attributes like annotations
        stream.writeShort(1); // attributes length
        stream.writeShort(6); // RuntimeVisibleAnnotations
        stream.writeInt(16); // length
        stream.writeShort(1); // no of annotations
        stream.writeShort(8); // ref to type
        stream.writeShort(2); // values size
        stream.writeShort(11); // ref to name[0]
        stream.writeByte(115); // type=value
        stream.writeShort(21); // ref to value[0]
        stream.writeShort(12); // ref to name[0]
        stream.writeByte(115); // type=value
        stream.writeShort(14); // ref to value[0]
    }

    private static void writeMethods(DataOutputStream stream) throws IOException {
        // Methods - we have one
        stream.writeShort(1); // method count
        stream.writeShort(0x0001); // Public
        stream.writeShort(3); // Name index
        stream.writeShort(4); // Type index
        // Method attributes - we have Code
        stream.writeShort(1);
        stream.writeShort(5); // Name index
        stream.writeInt(17); // Code size
        stream.writeShort(1); // Stack
        stream.writeShort(1); // Locals
        stream.writeInt(5); // Byte code length
        // byte code
        stream.writeByte(42); // aload_0
        stream.writeByte(183); // invokespecial
        stream.writeShort(18); // method ref
        stream.writeByte(177); // return
        // No exceptions
        stream.writeShort(0); // exception table length
        // No attributes like LineNumberTable
        stream.writeShort(0);
    }

    private static void writeClassAnnotations(DataOutputStream stream) throws IOException {
        // Class attributes like SourceFile, Annotations
        stream.writeShort(1); // attributes length
        stream.writeShort(6); // RuntimeVisibleAnnotations
        stream.writeInt(30); // length
        stream.writeShort(2); // no of annotations
        stream.writeShort(7); // ref to type XmlRootElement
        stream.writeShort(2); // values size
        stream.writeShort(11); // ref to name[0]
        stream.writeByte('s'); // type=String
        stream.writeShort(13); // ref to value[0]
        stream.writeShort(12); // ref to name[1]
        stream.writeByte('s'); // type=String
        stream.writeShort(14); // ref to value[1]
        stream.writeShort(9); // ref to type XmlType
        stream.writeShort(2); // values size
        stream.writeShort(11); // ref to name[0]
        stream.writeByte('s'); // type=String
        stream.writeShort(13); // ref to value[0]
        stream.writeShort(12); // ref to name[1]
        stream.writeByte('s'); // type=value
        stream.writeShort(14); // ref to value[1]
    }

    private static class GeneratedClassLoader extends ClassLoader {
        private Map<String, Class<?>> _generatedClasses = new HashMap<String, Class<?>>();

        /**
         * Construct a classloader that delegates to parent.
         *
         * @param parentLoader the parent classloader
         */
        public GeneratedClassLoader(ClassLoader parentLoader) {
            super(parentLoader);
        }

        /**
         * Returns the class that is already generated or generates one based on parameters.
         *
         * @param className the Class name
         * @param fieldType the field type to be included in the Class
         * @param param the name of the field
         * @param param the namespace for the field and Class
         * @return the generated Class
         */
        public Class<?> getGeneratedClass(String className, String fieldType, String param, String namespace) {
            String key = className.replace('/', '.');
            Class<?> cls = _generatedClasses.get(key);
            if (cls == null) {
                byte[] byteCode = generateWrapper(className, fieldType, param, namespace);
                cls = defineClass(key, byteCode, 0, byteCode.length);
                _generatedClasses.put(key, cls);
            }
            return cls;
        }
    }

    private static class MySchemaOutputResolver extends SchemaOutputResolver {
        private Set<DOMResult> _results;
        private Node _schema;

        /**
         * Construct a schema resolver.
         *
         * @param schema the parent Node where schemas will be added
         * @param results the DomResult set
         */
        public MySchemaOutputResolver(Node schema, Set<DOMResult> results) {
            _results = results;
            _schema = schema;
        }

        @Override
        public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
            DOMResult result = new DOMResult(_schema);
            result.setSystemId(namespaceUri);
            _results.add(result);
            return result;
        }
    }
}
