(function() {
    if (window.animelibSiteLogoLongPressSetup) return;
    window.animelibSiteLogoLongPressSetup = true;

    console.log('[AnimeLIB] Setting up site logo long-press handler');

    // 1. Inject CSS rule to permanently hide and neutralize the site's native sites-list popup
    function injectHideStyle() {
        var styleId = 'animelib-prevent-native-sites-popup';
        if (!document.getElementById(styleId)) {
            var hideStyle = document.createElement('style');
            hideStyle.id = styleId;
            hideStyle.innerHTML = '[data-name="sites-list"], .popup[data-name="sites-list"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }';
            if (document.head || document.documentElement) {
                (document.head || document.documentElement).appendChild(hideStyle);
            }
        }
    }
    injectHideStyle();
    document.addEventListener('DOMContentLoaded', injectHideStyle);

    // 2. Observer to auto-close any native sites-list popup if created by site JS
    function closeNativeSitesPopup() {
        var popups = document.querySelectorAll('[data-name="sites-list"]');
        popups.forEach(function(popup) {
            popup.classList.add('is-hidden');
            popup.style.setProperty('display', 'none', 'important');
            popup.style.setProperty('visibility', 'hidden', 'important');
            var closeBtn = popup.querySelector('.popup-close');
            if (closeBtn) {
                try { closeBtn.click(); } catch(err) {}
            }
        });
    }

    try {
        var observer = new MutationObserver(closeNativeSitesPopup);
        observer.observe(document.body || document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'style']
        });
    } catch(e) {}

    closeNativeSitesPopup();

    var longPressTimer = null;
    var isLongPressed = false;
    var suppressUntil = 0;
    var startX = 0;
    var startY = 0;
    var LONG_PRESS_DURATION = 300; // ms
    var activeLogoEl = null;

    function getTargetLogoAnchor(target) {
        if (!target || target.nodeType !== 1) return null;

        // 1. Explicit bottom menu home item or data attribute
        var homeItem = target.closest('[data-bottom-menu-name="home"], [data-nav="home"]');
        if (homeItem) return homeItem;

        // 2. Class names matching site logo or header logo
        var logoEl = target.closest('.site-logo, .header__logo, .header-logo, [class*="site-logo"], [class*="header__logo"]');
        if (logoEl) {
            var anchor = logoEl.closest('a');
            return anchor || logoEl;
        }

        // 3. Anchor containing logo class or SVG inside header or top bar
        var vpAnchor = target.closest('a.vp_j');
        if (vpAnchor && vpAnchor.querySelector('.site-logo, [class*="logo"], svg, img')) {
            return vpAnchor;
        }

        // 4. Any anchor pointing to home ("/" or "/ru" or "/ru/") inside header or nav or bottom menu
        var anchor = target.closest('a');
        if (anchor) {
            var href = anchor.getAttribute('href');
            if (href === '/' || href === '/ru' || href === '/ru/' || (href && (href.startsWith('/ru?') || href.startsWith('/?')))) {
                var isHeaderOrNav = anchor.closest('header, nav, .header, .bottom-menu, .top-bar, #header');
                var hasLogoVisual = anchor.querySelector('svg, img, [class*="logo"], [class*="icon"]');
                var isHomeAttr = anchor.getAttribute('data-bottom-menu-name') === 'home';
                if (isHeaderOrNav || hasLogoVisual || isHomeAttr || anchor.classList.contains('vp_j')) {
                    return anchor;
                }
            }
        }

        return null;
    }

    function cancelTimer() {
        if (longPressTimer) {
            clearTimeout(longPressTimer);
            longPressTimer = null;
        }
    }

    function blockEvent(e) {
        if (e.cancelable) e.preventDefault();
        e.stopPropagation();
        if (e.stopImmediatePropagation) e.stopImmediatePropagation();
        return false;
    }

    function handleStart(e) {
        var logoEl = getTargetLogoAnchor(e.target);
        if (!logoEl) return;

        if (Date.now() < suppressUntil) {
            closeNativeSitesPopup();
            return blockEvent(e);
        }

        cancelTimer();
        isLongPressed = false;
        activeLogoEl = logoEl;

        var touch = e.touches ? e.touches[0] : e;
        startX = touch.clientX;
        startY = touch.clientY;

        longPressTimer = setTimeout(function() {
            isLongPressed = true;
            suppressUntil = Date.now() + 2500;
            console.log('[AnimeLIB] Site logo long press triggered!');

            closeNativeSitesPopup();

            try {
                if (navigator.vibrate) navigator.vibrate(40);
            } catch(err) {}

            if (window.AndroidInterface && window.AndroidInterface.onSiteLogoLongPressed) {
                window.AndroidInterface.onSiteLogoLongPressed();
            }

            cancelTimer();
        }, LONG_PRESS_DURATION);
    }

    function handleMove(e) {
        if (!longPressTimer) return;
        var touch = e.touches ? e.touches[0] : e;
        var moveX = Math.abs(touch.clientX - startX);
        var moveY = Math.abs(touch.clientY - startY);
        if (moveX > 10 || moveY > 10) {
            cancelTimer();
            isLongPressed = false;
        }
    }

    function handleEnd(e) {
        cancelTimer();
        if (isLongPressed || Date.now() < suppressUntil) {
            isLongPressed = false;
            closeNativeSitesPopup();
            return blockEvent(e);
        }
    }

    function handleIntercept(e) {
        if (e.type === 'contextmenu') {
            var logoEl = getTargetLogoAnchor(e.target);
            if (logoEl) {
                closeNativeSitesPopup();
                return blockEvent(e);
            }
        }

        if (isLongPressed || Date.now() < suppressUntil) {
            closeNativeSitesPopup();
            return blockEvent(e);
        }
    }

    ['touchstart', 'mousedown', 'pointerdown'].forEach(function(evt) {
        document.addEventListener(evt, handleStart, { passive: false, capture: true });
    });

    ['touchmove', 'mousemove', 'pointermove'].forEach(function(evt) {
        document.addEventListener(evt, handleMove, { passive: true, capture: true });
    });

    ['touchend', 'touchcancel', 'mouseup', 'pointerup', 'pointercancel'].forEach(function(evt) {
        document.addEventListener(evt, handleEnd, { passive: false, capture: true });
    });

    ['click', 'contextmenu', 'auxclick'].forEach(function(evt) {
        document.addEventListener(evt, handleIntercept, { passive: false, capture: true });
        window.addEventListener(evt, handleIntercept, { passive: false, capture: true });
    });

    console.log('[AnimeLIB] Site logo long-press listeners successfully installed');
})();
