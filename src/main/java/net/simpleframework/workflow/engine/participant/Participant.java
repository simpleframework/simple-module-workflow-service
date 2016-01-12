package net.simpleframework.workflow.engine.participant;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.ctx.permission.PermissionUser;
import net.simpleframework.workflow.WorkflowException;
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
	public ID deptId;

	public Participant(final PermissionUser user, final ID roleId, final ID deptId) {
		this.userId = user.getId();
		this.roleId = roleId != null ? roleId : user.getRole().getId();
		this.deptId = deptId != null ? deptId : user.getDeptId();
	}

	public Participant(final ID userId, final ID roleId, final ID deptId) {
		this(permission.getUser(userId), roleId, deptId);
	}

	public Participant(final PermissionUser user) {
		this(user, null, null);
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
