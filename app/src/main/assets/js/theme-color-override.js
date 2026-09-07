(function() {
    try {
        var STYLE_ID = 'animelib-player-theme-colors-override';

        function applyThemeColorOverrides() {
            var docEl = document.documentElement;
            if (!docEl) return;

            var themeAttr = docEl.getAttribute('data-theme');
            var isAuto = (themeAttr === 'auto' || !themeAttr);
            var isDarkAttr = (themeAttr === 'dark');
            var isLightAttr = (themeAttr === 'light');

            var systemIsDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

            var computedBg = '';
            try {
                computedBg = window.getComputedStyle(docEl).getPropertyValue('--background').trim().toLowerCase();
            } catch (e) {}

            var isDarkBg = computedBg === '#0a0a0a' || computedBg === '#1c1c1c' || 
                           computedBg.includes('10, 10, 10') || computedBg.includes('28, 28, 28');

            var shouldApplyDarkPlayerColors = isDarkAttr || (isAuto && systemIsDark) || isDarkBg;

            if (isLightAttr && !isDarkBg) {
                shouldApplyDarkPlayerColors = false;
            }

            var existingStyle = document.getElementById(STYLE_ID);

            if (!shouldApplyDarkPlayerColors) {
                if (existingStyle) {
                    existingStyle.remove();
                    console.log('[AnimeLIB] Removed player dark theme color overrides (Light theme active)');
                }
                return;
            }

            if (existingStyle) return;

            var styleEl = document.createElement('style');
            styleEl.id = STYLE_ID;
            styleEl.textContent = `
                :root[data-theme="dark"],
                :root[data-theme="auto"],
                html[data-theme="dark"],
                html[data-theme="auto"],
                body[data-theme="dark"],
                body[data-theme="auto"] {
                    --background: #1c1c1c !important;
                    --background-rgb: 28, 28, 28 !important;
                    --background-dark: #2a2a2d !important;
                    --background-hover: rgba(255, 255, 255, .05) !important;
                    --background-fill-1: rgba(70, 70, 73, .36) !important;
                    --background-fill-2: rgba(70, 70, 73, .32) !important;
                    --background-fill-3: rgba(70, 70, 73, .24) !important;
                    --background-fill-4: rgba(70, 70, 73, .10) !important;
                    --background-elevated-1: rgb(37, 37, 39) !important;
                    --background-elevated-2: rgb(44, 44, 46) !important;
                    --background-elevated-3: rgb(54, 54, 59) !important;
                    --foreground: #1F1F24 !important;
                    --foreground-rgb: 31, 31, 36 !important;
                    --foreground-darken: rgba(0, 0, 0, .15) !important;

                    --btn-default-border: rgba(70, 70, 73, .5) !important;
                    --btn-default-bg: rgba(255, 255, 255, .06) !important;
                    --btn-default-bg-hover: rgba(255, 255, 255, .1) !important;
                    --btn-light-border: #464649 !important;
                    --btn-light-bg: #252527 !important;
                    --btn-light-bg-hover: rgba(255, 255, 255, .04) !important;

                    --border-base: rgba(70, 70, 73, .65) !important;
                    --border-light: #2e2e32 !important;
                    --border-darker: #464649 !important;
                    --border-opacity: rgba(70, 70, 73, .65) !important;
                    --border-primary: #464649 !important;

                    --input-border: #464649 !important;
                    --input-border-focus: #8852DE !important;
                    --input-bg: #222222 !important;

                    --placeholder-block-bg: #252527 !important;
                    --scrollbar-thumb: #464649 !important;
                    --scrollbar-thumb-hover: #636368 !important;
                    --scrollbar: #1c1c1c !important;
                    --comment-highlight: #252527 !important;
                    --sticky-comment-collapsed-gradient: #252527 !important;
                    --skeleton-bg: rgba(70, 70, 73, .32) !important;
                    --skeleton-bg-secondary: rgba(70, 70, 73, .24) !important;
                    --secondary: #252527 !important;
                    --link-border-color: rgba(255, 255, 255, .2) !important;
                }

                @media (prefers-color-scheme: dark) {
                    :root:not([data-theme="light"]),
                    html:not([data-theme="light"]),
                    body:not([data-theme="light"]) {
                        --background: #1c1c1c !important;
                        --background-rgb: 28, 28, 28 !important;
                        --background-dark: #2a2a2d !important;
                        --background-hover: rgba(255, 255, 255, .05) !important;
                        --background-fill-1: rgba(70, 70, 73, .36) !important;
                        --background-fill-2: rgba(70, 70, 73, .32) !important;
                        --background-fill-3: rgba(70, 70, 73, .24) !important;
                        --background-fill-4: rgba(70, 70, 73, .10) !important;
                        --background-elevated-1: rgb(37, 37, 39) !important;
                        --background-elevated-2: rgb(44, 44, 46) !important;
                        --background-elevated-3: rgb(54, 54, 59) !important;
                        --foreground: #1F1F24 !important;
                        --foreground-rgb: 31, 31, 36 !important;
                        --foreground-darken: rgba(0, 0, 0, .15) !important;

                        --btn-default-border: rgba(70, 70, 73, .5) !important;
                        --btn-default-bg: rgba(255, 255, 255, .06) !important;
                        --btn-default-bg-hover: rgba(255, 255, 255, .1) !important;
                        --btn-light-border: #464649 !important;
                        --btn-light-bg: #252527 !important;
                        --btn-light-bg-hover: rgba(255, 255, 255, .04) !important;

                        --border-base: rgba(70, 70, 73, .65) !important;
                        --border-light: #2e2e32 !important;
                        --border-darker: #464649 !important;
                        --border-opacity: rgba(70, 70, 73, .65) !important;
                        --border-primary: #464649 !important;

                        --input-border: #464649 !important;
                        --input-border-focus: #8852DE !important;
                        --input-bg: #222222 !important;

                        --placeholder-block-bg: #252527 !important;
                        --scrollbar-thumb: #464649 !important;
                        --scrollbar-thumb-hover: #636368 !important;
                        --scrollbar: #1c1c1c !important;
                        --comment-highlight: #252527 !important;
                        --sticky-comment-collapsed-gradient: #252527 !important;
                        --skeleton-bg: rgba(70, 70, 73, .32) !important;
                        --skeleton-bg-secondary: rgba(70, 70, 73, .24) !important;
                        --secondary: #252527 !important;
                        --link-border-color: rgba(255, 255, 255, .2) !important;
                    }
                }
            `;

            var target = document.head || document.documentElement || document.body;
            if (target) {
                target.appendChild(styleEl);
                console.log('[AnimeLIB] Successfully applied player dark theme color overrides');
            }
        }

        applyThemeColorOverrides();

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', applyThemeColorOverrides);
        }

        if (window.MutationObserver && document.documentElement) {
            var observer = new MutationObserver(function(mutations) {
                applyThemeColorOverrides();
            });
            observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'class', 'style'] });
        }

        if (window.matchMedia) {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function() {
                applyThemeColorOverrides();
            });
        }
    } catch (e) {
        console.error('[AnimeLIB] Error in theme color overrides:', e);
    }
})();
