package com.ndsoftwares.infiniteviewpager


import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.ndsoftwares.infiniteviewpager.SmartTabIndicationInterpolator.Companion.ID_SMART
import java.util.*
import kotlin.math.abs

class RecyclerTabLayout2 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {

    private var mIndicatorPaint: Paint
    private var mTabItemIndicatorPaint: Paint
    private var mTabBackgroundResId = 0
    private var mTabOnScreenLimit = 0
    private var mTabMinWidth = 0
    private var mTabMaxWidth = 0
    private var mTabTextAppearance = 0
    private var mTabSelectedTextColor = 0
    private var mTabSelectedTextColorSet = false
    private var mTabPaddingStart = 0
    private var mTabPaddingTop = 0
    private var mTabPaddingEnd = 0
    private var mTabPaddingBottom = 0
    private var mTabSelectType = 0
    private var mIndicatorHeight = 0
    private var mRealItemCount = 0

    //gather show indicator position

    private val lastPosition = 0
    private var selectedPosition = 0
    private var selectionOffset = 0f
    private val indicatorRectF = RectF()
    private var indicatorCornerRadius = 0
    private val indicationInterpolator: SmartTabIndicationInterpolator = SmartTabIndicationInterpolator.of(ID_SMART)

    private var mIndicatorPositionQueue: LinkedHashSet<Int> = LinkedHashSet<Int>(DEFAULT_DRAW_INDICATOR_COUNT)
    private var mIndicatorTempDeque: LinkedHashSet<Int> = LinkedHashSet<Int>()
    private var mLinearLayoutManager: LinearLayoutManager
    private var mRecyclerOnScrollListener: RecyclerOnScrollListener? = null
    private var mViewPager: InfiniteViewPager? = null
    private var mAdapter: Adapter<*>? = null
    private var mIndicatorPosition = 0
    private var mIndicatorGap = 0
    private var mIndicatorScroll = 0
    private var mOldPosition = 0
    private var mOldScrollOffset = 0
    private var mOldPositionOffset = 0f
    private var mPositionThreshold: Float
    private var mRequestScrollToTab = false
    private var mScrollEnabled = false
    private val mPxPaddingOval: Int
    private val mPxPaddingPartialRectTop: Int
    private val mPxRound: Int



