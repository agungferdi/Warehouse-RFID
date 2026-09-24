import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { getSession } from "@/lib/auth/session";
import { getDashboardStats } from "@/lib/services/statsService";

export async function GET() {
  const session = await getSession();
  if (!session) {
    return NextResponse.json({ success: false, error: { code: "UNAUTHORIZED", message: "Login required." } }, { status: 401 });
  }

  const stats = await getDashboardStats(prisma, { includeRecentActivity: true });
  return NextResponse.json({ success: true, ...stats });
}
