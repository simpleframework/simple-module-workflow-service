package net.simpleframework.workflow.engine;

import static net.simpleframework.common.I18n.$m;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public enum EActivityStatus {

	running {

		@Override
		public String toString() {
			return $m("EActivityStatus.running");
		}
	},

	suspended {

		@Override
		public String toString() {
			return $m("EActivityStatus.suspended");
		}
	},

	timeout {

		@Override
		public String toString() {
			return $m("EActivityStatus.timeout");
		}
	},

	waiting {

		@Override
		public String toString() {
			return $m("EActivityStatus.waiting");
		}
	},

	complete {

		@Override
		public String toString() {
			return $m("EActivityStatus.complete");
		}
	},

	abort {

		@Override
		public String toString() {
			return $m("EActivityStatus.abort");
		}
	},

	fallback {
		@Override
		public String toString() {
			return $m("EActivityStatus.fallback");
		}
	}
}
