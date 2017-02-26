package org.got5.tapestry5.upload.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.got5.tapestry5.upload.AjaxUploadEventConstants;

public class AjaxUploadTest {

    @Inject
    private AjaxResponseRenderer renderer;

    @Inject
    private AlertManager alertManager;

    @Inject
    private Messages messages;

    @InjectComponent
    private Zone uploadResult, allCompleteZone;

    @Property
    private Object[] context;

    @Persist(PersistenceConstants.FLASH)
    @Property
    private String message;

    @Persist
    @Property
    private List<File> uploadedFiles;

    @Property
    private boolean done;

    @Property
    private JSONObject customOptions;

    void onActivate() {

        if (uploadedFiles == null) {
            uploadedFiles = new ArrayList<>();
        }
    }

    void setupRender() {
        context = new Object[] { "Upload complete" };
        customOptions = new JSONObject();
        customOptions.put("messages", new JSONObject("typeError", messages.get("typeError-message")));
    }

    @OnEvent(component = "uploadImage", value = AjaxUploadEventConstants.UPLOAD)
    void onImageUpload(final File uploadedFile) {

        if (uploadedFile != null) {
            this.uploadedFiles.add(uploadedFile);
        }

        renderer.addRender(uploadResult);
    }

    @OnEvent(component = "uploadImage", value = AjaxUploadEventConstants.ALL_UPLOAD_COMPLETE)
    void onAllComplete(final String context) {

        done = true;
        alertManager.success(context);
        renderer.addRender(allCompleteZone);
    }

    void onUploadException(final FileUploadException ex) {

        message = "Upload exception: " + ex.getMessage();

        renderer.addRender(uploadResult);
    }
}
