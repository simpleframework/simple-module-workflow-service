package net.simpleframework.workflow.engine.impl;

import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.BeanUtils.PropertyWrapper;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.EWorkitemStatus;
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
	public UserStatBean getUserStat(final ID userId) {
		UserStatBean stat = getBean("userId=?", userId);
		if (stat == null) {
			stat = createBean();
			stat.setUserId(userId);
			insert(stat);
		}
		return stat;
	}

	@Override
	public int getAllWorkitems(final UserStatBean userStat) {
		int c = 0;
		for (final EWorkitemStatus s : EWorkitemStatus.values()) {
			c += (Integer) BeanUtils.getProperty(userStat, "workitem_" + s.name());
		}
		return c;
	}

	void reset(final UserStatBean stat) {
		for (final PropertyWrapper p : BeanUtils.getProperties(UserStatBean.class).values()) {
			if ("int".equals(p.type.getName())) {
				BeanUtils.setProperty(stat, p.name, 0);
			}
		}
	}
}
