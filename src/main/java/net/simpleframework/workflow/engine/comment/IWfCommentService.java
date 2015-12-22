package net.simpleframework.workflow.engine.comment;

import net.simpleframework.module.common.content.ICommentService;
import net.simpleframework.workflow.engine.bean.AbstractWorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfCommentService extends ICommentService<WfComment> {

	/**
	 * 获取当前的评论
	 * 
	 * @param workitem
	 * @return
	 */
	WfComment getCurComment(AbstractWorkitemBean workitem);
}