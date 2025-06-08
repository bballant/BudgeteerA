package bballant.budgeteer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `Transaction`")
    fun getAll(): List<Transaction>

    @Insert
    fun insert(transaction: Transaction)

    @Query("DELETE FROM `Transaction`")
    fun deleteAll()
    @Query("DELETE FROM `Transaction` WHERE ingestFile = :fileName")
    fun deleteByIngestFile(fileName: String)
}
