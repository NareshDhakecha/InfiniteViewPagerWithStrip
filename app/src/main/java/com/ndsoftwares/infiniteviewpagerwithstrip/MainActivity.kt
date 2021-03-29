package com.ndsoftwares.infiniteviewpagerwithstrip

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.ndsoftwares.infiniteviewpager.InfinitePagerAdapter
import com.ndsoftwares.infiniteviewpager.InfiniteViewPager
import com.ndsoftwares.infiniteviewpager.InfiniteViewPager.Companion.OFFSET
import com.ndsoftwares.infiniteviewpager.RecyclerTabLayout2
import hirondelle.date4j.DateTime
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    companion object{
        const val NUMBER_OF_PAGES = 6
    }

    private lateinit var tabs: RecyclerTabLayout2
    private lateinit var pageChangeListener: PageChangeListener
    private lateinit var fragments: ArrayList<ItemFragment>
    private var dateTimes: ArrayList<DateTime> = ArrayList()
    private lateinit var infiniteViewPager: InfiniteViewPager
    private var month: Int = -1
    private var year: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initContents(savedInstanceState)
    }

    private fun initContents(savedInstanceState: Bundle?) {
        val cal = Calendar.getInstance()

        // Get month, year
        month = cal[Calendar.MONTH] + 1
        year = cal[Calendar.YEAR]

        // Get current date time

        // Get current date time
        val currentDateTime = DateTime(year, month, 1, 0, 0, 0, 0)

        // Set to pageChangeListener
        pageChangeListener = PageChangeListener()
        pageChangeListener.setCurrentDateTime(currentDateTime)

        // Setup titles for the month fragments
        // Next month
        val nextDateTime = currentDateTime.plus(
                0, 1, 0, 0, 0, 0, 0,
                DateTime.DayOverflow.LastDay
        )

        // Next 2 month
        val next2DateTime = nextDateTime.plus(
                0, 1, 0, 0, 0, 0, 0,
                DateTime.DayOverflow.LastDay
        )

        // Next 3 month
        val next3DateTime = next2DateTime.plus(
                0, 1, 0, 0, 0, 0, 0,
                DateTime.DayOverflow.LastDay
        )

        // Previous month
        val prevDateTime = currentDateTime.minus(
                0, 1, 0, 0, 0, 0, 0,
                DateTime.DayOverflow.LastDay
        )
        // Previous 2 month
        val prev2DateTime = prevDateTime.minus(
                0, 1, 0, 0, 0, 0, 0,
                DateTime.DayOverflow.LastDay
        )

        // Add to the array of adapters
        dateTimes.add(currentDateTime)
        dateTimes.add(nextDateTime)
        dateTimes.add(next2DateTime)
        dateTimes.add(next3DateTime) // This is an extra
        dateTimes.add(prev2DateTime)
        dateTimes.add(prevDateTime)




        // Setup InfiniteViewPager and InfinitePagerAdapter. The
        // InfinitePagerAdapter is responsible
        // for reuse the fragments
        infiniteViewPager = findViewById<View>(R.id.months_infinite_pager) as InfiniteViewPager
        // Set enable swipe
        infiniteViewPager.isEnabled = true

        // ItemFragmentPagerAdapter actually provides 4 real fragments. The
        // InfinitePagerAdapter only recycles fragment provided by this

        // ItemFragmentPagerAdapter
        val itemFragmentPagerAdapter = ItemFragmentPagerAdapter(
                supportFragmentManager
        )

        // Provide initial data to the fragments, before they are attached to
        // view.
        fragments = itemFragmentPagerAdapter.getFragments()

        for (i in 0 until NUMBER_OF_PAGES) {
            val itemFragment: ItemFragment = fragments[i]
            val dt = dateTimes[i]
            itemFragment.dateTime = dt
        }

        // Set fragments to the pageChangeListener so it can refresh the textview
        // when page change
        pageChangeListener.setFragments(fragments)

        // Setup InfinitePagerAdapter to wrap around MonthPagerAdapter
        val infinitePagerAdapter = InfinitePagerAdapter(
                itemFragmentPagerAdapter
        )

        // Use the infinitePagerAdapter to provide data for dateViewPager
        infiniteViewPager.adapter = infinitePagerAdapter


        // Setup pageChangeListener
        infiniteViewPager.addOnPageChangeListener(pageChangeListener)

        // Setup pageChangeListener
        tabs = findViewById(R.id.tabs)
        tabs.setUpWithViewPager(infiniteViewPager)
        if(savedInstanceState == null) {
            tabs.setCurrentItem(OFFSET, false)
        }

    }

    class PageChangeListener: ViewPager.OnPageChangeListener{

        private var fragments: ArrayList<ItemFragment> = ArrayList()
        private var currentPage = OFFSET
        private var currentDateTime: DateTime? = null

        fun setCurrentDateTime(dateTime: DateTime?) {
            currentDateTime = dateTime
            //setCalendarDateTime(currentDateTime)
        }

        override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
        ) {

        }

        override fun onPageSelected(position: Int) {
            refreshFragments(position)
        }

        /**
         * Return virtual current position
         *
         * @param position
         * @return
         */
        fun getCurrent(position: Int): Int {
            return position % NUMBER_OF_PAGES
        }

        /**
         * Return virtual next position
         *
         * @param position
         * @return
         */
        private fun getNext(position: Int): Int {
            return (position + 1) % NUMBER_OF_PAGES
        }

        private fun getNext2(position: Int): Int {
            return (position + 2) % NUMBER_OF_PAGES
        }

        /**
         * Return virtual previous position
         *
         * @param position
         * @return
         */
        private fun getPrevious(position: Int): Int {
            return (position + 5) % NUMBER_OF_PAGES
        }

        private fun getPrevious2(position: Int): Int {
            return (position + 4) % NUMBER_OF_PAGES
        }

        private fun refreshFragments(position: Int) {
            // Get adapters to refresh
            val currentFragment: ItemFragment = fragments[getCurrent(position)]
            val prevFragment: ItemFragment = fragments[getPrevious(position)]
            val prev2Fragment: ItemFragment = fragments[getPrevious2(position)]
            val nextFragment: ItemFragment = fragments[getNext(position)]
            val next2Fragment: ItemFragment = fragments[getNext2(position)]



            currentDateTime?.let {
                when {
                    position == currentPage -> {
                        currentDateTime = it
                    }

                    // Detect if swipe right or swipe left
                    // Swipe right
                    position > currentPage -> {
                        // Update current date time to next month
                        currentDateTime = currentDateTime!!.plus(0, 1, 0, 0, 0, 0, 0,
                                DateTime.DayOverflow.LastDay)
                    }

                    // Swipe left
                    else -> {
                        // Update current date time to previous month
                        currentDateTime = currentDateTime!!.minus(0, 1, 0, 0, 0, 0, 0,
                                DateTime.DayOverflow.LastDay)
                    }
                }

                // Refresh current Fragment
                currentFragment.dateTime = currentDateTime!!

                // Refresh previous Fragment
                prevFragment.dateTime = currentDateTime!!.minus(0, 1, 0,
                        0, 0, 0, 0, DateTime.DayOverflow.LastDay)

                // Refresh previous 2 Fragment
                prev2Fragment.dateTime = currentDateTime!!.minus(0, 2, 0,
                        0, 0, 0, 0, DateTime.DayOverflow.LastDay)

                // Refresh next Fragment
                nextFragment.dateTime = currentDateTime!!.plus(0, 1, 0, 0,
                        0, 0, 0, DateTime.DayOverflow.LastDay)

                // Refresh next Fragment
                next2Fragment.dateTime = currentDateTime!!.plus(0, 2, 0, 0,
                        0, 0, 0, DateTime.DayOverflow.LastDay)
            }


            // Update current page
            currentPage = position

        }

        override fun onPageScrollStateChanged(state: Int) {

        }

        fun setFragments(fragments: ArrayList<ItemFragment>) {
            this.fragments = fragments
        }

    }
}