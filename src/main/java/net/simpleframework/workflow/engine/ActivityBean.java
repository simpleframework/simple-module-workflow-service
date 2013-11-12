package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.Properties;

import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ActivityBean extends AbstractWorkflowBean {
	private ID processId;

	private ID previousId;

	private String tasknodeId;

	private short tasknodeType;

	private EActivityStatus status;

	private Date completeDate;

	private Properties properties;

	public ID getProcessId() {
		return processId;
	}

	public void setProcessId(final ID processId) {
		this.processId = processId;
	}

	public ID getPreviousId() {
		return previousId;
	}

	public void setPreviousId(final ID previousId) {
		this.previousId = previousId;
	}

	public String getTasknodeId() {
		return tasknodeId;
	}

	public void setTasknodeId(final String tasknodeId) {
		this.tasknodeId = tasknodeId.trim();
	}

	public short getTasknodeType() {
		return tasknodeType;
	}

	public void setTasknodeType(final short tasknodeType) {
		this.tasknodeType = tasknodeType;
	}

	public EActivityStatus getStatus() {
		return status != null ? status : EActivityStatus.running;
	}

	public void setStatus(final EActivityStatus status) {
		this.status = status;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(final Date completeDate) {
		this.completeDate = completeDate;
	}

	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
		}
		return properties;
	}

	public void setProperties(final Properties properties) {
		this.properties = properties;
	}

	// private AbstractTaskNode taskNode;
	//
	// public AbstractTaskNode taskNode() {
	// if (taskNode == null) {
	// // taskNode = (AbstractTaskNode)
	// // process().processNode().getNodeById(getTasknodeId());
	// }
	// assert taskNode != null;
	// return taskNode;
	// }

	public static final String activityId = "__activity_Id";

	private static final long serialVersionUID = 5146309554672912773L;
}
