package net.simpleframework.workflow.engine.event;

import java.util.Calendar;
import java.util.Date;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkCalendarListener extends IWorkflowListener {

	/**
	 * 工作日历，根据小时数推算真实日期
	 * 
	 * @param hours
	 * @return
	 */
	Date getRealDate(int hours);

	public static class WorkCalendarAdapter implements IWorkCalendarListener {

		@Override
		public Date getRealDate(final int hours) {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(System.currentTimeMillis());
			cal.add(Calendar.HOUR_OF_DAY, hours);
			return cal.getTime();
		}
	}
}
