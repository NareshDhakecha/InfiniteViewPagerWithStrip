package com.ndsoftwares.infiniteviewpager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

class InfiniteViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    companion object{

        const val OFFSET = 6000 // OFFSET એ NUMBER_OF_PAGES નો અવયવી હોવો જોઇએ
    }

    /**
     * Enable swipe
     */
    private var isSwipeEnabled = true

    override fun setAdapter(adapter: PagerAdapter?) {
        super.setAdapter(adapter)
        // offset first element so that we can scroll to the left
        currentItem = OFFSET
    }

    override fun setCurrentItem(item: Int) {

        // offset the current item to ensure there is space to scroll
        setCurrentItem(item, false)
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        if (adapter is InfinitePagerAdapter && adapter!!.count > 0) {
            val infAdapter = adapter as InfinitePagerAdapter?
            val nearLeftEdge = item <= infAdapter!!.getRealCount()
            val nearRightEdge = item >= infAdapter.count - infAdapter.getRealCount()
            if (nearLeftEdge || nearRightEdge) {
                super.setCurrentItem(OFFSET + item % infAdapter.getRealCount(), false)
                return
            }
        }
        super.setCurrentItem(item, smoothScroll)
    }

//    fun getOffsetAmount(): Int {
//        if (adapter!!.count == 0) {
//            return 0
//        }
//        return if (adapter is InfinitePagerAdapter) {
//            val infAdapter = adapter as InfinitePagerAdapter?
//            OFFSET
//        } else {
//            0
//        }
//    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (isSwipeEnabled) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (isSwipeEnabled) {
            super.onInterceptTouchEvent(event)
        } else false
    }
}