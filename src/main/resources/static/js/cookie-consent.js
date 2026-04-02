(() => {
    const CONSENT_KEY = "mtg_cookie_consent_v1";
    const CONSENT_ACCEPTED = "accepted";
    const CONSENT_REJECTED = "rejected";

    const banner = document.getElementById("cookie-banner");
    const acceptButton = document.getElementById("cookie-accept");
    const rejectButton = document.getElementById("cookie-reject");

    if (!banner || !acceptButton || !rejectButton) {
        return;
    }

    const consent = localStorage.getItem(CONSENT_KEY);

    if (consent === CONSENT_ACCEPTED || consent === CONSENT_REJECTED) {
        banner.hidden = true;
        return;
    }

    banner.hidden = false;

    acceptButton.addEventListener("click", () => {
        localStorage.setItem(CONSENT_KEY, CONSENT_ACCEPTED);
        banner.hidden = true;

        if (typeof window.initPosthogAnalytics === "function") {
            window.initPosthogAnalytics();
        }
    });

    rejectButton.addEventListener("click", () => {
        localStorage.setItem(CONSENT_KEY, CONSENT_REJECTED);
        banner.hidden = true;
    });
})();
