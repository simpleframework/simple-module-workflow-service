package net.simpleframework.workflow.engine.ext;

import java.util.Date;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.WorkitemBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfCommentUserService extends AbstractDbBeanService<WfCommentUser> implements
		IWfCommentUserService, IWorkflowContextAware {

	@Override
	public WfCommentUser getCommentUser2(final ID userId, final ID contentId) {
		WfCommentUser commentUser = getBean("userId=? and contentId=?", userId, contentId);
		if (commentUser == null) {
			commentUser = createBean();
			commentUser.setCreateDate(new Date());
			commentUser.setUserId(userId);
			commentUser.setContentId(contentId);
			insert(commentUser);
		}
		return commentUser;
	}

	@Override
	public WfCommentUser getCommentUser(final WorkitemBean workitem) {
		return getCommentUser2(workitem.getUserId(), workitem.getProcessId());
	}

	@Override
	public void resetCommentUser(final ID userId, final ID contentId) {
		final WfCommentUser commentUser = getBean("userId=? and contentId=?", userId, contentId);
		if (commentUser != null) {
			commentUser.setNcomments(0);
			update(new String[] { "ncomments" }, commentUser);
		}
	}

}
