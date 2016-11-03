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

package org.wildfly.util.module.dependency.main;

import static org.wildfly.util.module.dependency.Util.MODULE_ID_COMPARATOR;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modules.ModuleIdentifier;
import org.wildfly.util.module.dependency.DependencyPathDFS;
import org.wildfly.util.module.dependency.EndSearchCondition;
import org.wildfly.util.module.dependency.ModuleDependency;
import org.wildfly.util.module.dependency.ModuleGraph;
import org.wildfly.util.module.dependency.ModuleSpecFinder;
import org.wildfly.util.module.dependency.Subsystems;

/**
 * @author Kabir Khan
 */
public class AllModulesAndJarsForJdk9Util {

    private static final Set<ModuleIdentifier> IGNORE;
    static {
        Set<ModuleIdentifier> ignored = new HashSet<>();

        ignored.add(ModuleIdentifier.fromString("org.jboss.as.server:main"));
        ignored.add(ModuleIdentifier.fromString("org.jboss.as.host-controller:main"));
        ignored.add(ModuleIdentifier.fromString("org.wildfly.common:main"));
        ignored.add(ModuleIdentifier.fromString("org.jboss.as.controller:main"));
        ignored.add(ModuleIdentifier.fromString("org.jboss.remoting:main"));
        ignored.add(ModuleIdentifier.fromString("javax.activation.api:main"));
        ignored.add(ModuleIdentifier.fromString("org.jboss.as.network:main"));

        IGNORE = Collections.unmodifiableSet(ignored);
    }

    public static void main(String[] args) throws Exception {
        final String modulesDir = System.getProperty("jboss.as.modules.dir", "/Users/kabir/sourcecontrol/wildfly/git/wildfly/dist/target/wildfly-11.0.0.Alpha1-SNAPSHOT/modules/system/layers/base/");
        final File root = new File(modulesDir);

        final ModuleSpecFinder finder = ModuleSpecFinder.Factory.create(root);
        final ModuleGraph graph = new ModuleGraph(finder).reverse();
        final Subsystems subsystems = Subsystems.create(finder);

        final Map<ModuleIdentifier, String[]> filesForModule = new LinkedHashMap<>();
        final Map<ModuleIdentifier, SubsystemInfo[]> subsystemsForModule = new LinkedHashMap<>();

        //A simple one with few dependencies as a sanity check
        //getSubsystemsForModule(graph, subsystems, ModuleIdentifier.fromString("org.jberet.jberet-core:main"));

        for (ModuleIdentifier id : finder.findAllModules().keySet()) {
            if (id.toString().startsWith("javax.")) {
                continue;
            }
            //ModuleSpec does not give any way to get the resources, so just iterate over the files in the directory instead.
            //This will not work for modules using maven dependencies etc.
            filesForModule.put(id, getModuleDirResources(modulesDir, id));
            SubsystemInfo[] subsystemInfos = getSubsystemsForModule(graph, subsystems, id);
            if (subsystemInfos != null) {
                subsystemsForModule.put(id, getSubsystemsForModule(graph, subsystems, id));
            }
        }

        outputCsvs(filesForModule, subsystemsForModule);
    }

    private static String[] getModuleDirResources(String root, ModuleIdentifier id) {
        String[] parts = id.getName().split("\\.");
        Path path = Paths.get(root, parts).resolve(id.getSlot());
        File moduleDir = path.toFile();
        Set<String> files = new TreeSet<>();
        for (File file : moduleDir.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                files.add(file.getName());
            }
        }

