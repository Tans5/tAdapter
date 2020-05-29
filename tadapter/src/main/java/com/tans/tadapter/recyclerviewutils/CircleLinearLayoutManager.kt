package com.tans.tadapter.recyclerviewutils

import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

/**
 *
 * author: pengcheng.tan
 * date: 2019-12-18
 */

class CircleLinearLayoutManager(val circleView: Boolean = false,
                                val orientation: Int = RecyclerView.VERTICAL)
    : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {

    private val orientationHelper: OrientationHelper =
        OrientationHelper.createOrientationHelper(this, orientation)

    private val anchorInfo: AnchorInfo = AnchorInfo(
        position = INVALID_INT,
        coordinate = INVALID_INT,
        layoutDirection = LayoutDirection.ToEnd
    )

    private val layoutState: LayoutState = LayoutState(
        offset = INVALID_INT,
        available = INVALID_INT,
        layoutDirection = LayoutDirection.ToEnd,
        currentPosition = INVALID_INT,
        isPreLayout = false,
        lastScrollDela = INVALID_INT,
        scrollOffset = INVALID_INT,
        scrapList = emptyList()
    )

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return if (orientation == RecyclerView.HORIZONTAL) {
            PointF(direction.toFloat(), 0f)
        } else {
            PointF(0f, direction.toFloat())
        }
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return if (orientation == RecyclerView.HORIZONTAL) scrollBy(dx, recycler, state) else 0
    }

    override fun canScrollVertically(): Boolean {
        return orientation == RecyclerView.VERTICAL
    }

    override fun canScrollHorizontally(): Boolean {
        return orientation == RecyclerView.HORIZONTAL
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return if (orientation == RecyclerView.VERTICAL) scrollBy(dy, recycler, state) else 0
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {

        layoutState.isPreLayout = state.isPreLayout

        anchorInfo.layoutDirection = when {
            layoutState.lastScrollDela == INVALID_INT -> {
                LayoutDirection.ToEnd
            }
            layoutState.lastScrollDela < 0 -> {
                LayoutDirection.ToStart
            }
            else -> {
                LayoutDirection.ToEnd
            }

        }

        updateAnchorByLayout(state, anchorInfo.layoutDirection)

        detachAndScrapAttachedViews(recycler)

        if (anchorInfo.layoutDirection == LayoutDirection.ToStart) {
            updateLayoutStateToStart(anchorInfo.position, anchorInfo.coordinate)
            fill(recycler, layoutState, state)
            var startOffset = layoutState.offset
            val firstElement = layoutState.currentPosition
            val extraAvailable = max(0, layoutState.available)
            updateLayoutStateToFillEnd(anchorInfo.position, anchorInfo.coordinate)
            layoutState.nextPosition(state, circleView)
            layoutState.available += extraAvailable
            fill(recycler, layoutState, state)
            var endOffset = layoutState.offset
            if (layoutState.available > 0) {
                val endAvailable = layoutState.available
                updateLayoutStateToStart(startOffset, firstElement)
                layoutState.available += endAvailable
                fill(recycler, layoutState, state)
            }

            val fixedStartOffset = fixLayoutStartGap(startOffset, recycler, state)
            startOffset -= fixedStartOffset
            endOffset -= fixedStartOffset
            val fixedEndOffset = fixLayoutEndGap(endOffset, recycler, state)
            startOffset += fixedEndOffset
            endOffset += fixedEndOffset

        } else {
            updateLayoutStateToFillEnd(anchorInfo.position, anchorInfo.coordinate)
            fill(recycler, layoutState, state)
            var endOffset = layoutState.offset
            val lastElement = layoutState.currentPosition
            val extraAvailable = max(0, layoutState.available)
            updateLayoutStateToStart(anchorInfo.position, anchorInfo.coordinate)
            layoutState.nextPosition(state, circleView)
            layoutState.available += extraAvailable
            fill(recycler, layoutState, state)
            var startOffset = layoutState.offset

            if (layoutState.available > 0) {
                val startAvailable = layoutState.available
                updateLayoutStateToFillEnd(lastElement, endOffset)
                layoutState.available += startAvailable
                fill(recycler, layoutState, state)
                endOffset = layoutState.offset
            }

            val fixedStartOffset = fixLayoutStartGap(startOffset, recycler, state)
            startOffset -= fixedStartOffset
            endOffset -= fixedStartOffset
            val fixedEndOffset = fixLayoutEndGap(endOffset, recycler, state)
            startOffset += fixedEndOffset
            endOffset += fixedEndOffset
        }

        if (!state.isPreLayout) {
            orientationHelper.onLayoutComplete()
        } else {
            anchorInfo.reset()
        }
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    fun scrollBy(delta: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount <= 0 || delta == 0) {
            return 0
        }
        updateLayoutStateByScroll(delta, state)
        val consumed = layoutState.scrollOffset + fill(recycler, layoutState, state)
        if (consumed < 0) {
            return 0
        }
        val absDelta = abs(delta)
        val directionInt = if (delta < 0) -1 else 1
        val scrolled = if (absDelta > consumed) consumed * directionInt else delta
        layoutState.lastScrollDela = scrolled
        orientationHelper.offsetChildren(-scrolled)
        return scrolled
    }

    fun updateLayoutStateByScroll(scrollDelta: Int, state: RecyclerView.State) {
        val layoutDirection = if (scrollDelta < 0) {
            LayoutDirection.ToStart
        } else {
            LayoutDirection.ToEnd
        }
        layoutState.layoutDirection = layoutDirection
        when (layoutDirection) {
            LayoutDirection.ToStart -> {
                val refView = getChildClosestToStart()
                layoutState.currentPosition = getPosition(refView!!)
                layoutState.nextPosition(state, circleView)
                layoutState.offset = orientationHelper.getDecoratedStart(refView)
                layoutState.scrollOffset =
                    -orientationHelper.getDecoratedStart(refView) + orientationHelper.startAfterPadding
            }
            LayoutDirection.ToEnd -> {
                val refView = getChildClosestToEnd()
                layoutState.currentPosition = getPosition(refView!!)
                layoutState.nextPosition(state, circleView)
                layoutState.offset = orientationHelper.getDecoratedEnd(refView)
                layoutState.scrollOffset =
                    orientationHelper.getDecoratedEnd(refView) - orientationHelper.endAfterPadding
            }
        }
        layoutState.available = abs(scrollDelta) - layoutState.scrollOffset
        if (layoutState.available < 0) {
            layoutState.scrollOffset += layoutState.available
        }
    }

    fun recycleFromStart(offset: Int, recycler: RecyclerView.Recycler) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (orientationHelper.getDecoratedEnd(child) > offset
                || orientationHelper.getTransformedEndWithDecoration(child) > offset
            ) {
                recycleChildren(recycler, 0, i)
                return
            }
        }
    }

    fun recycleFromEnd(offset: Int, recycler: RecyclerView.Recycler) {
        val limit = orientationHelper.end - offset
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (orientationHelper.getDecoratedStart(child) < limit
                || orientationHelper.getTransformedStartWithDecoration(child) < limit
            ) {
                recycleChildren(recycler, childCount - 1, i)
                return
            }
        }
    }

    private fun fixLayoutStartGap(
        startOffset: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val gap = startOffset - orientationHelper.startAfterPadding
        var fixedOffset = 0
        if (gap > 0) {
            fixedOffset = scrollBy(gap, recycler, state)
        } else {
            return 0
        }
        val remainingStartOffset = startOffset - fixedOffset
        val remainingGap = remainingStartOffset - orientationHelper.startAfterPadding
        if (remainingGap > 0) {
            orientationHelper.offsetChildren(-remainingGap)
            return fixedOffset + remainingGap
        }
        return fixedOffset
    }

    private fun fixLayoutEndGap(
        endOffset: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val gap = orientationHelper.endAfterPadding - endOffset
        var fixedOffset = 0
        if (gap > 0) {
            fixedOffset = -scrollBy(-gap, recycler, state)
        } else {
            return 0
        }
//        val remainingEndOffset = endOffset + fixedOffset
//        val remainingGap = orientationHelper.endAfterPadding - remainingEndOffset
//        if (remainingGap > 0) {
//            orientationHelper.offsetChildren(remainingGap)
//            return remainingGap + fixedOffset
//        }

        return fixedOffset
    }

    private fun recycleChildren(recycler: RecyclerView.Recycler, startIndex: Int, endIndex: Int) {
        if (startIndex == endIndex) {
            return
        }
        if (endIndex > startIndex) {
            for (i in endIndex - 1 downTo startIndex) {
                removeAndRecycleViewAt(i, recycler)
            }
        } else {
            for (i in startIndex downTo endIndex + 1) {
                removeAndRecycleViewAt(i, recycler)
            }
        }
    }

    private fun fill(
        recycler: RecyclerView.Recycler, layoutState: LayoutState,
        state: RecyclerView.State
    ): Int {
        val start = layoutState.available
        var remainingSpace = layoutState.available

        if (layoutState.scrollOffset != INVALID_INT) {
            if (layoutState.available < 0) {
                layoutState.scrollOffset += layoutState.available
            }
            recycleByLayoutState(recycler, layoutState)
        }

        while (remainingSpace > 0 && layoutState.hasMore(state, circleView)) {
            val result = layoutChunk(recycler, layoutState, state)
            layoutState.offset += result.consumed * if (layoutState.layoutDirection == LayoutDirection.ToStart) {
                -1
            } else {
                1
            }
            if (!result.ignoreConsumed || layoutState.scrapList.isNotEmpty() || !state.isPreLayout) {
                remainingSpace -= result.consumed
                layoutState.available -= result.consumed
            }
            if (layoutState.scrollOffset != INVALID_INT) {
                layoutState.scrollOffset += result.consumed
                if (layoutState.available < 0) {
                    layoutState.scrollOffset += layoutState.available
                }
                recycleByLayoutState(recycler, layoutState)
            }
        }
        val end = layoutState.available
        return start - end
    }

    private fun recycleByLayoutState(recycler: RecyclerView.Recycler, layoutState: LayoutState) {
        val scrollOffset = layoutState.scrollOffset
        if (layoutState.layoutDirection == LayoutDirection.ToStart) {
            recycleFromEnd(scrollOffset, recycler)
        } else {
            recycleFromStart(scrollOffset, recycler)
        }
    }

    private fun layoutChunk(
        recycler: RecyclerView.Recycler,
        layoutState: LayoutState,
        state: RecyclerView.State
    ): LayoutChunkResult {
        val view = layoutState.next(recycler, state, circleView)
        val params = view.layoutParams as RecyclerView.LayoutParams
        val layoutDirection = layoutState.layoutDirection
        val offset = layoutState.offset
        if (layoutState.scrapList.isEmpty()) {
            if (layoutDirection == LayoutDirection.ToStart) {
                addView(view, 0)
            } else {
                addView(view)
            }
        } else {
            if (layoutDirection == LayoutDirection.ToStart) {
                addDisappearingView(view, 0)
            } else {
                addDisappearingView(view)
            }
        }
        measureChildWithMargins(view, 0, 0)
        val consumed = orientationHelper.getDecoratedMeasurement(view)
        if (orientation == RecyclerView.VERTICAL) {
            val left = paddingLeft
            val right = left + orientationHelper.getDecoratedMeasurementInOther(view)
            val top = if (layoutDirection == LayoutDirection.ToStart) {
                offset - consumed
            } else {
                offset
            }
            val bottom = if (layoutDirection == LayoutDirection.ToStart) {
                offset
            } else {
                offset + consumed
            }

            layoutDecoratedWithMargins(view, left, top, right, bottom)
        } else {
            val left = if (layoutDirection == LayoutDirection.ToStart) {
                offset - consumed
            } else {
                offset
            }
            val right = if (layoutDirection == LayoutDirection.ToStart) {
                offset
            } else {
                offset + consumed
            }

            val top = paddingTop
            val bottom = top + orientationHelper.getDecoratedMeasurementInOther(view)

            layoutDecoratedWithMargins(view, left, top, right, bottom)
        }

        return LayoutChunkResult(
            consumed = consumed,
            ignoreConsumed = (params.isItemChanged || params.isItemChanged)
        )
    }

    private fun updateLayoutStateToFillEnd(itemPosition: Int, offset: Int) {
        layoutState.available = orientationHelper.endAfterPadding - offset
        layoutState.currentPosition = itemPosition
        layoutState.layoutDirection = LayoutDirection.ToEnd
        layoutState.offset = offset
        layoutState.scrollOffset = INVALID_INT
    }

    private fun updateLayoutStateToStart(itemPosition: Int, offset: Int) {
        layoutState.available = offset - orientationHelper.startAfterPadding
        layoutState.currentPosition = itemPosition
        layoutState.layoutDirection = LayoutDirection.ToStart
        layoutState.offset = offset
        layoutState.scrollOffset = INVALID_INT
    }

    private fun updateAnchorByLayout(state: RecyclerView.State, layoutDirection: LayoutDirection) {
        anchorInfo.reset()
        val refChild = when (layoutDirection) {
            LayoutDirection.ToStart -> {
                findLastReferenceChild(state)
            }

            LayoutDirection.ToEnd -> {
                findFirstReferenceChild(state)
            }
        }
        if (refChild != null) {
            anchorInfo.assignFromView(refChild, getPosition(refChild), layoutDirection, orientationHelper)
        } else {
            anchorInfo.position = 0
            anchorInfo.coordinate = when (layoutDirection) {
                LayoutDirection.ToStart -> {
                    orientationHelper.endAfterPadding
                }
                LayoutDirection.ToEnd -> {
                    orientationHelper.startAfterPadding
                }
            }
        }

    }

    private fun getChildClosestToStart(): View? {
        return getChildAt(0)
    }

    private fun getChildClosestToEnd(): View? {
        return getChildAt(childCount - 1)
    }

    private fun findLastReferenceChild(state: RecyclerView.State): View? {
        return findReferenceChild(childCount - 1, -1, state.itemCount)
    }

    private fun findFirstReferenceChild(state: RecyclerView.State): View? {
        return findReferenceChild(0, childCount, state.itemCount)
    }

    private fun findReferenceChild(
        start: Int, end: Int, itemCount: Int
    ): View? {
        var invalidMatch: View? = null
        var outOfBoundsMatch: View? = null
        val boundsStart = orientationHelper.startAfterPadding
        val boundsEnd = orientationHelper.endAfterPadding
        val diff = if (end > start) 1 else -1
        var i = start
        while (i != end) {
            val view = getChildAt(i)
            val position = getPosition(view!!)
            if (position in 0 until itemCount) {
                if ((view.layoutParams as RecyclerView.LayoutParams).isItemRemoved) {
                    if (invalidMatch == null) {
                        invalidMatch = view // removed item, least preferred
                    }
                } else if (orientationHelper.getDecoratedStart(view) >= boundsEnd || orientationHelper.getDecoratedEnd(
                        view
                    ) < boundsStart
                ) {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view // item is not visible, less preferred
                    }
                } else {
                    return view
                }
            }
            i += diff
        }
        return outOfBoundsMatch ?: invalidMatch
    }


}

