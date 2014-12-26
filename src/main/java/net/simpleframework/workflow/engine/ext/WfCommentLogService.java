package net.simpleframework.workflow.engine.ext;

import java.util.Date;

import net.simpleframework.ado.FilterItem;
import net.simpleframework.ado.FilterItems;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.engine.ext.WfCommentLog.ELogType;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfCommentLogService extends AbstractDbBeanService<WfCommentLog> implements
		IWfCommentLogService, IWorkflowContextAware {

	@Override
	public IDataQuery<WfCommentLog> queryLogs(final ID userId, final ELogType logType) {
		if (userId == null) {
			return DataQueryUtils.nullQuery();
		}
		final FilterItems items = FilterItems.of(new FilterItem("userId", userId));
		if (logType != null) {
			items.append(new FilterItem("logType", logType));
		}
		return queryByParams(items);
	}

	@Override
	public WfCommentLog getLog(final ID userId, final String ccomment, final ELogType logType) {
		return getBean("userid=? and ccomment=? and logtype=?", userId, ccomment, logType);
	}

	@Override
	public WfCommentLog insertLog(final WfComment comment, final ELogType logType) {
		final WfCommentLog log = createBean();
		log.setCommentId(comment.getId());
		log.setCreateDate(new Date());
		log.setUserId(comment.getUserId());
		log.setCcomment(comment.getCcomment());
		log.setLogType(logType);
		insert(log);
		return log;
	}

	@Override
	public int getLogSize() {
		return 10;
	}
}