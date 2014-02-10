package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.WorkitemBean;
import net.simpleframework.workflow.engine.WorkitemComplete;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkitemEventAdapter implements IWorkitemEventListener {

	@Override
	public void onCompleted(final WorkitemComplete workitemComplete) {
	}

	@Override
	public void onRetake(final WorkitemBean workitem) {
	}
}
