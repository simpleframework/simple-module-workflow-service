package net.simpleframework.workflow.engine.impl;

import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.IUserStatService;
import net.simpleframework.workflow.engine.bean.UserStatBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class UserStatService extends AbstractDbBeanService<UserStatBean> implements
		IUserStatService {

	@Override
	public UserStatBean getUserStat(ID userId) {
		UserStatBean stat = getBean("userId=?", userId);
		if (stat == null) {

		}
		return null;
	}
}
