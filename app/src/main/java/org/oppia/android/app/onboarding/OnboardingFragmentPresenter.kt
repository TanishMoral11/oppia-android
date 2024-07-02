package org.oppia.android.app.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import org.oppia.android.R
import org.oppia.android.app.fragment.FragmentScope
import org.oppia.android.app.model.AppLanguageSelection
import org.oppia.android.app.model.OppiaLanguage
import org.oppia.android.app.model.Profile
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.translation.AppLanguageResourceHandler
import org.oppia.android.databinding.OnboardingAppLanguageSelectionFragmentBinding
import org.oppia.android.domain.oppialogger.OppiaLogger
import org.oppia.android.domain.profile.ProfileManagementController
import org.oppia.android.domain.translation.TranslationController
import org.oppia.android.util.data.AsyncResult
import org.oppia.android.util.data.DataProviders.Companion.toLiveData
import org.oppia.android.util.locale.OppiaLocale
import org.oppia.android.util.profile.CurrentUserProfileIdIntentDecorator.decorateWithUserProfileId
import javax.inject.Inject

/** The presenter for [OnboardingFragment]. */
@FragmentScope
class OnboardingFragmentPresenter @Inject constructor(
  private val activity: AppCompatActivity,
  private val fragment: Fragment,
  private val appLanguageResourceHandler: AppLanguageResourceHandler,
  private val profileManagementController: ProfileManagementController,
  private val oppiaLogger: OppiaLogger,
  private val translationController: TranslationController,
  private val onboardingAppLanguageViewModel: OnboardingAppLanguageViewModel
) {
  private lateinit var binding: OnboardingAppLanguageSelectionFragmentBinding
  private var profileId: ProfileId = ProfileId.getDefaultInstance()
  private var acceptDefaultLanguageSelection = true
  val hasProfileEverBeenAddedValue = ObservableField(true)

  /** Handle creation and binding of the [OnboardingFragment] layout. */
  fun handleCreateView(inflater: LayoutInflater, container: ViewGroup?): View {
    binding = OnboardingAppLanguageSelectionFragmentBinding.inflate(
      inflater,
      container,
      /* attachToRoot= */ false
    )

    getSystemLanguage()

    getSupportedLanguages()

    subscribeToWasProfileEverBeenAdded()

    binding.apply {
      lifecycleOwner = fragment

      onboardingLanguageTitle.text = appLanguageResourceHandler.getStringInLocaleWithWrapping(
        R.string.onboarding_language_activity_title,
        appLanguageResourceHandler.getStringInLocale(R.string.app_name)
      )

      val adapter = ArrayAdapter(
        fragment.requireContext(),
        R.layout.onboarding_language_dropdown_item,
        R.id.onboarding_language_text_view,
        onboardingAppLanguageViewModel.supportedAppLanguagesList
      )

      onboardingLanguageDropdown.apply {

        setAdapter(adapter)

        onboardingAppLanguageViewModel.languageSelectionLiveData.observe(
          fragment,
          { language ->
            setText(
              language,
              false
            )
          }
        )

        setRawInputType(EditorInfo.TYPE_NULL)

        onItemClickListener =
          AdapterView.OnItemClickListener { _, _, position, _ ->
            adapter.getItem(position).let {
              if (it != null) {
                acceptDefaultLanguageSelection = false
                updateSelectedLanguage(it)
              }
            }
          }
      }

      onboardingLanguageLetsGoButton.setOnClickListener {
        if (acceptDefaultLanguageSelection) {
          onboardingAppLanguageViewModel.languageSelectionLiveData.observe(
            fragment, { updateSelectedLanguage(it) }
          )
        }

        val intent =
          OnboardingProfileTypeActivity.createOnboardingProfileTypeActivityIntent(activity)
        intent.decorateWithUserProfileId(profileId)
        fragment.startActivity(intent)
      }
    }
    return binding.root
  }

  private fun updateSelectedLanguage(selectedLanguage: String) {
    val oppiaLanguage = appLanguageResourceHandler.getOppiaLanguageFromDisplayName(selectedLanguage)
    val selection = AppLanguageSelection.newBuilder().setSelectedLanguage(oppiaLanguage).build()
    translationController.updateAppLanguage(profileId, selection)
  }

  private fun getSystemLanguage() {
    translationController.getSystemLanguageLocale().toLiveData().observe(
      fragment,
      { result ->
        onboardingAppLanguageViewModel.setSelectedLanguageDisplayName(
          appLanguageResourceHandler.computeLocalizedDisplayName(
            processSystemLanguageResult(result)
          )
        )
      }
    )
  }

  private fun processSystemLanguageResult(
    result: AsyncResult<OppiaLocale.DisplayLocale>
  ): OppiaLanguage {
    return when (result) {
      is AsyncResult.Success -> {
        result.value.getCurrentLanguage()
      }
      is AsyncResult.Failure -> {
        oppiaLogger.e(
          "OnboardingFragment",
          "Failed to retrieve system language locale.",
          result.error
        )
        OppiaLanguage.ENGLISH
      }
      is AsyncResult.Pending -> OppiaLanguage.ENGLISH
    }
  }

  private fun getSupportedLanguages() {
    translationController.getSupportedAppLanguages().toLiveData().observe(
      fragment,
      { result ->
        when (result) {
          is AsyncResult.Success -> {
            val supportedLanguages = mutableListOf<String>()
            result.value.map {
              supportedLanguages.add(
                appLanguageResourceHandler.computeLocalizedDisplayName(
                  it
                )
              )
              onboardingAppLanguageViewModel.setSupportedAppLanguages(supportedLanguages)
            }
          }
          is AsyncResult.Failure -> {
            oppiaLogger.e(
              "OnboardingFragment",
              "Failed to retrieve supported language list.",
              result.error
            )
          }
          is AsyncResult.Pending -> {}
        }
      }
    )
  }

  private fun subscribeToWasProfileEverBeenAdded() {
    wasProfileEverBeenAdded.observe(
      fragment,
      {
        if (it) {
          retrieveNewProfileId()
        } else {
          createDefaultProfile()
        }
      }
    )
  }

  private val wasProfileEverBeenAdded: LiveData<Boolean> by lazy {
    Transformations.map(
      profileManagementController.getWasProfileEverAdded().toLiveData(),
      ::processWasProfileEverBeenAddedResult
    )
  }

  private fun processWasProfileEverBeenAddedResult(
    wasProfileEverBeenAddedResult: AsyncResult<Boolean>
  ): Boolean {
    return when (wasProfileEverBeenAddedResult) {
      is AsyncResult.Failure -> {
        oppiaLogger.e(
          "ProfileChooserFragment",
          "Failed to retrieve the information on wasProfileEverBeenAdded",
          wasProfileEverBeenAddedResult.error
        )
        false
      }
      is AsyncResult.Pending -> false
      is AsyncResult.Success -> wasProfileEverBeenAddedResult.value
    }
  }

  private fun createDefaultProfile() {
    profileManagementController.addProfile(
      name = "",
      pin = "",
      avatarImagePath = null,
      allowDownloadAccess = false,
      colorRgb = -10710042,
      isAdmin = false
    ).toLiveData()
      .observe(
        fragment,
        { result ->
          when (result) {
            is AsyncResult.Success -> retrieveNewProfileId()
            is AsyncResult.Failure -> {
              oppiaLogger.e(
                "OnboardingFragment", "Error creating the default profile", result.error
              )
              Profile.getDefaultInstance()
            }
            is AsyncResult.Pending -> {}
          }
        }
      )
  }

  private fun retrieveNewProfileId() {
    profileManagementController.getProfiles().toLiveData().observe(
      fragment,
      { profilesResult ->
        when (profilesResult) {
          is AsyncResult.Failure -> {
            oppiaLogger.e(
              "OnboardingFragment",
              "Failed to retrieve the list of profiles",
              profilesResult.error
            )
          }
          is AsyncResult.Pending -> {}
          is AsyncResult.Success -> {
            profileId = profilesResult.value.firstOrNull()?.id ?: ProfileId.getDefaultInstance()
          }
        }
      }
    )
  }
}
