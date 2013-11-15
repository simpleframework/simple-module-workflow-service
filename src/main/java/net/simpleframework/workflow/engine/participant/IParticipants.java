package net.simpleframework.workflow.engine.participant;

import java.util.Collection;

import net.simpleframework.common.coll.KVMap;
import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.engine.IWorkflowContextAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IParticipants extends IWorkflowContextAware {

	/**
	 * 定义交互环节的参与者
	 * 
	 * @param script
	 * @param variables
	 * @return
	 */
	Collection<Participant> participants(IScriptEval script, KVMap variables);

	public abstract static class AbstractParticipants implements IParticipants {
	}
}
