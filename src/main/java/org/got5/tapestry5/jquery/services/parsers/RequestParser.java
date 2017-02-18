package org.got5.tapestry5.jquery.services.parsers;

import java.io.BufferedReader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.got5.tapestry5.jquery.services.MultipartUploadParser;

public class RequestParser {

    /**
     * The attribute of the input element which will contain the file name. For non-multipart-encoded upload requests, this will be included as a parameter in
     * the query string of the URI with a value equal to the file name.
     */
    private static String FILENAME_PARAM = "qqfile";

    /**
     * Name of the parameter passed with a chunked request that specifies the index of the associated partition.
     */
    private static String PART_INDEX_PARAM = "qqpartindex";

    /**
     * The name of the parameter passed that specifies the total file size in bytes.
     */
    private static String FILE_SIZE_PARAM = "qqtotalfilesize";

    /**
     * Name of the parameter passed with a chunked request that specifies the total number of chunks associated with the File or Blob.
     */
    private static String TOTAL_PARTS_PARAM = "qqtotalparts";

    /**
     * The name of the parameter the uniquely identifies each associated item. The value is a Level 4 UUID.
     */
    private static String UUID_PARAM = "qquuid";

    /**
     * The name of the parameter passed if the original filename has been edited or a Blob is being sent.
     */
    private static String PART_FILENAME_PARAM = "qqfilename";

    /**
     * Alternative HTTP method...
     */
    private static String METHOD_PARAM = "_method";

    private static String GENERATE_ERROR_PARAM = "generateError";

    private String filename;

    private FileItem uploadItem;

    private boolean generateError;

    private int partIndex = -1;

    private long totalFileSize;

    private int totalParts;

    private String uuid;

    private String originalFilename;

    private String method;

    private Map<String, String> customParams = new HashMap<>();

    private final HttpServletRequest request;

    private final MultipartUploadParser multipartUploadParser;

    public RequestParser(final HttpServletRequest request) throws Exception {

        this.request = request;
        this.multipartUploadParser = null;

        if (request.getMethod().equals("POST") && request.getContentType() == null) {
            parseXdrPostParams();
        }
        // The only request sent by Fine Uploader that has a form-URL encoded body is the "all chunks done" POST
        else if (request.getMethod().equals("POST") && request.getContentType().startsWith("application/x-www-form-urlencoded")) {
            parseAllChunksDoneRequestParams();
        } else {
            this.filename = request.getParameter(FILENAME_PARAM);
            parseQueryStringParams();
        }

        removeQqParams(this.customParams);
    }

    public RequestParser(final HttpServletRequest request, final MultipartUploadParser multipartUploadParser) throws Exception {

        this.request = request;
        this.multipartUploadParser = multipartUploadParser;

        this.uploadItem = multipartUploadParser.getFirstFile();
        this.filename = multipartUploadParser.getFirstFile().getName();

        // params could be in body or query string, depending on Fine Uploader request option properties
        parseRequestBodyParams();
        parseQueryStringParams();

        removeQqParams(this.customParams);
    }

    public String getFilename() {
        return originalFilename != null ? originalFilename : filename;
    }

    public FileItem getUploadItem() {
        return uploadItem;
    }

    public boolean generateError() {
        return generateError;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public int getTotalParts() {
        return totalParts;
    }

    public String getUuid() {
        return uuid;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    private void parseRequestBodyParams() throws Exception {
        if (multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM) != null) {
            this.generateError = Boolean.parseBoolean(multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM));
        }

        final String partNumStr = multipartUploadParser.getParams().get(PART_INDEX_PARAM);
        if (partNumStr != null) {
            this.partIndex = Integer.parseInt(partNumStr);

            this.totalFileSize = Long.parseLong(multipartUploadParser.getParams().get(FILE_SIZE_PARAM));
            this.totalParts = Integer.parseInt(multipartUploadParser.getParams().get(TOTAL_PARTS_PARAM));
        }

        for (final Map.Entry<String, String> paramEntry : multipartUploadParser.getParams().entrySet()) {
            this.customParams.put(paramEntry.getKey(), paramEntry.getValue());
        }

        if (this.uuid == null) {
            this.uuid = multipartUploadParser.getParams().get(UUID_PARAM);
        }

        if (this.originalFilename == null) {
            this.originalFilename = multipartUploadParser.getParams().get(PART_FILENAME_PARAM);
        }
    }

    private void parseAllChunksDoneRequestParams() {
        this.totalFileSize = Long.parseLong(request.getParameter(FILE_SIZE_PARAM));
        this.totalParts = Integer.parseInt(request.getParameter(TOTAL_PARTS_PARAM));
        this.uuid = request.getParameter(UUID_PARAM);
        this.originalFilename = request.getParameter(PART_FILENAME_PARAM);

        final Map<String, String[]> parameterMap = request.getParameterMap();
        for (final Map.Entry<String, String[]> paramEntry : parameterMap.entrySet()) {
            this.customParams.put(paramEntry.getKey(), paramEntry.getValue()[0]);
        }
    }

    private void parseQueryStringParams() {
        if (request.getParameter(GENERATE_ERROR_PARAM) != null) {
            this.generateError = Boolean.parseBoolean(request.getParameter(GENERATE_ERROR_PARAM));
        }

        final String partNumStr = request.getParameter(PART_INDEX_PARAM);
        if (partNumStr != null) {
            this.partIndex = Integer.parseInt(partNumStr);
            this.totalFileSize = Integer.parseInt(request.getParameter(FILE_SIZE_PARAM));
            this.totalParts = Integer.parseInt(request.getParameter(TOTAL_PARTS_PARAM));
        }

        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            final String paramName = paramNames.nextElement();
            this.customParams.put(paramName, request.getParameter(paramName));
        }

        if (this.uuid == null) {
            this.uuid = request.getParameter(UUID_PARAM);
        }

        if (this.method == null) {
            this.method = request.getParameter(METHOD_PARAM);
        }

        if (this.originalFilename == null) {
            this.originalFilename = request.getParameter(PART_FILENAME_PARAM);
        }
    }

    private static void removeQqParams(final Map<String, String> customParams) {
        final Iterator<Map.Entry<String, String>> paramIterator = customParams.entrySet().iterator();

        while (paramIterator.hasNext()) {
            final Map.Entry<String, String> paramEntry = paramIterator.next();
            if (paramEntry.getKey().startsWith("qq")) {
                paramIterator.remove();
            }
        }
    }

    private void parseXdrPostParams() throws Exception {
        final String queryString = getQueryStringFromRequestBody();
        final String[] queryParams = queryString.split("&");

        for (final String queryParam : queryParams) {
            final String[] keyAndVal = queryParam.split("=");
            final String key = URLDecoder.decode(keyAndVal[0], "UTF-8");
            final String value = URLDecoder.decode(keyAndVal[1], "UTF-8");

            if (key.equals(UUID_PARAM)) {
                this.uuid = value;
            } else if (key.equals(METHOD_PARAM)) {
                this.method = value;
            } else {
                this.customParams.put(key, value);
            }
        }
    }

    private String getQueryStringFromRequestBody() throws Exception {
        final StringBuilder content = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = request.getReader();
            final char[] chars = new char[128];
            int bytesRead;
            while ((bytesRead = reader.read(chars)) != -1) {
                content.append(chars, 0, bytesRead);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return content.toString();
    }
}
