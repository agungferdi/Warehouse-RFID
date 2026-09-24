import Link from "next/link";
import { Prisma } from "@prisma/client";
import { getSession } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AppShell } from "@/components/layout/AppShell";

const PAGE_SIZE = 30;

const ACTIVITY_LABELS: Record<string, string> = {
  inbound: "Inbound",
  stock_opname: "Stock Opname",
  transfer: "Transfer",
  outbound: "Outbound",
};

const ACTIVITY_BADGE: Record<string, string> = {
  inbound: "bg-brand-50 text-brand-700",
  stock_opname: "bg-amber-50 text-amber-700",
  transfer: "bg-emerald-50 text-emerald-700",
  outbound: "bg-red-50 text-red-700",
};

export default async function ActivitiesPage({
  searchParams,
}: {
  searchParams: Promise<{ type?: string; from?: string; to?: string; page?: string }>;
}) {
  const session = await getSession();
  const params = await searchParams;
  const type = params.type ?? "";
  const from = params.from ?? "";
  const to = params.to ?? "";
  const page = Math.max(1, parseInt(params.page ?? "1", 10) || 1);

  const where: Prisma.ActivityWhereInput = {
    AND: [
      type ? { activityType: type as Prisma.EnumActivityTypeFilter["equals"] } : {},
      from ? { activityTime: { gte: new Date(from) } } : {},
      to ? { activityTime: { lte: new Date(`${to}T23:59:59`) } } : {},
    ],
  };

  const [activities, total] = await Promise.all([
    prisma.activity.findMany({
      where,
      orderBy: { activityTime: "desc" },
      skip: (page - 1) * PAGE_SIZE,
      take: PAGE_SIZE,
      include: { product: { select: { productName: true, sku: true } } },
    }),
    prisma.activity.count({ where }),
  ]);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  function buildPageHref(targetPage: number) {
    const params = new URLSearchParams();
    if (type) params.set("type", type);
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    params.set("page", String(targetPage));
    return `/activities?${params.toString()}`;
  }

  return (
    <AppShell username={session!.username} active="/activities">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Activities</h1>
        <p className="text-sm text-slate-500">{total} logged event{total === 1 ? "" : "s"}</p>
      </div>

      <form method="get" className="mb-4 flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">Type</label>
          <select
            name="type"
            defaultValue={type}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            <option value="">All types</option>
            <option value="inbound">Inbound</option>
            <option value="stock_opname">Stock Opname</option>
            <option value="transfer">Transfer</option>
            <option value="outbound">Outbound</option>
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">From</label>
          <input
            type="date"
            name="from"
            defaultValue={from}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-600">To</label>
          <input
            type="date"
            name="to"
            defaultValue={to}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
          Filter
        </button>
        {(type || from || to) && (
          <Link href="/activities" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50">
            Clear
          </Link>
        )}
      </form>

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <th className="px-4 py-3">Time</th>
              <th className="px-4 py-3">Type</th>
              <th className="px-4 py-3">EPC</th>
              <th className="px-4 py-3">SKU</th>
              <th className="px-4 py-3">Product</th>
              <th className="px-4 py-3">Location</th>
              <th className="px-4 py-3">Qty</th>
            </tr>
          </thead>
          <tbody>
            {activities.length === 0 && (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-400">
                  No activity matches these filters.
                </td>
              </tr>
            )}
            {activities.map((a) => (
              <tr key={a.activityId} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-4 py-3 text-slate-500">{a.activityTime.toLocaleString()}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${ACTIVITY_BADGE[a.activityType]}`}>
                    {ACTIVITY_LABELS[a.activityType]}
                  </span>
                </td>
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  <Link href={`/products/${a.epc}`} className="hover:underline">
                    {a.epc}
                  </Link>
                </td>
                <td className="px-4 py-3 text-slate-600">{a.product.sku}</td>
                <td className="px-4 py-3 text-slate-700">{a.product.productName}</td>
                <td className="px-4 py-3 text-slate-600">{a.location ?? "—"}</td>
                <td className="px-4 py-3 text-slate-600">{a.quantity}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
          <span>
            Page {page} of {totalPages}
          </span>
          <div className="flex gap-2">
            <Link
              href={buildPageHref(Math.max(1, page - 1))}
              aria-disabled={page <= 1}
              className={`rounded-lg border border-slate-300 px-3 py-1.5 ${page <= 1 ? "pointer-events-none opacity-40" : "hover:bg-slate-50"}`}
            >
              Previous
            </Link>
            <Link
              href={buildPageHref(Math.min(totalPages, page + 1))}
              aria-disabled={page >= totalPages}
              className={`rounded-lg border border-slate-300 px-3 py-1.5 ${page >= totalPages ? "pointer-events-none opacity-40" : "hover:bg-slate-50"}`}
            >
              Next
            </Link>
          </div>
        </div>
      )}
    </AppShell>
  );
}
