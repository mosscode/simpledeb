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
package com.moss.simpledeb.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.moss.simpledeb.core.action.ControlAction;
import com.moss.simpledeb.core.action.CopyAction;
import com.moss.simpledeb.core.action.DebAction;
import com.moss.simpledeb.core.action.LaunchScriptAction;
import com.moss.simpledeb.core.fs.StaticFileSet;
import com.moss.simpledeb.core.path.ArchivePath;

public final class DebWriter {
	
	private final DebPackage pkg;
	
	public DebWriter(DebPackage pkg) {
		if (pkg == null) {
			throw new NullPointerException();
		}
		this.pkg = pkg;
	}
	
	public void write(OutputStream out) throws Exception {
		
		if (out == null) {
			throw new NullPointerException();
		}
		
		DebState state = new DebState();
		
		for (DebAction action : pkg.actions()) {
			action.run(state);
		}
		
		ArArchiveOutputStream ar = new ArArchiveOutputStream(out);
		
		/*
		 * Version
		 */
		
		byte[] versionData = "2.0\n".getBytes();
		
		ArArchiveEntry versionEntry = new ArArchiveEntry(
			"debian-binary", 
			versionData.length, 
			0, 
			0, 
			0664, 
			System.currentTimeMillis()
		);
		
		ar.putArchiveEntry(versionEntry);
		ar.write(versionData);
		ar.closeArchiveEntry();
		
		/*
		 * Control
		 */
		
		byte[] controlFileData = buildGzipTar(state.controlPaths);
		
		ArArchiveEntry controlEntry = new ArArchiveEntry(
			"control.tar.gz", 
			controlFileData.length, 
			0, 
			0, 
			0664, 
			System.currentTimeMillis()
		);
		
		ar.putArchiveEntry(controlEntry);
		ar.write(controlFileData);
		ar.closeArchiveEntry();
		
		/*
		 * Data
		 */
		
		byte[] contentData = buildGzipTar(state.contentPaths);
		
		ArArchiveEntry contentEntry = new ArArchiveEntry(
			"data.tar.gz", 
			contentData.length, 
			0, 
			0, 
			0664, 
			System.currentTimeMillis()
		);

		ar.putArchiveEntry(contentEntry);
		ar.write(contentData);
		ar.closeArchiveEntry();
		
		ar.close();
	}
	
	private byte[] buildGzipTar(List<ArchivePath> paths) throws Exception {
		
		byte[] tarData;
		{
			ByteArrayOutputStream tarOut = new ByteArrayOutputStream();
			TarArchiveOutputStream tar = new TarArchiveOutputStream(tarOut);
			
			Set<String> writtenPaths = new HashSet<String>();
			for (ArchivePath path : paths) {
				String name = path.entry().getName();
				
				if (writtenPaths.contains(name)) {
					throw new RuntimeException("Duplicate archive entry: " + name);
				}
				
				writtenPaths.add(name);
				
				tar.putArchiveEntry(path.entry());
				
				if (!path.entry().isDirectory()) {
					InputStream in = path.read();
					byte[] buffer = new byte[1024 * 10];
					for(int numRead = in.read(buffer); numRead!=-1; numRead = in.read(buffer)){
						tar.write(buffer, 0, numRead);
					}
					in.close();
				}
				
				tar.closeArchiveEntry();
			}

			tar.close();
			tarData = tarOut.toByteArray();
		}
		
		byte[] gzipData;
		{
			ByteArrayOutputStream gzipOut = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(gzipOut);
			gzip.write(tarData);
			gzip.close();
			
			gzipData = gzipOut.toByteArray();
		}
		
		return gzipData;
	}
	
	public static void main(String[] args) throws Exception {
		
		ControlAction control = new ControlAction();
		control.setPackageName("foo");
		control.setVersion("0.0.1");
		control.setArchitecture("all");
		control.setDepends("sun-java6-jre");
		control.setMaintainer("Mr. Bob <mr@bob.com>");
		control.setDescription("A great time.");
		
		StaticFileSet cpFiles = new StaticFileSet();
		cpFiles.add(new File("/home/bob/.m2/repository/com/bob/bob-app/0.34.0-SNAPSHOT/bob-app-0.34.0-SNAPSHOT.jar"));
		
		CopyAction cp = new CopyAction();
		cp.setFiles(cpFiles);
		cp.setDirMode("755");
		cp.setFileMode("644");
		cp.setTargetDir("usr/local/lib/foo");
		cp.setAssumedTargetPathLevel(3);
		cp.setAppendToClasspath(true);
		
		StaticFileSet files = new StaticFileSet();
		files.add(new File("fake/two"));
		
		CopyAction copy = new CopyAction();
		copy.setDirMode("755");
		copy.setFileMode("644");
		copy.setFiles(files);
		copy.setTargetDir("usr/local/shared/foo");
		copy.setAssumedTargetPathLevel(3);
		
		LaunchScriptAction launch = new LaunchScriptAction();
		launch.setClassName("");
		launch.setTargetFile("usr/local/bin/foo.sh");
		launch.setPathLevel(2);
		
		DebPackage pkg = new DebPackage()
		.add(control)
		.add(cp)
		.add(copy)
		.add(launch);
		
		DebWriter w = new DebWriter(pkg);
		w.write(new FileOutputStream("target/foo.deb"));
	}
}
