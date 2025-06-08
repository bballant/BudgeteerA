package bballant.budgeteer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val description: String,
    val amount: Double,
    val ingestTimestamp: String = "",
    val ingestFile: String = ""
)
