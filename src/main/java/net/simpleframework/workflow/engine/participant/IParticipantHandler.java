package net.simpleframework.workflow.engine.participant;

import java.util.Collection;
import java.util.Map;

import net.simpleframework.ctx.script.IScriptEval;
import net.simpleframework.workflow.IWorkflowHandler;
import net.simpleframework.workflow.engine.IWorkflowContextAware;
import net.simpleframework.workflow.schema.AbstractParticipantType;
import net.simpleframework.workflow.schema.TransitionNode;
import net.simpleframework.workflow.schema.UserNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IParticipantHandler extends IWorkflowHandler, IWorkflowContextAware {

	/**
	 * 定义交互环节的参与者
	 * 
	 * @param script
	 * @param variables
	 * @return
	 */
	Collection<Participant> getParticipants(IScriptEval script, Map<String, Object> variables);

	public abstract static class AbstractParticipantHandler implements IParticipantHandler {

		protected AbstractParticipantType getParticipantType(final Map<String, Object> variables) {
			return ((UserNode) ((TransitionNode) variables.get("transition")).to())
					.getParticipantType();
		}
	}
}
