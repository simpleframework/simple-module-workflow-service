package net.simpleframework.workflow.engine.participant;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class Participant implements IWorkflowContextAware {
	public ID userId;

	public ID roleId;

	public Participant(final ID userId, final ID roleId) {
		this.userId = userId;
		this.roleId = roleId != null ? roleId : permission.getUser(userId).getRoleId();
	}

	public Participant(final ID userId) {
		this(userId, null);
	}

	public String getId() {
		return roleId + "_" + userId;
	}

	@Override
	public String toString() {
		return userId + "," + roleId;
	}

	public static Participant of(final String participant) {
		final String[] pArr = StringUtils.split(participant, ",");
		return pArr.length == 2 ? new Participant(ID.of(pArr[0]), ID.of(pArr[1])) : null;
	}
}
