package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.EProcessAbortPolicy;
import net.simpleframework.workflow.engine.InitiateItem;
import net.simpleframework.workflow.engine.ProcessBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class ProcessEventAdapter implements IProcessEventListener {

	@Override
	public void onCreated(final InitiateItem initiateItem, final ProcessBean process) {
	}

	@Override
	public void onAbort(final ProcessBean process, final EProcessAbortPolicy policy) {
	}

	@Override
	public void onDelete(final ProcessBean process) {
	}

	@Override
	public void onSuspend(final ProcessBean process) {
	}

	@Override
	public void onResume(final ProcessBean process) {
	}
}
