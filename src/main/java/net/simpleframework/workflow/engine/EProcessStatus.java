package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public enum EProcessStatus {
	running {

		@Override
		public String toString() {
			return $m("EProcessStatus.running");
		}
	},

	complete {

		@Override
		public String toString() {
			return $m("EProcessStatus.complete");
		}
	},

	suspended {

		@Override
		public String toString() {
			return $m("EProcessStatus.suspended");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EProcessStatus.abort");
		}
	},

	timeout {

		@Override
		public String toString() {
			return $m("EProcessStatus.timeout");
		}
	}
}
