import { z } from "zod";

export const activityTypeSchema = z.enum(["inbound", "stock_opname", "transfer", "outbound"]);

const baseItemSchema = z.object({
  epc: z.string().trim().min(1).max(64),
  sku: z.string().trim().min(1).max(64).optional(),
  productName: z.string().trim().min(1).max(255).optional(),
  quantity: z.number().int().positive().optional(),
});

export const submitActivitySchema = z
  .object({
    clientBatchId: z.string().trim().min(1).max(128),
    activityType: activityTypeSchema,
    location: z.string().trim().min(1).max(100).optional(),
    readerId: z.string().trim().max(64).optional(),
    operatorName: z.string().trim().max(100).optional(),
    items: z.array(baseItemSchema).min(1, "At least one scanned tag is required."),
  })
  .superRefine((data, ctx) => {
    if (data.activityType === "inbound") {
      data.items.forEach((item, index) => {
        if (!item.sku) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, message: "sku is required for inbound items.", path: ["items", index, "sku"] });
        }
        if (!item.productName) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, message: "productName is required for inbound items.", path: ["items", index, "productName"] });
        }
      });
    }
    if ((data.activityType === "stock_opname" || data.activityType === "inbound" || data.activityType === "transfer") && !data.location) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: "location is required for this activity type.", path: ["location"] });
    }
  });

export type SubmitActivityInput = z.infer<typeof submitActivitySchema>;
