package org.ebookdroid.ui.library;

import org.ebookdroid.R;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.ui.library.adapters.BooksAdapter;
import org.ebookdroid.ui.library.adapters.FileListAdapter;
import org.ebookdroid.ui.library.adapters.RecentAdapter;
import org.ebookdroid.ui.library.views.BookcaseView;
import org.ebookdroid.ui.library.views.LibraryView;
import org.ebookdroid.ui.library.views.RecentBooksView;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.IActionController;
import org.emdev.utils.android.AndroidVersion;

@ActionTarget(
// actions
actions = {
// start
@ActionMethodDef(id = R.id.mainmenu_about, method = "showAbout")
// finish
})
public class RecentActivity extends AbstractActionActivity {

    public final LogContext LCTX;

    private static final AtomicLong SEQ = new AtomicLong();

    public static final int VIEW_RECENT = 0;
    public static final int VIEW_LIBRARY = 1;

    private ViewFlipper viewflipper;
    private ImageView libraryButton;
    private BookcaseView bookcaseView;
    private RecentBooksView recentBooksView;
    private LibraryView libraryView;

    private RecentActivityController controller;

    public RecentActivity() {
        super();
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return controller;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate()");
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recent);
        libraryButton = (ImageView) findViewById(R.id.recent_showlibrary);
        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);

        final Object last = this.getLastNonConfigurationInstance();
        if (last instanceof RecentActivityController) {
            this.controller = (RecentActivityController) last;
            controller.onRestore(this);
        } else {
            this.controller = new RecentActivityController(this);
            controller.onCreate();
        }

        if (AndroidVersion.VERSION == 3) {
            setActionForView(R.id.recent_showlibrary);
            setActionForView(R.id.recent_showbrowser);
            setActionForView(R.id.ShelfLeftButton);
            setActionForView(R.id.ShelfCaption);
            setActionForView(R.id.ShelfRightButton);
        }
    }

    @Override
    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }
        super.onResume();
        controller.onResume();
    }

    @Override
    protected void onPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause(): " + isFinishing());
        }
        super.onPause();
        controller.onPause();
    }

    @Override
    protected void onDestroy() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy(): " + isFinishing());
        }
        super.onDestroy();
        controller.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void changeLibraryView(final int view) {
        if (view == VIEW_LIBRARY) {
            viewflipper.setDisplayedChild(VIEW_LIBRARY);
            libraryButton.setImageResource(R.drawable.actionbar_recent);
        } else {
            viewflipper.setDisplayedChild(VIEW_RECENT);
            libraryButton.setImageResource(R.drawable.actionbar_library);
        }
    }

    int getViewMode() {
        return viewflipper.getDisplayedChild();
    }

    @Override
    protected IActionController<? extends AbstractActionActivity> createController() {
        if (controller == null) {
            controller = new RecentActivityController(this);
        }
        return controller;
    }

    void showBookshelf(final int shelfIndex) {
        if (bookcaseView != null) {
            bookcaseView.setCurrentList(shelfIndex);
        }
    }

    void showNextBookshelf() {
        if (bookcaseView != null) {
            bookcaseView.nextList();
        }
    }

    void showPrevBookshelf() {
        if (bookcaseView != null) {
            bookcaseView.prevList();
        }
    }

    void showBookcase(final BooksAdapter bookshelfAdapter, final RecentAdapter recentAdapter) {
        if (bookcaseView == null) {
            bookcaseView = new BookcaseView(controller, bookshelfAdapter);
        }
        viewflipper.removeAllViews();
        viewflipper.addView(bookcaseView, 0);

        libraryButton.setImageResource(R.drawable.actionbar_shelf);
    }

    void showLibrary(final FileListAdapter libraryAdapter, final RecentAdapter recentAdapter) {
        if (recentBooksView == null) {
            recentBooksView = new RecentBooksView(controller, recentAdapter);
        }
        if (libraryView == null) {
            libraryView = new LibraryView(controller, libraryAdapter);
        }

        viewflipper.removeAllViews();
        viewflipper.addView(recentBooksView, VIEW_RECENT);
        viewflipper.addView(libraryView, VIEW_LIBRARY);

        libraryButton.setImageResource(R.drawable.actionbar_library);
    }
}
