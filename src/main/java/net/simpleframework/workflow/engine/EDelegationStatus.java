package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EDelegationStatus {
	/**
	 * 开始状态
	 */
	start {

		@Override
		public String toString() {
			return $m("EDelegationStatus.start");
		}
	},

	/**
	 * 运行
	 */
	running {

		@Override
		public String toString() {
			return $m("EDelegationStatus.running");
		}
	},

	/**
	 * 完成
	 */
	complete {

		@Override
		public String toString() {
			return $m("EDelegationStatus.complete");
		}
	},

	/**
	 * 放弃
	 */
	abort {

		@Override
		public String toString() {
			return $m("EDelegationStatus.abort");
		}
	}
}
