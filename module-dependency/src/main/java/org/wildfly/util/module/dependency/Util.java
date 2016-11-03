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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.jboss.modules.ModuleIdentifier;

public class Util {

	public static Comparator<ModuleIdentifier> MODULE_ID_COMPARATOR = new Comparator<ModuleIdentifier>() {
		@Override
		public int compare(ModuleIdentifier o1, ModuleIdentifier o2) {
			int compare = o1.getName().compareToIgnoreCase(o2.getName());
			if (compare != 0) {
				return compare;
			}
			return o1.getSlot().compareToIgnoreCase(o2.getSlot());
		}
	};


	static void safeClose(Closeable c) {
		try {
			c.close();
		} catch (Exception e){
		}
	}

	public static <T> List<T> stackToReverseList(Stack<T> stack){
		if (stack.size() == 0) {
			return Collections.emptyList();
		}
		List<T> list = new ArrayList<>();
		for (ListIterator<T> it = stack.listIterator(stack.size()) ; it.hasPrevious() ; ) {
			list.add(it.previous());
		}

		return list;
	}
}
