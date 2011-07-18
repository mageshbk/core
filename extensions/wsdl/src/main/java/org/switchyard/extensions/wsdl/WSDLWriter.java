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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.switchyard.ExchangePattern;
import org.switchyard.common.xml.XMLHelper;
import org.switchyard.metadata.ServiceInterface;
import org.switchyard.metadata.ServiceOperation;
import org.switchyard.metadata.java.JavaService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class WSDLWriter {

    private static final Logger LOGGER = Logger.getLogger(WSDLWriter.class);
    public static final String WSDLNS_URI = "http://schemas.xmlsoap.org/wsdl/";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    public static final String SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    public static final String SOAP_URI = "http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String SOAP_HTTP_TRANSPORT = "http://schemas.xmlsoap.org/soap/http";
    public static final String ATTR_BINDING = "binding";
    public static final String ATTR_ELEMENT = "element";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TARGET_NS = "targetNamespace";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_XMLNS = "xmlns";
    public static final String ATTR_SOAP = "soap";
    public static final String ATTR_XSD = "xsd";
    public static final String ATTR_TNS = "tns";
    public static final String ATTR_LOCATION = "location";
    public static final String ATTR_USE = "use";
    public static final String ATTR_SOAP_LOCATION = "soapLocation";
    public static final String ATTR_NAMESPACE = "namespace";
    public static final String ATTR_SCHEMA_LOCATION = "schemaLocation";
    public static final String ATTR_STYLE = "style";
    public static final String ATTR_TRANSPORT = "transport";
    public static final String QUALIFIER = ":";

    public static final String ELEMENT_DEFINITIONS = "definitions";
    public static final String ELEMENT_SERVICE = "service";
    public static final String ELEMENT_PORT = "port";
    public static final String ELEMENT_BINDING = "binding";
    public static final String ELEMENT_OPERATION = "operation";
    public static final String ELEMENT_INPUT = "input";
    public static final String ELEMENT_OUTPUT = "output";
    public static final String ELEMENT_FAULT = "fault";
    public static final String ELEMENT_ADDRESS = "address";
    public static final String ELEMENT_BODY = "body";
    public static final String ELEMENT_PORT_TYPE = "portType";
    public static final String ELEMENT_MESSAGE = "message";
    public static final String ELEMENT_PART = "part";
    public static final String ELEMENT_TYPES = "types";
    public static final String ELEMENT_SCHEMA = "schema";
    public static final String ELEMENT_IMPORT = "import";

    public static final String VALUE_LITERAL = "literal";
    public static final String VALUE_RESPONSE = "Response";
    public static final String VALUE_DOCUMENT = "document";

    /**
     * Create WSDL document from a SwitchYard ServiceInterface.
     *
     * @param serviceName QName of the SwitchYard service.
     * @param serviceInt The SwitchYard service Interface.
     * @return the ServiceOperations.
     */
    public static void writeWSDL(final QName serviceName, final ServiceInterface serviceInt) throws WSDLWriterException {
        writeWSDL(serviceName, serviceInt, null);
    }

    /**
     * Create WSDL document from a SwitchYard ServiceInterface.
     *
     * @param serviceName QName of the SwitchYard Service.
     * @param serviceInt The SwitchYard service Interface.
     * @param filename The WSDL filename.
     * @return the ServiceOperations.
     */
    public static void writeWSDL(final QName serviceName, final ServiceInterface serviceInt, String filename) throws WSDLWriterException {
        if (filename == null) {
            filename = serviceName + ".wsdl";
        }
        String name = serviceName.getLocalPart();
        String namespace = serviceName.getNamespaceURI();
        if (namespace != null && namespace.equals("")){
            namespace = "urn:some-default";
        }
        FileOutputStream outStream = null;
        try {
            LOGGER.debug("Creating document at '" + filename + "'");
            Document doc = XMLHelper.getNewDocument();
            Element definitions = doc.createElement(ELEMENT_DEFINITIONS);
            definitions.setAttribute(ATTR_NAME, name);
            definitions.setAttribute(ATTR_TARGET_NS, namespace);
            definitions.setAttribute(ATTR_XMLNS, WSDLNS_URI);
            definitions.setAttribute(ATTR_XMLNS + QUALIFIER + ATTR_XSD, SCHEMA_URI);
            definitions.setAttribute(ATTR_XMLNS + QUALIFIER + ATTR_SOAP, SOAP_URI);
            definitions.setAttribute(ATTR_XMLNS + QUALIFIER + ATTR_TNS, namespace);

            Element types = doc.createElement(ELEMENT_TYPES);

            Element portType = doc.createElement(ELEMENT_PORT_TYPE);
            portType.setAttribute(ATTR_NAME, name);

            Element binding = doc.createElement(ELEMENT_BINDING);
            binding.setAttribute(ATTR_NAME, name + "PortBinding");
            binding.setAttribute(ATTR_TYPE, ATTR_TNS + QUALIFIER + name);

            Set<ServiceOperation> operations = serviceInt.getOperations();
            SchemaGen schemaGen = new SchemaGen(types, null);

            for (ServiceOperation operation : operations) {
                // For input, output and fault
                //Node schema = null;
                String inputName = operation.getName();
                String className =  Character.toUpperCase(inputName.charAt(0)) + inputName.substring(1);
                Class <?> intfClass = ((JavaService) serviceInt).getJavaInterface();
                if (intfClass.getPackage() != null) {
                    className = intfClass.getPackage().getName().replace('.', '/') + "/" + className;
                }
                String inputType = operation.getInputType().toString();
                String requestType = inputType.substring(inputType.indexOf(':') + 1).replace('.','/');
                Element inMessage = doc.createElement(ELEMENT_MESSAGE);
                inMessage.setAttribute(ATTR_NAME, operation.getName());
                Element inPart = doc.createElement(ELEMENT_PART);
                inPart.setAttribute(ATTR_ELEMENT, ATTR_TNS + QUALIFIER + inputName);
                inPart.setAttribute(ATTR_NAME, inputName);
                inMessage.appendChild(inPart);
                definitions.appendChild(inMessage);

                Element portOperation = doc.createElement(ELEMENT_OPERATION);
                portOperation.setAttribute(ATTR_NAME, operation.getName());
                Element portInput = doc.createElement(ELEMENT_INPUT);
                portInput.setAttribute(ATTR_MESSAGE, ATTR_TNS + QUALIFIER + inputName);
                portOperation.appendChild(portInput);

                Element eoperation = doc.createElement(ELEMENT_OPERATION);
                eoperation.setAttribute(ATTR_NAME, inputName);
                Element soapOperation = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_OPERATION);
                soapOperation.setAttribute(ATTR_SOAP_LOCATION, "");
                eoperation.appendChild(soapOperation);
                Element input = doc.createElement(ELEMENT_INPUT);
                Element soapBody1 = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_BODY);
                soapBody1.setAttribute(ATTR_USE, VALUE_LITERAL);
                input.appendChild(soapBody1);
                eoperation.appendChild(input);

                if (operation.getExchangePattern() == ExchangePattern.IN_OUT) {
                    // If there is an output
                    String outputName = inputName + VALUE_RESPONSE;
                    Element outMessage = doc.createElement(ELEMENT_MESSAGE);
                    outMessage.setAttribute(ATTR_NAME, outputName);
                    Element outPart = doc.createElement(ELEMENT_PART);
                    outPart.setAttribute(ATTR_ELEMENT, ATTR_TNS + QUALIFIER + outputName);
                    outPart.setAttribute(ATTR_NAME, outputName);
                    outMessage.appendChild(outPart);
                    definitions.appendChild(outMessage);

                    Element portOutput = doc.createElement(ELEMENT_OUTPUT);
                    portOutput.setAttribute(ATTR_MESSAGE, ATTR_TNS + QUALIFIER + outputName);
                    portOperation.appendChild(portOutput);
                    portType.appendChild(portOperation);

                    Element output = doc.createElement(ELEMENT_OUTPUT);
                    Element soapBody2 = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_BODY);
                    soapBody2.setAttribute(ATTR_USE, VALUE_LITERAL);
                    output.appendChild(soapBody2);
                    eoperation.appendChild(output);
                    String outputType = operation.getOutputType().toString();
                    String responseType = outputType.substring(outputType.indexOf(':') + 1).replace('.','/');
                    schemaGen.generateSchema(className, namespace, requestType, responseType);
                } else {
                    schemaGen.generateSchema(className, namespace, requestType);
                }
                Element soapBinding = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_BINDING);
                soapBinding.setAttribute(ATTR_TRANSPORT, SOAP_HTTP_TRANSPORT);
                soapBinding.setAttribute(ATTR_STYLE, VALUE_DOCUMENT);
                binding.appendChild(soapBinding);
                binding.appendChild(eoperation);
            }

            definitions.appendChild(types);
            definitions.appendChild(portType);
            definitions.appendChild(binding);

            Element service = doc.createElement(ELEMENT_SERVICE);
            service.setAttribute(ATTR_NAME, name);
            Element port = doc.createElement(ELEMENT_PORT);
            port.setAttribute(ATTR_NAME, name + "Port");
            port.setAttribute(ATTR_BINDING, ATTR_TNS + QUALIFIER + name + "PortBinding");
            Element soapAddress = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_ADDRESS);
            soapAddress.setAttribute(ATTR_LOCATION, "REPLACE_WITH_ACTUAL_URL");
            port.appendChild(soapAddress);
            service.appendChild(port);
            definitions.appendChild(service);

            doc.appendChild(definitions);

            Source source = new DOMSource(doc);
            File file = new File(filename);
            outStream = new FileOutputStream(file);
            Result result = new StreamResult(outStream);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.transform(source, result);

        } catch (FileNotFoundException fnfe) {
            throw new WSDLWriterException(fnfe);
        } catch (ParserConfigurationException pce) {
            throw new WSDLWriterException(pce);
        } catch (TransformerConfigurationException tce) {
            throw new WSDLWriterException(tce);
        } catch (TransformerException te) {
            throw new WSDLWriterException(te);
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ioe) {
                    // Ignore
                }
            }
        }
    }
}

