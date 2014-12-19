package net.simpleframework.workflow.engine.ext;

import net.simpleframework.module.common.content.impl.AbstractCommentService;
import net.simpleframework.workflow.engine.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfCommentService extends AbstractCommentService<WfComment> implements
		IWfCommentService {

	@Override
	public WfComment getCurComment(final WorkitemBean workitem) {
		return getBean("workitemId=?", workitem.getId());
	}
}