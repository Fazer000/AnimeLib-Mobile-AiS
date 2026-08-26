/**
 * Обработчик перехвата токена, ID пользователя и клика кнопки «Выход из аккаунта»
 */

(function() {
    console.log('Auth handler loading...');

    // 1. Перехват сетевых запросов (/api/auth/oauth/token и /api/auth/me)
    if (!window.__authInterceptorInjected) {
        window.__authInterceptorInjected = true;

        // Перехват fetch API
        const originalFetch = window.fetch;
        if (typeof originalFetch === 'function') {
            window.fetch = async function(...args) {
                const response = await originalFetch.apply(this, args);
                try {
                    const url = (typeof args[0] === 'string') ? args[0] : (args[0] && args[0].url ? args[0].url : '');
                    if (url) {
                        if (url.includes('auth/oauth/token')) {
                            console.log('Intercepted fetch: /api/auth/oauth/token');
                            const clone = response.clone();
                            clone.text().then(text => {
                                if (window.AndroidInterface && typeof window.AndroidInterface.saveOAuthToken === 'function') {
                                    window.AndroidInterface.saveOAuthToken(text);
                                }
                            }).catch(e => console.error('Error reading oauth token response:', e));
                        } else if (url.includes('auth/me')) {
                            console.log('Intercepted fetch: /api/auth/me');
                            const clone = response.clone();
                            clone.text().then(text => {
                                if (window.AndroidInterface && typeof window.AndroidInterface.saveAuthMeData === 'function') {
                                    window.AndroidInterface.saveAuthMeData(text);
                                }
                            }).catch(e => console.error('Error reading auth me response:', e));
                        } else if (url.includes('auth/logout') || url.includes('/logout')) {
                            console.log('Intercepted fetch: logout endpoint');
                            if (window.AndroidInterface && typeof window.AndroidInterface.clearAuthToken === 'function') {
                                window.AndroidInterface.clearAuthToken();
                            }
                        }
                    }
                } catch (e) {
                    console.error('Fetch intercept error:', e);
                }
                return response;
            };
        }

        // Перехват XMLHttpRequest
        const originalXhrOpen = XMLHttpRequest.prototype.open;
        const originalXhrSend = XMLHttpRequest.prototype.send;

        XMLHttpRequest.prototype.open = function(method, url) {
            this.__requestUrl = url;
            return originalXhrOpen.apply(this, arguments);
        };

        XMLHttpRequest.prototype.send = function() {
            this.addEventListener('load', function() {
                try {
                    const url = this.__requestUrl || '';
                    if (url) {
                        if (url.includes('auth/oauth/token')) {
                            console.log('Intercepted XHR: /api/auth/oauth/token');
                            if (window.AndroidInterface && typeof window.AndroidInterface.saveOAuthToken === 'function') {
                                window.AndroidInterface.saveOAuthToken(this.responseText);
                            }
                        } else if (url.includes('auth/me')) {
                            console.log('Intercepted XHR: /api/auth/me');
                            if (window.AndroidInterface && typeof window.AndroidInterface.saveAuthMeData === 'function') {
                                window.AndroidInterface.saveAuthMeData(this.responseText);
                            }
                        } else if (url.includes('auth/logout') || url.includes('/logout')) {
                            console.log('Intercepted XHR: logout endpoint');
                            if (window.AndroidInterface && typeof window.AndroidInterface.clearAuthToken === 'function') {
                                window.AndroidInterface.clearAuthToken();
                            }
                        }
                    }
                } catch (e) {
                    console.error('XHR intercept error:', e);
                }
            });
            return originalXhrSend.apply(this, arguments);
        };
        console.log('Auth request interceptors injected successfully');
    }

    // 2. Перехват клика по кнопке «Выход из аккаунта»
    if (!window.__logoutListenerInjected) {
        window.__logoutListenerInjected = true;

        document.addEventListener('click', function(e) {
            let target = e.target;
            while (target && target !== document.body && target !== document.documentElement) {
                if (target.tagName === 'BUTTON' || target.tagName === 'A' || (target.classList && target.classList.contains('btn'))) {
                    const text = (target.innerText || target.textContent || '').trim();
                    const hasLogoutText = text.includes('Выход из аккаунта') || text.includes('Выход');
                    const hasLogoutIcon = target.querySelector('.fa-right-from-bracket') !== null ||
                                          target.querySelector('[data-icon="right-from-bracket"]') !== null ||
                                          (target.getAttribute && target.getAttribute('data-icon') === 'right-from-bracket');
                    const isDangerBtn = target.classList && target.classList.contains('variant-danger');

                    if (hasLogoutText || (isDangerBtn && (hasLogoutIcon || hasLogoutText))) {
                        console.log('Logout button clicked in WebView');
                        if (window.AndroidInterface && typeof window.AndroidInterface.clearAuthToken === 'function') {
                            window.AndroidInterface.clearAuthToken();
                        }
                    }
                    break;
                }
                target = target.parentElement;
            }
        }, true);
        console.log('Logout click listener attached');
    }

    // 3. Дополнительная синхронизация из localStorage и document.cookie при наличии
    function syncAuthToken() {
        try {
            if (typeof localStorage !== 'undefined') {
                const auth = localStorage.getItem('auth');
                if (auth && auth !== 'null' && auth !== 'undefined') {
                    if (window.AndroidInterface && typeof window.AndroidInterface.getAuthFromLocalStorage === 'function') {
                        window.AndroidInterface.getAuthFromLocalStorage();
                    }
                }
            }
        } catch (error) {
            console.error('Error checking localStorage:', error);
        }

        try {
            if (document.cookie && window.CookieManager && typeof window.CookieManager.setCookie === 'function') {
                const cookies = document.cookie.split(';');
                cookies.forEach(c => {
                    const parts = c.trim().split('=');
                    if (parts.length >= 2) {
                        const name = parts[0].trim();
                        const val = parts.slice(1).join('=').trim();
                        if (name) {
                            window.CookieManager.setCookie(name, val, window.location.hostname);
                        }
                    }
                });
            }
        } catch (e) {
            console.error('Error syncing document.cookie:', e);
        }
    }
    syncAuthToken();
})();
'auth_handler_ok';

