package net.simpleframework.workflow.engine;

import net.simpleframework.workflow.IWorkflowHandler;
import net.simpleframework.workflow.schema.TransitionNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface ITransitionHandler extends IWorkflowHandler {

	/**
	 * 
	 * @param transition
	 * @return
	 */
	boolean isPass(TransitionNode transition);
}
