import Link from "next/link";
import { Prisma } from "@prisma/client";
import { getSession } from "@/lib/auth/session";
import { prisma } from "@/lib/db";
import { AppShell } from "@/components/layout/AppShell";

const PAGE_SIZE = 20;

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

export default async function ProductsPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; status?: string; location?: string; page?: string }>;
}) {
  const session = await getSession();
  const params = await searchParams;
  const q = params.q?.trim() ?? "";
  const status = params.status ?? "";
  const location = params.location?.trim() ?? "";
  const page = Math.max(1, parseInt(params.page ?? "1", 10) || 1);

  const where: Prisma.ProductWhereInput = {
    AND: [
      q
        ? {
            OR: [
              { epc: { contains: q, mode: "insensitive" } },
              { sku: { contains: q, mode: "insensitive" } },
              { productName: { contains: q, mode: "insensitive" } },
            ],
          }
        : {},
      status ? { status: status as Prisma.EnumProductStatusFilter["equals"] } : {},
      location ? { location: { contains: location, mode: "insensitive" } } : {},
    ],
  };

  const [products, total, locations] = await Promise.all([
    prisma.product.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * PAGE_SIZE,
      take: PAGE_SIZE,
    }),
    prisma.product.count({ where }),
    prisma.product.findMany({ distinct: ["location"], select: { location: true } }),
  ]);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  function buildPageHref(targetPage: number) {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (status) params.set("status", status);
    if (location) params.set("location", location);
    params.set("page", String(targetPage));
    return `/products?${params.toString()}`;
  }

  return (
    <AppShell username={session!.username} active="/products">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Products</h1>
        <p className="text-sm text-slate-500">{total} tagged item{total === 1 ? "" : "s"} in the warehouse</p>
      </div>

      <form method="get" className="mb-4 flex flex-wrap gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <input
          type="text"
          name="q"
          defaultValue={q}
          placeholder="Search EPC, SKU, or name"
          className="min-w-[220px] flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
        <select
          name="status"
          defaultValue={status}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        >
          <option value="">All statuses</option>
          <option value="available">Available</option>
          <option value="sold">Sold</option>
          <option value="in_transit">In Transit</option>
        </select>
        <select
          name="location"
          defaultValue={location}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        >
          <option value="">All locations</option>
          {locations
            .filter((l) => l.location)
            .map((l) => (
              <option key={l.location} value={l.location!}>
                {l.location}
              </option>
            ))}
        </select>
        <button type="submit" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
          Filter
        </button>
        {(q || status || location) && (
          <Link href="/products" className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50">
            Clear
          </Link>
        )}
      </form>

      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <th className="px-4 py-3">EPC</th>
              <th className="px-4 py-3">SKU</th>
              <th className="px-4 py-3">Product</th>
              <th className="px-4 py-3">Qty</th>
              <th className="px-4 py-3">Location</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Updated</th>
            </tr>
          </thead>
          <tbody>
            {products.length === 0 && (
              <tr>
                <td colSpan={7} className="py-8 text-center text-slate-400">
                  No products match these filters.
                </td>
              </tr>
            )}
            {products.map((p) => (
              <tr key={p.epc} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                <td className="px-4 py-3 font-mono text-xs text-slate-600">
                  <Link href={`/products/${p.epc}`} className="hover:underline">
                    {p.epc}
                  </Link>
                </td>
                <td className="px-4 py-3 text-slate-700">{p.sku}</td>
                <td className="px-4 py-3 text-slate-700">{p.productName}</td>
                <td className="px-4 py-3 text-slate-600">{p.quantity}</td>
                <td className="px-4 py-3 text-slate-600">{p.location ?? "—"}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[p.status]}`}>
                    {STATUS_LABELS[p.status]}
                  </span>
                </td>
                <td className="px-4 py-3 text-slate-500">{p.updatedAt.toLocaleString()}</td>
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
