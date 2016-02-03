package net.simpleframework.workflow.engine.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.ctx.service.ado.db.AbstractDbBeanService;
import net.simpleframework.workflow.engine.EWorkitemStatus;
import net.simpleframework.workflow.engine.IUserStatService;
import net.simpleframework.workflow.engine.bean.UserStatBean;
import net.simpleframework.workflow.engine.bean.WorkitemBean;

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

	@Override
	public int[] getComplete_AllWorkitems(final ID userId, final Date date) {
		final Calendar c1 = Calendar.getInstance();
		c1.setTime(date);
		c1.set(Calendar.HOUR_OF_DAY, 0);
		c1.set(Calendar.MINUTE, 0);
		c1.set(Calendar.SECOND, 0);

		final Calendar c2 = Calendar.getInstance();
		c2.setTime(c1.getTime());
		c2.add(Calendar.DATE, 1);

		final IDataQuery<Map<String, Object>> dq = getEntityManager().queryMapSet(
				new SQLValue("select status, count(status) as cc from "
						+ getTablename(WorkitemBean.class)
						+ " where userid=? and (createdate>? and createdate<?) group by status", userId,
						c1.getTime(), c2.getTime()));
		Map<String, Object> map;
		int complete = 0;
		int count = 0;
		while ((map = dq.next()) != null) {
			final int cc = Convert.toInt(map.get("cc"));
			final EWorkitemStatus status = Convert.toEnum(EWorkitemStatus.class, map.get("status"));
			if (status == EWorkitemStatus.complete) {
				complete = cc;
			}
			count += cc;
		}
		return new int[] { complete, count };
	}
}
