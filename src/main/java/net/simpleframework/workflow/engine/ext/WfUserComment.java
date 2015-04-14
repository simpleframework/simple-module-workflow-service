package net.simpleframework.workflow.engine.ext;

import net.simpleframework.ado.bean.AbstractUserAwareBean;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class WfUserComment extends AbstractUserAwareBean {
	/* 流程id */
	private ID contentId;
	/* 新到意见数 */
	private int comments;

	public ID getContentId() {
		return contentId;
	}

	public void setContentId(ID contentId) {
		this.contentId = contentId;
	}

	public int getComments() {
		return comments;
	}

	public void setComments(int comments) {
		this.comments = comments;
	}

	private static final long serialVersionUID = -4193189280207725892L;
}
