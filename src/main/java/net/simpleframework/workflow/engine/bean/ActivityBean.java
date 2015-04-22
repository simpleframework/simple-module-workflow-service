package net.simpleframework.workflow.engine.bean;

import java.util.Date;
import java.util.Properties;

import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.common.ID;
import net.simpleframework.workflow.engine.EActivityStatus;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
@EntityInterceptor(listenerTypes = { "net.simpleframework.module.log.EntityUpdateLogAdapter",
		"net.simpleframework.module.log.EntityDeleteLogAdapter" }, columns = { "status" })
public class ActivityBean extends AbstractWorkflowBean {

	/* 流程实例id */
	private ID processId;

	/* 前一环节id */
	private ID previousId;

	private String tasknodeId;

	/* 任务显示文本 */
	private String tasknodeText;

	private short tasknodeType;

	@ColumnMeta(columnText = "#(AbstractWorkflowBean.0)")
	private EActivityStatus status;

	/* 完成时间 */
	private Date completeDate;

	/* 过期时间 */
	private Date timeoutDate;

	/* 运行期属性 */
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

	public String getTasknodeText() {
		return tasknodeText;
	}

	public void setTasknodeText(final String tasknodeText) {
		this.tasknodeText = tasknodeText;
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

	public Date getTimeoutDate() {
		return timeoutDate;
	}

	public void setTimeoutDate(final Date timeoutDate) {
		this.timeoutDate = timeoutDate;
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

	@Override
	public String toString() {
		return getTasknodeText();
	}

	private static final long serialVersionUID = 5146309554672912773L;
}
