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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;
import org.junit.Assert;
import org.junit.Test;

public class ModuleGraphTestCase {

    @Test
    public void testModuleDependencyGraph() {
        ModuleGraph graph = new ModuleGraph();
        addDependency(graph, "a", "b");
        checkDeps(graph, "a", dep("a", "b"));
        checkDeps(graph, "b");

        addDependency(graph, "a", "c");
        checkDeps(graph, "a", dep("a", "b"), dep("a", "c"));
        checkDeps(graph, "b");
        checkDeps(graph, "c");
    }

    @Test
    public void testModuleDependencyGraphReverse() {
        ModuleGraph graph = new ModuleGraph();
        addDependency(graph, "a", "b");
        ModuleGraph reverse = graph.reverse();
        checkDeps(reverse, "b", dep("b", "a"));
        checkDeps(reverse, "a");

        addDependency(graph, "a", "c");
        reverse = graph.reverse();
        checkDeps(reverse, "b", dep("b", "a"));
        checkDeps(reverse, "c", dep("c", "a"));
        checkDeps(reverse, "a");

        addDependency(graph, "d", "b");
        reverse = graph.reverse();
        checkDeps(reverse, "b", dep("b", "a"), dep("b", "d"));
        checkDeps(reverse, "c", dep("c", "a"));
        checkDeps(reverse, "a");
        checkDeps(reverse, "d");
    }

    @Test
    public void testDependencyPathDFS() {
        ModuleGraph graph = new ModuleGraph();
        addDependency(graph, "a", "b");
        addDependency(graph, "a", "c");
        addDependency(graph, "c", "d");
        addDependency(graph, "no1", "no2");

        DependencyPathDFS search = new DependencyPathDFS(graph, id("a"));
        Set<ModuleIdentifier> moduleIds = search.getAllModules();
        Assert.assertEquals(createIdSet("a", "b", "c", "d"), moduleIds);

        List<ModuleIdentifier> path = search.simplePathTo(id("d"));
        Assert.assertEquals(createIdList("a", "c", "d"), path);

        path = search.simplePathTo(id("c"));
        Assert.assertEquals(createIdList("a", "c"), path);

        path = search.simplePathTo(id("b"));
        Assert.assertEquals(createIdList("a", "b"), path);

        path = search.simplePathTo(id("no1"));
        Assert.assertTrue(path.isEmpty());

        path = search.simplePathTo(id("no2"));
        Assert.assertTrue(path.isEmpty());
    }

    @Test
    public void testDependencyPathBFS() {
        ModuleGraph graph = new ModuleGraph();
        addDependency(graph, "a", "b");
        addDependency(graph, "b", "c");
        addDependency(graph, "c", "d");
        addDependency(graph, "d", "e");
        addDependency(graph, "e", "z");

        addDependency(graph, "a", "f");
        addDependency(graph, "f", "z");

        addDependency(graph, "a", "g");
        addDependency(graph, "g", "h");
        addDependency(graph, "h", "i");
        addDependency(graph, "i", "j");
        addDependency(graph, "j", "k");
        addDependency(graph, "k", "z");

        DependencyPathBFS search = new DependencyPathBFS(graph, id("a"));
        Set<ModuleIdentifier> moduleIds = search.getAllModules();
        Assert.assertEquals(createIdSet("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "z"), moduleIds);

        List<ModuleIdentifier> path = search.pathTo(id("z"));
        Assert.assertEquals(createIdList("a", "f", "z"), path);

        path = search.pathTo(id("c"));
        Assert.assertEquals(createIdList("a", "b", "c"), path);

        path = search.pathTo(id("b"));
        Assert.assertEquals(createIdList("a", "b"), path);

        path = search.pathTo(id("no1"));
        Assert.assertTrue(path.isEmpty());

        path = search.pathTo(id("no2"));
        Assert.assertTrue(path.isEmpty());
    }

    @Test
    public void testAllDependencyPathsSearch() {
        ModuleGraph graph = new ModuleGraph();

        /*
         *
         *   ->b->c<------------
         *  /         ->g->     |
         * |         /     \    ^
         * a->d->e->f       z   |
         * | |      |\     /|   |
         * | -<-----  ->h-> ^   |
         * |                |   |
         *  ->-----i---->---^   |
         * |                |   |
         *  ->-j----->k---->    |
         * |                    |
         *  ---------l--------->
        */

        addDependency(graph, "a", "b");
        addDependency(graph, "b", "c");

        addDependency(graph, "a", "d");
        addDependency(graph, "d", "e");
        addDependency(graph, "e", "f");
        addDependency(graph, "f", "g");
        addDependency(graph, "g", "z");
        addDependency(graph, "f", "h");
        addDependency(graph, "h", "z");
        addDependency(graph, "f", "d");//loop

        addDependency(graph, "a", "i");
        addDependency(graph, "i", "z");

        addDependency(graph, "a", "j");
        addDependency(graph, "j", "k");
        addDependency(graph, "k", "z");

        addDependency(graph, "a", "l");
        addDependency(graph, "l", "c");

        AllDependencyPathsDFS search = new AllDependencyPathsDFS(graph, id("a"), id("z"));
        List<List<ModuleIdentifier>> paths = search.pathsTo();

        List<List<ModuleIdentifier>> expected = new ArrayList<>();
        expected.add(createIdList("a", "d", "e", "f", "g", "z"));
        expected.add(createIdList("a", "d", "e", "f", "h", "z"));
        expected.add(createIdList("a", "i", "z"));
        expected.add(createIdList("a", "j", "k", "z"));

        Assert.assertEquals(expected.size(), paths.size());
        Assert.assertTrue(expected.containsAll(paths));
    }

    private void checkDeps(ModuleGraph graph, String from, ModuleDependency...expectedDeps){
        Set<ModuleDependency> deps = graph.getDependencies(id(from));
        Assert.assertEquals(deps, createDepSet(expectedDeps));
    }

    private Set<ModuleIdentifier> createIdSet(String...ids){
        Set<ModuleIdentifier> idSet = new HashSet<>();
        for (String id : ids) {
            idSet.add(id(id));
        }
        return idSet;
    }

    private List<ModuleIdentifier> createIdList(String...ids){
        List<ModuleIdentifier> list = new ArrayList<>();
        for (String id : ids) {
            list.add(id(id));
        }
        return list;
    }

    private Set<ModuleDependency> createDepSet(ModuleDependency...deps) {
        if (deps.length == 0) {
            return Collections.emptySet();
        }
        Set<ModuleDependency> depSet = new HashSet<>();
        for (ModuleDependency dep : deps) {
            depSet.add(dep);
        }
        return depSet;
    }

    private void addDependency(ModuleGraph graph, String from, String to) {
        graph.addDependency(id(from), dep(from, to));
    }

    private ModuleIdentifier id(String s){
        return ModuleIdentifier.create(s);
    }

    private ModuleDependency dep(String from, String to){
        return new ModuleDependency(id(from), id(to), false);
    }
}
