import { Prisma, PrismaClient } from "@prisma/client";
import { SubmitActivityInput } from "../validation/activitySchemas";

export type ItemStatus = "ACCEPTED" | "UNKNOWN_EPC" | "DUPLICATE_EPC" | "ALREADY_SOLD";

export interface ItemResult {
  epc: string;
  status: ItemStatus;
  reason?: string;
  sku?: string;
  productName?: string;
}

export interface SubmitActivityResponse {
  success: true;
  idempotentReplay: boolean;
  summary: {
    acceptedCount: number;
    rejectedCount: number;
    unknownCount: number;
  };
  items: ItemResult[];
}

function buildSummary(items: ItemResult[]) {
  let acceptedCount = 0;
  let rejectedCount = 0;
  let unknownCount = 0;
  for (const item of items) {
    if (item.status === "ACCEPTED") acceptedCount += 1;
    else if (item.status === "UNKNOWN_EPC") unknownCount += 1;
    else rejectedCount += 1;
  }
  return { acceptedCount, rejectedCount, unknownCount };
}

async function processInbound(
  tx: Prisma.TransactionClient,
  location: string,
  items: SubmitActivityInput["items"],
): Promise<ItemResult[]> {
  const results: ItemResult[] = [];
  const seenInBatch = new Set<string>();

  for (const item of items) {
    const epc = item.epc.toUpperCase();
    if (seenInBatch.has(epc)) {
      results.push({ epc, status: "DUPLICATE_EPC", reason: "Duplicate EPC within this scan batch." });
      continue;
    }
    seenInBatch.add(epc);

    const existing = await tx.product.findUnique({ where: { epc } });
    if (existing) {
      results.push({ epc, status: "DUPLICATE_EPC", reason: "EPC is already registered in the warehouse." });
      continue;
    }

    const quantity = item.quantity ?? 1;
    await tx.product.create({
      data: {
        epc,
        sku: item.sku!,
        productName: item.productName!,
        quantity,
        location,
        status: "available",
      },
    });
    await tx.activity.create({
      data: { epc, activityType: "inbound", location, quantity },
    });
    results.push({ epc, status: "ACCEPTED", sku: item.sku, productName: item.productName });
  }

  return results;
}

async function processStockOpname(
  tx: Prisma.TransactionClient,
  location: string,
  items: SubmitActivityInput["items"],
): Promise<ItemResult[]> {
  const results: ItemResult[] = [];

  for (const item of items) {
    const epc = item.epc.toUpperCase();
    const existing = await tx.product.findUnique({ where: { epc } });
    if (!existing) {
      results.push({ epc, status: "UNKNOWN_EPC", reason: "EPC is not registered in the warehouse." });
      continue;
    }

    await tx.product.update({ where: { epc }, data: { location } });
    await tx.activity.create({
      data: { epc, activityType: "stock_opname", location, quantity: existing.quantity },
    });
    results.push({ epc, status: "ACCEPTED", sku: existing.sku, productName: existing.productName });
  }

  return results;
}

async function processTransfer(
  tx: Prisma.TransactionClient,
  destinationLocation: string,
  items: SubmitActivityInput["items"],
): Promise<ItemResult[]> {
  const results: ItemResult[] = [];

  for (const item of items) {
    const epc = item.epc.toUpperCase();
    const existing = await tx.product.findUnique({ where: { epc } });
    if (!existing) {
      results.push({ epc, status: "UNKNOWN_EPC", reason: "EPC is not registered in the warehouse." });
      continue;
    }

    await tx.product.update({ where: { epc }, data: { location: destinationLocation } });
    await tx.activity.create({
      data: { epc, activityType: "transfer", location: destinationLocation, quantity: existing.quantity },
    });
    results.push({ epc, status: "ACCEPTED", sku: existing.sku, productName: existing.productName });
  }

  return results;
}

async function processOutbound(
  tx: Prisma.TransactionClient,
  location: string | undefined,
  items: SubmitActivityInput["items"],
): Promise<ItemResult[]> {
  const results: ItemResult[] = [];

  for (const item of items) {
    const epc = item.epc.toUpperCase();
    const existing = await tx.product.findUnique({ where: { epc } });
    if (!existing) {
      results.push({ epc, status: "UNKNOWN_EPC", reason: "EPC is not registered in the warehouse." });
      continue;
    }
    if (existing.status === "sold") {
      results.push({ epc, status: "ALREADY_SOLD", reason: "EPC has already been marked sold.", sku: existing.sku, productName: existing.productName });
      continue;
    }

    await tx.product.update({ where: { epc }, data: { status: "sold" } });
    await tx.activity.create({
      data: { epc, activityType: "outbound", location: location ?? existing.location, quantity: existing.quantity },
    });
    results.push({ epc, status: "ACCEPTED", sku: existing.sku, productName: existing.productName });
  }

  return results;
}

export async function processActivityBatch(
  prisma: PrismaClient,
  input: SubmitActivityInput,
): Promise<SubmitActivityResponse> {
  const existingBatch = await prisma.scanBatch.findUnique({ where: { clientBatchId: input.clientBatchId } });
  if (existingBatch) {
    const stored = JSON.parse(existingBatch.responseJson) as SubmitActivityResponse;
    return { ...stored, idempotentReplay: true };
  }

  const items = await prisma.$transaction(async (tx) => {
    switch (input.activityType) {
      case "inbound":
        return processInbound(tx, input.location!, input.items);
      case "stock_opname":
        return processStockOpname(tx, input.location!, input.items);
      case "transfer":
        return processTransfer(tx, input.location!, input.items);
      case "outbound":
        return processOutbound(tx, input.location, input.items);
      default:
        throw new Error(`Unsupported activity type: ${input.activityType}`);
    }
  });

  const summary = buildSummary(items);
  const response: SubmitActivityResponse = { success: true, idempotentReplay: false, summary, items };

  await prisma.scanBatch.create({
    data: {
      clientBatchId: input.clientBatchId,
      activityType: input.activityType,
      location: input.location,
      readerId: input.readerId,
      operatorName: input.operatorName,
      itemCount: input.items.length,
      responseJson: JSON.stringify(response),
    },
  });

  return response;
}
