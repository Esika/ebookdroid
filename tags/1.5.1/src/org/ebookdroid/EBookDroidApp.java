package org.ebookdroid;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.BackupSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.listeners.IAppSettingsChangeListener;
import org.ebookdroid.common.settings.listeners.IBackupSettingsChangeListener;

import org.emdev.BaseDroidApp;
import org.emdev.common.android.VMRuntimeHack;
import org.emdev.common.backup.BackupManager;
import org.emdev.common.fonts.FontManager;
import org.emdev.utils.concurrent.Flag;

public class EBookDroidApp extends BaseDroidApp implements IAppSettingsChangeListener, IBackupSettingsChangeListener {

    public static final Flag initialized = new Flag();

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        SettingsManager.init(this);
        CacheManager.init(this);
        FontManager.init();

        VMRuntimeHack.preallocateHeap(AppSettings.current().heapPreallocate);

        SettingsManager.addListener(this);
        onAppSettingsChanged(null, AppSettings.current(), null);
        onBackupSettingsChanged(null, BackupSettings.current(), null);

        initialized.set();
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Application#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        BitmapManager.clear("on Low Memory: ");
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings,
            final AppSettings.Diff diff) {

        BitmapManager.setPartSize(1 << newSettings.bitmapSize);
        BitmapManager.setUseEarlyRecycling(newSettings.useEarlyRecycling);
        BitmapManager.setUseBitmapHack(newSettings.useBitmapHack);

        setAppLocale(newSettings.lang);
    }

    @Override
    public void onBackupSettingsChanged(final BackupSettings oldSettings, final BackupSettings newSettings,
            final BackupSettings.Diff diff) {
        BackupManager.setMaxNumberOfAutoBackups(newSettings.maxNumberOfAutoBackups);
    }
}