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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modules.ModuleIdentifier;
import org.wildfly.util.module.dependency.ModuleGraph;
import org.wildfly.util.module.dependency.ModuleSpecFinder;
import org.wildfly.util.module.dependency.Subsystems;

/**
 * @author Kabir Khan
 */
public class AllModulesAndJarsForJdk9Util {
    public static void main(String[] args) throws Exception {
        final String modulesDir = System.getProperty("jboss.as.modules.dir", "/Users/kabir/sourcecontrol/wildfly/git/wildfly/dist/target/wildfly-11.0.0.Alpha1-SNAPSHOT/modules/system/layers/base/");
        final File root = new File(modulesDir);

        final ModuleSpecFinder finder = ModuleSpecFinder.Factory.create(root);
        final ModuleGraph graph = new ModuleGraph(finder).reverse();
        final Subsystems subsystems = Subsystems.create(finder);

        final Map<ModuleIdentifier, String[]> filesForModule = new LinkedHashMap<>();

        for (ModuleIdentifier id : finder.findAllModules().keySet()) {
            //ModuleSpec does not give any way to get the resources, so just iterate over the files in the directory instead.
            //This will not work for modules using maven dependencies etc.
            filesForModule.put(id, getModuleDirResources(modulesDir, id));
        }

        outputCsv(filesForModule);
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

    private static void outputCsv(
                    Map<ModuleIdentifier, String[]> filesForModule) throws IOException {
        final File outputFile = new File("module-dependency/target/output.csv");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        System.out.println("Writing to " + outputFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            //Write the headers
            writer.write("\"Owner\"");
            writer.write(",");
            writer.write("\"Module\"");
            writer.write(",");
            writer.write("\"Jar/library\"");
            writer.write("\n");

            for (Map.Entry<ModuleIdentifier, String[]> entry : filesForModule.entrySet()) {
                final ModuleIdentifier id = entry.getKey();
                final String[] files = entry.getValue();
                if (files.length == 0) {
                    continue;
                }


                boolean first = true;
                for (String file : files) {
                    writer.write("\"\",");
                    writer.write("\"");
                    writer.write(id.toString());
                    writer.write("\",\"");
                    writer.write(file);
                    writer.write("\"");
                    writer.write("\n");
                }
            }
        }
        System.out.println("Done");
    }

    private static String formatSubsystems(ModuleIdentifier[] subsystemsForModule) {
        if (subsystemsForModule == null) {
            return " - ";
        }

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (ModuleIdentifier id : subsystemsForModule) {
            String name = id.getName();
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
}
