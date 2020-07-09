/*
 Copyright 2020 Adobe. All rights reserved.

 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.

----
 XDM Java Enum Generated 2020-06-17 16:58:12.488018 -0700 PDT m=+9.740071377 by XDMTool
----
*/
package com.adobe.marketing.mobile.xdm;

@SuppressWarnings("unused")
public enum AuthenticatedStateEnum {
	AMBIGUOUS("ambiguous"), // Ambiguous
	AUTHENTICATED("authenticated"), // User identified by a login or similar action that was valid at the time of the event observation.
	LOGGEDOUT("loggedOut"); // User was identified by a login action at some point of time previously, but is not currently logged in.
	
	private final String value;

	AuthenticatedStateEnum(final String enumValue) {
		this.value = enumValue;
	}

	public String ToString() {
		return value;
	}
}
