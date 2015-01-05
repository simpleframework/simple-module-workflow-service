package net.simpleframework.workflow.engine.participant;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.workflow.WorkflowException;
import net.simpleframework.workflow.engine.IWorkflowServiceAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class Participant implements IWorkflowServiceAware {
	public ID userId;
	public ID roleId;
	public ID deptId;

	public Participant(final ID userId, final ID roleId, final ID deptId) {
		this.userId = userId;
		this.roleId = roleId != null ? roleId : getUser().getRoleId();
		this.deptId = deptId != null ? deptId : getUser().getDept().getId();
	}

	public Participant(final ID userId) {
		this(userId, null, null);
	}

	private PermissionUser _user;

	private PermissionUser getUser() {
		if (_user == null) {
			_user = permission.getUser(userId);
		}
		return _user;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("userId=").append(userId).append(";roleId=");
		if (roleId != null) {
			sb.append(roleId);
		}
		sb.append(";deptId=");
		if (deptId != null) {
			sb.append(deptId);
		}
		return sb.toString();
	}

	public static Participant of(final String participant) {
		final String[] pArr = StringUtils.split(participant, ";");
		if (pArr.length == 3) {
			final String a1 = StringUtils.split(pArr[0], "=")[1];
			final String a2 = StringUtils.split(pArr[1], "=")[1];
			final String a3 = StringUtils.split(pArr[2], "=")[1];
			return new Participant(ID.of(a1), ID.of(a2), ID.of(a3));
		}
		throw WorkflowException.of("Participant.of");
	}
}
