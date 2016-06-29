package net.simpleframework.workflow.engine;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public enum EVariableSource {

	/**
	 * 流程变量
	 */
	process,

	/**
	 * 环节变量
	 */
	activity,

	/**
	 * 流程变量静态类型
	 */
	model
}
