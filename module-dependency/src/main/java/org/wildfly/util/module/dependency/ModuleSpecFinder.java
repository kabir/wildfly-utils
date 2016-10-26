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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jboss.modules.ConcreteModuleSpec;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.LocalModuleFinder;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

public interface ModuleSpecFinder {
	ModuleLoader getModuleLoader();

	Map<ModuleIdentifier, Set<ModuleDependency>> findAllModules() throws ModuleLoadException;

    ModuleSpec findModule(ModuleIdentifier moduleId) throws ModuleLoadException;

    Set<ModuleDependency> getModuleDependencies(ModuleSpec moduleSpec) throws ModuleLoadException;

    static class Factory {
    	private static final String MODULE_XML = "module.xml";

    	public static ModuleSpecFinder create(final File root) throws Exception {
            if (!root.exists()){
                throw new IllegalArgumentException(root.getAbsolutePath() + " does not exist");
            }
            if (!root.isDirectory()){
                throw new IllegalArgumentException(root.getAbsolutePath() + " is not a directory");
            }
            final LocalModuleFinder finder = new LocalModuleFinder(new File[]{root});
            final ModuleLoader moduleLoader = new ModuleLoader(new ModuleFinder[]{finder});
            return new ModuleSpecFinder() {

            	Map<ModuleIdentifier, Set<ModuleDependency>> modules;

				@Override
				public ModuleLoader getModuleLoader() {
					return moduleLoader;
				}

				@Override
				public ModuleSpec findModule(ModuleIdentifier moduleId) throws ModuleLoadException {
					ModuleSpec spec = finder.findModule(moduleId, moduleLoader);
					if (spec == null) {
						throw new ModuleLoadException("Could not find" + moduleId);
					}
					return spec;
				}

				@Override
				public Map<ModuleIdentifier, Set<ModuleDependency>> findAllModules() throws ModuleLoadException {
					if (this.modules != null){
						return this.modules;
					}

					this.modules = new HashMap<ModuleIdentifier, Set<ModuleDependency>>();
					addModules(root, modules);
					return modules;
				}


				@Override
				public Set<ModuleDependency> getModuleDependencies(ModuleSpec moduleSpec) throws ModuleLoadException {
					Set<ModuleDependency> deps = new HashSet<>();
					if (moduleSpec instanceof ConcreteModuleSpec){
						for (DependencySpec dep : ((ConcreteModuleSpec)moduleSpec).getDependencies()){
							if (dep instanceof ModuleDependencySpec){
								ModuleDependencySpec depSpec = (ModuleDependencySpec)dep;
								deps.add(new ModuleDependency(((ConcreteModuleSpec)moduleSpec).getModuleIdentifier(), depSpec.getIdentifier(), depSpec.isOptional()));
							}
						}
					}
					return deps;
				}

			    private void addModules(File dir, Map<ModuleIdentifier, Set<ModuleDependency>> modules) throws ModuleLoadException {
			        for (File file : dir.listFiles()){
			            if (file.isDirectory()){
			                addModules(file, modules);
			            } else if (file.getName().equals(MODULE_XML)){
			            	ModuleIdentifier identifier = createModuleIdentifier(file);
			            	//System.out.println(identifier);
			            	ModuleSpec moduleSpec = findModule(identifier);
			            	Set<ModuleDependency> dependencies = getModuleDependencies(moduleSpec);
			            	modules.put(identifier, dependencies);
			            }
			        }
			    }

			    private ModuleIdentifier createModuleIdentifier(File moduleXml){
			    	File current = moduleXml.getParentFile();
			    	String slot = current.getName();
			    	current = current.getParentFile();
			    	Stack<String> nameStack = new Stack<String>();
			    	while (!current.equals(root)){
			    		nameStack.push(current.getName());
			    		current = current.getParentFile();
			    	}

			    	StringBuilder sb = new StringBuilder();
			    	boolean first = true;
			    	while (nameStack.size() > 0){

			    		if (first){
			    			first = false;
			    		} else {
			    			sb.append(".");
			    		}
			    		sb.append(nameStack.pop());
			    	}
			    	return ModuleIdentifier.create(sb.toString(), slot);
			    }
            };
        }
    }
}
