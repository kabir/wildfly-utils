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
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jboss.modules.ModuleIdentifier;

public class AllDependencyPathsDFS {
	private final ModuleIdentifier search;
	private final Stack<ModuleIdentifier> stack = new Stack<>();
	private final List<List<ModuleIdentifier>> paths = new ArrayList<>();

	AllDependencyPathsDFS(ModuleGraph graph, ModuleIdentifier root, ModuleIdentifier search) {
		this.search = search;
		dfs(graph, root, false);
	}

	private void dfs(ModuleGraph graph, ModuleIdentifier id, boolean optional){
		stack.push(id);
		try {
			if (search.equals(id)){
				paths.add(new ArrayList<>(stack));

			} else {
				Set<ModuleDependency> deps = graph.getDependencies(id);
				for (ModuleDependency dep : deps){
					ModuleIdentifier depId = dep.getToId();
					if (!stack.contains(depId)){
						dfs(graph, depId, dep.isOptional());
					}
				}
			}
		} finally {
			stack.pop();
		}
	}

	List<List<ModuleIdentifier>> pathsTo(){
		return paths;
	}
}
