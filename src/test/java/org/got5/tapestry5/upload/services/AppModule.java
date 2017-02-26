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
package org.got5.tapestry5.upload.services;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.services.ApplicationDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.got5.tapestry5.upload.services.AjaxUploadModule;

@ImportModule(value = AjaxUploadModule.class)
public class AppModule {
    @Contribute(SymbolProvider.class)
    @ApplicationDefaults
    public static void contributeApplicationDefaults(final MappedConfiguration<String, Object> configuration) {
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en,fr,de,ru,ua");

        configuration.add(SymbolConstants.PRODUCTION_MODE, false);

        configuration.add(SymbolConstants.COMBINE_SCRIPTS, false);

        configuration.add(SymbolConstants.COMPRESS_WHITESPACE, false);

        configuration.add(SymbolConstants.GZIP_COMPRESSION_ENABLED, false);

        configuration.add(SymbolConstants.ASSET_URL_FULL_QUALIFIED, true);

        configuration.add(SymbolConstants.ASSET_PATH_PREFIX, "assets");
        configuration.add("demo-src-dir", "");

    }

    public static void contributeClasspathAssetAliasManager(final MappedConfiguration<String, String> configuration) {
        configuration.add("demo-jquery", "static/css");
    }

}
