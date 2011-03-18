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

package org.switchyard.metadata;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Test;
import org.switchyard.ExchangePattern;
import org.switchyard.metadata.wsdl.WSDLReaderException;
import org.switchyard.metadata.wsdl.WSDLService;

/**
 * Tests for WSDLService
 * 
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class WSDLServiceTest {
    
    static final QName METHOD1_INPUT = new QName("urn:switchyard-metadata-wsdl", "helloWS");
    static final QName METHOD2_INPUT = new QName("urn:switchyard-metadata-wsdl", "sayHello");
    static final QName METHOD2_OUTPUT = new QName("urn:switchyard-metadata-wsdl", "sayHelloResponse");

    @Test
    public void interfaceWSDLService() throws Exception {
        WSDLService wsdlService = WSDLService.fromWSDL("HelloWebService.wsdl", "HelloWebService");
        // There should be two operations
        Assert.assertEquals(2, wsdlService.getOperations().size());
        
        // method1 is InOnly
        ServiceOperation method1 = wsdlService.getOperation("helloWS");
        Assert.assertNotNull(method1);
        Assert.assertEquals(method1.getInputType(), METHOD1_INPUT);
        Assert.assertEquals(method1.getExchangePattern(), ExchangePattern.IN_ONLY);
        
        //method2 is InOut
        ServiceOperation method2 = wsdlService.getOperation("sayHello");
        Assert.assertNotNull(method2);
        Assert.assertEquals(method2.getInputType(), METHOD2_INPUT);
        Assert.assertEquals(method2.getOutputType(), METHOD2_OUTPUT);
        Assert.assertEquals(method2.getExchangePattern(), ExchangePattern.IN_OUT);
    }

    @Test
    public void interfaceWSDLServiceNoPortName() throws Exception {
        WSDLService wsdlService = WSDLService.fromWSDL("HelloWebService.wsdl");
        // There should be two operations
        Assert.assertEquals(2, wsdlService.getOperations().size());
    }

    @Test
    public void nonExistingPort() throws Exception {
        try {
            WSDLService.fromWSDL("HelloWebService.wsdl", "hello");
        } catch (WSDLReaderException e) {
            Assert.assertEquals("Unable to find portType with name hello", e.getMessage());
        }
    }

    @Test
    public void nonExistingFile() throws Exception {
        try {
            WSDLService.fromWSDL("unknown.wsdl", "hello");
        } catch (WSDLReaderException e) {
            Assert.assertEquals("Unable to resolve WSDL document at unknown.wsdl", e.getMessage());
        }
    }

    @Test
    public void improperWSDL() throws Exception {
        try {
            WSDLService.fromWSDL("HelloWebService2.wsdl", "HelloWebService");
        } catch (WSDLReaderException e) {
            Assert.assertEquals("Service operations on a WSDL interface must have exactly one parameter.", e.getMessage());
        }
    }
}
