package net.simpleframework.workflow.engine.ext;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfCommentUserService extends AbstractDbBeanService<WfCommentUser> implements
		IWfCommentUserService, IWorkflowContextAware {

	@Override
	public WfCommentUser getCommentUser(final ID userId, final Object content) {
		return getBean("userId=? and contentId=?", userId, getIdParam(content));
	}

	@Override
	public void resetCommentUser(final ID userId, final Object content) {
		final WfCommentUser commentUser = getCommentUser(userId, getIdParam(content));
		if (commentUser != null) {
			commentUser.setNcomments(0);
			update(new String[] { "ncomments" }, commentUser);
		}
	}
}
