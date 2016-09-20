package net.simpleframework.workflow.engine.comment;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.module.common.content.impl.AbstractCommentService;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.bean.AbstractWorkitemBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.comment.WfCommentLog.ELogType;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfCommentService extends AbstractCommentService<WfComment>
		implements IWfCommentService, IWorkflowContextAware {

	@Override
	public WfComment getCurComment(final AbstractWorkitemBean workitem) {
		return workitem == null ? null : getBean("workitemId=?", workitem.getId());
	}

	protected void updateUserComments(final WfComment c, final int i) {
		final ProcessBean process = wfpService.getBean(c.getContentId());
		if (process != null) {
			final IWfCommentUserService uService = workflowContext.getCommentUserService();
			// 获取除放弃状态下的任务项，通知新到意见数
			final List<WorkitemBean> list = wfwService.getWorkitems(process, null,
					EWorkitemStatus.running, EWorkitemStatus.suspended, EWorkitemStatus.delegate,
					EWorkitemStatus.complete);

			final ID processId = process.getId();
			final HashSet<ID> users = new HashSet<ID>();
			for (final WorkitemBean w : list) {
				users.add(w.getUserId2());
			}

			for (final ID userId : users) {
				WfCommentUser commentUser = uService.getCommentUser(userId, processId);
				if (commentUser == null) {
					commentUser = uService.createBean();
					commentUser.setCreateDate(new Date());
					commentUser.setUserId(userId);
					commentUser.setContentId(processId);
					uService.insert(commentUser);
				}
				commentUser.setNcomments(commentUser.getNcomments() + i);
				uService.update(new String[] { "ncomments" }, commentUser);
			}
		}
	}

	protected void updateProcessComments(final WfComment c) {
		final ProcessBean process = wfpService.getBean(c.getContentId());
		process.setComments(count("contentId=?", process.getId()));
		wfpService.update(new String[] { "comments" }, process);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		final WfCommentLogService lService = (WfCommentLogService) workflowContext
				.getCommentLogService();

		addListener(new DbEntityAdapterEx<WfComment>() {
			@Override
			public void onAfterInsert(final IDbEntityManager<WfComment> manager,
					final WfComment[] beans) throws Exception {
				super.onAfterInsert(manager, beans);
				for (final WfComment c : beans) {
					lService.insertLog(c, ELogType.history);

					// 更新process
					updateProcessComments(c);
					updateUserComments(c, 1);
				}
			}

			@Override
			public void onAfterDelete(final IDbEntityManager<WfComment> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onAfterDelete(manager, paramsValue);
				for (final WfComment c : coll(manager, paramsValue)) {
					// 删除关联的意见
					lService.deleteWith("commentId=?", c.getId());

					updateProcessComments(c);
					updateUserComments(c, -1);
				}
			}

			@Override
			public void onAfterUpdate(final IDbEntityManager<WfComment> manager,
					final String[] columns, final WfComment[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);
				if (ArrayUtils.isEmpty(columns) || ArrayUtils.contains(columns, "ccomment", true)) {
					for (final WfComment comment : beans) {
						if (lService.getLog(comment.getUserId(), comment.getCcomment(),
								ELogType.history) == null) {
							lService.insertLog(comment, ELogType.history);
						}
					}
				}
			}
		});

		// 流程被删除后执行
		wfpService.addListener(new DbEntityAdapterEx<ProcessBean>() {
			@Override
			public void onAfterDelete(final IDbEntityManager<ProcessBean> manager,
					final IParamsValue paramsValue) throws Exception {
				super.onAfterDelete(manager, paramsValue);
				for (final ProcessBean process : coll(manager, paramsValue)) {
					// 删除意见
					final Object id = process.getId();
					wfcService.deleteWith("contentId=?", id);
					wfcuService.deleteWith("contentId=?", id);
				}
			}
		});
	}
}