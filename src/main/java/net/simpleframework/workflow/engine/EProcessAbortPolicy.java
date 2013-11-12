package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EProcessAbortPolicy {

	/**
	 * 仅放弃当前流程 此时，活动的环节依旧可以完成当前工作，但不再创建后续环节
	 */
	normal {
		@Override
		public String toString() {
			return $m("EProcessAbortPolicy.normal");
		}
	},

	/**
	 * 同时放弃活动的环节
	 */
	allActivities {
		@Override
		public String toString() {
			return $m("EProcessAbortPolicy.allActivities");
		}
	}
}
