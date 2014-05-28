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

package org.wildfly.util.deploymentprocessor.sorter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.wildfly.util.module.dependency.Util;

public class Phase {

    private final String phaseName;
    private final Map<String, DeploymentUnitProcessor> allObjects= new HashMap<>();
    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final Phase previous;

    Phase(String phaseName, Phase previous) {
        this.phaseName = phaseName;
        if (previous == null){
            previous = new Phase();
        }
        this.previous = previous;
    }

    private Phase(){
        this.phaseName = "<NULL>";
        this.previous = null;
    }

    String getPhaseName() {
        return phaseName;
    }

    Set<String> getAllDupNames() {
        return allObjects.keySet();
    }

    void addDup(DeploymentUnitProcessor dup, String...dependencies) {
        final String name = dup.getName();
        assert allObjects.containsValue(name);
        this.dependencies.put(name, new HashSet<>(Arrays.asList(dependencies)));
        allObjects.put(name, dup);
    }

    public List<DeploymentUnitProcessor> sort() {
        Sorter sorter = new Sorter();
        return sorter.sort();
    }

    private class Sorter {
        private Set<String> marked = new HashSet<>();
        private Set<String> onStack = new HashSet<>();
        private Stack<DeploymentUnitProcessor> reversePostOrder = new Stack<DeploymentUnitProcessor>();
        private List<DeploymentUnitProcessor> sort() {
            for (String name : allObjects.keySet()) {
                if (!marked.contains(name)) {
                    sort(name);
                }
            }
            return Util.stackToReverseList(reversePostOrder);
        }

        private void sort(String name) {
            onStack.add(name);
            marked.add(name);
            for (String dependency : dependencies.get(name)){
                if (!marked.contains(dependency)) {
                    sort(dependency);
                } else if (onStack.contains(dependency)) {
                    throw new IllegalStateException("Cycle"); //TODO explain what!
                }
            }
            onStack.remove(name);
            reversePostOrder.push(allObjects.get(name));
        }
    }

}
