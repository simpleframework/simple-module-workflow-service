package net.simpleframework.workflow.engine.comment;

import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;
import net.simpleframework.workflow.engine.comment.WfCommentLog.ELogType;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfCommentLogService extends IDbBeanService<WfCommentLog> {

	/**
	 * 
	 * @param userId
	 * @param logType
	 * @return
	 */
	IDataQuery<WfCommentLog> queryLogs(ID userId, ELogType logType);

	WfCommentLog getLog(ID userId, String ccomment, ELogType logType);

	WfCommentLog insertLog(WfComment comment, ELogType logType);

	/**
	 * 获取保存日志的记录个数
	 * 
	 * @return
	 */
	int getLogSize();
}