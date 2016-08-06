(function() {
    var dom, doc, where, iframe = document.createElement('iframe'),
        jivo_container = document.createElement('div');
    iframe.src = "javascript:false";
    iframe.title = "";
    iframe.role = "presentation";
    iframe.setAttribute("name", "jivo_container");
    iframe.setAttribute("id", "jivo_container");
    iframe.setAttribute("frameborder", "no");
    jivo_container.className += "jivo-no-transition";
    if ((typeof SVGRect) === "undefined") {
        jivo_container.className += " no-svg";
    }
    (iframe.frameElement || iframe).style.cssText = "width:100%;height:100%;border:0";

    var counter = 0;

    function init(jivo_init) {
        where = document.body.lastChild;

        if (jivo_container.style) {
            jivo_container.style.visibility = 'hidden';
        }
        jivo_container.setAttribute("id", "jivo-iframe-container");
        jivo_container.appendChild(iframe);

        where.parentNode.insertBefore(jivo_container, where.nextSibling);

        if (jivo_init !== 'inited') {
            var jivo_style = document.createElement('style');
            jivo_style.type = 'text/css';
            var text = 'div#jivo-iframe-container *{max-height:100%}body#jivo_outer_body div#jivo-iframe-container.jivo-custom-label{z-index:2147483645 !important;-webkit-transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;-webkit-animation-fill-mode:forwards !important}div#jivo-iframe-container{z-index:2147483645;-webkit-transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;-webkit-animation-fill-mode:forwards !important;animation-fill-mode:forwards !important;position:fixed !important;transform:scale(1);transform-origin:0 100%;font-size:0 !important;min-width:38px !important;max-width:100% !important;display:inline-block !important;overflow:visible !important;background:transparent !important;max-height:100% !important;box-sizing:initial;padding:0;margin:0;top:auto}div#jivo-iframe-container #jivo-action-container{display:block !important;position:static !important;left:0 !important;right:0 !important;bottom:0 !important;padding:0 !important;margin:0 !important;opacity:1 !important}div#jivo-iframe-container.jivo-c-mobile{transition:none !important;-webkit-transition:none !important;max-width:100%;position:fixed !important}div#jivo-iframe-container.jivo-c-mobile.jivo-c-mobile-absolute{position:absolute !important}div#jivo-iframe-container.jivo-c-mobile.jivo-c-mobile-absolute.jivo-no-transition.jivo-transition-opacity{-webkit-transition:opacity .3s cubic-bezier(.39, .24, .21, .99) !important;transition:opacity .3s cubic-bezier(.39, .24, .21, .99) !important}div#jivo-iframe-container.jivo-custom-label{transition:none !important}div#jivo-iframe-container.jivo-custom-label.jivo-state-widget{display:none !important}div#jivo-iframe-container.jivo_shadow.jivo-opening div#jivo-iframe-container.jivo_shadow:after{height:100% !important}div#jivo-iframe-container.jivo_shadow.jivo-state-widget div#jivo-iframe-container.jivo_shadow.jivo-iframe-container-bottom:after,div#jivo-iframe-container.jivo_shadow.jivo-state-widget div#jivo-iframe-container.jivo_shadow.jivo-iframe-container-top:after{height:38px}div#jivo-iframe-container.jivo_shadow:after{position:absolute !important;width:100%;bottom:0 !important;right:0 !important;border-radius:3px 30px 0 0 !important;content:" "}div#jivo-iframe-container.jivo-expanded{overflow:visible !important}div#jivo-iframe-container.jivo-expanded #jivo_close_button{opacity:1}div#jivo-iframe-container.jivo-expanded:after{background:transparent;position:absolute !important;width:100% !important;bottom:0 !important;right:0 !important;border-radius:3px 30px 0 0 !important;content:" " !important;height:100% !important}div#jivo-iframe-container iframe body{overflow:hidden}div#jivo-iframe-container.jivo-ie8{border-top-width:2px;border-left-width:2px;border-right-width:2px;border-bottom-width:0;border-style:solid}div#jivo-iframe-container.jivo-ie8:after{display:none;border-width:0}iframe#jivo_container{z-index:2147483647 !important;position:relative !important;padding:0 !important;margin:auto !important;left:auto !important;right:auto !important;width:100% !important;height:100% !important;max-width:100% !important;min-width:100% !important;min-height:0 !important;max-height:100% !important;display:block !important;background:transparent !important;top:0 !important;bottom:0 !important;opacity:1;visibility:visible}div#jivo-iframe-container.jivo-iframe-container-bottom{right:30px;bottom:0;border-radius:3px 30px 0 0 !important;min-width:300px !important}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo_shadow.jivo-expanded:after,div#jivo-iframe-container.jivo-iframe-container-bottom.jivo_shadow.jivo-state-widget:after{height:38px;-webkit-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);-moz-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);box-shadow:0 12px 25px 8px rgba(0,0,0,0.17)}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo_shadow:after{height:100%;-webkit-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);-moz-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);box-shadow:0 12px 25px 8px rgba(0,0,0,0.17)}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo-expanded{bottom:0}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo-c-mobile{min-width:175px !important}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo-ie8{min-width:auto !important;width:300px}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo-ie8.jivo-state-widget-expanded{border-top-width:0;border-left-width:0;border-right-width:0;border-bottom-width:0}div#jivo-iframe-container.jivo-iframe-container-bottom.jivo-wp{bottom:5px !important}div#jivo-iframe-container.jivo-iframe-container-left{left:-48px;bottom:10px}div#jivo-iframe-container.jivo-iframe-container-left.jivo-collapsed{min-height:124px}div#jivo-iframe-container.jivo-iframe-container-left.jivo-state-widget{border-radius:0 30px 3px 0 !important}div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow.jivo-state-widget:after{width:38px;height:100%}div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow.jivo-expanded:after{width:100%}div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow.jivo-collapsed:after{width:38px;height:100%}div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow:after{border-radius:0 30px 3px 0 !important;right:auto !important;left:0 !important;-webkit-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);-moz-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);box-shadow:0 12px 25px 8px rgba(0,0,0,0.17)}div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow.jivo-opening:after,div#jivo-iframe-container.jivo-iframe-container-left.jivo_shadow.jivo-closing:after{-webkit-box-shadow:none !important;-moz-box-shadow:none !important;box-shadow:none !important}div#jivo-iframe-container.jivo-iframe-container-left.jivo-expanded{left:0}div#jivo-iframe-container.jivo-iframe-container-right{right:-48px;bottom:10px}div#jivo-iframe-container.jivo-iframe-container-right.jivo-collapsed{min-height:124px}div#jivo-iframe-container.jivo-iframe-container-right.jivo-state-widget{border-radius:30px 0 0 3px !important}div#jivo-iframe-container.jivo-iframe-container-right.jivo_shadow.jivo-expanded:after{border-radius:3px 30px 0 3px !important;width:100%;visibility:visible}div#jivo-iframe-container.jivo-iframe-container-right.jivo_shadow:after{top:1px;border-radius:30px 0 0 3px !important;width:38px !important;height:99% !important;-webkit-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);-moz-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);box-shadow:0 12px 25px 8px rgba(0,0,0,0.17)}div#jivo-iframe-container.jivo-iframe-container-right.jivo-opening:after,div#jivo-iframe-container.jivo-iframe-container-right.jivo-closing:after{-webkit-box-shadow:none !important;-moz-box-shadow:none !important;box-shadow:none !important}div#jivo-iframe-container.jivo-iframe-container-right.jivo-expanded{right:0}div#jivo-iframe-container.jivo-iframe-container-top{right:30px;min-width:300px !important}div#jivo-iframe-container.jivo-iframe-container-top.jivo-cbenabled{min-width:300px !important}div#jivo-iframe-container.jivo-iframe-container-top.jivo_shadow.jivo-expanded:after{border-radius:0 30px 3px 0 !important}div#jivo-iframe-container.jivo-iframe-container-top.jivo_shadow.jivo-opening:after{border-radius:3px 30px 0 0 !important}div#jivo-iframe-container.jivo-iframe-container-top.jivo_shadow.jivo-closing:after{border-radius:0 0 3px 30px !important}div#jivo-iframe-container.jivo-iframe-container-top.jivo_shadow:after{top:0 !important;border-radius:0 0 3px 30px !important;-webkit-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);-moz-box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);box-shadow:0 12px 25px 8px rgba(0,0,0,0.17);height:38px}div#jivo-iframe-container.jivo-iframe-container-top.jivo-expanded{top:0}div#jivo-iframe-container.jivo-iframe-container-topdiv.jivo-resizing{top:auto}div#jivo-iframe-container.jivo-no-transition{transition:none !important}div#jivo_action{position:absolute !important;top:0 !important;bottom:0 !important;left:-10px !important;right:0 !important;display:block;overflow:visible;max-height:initial !important;-webkit-touch-callout:none !important;user-select:none !important;-webkit-user-select:none !important;-moz-user-select:none !important;-ms-user-select:none !important;direction:ltr !important}div#jivo_action:hover{opacity:.9 !important}div.jivo-visible{left:-25px !important;display:block !important}div#jivo_close_button{margin-top:0 !important;margin-left:-20px !important;width:23px !important;height:23px !important;cursor:pointer !important;overflow:visible !important;opacity:0;-webkit-transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;transition:all .3s cubic-bezier(.39, .24, .21, .99) !important;transition-delay:.3s}div#jivo_close_button svg{width:23px !important;height:23px !important;position:relative !important;top:0 !important;left:0 !important;margin:0 !important;padding:0;display:block !important;opacity:1;visibility:visible !important}div#jivo-mouse-tracker{position:fixed !important;width:auto !important;height:auto !important;max-height:initial !important;z-index:2147483647 !important;left:-300px !important;right:-300px !important;top:-200px !important;bottom:0 !important;display:block;background-color:transparent !important;opacity:0 !important;-webkit-touch-callout:none !important;-webkit-user-select:none !important;-khtml-user-select:none !important;-moz-user-select:none !important;-ms-user-select:none !important;-o-user-select:none !important;user-select:none !important}div#jivo-drag-handle{position:absolute !important;top:0 !important;left:0 !important;width:79% !important;height:70px !important;background-color:transparent !important;z-index:2147483647 !important;cursor:move !important;-webkit-touch-callout:none !important;-webkit-user-select:none !important;-khtml-user-select:none !important;-moz-user-select:none !important;-ms-user-select:none !important;-o-user-select:none !important;user-select:none !important}#jivo_magic_iframe{width:100%;height:100%;position:fixed;margin:0;padding:0;left:0;top:0;border:0;z-index:200000;background-color:white;-webkit-overflow-scrolling:touch;overflow:auto;filter:none}.jivo-frame-visible{display:block;visibility:visible}.jivo-frame-hidden{display:block;visibility:hidden;width:100%;height:100%;position:absolute;left:-10000px;top:-10000px}.jivo_cobrowsing_element{border:8px solid #c8f70c;-webkit-border-radius:10px;-khtml-border-radius:10px;-moz-border-radius:10px;-ms-border-radius:10px;-o-border-radius:10px;border-radius:10px;behavior:url(corners.htc);box-shadow:0 3px 11px rgba(0,0,0,0.2);-webkit-box-shadow:0 3px 11px rgba(0,0,0,0.2);position:absolute;z-index:199998;pointer-events:none;margin-top:-10px}.jivo_cobrowsing_element .jivo_cobrowsing_element_inner{width:100%;height:100%;background-color:rgba(200,247,12,0.15)}.jivo_cobrowsing_tooltip{position:absolute;width:300px;z-index:199999}.jivo_cobrowsing_tooltip #jivo_action{height:15px !important;z-index:auto}.jivo_cobrowsing_tooltip #jivo_action #jivo_close_button{margin-left:25px}.jivo_cobrowsing_tooltip>div{font-family:"Arial",sans-serif;font-size:13px;background-color:#3cb868;color:#fff;padding:10px;border:0;-webkit-border-radius:15px;-moz-border-radius:15px;border-radius:15px}.jivo_cobrowsing_tooltip>div:after{width:0;height:0;position:absolute;content:" ";border-left:9px solid transparent;border-right:9px solid transparent;border-top:9px solid #3cb868;left:50%;bottom:-9px;margin-left:-9px}.jivo_cobrowsing_tooltip>div.jivo-top:after{border-top-color:transparent;border-bottom:9px solid #3cb868;top:-17px;bottom:auto}.jivo_cobrowsing_tooltip>div.jivo-top.jivo-left #jivo_action{right:-45px !important;left:initial}.jivo_cobrowsing_tooltip>div.jivo-left #jivo_action{right:-25px !important;left:initial}.jivo_cobrowsing_tooltip>div.jivo-left:after{left:20px;margin:0}.jivo_cobrowsing_tooltip>div.jivo-right:after{right:20px;left:auto;margin:0}.jivo_cobrowsing_tooltip>div .jivo_cobrowsing_tooltip_agent{font-weight:bold;padding-bottom:2px}';
            if (jivo_style.styleSheet) {
                jivo_style.styleSheet.cssText = text;
            } else {
                if (jivo_style.innerText == '') {
                    jivo_style.innerText = text;
                } else {
                    jivo_style.innerHTML = text;
                }
            }

            jivo_container.appendChild(jivo_style);
        }

        reloadFrame();
    }

    window.jivo_init = function() {
        counter = 0; //обнуляем счетчик, чтобы не конфликтовал с youtubefix
        init('inited');
    };

    if(!window.atob || document.readyState === 'complete' ){ // ie9 & after onload init
        init();
    } else {
        addListener(window, 'load', init);
    }

    function reloadFrame() { // youtube fix.
        if (counter++ > 3) { // Защита от зацикливания.
            return;
        }

        try {
            doc = iframe.contentWindow.document;
        } catch (e) {
            dom = document.domain;
            iframe.src = "javascript:var d=document.open();d.domain='" + dom + "';void(0);";
            doc = iframe.contentWindow.document;
        }

        doc.open()._l = function() {
            // создаем html и прописываем как стили.
            var js = this.createElement('script');
            if (dom) this.domain = dom;
            js.id = "js-iframe-async";
            js.charset = 'UTF-8';
            if (jivo_config.static_host){
                js.src = '//' + jivo_config.static_host + '/js/bundle_' + jivo_config.locale + '.js?rand=1470404690831';
            } else {
                js.src = jivo_config.base_url + '/js/bundle_' + jivo_config.locale + '.js?rand=1470404690831';
                //js.src = '//widget.dev-local:8181/public/bundle.js';
                //js.src = jivo_config.base_url + '/js/chat_' + jivo_config.locale + '.js?rand=1459200561';
            }

            this.body.appendChild(js);
        };

        doc.write('<!doctype HTML><head><meta http-equiv="X-UA-Compatible" content="IE=edge" /></head><body onload="document._l();"><div id="widget"></div></body>');
        doc.close();
    }

    // код для обратной совместимости указки
    function addListener(element, event, fn) {
        if (element.addEventListener) {
            element.addEventListener(event, fn, false);
        } else if (element.attachEvent) {
            element.attachEvent('on' + event, (function(el) {
                return function() {
                    fn.call(el, window.event);
                };
            }(element)));
            element = null;
        }
    }

    addListener(window, 'message', function(event) {
        if(!event) {
            if(console && console.log){
                console.log('Error receive postMessage, window message event is empty.');
            }
            return;
        }
        var data = event.data,
            source,
            origin;

        if (data.name == 'in_node_webkit') {
            if (!source) {
                source = event.source;
                origin = event.origin;
            }

            if (source && origin) {
                window.jivo_cobrowse = {
                    source: source,
                    origin: origin
                };
                var cookieLangpack = 'jv_' + encodeURIComponent('langpack') + '_' + jivo_config.widget_id + '=' + encodeURIComponent(JSON.stringify(data.langpack)) + '';
                if (jivo_config.cookie_domain) {
                    cookieLangpack += '; domain=' + jivo_config.cookie_domain + '';
                }
                cookieLangpack += '; path=/';
                document.cookie = cookieLangpack;
                source.postMessage({
                    name: 'widget_ready'
                }, origin);
            }
        }

        if (data.name == 'iframe_url_changed' || data == 'iframe_url_changed') {
            reloadFrame();
        }
    }, false);
})();
