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
package com.moss.simpledeb.mojo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import com.moss.simpledeb.core.DebComponent;
import com.moss.simpledeb.core.DebPackage;
import com.moss.simpledeb.core.DebWriter;
import com.moss.simpledeb.core.action.ControlAction;
import com.moss.simpledeb.core.action.CopyAction;
import com.moss.simpledeb.core.action.DebAction;
import com.moss.simpledeb.core.action.DummyAutoClasspathAction;
import com.moss.simpledeb.core.fs.StaticFileSet;

/**
 * @goal deb
 */
public class DebWriterMojo extends AbstractMojo {
	
	/** @component */
	private ArtifactFactory artifactFactory;
	
	/** @component */
	private ArtifactResolver resolver;
	
	/** @component */
	private ArtifactMetadataSource artifactMetadataSource;
	
	/** @parameter expression="${project}" */
	private MavenProject project;
	
	/** @component */
	private MavenProjectHelper projectHelper;

	/**@parameter expression="${localRepository}" */
	private ArtifactRepository localRepository;
	
	/** @parameter expression="${project.runtimeClasspathElements}" */
	private List<String> classpathElements;

	/** @parameter expression="${project.remoteArtifactRepositories}" */
	private List<ArtifactRepository> remoteRepositories;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			
			File configDir = new File(project.getBasedir(), "src/main/deb");
			if (!configDir.exists() && !configDir.mkdirs()) {
				throw new MojoExecutionException("Cannot create dir: " + configDir);
			}

			JAXBContext ctx = JAXBContext.newInstance(DebPackage.class);
			for (File f : configDir.listFiles()) {
				if (f.getName().endsWith(".xml")) {
					Unmarshaller u = ctx.createUnmarshaller();
					DebPackage pkg = (DebPackage)u.unmarshal(f);
					build(pkg);
				}
			}
		}
		catch (Exception ex) {
			throw new MojoFailureException("Oops", ex);
		}
	}
	
	private void build(DebPackage pkg) throws Exception {
		
		ControlAction control = null;
		for (DebAction action : pkg.actions()) {
			if (action instanceof ControlAction) {
				control = (ControlAction)action;
				break;
			}
		}
		
		if (control == null) {
			control = new ControlAction();
			pkg.actions().add(0, control);
		}
		if (control.getPackageName() == null) {
			control.setPackageName(project.getArtifactId());
		}
		if (control.getVersion() == null) {
			control.setVersion(project.getVersion());
		}
		if (control.getArchitecture() == null) {
			control.setArchitecture("all");
		}
		if (control.getDepends() == null) {
			control.setDepends("sun-java6-jre");
		}
		if (control.getMaintainer() == null) {
			control.setMaintainer("John Doe <john@doe.com>");
		}
		if (control.getDescription() == null) {
			if (project.getDescription() != null) {
				control.setDescription(project.getDescription());
			}
			else {
				control.setDescription("This is a great program.");
			}
		}
		
		DummyAutoClasspathAction autoAction = null;
		for (DebAction action : pkg.actions()) {
			if (action instanceof DummyAutoClasspathAction) {
				autoAction = (DummyAutoClasspathAction)action;
				break;
			}
		}
		
		if (autoAction != null) {
			
			StaticFileSet cpFiles = new StaticFileSet();
			for (File f : buildCp()) {
				cpFiles.add(f);
			}
			
			CopyAction cp = new CopyAction();
			cp.setFiles(cpFiles);
			cp.setDirMode("755");
			cp.setFileMode("644");
			cp.setTargetDir("usr/local/lib/" + control.getPackageName());
			cp.setAssumedTargetPathLevel(3);
			cp.setAppendToClasspath(true);
			cp.setComponent(DebComponent.CONTENT);
			
			int index = pkg.actions().indexOf(autoAction);
			pkg.actions().set(index, cp);
		}
		
		File f = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".deb");
		DebWriter w = new DebWriter(pkg);
		w.write(new FileOutputStream(f));

		projectHelper.attachArtifact(project, "deb", f);
	}
	
	private Set<File> buildCp() {
        try {

            Set<File> classpath = new HashSet<File>();

            Set<Artifact> artifacts = project.createArtifacts(artifactFactory, null, null);
            ArtifactResolutionResult arr = resolver.resolveTransitively(artifacts, project.getArtifact(), localRepository, remoteRepositories, artifactMetadataSource, null);

            for (Artifact resolvedArtifact : (Set<Artifact>)arr.getArtifacts()) {
                classpath.add(resolvedArtifact.getFile());
            }

            for (String e : classpathElements) {
            	classpath.add(new File(e));
            }
            
            File classesDir = new File(project.getBuild().getDirectory(), "classes");
            classpath.remove(classesDir);
            
            File artifactFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar");
            classpath.add(artifactFile);

            return classpath;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
