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
	 * 工作日历，根据minute数推算真实日期
	 * 
	 * @param minute
	 * @return
	 */
	Date getRealDate(Date start, int minute);

	Date getRealDate(int minute);

	long getRelativeMilliseconds(Date start, Date end);

	public static class WorkCalendarAdapter implements IWorkCalendarListener {

		@Override
		public Date getRealDate(Date start, final int minute) {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(start.getTime());
			cal.add(Calendar.MINUTE, minute);
			return cal.getTime();
		}

		@Override
		public Date getRealDate(int minute) {
			return getRealDate(new Date(), minute);
		}

		@Override
		public long getRelativeMilliseconds(Date start, Date end) {
			return Math.max(0, end.getTime() - start.getTime());
		}
	}
}
