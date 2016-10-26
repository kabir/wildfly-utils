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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jboss.modules.ModuleIdentifier;
import org.wildfly.util.module.dependency.DependencyPathDFS;
import org.wildfly.util.module.dependency.EndSearchCondition;
import org.wildfly.util.module.dependency.ModuleDependency;
import org.wildfly.util.module.dependency.ModuleGraph;
import org.wildfly.util.module.dependency.ModuleSpecFinder;
import org.wildfly.util.module.dependency.Subsystems;
import org.wildfly.util.module.dependency.Util;

/**
 * Hello world!
 *
 */
public class SubsystemReachabilityUtil
{
    public static void main( String[] args ) throws Exception {
        String modulesDir = System.getProperty("jboss.as.modules.dir", "/Users/kabir/sourcecontrol/wildfly/git/wildfly/build/target/wildfly-11.0.0.Alpha1-SNAPSHOT/modules/system/layers/base/");
        File root = new File(modulesDir);

        ModuleSpecFinder finder = ModuleSpecFinder.Factory.create(root);
        ModuleGraph graph = new ModuleGraph(finder);

        Subsystems subsystems = Subsystems.create(finder);
        subsystemsFrom(graph, subsystems, "org.jboss.as.server");
    }

    private static void subsystemsFrom(final ModuleGraph graph, final Subsystems subsystems, final String source) {
    	final ModuleIdentifier sourceId = ModuleIdentifier.create(source);
        DependencyPathDFS coreModules = new DependencyPathDFS(graph, sourceId);

        Set<ModuleIdentifier> nonReachable = new TreeSet<>(Util.MODULE_ID_COMPARATOR);
        Map<ModuleIdentifier, List<ModuleIdentifier>> reachable = new TreeMap<>(Util.MODULE_ID_COMPARATOR);
        for (ModuleIdentifier subsystem : subsystems.getNames()) {
        	List<ModuleIdentifier> path = coreModules.simplePathTo(subsystem);
        	if (path.size() == 0){
        		nonReachable.add(subsystem);
        	} else {
        		reachable.put(subsystem, path);
        	}
        }

        System.out.println("\n\n****** Subsystems not reachable from " + sourceId + "\n");
        for (ModuleIdentifier id : nonReachable) {
        	System.out.println(id);
        }

        System.out.println("\n\n****** Subsystems reachable from " + sourceId + "\n");
        for (Map.Entry<ModuleIdentifier, List<ModuleIdentifier>> entry : reachable.entrySet()) {
        	System.out.println("* " + entry.getKey() + "\n" + format(entry.getValue(), 5));
        }


        final Map<ModuleIdentifier, ModuleIdentifier> directCoreSubsystemsWithParents = new TreeMap<>(Util.MODULE_ID_COMPARATOR);
        final DependencyPathDFS nonSubsystemCoreModulesStrict = new DependencyPathDFS(graph, sourceId, new EndSearchCondition() {
            @Override
            public boolean endSearch(ModuleIdentifier id, ModuleDependency dep) {
                ModuleIdentifier depId = dep.getToId();
                if (subsystems.getNames().contains(depId)){
                    directCoreSubsystemsWithParents.put(depId, id);
                    return true;
                }
                return false;
            }
        });

        final TreeSet<ModuleIdentifier> nonSubsystemCoreModulesStrictTree = new TreeSet<ModuleIdentifier>(Util.MODULE_ID_COMPARATOR);
        nonSubsystemCoreModulesStrictTree.addAll(nonSubsystemCoreModulesStrict.getAllModules());
        System.out.println("\n\n****** All modules reachable from server but not via subsystems (strict)" + nonSubsystemCoreModulesStrictTree.size());
        for (ModuleIdentifier id : nonSubsystemCoreModulesStrictTree){
            System.out.println(id);
        }
        System.out.println("\n\n****** Subsystems reachable from server (not via other subsystem)" + directCoreSubsystemsWithParents.size());
        for (Map.Entry<ModuleIdentifier, ModuleIdentifier> entry : directCoreSubsystemsWithParents.entrySet()){
            ModuleIdentifier parent = entry.getValue();
            List<ModuleIdentifier> pathToParent = nonSubsystemCoreModulesStrict.simplePathTo(parent);
            List<ModuleIdentifier> path = new ArrayList<>(pathToParent);
            path.add(entry.getKey());
            System.out.println(entry.getKey() + " " + path);
        }

        Map<ModuleIdentifier, Set<ModuleIdentifier>> subsystemsBySubsystems = new TreeMap<>(Util.MODULE_ID_COMPARATOR);
        for (final ModuleIdentifier subsystemId : subsystems.getNames()) {
            DependencyPathDFS dfs = new DependencyPathDFS(graph, subsystemId, new EndSearchCondition() {
                public boolean endSearch(ModuleIdentifier id, ModuleDependency dep) {
                    if (subsystems.getNames().contains(dep.getFromId()) && !subsystemId.equals(id)) {
                        return true;
                    }
                    if (sourceId.equals(id)){
                        return true;
                    }
                    if (nonSubsystemCoreModulesStrictTree.contains(dep.getToId())){
                        return true;
                    }
                    return false;
                }
            });
            Set<ModuleIdentifier> modules = new TreeSet<>(Util.MODULE_ID_COMPARATOR);
            modules.addAll(dfs.getAllModules());
            subsystemsBySubsystems.put(subsystemId, modules);
        }

        System.out.println("\n\n****** All modules (apart from stuff reachable from server) reachable from a subsystem, stopping at a subsystem");
        for (Map.Entry<ModuleIdentifier, Set<ModuleIdentifier>> entry : subsystemsBySubsystems.entrySet()) {
            System.out.println("------ " + entry.getKey());
            for (ModuleIdentifier module : entry.getValue()){
                if (subsystems.getNames().contains(module)) {
                    System.out.println(module + " *");
                }
            }
            for (ModuleIdentifier module : entry.getValue()){
                if (!subsystems.getNames().contains(module)) {
                    System.out.println(module);
                }
            }
        }



//        System.out.println("\n====== All paths for subsystems reachable from " + sourceId + "\n");
//        for (ModuleIdentifier subsystemId : reachable.keySet()) {
//            System.out.println("----- " + subsystemId + "\n");
//            AllDependencyPathsDFS search = new AllDependencyPathsDFS(graph, sourceId, subsystemId);
//            for (List<ModuleIdentifier> path : search.pathsTo()) {
//                System.out.println(format(path, 5));
//            }
//        }
    }


    static String format(List<?> list, int size) {
    	StringBuilder sb = new StringBuilder();
    	int i = 0;
    	sb.append("            ");
    	sb.append('[');
    	for (Object o : list){
    		if (i != 0) {
    			sb.append(",");
    			if (i % size == 0){
    				sb.append("\n            ");
    			}
    		}
			sb.append(o);
    		i++;
    	}
    	sb.append(']');
    	return sb.toString();
    }
}
