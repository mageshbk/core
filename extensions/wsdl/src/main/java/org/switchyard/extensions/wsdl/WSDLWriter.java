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
 * A utility class that generates a WSDL from plain Java Interface.
 * 
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class WSDLWriter {

    private static final Logger LOGGER = Logger.getLogger(WSDLWriter.class);
    protected static final String WSDLNS_URI = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    protected static final String SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    protected static final String SOAP_URI = "http://schemas.xmlsoap.org/wsdl/soap/";
    protected static final String SOAP_HTTP_TRANSPORT = "http://schemas.xmlsoap.org/soap/http";
    protected static final String ATTR_BINDING = "binding";
    protected static final String ATTR_ELEMENT = "element";
    protected static final String ATTR_MESSAGE = "message";
    protected static final String ATTR_NAME = "name";
    protected static final String ATTR_TARGET_NS = "targetNamespace";
    protected static final String ATTR_TYPE = "type";
    protected static final String ATTR_XMLNS = "xmlns";
    protected static final String ATTR_SOAP = "soap";
    protected static final String ATTR_XS = "xs";
    protected static final String ATTR_XSD = "xsd";
    protected static final String ATTR_TNS = "tns";
    protected static final String ATTR_LOCATION = "location";
    protected static final String ATTR_USE = "use";
    protected static final String ATTR_SOAP_LOCATION = "soapLocation";
    protected static final String ATTR_NAMESPACE = "namespace";
    protected static final String ATTR_SCHEMA_LOCATION = "schemaLocation";
    protected static final String ATTR_STYLE = "style";
    protected static final String ATTR_TRANSPORT = "transport";
    protected static final String QUALIFIER = ":";

    protected static final String ELEMENT_DEFINITIONS = "definitions";
    protected static final String ELEMENT_SERVICE = "service";
    protected static final String ELEMENT_PORT = "port";
    protected static final String ELEMENT_BINDING = "binding";
    protected static final String ELEMENT_OPERATION = "operation";
    protected static final String ELEMENT_INPUT = "input";
    protected static final String ELEMENT_OUTPUT = "output";
    protected static final String ELEMENT_FAULT = "fault";
    protected static final String ELEMENT_ADDRESS = "address";
    protected static final String ELEMENT_BODY = "body";
    protected static final String ELEMENT_PORT_TYPE = "portType";
    protected static final String ELEMENT_MESSAGE = "message";
    protected static final String ELEMENT_PART = "part";
    protected static final String ELEMENT_TYPES = "types";
    protected static final String ELEMENT_SCHEMA = "schema";
    protected static final String ELEMENT_IMPORT = "import";
    protected static final String ELEMENT_XS_IMPORT = ATTR_XS + ELEMENT_IMPORT;

    protected static final String VALUE_LITERAL = "literal";
    protected static final String VALUE_RESPONSE = "Response";
    protected static final String VALUE_DOCUMENT = "document";
    protected static final String VALUE_PORT = "Port";
    protected static final String VALUE_PORT_BINDING = "PortBinding";

    /**
     * Generate and write a WSDL document from a SwitchYard ServiceInterface.
     *
     * @param serviceName QName of the SwitchYard service.
     * @param serviceInt The SwitchYard service Interface.
     * @throws WSDLWriterException if the WSDL could not be generated or written.
     */
    public void writeWSDL(final QName serviceName, final ServiceInterface serviceInt) throws WSDLWriterException {
        writeWSDL(serviceName, serviceInt, null);
    }

    /**
     * Generate and write a WSDL document from a SwitchYard ServiceInterface. Optionally accepts a WSDL filename.
     *
     * @param serviceName QName of the SwitchYard Service.
     * @param serviceInt The SwitchYard service Interface.
     * @param filename The WSDL filename.
     * @throws WSDLWriterException if the WSDL could not be generated or written.
     */
    public void writeWSDL(final QName serviceName, final ServiceInterface serviceInt, String filename) throws WSDLWriterException {
        if (filename == null) {
            filename = serviceName + ".wsdl";
        }
        String name = serviceName.getLocalPart();
        String namespace = serviceName.getNamespaceURI();
        if (namespace != null && namespace.equals("")) {
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
            binding.setAttribute(ATTR_NAME, name + VALUE_PORT_BINDING);
            binding.setAttribute(ATTR_TYPE, ATTR_TNS + QUALIFIER + name);

            Set<ServiceOperation> operations = serviceInt.getOperations();
            SchemaGen schemaGen = new SchemaGen();

            for (ServiceOperation operation : operations) {
                // For input, output and fault
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

                    Element output = doc.createElement(ELEMENT_OUTPUT);
                    Element soapBody2 = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_BODY);
                    soapBody2.setAttribute(ATTR_USE, VALUE_LITERAL);
                    output.appendChild(soapBody2);
                    eoperation.appendChild(output);
                    String outputType = operation.getOutputType().toString();
                    String responseType = outputType.substring(outputType.indexOf(':') + 1).replace('.','/');
                    schemaGen.generateClass(className, namespace, requestType, responseType);
                } else {
                    schemaGen.generateClass(className, namespace, requestType);
                }
                portType.appendChild(portOperation);
                Element soapBinding = doc.createElementNS(SOAP_URI, ATTR_SOAP + QUALIFIER + ELEMENT_BINDING);
                soapBinding.setAttribute(ATTR_TRANSPORT, SOAP_HTTP_TRANSPORT);
                soapBinding.setAttribute(ATTR_STYLE, VALUE_DOCUMENT);
                binding.appendChild(soapBinding);
                binding.appendChild(eoperation);
            }
            schemaGen.generateSchema(types);
            definitions.appendChild(types);
            definitions.appendChild(portType);
            definitions.appendChild(binding);

            Element service = doc.createElement(ELEMENT_SERVICE);
            service.setAttribute(ATTR_NAME, name);
            Element port = doc.createElement(ELEMENT_PORT);
            port.setAttribute(ATTR_NAME, name + VALUE_PORT);
            port.setAttribute(ATTR_BINDING, ATTR_TNS + QUALIFIER + name + VALUE_PORT_BINDING);
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
                    LOGGER.error(ioe);
                }
            }
        }
    }
}

