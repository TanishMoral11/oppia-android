package org.oppia.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.oppia.android.app.viewmodel.ObservableViewModel
import javax.inject.Inject

class OnboardingAppLanguageViewModel @Inject constructor() :
  ObservableViewModel() {
  /** The selected app language displayed in the language dropdown. */
  val languageSelectionLiveData: LiveData<String> get() = _languageSelectionLiveData

  /** Set the app language selection. */
  fun setSelectedLanguageDisplayName(language: String) {
    _languageSelectionLiveData.value = language
  }

  private val _languageSelectionLiveData = MutableLiveData<String>()

  /** Get the list of app supported languages to be displayed in the language dropdown. */
  val supportedAppLanguagesList = mutableListOf<String>()

  /** Sets the list of app supported languages to be displayed in the language dropdown. */
  fun setSupportedAppLanguages(languageList: List<String>) {
    supportedAppLanguagesList.clear()
    supportedAppLanguagesList.addAll(languageList)
  }
}
