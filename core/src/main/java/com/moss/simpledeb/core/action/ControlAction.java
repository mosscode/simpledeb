/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of simpledeb.
 *
 * simpledeb is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * simpledeb is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with simpledeb; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.simpledeb.core.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import com.moss.simpledeb.core.DebComponent;
import com.moss.simpledeb.core.DebState;
import com.moss.simpledeb.core.path.BytesArchivePath;

@XmlAccessorType(XmlAccessType.FIELD)
public final class ControlAction extends DebAction {
	
	@XmlAttribute(name="package")
    private String packageName;

	@XmlAttribute
    private String version; 

	@XmlAttribute
    private String architecture;

	@XmlAttribute
    private String depends; // optional

	@XmlAttribute
    private String maintainer; 

	@XmlAttribute
    private String description;
	
	@Override
	public void run(DebState state) throws Exception {
		
		require("Package", packageName);
		require("Version", version);
		require("Architecture", architecture);
		require("Depends", depends);
		require("Maintainer", maintainer);
		require("Description", description);
		
		byte[] controlFileData;
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Package: ")		.append(packageName)	.append("\n");
			sb.append("Version: ")		.append(version)		.append("\n");
			sb.append("Architecture: ")	.append(architecture)	.append("\n");
			sb.append("Depends: ")		.append(depends)		.append("\n");
			sb.append("Maintainer: ")	.append(maintainer)		.append("\n");
			sb.append("Description: ")	.append(description)	.append("\n");
			
			controlFileData = sb.toString().getBytes();
		}
		
		TarArchiveEntry tarEntry = new TarArchiveEntry("control");
		tarEntry.setGroupId(0);
		tarEntry.setGroupName("root");
		tarEntry.setIds(0, 0);
		tarEntry.setModTime(System.currentTimeMillis());
		tarEntry.setSize(controlFileData.length);
		tarEntry.setUserId(0);
		tarEntry.setUserName("root");

		state.addPath(DebComponent.CONTROL, new BytesArchivePath(tarEntry, controlFileData));
	}
	
	private static final void require(String name, String value) {
		if (value == null || value.trim().length() == 0) {
			throw new RuntimeException("Control parameter " + name + " is required.");
		}
	}
	
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getArchitecture() {
		return architecture;
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public String getDepends() {
		return depends;
	}

	public void setDepends(String depends) {
		this.depends = depends;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
