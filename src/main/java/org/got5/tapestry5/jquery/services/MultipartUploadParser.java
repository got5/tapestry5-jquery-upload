package org.got5.tapestry5.jquery.services;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipartUploadParser {
    final Logger log = LoggerFactory.getLogger(MultipartUploadParser.class);

    private Map<String, String> params = new HashMap<String, String>();

    private List<FileItem> files = new ArrayList<FileItem>();

    // fileItemsFactory is a field (even though it's scoped to the constructor) to prevent the
    // org.apache.commons.fileupload.servlet.FileCleanerCleanup thread from attempting to delete the
    // temp file before while it is still being used.
    //
    // FileCleanerCleanup uses a java.lang.ref.ReferenceQueue to delete the temp file when the FileItemsFactory marker object is GCed
    private DiskFileItemFactory fileItemsFactory;

    public MultipartUploadParser(final HttpServletRequest request, final File repository, final FileCleaningTracker tracker) throws Exception {
        if (!repository.exists() && !repository.mkdirs()) {
            throw new IOException("Unable to mkdirs to " + repository.getAbsolutePath());
        }

        fileItemsFactory = setupFileItemFactory(repository, tracker);

        final ServletFileUpload upload = new ServletFileUpload(fileItemsFactory);
        final List<FileItem> formFileItems = upload.parseRequest(request);

        parseFormFields(formFileItems);

        if (files.isEmpty()) {
            log.warn("No files were found when processing the requst. Debugging info follows.");

            writeDebugInfo(request);

            throw new FileUploadException("No files were found when processing the requst.");
        }

        if (log.isDebugEnabled()) {
            writeDebugInfo(request);
        }
    }

    private DiskFileItemFactory setupFileItemFactory(final File repository, final FileCleaningTracker tracker) {
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        factory.setRepository(repository);

        factory.setFileCleaningTracker(tracker);

        return factory;
    }

    private void writeDebugInfo(final HttpServletRequest request) {

        log.debug("-- POST HEADERS --");

        @SuppressWarnings("unchecked")
        final List<Object> headerNames = Collections.<Object>list(request.getHeaderNames());
        for (final Object header : headerNames) {
            log.debug("{}: {}", header, request.getHeader(header.toString()));
        }

        log.debug("-- POST PARAMS --");
        for (final String key : params.keySet()) {
            log.debug("{}: {}", key, params.get(key));
        }
    }

    private void parseFormFields(final List<FileItem> items) throws UnsupportedEncodingException {
        for (final FileItem item : items) {
            if (item.isFormField()) {
                final String key = item.getFieldName();
                final String value = item.getString("UTF-8");
                if (StringUtils.isNotBlank(key)) {
                    params.put(key, StringUtils.defaultString(value));
                }
            } else {
                files.add(item);
            }
        }
    }

    public Map<String, String> getParams() {
        return params;
    }

    public List<FileItem> getFiles() {
        if (files.isEmpty()) {
            throw new RuntimeException("No FileItems exist.");
        }

        return files;
    }

    public FileItem getFirstFile() {
        if (files.isEmpty()) {
            throw new RuntimeException("No FileItems exist.");
        }

        return files.iterator().next();
    }
}
