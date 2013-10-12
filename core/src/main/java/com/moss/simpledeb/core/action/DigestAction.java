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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import com.moss.simpledeb.core.DebComponent;
import com.moss.simpledeb.core.DebState;
import com.moss.simpledeb.core.path.ArchivePath;
import com.moss.simpledeb.core.path.BytesArchivePath;


/*
 * This action draws out a weakness of the current design.
 * There isn't a way for this to work at the moment.
 * 
 * There would have to be multiple stages. Perhaps the DebState would contain
 * more than just a classpath list. It could contain state.content, which would
 * be a list of the paths to be added to the content tarball.
 * Path {
 *     ArchiveEntry entry;
 *     InputStream data(); // for files, not dirs
 * }
 * 
 * So, we'd have the action stage, and then the generation stage. The action stage
 * would create the Path entries for control and content and the generation stage
 * would package them up accordingly.
 */
public final class DigestAction extends DebAction {
	
	@Override
	public void run(DebState state) throws Exception {
		
		final StringBuilder sb = new StringBuilder();
		final MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		
		for (ArchivePath path : state.contentPaths) {
			
			if (path.entry().isDirectory()) {
				continue;
			}
			
			byte[] fileData;
			{
				InputStream in = path.read();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024 * 10]; //10k buffer
				for(int numRead = in.read(buffer); numRead!=-1; numRead = in.read(buffer)){
					out.write(buffer, 0, numRead);
				}
				in.close();
				out.close();
				fileData = out.toByteArray();
			}

			digest.update(fileData);
			byte[] hash = digest.digest();
			digest.reset();

			sb.append(HexUtil.toHex(hash).toLowerCase());
			sb.append("  ");
			sb.append(path.entry().getName());
			sb.append("\n");
		}
		
		byte[] data = sb.toString().getBytes();
		
		TarArchiveEntry tarEntry = new TarArchiveEntry("md5sum");
		tarEntry.setGroupId(0);
		tarEntry.setGroupName("root");
		tarEntry.setIds(0, 0);
		tarEntry.setModTime(System.currentTimeMillis());
		tarEntry.setSize(data.length);
		tarEntry.setUserId(0);
		tarEntry.setUserName("root");

		state.addPath(DebComponent.CONTROL, new BytesArchivePath(tarEntry, data));
	}
}
