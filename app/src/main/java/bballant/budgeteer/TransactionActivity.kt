package bballant.budgeteer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import bballant.budgeteer.databinding.ActivityTransactionBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.ArrayAdapter

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private lateinit var db: AppDatabase
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize file chooser launcher for selecting CSV files.
        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    // Open the CSV file
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val lines = reader.readLines()
                    reader.close()

                    if (lines.size >= 2) {
                        // Use formatted current time as ingest timestamp and get actual file name from the content resolver
                        
                        val now = Instant.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                        val ingestTimestamp = formatter.format(now)

                        var ingestFile = "unknown.csv"
                        if (uri.scheme == "content") {
                            val cursor = contentResolver.query(uri, null, null, null, null)
                            cursor?.use {
                                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1 && it.moveToFirst()) {
                                    ingestFile = it.getString(nameIndex)
                                }
                            }
                        } else {
                            ingestFile = uri.lastPathSegment ?: "unknown.csv"
                        }

                        // Parse CSV rows (skip header; load all data rows)
                        val transactions = mutableListOf<Transaction>()
                        for (line in lines.drop(1)) {
                            // Assuming CSV format: "Date","Transaction","Name","Memo","Amount"
                            val tokens = line.split(",")
                            if (tokens.size >= 5) {
                                val date = tokens[0].trim('"')
                                val name = tokens[2].trim('"')  // "Name" maps to "description"
                                val amount = tokens[4].trim('"').toDoubleOrNull() ?: 0.0
                                transactions.add(
                                    Transaction(
                                        date = date,
                                        description = name,
                                        amount = amount,
                                        ingestTimestamp = ingestTimestamp,
                                        ingestFile = ingestFile
                                    )
                                )
                            }
                        }

                        // Insert new transactions only if this file has not been ingested before
                        val alreadyIngested = db.transactionDao().getAll().any { it.ingestFile == ingestFile }
                        if (alreadyIngested) {
                            binding.tvCsvPreview.text = "File '$ingestFile' already ingested."
                        } else {
                            transactions.forEach {
                                db.transactionDao().insert(it)
                            }
                        }

                        // Refresh the TableView with the updated transactions
                        val updatedTransactions = db.transactionDao().getAll()
                        val columnHeaders = listOf("ID", "Date", "Description", "Amount", "Ingest Timestamp", "Ingest File")
                        val rowHeaders = mutableListOf<String>()
                        val cellData = mutableListOf<List<String>>()
                        updatedTransactions.forEach {
                            //rowHeaders.add(it.id.toString())
                            cellData.add(listOf(
                                it.id.toString(),
                                it.date,
                                it.description,
                                it.amount.toString(),
                                it.ingestTimestamp,
                                it.ingestFile
                            ))
                        }
                        binding.tvCsvPreview.text = "Transactions (${updatedTransactions.size})"
                        val adapter = TransactionTableAdapter(cellData, columnHeaders, rowHeaders)
                        adapter.setTableView(binding.tableView)
                        binding.tableView.setAdapter(adapter)
                        updateSpinner()
                    } else {
                        binding.tvCsvPreview.text = "CSV file does not have enough lines."
                    }
                } catch (e: Exception) {
                    binding.tvCsvPreview.text = "Error reading file: ${e.message}"
                }
            }
        }

        // Set up the file chooser button to launch file picker
        binding.btnChooseFile.setOnClickListener {
            filePickerLauncher.launch("text/*")
        }

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "budgeteer-db")
            .allowMainThreadQueries()
            .build()

        // (Removed sample data insertion; data will now be loaded via CSV file selection.)
        
        // Fetch transactions and prepare data for the TableView
        val transactionsList = db.transactionDao().getAll()
        if (transactionsList.isNotEmpty()) {
            binding.tvCsvPreview.text = "Transactions (${transactionsList.size})"
        }

        val columnHeaders = listOf("ID", "Date", "Description", "Amount", "Ingest Timestamp", "Ingest File")
        val rowHeaders = mutableListOf<String>()
        val cellData = mutableListOf<List<String>>()
        transactionsList.forEachIndexed { index, transaction ->
            rowHeaders.add((index + 1).toString())
            cellData.add(listOf(
                transaction.id.toString(),
                transaction.date,
                transaction.description,
                transaction.amount.toString(),
                transaction.ingestTimestamp,
                transaction.ingestFile
            ))
        }
        // Set up the TableView with our custom adapter
        val adapter = TransactionTableAdapter(cellData, columnHeaders, rowHeaders)
        adapter.setTableView(binding.tableView)
        binding.tableView.setAdapter(adapter)

        // Populate spinner with distinct ingestFile values from database.
        val allTransactions = db.transactionDao().getAll()
        val fileNames = allTransactions.map { it.ingestFile }.distinct().filter { it.isNotBlank() }
        spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, fileNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFileNames.adapter = spinnerAdapter

        // Set delete button listener to delete data for the selected file.
        binding.btnDeleteFileData.setOnClickListener {
            val selectedFile = binding.spinnerFileNames.selectedItem as? String
            if (selectedFile != null) {
                db.transactionDao().deleteByIngestFile(selectedFile)
                // Refresh TableView with updated transactions.
                val updatedTransactions = db.transactionDao().getAll()
                val newRowHeaders = mutableListOf<String>()
                val newCellData = mutableListOf<List<String>>()
                updatedTransactions.forEachIndexed { index, transaction ->
                    newRowHeaders.add((index + 1).toString())
                    newCellData.add(listOf(
                        transaction.id.toString(),
                        transaction.date,
                        transaction.description,
                        transaction.amount.toString(),
                        transaction.ingestTimestamp,
                        transaction.ingestFile
                    ))
                }
                binding.tvCsvPreview.text = "Transactions (${updatedTransactions.size})"
                val newAdapter = TransactionTableAdapter(newCellData, columnHeaders, newRowHeaders)
                newAdapter.setTableView(binding.tableView)
                binding.tableView.setAdapter(newAdapter)
                updateSpinner()
            }
        }
    }

    private fun updateSpinner() {
        val updatedTransactions = db.transactionDao().getAll()
        val updatedFileNames = updatedTransactions.map { it.ingestFile }
            .distinct().filter { it.isNotBlank() }
        spinnerAdapter.clear()
        spinnerAdapter.addAll(updatedFileNames)
        spinnerAdapter.notifyDataSetChanged()
    }
}
