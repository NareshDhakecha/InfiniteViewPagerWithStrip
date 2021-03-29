package com.ndsoftwares.infiniteviewpagerwithstrip

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.ndsoftwares.infiniteviewpagerwithstrip.MainActivity.Companion.NUMBER_OF_PAGES
import hirondelle.date4j.DateTime

class ItemFragmentPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var fragments: ArrayList<ItemFragment>? = null

    // Lazily create the fragments
    fun getFragments(): ArrayList<ItemFragment> {
        if (fragments == null) {
            fragments = ArrayList()
            for (i in 0 until count) {
                fragments!!.add(ItemFragment())
            }
        }
        return fragments!!
    }

    fun setFragments(fragments: ArrayList<ItemFragment>) {
        this.fragments = fragments
    }

    override fun getCount(): Int {

        // We need 6 fragments for previous 2 month, previous month, current month, next month and next 2 month,
        // and 1 extra fragment for fragment recycle
        return NUMBER_OF_PAGES
    }

    override fun getItem(position: Int): Fragment {
        return getFragments()[position]
    }

    override fun getPageTitle(position: Int): CharSequence {
        val itemFragment: ItemFragment = fragments!![position]
        return itemFragment.getTitle()
    }

    fun getDateTime(position: Int): DateTime{
        val itemFragment: ItemFragment = fragments!![position]
        return itemFragment.dateTime
    }
}