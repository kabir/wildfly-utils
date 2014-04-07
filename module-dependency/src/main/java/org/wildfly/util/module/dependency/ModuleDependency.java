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

import org.jboss.modules.ModuleIdentifier;

public class ModuleDependency {
    private final ModuleIdentifier from;
	private final ModuleIdentifier to;
	private final boolean optional;

	ModuleDependency(ModuleIdentifier from, ModuleIdentifier id, boolean optional) {
	    this.from = from;
		this.to = id;
		this.optional = optional;
	}


    public ModuleIdentifier getFromId() {
        return from;
    }

	public ModuleIdentifier getToId() {
		return to;
	}

	public boolean isOptional() {
		return optional;
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + to.hashCode();
		hash = 31 * hash + Boolean.valueOf(optional).hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null){
			return false;
		}
		if (obj instanceof ModuleDependency == false) {
			return false;
		}
		ModuleDependency other = (ModuleDependency)obj;
		//Do NOT compare the value of optional
		return to.equals(other.to) && from.equals(other.from);
	}

	@Override
	public String toString() {
		if (optional){
			return from + "->" + to + " (optional)";
		}
		return from + "->" + to;
	}
}