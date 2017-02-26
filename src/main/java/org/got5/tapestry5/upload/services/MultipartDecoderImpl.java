// Copyright 2007, 2008, 2010, 2011 The Apache Software Foundation
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

package org.got5.tapestry5.upload.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.IOUtils;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.annotations.Scope;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.CollectionFactory;
import org.apache.tapestry5.upload.internal.services.ParametersServletRequestWrapper;
import org.apache.tapestry5.upload.internal.services.UploadedFileItem;
import org.apache.tapestry5.upload.services.UploadSymbols;
import org.slf4j.Logger;

/**
 * Implementation of multipart decoder for servlets. This implementation is perthread scope.
 */
@Scope(ScopeConstants.PERTHREAD)
public class MultipartDecoderImpl implements FineUploaderDecoder, Runnable {

    private static String CONTENT_LENGTH = "Content-Length";

    private final Logger log;

    private final Map<String, UploadedFileItem> uploads = CollectionFactory.newMap();

    private final FileItemFactory fileItemFactory;

    private final long maxRequestSize;

    private final long maxFileSize;

    private final String requestEncoding;

    private FileUploadException uploadException;

    private final File uploadDir;

    private final FileCleaningTracker fileCleaningTracker;

    private File uploadedFile;

    private boolean multiPart;

    private String uuid;

    private boolean delete;

    private boolean done;

    public MultipartDecoderImpl(final Logger log,
            final FileCleaningTracker fileCleaningTracker,
            final FileItemFactory fileItemFactory,
            @Symbol(UploadSymbols.REPOSITORY_LOCATION) final File uploadDir,
            @Symbol(UploadSymbols.REQUESTSIZE_MAX) final long maxRequestSize,
            @Symbol(UploadSymbols.FILESIZE_MAX) final long maxFileSize,
            @Symbol(SymbolConstants.CHARSET) final String requestEncoding) {
        this.log = log;
        this.fileCleaningTracker = fileCleaningTracker;
        this.uploadDir = uploadDir;
        this.fileItemFactory = fileItemFactory;
        this.maxRequestSize = maxRequestSize;
        this.maxFileSize = maxFileSize;
        this.requestEncoding = requestEncoding;
    }

    @Override
    public File getFileUpload() {
        return uploadedFile;
    }

    @Override
    public boolean isMultiPart() {

        return multiPart;
    }

    @Override
    public String getUuid() {

        return uuid;
    }

    @Override
    public boolean isDelete() {

        return delete;
    }

    @Override
    public boolean isDone() {

        return done;
    }

    @Override
    public HttpServletRequest decode(final HttpServletRequest req) {

        final RequestParser requestParser;

        try {

            if (req.getParameter("done") != null) {
                requestParser = new RequestParser(req);
                this.uuid = requestParser.getUuid();
                final File dir = new File(uploadDir, uuid);
                final File[] parts = getPartitionFiles(dir, uuid);
                final File outputFile = new File(dir, requestParser.getFilename());
                for (final File part : parts) {
                    mergeFiles(outputFile, part);
                }

                assertCombinedFileIsValid(requestParser.getTotalFileSize(), outputFile, uuid);
                deletePartitionFiles(dir, uuid);

                this.uploadedFile = outputFile;
                this.multiPart = false;
                this.done = true;

            } else if (ServletFileUpload.isMultipartContent(req)) {
                final MultipartUploadParser multipartUploadParser = new MultipartUploadParser(req, uploadDir, fileCleaningTracker);
                requestParser = new RequestParser(req, multipartUploadParser);
                final File file = writeFileForMultipartRequest(requestParser);

                this.uploadedFile = file;
                this.multiPart = true;

            } else {
                requestParser = new RequestParser(req);
                this.uuid = requestParser.getUuid();

                // handle POST delete file request
                if (requestParser.getMethod() != null && requestParser.getMethod().equalsIgnoreCase("DELETE")) {
                    delete = true;
                } else {
                    final File file = writeFileForNonMultipartRequest(req, requestParser);

                    this.uploadedFile = file;
                    this.multiPart = false;
                }
            }
        } catch (final Exception e) {
            log.error("Problem handling upload request", e);
            throw new RuntimeException(e);
        }

        return req;
    }

    @Override
    public void run() {
        for (final UploadedFileItem uploaded : uploads.values()) {
            uploaded.cleanup();
        }
    }

    protected List<FileItem> parseRequest(final HttpServletRequest request) {
        try {
            return createFileUpload().parseRequest(request);
        } catch (final FileUploadException ex) {
            uploadException = ex;

            return Collections.emptyList();
        }
    }

    protected ServletFileUpload createFileUpload() {
        final ServletFileUpload upload = new ServletFileUpload(fileItemFactory);

        // set maximum file upload size
        upload.setSizeMax(maxRequestSize);
        upload.setFileSizeMax(maxFileSize);

        return upload;
    }

