package com.qu.qu;

import com.orm.SugarApp;
import com.qu.qu.net.QuEndpointsService;
import com.qu.qu.net.RetrofitService;
import com.squareup.otto.Bus;

import timber.log.Timber;

/**
 * Created by Taylor on 3/15/2015.
 */
public class BaseApplication extends SugarApp {

    private static QuEndpointsService quEndpointsService;

    private static Bus bus;

    public static QuEndpointsService getQuEndpointsService() {
        return quEndpointsService;
    }

    public static Bus getBus() {
        return bus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bus = new Bus();
        quEndpointsService = RetrofitService.createRestAdapter();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.HollowTree {
        @Override
        public void i(String message, Object... args) {
            // TODO e.g., Crashlytics.log(String.format(message, args));
        }

        @Override
        public void i(Throwable t, String message, Object... args) {
            i(message, args); // Just add to the log.
        }

        @Override
        public void e(String message, Object... args) {
            i("ERROR: " + message, args); // Just add to the log.
        }

        @Override
        public void e(Throwable t, String message, Object... args) {
            e(message, args);

            // TODO e.g., Crashlytics.logException(t);
        }
    }
}
