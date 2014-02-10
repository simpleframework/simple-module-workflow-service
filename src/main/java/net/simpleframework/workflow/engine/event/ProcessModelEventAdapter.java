package net.simpleframework.workflow.engine.event;

import net.simpleframework.workflow.engine.ProcessModelBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class ProcessModelEventAdapter implements IProcessModelEventListener {

	@Override
	public void onDeploy(final ProcessModelBean processModel) {
	}

	@Override
	public void onResume(final ProcessModelBean processModel) {
	}
}
