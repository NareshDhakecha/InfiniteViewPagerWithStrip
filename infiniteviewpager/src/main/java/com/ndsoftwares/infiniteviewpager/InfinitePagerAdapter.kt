package com.ndsoftwares.infiniteviewpager

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.ndsoftwares.infiniteviewpager.InfiniteViewPager.Companion.OFFSET

class InfinitePagerAdapter(private val adapter: PagerAdapter): PagerAdapter() {

    private var selectedPosition = 0

    override fun getCount(): Int {

        // warning: scrolling to very high values (1,000,000+) results in
        // strange drawing behaviour
        return OFFSET * 2
    }

    /**
     * @return the [.getCount] result of the wrapped adapter
     */
    fun getRealCount(): Int {
        return adapter.count
    }

    override fun getPageTitle(position: Int): CharSequence? {
        if (getRealCount() == 0) return null
        val virtualPosition = position % getRealCount()
        return adapter.getPageTitle(virtualPosition)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val virtualPosition = position % getRealCount()
        // only expose virtual position to the inner adapter
        return adapter.instantiateItem(container, virtualPosition)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return adapter.isViewFromObject(view, `object`)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val virtualPosition = position % getRealCount()
        // only expose virtual position to the inner adapter
        adapter.destroyItem(container, virtualPosition, `object`)
    }

    /*
	 * Delegate rest of methods directly to the inner adapter.
	 */
    override fun finishUpdate(container: ViewGroup) {
        adapter.finishUpdate(container)
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        adapter.restoreState(state, loader)
    }

    override fun startUpdate(container: ViewGroup) {
        adapter.startUpdate(container)
    }

    fun willBePageSelect(position: Int) {
        this.selectedPosition = position
    }
}