    private fun getAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.rtl_RecyclerTabLayout,
                defStyle, R.style.rtl_RecyclerTabLayout)
        setIndicatorColor(a.getColor(R.styleable.rtl_RecyclerTabLayout_rtl_tabIndicatorColor, 0))
        setIndicatorHeight(a.getDimensionPixelSize(R.styleable.rtl_RecyclerTabLayout_rtl_tabIndicatorHeight, 0))
        indicatorCornerRadius = a
                .getDimensionPixelSize(R.styleable.rtl_RecyclerTabLayout_rtl_indicatorCornerRadius, 0)

        mTabTextAppearance = a.getResourceId(R.styleable.rtl_RecyclerTabLayout_rtl_tabTextAppearance,
                R.style.TabTextStyle)
        mTabPaddingBottom = a
                .getDimensionPixelSize(R.styleable.rtl_RecyclerTabLayout_rtl_tabPadding, 0)
        mTabPaddingEnd = mTabPaddingBottom
        mTabPaddingTop = mTabPaddingEnd
        mTabPaddingStart = mTabPaddingTop
        mTabPaddingStart = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingStart, mTabPaddingStart)
        mTabPaddingTop = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingTop, mTabPaddingTop)
        mTabPaddingEnd = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingEnd, mTabPaddingEnd)
        mTabPaddingBottom = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingBottom, mTabPaddingBottom)
        mTabSelectType = a.getInt(R.styleable.rtl_RecyclerTabLayout_rtl_selectType, -1)
        if (a.hasValue(R.styleable.rtl_RecyclerTabLayout_rtl_tabSelectedTextColor)) {
            mTabSelectedTextColor = a
                    .getColor(R.styleable.rtl_RecyclerTabLayout_rtl_tabSelectedTextColor, 0)
            mTabSelectedTextColorSet = true
        }
        mTabOnScreenLimit = a.getInteger(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabOnScreenLimit, 0)
        if (mTabOnScreenLimit == 0) {
            mTabMinWidth = a.getDimensionPixelSize(
                    R.styleable.rtl_RecyclerTabLayout_rtl_tabMinWidth, 0)
            mTabMaxWidth = a.getDimensionPixelSize(
                    R.styleable.rtl_RecyclerTabLayout_rtl_tabMaxWidth, 0)
        }
        mTabBackgroundResId = a
                .getResourceId(R.styleable.rtl_RecyclerTabLayout_rtl_tabBackground, 0)
        mScrollEnabled = a.getBoolean(R.styleable.rtl_RecyclerTabLayout_rtl_scrollEnabled, true)
        a.recycle()
    }

    override fun onDetachedFromWindow() {
        if (mRecyclerOnScrollListener != null) {
            removeOnScrollListener(mRecyclerOnScrollListener!!)
            mRecyclerOnScrollListener = null
        }
        super.onDetachedFromWindow()
    }

    private fun setIndicatorColor(color: Int) {
        mIndicatorPaint.color = color
        mTabItemIndicatorPaint.color = color
    }

    private fun setIndicatorHeight(indicatorHeight: Int) {
        mIndicatorHeight = indicatorHeight
    }

    fun setAutoSelectionMode(autoSelect: Boolean) {
        if (mRecyclerOnScrollListener != null) {
            removeOnScrollListener(mRecyclerOnScrollListener!!)
            mRecyclerOnScrollListener = null
        }
        if (autoSelect) {
            mRecyclerOnScrollListener = RecyclerOnScrollListener(this, mLinearLayoutManager)
            addOnScrollListener(mRecyclerOnScrollListener!!)
        }
    }

    fun setPositionThreshold(positionThreshold: Float) {
        mPositionThreshold = positionThreshold
    }

    private fun setRefreshIndicatorWithScroll(autoRefreshIndicator: Boolean) {
        if (mRecyclerOnScrollListener != null) {
            removeOnScrollListener(mRecyclerOnScrollListener!!)
            mRecyclerOnScrollListener = null
        }
        if (autoRefreshIndicator) {
            mRecyclerOnScrollListener = RecyclerOnScrollListener(this, mLinearLayoutManager)
            addOnScrollListener(mRecyclerOnScrollListener!!)
        }
    }

    fun setTabOnScreenLimit(tabOnScreenLimit: Int) {
        mTabOnScreenLimit = tabOnScreenLimit
    }

    fun setUpWithViewPager(viewPager: InfiniteViewPager) {
        val adapter = DefaultAdapter(viewPager)
        adapter.setTabPadding(mTabPaddingStart, mTabPaddingTop, mTabPaddingEnd, mTabPaddingBottom)
        adapter.setTabTextAppearance(mTabTextAppearance)
        adapter.setTabSelectedTextColor(mTabSelectedTextColorSet, mTabSelectedTextColor)
        adapter.setTabMaxWidth(mTabMaxWidth)
        adapter.setTabMinWidth(mTabMinWidth)
        adapter.setTabBackgroundResId(mTabBackgroundResId)
        adapter.setTabOnScreenLimit(mTabOnScreenLimit)
        if (viewPager.adapter is InfinitePagerAdapter) {
            mRealItemCount = (viewPager.adapter as InfinitePagerAdapter?)!!.getRealCount()
            adapter.setRealItemCount(mRealItemCount)
        }
        setUpWithAdapter(adapter)
    }

    private fun setUpWithAdapter(adapter: Adapter<*>) {
        mAdapter = adapter
        mViewPager = adapter.viewPager
        requireNotNull(mViewPager!!.adapter) { "ViewPager does not have a PagerAdapter set" }
        mViewPager!!.addOnPageChangeListener(ViewPagerOnPageChangeListener(this))
        setAdapter(adapter)
        scrollToTab(mViewPager!!.currentItem)
    }

    fun setCurrentItem(position: Int, smoothScroll: Boolean) {
        if (mViewPager != null) {
            val adapter = mViewPager!!.adapter
            if (adapter is InfinitePagerAdapter) {
                adapter.willBePageSelect(position)
            }
            mViewPager!!.setCurrentItem(position, smoothScroll)
            scrollToTab(mViewPager!!.currentItem)
            return
        }
        if (smoothScroll && position != mIndicatorPosition) {
            startAnimation(position)
        } else {
            scrollToTab(position)
        }
    }

    fun setCurrentCenterItem(position: Int) {
        mIndicatorPositionQueue.clear()
        if (mTabOnScreenLimit > 0) {
            mIndicatorPositionQueue.add(position)
        } else {
            for (i in position - DEFAULT_DRAW_INDICATOR_COUNT until position + DEFAULT_DRAW_INDICATOR_COUNT) {
                if ((i - mIndicatorPosition) % mRealItemCount == 0) {
                    mIndicatorPositionQueue.add(i)
                }
            }
        }
        invalidate()
    }

    private fun startAnimation(position: Int) {
        var distance = 1f
        val view = mLinearLayoutManager.findViewByPosition(position)
        if (view != null) {
            val currentX = view.x + view.measuredWidth / 2f
            val centerX = measuredWidth / 2f
            distance = Math.abs(centerX - currentX) / view.measuredWidth
        }
        val animator: ValueAnimator
        animator = if (position < mIndicatorPosition) {
            ValueAnimator.ofFloat(distance, 0f)
        } else {
            ValueAnimator.ofFloat(-distance, 0f)
        }
        animator.duration = DEFAULT_SCROLL_DURATION
        animator.addUpdateListener { animation -> scrollToTab(position, animation.animatedValue as Float, true) }
        animator.start()
    }

    private fun scrollToTab(position: Int) {
        scrollToTab(position, 0f, false)
        mAdapter!!.currentIndicatorPosition = position
        mAdapter!!.notifyDataSetChanged()
    }

    private fun scrollToTab(position: Int, positionOffset: Float, fitIndicator: Boolean) {
        var position = position
        var scrollOffset = 0
        var selectedView: View? = null
        val firstVisiblePosition = mLinearLayoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = mLinearLayoutManager.findLastVisibleItemPosition()

        //search visible area the same indicator item
        for (i in firstVisiblePosition..lastVisiblePosition) {
            if ((i - position) % mRealItemCount == 0) {
                selectedView = mLinearLayoutManager.findViewByPosition(i)
                position = i
                break
            }
        }
        val nextView = mLinearLayoutManager.findViewByPosition(position + 1)
        if (selectedView != null) {
            val width = measuredWidth
            val sLeft: Float = if (position == 0) 0f else width / 2f - selectedView.measuredWidth / 2f // left edge of selected tab
            val sRight = sLeft + selectedView.measuredWidth // right edge of selected tab
            if (nextView != null) {
                val nLeft = width / 2f - nextView.measuredWidth / 2f // left edge of next tab
                val distance = sRight - nLeft // total distance that is needed to distance to next tab
                val dx = distance * positionOffset
                scrollOffset = (sLeft - dx).toInt()
                if (position == 0) {
                    val indicatorGap = ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()
                    mIndicatorGap = (indicatorGap * positionOffset).toInt()
                    mIndicatorScroll = ((selectedView.measuredWidth + indicatorGap) * positionOffset).toInt()
                } else {
                    val indicatorGap = ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()
                    mIndicatorGap = (indicatorGap * positionOffset).toInt()
                    mIndicatorScroll = dx.toInt()
                }
            } else {
                scrollOffset = sLeft.toInt()
                mIndicatorScroll = 0
                mIndicatorGap = 0
            }
            if (fitIndicator) {
                mIndicatorScroll = 0
                mIndicatorGap = 0
            }
        } else {
            if (measuredWidth > 0 && mTabMaxWidth > 0 && mTabMinWidth == mTabMaxWidth) { //fixed size
                val width = mTabMinWidth
                val offset = (positionOffset * -width).toInt()
                val leftOffset = ((measuredWidth - width) / 2f).toInt()
                scrollOffset = offset + leftOffset
            }
            mRequestScrollToTab = true
        }
        updateCurrentIndicatorPosition(position, positionOffset - mOldPositionOffset, positionOffset)
        mIndicatorPosition = position
        setCurrentCenterItem(mIndicatorPosition)
        stopScroll()
        if (position != mOldPosition || scrollOffset != mOldScrollOffset) {
            mLinearLayoutManager.scrollToPositionWithOffset(position, scrollOffset)
        }
        if (mIndicatorHeight > 0) {
            invalidate()
        }
        mOldPosition = position
        mOldScrollOffset = scrollOffset
        mOldPositionOffset = positionOffset
    }

    private fun updateCurrentIndicatorPosition(position: Int, dx: Float, positionOffset: Float) {
        if (mAdapter == null) {
            return
        }
        var indicatorPosition = -1
        if (dx > 0 && positionOffset >= mPositionThreshold - POSITION_THRESHOLD_ALLOWABLE) {
            indicatorPosition = position + 1
        } else if (dx < 0 && positionOffset <= 1 - mPositionThreshold + POSITION_THRESHOLD_ALLOWABLE) {
            indicatorPosition = position
        }
        if (indicatorPosition >= 0 && indicatorPosition != mAdapter!!.currentIndicatorPosition) {
            mAdapter!!.currentIndicatorPosition = indicatorPosition
            mAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mIndicatorPositionQueue.size == 0) return
        mIndicatorTempDeque.clear()

        //元のqueueはそのままに保持しつつ、新しいqueueで回す。
        mIndicatorTempDeque.addAll(mIndicatorPositionQueue)
        for (indicatorPosition in mIndicatorTempDeque) {
            val selectedTab = mLinearLayoutManager.findViewByPosition(indicatorPosition)
            if (selectedTab == null) {
                if (mRequestScrollToTab) {
                    mRequestScrollToTab = false
                    scrollToTab(mViewPager!!.currentItem)
                }
                continue
            }
            mRequestScrollToTab = false
            var left: Int
            var right: Int
            val selectedStart: Int = Utils.getStart(selectedTab, false)
            val selectedEnd: Int = Utils.getEnd(selectedTab, false)
            if (isLayoutRtl) {
                left = selectedEnd
                right = selectedStart
            } else {
                left = selectedStart
                right = selectedEnd
            }

            var thickness: Float = mIndicatorHeight.toFloat()
            if (selectionOffset > 0f) {

                // Draw the selection partway between the tabs
                val startOffset = indicationInterpolator.getLeftEdge(selectionOffset)
                val endOffset = indicationInterpolator.getRightEdge(selectionOffset)
                val thicknessOffset = indicationInterpolator.getThickness(selectionOffset)

                val nextTab = mLinearLayoutManager.findViewByPosition(indicatorPosition + 1)
                val nextStart = Utils.getStart(nextTab, false)
                val nextEnd = Utils.getEnd(nextTab, false)
                if (isLayoutRtl) {
                    left = (endOffset * nextEnd + (1.0f - endOffset) * left).toInt()
                    right = (startOffset * nextStart + (1.0f - startOffset) * right).toInt()
                } else {
                    left = (startOffset * nextStart + (1.0f - startOffset) * left).toInt()
                    right = (endOffset * nextEnd + (1.0f - endOffset) * right).toInt()
                }
                thickness *= thicknessOffset
            }

            drawIndicator(canvas, left, right, height, thickness)
        }
    }

    private fun drawIndicator(canvas: Canvas, left: Int, right: Int, height: Int, thickness: Float) {
        if (mIndicatorHeight <= 0 ) {
            return
        }
        val top: Float
        val bottom: Float
        val center: Float = height / 2f
        top = center - thickness / 2f
        bottom = center + thickness / 2f

        indicatorRectF.set(left.toFloat(), top, right.toFloat(), bottom)
        if (indicatorCornerRadius > 0f) {
            canvas.drawRoundRect(
                    indicatorRectF, indicatorCornerRadius.toFloat(),
                    indicatorCornerRadius.toFloat(), mIndicatorPaint)
        } else {
            canvas.drawRect(indicatorRectF, mIndicatorPaint)
        }
    }

