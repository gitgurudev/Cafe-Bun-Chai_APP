package com.cafebunchai.pos.data

import android.content.Context
import com.cafebunchai.pos.data.auth.AuthRepository
import com.cafebunchai.pos.data.backup.BackupManager
import com.cafebunchai.pos.data.cloud.FirestoreCafeSync
import com.cafebunchai.pos.data.local.AppDatabase
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import com.cafebunchai.pos.data.repo.RoomOrderInventoryRepository

class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.get(context)
    private val cloud = FirestoreCafeSync()
    val stockRepo = RoomOrderInventoryRepository(db, cloud)
    val repository: OrderInventoryRepository = stockRepo
    val backup: BackupManager = BackupManager(db)
    val auth = AuthRepository()
}
