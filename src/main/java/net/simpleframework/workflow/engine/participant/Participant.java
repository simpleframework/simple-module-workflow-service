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

	private static final String SEP = "#";

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(userId).append(SEP);
		if (roleId != null) {
			sb.append(roleId);
		}
		sb.append(SEP);
		if (deptId != null) {
			sb.append(deptId);
		}
		return sb.toString();
	}

	public static Participant of(final String participant) {
		final String[] pArr = StringUtils.split(participant, SEP);
		if (pArr.length == 3) {
			return new Participant(ID.of(pArr[0]), ID.of(pArr[1]), ID.of(pArr[2]));
		}
		throw WorkflowException.of("Participant.of");
	}
}
