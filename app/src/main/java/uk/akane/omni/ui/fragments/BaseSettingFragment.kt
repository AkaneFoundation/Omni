package uk.akane.omni.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import uk.akane.omni.R
import uk.akane.omni.logic.enableEdgeToEdgePaddingListener

abstract class BaseSettingFragment(private val str: Int,
                                   private val fragmentCreator: () -> BasePreferenceFragment)
	: BaseFragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		val rootView = inflater.inflate(R.layout.fragment_top_settings, container, false)
		val topAppBar = rootView.findViewById<MaterialToolbar>(R.id.topAppBar)
		val collapsingToolbar =
			rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingtoolbar)

		rootView.findViewById<AppBarLayout>(R.id.appbarlayout).enableEdgeToEdgePaddingListener()
		collapsingToolbar.title = getString(str)

		topAppBar.setNavigationOnClickListener {
			requireActivity().supportFragmentManager.popBackStack()
		}

		childFragmentManager
			.beginTransaction()
			.addToBackStack(System.currentTimeMillis().toString())
			.add(R.id.settings, fragmentCreator())
			.commit()

		return rootView
	}
}