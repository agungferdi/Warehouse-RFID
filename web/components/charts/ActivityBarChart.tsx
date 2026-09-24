"use client";

import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export interface ActivityDay {
  date: string;
  inbound: number;
  stock_opname: number;
  transfer: number;
  outbound: number;
}

const SERIES: Array<{ key: keyof Omit<ActivityDay, "date">; label: string; color: string }> = [
  { key: "inbound", label: "Inbound", color: "#3b6fd6" },
  { key: "stock_opname", label: "Stock Opname", color: "#f0b429" },
  { key: "transfer", label: "Transfer", color: "#12b76a" },
  { key: "outbound", label: "Outbound", color: "#f04438" },
];

export function ActivityBarChart({ data }: { data: ActivityDay[] }) {
  const formatted = data.map((d) => ({ ...d, label: d.date.slice(5) }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={formatted}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
        <Tooltip />
        <Legend />
        {SERIES.map((s) => (
          <Bar key={s.key} dataKey={s.key} name={s.label} fill={s.color} radius={[4, 4, 0, 0]} />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
}
