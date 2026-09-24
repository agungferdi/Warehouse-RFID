import { getSession } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { getDashboardStats } from "@/lib/services/statsService";
import { AppShell } from "@/components/layout/AppShell";
import { KpiCard } from "@/components/layout/KpiCard";
import { StatusPieChart } from "@/components/charts/StatusPieChart";
import { ActivityBarChart } from "@/components/charts/ActivityBarChart";
import Link from "next/link";

const ACTIVITY_LABELS: Record<string, string> = {
  inbound: "Inbound",
  stock_opname: "Stock Opname",
  transfer: "Transfer",
  outbound: "Outbound",
};

export default async function DashboardPage() {
  const session = await getSession();
  const stats = await getDashboardStats(prisma, { includeRecentActivity: true });

  return (
    <AppShell username={session!.username} active="/">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Live warehouse stock and activity overview</p>
        </div>
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard label="Total Products" value={stats.totalProducts} />
        <KpiCard label="Available" value={stats.productsByStatus.available} accent="text-brand-600" />
        <KpiCard label="Sold" value={stats.productsByStatus.sold} accent="text-slate-500" />
        <KpiCard label="In Transit" value={stats.productsByStatus.in_transit} accent="text-amber-600" />
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 lg:grid-cols-5">
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-2">
          <h2 className="mb-2 text-sm font-semibold text-slate-800">Stock by Status</h2>
          <StatusPieChart data={stats.productsByStatus} />
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-3">
          <h2 className="mb-2 text-sm font-semibold text-slate-800">Activity — Last 7 Days</h2>
          <ActivityBarChart data={stats.activityLast7Days} />
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-800">Recent Activity</h2>
          <Link href="/activities" className="text-xs font-medium text-brand-600 hover:underline">
            View all
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                <th className="py-2 pr-4">Time</th>
                <th className="py-2 pr-4">Type</th>
                <th className="py-2 pr-4">EPC</th>
                <th className="py-2 pr-4">Product</th>
                <th className="py-2 pr-4">Location</th>
              </tr>
            </thead>
            <tbody>
              {stats.recentActivity.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-6 text-center text-slate-400">
                    No activity recorded yet.
                  </td>
                </tr>
              )}
              {stats.recentActivity.map((a) => (
                <tr key={a.activityId} className="border-b border-slate-100 last:border-0">
                  <td className="py-2 pr-4 text-slate-500">{new Date(a.activityTime).toLocaleString()}</td>
                  <td className="py-2 pr-4">
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
                      {ACTIVITY_LABELS[a.activityType] ?? a.activityType}
                    </span>
                  </td>
                  <td className="py-2 pr-4 font-mono text-xs text-slate-600">
                    <Link href={`/products/${a.epc}`} className="hover:underline">
                      {a.epc}
                    </Link>
                  </td>
                  <td className="py-2 pr-4">{a.productName}</td>
                  <td className="py-2 pr-4 text-slate-500">{a.location ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
