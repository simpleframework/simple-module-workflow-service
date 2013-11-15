package net.simpleframework.workflow.engine.participant;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.simpleframework.common.ID;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.permission.IPermissionHandler;
import net.simpleframework.workflow.engine.EDelegationSource;
import net.simpleframework.workflow.engine.ProcessModelBean;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IParticipantModel extends IPermissionHandler {
	/**
	 * 获取相对参与者
	 * 
	 * @param user
	 * @param role
	 * @param relative
	 * @param variables
	 * @return
	 */
	Collection<Participant> getRelativeParticipants(Object user, Object role, String relative,
			KVMap variables);

	/**
	 * 在设置委托时，返回定义的用户列表，默认实现为当前用户所在部门的所有用户
	 * 
	 * @param processModel
	 * @param source
	 * @param filterMap
	 * @return
	 */
	Iterator<ID> getUsersOfDelegation(ProcessModelBean processModel, EDelegationSource source,
			Map<String, String> filterMap);
}
