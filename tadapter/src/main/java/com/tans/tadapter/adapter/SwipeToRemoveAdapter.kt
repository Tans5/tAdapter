package com.tans.tadapter.adapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.tans.tadapter.spec.AdapterSpec

/**
 *
 * author: pengcheng.tan
 * date: 2019-09-25
 */
class SwipeToRemoveAdapter<D, Binding : ViewDataBinding>(
    adapterSpec: AdapterSpec<D, Binding>,
    val deleteIcon: Drawable? = null,
    val background: Drawable
) : BaseAdapter<D, Binding>(adapterSpec) {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val itemTouchHelper = ItemTouchHelper(
            SwipeToRemoveCallBack(
                deleteIcon = deleteIcon,
                background = background,
                removeCallBack = { position -> adapterSpec.swipeRemove(position, getItem(position)) }
            )
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

}

class SwipeToRemoveCallBack(
    val deleteIcon: Drawable?,
    val background: Drawable,
    val removeCallBack: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {

    val clearPaint: Paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // Calculate position of delete icon
        val deleteIconTop = itemView.top + (itemHeight - (deleteIcon?.intrinsicHeight ?: 0)) / 2
        val deleteIconMargin = (itemHeight - (deleteIcon?.intrinsicHeight ?: 0)) / 2
        val deleteIconLeft =
            itemView.right - deleteIconMargin / 2 - (deleteIcon?.intrinsicWidth ?: 0)
        val deleteIconRight = itemView.right - deleteIconMargin / 2
        val deleteIconBottom = deleteIconTop + (deleteIcon?.intrinsicHeight ?: 0)

        // Draw the delete icon
        deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon?.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        removeCallBack(viewHolder.adapterPosition)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }


}