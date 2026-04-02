(() => {
  const dataNode = document.getElementById("chart-data");
  if (!dataNode || typeof window.ApexCharts === "undefined") return;

  const payloadRaw = dataNode.dataset.payload;
  if (!payloadRaw) return;

  const p = JSON.parse(payloadRaw);
  const years = p.years.map(String);

  const FONT = '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
  const MALE_COLOR   = "#2563eb";
  const FEMALE_COLOR = "#e11d48";
  const MEAN_COLOR   = "#4f46e5";
  const MEDIAN_COLOR = "#0ea5e9";
  const MUTED        = "#64748b";
  const BORDER       = "#e2e8f0";

  // ── Shared axis styles ──────────────────────────────────
  function axisLabels(formatter) {
    return { style: { colors: MUTED, fontSize: "12px", fontFamily: FONT },
             formatter };
  }

  function baseChart(height) {
    return {
      height,
      toolbar: { show: false },
      fontFamily: FONT,
      animations: { enabled: true, easing: "easeinout", speed: 500 },
    };
  }

  function baseGrid() {
    return { borderColor: BORDER, strokeDashArray: 4, xaxis: { lines: { show: false } } };
  }

  function baseLegend(show = true) {
    return {
      show,
      position: "bottom",
      horizontalAlign: "center",
      offsetY: 4,
      markers: { size: 7, shape: "circle" },
      labels: { colors: "#0f172a" },
      fontSize: "12px",
      fontFamily: FONT,
    };
  }

  function stackedBarOptions({ series, height = 260, showLegend = false }) {
    return {
      chart: { ...baseChart(height), type: "bar", stacked: true },
      series,
      colors: [MALE_COLOR, FEMALE_COLOR],
      plotOptions: {
        bar: { columnWidth: "58%", borderRadius: 3,
               borderRadiusApplication: "end", borderRadiusWhenStacked: "last" }
      },
      dataLabels: { enabled: false },
      xaxis: { categories: years, axisBorder: { show: false }, axisTicks: { show: false },
               labels: axisLabels() },
      yaxis: { min: 0, max: 100, tickAmount: 4,
               labels: axisLabels(v => v + "%") },
      tooltip: { theme: "light", y: { formatter: v => v.toFixed(1) + "%" } },
      legend: baseLegend(showLegend),
      grid: baseGrid(),
    };
  }

  // ── 1. Overall workforce split (deviation from 50:50) ──
  const elSplit = document.getElementById("chart-split");
  if (elSplit) {
    const deviation = p.female.map(v => v != null ? Math.round((v - 50) * 10) / 10 : null);
    const hasData = deviation.some(v => v !== null);
    if (hasData) {
      new ApexCharts(elSplit, {
        chart: { ...baseChart(280), type: "line" },
        series: [{ name: "Gender balance", data: deviation }],
        colors: ["#7c3aed"],
        stroke: { width: [2.5], curve: "smooth" },
        markers: { size: 4, strokeWidth: 0 },
        dataLabels: { enabled: false },
        xaxis: { categories: years, axisBorder: { show: false }, axisTicks: { show: false },
                 labels: axisLabels() },
        yaxis: {
          labels: axisLabels(v => {
            if (v > 0) return "+" + v + "%";
            if (v < 0) return v + "%";
            return "0%";
          })
        },
        annotations: {
          yaxis: [{ y: 0, borderColor: "#94a3b8", strokeDashArray: 4,
                    label: { text: "Equal (50:50)", style: { background: "#f1f5f9",
                             color: MUTED, fontSize: "11px", fontFamily: FONT } } }]
        },
        tooltip: {
          theme: "light",
          custom: function({ series, seriesIndex, dataPointIndex }) {
            const femPct = p.female[dataPointIndex];
            const malePct = p.male[dataPointIndex];
            const dev = series[seriesIndex][dataPointIndex];
            const sign = dev > 0 ? "+" : "";
            return '<div style="padding:8px 12px;font-size:13px;line-height:1.6">' +
              '<strong>' + years[dataPointIndex] + '</strong><br>' +
              '<span style="color:' + MALE_COLOR + '">Men: ' + malePct.toFixed(1) + '%</span><br>' +
              '<span style="color:' + FEMALE_COLOR + '">Women: ' + femPct.toFixed(1) + '%</span><br>' +
              '<span style="color:#64748b">' + sign + dev.toFixed(1) + '% from parity</span></div>';
          }
        },
        legend: baseLegend(false),
        grid: baseGrid(),
      }).render();
    } else {
      elSplit.innerHTML = '<p class="chart-no-data">No workforce composition data available.</p>';
    }
  }

  // ── 2. Hourly pay gap line chart ───────────────────────
  const elPayGap = document.getElementById("chart-pay-gap");
  if (elPayGap) {
    const hasData = p.diffMeanHourly.some(v => v !== null)
                 || p.diffMedianHourly.some(v => v !== null);
    if (hasData) {
      new ApexCharts(elPayGap, {
        chart: { ...baseChart(280), type: "line" },
        series: [
          { name: "Mean pay gap",   data: p.diffMeanHourly   },
          { name: "Median pay gap", data: p.diffMedianHourly },
        ],
        colors: [MEAN_COLOR, MEDIAN_COLOR],
        stroke: { width: [2.5, 2.5], curve: "smooth", dashArray: [0, 5] },
        markers: { size: 4, strokeWidth: 0 },
        dataLabels: { enabled: false },
        xaxis: { categories: years, axisBorder: { show: false }, axisTicks: { show: false },
                 labels: axisLabels() },
        yaxis: { labels: axisLabels(v => v != null ? v.toFixed(1) + "%" : "") },
        annotations: {
          yaxis: [{ y: 0, borderColor: "#94a3b8", strokeDashArray: 4,
                    label: { text: "Equal", style: { background: "#f1f5f9",
                             color: MUTED, fontSize: "11px", fontFamily: FONT } } }]
        },
        tooltip: { theme: "light", y: { formatter: v => v != null ? v.toFixed(1) + "%" : "N/A" } },
        legend: baseLegend(true),
        grid: baseGrid(),
      }).render();
    } else {
      elPayGap.innerHTML = '<p class="chart-no-data">No hourly pay gap data available.</p>';
    }
  }

  // ── 3–6. Quartile charts ──────────────────────────────
  const quartiles = [
    { id: "chart-lq",  male: p.maleLowerQ,  female: p.femaleLowerQ  },
    { id: "chart-lmq", male: p.maleLowerMQ, female: p.femaleLowerMQ },
    { id: "chart-umq", male: p.maleUpperMQ, female: p.femaleUpperMQ },
    { id: "chart-tq",  male: p.maleTopQ,    female: p.femaleTopQ    },
  ];
  for (const q of quartiles) {
    const el = document.getElementById(q.id);
    if (el) {
      new ApexCharts(el, stackedBarOptions({
        series: [
          { name: "Men (%)",   data: q.male   },
          { name: "Women (%)", data: q.female },
        ],
        height: 220,
      })).render();
    }
  }

  // ── 7. Bonus participation grouped bar ────────────────
  const elBonusPart = document.getElementById("chart-bonus-participation");
  if (elBonusPart) {
    const hasBonus = p.maleBonusPct.some(v => v > 0) || p.femaleBonusPct.some(v => v > 0);
    if (hasBonus) {
      new ApexCharts(elBonusPart, {
        chart: { ...baseChart(260), type: "bar" },
        series: [
          { name: "Men receiving bonus (%)",   data: p.maleBonusPct   },
          { name: "Women receiving bonus (%)", data: p.femaleBonusPct },
        ],
        colors: [MALE_COLOR, FEMALE_COLOR],
        plotOptions: { bar: { columnWidth: "58%", borderRadius: 3 } },
        dataLabels: { enabled: false },
        xaxis: { categories: years, axisBorder: { show: false }, axisTicks: { show: false },
                 labels: axisLabels() },
        yaxis: { min: 0, max: 100, tickAmount: 4, labels: axisLabels(v => v + "%") },
        tooltip: { theme: "light", y: { formatter: v => v.toFixed(1) + "%" } },
        legend: baseLegend(true),
        grid: baseGrid(),
      }).render();
    } else {
      elBonusPart.innerHTML = '<p class="chart-no-data">No bonus participation data available.</p>';
    }
  }

  // ── 8. Bonus pay gap line chart ───────────────────────
  const elBonusGap = document.getElementById("chart-bonus-gap");
  if (elBonusGap) {
    const hasData = p.diffMeanBonus.some(v => v !== null)
                 || p.diffMedianBonus.some(v => v !== null);
    if (hasData) {
      new ApexCharts(elBonusGap, {
        chart: { ...baseChart(260), type: "line" },
        series: [
          { name: "Mean bonus gap",   data: p.diffMeanBonus   },
          { name: "Median bonus gap", data: p.diffMedianBonus },
        ],
        colors: [MEAN_COLOR, MEDIAN_COLOR],
        stroke: { width: [2.5, 2.5], curve: "smooth", dashArray: [0, 5] },
        markers: { size: 4, strokeWidth: 0 },
        dataLabels: { enabled: false },
        xaxis: { categories: years, axisBorder: { show: false }, axisTicks: { show: false },
                 labels: axisLabels() },
        yaxis: { labels: axisLabels(v => v != null ? v.toFixed(1) + "%" : "") },
        annotations: {
          yaxis: [{ y: 0, borderColor: "#94a3b8", strokeDashArray: 4,
                    label: { text: "Equal", style: { background: "#f1f5f9",
                             color: MUTED, fontSize: "11px", fontFamily: FONT } } }]
        },
        tooltip: { theme: "light", y: { formatter: v => v != null ? v.toFixed(1) + "%" : "N/A" } },
        legend: baseLegend(true),
        grid: baseGrid(),
      }).render();
    } else {
      elBonusGap.innerHTML = '<p class="chart-no-data">No bonus gap data available.</p>';
    }
  }
})();
