package com.olacabs.olaplaystudio.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.util.LongSparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.olacabs.olaplaystudio.OlaApplication
import com.olacabs.olaplaystudio.di.component.ConfigPersistentComponent
import com.olacabs.olaplaystudio.di.component.DaggerConfigPersistentComponent
import com.olacabs.olaplaystudio.di.component.FragmentComponent
import com.olacabs.olaplaystudio.di.module.FragmentModule
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract Fragment that every other Fragment in this application must implement. It handles
 * creation of Dagger components and makes sure that instances of ConfigPersistentComponent are kept
 * across configuration changes.
 */
abstract class BaseFragment : Fragment() {

    private var mFragmentComponent: FragmentComponent? = null
    private var mFragmentId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the FragmentComponent and reuses cached ConfigPersistentComponent if this is
        // being called after a configuration change.
        mFragmentId = savedInstanceState?.getLong(KEY_FRAGMENT_ID) ?: NEXT_ID.getAndIncrement()
        val configPersistentComponent: ConfigPersistentComponent
        if (sComponentsArray.get(mFragmentId) == null) {
            Timber.i("Creating new ConfigPersistentComponent id=%d", mFragmentId)
            configPersistentComponent = DaggerConfigPersistentComponent.builder()
                    .applicationComponent(OlaApplication[activity].component)
                    .build()
            sComponentsArray.put(mFragmentId, configPersistentComponent)
        } else {
            Timber.i("Reusing ConfigPersistentComponent id=%d", mFragmentId)
            configPersistentComponent = sComponentsArray.get(mFragmentId)
        }
        mFragmentComponent = configPersistentComponent.fragmentComponent(FragmentModule(this))
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View? = inflater?.inflate(layout, container, false)
        return view
    }

    abstract val layout: Int

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putLong(KEY_FRAGMENT_ID, mFragmentId)
    }

    override fun onDestroy() {
        if (!activity.isChangingConfigurations) {
            Timber.i("Clearing ConfigPersistentComponent id=%d", mFragmentId)
            sComponentsArray.remove(mFragmentId)
        }
        super.onDestroy()
    }

    fun fragmentComponent(): FragmentComponent {
        return mFragmentComponent as FragmentComponent
    }

    companion object {

        private val KEY_FRAGMENT_ID = "KEY_FRAGMENT_ID"
        private val sComponentsArray = LongSparseArray<ConfigPersistentComponent>()
        private val NEXT_ID = AtomicLong(0)
    }
}
