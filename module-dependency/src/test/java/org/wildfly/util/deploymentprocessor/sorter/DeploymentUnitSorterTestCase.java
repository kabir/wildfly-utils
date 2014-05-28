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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DeploymentUnitSorterTestCase {

    public static final String PHASE_1_NAME = "Phase1";
    public static final String PHASE_2_NAME = "Phase2";
    public static final String PHASE_3_NAME = "Phase3";


    @Test
    public void testOnePhaseDependencies() {
        final Phase PHASE_1 = new Phase(PHASE_1_NAME, null);
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

        Assert.assertEquals(list(one, two, three, four), sorted.get(PHASE_1));
    }

    @Test
    public void testOnePhaseDependenciesDifferentOrder() {
        final Phase PHASE_1 = new Phase(PHASE_1_NAME, null);
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

        Assert.assertEquals(list(one, two, three, four), sorted.get(PHASE_1));
    }

    @Test(expected=IllegalStateException.class)
    public void testOnePhaseDetectCycle() {
        final Phase PHASE_1 = new Phase(PHASE_1_NAME, null);
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

    @Test
    public void testSeveralPhasesDependencies() {
        final Phase PHASE_1 = new Phase(PHASE_1_NAME, null);
        final Phase PHASE_2 = new Phase(PHASE_2_NAME, PHASE_1);
        final Phase PHASE_3 = new Phase(PHASE_3_NAME, PHASE_2);
        Phases phases = new Phases(PHASE_1, PHASE_2, PHASE_3);

        DeploymentUnitProcessor one_one = new DeploymentUnitProcessor("one-one");
        DeploymentUnitProcessor one_two = new DeploymentUnitProcessor("one-two");
        DeploymentUnitProcessor one_three = new DeploymentUnitProcessor("one-three");
        DeploymentUnitProcessor one_four = new DeploymentUnitProcessor("one-four");
        phases.addDup(PHASE_1_NAME, one_one, "one-two");
        phases.addDup(PHASE_1_NAME, one_two, "one-three", "one-four");
        phases.addDup(PHASE_1_NAME, one_three, "one-four");
        phases.addDup(PHASE_1_NAME, one_four);

        DeploymentUnitProcessor two_one = new DeploymentUnitProcessor("two-one");
        DeploymentUnitProcessor two_two = new DeploymentUnitProcessor("two-two");
        DeploymentUnitProcessor two_three = new DeploymentUnitProcessor("two-three");
        DeploymentUnitProcessor two_four = new DeploymentUnitProcessor("two-four");
        phases.addDup(PHASE_2_NAME, two_one, "two-two", "one-two");
        phases.addDup(PHASE_2_NAME, two_two, "two-three", "two-four", "one-three", "one-four");
        phases.addDup(PHASE_2_NAME, two_three, "two-four", "one-four");
        phases.addDup(PHASE_2_NAME, two_four);

        DeploymentUnitProcessor three_one = new DeploymentUnitProcessor("three-one");
        DeploymentUnitProcessor three_two = new DeploymentUnitProcessor("three-two");
        DeploymentUnitProcessor three_three = new DeploymentUnitProcessor("three-three");
        DeploymentUnitProcessor three_four = new DeploymentUnitProcessor("three-four");
        phases.addDup(PHASE_3_NAME, three_one, "three-two", "two-two", "one-two");
        phases.addDup(PHASE_3_NAME, three_two, "three-three", "three-four", "two-three", "two-four", "one-three", "one-four");
        phases.addDup(PHASE_3_NAME, three_three, "three-four", "two-four", "one-four");
        phases.addDup(PHASE_3_NAME, three_four);

        Map<Phase, List<DeploymentUnitProcessor>>  sorted = phases.sort();

        Assert.assertEquals(list(one_one, one_two, one_three, one_four), sorted.get(PHASE_1));
        Assert.assertEquals(list(two_one, two_two, two_three, two_four), sorted.get(PHASE_2));
        Assert.assertEquals(list(three_one, three_two, three_three, three_four), sorted.get(PHASE_3));
    }


    @Test
    public void testStress() {
        System.out.println("=== Warmup ");
        for (int i = 0 ; i < 10 ; i++) {
            executeStressTest(50, 50, 50, 50);
        }

        executeStressTest(50, 500, 50, 50);

    }

    private void executeStressTest(final int numPhases, final int dupsPerPhase, final int otherPhaseDeps, final int currentPhaseDeps){
        System.out.println("--phases=" + numPhases + "; dups/phase=" + dupsPerPhase + "; otherPhaseDeps=" + otherPhaseDeps + "; deps/current=" + currentPhaseDeps);
        long start = System.currentTimeMillis();

        List<Phase> phasesList = new ArrayList<>();
        for (int p = 0 ; p < numPhases ; p++) {
            Phase previous = p == 0 ? null : phasesList.get(p - 1);
            Phase phase = new Phase("Phase" + p, previous);
            phasesList.add(phase);


            for (int d = 0 ; d < dupsPerPhase ; d++) {
                DeploymentUnitProcessor processor = new DeploymentUnitProcessor(getName(p, d));

                List<String> deps = new ArrayList<>();
                //current deps
                int max = d < currentPhaseDeps ? 0 : d - currentPhaseDeps;
                for (int i = d - 1 ; i >= max ; i--) {
                    deps.add(getName(p, i));
                }
                //other phase deps
                int total = 0;
                for (int i = 0 ; i < p && total <= otherPhaseDeps ; i++, total++) {
                    for (int j = 0 ; j < dupsPerPhase && total <= otherPhaseDeps ; j++, total++) {
                        deps.add(getName(i, j));
                    }
                }

                phase.addDup(processor, deps.toArray(new String[deps.size()]));
            }
        }


        Phases phases = new Phases(phasesList.toArray(new Phase[phasesList.size()]));

        long presort = System.currentTimeMillis();
        Map<Phase, List<DeploymentUnitProcessor>>  sorted = phases.sort();
        long end = System.currentTimeMillis();

        System.out.println("\tSetup took: " + String.valueOf(presort - start) + "ms");
        System.out.println("\tSorting took: " + String.valueOf(end - presort) + "ms");
    }


    private String getName(int phase, int dup){
        return phase + "-" + dup;
    }

    private List<DeploymentUnitProcessor> list(DeploymentUnitProcessor...processors) {
        return Arrays.asList(processors);
    }

}
