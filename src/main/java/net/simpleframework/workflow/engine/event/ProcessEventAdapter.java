package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;
import net.simpleframework.workflow.engine.ProcessModelBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ProcessEventAdapter implements IProcessEventListener {

	@Override
	public void onModelDeploy(final ProcessModelBean processModel) {
	}

	@Override
	public void onModelResume(final ProcessModelBean processModel) {
	}

	@Override
	public void onProcessCreated(final InitiateItem initiateItem, final ProcessBean process) {
	}

	@Override
	public void onProcessAbort(final ProcessBean process, final EProcessAbortPolicy policy) {
	}

	@Override
	public void onProcessDelete(final ProcessBean process) {
	}

	@Override
	public void onProcessSuspend(final ProcessBean process) {
	}

	@Override
	public void onProcessResume(final ProcessBean process) {
	}
}
