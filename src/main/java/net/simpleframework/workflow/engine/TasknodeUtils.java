package net.simpleframework.workflow.engine;

import net.simpleframework.common.Convert;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.AbstractTaskNode;
import net.simpleframework.workflow.schema.UserNode;
import net.simpleframework.workflow.schema.UserNode.Role;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class TasknodeUtils {

	public static boolean isSequential(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode
				? ((UserNode) taskNode).getParticipantType() : null;
		return pt instanceof Role && ((Role) pt).isSequential();
	}

	public static boolean isInstanceShared(final AbstractTaskNode taskNode) {
		final AbstractParticipantType pt = taskNode instanceof UserNode
				? ((UserNode) taskNode).getParticipantType() : null;
		return pt instanceof Role ? ((Role) pt).isInstanceShared() : true;
	}

	public static int getResponseValue(final AbstractTaskNode taskNode, final int max) {
		final AbstractParticipantType pt = taskNode instanceof UserNode
				? ((UserNode) taskNode).getParticipantType() : null;
		int rv = 0;
		if (pt instanceof Role) {
			final Role r = (Role) pt;
			if (r.isSequential()) {
				// 顺序模式时，响应数无效
				rv = max;
			} else {
				final double f = Convert.toDouble(r.getResponseValue(), 0);
				if (f >= 1d) {
					rv = (int) f;
				} else if (f > 0d) {
					rv = (int) (max * f);
					if (rv == 0) {
						rv = 1;
					}
				}
			}
		}
		if (rv <= 0 || rv > max) {
			rv = max;
		}
		return rv;
	}
}
