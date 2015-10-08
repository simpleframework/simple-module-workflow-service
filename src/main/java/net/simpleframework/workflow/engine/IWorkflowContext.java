package net.simpleframework.workflow.engine;

import net.simpleframework.ctx.IModuleContext;
import net.simpleframework.workflow.engine.comment.IWfCommentLogService;
import net.simpleframework.workflow.engine.comment.IWfCommentService;
import net.simpleframework.workflow.engine.comment.IWfCommentUserService;
import net.simpleframework.workflow.engine.notice.IWfNoticeService;
import net.simpleframework.workflow.engine.participant.IWorkflowPermissionHandler;
import net.simpleframework.workflow.engine.remote.IProcessRemoteHandler;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IWorkflowContext extends IModuleContext {

	static final String MODULE_NAME = "simple-workflow";

	/**
	 * 获取脚本执行时的缺省导入包
	 * 
	 * @return
	 */
	Package[] getScriptImportPackages();

	/**
	 * 模型服务
	 * 
	 * @return
	 */
	IProcessModelService getProcessModelService();

	IProcessModelDomainRService getProcessModelDomainRService();

	/**
	 * 流程实例服务
	 * 
	 * @return
	 */
	IProcessService getProcessService();

	/**
	 * 任务环节服务
	 * 
	 * @return
	 */
	IActivityService getActivityService();

	/**
	 * 工作列表服务
	 * 
	 * @return
	 */
	IWorkitemService getWorkitemService();

	/**
	 * 得到委托服务
	 * 
	 * @return
	 */
	IDelegationService getDelegationService();

	IWorkviewService getWorkviewService();

	IUserStatService getUserStatService();

	/**
	 * 获取评论服务
	 * 
	 * @return
	 */
	IWfCommentService getCommentService();

	IWfCommentUserService getCommentUserService();

	IWfCommentLogService getCommentLogService();

	/**
	 * 获取消息通知服务接口
	 * 
	 * @return
	 */
	IWfNoticeService getNoticeService();

	/**
	 * 参与者模型接口
	 * 
	 * @return
	 */
	IWorkflowPermissionHandler getPermissionHandler();

	/**
	 * 远程服务
	 * 
	 * @return
	 */
	IProcessRemoteHandler getProcessRemoteHandler();
}
