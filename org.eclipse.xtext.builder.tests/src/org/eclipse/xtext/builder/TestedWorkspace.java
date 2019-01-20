/*******************************************************************************
 * Copyright (c) 2019 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.builder;

import static org.eclipse.xtext.builder.impl.BuilderUtil.*;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.xtext.builder.impl.ProjectOpenedOrClosedListener;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.testing.RepeatedTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.util.Exceptions;
import org.junit.Assert;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@Beta
public abstract class TestedWorkspace extends TestWatcher {

	private String name;

	private final ProjectOpenedOrClosedListener closedProjectTaskProcessor;

	protected TestedWorkspace(ProjectOpenedOrClosedListener closedProjectTaskProcessor) {
		this.closedProjectTaskProcessor = closedProjectTaskProcessor;
	}
	
	@Override
	public Statement apply(Statement statement, Description description) {
		Statement result = super.apply(statement, description);
		// Apparently there is no good solution to order TestRules
		// that's why we do inline the (simple) logic for RepeatedTest here.
		RepeatedTest repeat = description.getAnnotation(RepeatedTest.class);
		if (repeat == null) {
			repeat = description.getTestClass().getAnnotation(RepeatedTest.class);
		}
		if (repeat != null) {
			result = new RepeatedTest.Rule.RepeatedTestStatement(repeat.times(), result, description, false);
		}
		return result;
	}
	
	@Override
	protected void starting(Description d) {
		name = d.getMethodName();
		
		assertEmptyIndex();
		Assert.assertEquals(0, IResourcesSetupUtil.root().getProjects().length);

		if (PlatformUI.isWorkbenchRunning()) {
			final IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
			if (introManager.getIntro() != null) {
				Display.getDefault().asyncExec(()->introManager.closeIntro(introManager.getIntro()));
			}
		}
	}

	private void joinRemovedProjectsJob() {
		closedProjectTaskProcessor.joinRemoveProjectJob();
	}

	@Override
	protected void finished(Description description) {
		super.finished(description);
		deleteAllProjects();
		build();
		assertEmptyIndex();
	}

	public void assertEmptyIndex() {
		Collection<IResourceDescription> inIndex = Lists.newArrayList(getBuilderState().getAllResourceDescriptions());
		if (!inIndex.isEmpty()) {
			StringBuilder remaining = new StringBuilder();
			inIndex.forEach(desc -> remaining.append(desc.getURI()).append("\n"));
			Assert.assertEquals(remaining.toString(), 0, inIndex.size());
		}
	}

	public void build() {
		joinJobsBeforeBuild();
		IResourcesSetupUtil.waitForBuild();
	}

	public void joinJobsBeforeBuild() {
		joinRemovedProjectsJob();
	}

	public void cleanBuild() {
		try {
			joinJobsBeforeBuild();
			IResourcesSetupUtil.cleanBuild();
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}

	public void fullBuild() {
		try {
			joinJobsBeforeBuild();
			IResourcesSetupUtil.fullBuild();
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}

	public IFile createFile(IPath wsRelativePath, final String content) {
		try {
			return IResourcesSetupUtil.createFile(wsRelativePath, content);
		} catch (Exception e) {
			return Exceptions.throwUncheckedException(e);
		}
	}
	
	public IFile createFile(String wsRelativePath, String content) {
		return createFile(new Path(wsRelativePath), content);
	}

	public String readFile(IFile file) {
		try {
			return IResourcesSetupUtil.fileToString(file);
		} catch (Exception e) {
			return Exceptions.throwUncheckedException(e);
		}
	}

	public void deleteAllProjects() {
		try {
			IResourcesSetupUtil.cleanWorkspace();
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}

	/**
	 * @return the name of the currently-running test method
	 */
	public String getTestName() {
		return name;
	}

	public IProject createProject() {
		return createProject(getTestName());
	}

	public IProject createProject(String name) {
		try {
			return IResourcesSetupUtil.createProject(name);
		} catch (Exception e) {
			return Exceptions.throwUncheckedException(e);
		}
	}

	public void addNature(IProject project, String natureId) {
		try {
			IResourcesSetupUtil.addNature(project, natureId);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}
	
	public void addBuilder(IProject project, String builderId) {
		try {
			IResourcesSetupUtil.addBuilder(project, builderId);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}
	
	public void setReference(final IProject from, final IProject to) {
		try {
			IResourcesSetupUtil.setReference(from, to);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}
	
	public void removeReference(final IProject from, final IProject to) {
		try {
			IResourcesSetupUtil.removeReference(from, to);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}
	
	public void removeNature(IProject project, String natureId) {
		try {
			IResourcesSetupUtil.removeNature(project, natureId);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}
	
	public void removeBuilder(IProject project, String builderId) {
		try {
			IResourcesSetupUtil.removeBuilder(project, builderId);
		} catch (Exception e) {
			Exceptions.throwUncheckedException(e);
		}
	}

	public IWorkspaceRoot root() {
		return IResourcesSetupUtil.root();
	}

	public IProgressMonitor monitor() {
		return IResourcesSetupUtil.monitor();
	}

	public void enableAutobuild(Runnable r) {
		withAutobuild(true, r);
	}

	public void disableAutobuild(Runnable r) {
		withAutobuild(false, r);
	}

	private void withAutobuild(boolean enabled, Runnable r) {
		boolean prev = ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		if (prev == enabled) {
			r.run();
		} else {
			try {
				IResourcesSetupUtil.setAutobuild(enabled);
				r.run();
			} finally {
				IResourcesSetupUtil.setAutobuild(prev);
			}
		}
	}
}