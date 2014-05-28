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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DeploymentUnitSorterTestCase {

    public static final String PHASE_1_NAME = "Phase1";
    public static final Phase PHASE_1 = new Phase(PHASE_1_NAME, null);


    @Test
    public void testOnePhaseDependencies() {
        Phases phases = new Phases(PHASE_1);

        DeploymentUnitProcessor one = new DeploymentUnitProcessor("one");
        DeploymentUnitProcessor two = new DeploymentUnitProcessor("two");
        DeploymentUnitProcessor three = new DeploymentUnitProcessor("three");
        DeploymentUnitProcessor four = new DeploymentUnitProcessor("four");

        phases.addDup(PHASE_1_NAME, four);
        phases.addDup(PHASE_1_NAME, three, "four");
        phases.addDup(PHASE_1_NAME, two, "three", "four");
        phases.addDup(PHASE_1_NAME, one, "two", "four");

        Map<Phase, List<DeploymentUnitProcessor>> sorted = phases.sort();

        List<DeploymentUnitProcessor> phase1Dups = new ArrayList<>();
        phase1Dups.add(one);
        phase1Dups.add(two);
        phase1Dups.add(three);
        phase1Dups.add(four);

        Assert.assertEquals(phase1Dups, sorted.get(PHASE_1));

    }

    @Test
    public void testOnePhaseDependenciesDifferentOrder() {
        Phases phases = new Phases(PHASE_1);

        DeploymentUnitProcessor one = new DeploymentUnitProcessor("one");
        DeploymentUnitProcessor two = new DeploymentUnitProcessor("two");
        DeploymentUnitProcessor three = new DeploymentUnitProcessor("three");
        DeploymentUnitProcessor four = new DeploymentUnitProcessor("four");

        phases.addDup(PHASE_1_NAME, one, "two", "four");
        phases.addDup(PHASE_1_NAME, two, "three", "four");
        phases.addDup(PHASE_1_NAME, three, "four");
        phases.addDup(PHASE_1_NAME, four);

        Map<Phase, List<DeploymentUnitProcessor>> sorted = phases.sort();

        List<DeploymentUnitProcessor> phase1Dups = new ArrayList<>();
        phase1Dups.add(one);
        phase1Dups.add(two);
        phase1Dups.add(three);
        phase1Dups.add(four);

        Assert.assertEquals(phase1Dups, sorted.get(PHASE_1));
    }

    @Test(expected=IllegalStateException.class)
    public void testOnePhaseDetectCycle() {
        Phases phases = new Phases(PHASE_1);

        DeploymentUnitProcessor one = new DeploymentUnitProcessor("one");
        DeploymentUnitProcessor two = new DeploymentUnitProcessor("two");
        DeploymentUnitProcessor three = new DeploymentUnitProcessor("three");
        DeploymentUnitProcessor four = new DeploymentUnitProcessor("four");

        phases.addDup(PHASE_1_NAME, one, "two");
        phases.addDup(PHASE_1_NAME, two, "three");
        phases.addDup(PHASE_1_NAME, three, "four");
        phases.addDup(PHASE_1_NAME, four, "one");

        phases.sort();
    }
}
