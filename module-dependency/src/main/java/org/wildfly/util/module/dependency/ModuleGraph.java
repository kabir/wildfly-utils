/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;


public class ModuleGraph {
    private Map<ModuleIdentifier, Set<ModuleDependency>> modules;

    ModuleGraph(ModuleSpecFinder finder) throws ModuleLoadException {
        this.modules = new HashMap<>(finder.findAllModules());
        //System.out.println(modules);
    }

    ModuleGraph(Map<ModuleIdentifier, Set<ModuleDependency>> modules) {
    	this.modules = new HashMap<>(modules);
    }

    ModuleGraph() {
    	this.modules = new HashMap<>();
    }

    public Set<ModuleDependency> getDependencies(ModuleIdentifier id) {
    	Set<ModuleDependency> deps = modules.get(id);
    	if (deps != null) {
    		return modules.get(id);
    	}
    	return Collections.emptySet();
    }

    public void addDependency(ModuleIdentifier from, ModuleDependency to) {
    	Set<ModuleDependency> deps = modules.get(from);
    	if (deps == null){
    		deps = new HashSet<>();
    		modules.put(from, deps);
    	}
    	deps.add(to);
    }

    public ModuleGraph reverse() {
    	ModuleGraph reverse = new ModuleGraph();
    	for (Map.Entry<ModuleIdentifier, Set<ModuleDependency>> entry : modules.entrySet()) {
    		final ModuleIdentifier from = entry.getKey();
    		for (ModuleDependency to : entry.getValue()) {
    			reverse.addDependency(to.getToId(), new ModuleDependency(to.getToId(), from, to.isOptional()));
    		}
    	}
    	return reverse;
    }
}
