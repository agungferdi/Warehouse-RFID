import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { validateDeviceApiKey } from "@/lib/auth/deviceAuth";

export async function GET(request: Request) {
  const auth = validateDeviceApiKey(request.headers);
  if (!auth.ok) return NextResponse.json(auth.body, { status: auth.status });

  const rows = await prisma.product.findMany({
    where: { location: { not: null } },
    distinct: ["location"],
    select: { location: true },
    orderBy: { location: "asc" },
  });

  const locations = rows.map((r) => r.location).filter((l): l is string => Boolean(l));
  return NextResponse.json({ success: true, locations });
}
