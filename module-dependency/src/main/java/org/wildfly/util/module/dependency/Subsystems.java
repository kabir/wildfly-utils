/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.util.module.dependency;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

public class Subsystems {

	private static final String CONTROLLER_MODULE_NAME = "org.jboss.as.controller:main";
    private static final String EXTENSION_CLASS_NAME = "org.jboss.as.controller.Extension";

	private final Set<ModuleIdentifier> subsystemNames;

	private Subsystems(Set<ModuleIdentifier> subsystemNames){
		this.subsystemNames = subsystemNames;
	}

	public static Subsystems create(ModuleSpecFinder finder) throws ModuleLoadException, ClassNotFoundException {
		Set<ModuleIdentifier> subsystemNames = new TreeSet<>(Util.MODULE_ID_COMPARATOR);

		final ModuleLoader moduleLoader = finder.getModuleLoader();
		final Module controllerModule = moduleLoader.loadModule(ModuleIdentifier.fromString(CONTROLLER_MODULE_NAME));
        final Class<?> extensionClass = controllerModule.getClassLoader().loadClass(EXTENSION_CLASS_NAME);

        for (ModuleIdentifier moduleId : finder.findAllModules().keySet()) {
            final Module module = moduleLoader.loadModule(moduleId);
            ServiceLoader<?> sl = module.loadService(extensionClass);
            for (Object extension : sl) {
                //Make sure that we don't report imported services, i.e. check that the defining module of the extension
                //is the one we are checking
                ModuleClassLoader extensionCl = (ModuleClassLoader)extension.getClass().getClassLoader();
                ModuleIdentifier extensionId = extensionCl.getModule().getIdentifier();
                if (extensionId.equals(moduleId)) {
                    subsystemNames.add(moduleId);
                    break;
                }
            }
        }


		return new Subsystems(subsystemNames);
	}

	public Set<ModuleIdentifier> getNames() {
		return subsystemNames;
	}
}
