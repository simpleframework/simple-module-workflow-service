package net.simpleframework.workflow.engine;

import java.util.Collection;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IVariableAware<T extends AbstractWorkflowBean> {

	/**
	 * 获取变量的值
	 * 
	 * @param bean
	 * @param name
	 * @return
	 */
	Object getVariable(T bean, String name);

	/**
	 * 设置变量的值
	 * 
	 * @param bean
	 * @param name
	 * @param value
	 */
	void setVariable(T bean, String name, Object value);

	void setVariable(T bean, String[] names, Object[] values);

	/**
	 * 获取（流程、环节）所有定义的变量
	 * 
	 * @param bean
	 * @return
	 */
	Collection<String> getVariableNames(T bean);
}
