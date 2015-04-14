package net.simpleframework.workflow.engine.ext;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfUserCommentService extends IDbBeanService<WfUserComment> {

	/**
	 * @param userId
	 * @param contentId
	 * @return
	 */
	WfUserComment getUserComment(ID userId, ID contentId);
}
