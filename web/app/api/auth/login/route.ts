import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/db";
import { createSessionToken, setSessionCookie } from "@/lib/auth/session";

const loginSchema = z.object({
  username: z.string().trim().min(1),
  password: z.string().min(1),
});

export async function POST(request: Request) {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ success: false, error: { code: "INVALID_BODY", message: "Request body must be JSON." } }, { status: 400 });
  }

  const parsed = loginSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ success: false, error: { code: "VALIDATION_ERROR", message: "Username and password are required." } }, { status: 400 });
  }

  const user = await prisma.user.findUnique({ where: { username: parsed.data.username } });
  if (!user) {
    return NextResponse.json({ success: false, error: { code: "INVALID_CREDENTIALS", message: "Invalid username or password." } }, { status: 401 });
  }

  const passwordMatches = await bcrypt.compare(parsed.data.password, user.passwordHash);
  if (!passwordMatches) {
    return NextResponse.json({ success: false, error: { code: "INVALID_CREDENTIALS", message: "Invalid username or password." } }, { status: 401 });
  }

  const token = await createSessionToken({ userId: user.id, username: user.username });
  await setSessionCookie(token);

  return NextResponse.json({ success: true });
}
