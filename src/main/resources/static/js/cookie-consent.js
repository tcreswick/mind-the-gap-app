(() => {
    const CONSENT_KEY = "mtg_cookie_consent_v1";
    const CONSENT_ACCEPTED = "accepted";
    const CONSENT_REJECTED = "rejected";
    let inMemoryConsent = null;

    const banner = document.getElementById("cookie-banner");
    const acceptButton = document.getElementById("cookie-accept");
    const rejectButton = document.getElementById("cookie-reject");

    if (!banner || !acceptButton || !rejectButton) {
        return;
    }

    function readConsent() {
        try {
            return localStorage.getItem(CONSENT_KEY);
        } catch (error) {
            return inMemoryConsent;
        }
    }

    function writeConsent(value) {
        inMemoryConsent = value;
        try {
            localStorage.setItem(CONSENT_KEY, value);
        } catch (error) {
            // Some privacy modes block storage; keep working for current session.
        }
    }

    function hideBanner() {
        banner.hidden = true;
        banner.classList.add("cookie-banner--hidden");
    }

    const consent = readConsent();

    if (consent === CONSENT_ACCEPTED || consent === CONSENT_REJECTED) {
        hideBanner();
        return;
    }

    banner.hidden = false;
    banner.classList.remove("cookie-banner--hidden");

    acceptButton.addEventListener("click", () => {
        writeConsent(CONSENT_ACCEPTED);
        hideBanner();

        if (typeof window.initPosthogAnalytics === "function") {
            window.initPosthogAnalytics();
        }
    });

    rejectButton.addEventListener("click", () => {
        writeConsent(CONSENT_REJECTED);
        hideBanner();
    });

    banner.addEventListener("click", (event) => {
        if (event.target === banner || event.target.classList.contains("cookie-banner-text")) {
            writeConsent(CONSENT_REJECTED);
            hideBanner();
        }
    });
})();
