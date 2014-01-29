package net.simpleframework.workflow.engine;

import java.util.Date;

import net.simpleframework.ado.bean.AbstractDescriptionBean;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status" })
public class DelegationBean extends AbstractDescriptionBean {

	/* 委托的对象 */
	private EDelegationSource delegationSource;

	/* 委托的对象id */
	private ID sourceId;

	private EDelegationStatus status;

	/* 委托的用户id */
	private ID userId;

	/* 创建时间 */
	private Date createDate;

	/* 委托的实际开始时间和结束时间 */
	private Date runningDate, completeDate;

	/* 定义委托的开始执行时间和结束执行时间 */
	private Date startDate, endDate;

	/* 定义该委托类，参考IDelegationHandler */
	private String ruleHandler;

	public EDelegationSource getDelegationSource() {
		return delegationSource;
	}

	public void setDelegationSource(final EDelegationSource delegationSource) {
		this.delegationSource = delegationSource;
	}

	public ID getSourceId() {
		return sourceId;
	}

	public void setSourceId(final ID sourceId) {
		this.sourceId = sourceId;
	}

	public EDelegationStatus getStatus() {
		return status != null ? status : EDelegationStatus.ready;
	}

	public void setStatus(final EDelegationStatus status) {
		this.status = status;
	}

	public ID getUserId() {
		return userId;
	}

	public void setUserId(final ID userId) {
		this.userId = userId;
	}

	public Date getCreateDate() {
		if (createDate == null) {
			createDate = new Date();
		}
		return createDate;
	}

	public void setCreateDate(final Date createDate) {
		this.createDate = createDate;
	}

	public Date getRunningDate() {
		return runningDate;
	}

	public void setRunningDate(final Date runningDate) {
		this.runningDate = runningDate;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(final Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(final Date endDate) {
		this.endDate = endDate;
	}

	public String getRuleHandler() {
		return ruleHandler;
	}

	public void setRuleHandler(final String ruleHandler) {
		this.ruleHandler = ruleHandler;
	}

	private static final long serialVersionUID = -642924978376103383L;
}
