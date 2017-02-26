requirejs.config({
  'shim' : {
    'fineuploader' : [ 'jquery' ]
  },
  'paths' : {
    'tjq/upload/fineuploader' : 'tjq-upload/vendor/fineuploader/all.fine-uploader.min'
  }
});

define([ 'underscore', 'jquery', 't5/core/pageinit', 'tjq/upload/fineuploader', 't5/core/ajax', 't5/core/zone', 't5/core/events' ], function(_, $, pageinit, qq, ajax, zone, events) {

  return function(spec) {

    var uploader = new qq.FineUploader(_.defaults(spec.options, {
        element: document.getElementById(spec.id),
        request: {
            endpoint: spec.url
        },
        retry: {
           enableAuto: true
        },
        callbacks: {
          onComplete: function (id, name, responseJSON, xhr) {
            pageinit.handlePartialPageRenderResponse({ 'json': responseJSON });
          },
          onAllComplete: function() {

            return ajax(spec.allCompleteURL, {
              success : function(data) {

                if (data.zones) {
                  // perform multi zone update
                  $.each(data.zones, function(zoneId, content) {

                    zone.findZone($('#' + zoneId)).trigger(events.zone.update, {
                      content : content
                    });
                  });
                }
              }
            });
          }
        }
    }));
  };
});