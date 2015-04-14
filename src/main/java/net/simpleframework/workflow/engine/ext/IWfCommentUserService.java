package net.simpleframework.workflow.engine.ext;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.IDbBeanService;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWfCommentUserService extends IDbBeanService<WfCommentUser> {

	/**
	 * @param userId
	 * @param content
	 * @return
	 */
	WfCommentUser getCommentUser(ID userId, Object content);

	void resetCommentUser(ID userId, Object content);
}
