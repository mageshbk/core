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

import java.util.Set;
import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Test;
import org.switchyard.ExchangePattern;
import org.switchyard.metadata.ServiceOperation;
import org.switchyard.metadata.java.JavaService;

public class WSDLWriterTest {

    @Test
    public void writeWsdl() throws Exception {
        JavaService js = JavaService.fromClass(MyInterface.class);
        WSDLWriter writer = new WSDLWriter();
        writer.writeWSDL(new QName("urn:switchyard-extensions-wsdl:orders:1.0","OrderService"), js, "target/order.wsdl");

        WSDLReader reader = new WSDLReader();
        Set<ServiceOperation> ops = reader.readWSDL("target/order.wsdl", null);
        // There should be one operation
        Assert.assertEquals(1, ops.size());

        // method1 is InOnly
        ServiceOperation method1 = null;
        for (ServiceOperation op : ops) {
            method1 = op;
            break;
        }
        Assert.assertNotNull(method1);
        Assert.assertEquals(method1.getName(), "submitOrder");
        Assert.assertEquals(method1.getInputType(), new QName("urn:switchyard-extensions-wsdl:orders:1.0", "submitOrder"));
        Assert.assertEquals(method1.getExchangePattern(), ExchangePattern.IN_ONLY);
    }
}
