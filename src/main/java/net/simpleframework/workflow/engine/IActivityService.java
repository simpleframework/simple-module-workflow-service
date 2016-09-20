package net.simpleframework.workflow.engine;

import java.util.Date;
import java.util.List;

import net.simpleframework.workflow.engine.bean.ActivityBean;
import net.simpleframework.workflow.engine.bean.ProcessBean;
import net.simpleframework.workflow.engine.event.IWorkCalendarListener;
import net.simpleframework.workflow.engine.participant.Participant;
import net.simpleframework.workflow.schema.AbstractTaskNode;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IActivityService
		extends IWorkflowVariableAware<ActivityBean>, IWorkflowService<ActivityBean>,
		IEventListenerAware<ActivityBean>, IWorkflowScriptAware<ActivityBean> {

	/**
	 * 获取流程实例对象
	 * 
	 * @param activity
	 * @return
	 */
	ProcessBean getProcessBean(ActivityBean activity);

	/**
	 * 获取当前环节的节点描述
	 * 
	 * @param activity
	 * @return
	 */
	AbstractTaskNode getTaskNode(ActivityBean activity);

	/**
	 * 获取指定流程实例下的所有环节实例，默认按创建日期排序
	 * 
	 * @param processBean
	 * @return
	 */
	List<ActivityBean> getActivities(ProcessBean processBean, EActivityStatus... status);

	/**
	 * 获取指定taskNode的环节实例
	 * 
	 * @param processBean
	 * @param tasknodeId
	 * @return
	 */
	List<ActivityBean> getActivities(ProcessBean processBean, Object tasknodeId);

	/**
	 * 获取指定环节的后续环节实例
	 * 
	 * @param preActivity
	 * @return
	 */
	List<ActivityBean> getNextActivities(ActivityBean preActivity);

	/**
	 * 
	 * @param preActivity
	 * @return
	 */
	List<ActivityBean> getLastNextActivities(ActivityBean preActivity);

	/**
	 * 获取指定环节的所有后续环节实例
	 * 
	 * @param preActivity
	 * @return
	 */
	List<ActivityBean> getNextAllActivities(ActivityBean preActivity);

	/**
	 * 
	 * @param activity
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity);

	/**
	 * 获取前一指定环节，获取的环节必须在创建链路上
	 * 
	 * @param activity
	 * @param taskname
	 *        环节id或名称
	 * @return
	 */
	ActivityBean getPreActivity(ActivityBean activity, String taskname);

	/**
	 * 
	 * @param processBean
	 * @return
	 */
	ActivityBean getStartActivity(ProcessBean processBean);

	/**
	 * 
	 * @param activityCallback
	 */
	void doComplete(ActivityComplete activityCallback);

	/**
	 * 挂起当前环节
	 * 
	 * @param activity
	 * @param resume
	 */
	void doSuspend(ActivityBean activity);

	/**
	 * 恢复当前环节
	 * 
	 * @param activity
	 */
	void doResume(ActivityBean activity);

	/**
	 * 
	 * @param activity
	 * @param policy
	 *        放弃策略
	 */
	void doAbort(ActivityBean activity, EActivityAbortPolicy policy);

	void doAbort(ActivityBean activity);

	/**
	 * 跳转到指定的任务环节
	 * 
	 * 跳转和回退的区别：跳转可以到任何一个有效的任务环节，按照模型创建参与者；回退只能是已运行过的任务，按运行历史创建参与者
	 * 
	 * @param activity
	 * @param taskname
	 *        环节id或名称
	 * @param bComplete
	 *        是否完成当前环节
	 */
	void doJump(ActivityBean activity, String taskname, boolean bComplete);

	void doJump(ActivityBean activity, String taskname);

	/**
	 * 回退到指定的任务环节
	 * 
	 * @param activity
	 * @param taskname
	 *        环节id或名称
	 * @param isNextActivity
	 *        是否完成后直接回到直退环节
	 */
	void doFallback(ActivityBean activity, String taskname, boolean isNextActivity);

	void doFallback(ActivityBean activity);

	ActivityBean getFallbackNextActivity(ActivityBean activity);

	/**
	 * 完成子流程环节
	 * 
	 * @param activity
	 * @param mappingVal
	 */
	void doSubComplete(ActivityBean activity, IMappingVal mappingVal);

	/**
	 * 获取表单实例
	 * 
	 * @param activity
	 * @return
	 */
	IWorkflowForm getWorkflowForm(ActivityBean activity);

	/**
	 * 获取定义参与者
	 * 
	 * @param activity
	 * @param all
	 *        包含废止的
	 * @return
	 */
	List<Participant> getParticipants(ActivityBean activity, boolean all);

	/**
	 * 获取实际参与者
	 * 
	 * @param activity
	 * @return
	 */
	List<Participant> getParticipants2(ActivityBean activity);

	/**
	 * 更新环节的过期时间
	 * 
	 * @param activity
	 * @param timeoutDate
	 */
	void doUpdateTimeoutDate(ActivityBean activity, Date timeoutDate);

	void doUpdateTimeoutDate(ActivityBean activity, int hours);

	/**
	 * 获取空节点保存的参与者
	 * 
	 * @param activity
	 * @return
	 */
	List<Participant> getEmptyParticipants(ActivityBean activity);

	/**
	 * 获取工作日历接口
	 * 
	 * @param activity
	 * @return
	 */
	IWorkCalendarListener getWorkCalendarListener(ActivityBean activity);
}
