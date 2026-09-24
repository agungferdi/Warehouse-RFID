-- CreateEnum
CREATE TYPE "activity_type_enum" AS ENUM ('inbound', 'stock_opname', 'transfer', 'outbound');

-- CreateEnum
CREATE TYPE "product_status" AS ENUM ('available', 'sold', 'in_transit');

-- CreateTable
CREATE TABLE "activities" (
    "activity_id" SERIAL NOT NULL,
    "epc" VARCHAR(64) NOT NULL,
    "activity_type" "activity_type_enum" NOT NULL,
    "location" VARCHAR(100),
    "quantity" INTEGER NOT NULL DEFAULT 1,
    "activity_time" TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "activities_pkey" PRIMARY KEY ("activity_id")
);

-- CreateTable
CREATE TABLE "products" (
    "epc" VARCHAR(64) NOT NULL,
    "sku" VARCHAR(64) NOT NULL,
    "product_name" VARCHAR(255) NOT NULL,
    "quantity" INTEGER NOT NULL DEFAULT 1,
    "location" VARCHAR(100),
    "status" "product_status" NOT NULL DEFAULT 'available',
    "created_at" TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "products_pkey" PRIMARY KEY ("epc")
);

-- CreateIndex
CREATE INDEX "idx_activities_epc" ON "activities"("epc" ASC);

-- CreateIndex
CREATE INDEX "idx_activities_type" ON "activities"("activity_type" ASC);

-- CreateIndex
CREATE INDEX "idx_products_sku" ON "products"("sku" ASC);

-- CreateIndex
CREATE INDEX "idx_products_status" ON "products"("status" ASC);

-- AddForeignKey
ALTER TABLE "activities" ADD CONSTRAINT "activities_epc_fkey" FOREIGN KEY ("epc") REFERENCES "products"("epc") ON DELETE NO ACTION ON UPDATE NO ACTION;

