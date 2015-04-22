package net.simpleframework.workflow.engine;

import java.util.Collection;

import net.simpleframework.workflow.engine.bean.AbstractWorkflowBean;
import net.simpleframework.workflow.engine.event.IWorkflowListener;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IEventListenerAware<T extends AbstractWorkflowBean> {

	/**
	 * 
	 * @param bean
	 * @param listenerClass
	 */
	void addEventListener(T bean, Class<? extends IWorkflowListener> listenerClass);

	/**
	 * 
	 * @param bean
	 * @param listenerClass
	 * @return
	 */
	boolean removeEventListener(T bean, Class<? extends IWorkflowListener> listenerClass);

	/**
	 * 
	 * @param bean
	 * @return
	 */
	Collection<IWorkflowListener> getEventListeners(T bean);
}
