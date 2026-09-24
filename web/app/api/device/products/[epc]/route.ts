import { NextResponse } from "next/server";
import { prisma } from "@/lib/db";
import { validateDeviceApiKey } from "@/lib/auth/deviceAuth";

export async function GET(request: Request, { params }: { params: Promise<{ epc: string }> }) {
  const auth = validateDeviceApiKey(request.headers);
  if (!auth.ok) return NextResponse.json(auth.body, { status: auth.status });

  const { epc: rawEpc } = await params;
  const epc = decodeURIComponent(rawEpc).toUpperCase();

  const product = await prisma.product.findUnique({ where: { epc } });
  if (!product) {
    return NextResponse.json({ success: true, found: false });
  }

  return NextResponse.json({
    success: true,
    found: true,
    product: {
      epc: product.epc,
      sku: product.sku,
      productName: product.productName,
      quantity: product.quantity,
      location: product.location,
      status: product.status,
    },
  });
}
