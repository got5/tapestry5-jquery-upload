package org.got5.tapestry5.jquery;

import org.apache.tapestry5.EventConstants;

/**
 * Analog to {@link EventConstants}-
 *
 * @author ffacon
 */
public class JQueryUploadEventConstants {

    /**
     * Triggered when a file has been uploaded via {@link org.got5.tapestry5.jquery.components.AjaxUpload}.
     */
    public static final String AJAX_UPLOAD = "ajaxFileUpload";

    /**
     * Triggered when a file has been uploaded via {@link org.got5.tapestry5.jquery.components.AjaxUpload} via a
     * regular post (applies to IE browsers).
     */
    public static final String NON_XHR_UPLOAD = "fileUpload";


}
