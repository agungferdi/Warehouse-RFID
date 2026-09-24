import Link from "next/link";
import { notFound } from "next/navigation";
import { getSession } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AppShell } from "@/components/layout/AppShell";

const STATUS_LABELS: Record<string, string> = {
  available: "Available",
  sold: "Sold",
  in_transit: "In Transit",
};

const STATUS_BADGE: Record<string, string> = {
  available: "bg-brand-50 text-brand-700",
  sold: "bg-slate-100 text-slate-600",
  in_transit: "bg-amber-50 text-amber-700",
};

const ACTIVITY_LABELS: Record<string, string> = {
  inbound: "Inbound",
  stock_opname: "Stock Opname",
  transfer: "Transfer",
  outbound: "Outbound",
};

export default async function ProductDetailPage({ params }: { params: Promise<{ epc: string }> }) {
  const session = await getSession();
  const { epc: rawEpc } = await params;
  const epc = decodeURIComponent(rawEpc).toUpperCase();

  const product = await prisma.product.findUnique({
    where: { epc },
    include: { activities: { orderBy: { activityTime: "desc" } } },
  });

  if (!product) notFound();

  return (
    <AppShell username={session!.username} active="/products">
      <div className="mb-4">
        <Link href="/products" className="text-sm text-brand-600 hover:underline">
          ← Back to Products
        </Link>
      </div>

      <div className="mb-6 grid grid-cols-1 gap-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">EPC</p>
          <p className="mt-1 font-mono text-sm text-slate-800">{product.epc}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">SKU</p>
          <p className="mt-1 text-sm text-slate-800">{product.sku}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Product</p>
          <p className="mt-1 text-sm text-slate-800">{product.productName}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</p>
          <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[product.status]}`}>
            {STATUS_LABELS[product.status]}
          </span>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Quantity</p>
          <p className="mt-1 text-sm text-slate-800">{product.quantity}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Location</p>
          <p className="mt-1 text-sm text-slate-800">{product.location ?? "—"}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Created</p>
          <p className="mt-1 text-sm text-slate-800">{product.createdAt.toLocaleString()}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Last Updated</p>
          <p className="mt-1 text-sm text-slate-800">{product.updatedAt.toLocaleString()}</p>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-slate-800">Activity History</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                <th className="py-2 pr-4">Time</th>
                <th className="py-2 pr-4">Type</th>
                <th className="py-2 pr-4">Location</th>
                <th className="py-2 pr-4">Quantity</th>
              </tr>
            </thead>
            <tbody>
              {product.activities.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-slate-400">
                    No activity recorded for this item yet.
                  </td>
                </tr>
              )}
              {product.activities.map((a) => (
                <tr key={a.activityId} className="border-b border-slate-100 last:border-0">
                  <td className="py-2 pr-4 text-slate-500">{a.activityTime.toLocaleString()}</td>
                  <td className="py-2 pr-4">
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
                      {ACTIVITY_LABELS[a.activityType] ?? a.activityType}
                    </span>
                  </td>
                  <td className="py-2 pr-4 text-slate-600">{a.location ?? "—"}</td>
                  <td className="py-2 pr-4 text-slate-600">{a.quantity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AppShell>
  );
}
