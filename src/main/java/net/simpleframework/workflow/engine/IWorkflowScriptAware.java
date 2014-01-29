package net.simpleframework.workflow.engine;

import java.util.Map;

import net.simpleframework.ctx.script.IScriptEval;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowScriptAware<T extends AbstractWorkflowBean> {

	/**
	 * 
	 * @param bean
	 * @return
	 */
	IScriptEval createScriptEval(final T bean);

	/**
	 * 
	 * @param bean
	 * @return
	 */
	Map<String, Object> createVariables(final T bean);
}
