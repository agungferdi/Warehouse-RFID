"use client";

import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

const COLORS: Record<string, string> = {
  available: "#3b6fd6",
  sold: "#94a3b8",
  in_transit: "#f0b429",
};

const LABELS: Record<string, string> = {
  available: "Available",
  sold: "Sold",
  in_transit: "In Transit",
};

export function StatusPieChart({ data }: { data: { available: number; sold: number; in_transit: number } }) {
  const chartData = Object.entries(data).map(([key, value]) => ({ key, name: LABELS[key], value }));
  const hasData = chartData.some((d) => d.value > 0);

  if (!hasData) {
    return <div className="flex h-64 items-center justify-center text-sm text-slate-400">No product data yet</div>;
  }

  return (
    <ResponsiveContainer width="100%" height={260}>
      <PieChart>
        <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={95} paddingAngle={2}>
          {chartData.map((entry) => (
            <Cell key={entry.key} fill={COLORS[entry.key]} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
