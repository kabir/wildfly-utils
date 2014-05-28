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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Phases {


    private Map<String, Phase> phases = new LinkedHashMap<>();
    private static Set<String> allNames = new HashSet<>();

    public Phases(Phase...phases){
        for (Phase phase : phases) {
            this.phases.put(phase.getPhaseName(), phase);
        }
    }


    public void addDup(String phaseName, DeploymentUnitProcessor dup, String...dependencies) {
        Phase phase = phases.get(phaseName);
        phase.addDup(dup, dependencies);
    }

    public Map<Phase, List<DeploymentUnitProcessor>> sort() {
        Map<Phase, List<DeploymentUnitProcessor>> sorted = new LinkedHashMap<Phase, List<DeploymentUnitProcessor>>();
        for (Phase phase : phases.values()) {
            sorted.put(phase, phase.sort());
        }
        return sorted;
    }
}
