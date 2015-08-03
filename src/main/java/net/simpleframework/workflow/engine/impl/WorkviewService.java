package net.simpleframework.workflow.engine.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkviewService;
import net.simpleframework.workflow.engine.bean.AbstractWorkitemBean;
import net.simpleframework.workflow.engine.bean.UserStatBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;
import net.simpleframework.workflow.engine.bean.WorkviewBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WorkviewService extends AbstractDbBeanService<WorkviewBean> implements
		IWorkviewService {

	@Override
	public List<WorkviewBean> createWorkviews(final WorkitemBean workitem, final ID... userIds) {
		return _createWorkviews(workitem, userIds);
	}

	@Override
	public List<WorkviewBean> createForwardWorkviews(final WorkviewBean workview,
			final ID... userIds) {
		return _createWorkviews(workview, userIds);
	}

	protected List<WorkviewBean> _createWorkviews(final AbstractWorkitemBean _workitem,
			final ID... userIds) {
		WorkviewBean workview2 = null;
		WorkitemBean workitem;
		if (_workitem instanceof WorkitemBean) {
			workitem = (WorkitemBean) _workitem;
		} else {
			workview2 = (WorkviewBean) _workitem;
			workitem = wfwService.getBean(workview2.getWorkitemId());
		}
		final List<WorkviewBean> list = new ArrayList<WorkviewBean>();
		for (final ID id : userIds) {
			final ID processId = workitem.getProcessId();
			if (getWorkviewBean(processId, id) != null) {
				continue;
			}
			final WorkviewBean workview = createBean();
			workview.setWorkitemId(workitem.getId());

			workview.setProcessId(processId);
			if (workview2 != null) {
				workview.setParentId(workview2.getId());
			}

			final PermissionUser user = permission.getUser(id);
			workview.setUserId(user.getId());
			workview.setUserText(user.getText());
			workview.setDeptId(user.getDept().getId());
			final ID domainId = user.getDept().getDomainId();
			if (domainId != null) {
				workview.setDomainId(domainId);
			}
			insert(workview);
			list.add(workview);
		}
		return list;
	}

	@Override
	public WorkviewBean getWorkviewBean(final Object processId, final Object userId) {
		return getBean("processId=? and userId=?", getIdParam(processId), getIdParam(userId));
	}

	@Override
	public void doReadMark(final WorkviewBean workview) {
		if (!workview.isReadMark()) {
			workview.setReadMark(true);
			workview.setReadDate(new Date());
			update(new String[] { "readMark", "readDate" }, workview);
		}
	}

	protected String getDefaultOrderby() {
		return " order by createdate desc";
	}

	@Override
	public IDataQuery<WorkviewBean> getWorkviewsList(final ID userId) {
		return query("userId=?" + getDefaultOrderby(), userId);
	}

	@Override
	public IDataQuery<WorkviewBean> getUnreadWorkviewsList(final ID userId) {
		return query("userId=? and readMark=?" + getDefaultOrderby(), userId, Boolean.FALSE);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		addListener(new DbEntityAdapterEx<WorkviewBean>() {

			@Override
			public void onAfterUpdate(final IDbEntityManager<WorkviewBean> manager,
					final String[] columns, final WorkviewBean[] beans) throws Exception {
				super.onAfterUpdate(manager, columns, beans);
				for (final WorkviewBean workview : beans) {
					if (ArrayUtils.contains(columns, "readMark", true)) {
						doUserStat_readMark(workview.getUserId());
					}
				}
			}

			@Override
			public void onAfterInsert(final IDbEntityManager<WorkviewBean> manager,
					final WorkviewBean[] beans) throws Exception {
				super.onAfterInsert(manager, beans);

				for (final WorkviewBean workview : beans) {
					// 设置用户统计
					doUserStat_readMark(workview.getUserId());
				}
			}

			private void doUserStat_readMark(final ID userId) {
				final UserStatBean stat = wfusService.getUserStat(userId);
				stat.setWorkview_unread(getUnreadWorkviewsList(userId).getCount());
				wfusService.update(new String[] { "workview_unread" }, stat);
			}
		});
	}
}