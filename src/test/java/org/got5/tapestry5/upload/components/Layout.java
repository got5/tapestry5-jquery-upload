//
// Copyright 2015 GOT5
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// 	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.got5.tapestry5.upload.components;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;

@Import(stack=InternalConstants.CORE_STACK_NAME)
public class Layout
{
    @Property
    private String title;

    @Inject
    @Symbol(SymbolConstants.TAPESTRY_VERSION)
    @Property
    private String tapestryVersion;
    
    @Inject
    private ComponentResources resources;

    @SetupRender
    void init()
    {
        this.title = resources.getPageName();
    }
}
