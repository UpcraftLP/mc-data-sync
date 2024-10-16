/*
  Warnings:

  - You are about to drop the column `key` on the `UserConfigData` table. All the data in the column will be lost.
  - Added the required column `entitlementId` to the `UserConfigData` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_UserConfigData" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "minecraftUserId" TEXT NOT NULL,
    "entitlementId" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "UserConfigData_minecraftUserId_fkey" FOREIGN KEY ("minecraftUserId") REFERENCES "MinecraftUser" ("id") ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT "UserConfigData_entitlementId_fkey" FOREIGN KEY ("entitlementId") REFERENCES "Entitlement" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);
INSERT INTO "new_UserConfigData" ("createdAt", "id", "minecraftUserId", "updatedAt", "value") SELECT "createdAt", "id", "minecraftUserId", "updatedAt", "value" FROM "UserConfigData";
DROP TABLE "UserConfigData";
ALTER TABLE "new_UserConfigData" RENAME TO "UserConfigData";
CREATE UNIQUE INDEX "UserConfigData_minecraftUserId_entitlementId_key" ON "UserConfigData"("minecraftUserId", "entitlementId");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;
