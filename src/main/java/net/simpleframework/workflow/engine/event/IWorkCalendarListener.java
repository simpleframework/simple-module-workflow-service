package net.simpleframework.workflow.engine.event;

import java.util.Calendar;
import java.util.Date;

import net.simpleframework.workflow.engine.bean.AbstractWorkflowBean;

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
	 * @param workflowBean
	 * @param start
	 * @param minute
	 * @return
	 */
	Date getRealDate(AbstractWorkflowBean workflowBean, Date start, int minute);

	Date getRealDate(AbstractWorkflowBean workflowBean, int minute);

	long getRelativeMilliseconds(AbstractWorkflowBean workflowBean, Date start, Date end);

	public static class WorkCalendarAdapter implements IWorkCalendarListener {

		@Override
		public Date getRealDate(final AbstractWorkflowBean workflowBean, final Date start,
				final int minute) {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(start.getTime());
			cal.add(Calendar.MINUTE, minute);
			return cal.getTime();
		}

		@Override
		public Date getRealDate(final AbstractWorkflowBean workflowBean, final int minute) {
			return getRealDate(workflowBean, new Date(), minute);
		}

		@Override
		public long getRelativeMilliseconds(final AbstractWorkflowBean workflowBean,
				final Date start, final Date end) {
			return Math.max(0, end.getTime() - start.getTime());
		}
	}
}