//    override fun onDraw(canvas: Canvas) {
//        if (mIndicatorPositionQueue.size == 0) return
//        mIndicatorTempDeque.clear()
//
//        //元のqueueはそのままに保持しつつ、新しいqueueで回す。
//        mIndicatorTempDeque.addAll(mIndicatorPositionQueue)
//        for (indicatorPosition in mIndicatorTempDeque) {
//            val view = mLinearLayoutManager.findViewByPosition(indicatorPosition)
//            if (view == null) {
//                if (mRequestScrollToTab) {
//                    mRequestScrollToTab = false
//                    scrollToTab(mViewPager!!.currentItem)
//                }
//                continue
//            }
//            mRequestScrollToTab = false
//            var left: Int
//            var right: Int
//            if (isLayoutRtl) {
//                left = view.left - mIndicatorScroll - mIndicatorGap
//                right = view.right - mIndicatorScroll + mIndicatorGap
//            } else {
//                left = view.left + mIndicatorScroll - mIndicatorGap
//                right = view.right + mIndicatorScroll + mIndicatorGap
//            }
//            val top = height - mIndicatorHeight
//            val bottom = height
//            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mIndicatorPaint)
//            when (mTabSelectType) {
//                0 ->                     //oval
//                    canvas.drawRoundRect(RectF(left.toFloat(), mPxPaddingOval.toFloat(), right.toFloat(), (bottom - mPxPaddingOval).toFloat()), mPxRound.toFloat(), mPxRound.toFloat(), mTabItemIndicatorPaint)
//                1 ->                     //rect
//                    canvas.drawRect(left.toFloat(), 0f, right.toFloat(), top.toFloat(), mTabItemIndicatorPaint)
//                2 -> {
//                    //partialRect
//                    val path = RoundedRect(left.toFloat(), mPxPaddingPartialRectTop.toFloat(), right.toFloat(), bottom.toFloat(), 30f, 30f, true, true, false, false)
//                    canvas.drawPath(path, mTabItemIndicatorPaint)
//                }
//            }
//        }
//    }

    private fun RoundedRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float,
                            tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean): Path {
        var rx = rx
        var ry = ry
        val path = Path()
        if (rx < 0) rx = 0f
        if (ry < 0) ry = 0f
        val width = right - left
        val height = bottom - top
        if (rx > width / 2) rx = width / 2
        if (ry > height / 2) ry = height / 2
        val widthMinusCorners = width - 2 * rx
        val heightMinusCorners = height - 2 * ry
        path.moveTo(right, top + ry)
        if (tr) path.rQuadTo(0f, -ry, -rx, -ry) //top-right corner
        else {
            path.rLineTo(0f, -ry)
            path.rLineTo(-rx, 0f)
        }
        path.rLineTo(-widthMinusCorners, 0f)
        if (tl) path.rQuadTo(-rx, 0f, -rx, ry) //top-left corner
        else {
            path.rLineTo(-rx, 0f)
            path.rLineTo(0f, ry)
        }
        path.rLineTo(0f, heightMinusCorners)
        if (bl) path.rQuadTo(0f, ry, rx, ry) //bottom-left corner
        else {
            path.rLineTo(0f, ry)
            path.rLineTo(rx, 0f)
        }
        path.rLineTo(widthMinusCorners, 0f)
        if (br) path.rQuadTo(rx, 0f, rx, -ry) //bottom-right corner
        else {
            path.rLineTo(rx, 0f)
            path.rLineTo(0f, -ry)
        }
        path.rLineTo(0f, -heightMinusCorners)
        path.close() //Given close, last lineto can be removed.
        return path
    }

     private val isLayoutRtl: Boolean
        get() = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

    private class RecyclerOnScrollListener(private var mRecyclerTabLayout: RecyclerTabLayout2,
                                           private var mLinearLayoutManager: LinearLayoutManager) : OnScrollListener() {
        var mDx = 0
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            mDx += dx
            if (mDx > REFRESH_DISTANCE) {
                refreshCenterTabForRightScroll()
                mDx = 0
            } else if (mDx < -REFRESH_DISTANCE) {
                refreshCenterTabForLeftScroll()
                mDx = 0
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                SCROLL_STATE_IDLE -> {
                    if (mDx > 0) {
                        refreshCenterTabForRightScroll()
                    } else {
                        refreshCenterTabForLeftScroll()
                    }
                    mDx = 0
                }
                SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING -> {
                }
            }
        }

        private fun refreshCenterTabForRightScroll() {
            val first = mLinearLayoutManager.findFirstVisibleItemPosition()
            val last = mLinearLayoutManager.findLastVisibleItemPosition()
            val center = mRecyclerTabLayout.width / 2
            for (position in first..last) {
                val view = mLinearLayoutManager.findViewByPosition(position)
                if (view!!.left + view.width >= center) {
                    mRecyclerTabLayout.setCurrentCenterItem(position)
                    break
                }
            }
        }

        private fun refreshCenterTabForLeftScroll() {
            val first = mLinearLayoutManager.findFirstVisibleItemPosition()
            val last = mLinearLayoutManager.findLastVisibleItemPosition()
            val center = mRecyclerTabLayout.width / 2
            for (position in last downTo first) {
                val view = mLinearLayoutManager.findViewByPosition(position)
                if (view!!.left <= center) {
                    mRecyclerTabLayout.setCurrentCenterItem(position)
                    break
                }
            }
        }

        companion object {
            private const val REFRESH_DISTANCE = 3000
        }
    }

    private class ViewPagerOnPageChangeListener(private val mRecyclerTabLayout: RecyclerTabLayout2) : OnPageChangeListener {
        private var mScrollState = 0
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            mRecyclerTabLayout.selectionOffset = positionOffset
            mRecyclerTabLayout.selectedPosition = position

            if (mScrollState != ViewPager.SCROLL_STATE_IDLE) {
                mRecyclerTabLayout.scrollToTab(position, positionOffset, false)
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            mScrollState = state
        }

        override fun onPageSelected(position: Int) {
            val adapter = mRecyclerTabLayout.mViewPager!!.adapter
            if (adapter is InfinitePagerAdapter) {
                adapter.willBePageSelect(position)
            }
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mRecyclerTabLayout.selectionOffset = 0f
                mRecyclerTabLayout.selectedPosition = position

                if (mRecyclerTabLayout.mIndicatorPosition != position) {
                    mRecyclerTabLayout.scrollToTab(position)
                }
            }
        }
    }

    abstract class Adapter<T : ViewHolder?>(var viewPager: InfiniteViewPager) : RecyclerView.Adapter<T>() {
        var currentIndicatorPosition = 0

    }

    class DefaultAdapter(viewPager: InfiniteViewPager) : Adapter<DefaultAdapter.ViewHolder?>(viewPager) {
        private var mTabPaddingStart = 0
        private var mTabPaddingTop = 0
        private var mTabPaddingEnd = 0
        private var mTabPaddingBottom = 0
        private var mTabTextAppearance = 0
        private var mTabSelectedTextColorSet = false
        private var mTabSelectedTextColor = 0
        private var mTabMaxWidth = 0
        private var mTabMinWidth = 0
        private var mTabBackgroundResId = 0
        private var mTabOnScreenLimit = 0
        private var mRealItemCount = 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tabTextView = TabTextView(parent.context)
            ViewCompat.setPaddingRelative(tabTextView, mTabPaddingStart, mTabPaddingTop,
                    mTabPaddingEnd, mTabPaddingBottom)
            tabTextView.gravity = Gravity.CENTER
            tabTextView.maxLines = MAX_TAB_TEXT_LINES
            tabTextView.ellipsize = TextUtils.TruncateAt.END
            if (mTabOnScreenLimit > 0) {
                val width = parent.measuredWidth / mTabOnScreenLimit
                tabTextView.maxWidth = width
                tabTextView.minWidth = width
            } else {
                if (mTabMaxWidth > 0) {
                    tabTextView.maxWidth = mTabMaxWidth
                }
                tabTextView.minWidth = mTabMinWidth
            }
            tabTextView.setTextAppearance(tabTextView.context, mTabTextAppearance)
            if (mTabSelectedTextColorSet) {
                tabTextView.setTextColor(tabTextView.createColorStateList(
                        tabTextView.currentTextColor, mTabSelectedTextColor))
            }
            if (mTabBackgroundResId != 0) {
                tabTextView.setBackgroundDrawable(
                        AppCompatResources.getDrawable(tabTextView.context, mTabBackgroundResId))
            }
            tabTextView.layoutParams = createLayoutParamsForTabs()
            return ViewHolder(tabTextView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val title = viewPager.adapter!!.getPageTitle(position)
            holder.title.text = title
            holder.title.isSelected = currentIndicatorPosition % mRealItemCount == position % mRealItemCount
        }

        override fun getItemCount(): Int {
            return viewPager.adapter!!.count
        }

        fun setTabPadding(tabPaddingStart: Int, tabPaddingTop: Int, tabPaddingEnd: Int,
                          tabPaddingBottom: Int) {
            mTabPaddingStart = tabPaddingStart
            mTabPaddingTop = tabPaddingTop
            mTabPaddingEnd = tabPaddingEnd
            mTabPaddingBottom = tabPaddingBottom
        }

        fun setTabTextAppearance(tabTextAppearance: Int) {
            mTabTextAppearance = tabTextAppearance
        }

        fun setTabSelectedTextColor(tabSelectedTextColorSet: Boolean,
                                    tabSelectedTextColor: Int) {
            mTabSelectedTextColorSet = tabSelectedTextColorSet
            mTabSelectedTextColor = tabSelectedTextColor
        }

        fun setTabMaxWidth(tabMaxWidth: Int) {
            mTabMaxWidth = tabMaxWidth
        }

        fun setTabMinWidth(tabMinWidth: Int) {
            mTabMinWidth = tabMinWidth
        }

        fun setTabBackgroundResId(tabBackgroundResId: Int) {
            mTabBackgroundResId = tabBackgroundResId
        }

        fun setTabOnScreenLimit(tabOnScreenLimit: Int) {
            mTabOnScreenLimit = tabOnScreenLimit
        }

        fun setRealItemCount(realCount: Int) {
            mRealItemCount = realCount
        }

        private fun createLayoutParamsForTabs(): LayoutParams {
            return LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var title: TextView = itemView as TextView

            init {
                itemView.setOnClickListener(object : OnClickListener {
                    override fun onClick(v: View) {
                        val pos = adapterPosition
                        if (pos != NO_POSITION) {
                            if ((pos - currentIndicatorPosition) % mRealItemCount == 0) {
                                return
                            }
                            val pagerAdapter = viewPager.adapter
                            var realCount = 0
                            if (pagerAdapter is InfinitePagerAdapter) {
                                pagerAdapter.willBePageSelect(pos)
                                realCount = pagerAdapter.getRealCount()
                            }
                            var loopDistance: Int = pos - currentIndicatorPosition
                            val percent = loopDistance.toFloat() / mRealItemCount
                            if (mTabOnScreenLimit == 0 && abs(percent) != 0.5f) {
                                val nearlyTheSameItem: Int = currentIndicatorPosition + Math.round(percent) * mRealItemCount
                                loopDistance = pos - nearlyTheSameItem
                            }
                            var currentItem = viewPager.currentItem
                            currentItem += loopDistance

                            // loopしない場合にはマイナスになる
                            if (currentItem < 0) {
                                currentItem += realCount
                            }
                            viewPager.setCurrentItem(currentItem, true)
                        }
                    }
                })
            }
        }

        companion object {
            private const val MAX_TAB_TEXT_LINES = 2
        }
    }

    class TabTextView(context: Context) : AppCompatTextView(context) {

        fun createColorStateList(defaultColor: Int, selectedColor: Int): ColorStateList {
            val states = arrayOfNulls<IntArray>(2)
            val colors = IntArray(2)
            states[0] = SELECTED_STATE_SET
            colors[0] = selectedColor
            // Default enabled state
            states[1] = EMPTY_STATE_SET
            colors[1] = defaultColor
            return ColorStateList(states, colors)
        }

        override fun setSelected(selected: Boolean) {
            super.setSelected(selected)

            val textStyleResId = if (selected) R.style.TabTextStyleSelected else R.style.TabTextStyle
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                TextViewCompat.setTextAppearance(this, textStyleResId);
            } else {
                setTextAppearance(textStyleResId);
            }
        }
    }

    private fun dip(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val DEFAULT_SCROLL_DURATION: Long = 200
        private const val DEFAULT_POSITION_THRESHOLD = 0.6f
        private const val POSITION_THRESHOLD_ALLOWABLE = 0.001f
        private const val DEFAULT_DRAW_INDICATOR_COUNT = 20 //cached indicator count
    }

    init {
        setWillNotDraw(false)
        mIndicatorPaint = Paint()
        mTabItemIndicatorPaint = Paint()
        mPxPaddingOval = dip(getContext(), 4)
        mPxPaddingPartialRectTop = dip(getContext(), 8)
        mPxRound = dip(getContext(), 25)
        getAttributes(context, attrs, defStyle)
        mLinearLayoutManager = object : LinearLayoutManager(getContext()) {
            override fun canScrollHorizontally(): Boolean {
                return mScrollEnabled
            }
        }
        mLinearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager = mLinearLayoutManager
        itemAnimator = null
        mPositionThreshold = DEFAULT_POSITION_THRESHOLD
        setRefreshIndicatorWithScroll(true)
    }
}