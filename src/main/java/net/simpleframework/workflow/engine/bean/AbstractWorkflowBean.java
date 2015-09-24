package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.bean.AbstractDateAwareBean;
import net.simpleframework.workflow.engine.IWorkflowServiceAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@SuppressWarnings("serial")
public abstract class AbstractWorkflowBean extends AbstractDateAwareBean implements
		IWorkflowServiceAware {
}