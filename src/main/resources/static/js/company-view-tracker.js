(() => {
  const tracker = document.getElementById("company-view-tracker");
  if (!tracker) {
    return;
  }

  const employerId = tracker.dataset.employerId;
  const trackUrl = tracker.dataset.trackUrl || "/api/track/company-view";
  if (!employerId) {
    return;
  }

  const payload = JSON.stringify({ employerId });
  const blob = new Blob([payload], { type: "application/json" });

  if (navigator.sendBeacon && navigator.sendBeacon(trackUrl, blob)) {
    return;
  }

  fetch(trackUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: payload,
    keepalive: true
  }).catch(() => {
    // Best effort tracking only.
  });
})();
