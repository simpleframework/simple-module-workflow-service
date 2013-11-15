package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ActivityBean;
import net.simpleframework.workflow.engine.ActivityComplete;
import net.simpleframework.workflow.engine.EActivityAbortPolicy;
import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ActivityAdapter implements IActivityListener {

	@Override
	public void onCompleted(final ActivityComplete activityComplete) {
	}

	@Override
	public void onAbort(final ActivityBean activity, final EActivityAbortPolicy policy) {
	}

	@Override
	public void onSuspend(final ActivityBean activity) {
	}

	@Override
	public void onFallback(final ActivityBean activity, final String tasknode) {
	}

	@Override
	public void onWorkitemCompleted(final WorkitemComplete workitemComplete) {
	}

	@Override
	public void onWorkitemRetake(final WorkitemBean workitem) {
	}
}
