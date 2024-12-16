package org.oppia.android.app.policies

import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import org.oppia.android.R
import org.oppia.android.app.fragment.FragmentScope
import org.oppia.android.app.model.PoliciesFragmentArguments
import org.oppia.android.app.model.PolicyPage
import org.oppia.android.app.translation.AppLanguageResourceHandler
import org.oppia.android.databinding.PoliciesFragmentBinding
import org.oppia.android.util.parser.html.HtmlParser
import org.oppia.android.util.parser.html.PolicyType
import javax.inject.Inject
import org.oppia.android.util.parser.html.LeftAlignedSymbolsSpan

/** The presenter for [PoliciesFragment]. */
@FragmentScope
class PoliciesFragmentPresenter @Inject constructor(
  private val activity: AppCompatActivity,
  private val htmlParserFactory: HtmlParser.Factory,
  private val resourceHandler: AppLanguageResourceHandler
) : HtmlParser.PolicyOppiaTagActionListener {

  /** Handles onCreate() method of the [PoliciesFragment]. */
  fun handleCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    policiesFragmentArguments: PoliciesFragmentArguments
  ): View {
    val binding = PoliciesFragmentBinding.inflate(
      inflater,
      container,
      /* attachToRoot= */ false
    )
    setUpContentForTextViews(policiesFragmentArguments.policyPage, binding)

    return binding.root
  }

  private fun setUpContentForTextViews(
    policyPage: PolicyPage,
    binding: PoliciesFragmentBinding
  ) {
    var policyDescription = ""
    var policyWebLink = ""

    if (policyPage == PolicyPage.PRIVACY_POLICY) {
      policyDescription = resourceHandler.getStringInLocale(R.string.privacy_policy_content)
      policyWebLink = resourceHandler.getStringInLocale(R.string.privacy_policy_web_link)
    } else if (policyPage == PolicyPage.TERMS_OF_SERVICE) {
      policyDescription = resourceHandler.getStringInLocale(R.string.terms_of_service_content)
      policyWebLink = resourceHandler.getStringInLocale(R.string.terms_of_service_web_link)
    }

    val parsedHtmlDescription = htmlParserFactory.create(
      policyOppiaTagActionListener = this,
      displayLocale = resourceHandler.getDisplayLocale()
    ).parseOppiaHtml(
      rawString = policyDescription,
      htmlContentTextView = binding.policyDescriptionTextView,
      supportsLinks = true,
      supportsConceptCards = false
    )

    binding.policyDescriptionTextView.apply {
      layoutDirection = View.LAYOUT_DIRECTION_LTR
      textAlignment = View.TEXT_ALIGNMENT_TEXT_START
      textDirection = View.TEXT_DIRECTION_LTR
      setSingleLine(false)
      setMaxLines(Int.MAX_VALUE)
    }

    val spannableString = SpannableString(parsedHtmlDescription)
    parsedHtmlDescription.split("\n").forEachIndexed { lineIndex, line ->
      val lineStart = parsedHtmlDescription.indexOf(line)
      if (line.trimStart().startsWith("•")) {
        val bulletIndex = lineStart + line.indexOf("•")
        spannableString.setSpan(
          LeftAlignedSymbolsSpan(),
          bulletIndex,
          bulletIndex + 1,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
      }
    }
    binding.policyDescriptionTextView.text = spannableString

    binding.policyWebLinkTextView.text = htmlParserFactory.create(
      gcsResourceName = "",
      entityType = "",
      entityId = "",
      imageCenterAlign = false,
      customOppiaTagActionListener = null,
      resourceHandler.getDisplayLocale()
    ).parseOppiaHtml(
      policyWebLink,
      binding.policyWebLinkTextView,
      supportsLinks = true,
      supportsConceptCards = false
    )
  }

  override fun onPolicyPageLinkClicked(policyType: PolicyType) {
    when (policyType) {
      PolicyType.PRIVACY_POLICY ->
        (activity as RouteToPoliciesListener).onRouteToPolicies(PolicyPage.PRIVACY_POLICY)
      PolicyType.TERMS_OF_SERVICE ->
        (activity as RouteToPoliciesListener).onRouteToPolicies(PolicyPage.TERMS_OF_SERVICE)
    }
  }
}
