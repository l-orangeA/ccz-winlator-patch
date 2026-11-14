package com.winlator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.winlatcn.R;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.Callback;
import com.winlator.core.PreloaderDialog;
import com.winlator.xenvironment.ImageFsInstaller;

/* loaded from: classes.dex */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Fragment currentFragment;
    private DrawerLayout drawerLayout;
    private Callback<Uri> openFileCallback;
    private SharedPreferences preferences;
    private int selectedProfileId;
    public final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
    private boolean editInputControls = false;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        this.drawerLayout = (DrawerLayout) findViewById(R.id.DrawerLayout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.NavigationView);
        navigationView.setNavigationItemSelectedListener(this);
        setSupportActionBar((Toolbar) findViewById(R.id.Toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        boolean booleanExtra = intent.getBooleanExtra("edit_input_controls", false);
        this.editInputControls = booleanExtra;
        if (booleanExtra) {
            this.selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.menu_item_input_controls));
            navigationView.setCheckedItem(R.id.menu_item_input_controls);
            return;
        }
        boolean showShortcutsFirst = this.preferences.getBoolean("show_shortcuts_first", false);
        int selectedMenuItemId = intent.getIntExtra("selected_menu_item_id", 0);
        int menuItemId = selectedMenuItemId > 0 ? selectedMenuItemId : showShortcutsFirst ? R.id.menu_item_shortcuts : R.id.menu_item_containers;
        actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_menu);
        onNavigationItemSelected(navigationView.getMenu().findItem(menuItemId));
        navigationView.setCheckedItem(menuItemId);
        if (!requestAppPermissions()) {
            ImageFsInstaller.installIfNeeded(this);
        }
        int containerId = intent.getIntExtra("container_id", 0);
        String startPath = intent.getStringExtra("start_path");
        if (containerId > 0 && startPath != null) {
            showFragment(new ContainerFileManagerFragment(containerId, startPath));
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == 0) {
                ImageFsInstaller.installIfNeeded(this);
            } else {
                finish();
            }
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Callback<Uri> callback;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == -1 && (callback = this.openFileCallback) != null) {
            callback.call(data.getData());
            this.openFileCallback = null;
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int i = newConfig.orientation;
        if (i == 2 || i == 1) {
            Fragment fragment = this.currentFragment;
            if (fragment instanceof BaseFileManagerFragment) {
                ((BaseFileManagerFragment) fragment).onOrientationChanged();
            }
        }
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        Fragment fragment = this.currentFragment;
        if (fragment != null && fragment.isVisible()) {
            Fragment fragment2 = this.currentFragment;
            if (fragment2 instanceof BaseFileManagerFragment) {
                BaseFileManagerFragment fileManagerFragment = (BaseFileManagerFragment) fragment2;
                if (fileManagerFragment.onBackPressed()) {
                    return;
                }
            } else if (fragment2 instanceof ContainersFragment) {
                finish();
            }
        }
        showFragment(new ContainersFragment());
    }

    public void setOpenFileCallback(Callback<Uri> openFileCallback) {
        this.openFileCallback = openFileCallback;
    }

    private boolean requestAppPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0 && ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
            return false;
        }
        String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
        ActivityCompat.requestPermissions(this, permissions, 1);
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_add || itemId == R.id.menu_item_home || itemId == R.id.menu_item_view_style || itemId == R.id.menu_item_new_folder) {
            return super.onOptionsItemSelected(menuItem);
        }
        if (this.editInputControls) {
            setResult(-1);
            finish();
        } else {
            Fragment fragment = this.currentFragment;
            if (fragment instanceof BaseFileManagerFragment) {
                BaseFileManagerFragment fileManagerFragment = (BaseFileManagerFragment) fragment;
                if (fileManagerFragment.onOptionsMenuClicked()) {
                    return true;
                }
            }
            this.drawerLayout.openDrawer(8388611);
        }
        return true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x005d, code lost:
    
        return true;
     */
    @Override // com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean onNavigationItemSelected(android.view.MenuItem r6) {
        /*
            r5 = this;
            androidx.fragment.app.FragmentManager r0 = r5.getSupportFragmentManager()
            int r1 = r0.getBackStackEntryCount()
            r2 = 1
            if (r1 <= 0) goto Lf
            r1 = 0
            r0.popBackStack(r1, r2)
        Lf:
            int r1 = r6.getItemId()
            java.lang.String r3 = "show_shortcuts_first"
            switch(r1) {
                case 2131296672: goto L5a;
                case 2131296676: goto L43;
                case 2131296688: goto L38;
                case 2131296701: goto L2f;
                case 2131296702: goto L19;
                default: goto L18;
            }
        L18:
            goto L5d
        L19:
            android.content.SharedPreferences r1 = r5.preferences
            android.content.SharedPreferences$Editor r1 = r1.edit()
            android.content.SharedPreferences$Editor r1 = r1.putBoolean(r3, r2)
            r1.apply()
            com.winlator.ShortcutsFragment r1 = new com.winlator.ShortcutsFragment
            r1.<init>()
            r5.showFragment(r1)
            goto L5d
        L2f:
            com.winlator.SettingsFragment r1 = new com.winlator.SettingsFragment
            r1.<init>()
            r5.showFragment(r1)
            goto L5d
        L38:
            com.winlator.InputControlsFragment r1 = new com.winlator.InputControlsFragment
            int r3 = r5.selectedProfileId
            r1.<init>(r3)
            r5.showFragment(r1)
            goto L5d
        L43:
            android.content.SharedPreferences r1 = r5.preferences
            android.content.SharedPreferences$Editor r1 = r1.edit()
            r4 = 0
            android.content.SharedPreferences$Editor r1 = r1.putBoolean(r3, r4)
            r1.apply()
            com.winlator.ContainersFragment r1 = new com.winlator.ContainersFragment
            r1.<init>()
            r5.showFragment(r1)
            goto L5d
        L5a:
            r5.showAboutDialog()
        L5d:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.MainActivity.onNavigationItemSelected(android.view.MenuItem):boolean");
    }

    public void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.FLFragmentContainer, fragment).commit();
        this.drawerLayout.closeDrawer(8388611);
        this.currentFragment = fragment;
    }

    private void showAboutDialog() {
        ContentDialog dialog = new ContentDialog(this, R.layout.about_dialog);
        dialog.findViewById(R.id.LLBottomBar).setVisibility(8);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView tvWebpage = (TextView) dialog.findViewById(R.id.TVWebpage);
            tvWebpage.setText(Html.fromHtml("<a href=\"https://www.winlator.org\">汉化：dreamboyn81</a>", 0));
            tvWebpage.setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) dialog.findViewById(R.id.TVAppVersion)).setText(getString(R.string.version) + " " + pInfo.versionName);
            String creditsAndThirdPartyAppsHTML = String.join("<br />", "Ubuntu RootFs (<a href=\"https://releases.ubuntu.com/focal\">Focal Fossa</a>)", "Wine (<a href=\"https://www.winehq.org\">winehq.org</a>)", "Box86/Box64 by <a href=\"https://github.com/ptitSeb\">ptitseb</a>", "PRoot (<a href=\"https://proot-me.github.io\">proot-me.github.io</a>)", "Mesa (Turnip/Zink/VirGL) (<a href=\"https://www.mesa3d.org\">mesa3d.org</a>)", "DXVK (<a href=\"https://github.com/doitsujin/dxvk\">github.com/doitsujin/dxvk</a>)", "VKD3D (<a href=\"https://gitlab.winehq.org/wine/vkd3d\">gitlab.winehq.org/wine/vkd3d</a>)", "D8VK (<a href=\"https://github.com/AlpyneDreams/d8vk\">github.com/AlpyneDreams/d8vk</a>)", "CNC DDraw (<a href=\"https://github.com/FunkyFr3sh/cnc-ddraw\">github.com/FunkyFr3sh/cnc-ddraw</a>)");
            TextView tvCreditsAndThirdPartyApps = (TextView) dialog.findViewById(R.id.TVCreditsAndThirdPartyApps);
            tvCreditsAndThirdPartyApps.setText(Html.fromHtml(creditsAndThirdPartyAppsHTML, 0));
            tvCreditsAndThirdPartyApps.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (PackageManager.NameNotFoundException e) {
        }
        dialog.show();
    }
}
