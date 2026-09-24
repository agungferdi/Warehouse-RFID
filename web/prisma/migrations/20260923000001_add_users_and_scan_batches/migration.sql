-- CreateTable
CREATE TABLE "users" (
    "id" TEXT NOT NULL,
    "username" VARCHAR(64) NOT NULL,
    "password_hash" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "scan_batches" (
    "id" TEXT NOT NULL,
    "client_batch_id" TEXT NOT NULL,
    "activity_type" "activity_type_enum" NOT NULL,
    "location" VARCHAR(100),
    "reader_id" VARCHAR(64),
    "operator_name" VARCHAR(100),
    "item_count" INTEGER NOT NULL,
    "response_json" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "scan_batches_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_username_key" ON "users"("username");

-- CreateIndex
CREATE UNIQUE INDEX "scan_batches_client_batch_id_key" ON "scan_batches"("client_batch_id");