        return files.toArray(new String[files.size()]);
    }

    private static SubsystemInfo[] getSubsystemsForModule(ModuleGraph graph, Subsystems subsystems, ModuleIdentifier id) {
        if (subsystems.getNames().contains(id)) {
            return new SubsystemInfo[]{new SubsystemInfo(id, Collections.singletonList(id))};
        } else {
            final Set<ModuleIdentifier> foundSubsystems = new TreeSet<>(MODULE_ID_COMPARATOR);
            final DependencyPathDFS findSubsystem = new DependencyPathDFS(graph, id, new UsedBySubsystem(subsystems, foundSubsystems));

            if (foundSubsystems.size() > 0) {
                final DependencyPathDFS pathToSubsystem = new DependencyPathDFS(graph, id, new PathToSubsystem(subsystems));
                SubsystemInfo[] subsystemInfos = new SubsystemInfo[foundSubsystems.size()];

                int i = 0;
                for (ModuleIdentifier subsystem : foundSubsystems) {
                    List path = pathToSubsystem.simplePathTo(subsystem);
                    subsystemInfos[i++] = new SubsystemInfo(subsystem, path);
                }
                return subsystemInfos;
            }
        }
        return null;
    }

    private static void outputCsvs(
            Map<ModuleIdentifier, String[]> filesForModule,
            Map<ModuleIdentifier, SubsystemInfo[]> subsystemsForModule) throws IOException {
        outputModulesCsv(filesForModule, subsystemsForModule);
        outputSubsystemPathsCsv(subsystemsForModule);
        System.out.println("Done");
    }

    private static void outputModulesCsv(
            Map<ModuleIdentifier, String[]> filesForModule,
            Map<ModuleIdentifier, SubsystemInfo[]> subsystemsForModule) throws IOException {
        final File outputFile = new File("module-dependency/target/modules.csv");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        System.out.println("Writing to " + outputFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            //Write the headers
            writer.write("\"Module\"");
            writer.write(",");
            writer.write("\"Jar/library\"");
            writer.write(",");
            writer.write("\"Subsystem(s)\"");
            writer.write("\n");

            for (Map.Entry<ModuleIdentifier, String[]> entry : filesForModule.entrySet()) {
                final ModuleIdentifier id = entry.getKey();
                final String[] files = entry.getValue();
                if (files.length == 0) {
                    continue;
                }


                boolean first = true;
                for (String file : files) {
                    final String subsystems;
                    if (first) {
                        subsystems = formatSubsystemsForCsv(subsystemsForModule.get(id));
                        first = false;
                    } else {
                        subsystems = "";
                    }

                    writer.write("\"");
                    writer.write(id.toString());
                    writer.write("\",\"");
                    writer.write(file);
                    writer.write("\",\"");
                    writer.write(subsystems);
                    writer.write("\"");
                    writer.write("\n");
                }
            }
        }
    }

    private static void outputSubsystemPathsCsv(Map<ModuleIdentifier, SubsystemInfo[]> subsystemsForModule) throws IOException {
        final File outputFile = new File("module-dependency/target/paths.csv");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        System.out.println("Writing to " + outputFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("\"Module\"");
            writer.write(",");
            writer.write("\"Subsystem Path\"");
            writer.write("\n");

            for (Map.Entry<ModuleIdentifier, SubsystemInfo[]> entry : subsystemsForModule.entrySet()) {
                final ModuleIdentifier id = entry.getKey();
                final SubsystemInfo[] subsystemInfos = entry.getValue();
                if (subsystemInfos.length == 0) {
                    continue;
                }

                for (SubsystemInfo subsystemInfo : subsystemInfos) {

                    writer.write("\"");
                    writer.write(id.toString());
                    writer.write("\",\"");
                    writer.write(subsystemInfo.pathsToSubsystem.toString());
                    writer.write("\"");
                    writer.write("\n");
                }
            }
        }
    }



    private static String formatSubsystemsForCsv(SubsystemInfo[] subsystemsForModule) {
        if (subsystemsForModule == null) {
            return " - ";
        }

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (SubsystemInfo subsystemInfo : subsystemsForModule) {
            String name = subsystemInfo.subsystemId.getName();
            name = name.substring(name.lastIndexOf(".") + 1);
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name);
        }
        return sb.toString();
    }

    private static abstract class SubsystemSearchCondition implements EndSearchCondition {
        private final Subsystems subsystems;

        public SubsystemSearchCondition(Subsystems subsystems) {
            this.subsystems = subsystems;
        }

        @Override
        public boolean endSearch(ModuleIdentifier id, ModuleDependency dep) {
            ModuleIdentifier useId = id(dep);
            if (IGNORE.contains(useId)) {
                //Don't pull in services reachable from the ignored modules
                return true;
            }

            if (subsystems.getNames().contains(useId)) {
                onSubsystem(useId);
                return true;
            }
            return false;
        }

        abstract ModuleIdentifier id(ModuleDependency dependency);

        abstract void onSubsystem(ModuleIdentifier id);
    }

    private static class UsedBySubsystem extends SubsystemSearchCondition {
        private final Set<ModuleIdentifier> foundSubsystems;

        public UsedBySubsystem(Subsystems subsystems, Set<ModuleIdentifier> foundSubsystems) {
            super(subsystems);
            this.foundSubsystems = foundSubsystems;
        }

        @Override
        ModuleIdentifier id(ModuleDependency dependency) {
            return dependency.getToId();
        }

        @Override
        void onSubsystem(ModuleIdentifier id) {
            foundSubsystems.add(id);
        }
    }

    private static class PathToSubsystem extends SubsystemSearchCondition {

        public PathToSubsystem(Subsystems subsystems) {
            super(subsystems);
        }

        @Override
        ModuleIdentifier id(ModuleDependency dependency) {
            return dependency.getFromId();
        }

        @Override
        void onSubsystem(ModuleIdentifier id) {

        }
    }

    private static class SubsystemInfo {
        private final ModuleIdentifier subsystemId;
        private final List<ModuleIdentifier> pathsToSubsystem;

        public SubsystemInfo(ModuleIdentifier subsystemId, List<ModuleIdentifier> pathsToSubsystem) {
            this.subsystemId = subsystemId;
            this.pathsToSubsystem = pathsToSubsystem;
        }
    }
}
