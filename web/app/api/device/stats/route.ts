import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { validateDeviceApiKey } from "@/lib/auth/deviceAuth";
import { getDashboardStats } from "@/lib/services/statsService";

export async function GET(request: Request) {
  const auth = validateDeviceApiKey(request.headers);
  if (!auth.ok) return NextResponse.json(auth.body, { status: auth.status });

  const stats = await getDashboardStats(prisma);
  return NextResponse.json({ success: true, ...stats });
}
