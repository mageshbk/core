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
package org.switchyard.deploy;

import org.switchyard.ServiceDomain;
import org.switchyard.config.Configuration;
import org.switchyard.config.Configurations;

/**
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public abstract class BaseComponent implements Component {

    protected String _name;
    protected Activator _activator;
    protected Configuration _config = Configurations.emptyConfig();

    /*
     * Default constructor
     */
    public BaseComponent() {
    }

    /* (non-Javadoc)
     * @see org.switchyard.deploy.Component#getActivator(org.switchyard.ServiceDomain)
     */
    @Override
    public Activator getActivator(ServiceDomain domain) {
        if (_activator != null) {
            ((BaseActivator) _activator).setServiceDomain(domain);
        }
        return _activator;
    }

    /* (non-Javadoc)
     * @see org.switchyard.deploy.Component#getName()
     */
    @Override
    public String getName() {
        return _name;
    }

    /* (non-Javadoc)
     * @see org.switchyard.deploy.Component#init(org.switchyard.config.Configuration)
     */
    @Override
    public void init(Configuration config) {
        _config = config;
    }

    /* (non-Javadoc)
     * @see org.switchyard.deploy.Component#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
