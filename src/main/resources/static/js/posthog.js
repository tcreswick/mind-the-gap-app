!(function (t, e) {
    var o, n, p, r;
    if (!e.__SV && !(window.posthog && window.posthog.__loaded)) {
        window.posthog = e;
        e._i = [];
        e.init = function (i, s, a) {
            function g(target, method) {
                var split = method.split(".");
                if (split.length === 2) {
                    target = target[split[0]];
                    method = split[1];
                }
                target[method] = function () {
                    target.push([method].concat(Array.prototype.slice.call(arguments, 0)));
                };
            }

            p = t.createElement("script");
            p.type = "text/javascript";
            p.crossOrigin = "anonymous";
            p.async = true;
            p.src = s.api_host.replace(".i.posthog.com", "-assets.i.posthog.com") + "/static/array.js";
            r = t.getElementsByTagName("script")[0];
            r.parentNode.insertBefore(p, r);

            var u = e;
            if (a !== undefined) {
                u = e[a] = [];
            } else {
                a = "posthog";
            }
            u.people = u.people || [];
            u.toString = function (stub) {
                var name = "posthog";
                if (a !== "posthog") {
                    name += "." + a;
                }
                if (!stub) {
                    name += " (stub)";
                }
                return name;
            };
            u.people.toString = function () {
                return u.toString(1) + ".people (stub)";
            };
            o = "fi init Oi Fi ft Ii Ai Ri capture calculateEventProperties Ni register register_once register_for_session unregister unregister_for_session Hi getFeatureFlag getFeatureFlagPayload getFeatureFlagResult isFeatureEnabled reloadFeatureFlags updateFlags updateEarlyAccessFeatureEnrollment getEarlyAccessFeatures on onFeatureFlags onSurveysLoaded onSessionId getSurveys getActiveMatchingSurveys renderSurvey displaySurvey cancelPendingSurvey canRenderSurvey canRenderSurveyAsync qi identify setPersonProperties group resetGroups setPersonPropertiesForFlags resetPersonPropertiesForFlags setGroupPropertiesForFlags resetGroupPropertiesForFlags reset get_distinct_id getGroups get_session_id get_session_replay_url alias set_config startSessionRecording stopSessionRecording sessionRecordingStarted captureException startExceptionAutocapture stopExceptionAutocapture loadToolbar get_property getSessionProperty zi Li createPersonProfile setInternalOrTestUser Bi $i Wi opt_in_capturing opt_out_capturing has_opted_in_capturing has_opted_out_capturing get_explicit_consent_status is_capturing clear_opt_in_out_capturing Mi debug bt ji getPageViewId captureTraceFeedback captureTraceMetric Si".split(" ");
            for (n = 0; n < o.length; n += 1) {
                g(u, o[n]);
            }
            e._i.push([i, s, a]);
        };
        e.__SV = 1;
    }
})(document, window.posthog || []);

(() => {
    const CONSENT_KEY = "mtg_cookie_consent_v1";
    const CONSENT_ACCEPTED = "accepted";
    let initialized = false;

    function initPosthogAnalytics() {
        if (initialized || !window.posthog || typeof window.posthog.init !== "function") {
            return;
        }

        initialized = true;
        window.posthog.init("phc_A3WCMrUGWYMvCbQUX5tVWtgGFTr3jew4ZtGVBAj48gDa", {
            api_host: "https://eu.i.posthog.com",
            defaults: "2026-01-30",
            person_profiles: "identified_only"
        });
    }

    window.initPosthogAnalytics = initPosthogAnalytics;

    if (localStorage.getItem(CONSENT_KEY) === CONSENT_ACCEPTED) {
        initPosthogAnalytics();
    }
})();
