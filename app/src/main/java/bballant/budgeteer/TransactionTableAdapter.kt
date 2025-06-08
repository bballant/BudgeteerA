package bballant.budgeteer

import com.evrencoskun.tableview.ITableView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.evrencoskun.tableview.adapter.AbstractTableAdapter
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder

// Simple data models for table components
data class ColumnHeader(val data: String)
data class RowHeader(val data: String)
data class Cell(val data: String)

class TransactionTableAdapter(
    cellData: List<List<String>>,
    columnHeaders: List<String>,
    rowHeaders: List<String>
) : AbstractTableAdapter<ColumnHeader, RowHeader, Cell>() {

    private val columnHeaderList = columnHeaders.map { ColumnHeader(it) }
    private val rowHeaderList = rowHeaders.map { RowHeader(it) }
    private val cellList = cellData.map { row -> row.map { Cell(it) } }

    // Custom Cell ViewHolder
    inner class MyCellViewHolder(view: View) : AbstractViewHolder(view) {
        val cellContainer: LinearLayout = view.findViewById(R.id.cell_container)
        val cellTextView: TextView = view.findViewById(R.id.cell_data)
    }

    override fun onCreateCellViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_view_cell_layout, parent, false)
        return MyCellViewHolder(layout)
    }

    override fun onBindCellViewHolder(holder: AbstractViewHolder, cellItemModel: Cell?, columnPosition: Int, rowPosition: Int) {
        val cell = cellList.getOrNull(rowPosition)?.getOrNull(columnPosition) ?: Cell("")
        val viewHolder = holder as MyCellViewHolder
        viewHolder.cellTextView.text = cell.data
        // Auto-resize adjustment
        viewHolder.cellContainer.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        viewHolder.cellTextView.requestLayout()
    }

    // Custom Column Header ViewHolder
    inner class MyColumnHeaderViewHolder(view: View) : AbstractViewHolder(view) {
        val columnHeaderContainer: LinearLayout = view.findViewById(R.id.column_header_container)
        val columnHeaderTextView: TextView = view.findViewById(R.id.column_header_textView)
    }

    override fun onCreateColumnHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_view_column_header_layout, parent, false)
        return MyColumnHeaderViewHolder(layout)
    }

    override fun onBindColumnHeaderViewHolder(holder: AbstractViewHolder, columnHeaderItemModel: ColumnHeader?, position: Int) {
        val columnHeader = columnHeaderList.getOrNull(position) ?: ColumnHeader("")
        val viewHolder = holder as MyColumnHeaderViewHolder
        viewHolder.columnHeaderTextView.text = columnHeader.data
        viewHolder.columnHeaderContainer.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        viewHolder.columnHeaderTextView.requestLayout()
    }

    // Custom Row Header ViewHolder
    inner class MyRowHeaderViewHolder(view: View) : AbstractViewHolder(view) {
        val rowHeaderTextView: TextView = view.findViewById(R.id.row_header_textview)
    }

    override fun onCreateRowHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_view_row_header_layout, parent, false)
        return MyRowHeaderViewHolder(layout)
    }

    override fun onBindRowHeaderViewHolder(holder: AbstractViewHolder, rowHeaderItemModel: RowHeader?, position: Int) {
        val rowHeader = rowHeaderList.getOrNull(position) ?: RowHeader("")
        val viewHolder = holder as MyRowHeaderViewHolder
        viewHolder.rowHeaderTextView.text = rowHeader.data
    }

    override fun onCreateCornerView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.table_view_corner_layout, parent, false)
    }

    override fun getColumnHeaderItemViewType(columnPosition: Int): Int = 0

    override fun getRowHeaderItemViewType(rowPosition: Int): Int = 0

    override fun getCellItemViewType(columnPosition: Int): Int = 0

    override fun setTableView(tableView: ITableView) {
        super.setTableView(tableView)
        setAllItems(columnHeaderList, rowHeaderList, cellList)
    }
}
