package org.oppia.android.domain.system

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import org.oppia.android.domain.oppialogger.ApplicationStartupListener

@Qualifier
annotation class LearnerAnalyticsInactivityLimit

@Module
class ApplicationLifecycleModule {

  @Provides
  @IntoSet
  fun bindLifecycleObserver(
    applicationLifecycleObserver: ApplicationLifecycleObserver
  ): ApplicationStartupListener = applicationLifecycleObserver

  @Provides
  @LearnerAnalyticsInactivityLimit
  fun provideLearnerAnalyticsInactivityLimit(): Int = 30
}