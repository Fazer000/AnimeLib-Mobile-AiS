(function() {
    function cleanBottomMenu() {
        try {
            var body = document.body;
            if (body) {
                body.removeAttribute('data-bottom-menu-settings');
                body.removeAttribute('data-bottom-menu-visible');
                body.classList.remove('head-track_top', 'head-track_bottom', 'head-track_pinned');
            }
            
            var selectors = [
                '[data-mobile-bottom-menu]',
                '[data-bottom-menu]',
                '[data-bottom-menu-name]',
                '.bottom-menu',
                '.header-bottom',
                '.n5_hn'
            ];
            for (var i = 0; i < selectors.length; i++) {
                var els = document.querySelectorAll(selectors[i]);
                for (var j = 0; j < els.length; j++) {
                    var el = els[j];
                    if (el && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
                        el.remove();
                    }
                }
            }

            if (!document.getElementById('bs-hide-bottom-menu-style')) {
                var style = document.createElement('style');
                style.id = 'bs-hide-bottom-menu-style';
                style.innerHTML = `
                    body,
                    body[data-bottom-menu-settings],
                    body[data-bottom-menu-visible],
                    body.head-track_bottom,
                    body.head-track_pinned {
                        padding-bottom: 0 !important;
                        margin-bottom: 0 !important;
                    }
                    [data-mobile-bottom-menu],
                    [data-bottom-menu],
                    [data-bottom-menu-name],
                    .bottom-menu,
                    .n5_hn,
                    div[class*="bottom-menu"] {
                        display: none !important;
                        visibility: hidden !important;
                        height: 0 !important;
                        max-height: 0 !important;
                        overflow: hidden !important;
                        opacity: 0 !important;
                        pointer-events: none !important;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            }
        } catch(e) {}
    }

    cleanBottomMenu();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', cleanBottomMenu);
    }

    if (window._bsBottomMenuObserver) {
        try { window._bsBottomMenuObserver.disconnect(); } catch(e) {}
    }

    try {
        var targetNode = document.body || document.documentElement;
        if (targetNode) {
            var observer = new MutationObserver(function() {
                cleanBottomMenu();
            });
            observer.observe(targetNode, { attributes: true, childList: true, subtree: true });
            window._bsBottomMenuObserver = observer;
        }
    } catch(e) {}
})();

