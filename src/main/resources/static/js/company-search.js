(() => {
  const input = document.getElementById("company-search");
  const suggestions = document.getElementById("company-suggestions");
  if (!input || !suggestions) {
    return;
  }

  const searchUrl = input.dataset.searchUrl || "/api/companies";
  const companyBaseUrl = (input.dataset.companyBaseUrl || "/company/").replace(/\/?$/, "/");

  let current = [];
  let debounceTimer = null;

  function companyUrl(employerId) {
    return `${companyBaseUrl}${encodeURIComponent(employerId)}`;
  }

  function clearSuggestions() {
    suggestions.innerHTML = "";
    current = [];
  }

  function render(items) {
    clearSuggestions();
    current = items;
    for (const item of items) {
      const li = document.createElement("li");
      li.className = "suggestion-item";
      li.setAttribute("role", "option");
      li.textContent = item.displayName;
      li.addEventListener("mousedown", (event) => {
        event.preventDefault();
        window.location.assign(companyUrl(item.employerId));
      });
      suggestions.appendChild(li);
    }
  }

  async function search(query) {
    if (!query || query.trim().length < 2) {
      clearSuggestions();
      return [];
    }
    const requestUrl = new URL(searchUrl, window.location.origin);
    requestUrl.searchParams.set("q", query.trim());
    let response;
    try {
      response = await fetch(requestUrl.toString(), {
        headers: { Accept: "application/json" }
      });
    } catch (_) {
      clearSuggestions();
      return [];
    }
    if (!response.ok) {
      clearSuggestions();
      return [];
    }
    const results = await response.json();
    render(results);
    return results;
  }

  input.addEventListener("input", () => {
    if (debounceTimer) {
      window.clearTimeout(debounceTimer);
    }
    debounceTimer = window.setTimeout(() => search(input.value), 130);
  });

  input.addEventListener("keydown", async (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      let topResult = current[0];
      if (!topResult) {
        const latest = await search(input.value);
        topResult = latest[0];
      }
      if (topResult) {
        window.location.assign(companyUrl(topResult.employerId));
      }
    }
  });

  document.addEventListener("click", (event) => {
    if (!suggestions.contains(event.target) && event.target !== input) {
      clearSuggestions();
    }
  });
})();
