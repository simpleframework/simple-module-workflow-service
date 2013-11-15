package net.simpleframework.workflow.engine;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IMappingVal {

	/**
	 * 获取映射变量的值。主要用在主-子流程的变量映射中
	 * 
	 * @param mapping
	 * @return
	 */
	Object val(String mapping);
}
