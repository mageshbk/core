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
package org.switchyard.config;

import java.util.Properties;

/**
 * Environment level properties needed to configure SwitchYard runtime
 * are maintained here.
 * 
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public final class Environment {

    /**
     * The delimiter used for prefixing component properties.
     */
    public static final String DELIMITER = ".";

    private static Properties _props = new Properties();

    private Environment() {
        
    }

    /**
     * Returns the SwitchYard Environment properties.
     * 
     * @return the properties
     */
    public static Properties getProperties() {
      return _props;
    }

    /**
     * Sets the SwitchYard Environment properties.
     * 
     * @param props the environment properties
     */
    public static void setProperties(Properties props) {
      if (props == null) {
        _props = new Properties();
      }
      _props = props;
    }

    /**
     * Get the SwitchYard Environment property.
     * 
     * @param name the name of the property
     * @return the property value
     */
    public static String getProperty(String name) {
        assertName(name);
        return _props.getProperty(name);
    }

    /**
     * Returns the SwitchYard Environment property. Returns the default value passed if none has been set.
     * 
     * @param name the name of the property
     * @param defaultValue the default value if not set
     * @return the property value or the default value if the property is not set
     */
    public static String getProperty(String name, String defaultValue) {
        assertName(name);
        return _props.getProperty(name, defaultValue);
    }

    /**
     * Sets the SwitchYard Environment property.
     * 
     * @param name the name of the property
     * @param value the value to be set
     * @return the previous value of this property
     */
    public static String setProperty(String name, String value) {
        assertName(name);
        return (String)_props.setProperty(name, value);
    }

    /**
     * Clears the SwitchYard Environment property.
     * 
     * @param name the name of the property
     * @return the previous value of this property
     */
    public static String clearProperty(String name) {
        assertName(name);
        return (String)_props.remove(name);
    }

    private static void assertName(String name) {
        if (name == null) {
            throw new NullPointerException("Key cannot be null");
        }
        if (name.equals("")) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
    }
}
