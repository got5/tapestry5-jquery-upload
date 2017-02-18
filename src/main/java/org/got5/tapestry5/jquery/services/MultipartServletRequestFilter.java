// Copyright 2007, 2008 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.got5.tapestry5.jquery.services;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.HttpServletRequestHandler;

/**
 * Filter that decodes an incoming multipart request.
 */
public class MultipartServletRequestFilter implements HttpServletRequestFilter {

    private final FineUploaderDecoder decoder;

    public MultipartServletRequestFilter(FineUploaderDecoder multipartDecoder) {

        decoder = multipartDecoder;
    }

    @Override
    public boolean service(HttpServletRequest request, HttpServletResponse response, HttpServletRequestHandler handler) throws IOException {

        final boolean isAjaxUpload = "ajaxupload".equals(request.getParameter("component"));
        if (isAjaxUpload) {

            return handler.service(decoder.decode(request), response);
        }

        return handler.service(request, response);
    }

}
