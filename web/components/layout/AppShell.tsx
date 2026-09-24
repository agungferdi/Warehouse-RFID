import Link from "next/link";
import { LogoutButton } from "./LogoutButton";

const NAV_ITEMS = [
  { href: "/", label: "Dashboard" },
  { href: "/products", label: "Products" },
  { href: "/activities", label: "Activities" },
];

export function AppShell({ username, active, children }: { username: string; active: string; children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <aside className="w-64 shrink-0 border-r border-slate-200 bg-white px-5 py-6">
        <div className="mb-8 flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-white font-semibold">R</div>
          <div>
            <p className="text-sm font-semibold text-slate-900">RFID Warehouse</p>
            <p className="text-xs text-slate-500">Operations Console</p>
          </div>
        </div>
        <nav className="flex flex-col gap-1">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                active === item.href ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="mt-10 border-t border-slate-200 pt-4">
          <p className="px-3 text-xs text-slate-500">Signed in as</p>
          <p className="px-3 text-sm font-medium text-slate-800">{username}</p>
          <div className="mt-3 px-1">
            <LogoutButton />
          </div>
        </div>
      </aside>
      <main className="flex-1 bg-slate-50 px-8 py-8">{children}</main>
    </div>
  );
}
