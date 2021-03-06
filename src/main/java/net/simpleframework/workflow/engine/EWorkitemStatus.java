package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public enum EWorkitemStatus {
	running {
		@Override
		public String toString() {
			return $m("EWorkitemStatus.running");
		}
	},
	suspended {
		@Override
		public String toString() {
			return $m("EWorkitemStatus.suspended");
		}
	},
	delegate {
		@Override
		public String toString() {
			return $m("EWorkitemStatus.delegate");
		}
	},
	complete {
		@Override
		public String toString() {
			return $m("EWorkitemStatus.complete");
		}
	},
	abort {
		@Override
		public String toString() {
			return $m("EWorkitemStatus.abort");
		}
	}
}