internal class AnchorInfo(
    var position: Int,
    var coordinate: Int,
    var layoutDirection: LayoutDirection
) {

    fun reset() {
        position = INVALID_INT
        coordinate = INVALID_INT
    }

    fun assignFromView(child: View, position: Int,
                       layoutDirection: LayoutDirection,
                       orientationHelper: OrientationHelper) {
        coordinate = when (layoutDirection) {
            LayoutDirection.ToStart -> {
                orientationHelper.getDecoratedEnd(child) + orientationHelper.totalSpaceChange
            }

            LayoutDirection.ToEnd -> {
                orientationHelper.getDecoratedStart(child)
            }
        }
        this.position = position
    }
}

internal enum class LayoutDirection { ToStart, ToEnd }

internal const val INVALID_INT = Int.MIN_VALUE

internal class LayoutChunkResult(
    val consumed: Int = 0,
    val ignoreConsumed: Boolean = false
)

internal class LayoutState(
    var offset: Int,
    var available: Int,
    var currentPosition: Int,
    var layoutDirection: LayoutDirection,
    var isPreLayout: Boolean,
    var lastScrollDela: Int,
    var scrollOffset: Int,
    var scrapList: List<RecyclerView.ViewHolder>
) {
    fun reset() {
        offset = INVALID_INT
        available = INVALID_INT
        currentPosition = INVALID_INT
        layoutDirection = LayoutDirection.ToEnd
        isPreLayout = false
        lastScrollDela = INVALID_INT
        scrollOffset = INVALID_INT
        scrapList = emptyList()
    }

    fun hasMore(state: RecyclerView.State, circleView: Boolean = false): Boolean {
        return if (circleView) {
            state.itemCount > 0
        } else {
            currentPosition in 0 until state.itemCount
        }
    }

    fun next(recycler: RecyclerView.Recycler, state: RecyclerView.State, circleView: Boolean = false): View {
//            return nextViewFromScrapList().let {
//                if (it == null) {
//                    val v = recycler.getViewForPosition(currentPosition)
//                    currentPosition += when (layoutDirection) {
//                        LayoutDirection.ToStart -> {
//                            -1
//                        }
//
//                        LayoutDirection.ToEnd -> {
//                            1
//                        }
//                    }
//                    v
//                } else {
//                    it
//                }
//            }
        val v = recycler.getViewForPosition(currentPosition)

        nextPosition(state, circleView)
        return v
    }

    fun nextPosition(state: RecyclerView.State, circleView: Boolean = false) {
        val itemCount = state.itemCount
        val needToFixPosition = currentPosition + when (layoutDirection) {
            LayoutDirection.ToStart -> {
                -1
            }

            LayoutDirection.ToEnd -> {
                1
            }
        }
        if (itemCount <= 0) return
        currentPosition = if (circleView) (needToFixPosition + itemCount) % itemCount else needToFixPosition
    }

    private fun nextViewFromScrapList(): View? {
        val size = scrapList.size
        for (i in 0 until size) {
            val view = scrapList.get(i).itemView
            val lp = view.getLayoutParams() as RecyclerView.LayoutParams
            if (lp.isItemRemoved) {
                continue
            }
            if (currentPosition == lp.viewLayoutPosition) {
                assignPositionFromScrapList(view)
                return view
            }
        }
        return null
    }

    fun assignPositionFromScrapList(ignore: View?) {
        val closest = nextViewInLimitedList(ignore)
        currentPosition = if (closest == null) {
            RecyclerView.NO_POSITION
        } else {
            (closest.layoutParams as RecyclerView.LayoutParams)
                .viewLayoutPosition
        }
    }

    fun nextViewInLimitedList(ignore: View?): View? {
        val size = scrapList.size
        var closest: View? = null
        var closestDistance = Integer.MAX_VALUE
        for (i in 0 until size) {
            val view = scrapList[i].itemView
            val lp = view.getLayoutParams() as RecyclerView.LayoutParams
            if (view === ignore || lp.isItemRemoved) {
                continue
            }
            val distance =
                (lp.viewLayoutPosition - currentPosition) * if (layoutDirection == LayoutDirection.ToStart) -1 else 1
            if (distance < 0) {
                continue // item is not in current direction
            }
            if (distance < closestDistance) {
                closest = view
                closestDistance = distance
                if (distance == 0) {
                    break
                }
            }
        }
        return closest
    }
}