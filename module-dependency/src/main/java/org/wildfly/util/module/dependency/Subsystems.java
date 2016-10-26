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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.modules.ModuleIdentifier;

public class Subsystems {

	private final Set<ModuleIdentifier> subsystemNames;

	private Subsystems(Set<ModuleIdentifier> subsystemNames){
		this.subsystemNames = subsystemNames;
	}

	public static Subsystems create() throws IOException, URISyntaxException {
		Set<ModuleIdentifier> subsystemNames = new TreeSet<>(Util.MODULE_ID_COMPARATOR);
		final URL url = Subsystems.class.getResource("subsystems.txt");
		final File file = new File(url.toURI());
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0 && line.charAt(0) != '#'){
					subsystemNames.add(ModuleIdentifier.create(line));
				}
				line = reader.readLine();
			}
		} finally {
			Util.safeClose(reader);
		}
		return new Subsystems(subsystemNames);
	}

	public Set<ModuleIdentifier> getNames() {
		return subsystemNames;
	}
}
