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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.annotations.Scope;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.LibraryMapping;;

public class JQueryUploadModule {

    private static final AtomicBoolean needToAddShutdownListener = new AtomicBoolean(true);

    public static void contributeComponentClassResolver(final Configuration<LibraryMapping> configuration) {
        configuration.add(new LibraryMapping("upload", "org.got5.tapestry5.jquery"));
    }

    public static void contributeClasspathAssetAliasManager(final MappedConfiguration<String, String> configuration) {
        configuration.add("tapestry-jquery-upload", "META-INF/modules/tjq-upload");
    }

    public static FileCleaningTracker buildFileCleaningTracker() {
        return new FileCleaningTracker();
    }

    @Scope(ScopeConstants.PERTHREAD)
    public static FineUploaderDecoder buildFineUploaderDecoder(final PerthreadManager perthreadManager, final RegistryShutdownHub shutdownHub,
            @Autobuild final MultipartDecoderImpl multipartDecoder, final FileCleaningTracker fileCleaningTracker) {

        // This is probably overkill since the FileCleaner should catch temporary files, but lets be safe.
        perthreadManager.addThreadCleanupCallback(multipartDecoder);

        if (needToAddShutdownListener.getAndSet(false)) {
            shutdownHub.addRegistryShutdownListener(new Runnable() {
                @Override
                public void run() {
                    fileCleaningTracker.exitWhenFinished();
                }
            });
        }

        return multipartDecoder;
    }

    public static void contributeHttpServletRequestHandler(final OrderedConfiguration<HttpServletRequestFilter> configuration,
            final FineUploaderDecoder multipartDecoder) {
        /*
         * Adding the FineUploader filter before Tapestry’s ensures that both upload components can live in the same project.
         */
        configuration.add("FineUploaderFilter", new MultipartServletRequestFilter(multipartDecoder), "before:MultipartFilter");
    }

    public static void contributeFactoryDefaults(final MappedConfiguration<String, String> configuration) {

        configuration.add(UploadSymbols.ASSETS_ROOT, "classpath:/META-INF/assets/tjq-upload");
    }
}
