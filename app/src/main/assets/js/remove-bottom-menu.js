(function() {
    function injectHideStyle() {
        if (document.getElementById('bs-hide-bottom-menu-style')) return;
        var style = document.createElement('style');
        style.id = 'bs-hide-bottom-menu-style';
        style.textContent = `
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
            .header-bottom,
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
        var target = document.head || document.documentElement;
        if (target) {
            target.appendChild(style);
        }
    }

    injectHideStyle();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', injectHideStyle);
    }
})();
