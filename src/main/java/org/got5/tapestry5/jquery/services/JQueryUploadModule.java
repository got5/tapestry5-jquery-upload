//
// Copyright 2015 GOT5 (GO Tapestry 5)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.got5.tapestry5.jquery.services;


import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.LibraryMapping;;



public class JQueryUploadModule {

	public static void contributeComponentClassResolver(
			Configuration<LibraryMapping> configuration) {
		configuration.add(new LibraryMapping("jquery-upload",
				"org.got5.tapestry5.jquery"));
	}

	

	public static void contributeClasspathAssetAliasManager(
			MappedConfiguration<String, String> configuration) {
		configuration.add("tapestry-jquery-upload", "META-INF/modules/tjq/upload");
	}

	

	public static void bind(ServiceBinder binder) {
		binder.bind(AjaxUploadDecoder.class, AjaxUploadDecoderImpl.class)
				.scope(ScopeConstants.PERTHREAD);
	}

	


	public static void contributeHttpServletRequestHandler(
			final OrderedConfiguration<HttpServletRequestFilter> configuration,
			final AjaxUploadDecoder ajaxUploadDecoder) {

		configuration.add("AjaxUploadFilter",
				new AjaxUploadServletRequestFilter(ajaxUploadDecoder),
				"after:IgnoredPaths");
	}

	
}
