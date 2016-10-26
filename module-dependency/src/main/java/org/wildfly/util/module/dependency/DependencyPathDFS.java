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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jboss.modules.ModuleIdentifier;

public class DependencyPathDFS {

	private final ModuleIdentifier root;
	private final Set<ModuleIdentifier> marked = new HashSet<>();
	private final Map<ModuleIdentifier, ModuleIdentifier> edgeTo = new HashMap<ModuleIdentifier, ModuleIdentifier>();
    private final Set<ModuleDependency> edges = new HashSet<>();//TODO get better paths with optional dependency information
    private final EndSearchCondition endSearchCondition;


    public DependencyPathDFS(ModuleGraph graph, ModuleIdentifier root) {
        this(graph, root, EndSearchCondition.NO_OP);
    }

	public DependencyPathDFS(ModuleGraph graph, ModuleIdentifier root, EndSearchCondition endSearchCondition) {
		this.root = root;
		this.endSearchCondition = endSearchCondition;
		dfs(graph, root);
	}

	private void dfs(ModuleGraph graph, ModuleIdentifier id){
		marked.add(id);
		Set<ModuleDependency> deps = graph.getDependencies(id);
		if (deps == null){
			return;
		}
		for (ModuleDependency dep : deps){
			ModuleIdentifier depId = dep.getToId();
			if (!marked.contains(depId)){
				edgeTo.put(depId, id);
				edges.add(new ModuleDependency(id, depId, dep.isOptional()));
				if (!endSearchCondition.endSearch(id, dep)) {
				    dfs(graph, depId);
				}
			}
		}
	}

	boolean isCoreDependency(ModuleIdentifier id){
		return marked.contains(id);
	}

	public Set<ModuleIdentifier> getAllModules(){
		return Collections.unmodifiableSet(marked);
	}

	public List<ModuleIdentifier> simplePathTo(ModuleIdentifier id){
		if (!marked.contains(id)){
			return Collections.emptyList();
		}

		Stack<ModuleIdentifier> ids = new Stack<>();
		ids.push(id);
		ModuleIdentifier parent = edgeTo.get(id);
		do {
			ids.push(parent);
			parent = edgeTo.get(parent);
		} while (parent != null);

		return Util.stackToReverseList(ids);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("CoreModules for " + root + ":{\n");
		for (ModuleIdentifier id : marked) {
			sb.append(" ");
			sb.append(id);
			sb.append("\n");
		}
		sb.append("}");
		return sb.toString();
	}


}