    protected HttpServletRequest processFileItems(final HttpServletRequest request, final List<FileItem> fileItems) {
        if (uploadException == null && fileItems.isEmpty()) {
            return request;
        }

        final ParametersServletRequestWrapper wrapper = new ParametersServletRequestWrapper(request);

        // First add parameters from the request
        for (final Object e : request.getParameterMap().entrySet()) {
            @SuppressWarnings("unchecked")
            final Map.Entry<String, String[]> ee = (Map.Entry<String, String[]>) e;
            for (final String s : ee.getValue()) {
                wrapper.addParameter(ee.getKey(), s);
            }
        }

        for (final FileItem item : fileItems) {
            if (item.isFormField()) {
                final String fieldValue;

                try {
                    fieldValue = item.getString(requestEncoding);
                } catch (final UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }

                wrapper.addParameter(item.getFieldName(), fieldValue);
            } else {
                wrapper.addParameter(item.getFieldName(), item.getName());
                addUploadedFile(item.getFieldName(), new UploadedFileItem(item));
            }
        }

        return wrapper;
    }

    protected void addUploadedFile(final String name, final UploadedFileItem file) {
        uploads.put(name, file);
    }

    @Override
    public FileUploadException getUploadException() {
        return uploadException;
    }

    private File writeFileForNonMultipartRequest(final HttpServletRequest req, final RequestParser requestParser) throws Exception {
        final File dir = new File(uploadDir, requestParser.getUuid());
        dir.mkdirs();

        final String contentLengthHeader = req.getHeader(CONTENT_LENGTH);
        final long expectedFileSize = Long.parseLong(contentLengthHeader);

        if (requestParser.getPartIndex() >= 0) {
            writeFile(req.getInputStream(), new File(dir, requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex())), null);
        } else {
            final File file = new File(dir, requestParser.getFilename());
            writeFile(req.getInputStream(), file, expectedFileSize);
            return file;
        }

        return null;
    }

    private File writeFileForMultipartRequest(final RequestParser requestParser) throws Exception {
        final File dir = new File(uploadDir, requestParser.getUuid());
        dir.mkdirs();

        if (requestParser.getPartIndex() >= 0) {
            writeFile(requestParser.getUploadItem().getInputStream(),
                    new File(dir, requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex())), null);
        } else {
            final File file = new File(dir, requestParser.getFilename());
            writeFile(requestParser.getUploadItem().getInputStream(), file, null);
            return file;
        }

        return null;
    }

    private void assertCombinedFileIsValid(final long totalFileSize, final File outputFile, final String uuid) throws MergePartsException {
        if (totalFileSize != outputFile.length()) {
            deletePartitionFiles(uploadDir, uuid);
            outputFile.delete();
            throw new MergePartsException("Incorrect combined file size!");
        }
    }

    private static class PartitionFilesFilter implements FilenameFilter {
        private String filename;

        PartitionFilesFilter(final String filename) {
            this.filename = filename;
        }

        @Override
        public boolean accept(final File file, final String s) {
            return s.matches(Pattern.quote(filename) + "_\\d+");
        }
    }

    private static File[] getPartitionFiles(final File directory, final String filename) {
        final File[] files = directory.listFiles(new PartitionFilesFilter(filename));
        Arrays.sort(files);
        return files;
    }

    private static void deletePartitionFiles(final File directory, final String filename) {
        final File[] partFiles = getPartitionFiles(directory, filename);
        for (final File partFile : partFiles) {
            partFile.delete();
        }
    }

    private File mergeFiles(final File outputFile, final File partFile) throws IOException {
        final FileOutputStream fos = new FileOutputStream(outputFile, true);

        try {
            final FileInputStream fis = new FileInputStream(partFile);

            try {
                IOUtils.copy(fis, fos);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        } finally {
            IOUtils.closeQuietly(fos);
        }

        return outputFile;
    }

    private File writeFile(final InputStream in, final File out, final Long expectedFileSize) throws IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(out);

            IOUtils.copy(in, fos);

            if (expectedFileSize != null) {
                final Long bytesWrittenToDisk = out.length();
                if (!expectedFileSize.equals(bytesWrittenToDisk)) {
                    log.warn("Expected file {} to be {} bytes; file on disk is {} bytes", new Object[] { out.getAbsolutePath(), expectedFileSize, 1 });
                    out.delete();
                    throw new IOException(
                            String.format("Unexpected file size mismatch. Actual bytes %s. Expected bytes %s.", bytesWrittenToDisk, expectedFileSize));
                }
            }

            return out;
        } catch (final Exception e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private class MergePartsException extends Exception {

        private static final long serialVersionUID = 5647243936852170818L;

        MergePartsException(final String message) {
            super(message);
        }
    }
}
