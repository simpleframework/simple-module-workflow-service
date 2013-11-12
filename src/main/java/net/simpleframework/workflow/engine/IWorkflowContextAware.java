package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.IModuleContextAware;
import net.simpleframework.ctx.ModuleContextFactory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IWorkflowContextAware extends IModuleContextAware {

	static final IWorkflowContext context = ModuleContextFactory.get(IWorkflowContext.class);
}
