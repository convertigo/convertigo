(function() {
    // Recursively traverse shadow roots to find the screencast toggle button
    function findToggleButton(root) {
        if (!root) return null;

        const devtoolsButton = root.querySelector('devtools-button[aria-label="Toggle screencast"]');
        if (devtoolsButton && devtoolsButton.shadowRoot) {
            const button = devtoolsButton.shadowRoot.querySelector('button[title="Toggle screencast"]');
            if (button) return button;
        }

        // Try children recursively if not found directly
        const allShadowHosts = root.querySelectorAll('*');
        for (const el of allShadowHosts) {
            if (el.shadowRoot) {
                const result = findToggleButton(el.shadowRoot);
                if (result) return result;
            }
        }

        return null;
    }

    // Retry finding and clicking the screencast button up to maxAttempts times
    function tryClickScreencastToggle(maxAttempts = 20, delay = 100) {
        let attempts = 0;

        function attempt() {
            const button = findToggleButton(document.body);
            if (button) {
                if (button.getAttribute('aria-pressed') === 'true') {
                    button.click();
                    console.log("Screencast turned off");
                } else {
                    console.log("Screencast already off");
                }
            } else if (++attempts < maxAttempts) {
                setTimeout(attempt, delay);
            } else {
                console.warn("Screencast toggle button not found after multiple attempts");
            }
        }

        attempt();
    }

    tryClickScreencastToggle();

    // Additional: click "Don't show again" infobar button
    function clickDontShowAgain(root) {
        if (!root) return;

        const infobarButton = root.querySelector('devtools-button.infobar-button');
        if (infobarButton && infobarButton.shadowRoot) {
            const button = infobarButton.shadowRoot.querySelector('button.outlined');
            if (button) {
                button.click();
                console.log("'Don't show again' clicked");
                return;
            }
        }

        // Traverse deeper if not directly found
        const allShadowHosts = root.querySelectorAll('*');
        for (const el of allShadowHosts) {
            if (el.shadowRoot) {
                clickDontShowAgain(el.shadowRoot);
            }
        }
    }

    // Retry clicking the "Don't show again" button to handle slow loading
    function tryClickDontShowAgain(maxAttempts = 20, delay = 100) {
        let attempts = 0;

        function attempt() {
            clickDontShowAgain(document.body);

            if (++attempts < maxAttempts) {
                setTimeout(attempt, delay);
            }
        }

        attempt();
    }

    tryClickDontShowAgain();
})();