package net.simpleframework.workflow.engine.participant;

import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;
import net.simpleframework.ctx.permission.PermissionRole;
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

	private final PermissionUser user;

	/* 角色id */
	private ID roleId;
	/* 部门id */
	private ID deptId;

	public Participant(final PermissionUser user, final ID roleId, final ID deptId) {
		this.user = user;
		this.roleId = roleId;
		this.deptId = deptId;
	}

	public Participant(final ID userId, final ID roleId, final ID deptId) {
		this(permission.getUser(userId), roleId, deptId);
	}

	public Participant(final PermissionUser user) {
		this(user, null, null);
	}

	public PermissionUser getUser() {
		return user;
	}

	public ID getUserId() {
		return getUser().getId();
	}

	public ID getRoleId() {
		if (roleId == null) {
			// 如果没有指定角色, 则用用户的缺省角色
			roleId = getUser().getRole().getId();
		}
		return roleId;
	}

	public void setRoleId(final ID roleId) {
		this.roleId = roleId;
	}

	public ID getDeptId() {
		if (deptId == null) {
			// 获取当前用户user指定roleId的角色
			final PermissionRole role = permission.getRole(getRoleId()).setUser(getUser());
			deptId = role.getDept().getId();
		}
		if (deptId == null) {
			deptId = getUser().getDept().getId();
		}
		return deptId;
	}

	public void setDeptId(final ID deptId) {
		this.deptId = deptId;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getUserId()).append(SEP);
		final ID roleId = getRoleId();
		if (roleId != null) {
			sb.append(roleId);
		}
		sb.append(SEP);
		final ID deptId = getDeptId();
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

	private static final String SEP = "#";
}
