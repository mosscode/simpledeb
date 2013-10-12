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

import java.io.File;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import com.moss.simpledeb.core.DebComponent;
import com.moss.simpledeb.core.DebState;
import com.moss.simpledeb.core.path.ArchivePath;
import com.moss.simpledeb.core.path.BytesArchivePath;
import com.moss.simpledeb.core.path.DirArchivePath;

@XmlAccessorType(XmlAccessType.FIELD)
public final class LaunchScriptAction extends DebAction {
	
	@XmlAttribute(name="class-name")
	private String className;
	
	@XmlAttribute(name="target-file")
	private String targetFile;
	
	@XmlAttribute(name="path-level")
	private int pathLevel;
	
	@Override
	public void run(DebState state) throws Exception {
		
		{
			File target = new File(targetFile).getParentFile();
			LinkedList<File> pathsNeeded = new LinkedList<File>();

			File f = target;
			while (f != null) {
				pathsNeeded.addFirst(f);
				f = f.getParentFile();
			}

			for (int i=0; i<pathLevel; i++) {
				pathsNeeded.removeFirst();
			}

			for (File e : pathsNeeded) {
				String p = "./" + e.getPath();

				if (!p.endsWith("/")) {
					p = p + "/";
				}

				TarArchiveEntry tarEntry = new TarArchiveEntry(p);
				tarEntry.setGroupId(0);
				tarEntry.setGroupName("root");
				tarEntry.setIds(0, 0);
				tarEntry.setModTime(System.currentTimeMillis());
				tarEntry.setSize(0);
				tarEntry.setUserId(0);
				tarEntry.setUserName("root");
				tarEntry.setMode(Integer.parseInt("755", 8));
				
				ArchivePath path = new DirArchivePath(tarEntry);
				state.addPath(DebComponent.CONTENT, path);
			}
		}
		
		String cp;
		{
			StringBuffer sb = new StringBuffer();
			for (String path : state.classpath) {
				if (sb.length() == 0) {
					sb.append(path);
				}
				else {
					sb.append(":");
					sb.append(path);
				}
			}
			cp = sb.toString();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/bash\n");
		sb.append("CP=\"");
		sb.append(cp);
		sb.append("\"\n");
		sb.append("/usr/bin/java -cp $CP ");
		sb.append(className);
		sb.append(" $@\n");
		
		byte[] data = sb.toString().getBytes();
		
		String entryName = "./" + targetFile;
		
		TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
		tarEntry.setGroupId(0);
		tarEntry.setGroupName("root");
		tarEntry.setIds(0, 0);
		tarEntry.setModTime(System.currentTimeMillis());
		tarEntry.setSize(data.length);
		tarEntry.setUserId(0);
		tarEntry.setUserName("root");
		tarEntry.setMode(Integer.parseInt("755", 8));
		
		ArchivePath path = new BytesArchivePath(tarEntry, data);
		state.addPath(DebComponent.CONTENT, path);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public int getPathLevel() {
		return pathLevel;
	}

	public void setPathLevel(int assumedTargetPathLevel) {
		this.pathLevel = assumedTargetPathLevel;
	}
}
