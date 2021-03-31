package org.got5.tapestry5.upload.components;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ClientElement;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.Events;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.PartialMarkupRenderer;
import org.apache.tapestry5.services.PartialMarkupRendererFilter;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.got5.tapestry5.upload.AjaxUploadEventConstants;
import org.got5.tapestry5.upload.AjaxUploadSymbolConstants;
import org.got5.tapestry5.upload.services.FineUploaderDecoder;

/**
 * File-Upload component based on Tapestry-Upload and https://github.com/FineUploader/fine-uploader
 *
 * <p>
 * The AjaxUpload will trigger the event {@link AjaxUploadEventConstants#UPLOAD} after a file has been uploaded to the temporary directory and
 * {@link AjaxUploadEventConstants#ALL_UPLOAD_COMPLETE} after all files are uploaded.
 * </p>
 *
 * @author criedel
 *
 * @tapestrydoc
 */
@Events({ AjaxUploadEventConstants.ALL_UPLOAD_COMPLETE, AjaxUploadEventConstants.UPLOAD })
@Import(stylesheet = AjaxUploadSymbolConstants.ASSETS_ROOT_VALUE + "/vendor/fineuploader/fine-uploader-new.min.css")
public class AjaxUpload implements ClientElement {

    private static final String[] UNITS = new String[] { "K", "M", "G" };

    /**
     * (optional, all files are allowed by default) Restrict allowed file extensions.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String allowedExtensions;

    /**
     * (optional, defaults to 0 = no limit)
     *
     * The maximum size of one single file in bytes. If the number has a trailing K, M or G the limit will be calculated accordingly.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL, value = "0")
    private String sizeLimit;

    /**
     * Additional parameters (please refer to FineUploader’s documentation)
     * http://docs.fineuploader.com/branch/master/api/options.html
     */
    @Parameter
    private JSONObject options;

    @Parameter
    private Object[] context;

    @Inject
    private JavaScriptSupport javaScriptSupport;

    @Inject
    private ComponentResources resources;

    @Inject
    private Request request;

    @Inject
    private Messages messages;

    @Inject
    private FineUploaderDecoder multipartDecoder;

    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;

    private String clientId;

    void beginRender(final MarkupWriter writer) {

        this.clientId = javaScriptSupport.allocateClientId(resources);

        writer.element("div", "id", getClientId());
    }

    void afterRender(final MarkupWriter writer) {

        writer.end();

        if (options == null) {
            options = new JSONObject();
        }

        final long sizeLimit = calculateSizeLimit();

        final JSONObject parameter = new JSONObject("id", getClientId())
            .put("url", resources.createEventLink("upload", context).addParameter("component", "ajaxupload").toURI())
            .put("allCompleteURL", resources.createEventLink("allComplete", context).toURI());

        final JSONObject options = new JSONObject();
        final JSONObject validation = new JSONObject()
                .put("sizeLimit", sizeLimit);

        if (allowedExtensions != null) {
            validation.put("allowedExtensions", new JSONArray(allowedExtensions));
        }
        options.put("validation", validation);

        merge(options, this.options);

        parameter.put("options", options);

        javaScriptSupport.require("tjq-upload/fineuploader").with(parameter);
    }

    /**
     * @return 0 if {@link #sizeLimit} is a non valid value or the correct size limit in bytes.
     */
    private long calculateSizeLimit() {

        if (sizeLimit.matches("^[\\d]+$")) {

            return Long.valueOf(this.sizeLimit);
        }

        double size = Double.valueOf(sizeLimit.substring(0, sizeLimit.length() - 1));
        int i;

        for (i = 0; i < UNITS.length; i++) {

            size = size * 1024;

            if (sizeLimit.endsWith(UNITS[i])) {

                return (long) size;
            }
        }

        return 0;
    }

    @OnEvent(value = "upload")
    Object onUpload(final EventContext ctx) {

        final File uploadedFile = multipartDecoder.getFileUpload();

        final Holder<Object> holder = Holder.create();

        final ComponentEventCallback<Object> callback = new ComponentEventCallback<Object>() {

            @Override
            public boolean handleResult(final Object result) {

                holder.put(result);

                return true;
            }
        };

        final boolean success = uploadedFile != null;

        final Object[] context = ArrayUtils.addAll(new Object[] { uploadedFile }, (Object[]) ctx.toStrings());
        this.resources.triggerEvent(AjaxUploadEventConstants.UPLOAD, context, callback);
        return processXHRResult(success, holder.get());
    }

    @OnEvent(value = "allComplete")
    Object onAllComplete(final EventContext ctx) {

        final Holder<Object> holder = Holder.create();
        final ComponentEventCallback<Object> callback = new ComponentEventCallback<Object>() {

            @Override
            public boolean handleResult(final Object result) {

                holder.put(result);

                return true;
            }
        };

        this.resources.triggerContextEvent(AjaxUploadEventConstants.ALL_UPLOAD_COMPLETE, ctx, callback);
        return holder.get();
    }

    private Object processXHRResult(final boolean success, final Object triggerResult) {

        final JSONObject result = new JSONObject().put("success", success);
        if (triggerResult != null && triggerResult instanceof JSONObject) {

            merge(result, (JSONObject) triggerResult);
            return result;
        }

        ajaxResponseRenderer.addFilter(new PartialMarkupRendererFilter() {

            @Override
            public void renderMarkup(final MarkupWriter writer, final JSONObject reply, final PartialMarkupRenderer renderer) {

                renderer.renderMarkup(writer, reply);
                merge(reply, result);
            }
        });

        return triggerResult;
    }

    @Override
    public String getClientId() {

        return this.clientId;
    }

    public JSONObject merge(final JSONObject defaults, final JSONObject overrides) {

        if (defaults == null) {
            return overrides;
        }

        if (overrides == null) {
            return defaults;
        }

        for (final String key : overrides.keys()) {

            final Object overrideChild = overrides.get(key);

            if (defaults.containsKey(key)) {

                final Object defaultChild = defaults.get(key);

                if (defaultChild instanceof JSONObject && overrideChild instanceof JSONObject) {
                    defaults.put(key, merge((JSONObject) defaultChild, (JSONObject) overrideChild));
                } else {
                    defaults.put(key, overrideChild);
                }

            } else {
                defaults.put(key, overrideChild);
            }
        }

        return defaults;
    }

}