import { NextResponse } from "next/server";
import { ZodError } from "zod";
import { prisma } from "@/lib/db";
import { validateDeviceApiKey } from "@/lib/auth/deviceAuth";
import { submitActivitySchema } from "@/lib/validation/activitySchemas";
import { processActivityBatch } from "@/lib/services/activityProcessing";

export async function POST(request: Request) {
  const auth = validateDeviceApiKey(request.headers);
  if (!auth.ok) return NextResponse.json(auth.body, { status: auth.status });

  try {
    const json = await request.json();
    const input = submitActivitySchema.parse(json);
    const response = await processActivityBatch(prisma, input);
    return NextResponse.json(response, { status: 200 });
  } catch (error) {
    if (error instanceof ZodError) {
      return NextResponse.json(
        { success: false, error: { code: "VALIDATION_ERROR", message: "Invalid activity payload.", details: error.flatten() } },
        { status: 400 },
      );
    }

    console.error("device/activities error", error);
    return NextResponse.json(
      { success: false, error: { code: "SERVER_ERROR", message: "Unable to process activity batch." } },
      { status: 500 },
    );
  }
}
