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
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.modules.ModuleIdentifier;

public class DependencyPathBFS {

	private final ModuleIdentifier root;
	private final Set<ModuleIdentifier> marked = new HashSet<>();
	private final Map<ModuleIdentifier, ModuleIdentifier> edgeTo = new HashMap<>();
	private final Set<ModuleDependency> edges = new HashSet<>();

	DependencyPathBFS(ModuleGraph graph, ModuleIdentifier root) {
		this.root = root;
		bfs(graph, root);
	}

	private void bfs(ModuleGraph graph, ModuleIdentifier id){
	    Queue<ModuleIdentifier> queue = new LinkedBlockingQueue<>();
		marked.add(id);
		queue.add(id);
		while (!queue.isEmpty()) {
		    ModuleIdentifier cur = queue.poll();
		    for (ModuleDependency dep : graph.getDependencies(cur)) {
		        ModuleIdentifier depId = dep.getToId();
		        if (!marked.contains(depId)) {
		            queue.add(depId);
		            marked.add(depId);
		            edgeTo.put(depId, cur);
		            edges.add(new ModuleDependency(cur, depId, dep.isOptional()));
		        }
		    }
		}
	}

	boolean hasPathTo(ModuleIdentifier id){
		return marked.contains(id);
	}

	Set<ModuleIdentifier> getAllModules(){
		return Collections.unmodifiableSet(marked);
	}

	List<ModuleIdentifier> pathTo(ModuleIdentifier id){
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
