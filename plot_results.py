#!/usr/bin/env python3
"""Create a dependency-free SVG comparison chart from results.csv."""
import csv
import html
import math
from collections import defaultdict
from pathlib import Path

source = Path("results.csv")
if not source.exists():
    raise SystemExit("results.csv not found. Run SortingBenchmark first.")

cases = ["Random", "Reverse", "Partial (80%)", "Sorted"]
algorithms = ["Selection Sort", "Insertion Sort", "Bubble Sort", "Merge Sort", "Quick Sort"]
colors = ["#3468a8", "#e38b35", "#4c9b69", "#8a68ad"]
data = defaultdict(dict)
with source.open(newline="", encoding="utf-8") as f:
    for row in csv.DictReader(f):
        data[row["Algorithm"]][row["Case"]] = max(1, int(row["TimeMicroseconds"]))

W, H = 1100, 650
left, right, top, bottom = 90, 25, 80, 150
plot_w, plot_h = W - left - right, H - top - bottom
all_values = [v for alg in algorithms for v in data[alg].values()]
max_log = max(1.0, math.ceil(math.log10(max(all_values))))
min_log = 0.0

def y_for(v):
    return top + plot_h * (max_log - math.log10(max(v, 1))) / max_log

parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
         '<rect width="100%" height="100%" fill="#ffffff"/>',
         '<style>text{font-family:Arial,sans-serif;fill:#243244}.title{font-size:24px;font-weight:700}.axis{font-size:13px}.label{font-size:12px}.legend{font-size:14px}</style>',
         f'<text x="{W/2}" y="36" text-anchor="middle" class="title">Sorting Algorithms — Runtime (µs, logarithmic scale)</text>']
for power in range(int(min_log), int(max_log) + 1):
    y = y_for(10 ** power)
    parts.append(f'<line x1="{left}" y1="{y:.1f}" x2="{W-right}" y2="{y:.1f}" stroke="#dce2e8"/>')
    parts.append(f'<text x="{left-12}" y="{y+4:.1f}" text-anchor="end" class="axis">10^{power}</text>')

cluster = plot_w / len(algorithms)
bar_w = min(24, cluster / 5.2)
for ai, alg in enumerate(algorithms):
    center = left + cluster * (ai + 0.5)
    parts.append(f'<text x="{center:.1f}" y="{H-bottom+28}" text-anchor="middle" class="label">{html.escape(alg)}</text>')
    for ci, case in enumerate(cases):
        value = data[alg].get(case, 1)
        x = center + (ci - 1.5) * (bar_w + 3) - bar_w / 2
        y = y_for(value)
        height = top + plot_h - y
        parts.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{bar_w:.1f}" height="{height:.1f}" fill="{colors[ci]}"/>')
        parts.append(f'<title>{html.escape(alg)} — {html.escape(case)}: {value:,} µs</title>')

legend_y = H - 52
legend_total = 4 * 190
legend_x = (W - legend_total) / 2
for i, case in enumerate(cases):
    x = legend_x + i * 190
    parts.append(f'<rect x="{x:.1f}" y="{legend_y-13}" width="15" height="15" fill="{colors[i]}"/>')
    parts.append(f'<text x="{x+22:.1f}" y="{legend_y}" class="legend">{html.escape(case)}</text>')
parts.extend(['</svg>'])
Path("performance_comparison.svg").write_text("\n".join(parts), encoding="utf-8")
print("Created performance_comparison.svg")
