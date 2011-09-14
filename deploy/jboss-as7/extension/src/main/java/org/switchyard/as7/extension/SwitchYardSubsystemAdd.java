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
package org.switchyard.as7.extension;

import static org.switchyard.as7.extension.CommonAttributes.MODULES;
import static org.switchyard.as7.extension.CommonAttributes.PROPERTIES;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;
import org.switchyard.as7.extension.deployment.SwitchYardCdiIntegrationProcessor;
import org.switchyard.as7.extension.deployment.SwitchYardConfigDeploymentProcessor;
import org.switchyard.as7.extension.deployment.SwitchYardConfigProcessor;
import org.switchyard.as7.extension.deployment.SwitchYardDependencyProcessor;
import org.switchyard.as7.extension.deployment.SwitchYardDeploymentProcessor;
import org.switchyard.as7.extension.services.SwitchYardAdminService;
import org.switchyard.as7.extension.services.SwitchYardServiceDomainManagerService;
import org.switchyard.config.Configuration;
import org.switchyard.config.ConfigurationPuller;
import org.switchyard.deploy.BaseComponent;
import org.switchyard.deploy.Component;

/**
 * The SwitchYard subsystem add update handler.
 *
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public final class SwitchYardSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final Logger LOG = Logger.getLogger("org.switchyard");

    static final SwitchYardSubsystemAdd INSTANCE = new SwitchYardSubsystemAdd();

    // Private to ensure a singleton.
    private SwitchYardSubsystemAdd() {
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode submodel) throws OperationFailedException {
        if (operation.has(MODULES)) {
            submodel.get(MODULES).set(operation.get(MODULES));
        }
        if (operation.has(PROPERTIES)) {
            submodel.get(PROPERTIES).set(operation.get(PROPERTIES));
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final List<ModuleIdentifier> modules = new ArrayList<ModuleIdentifier>();
        final List<Component> components = new ArrayList<Component>();
        if (operation.has(MODULES)) {
            ModelNode opmodules = operation.get(MODULES);
            Set<String> keys = opmodules.keys();
            if (keys != null) {
                for (String current : keys) {
                    ModuleIdentifier moduleIdentifier = ModuleIdentifier.fromString(current);
                    modules.add(moduleIdentifier);
                    Class<? extends BaseComponent> componentClass;
                    try {
                        componentClass = Module.loadClassFromCallerModuleLoader(moduleIdentifier, current + ".deploy.ComponentImpl").asSubclass(BaseComponent.class);
                        Component component;
                        try {
                            component = componentClass.newInstance();
                            ModelNode opmodule = opmodules.get(current);
                            if (opmodule.has(PROPERTIES)) {
                                ModelNode properties = opmodule.get(PROPERTIES);
                                Set<String> propertyNames = properties.keys();
                                if (propertyNames != null) {
                                    Configuration envConfig = new ConfigurationPuller().pull(new QName("properties"));
                                    for (String propertyName : propertyNames) {
                                        Configuration propConfig = new ConfigurationPuller().pull(new QName(propertyName));
                                        propConfig.setValue(properties.get(propertyName).asString());
                                        envConfig.addChild(propConfig);
                                    }
                                    component.init(envConfig);
                                }
                            }
                            components.add(component);
                        } catch (InstantiationException e) {
                            LOG.error("Unable to instantiate class " + componentClass);
                        } catch (IllegalAccessException e) {
                            LOG.error("Unable to access constructor for " + componentClass);
                        }
                    } catch (ClassNotFoundException e1) {
                        LOG.error("Unable to load class " + current + ".deploy.ComponentImpl");
                    } catch (ModuleLoadException mle) {
                        LOG.error("Unable to load module " + moduleIdentifier);
                    }
                }
            }
        }

        if (operation.has(CommonAttributes.MODULES)) {
            ModelNode opmodules = operation.get(CommonAttributes.MODULES);
            Set<String> keys = opmodules.keys();
            if (keys != null) {
                for (String current : keys) {
                    modules.add(ModuleIdentifier.fromString(current));
                }
            }
        }
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                int priority = 0x4000;
                processorTarget.addDeploymentProcessor(Phase.PARSE, priority++, new SwitchYardConfigDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, priority++, new SwitchYardDependencyProcessor(modules));
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, priority++, new SwitchYardCdiIntegrationProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, priority++, new SwitchYardConfigProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, priority++, new SwitchYardDeploymentProcessor(components));
            }
        }, OperationContext.Stage.RUNTIME);
        LOG.info("Activating SwitchYard Extension");

        // TODO: introspect switchyard version
        final String version = "0.3.0";
        final SwitchYardAdminService adminService = new SwitchYardAdminService(version);
        newControllers.add(context.getServiceTarget().addService(SwitchYardAdminService.SERVICE_NAME, adminService)
                .install());

        // Add the AS7 Service for the ServiceDomainManager...
        newControllers.add(context.getServiceTarget().addService(SwitchYardServiceDomainManagerService.SERVICE_NAME, new SwitchYardServiceDomainManagerService()).install());
    }

}
