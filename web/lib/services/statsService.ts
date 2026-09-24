import { PrismaClient } from "@prisma/client";

export interface DashboardStats {
  productsByStatus: {
    available: number;
    sold: number;
    in_transit: number;
  };
  totalProducts: number;
  activityLast7Days: Array<{
    date: string;
    inbound: number;
    stock_opname: number;
    transfer: number;
    outbound: number;
  }>;
  recentActivity: Array<{
    activityId: number;
    epc: string;
    activityType: string;
    location: string | null;
    quantity: number;
    activityTime: string;
    productName: string;
  }>;
}

function emptyDay(date: string) {
  return { date, inbound: 0, stock_opname: 0, transfer: 0, outbound: 0 };
}

export async function getDashboardStats(prisma: PrismaClient, options?: { includeRecentActivity?: boolean }): Promise<DashboardStats> {
  const [statusCounts, totalProducts, rawDaily, recent] = await Promise.all([
    prisma.product.groupBy({ by: ["status"], _count: { _all: true } }),
    prisma.product.count(),
    prisma.$queryRaw<Array<{ day: Date; activity_type: string; count: bigint }>>`
      SELECT date_trunc('day', activity_time) AS day, activity_type, COUNT(*)::bigint AS count
      FROM activities
      WHERE activity_time >= NOW() - INTERVAL '7 days'
      GROUP BY day, activity_type
      ORDER BY day ASC
    `,
    options?.includeRecentActivity
      ? prisma.activity.findMany({
          orderBy: { activityTime: "desc" },
          take: 20,
          include: { product: { select: { productName: true } } },
        })
      : Promise.resolve([]),
  ]);

  const productsByStatus = { available: 0, sold: 0, in_transit: 0 };
  for (const row of statusCounts) {
    productsByStatus[row.status] = row._count._all;
  }

  const dayMap = new Map<string, ReturnType<typeof emptyDay>>();
  const today = new Date();
  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    dayMap.set(key, emptyDay(key));
  }
  for (const row of rawDaily) {
    const key = row.day.toISOString().slice(0, 10);
    const entry = dayMap.get(key);
    if (entry && row.activity_type in entry) {
      (entry as any)[row.activity_type] = Number(row.count);
    }
  }

  return {
    productsByStatus,
    totalProducts,
    activityLast7Days: Array.from(dayMap.values()),
    recentActivity: recent.map((a) => ({
      activityId: a.activityId,
      epc: a.epc,
      activityType: a.activityType,
      location: a.location,
      quantity: a.quantity,
      activityTime: a.activityTime.toISOString(),
      productName: a.product.productName,
    })),
  };
}
