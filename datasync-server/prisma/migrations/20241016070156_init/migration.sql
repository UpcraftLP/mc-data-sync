-- CreateTable
CREATE TABLE "MinecraftUser" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "bannedAt" DATETIME,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- CreateTable
CREATE TABLE "Entitlement" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- CreateTable
CREATE TABLE "UserConfigData" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "minecraftUserId" TEXT NOT NULL,
    "key" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "UserConfigData_minecraftUserId_fkey" FOREIGN KEY ("minecraftUserId") REFERENCES "MinecraftUser" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "_EntitlementToMinecraftUser" (
    "A" TEXT NOT NULL,
    "B" TEXT NOT NULL,
    CONSTRAINT "_EntitlementToMinecraftUser_A_fkey" FOREIGN KEY ("A") REFERENCES "Entitlement" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "_EntitlementToMinecraftUser_B_fkey" FOREIGN KEY ("B") REFERENCES "MinecraftUser" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateIndex
CREATE UNIQUE INDEX "UserConfigData_minecraftUserId_key_key" ON "UserConfigData"("minecraftUserId", "key");

-- CreateIndex
CREATE UNIQUE INDEX "_EntitlementToMinecraftUser_AB_unique" ON "_EntitlementToMinecraftUser"("A", "B");

-- CreateIndex
CREATE INDEX "_EntitlementToMinecraftUser_B_index" ON "_EntitlementToMinecraftUser"("B");
