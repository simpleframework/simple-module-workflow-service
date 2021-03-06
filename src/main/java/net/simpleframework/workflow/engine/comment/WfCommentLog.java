package net.simpleframework.workflow.engine.comment;

import net.simpleframework.ado.bean.AbstractUserAwareBean;
import net.simpleframework.ado.bean.IOrderBeanAware;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityDeleteLogAdapter" })
public class WfCommentLog extends AbstractUserAwareBean implements IOrderBeanAware {
	/* 意见id */
	private ID commentId;
	/* 内容 */
	private String ccomment;
	/* 类型 */
	private ELogType logType;

	/* 排序字段 */
	private int oorder;

	public ID getCommentId() {
		return commentId;
	}

	public void setCommentId(final ID commentId) {
		this.commentId = commentId;
	}

	public String getCcomment() {
		return ccomment;
	}

	public void setCcomment(final String ccomment) {
		this.ccomment = ccomment;
	}

	public ELogType getLogType() {
		return logType == null ? ELogType.history : logType;
	}

	public void setLogType(final ELogType logType) {
		this.logType = logType;
	}

	@Override
	public int getOorder() {
		return oorder;
	}

	@Override
	public void setOorder(final int oorder) {
		this.oorder = oorder;
	}

	public static enum ELogType {
		/* 历史意见 */
		history,
		/* 收藏意见 */
		collection
	}

	private static final long serialVersionUID = 8497294888344551942L;
}
