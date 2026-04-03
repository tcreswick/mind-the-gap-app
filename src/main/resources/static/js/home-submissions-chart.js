(() => {
  const payloadNode = document.getElementById("home-submissions-trend-data");
  const chartEl = document.getElementById("home-submissions-chart");
  if (!payloadNode || !chartEl || typeof window.ApexCharts === "undefined") return;

  const payloadRaw = payloadNode.dataset.payload;
  if (!payloadRaw) return;

  const payload = JSON.parse(payloadRaw);
  const labels = Array.isArray(payload.labels) ? payload.labels : [];
  const counts = Array.isArray(payload.counts) ? payload.counts : [];

  if (labels.length === 0 || counts.length === 0 || labels.length !== counts.length) {
    chartEl.innerHTML = '<p class="chart-no-data">No recent submission data available.</p>';
    return;
  }

  const options = {
    chart: {
      type: "bar",
      height: 200,
      toolbar: { show: false },
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      animations: { enabled: true, easing: "easeinout", speed: 500 },
    },
    series: [{ name: "Submissions", data: counts }],
    colors: ["#4f46e5"],
    plotOptions: {
      bar: {
        borderRadius: 3,
        columnWidth: "64%",
      },
    },
    dataLabels: { enabled: false },
    xaxis: {
      categories: labels,
      tickPlacement: "on",
      axisBorder: { show: false },
      axisTicks: { show: false },
      labels: {
        rotate: -45,
        hideOverlappingLabels: true,
        style: { colors: "#64748b", fontSize: "11px" },
      },
    },
    yaxis: {
      min: 0,
      forceNiceScale: true,
      labels: {
        style: { colors: "#64748b", fontSize: "12px" },
        formatter: (value) => String(Math.round(value)),
      },
      title: {
        text: "Submissions",
        style: { color: "#64748b", fontSize: "12px", fontWeight: 600 },
      },
    },
    grid: {
      borderColor: "#e2e8f0",
      strokeDashArray: 4,
      xaxis: { lines: { show: false } },
    },
    tooltip: {
      theme: "light",
      y: {
        formatter: (value) =>
          `${value} submission${value === 1 ? "" : "s"}`,
      },
    },
  };

  new ApexCharts(chartEl, options).render();
})();
