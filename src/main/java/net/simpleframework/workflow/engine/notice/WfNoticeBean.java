package net.simpleframework.workflow.engine.notice;

import java.util.Date;

import net.simpleframework.ado.bean.AbstractUserAwareBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfNoticeBean extends AbstractUserAwareBean {
	/* 当前流程id */
	private ID processId;

	/* 关联的工作项id */
	private ID workitemId;

	/* 发送id，和userid唯一 */
	private String sentKey;

	/* 状态 */
	private ENoticeStatus status;

	/* 预计发送时间 */
	private Date dsentDate;
	/* 实际发送时间 */
	private Date sentDate;

	/* 接口唯一编号 */
	private int typeNo;
	/* 发送的消息 */
	private String smessage;

	/* 发送次数 */
	private int sents;

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(final ID processId) {
		this.processId = processId;
	}

	public ID getWorkitemId() {
		return workitemId;
	}

	public void setWorkitemId(final ID workitemId) {
		this.workitemId = workitemId;
	}

	public String getSentKey() {
		return sentKey;
	}

	public void setSentKey(String sentKey) {
		this.sentKey = sentKey;
	}

	public ENoticeStatus getStatus() {
		return status;
	}

	public void setStatus(final ENoticeStatus status) {
		this.status = status;
	}

	public Date getDsentDate() {
		return dsentDate;
	}

	public void setDsentDate(final Date dsentDate) {
		this.dsentDate = dsentDate;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(final Date sentDate) {
		this.sentDate = sentDate;
	}

	public int getTypeNo() {
		return typeNo;
	}

	public void setTypeNo(final int typeNo) {
		this.typeNo = typeNo;
	}

	public String getSmessage() {
		return smessage;
	}

	public void setSmessage(final String smessage) {
		this.smessage = smessage;
	}

	public int getSents() {
		return sents;
	}

	public void setSents(int sents) {
		this.sents = sents;
	}

	public static enum ENoticeStatus {
		ready,

		sent,

		unsent,

		fail,

		abort
	}

	private static final long serialVersionUID = 7506873113036405724L;
}
