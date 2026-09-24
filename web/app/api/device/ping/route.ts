import { NextResponse } from "next/server";
import { validateDeviceApiKey } from "@/lib/auth/deviceAuth";

export async function GET(request: Request) {
  const auth = validateDeviceApiKey(request.headers);
  if (!auth.ok) return NextResponse.json(auth.body, { status: auth.status });

  return NextResponse.json({ success: true, serverTime: new Date().toISOString() });
}
