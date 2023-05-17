package org.oppia.android.app.options

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.oppia.android.app.fragment.InjectableFragment
import org.oppia.android.util.extensions.getStringFromBundle
import javax.inject.Inject

private const val APP_LANGUAGE_PREFERENCE_SUMMARY_VALUE_ARGUMENT_KEY =
  "AppLanguageFragment.app_language_preference_summary_value"
private const val SELECTED_LANGUAGE_SAVED_KEY = "AppLanguageFragment.selected_language"

/** The fragment to change the language of the app. */
class AppLanguageFragment : InjectableFragment(), AppLanguageRadioButtonListener {

  @Inject
  lateinit var appLanguageFragmentPresenter: AppLanguageFragmentPresenter

  companion object {
    fun newInstance(prefsSummaryValue: String): AppLanguageFragment {
      val fragment = AppLanguageFragment()
      val args = Bundle()
      args.putString(APP_LANGUAGE_PREFERENCE_SUMMARY_VALUE_ARGUMENT_KEY, prefsSummaryValue)
      fragment.arguments = args
      return fragment
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (fragmentComponent as Injector).inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val args =
      checkNotNull(arguments) { "Expected arguments to be passed to AppLanguageFragment" }
    val prefsSummaryValue = if (savedInstanceState == null) {
      args.getStringFromBundle(APP_LANGUAGE_PREFERENCE_SUMMARY_VALUE_ARGUMENT_KEY)
    } else {
      savedInstanceState.get(SELECTED_LANGUAGE_SAVED_KEY) as String
    }
    return appLanguageFragmentPresenter.handleOnCreateView(
      inflater,
      container,
      prefsSummaryValue!!
    )
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(
      SELECTED_LANGUAGE_SAVED_KEY,
      appLanguageFragmentPresenter.getLanguageSelected()
    )
  }

  override fun onLanguageSelected(appLanguage: String) {
    appLanguageFragmentPresenter.onLanguageSelected(appLanguage)
  }

  /** Dagger injector for [AppLanguageFragment]. */
  interface Injector {
    /** Injects dependencies into the [fragment]. */
    fun inject(fragment: AppLanguageFragment)
  }
}
