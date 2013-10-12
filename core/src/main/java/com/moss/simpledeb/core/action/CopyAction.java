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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import com.moss.simpledeb.core.DebComponent;
import com.moss.simpledeb.core.DebState;
import com.moss.simpledeb.core.fs.FileSet;
import com.moss.simpledeb.core.fs.FileVisitor;
import com.moss.simpledeb.core.fs.StaticFileSet;
import com.moss.simpledeb.core.path.ArchivePath;
import com.moss.simpledeb.core.path.DirArchivePath;
import com.moss.simpledeb.core.path.FileArchivePath;

@XmlAccessorType(XmlAccessType.FIELD)
public final class CopyAction extends DebAction {
	
	@XmlElements({
		@XmlElement(name="list", type=StaticFileSet.class)
	})
	private FileSet files;

	@XmlAttribute
	private String targetDir;
	
	@XmlAttribute
	private int assumedTargetPathLevel;
	
	@XmlAttribute(name="file-mode")
	private String fileMode;
	
	@XmlAttribute(name="dir-mode")
	private String dirMode;
	
	@XmlAttribute
	private boolean appendToClasspath;
	
	@XmlAttribute
	private DebComponent component;
	
	@Override
	public void run(final DebState state) throws Exception {
		
		File target = new File(targetDir);
		LinkedList<File> pathsNeeded = new LinkedList<File>();
		
		File f = target;
		while (f != null) {
			pathsNeeded.addFirst(f);
			f = f.getParentFile();
		}
		
		for (int i=0; i<assumedTargetPathLevel; i++) {
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
			state.addPath(component, path);
		}
		
		files.visit(new FileVisitor() {
			public void file(File file) {
				try {
					copyFile(file, state);
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}
	
	private void copyFile(File file, DebState state) throws Exception {
		
		String entryName = "./" + targetDir + "/" + file.getName();
		
		if (!file.isDirectory()) {
			
			TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
			tarEntry.setGroupId(0);
			tarEntry.setGroupName("root");
			tarEntry.setIds(0, 0);
			tarEntry.setModTime(System.currentTimeMillis());
			tarEntry.setSize(file.length());
			tarEntry.setUserId(0);
			tarEntry.setUserName("root");
			tarEntry.setMode(Integer.parseInt(fileMode, 8));
			
			ArchivePath path = new FileArchivePath(tarEntry, file);
			state.addPath(component, path);

			if (appendToClasspath) {
				state.classpath.add("/" + targetDir + "/" + file.getName());
			}
		}
		else {
			entryName = entryName + "/";
			
			TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
			tarEntry.setGroupId(0);
			tarEntry.setGroupName("root");
			tarEntry.setIds(0, 0);
			tarEntry.setModTime(System.currentTimeMillis());
			tarEntry.setSize(0);
			tarEntry.setUserId(0);
			tarEntry.setUserName("root");
			tarEntry.setMode(Integer.parseInt(dirMode, 8));
			
			ArchivePath path = new DirArchivePath(tarEntry);
			state.addPath(component, path);
		}
	}
	
	public String getTargetDir() {
		return targetDir;
	}

	public void setTargetDir(String targetDir) {
		this.targetDir = targetDir;
	}

	public String getFileMode() {
		return fileMode;
	}

	public void setFileMode(String fileMode) {
		this.fileMode = fileMode;
	}

	public String getDirMode() {
		return dirMode;
	}

	public void setDirMode(String dirMode) {
		this.dirMode = dirMode;
	}

	public FileSet getFiles() {
		return files;
	}

	public void setFiles(FileSet files) {
		this.files = files;
	}

	public int getAssumedTargetPathLevel() {
		return assumedTargetPathLevel;
	}

	public void setAssumedTargetPathLevel(int assumedTargetPathLevel) {
		this.assumedTargetPathLevel = assumedTargetPathLevel;
	}

	public boolean isAppendToClasspath() {
		return appendToClasspath;
	}

	public void setAppendToClasspath(boolean appendToClasspath) {
		this.appendToClasspath = appendToClasspath;
	}

	public DebComponent getComponent() {
		return component;
	}

	public void setComponent(DebComponent component) {
		this.component = component;
	}
}
