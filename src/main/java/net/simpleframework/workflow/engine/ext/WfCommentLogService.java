package net.simpleframework.workflow.engine.ext;

import net.simpleframework.ado.FilterItem;
import net.simpleframework.ado.FilterItems;
import net.simpleframework.ado.query.IDataQuery;
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
	public IDataQuery<WfCommentLog> queryLogs(final Object comment, final ELogType logType) {
		final FilterItems items = FilterItems.of(new FilterItem("commentId", getIdParam(comment)));
		if (logType != null) {
			items.append(new FilterItem("logType", logType));
		}
		return queryByParams(items);
	}

	@Override
	public int getLogSize() {
		return 5;
	}
}