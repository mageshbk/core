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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.switchyard.common.type.Classes;
import org.w3c.dom.Node;

/**
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class SchemaGen {
    private GeneratedClassLoader _loader;
    private String _schemaLocation;
    private Node _schema;

    public SchemaGen(Node schema, final String schemaLocation) {
        _loader = new GeneratedClassLoader(Classes.getTCCL());
        _schemaLocation = schemaLocation;
        _schema = schema;
    }

    public void generateSchema(final String className, final String namespace, final String requestType) throws WSDLWriterException {
        generateSchema(className, namespace, requestType, null);
    }

    public void generateSchema(final String className, final String namespace, final String requestType, String responseType) throws WSDLWriterException {
        Class<?> request = _loader.getGeneratedClass(className, generateWrapper(className, requestType, "arg0", namespace));
        Class<?> response = null;
        if (responseType != null) {
            response = _loader.getGeneratedClass(className + "Response", generateWrapper(className + "Response", responseType, "return", namespace));
        }

        Node doc = null;
        List<DOMResult> results = new ArrayList<DOMResult>();
        try {
            JAXBContext context = null;
            if (responseType != null) {
                context = JAXBContext.newInstance(request, response);
            } else {
                context = JAXBContext.newInstance(request);
            }
            context.generateSchema(new MySchemaOutputResolver(_schema, results));
            // All packages should have same namespace for now :(, other schemas will be ignored
            // Use a package-info.java to specify the namespace
            // TODO: Support multiple namespaces
            DOMResult domResult = results.get(0);
            doc = domResult.getNode();
        } catch (JAXBException jbe) {
            throw new WSDLWriterException(jbe);
        } catch (IOException ioe) {
            throw new WSDLWriterException(ioe);
        }
    }



    public byte[] generateWrapper(String className, String fieldType, String param, String namespace) {
        String name = className.substring(className.lastIndexOf('/') + 1);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        String fieldSignature = "L" + fieldType + ";";
        String member = fieldType.substring(fieldType.lastIndexOf("/") + 1, fieldType.length());
        String fieldMember = Character.toLowerCase(member.charAt(0)) + member.substring(1);
        ByteArrayOutputStream byteCode = new ByteArrayOutputStream();

        DataOutputStream stream = new DataOutputStream(byteCode);
        try {
            stream.writeInt(0xCAFEBABE);// magic
            stream.writeInt(0x32);// version
            stream.writeShort(23);// pool size
            // The constant pool
            // 1:UTF8 arg0
            stream.writeByte(1);
            stream.writeUTF(fieldMember);
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

            stream.writeShort(0x0020 | 0x0001); // Super | Public
            stream.writeShort(20); // Class index
            stream.writeShort(19); // Super Class index
            stream.writeShort(0); // Interfaces count

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

    private class GeneratedClassLoader extends ClassLoader {
        private Map<String, Class<?>> _generatedClasses = new HashMap<String, Class<?>>();

        public  GeneratedClassLoader(ClassLoader parentLoader) {
            super(parentLoader);
        }

        public Class<?> getGeneratedClass(String className, byte[] byteCode) {
            String key = className.replace('/', '.');
            Class<?> cls = _generatedClasses.get(key);
            if (cls == null) {
                cls = defineClass(key, byteCode, 0, byteCode.length);
                _generatedClasses.put(key, cls);
            }
            return cls;
        }
    }

    private class MySchemaOutputResolver extends SchemaOutputResolver {
        List<DOMResult> _results;
        Node _schema;
        public MySchemaOutputResolver(Node schema, List<DOMResult> results) {
            _results = results;
            _schema = schema;
        }

        public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
            DOMResult result = new DOMResult(_schema);
            result.setSystemId(namespaceUri);
            _results.add(result);
            return result;
        }
    }
}
