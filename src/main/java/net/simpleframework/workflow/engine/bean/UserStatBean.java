package net.simpleframework.workflow.engine.bean;

import net.simpleframework.ado.bean.AbstractIdBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class UserStatBean extends AbstractIdBean {
	/* 用户id */
	private ID userId;

	/* 按状态 */
	private int workitem_running;
	private int workitem_suspended;
	private int workitem_delegate;
	private int workitem_complete;
	private int workitem_abort;
	private int workitem_retake;

	/* 未读待办 */
	private int workitem_unread;

	/* 未读待阅 */
	private int workview_unread;

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public int getWorkitem_running() {
		return workitem_running;
	}

	public void setWorkitem_running(final int workitem_running) {
		this.workitem_running = workitem_running;
	}

	public int getWorkitem_suspended() {
		return workitem_suspended;
	}

	public void setWorkitem_suspended(final int workitem_suspended) {
		this.workitem_suspended = workitem_suspended;
	}

	public int getWorkitem_delegate() {
		return workitem_delegate;
	}

	public void setWorkitem_delegate(final int workitem_delegate) {
		this.workitem_delegate = workitem_delegate;
	}

	public int getWorkitem_complete() {
		return workitem_complete;
	}

	public void setWorkitem_complete(final int workitem_complete) {
		this.workitem_complete = workitem_complete;
	}

	public int getWorkitem_abort() {
		return workitem_abort;
	}

	public void setWorkitem_abort(final int workitem_abort) {
		this.workitem_abort = workitem_abort;
	}

	public int getWorkitem_retake() {
		return workitem_retake;
	}

	public void setWorkitem_retake(final int workitem_retake) {
		this.workitem_retake = workitem_retake;
	}

	public int getWorkitem_unread() {
		return workitem_unread;
	}

	public void setWorkitem_unread(final int workitem_unread) {
		this.workitem_unread = workitem_unread;
	}

	public int getWorkview_unread() {
		return workview_unread;
	}

	public void setWorkview_unread(final int workview_unread) {
		this.workview_unread = workview_unread;
	}

	private static final long serialVersionUID = 6821091881344434822L;
}
