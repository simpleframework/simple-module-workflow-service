package net.simpleframework.workflow.engine.ext;

import java.util.Date;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfUserCommentService extends AbstractDbBeanService<WfUserComment> implements
		IWfUserCommentService, IWorkflowContextAware {

	@Override
	public WfUserComment getUserComment(ID userId, ID contentId) {
		WfUserComment ucomment = getBean("userId=? and contentId=?", userId, contentId);
		if (ucomment == null) {
			ucomment = createBean();
			ucomment.setCreateDate(new Date());
			ucomment.setUserId(userId);
			ucomment.setContentId(contentId);
		}
		return ucomment;
	}
}
