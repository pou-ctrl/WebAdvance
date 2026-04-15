package com.cylonid.nativealpha.model

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cylonid.nativealpha.R
import kotlin.jvm.JvmName
import com.cylonid.nativealpha.WebAppSettingsActivity
import com.cylonid.nativealpha.data.Converters
import com.cylonid.nativealpha.model.SandboxManager
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.Utility
import com.cylonid.nativealpha.helper.BiometricPromptHelper
import com.cylonid.nativealpha.util.ShortcutIconUtils
import java.util.Date

@Entity(tableName = "webapps")
@TypeConverters(Converters::class)
data class WebApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val iconUrl: String? = null,
    val category: String? = null,
    var userAgent: String? = null,
    val isJavaScriptEnabled: Boolean = true,
    val isAdblockEnabled: Boolean = false,
    val isDarkModeEnabled: Boolean = false,
    val refreshInterval: Long = 0, // 0 = manual
    val isSmartRefreshEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val pin: String? = null,
    var customDownloadFolder: String? = null,
    var clipboardMaxItems: Int = 50,
    val credentialAutoLockTimeout: Long = 300000, // 5 min
    val floatingWindowDefaultWidth: Int = 800,
    val floatingWindowDefaultHeight: Int = 600,
    val floatingWindowDefaultOpacity: Float = 1.0f,
    val screenshotSaveLocation: String = "app", // app or global
    val linkCopierDefaultFormat: String = "url", // url, url_title, markdown, html
    val userAgentOverride: String? = null,
    val cacheMode: String = "default",
    val status: Status = Status.ACTIVE,
    val lastUpdated: Date? = null,
    val notificationCount: Int = 0,
    val thumbnail: Bitmap? = null,
    val lastUrl: String? = null,
    val sessionData: String? = null, // JSON for cookies, localStorage
    val lastUsed: Date? = null,
    val usageCount: Int = 0,
    val customOrder: Int = 0,
    val customGroup: String? = null,
    val isActive: Boolean = true,
    val isBackground: Boolean = false,
    var isKeepAwake: Boolean = false,
    var isCameraPermission: Boolean = false,
    var isMicrophonePermission: Boolean = false,
    var isEnableZooming: Boolean = false,
    val isPinned: Boolean = false
) {
    enum class Status {
        ACTIVE,
        BACKGROUND,
        ERROR
    }

    @Ignore
    var baseUrl: String = ""
    @Ignore
    var ID: Int = -1
    @Ignore
    var title: String = ""
    @Ignore
    var isOpenUrlExternal: Boolean = false
    @Ignore
    var isAllowCookies: Boolean = true
    @Ignore
    var isAllowThirdPartyCookies: Boolean = false
    @Ignore
    var isRestorePage: Boolean = false
    @Ignore
    var isAllowJs: Boolean = true
    @Ignore
    var isActiveEntry: Boolean = true
    @Ignore
    var isRequestDesktop: Boolean = false
    @Ignore
    var isClearCache: Boolean = false
    @Ignore
    var isUseAdblock: Boolean = false
    @Ignore
    var isSendSavedataRequest: Boolean = false
    @Ignore
    var isBlockImages: Boolean = false
    @Ignore
    var isAllowHttp: Boolean = true
    @Ignore
    var isAllowLocationAccess: Boolean = false
    @Ignore
    var isUseCustomUserAgent: Boolean = false
    @Ignore
    var isAutoreload: Boolean = false
    @Ignore
    var timeAutoreload: Long = 0
    @Ignore
    var isForceDarkMode: Boolean = false
    @Ignore
    var isUseTimespanDarkMode: Boolean = false
    @Ignore
    var timespanDarkModeBegin: String = ""
    @Ignore
    var timespanDarkModeEnd: String = ""
    @Ignore
    var isIgnoreSslErrors: Boolean = false
    @Ignore
    var isShowExpertSettings: Boolean = false
    @Ignore
    var isSafeBrowsing: Boolean = true
    @Ignore
    var isBlockThirdPartyRequests: Boolean = false
    @Ignore
    var isDrmAllowed: Boolean = true
    @Ignore
    var isShowFullscreen: Boolean = false
    @Ignore
    var isOverrideGlobalSettings: Boolean = false
    @Ignore
    var containerId: Int = 0
    @Ignore
    var isUseContainer: Boolean = false

    var isBiometricProtection = false
    var isAllowMediaPlaybackInBackground = false
    var order = 0
    var alwaysUseFallbackContextMenu = false
    @Ignore
    var adBlockSettings = mutableListOf<AdblockConfig>()

    var iconUri: String? = null
    var group: String = "Default"
    @Ignore
    private var _clipboardSyncEnabled: Boolean = true
    @get:JvmName("isClipboardSyncEnabled")
    var clipboardSyncEnabled: Boolean
        get() = _clipboardSyncEnabled
        set(value) { _clipboardSyncEnabled = value }
    var floatingWindowWidth: Int = 360
    var floatingWindowHeight: Int = 640
    var floatingWindowOpacity: Int = 100
    var lastScrollPosition: Int = 0

    init {
        if (baseUrl.isEmpty()) {
            baseUrl = url
        }
        if (ID < 0) {
            ID = id.toInt()
        }
        if (title.isEmpty()) {
            title = baseUrl.replace("http://", "").replace("https://", "").replace("www.", "")
        }
        initDefaultSettings()
    }

    constructor(baseUrl: String, ID: Int, order: Int) : this(
        id = ID.toLong(),
        name = baseUrl,
        url = baseUrl
    ) {
        this.baseUrl = baseUrl
        this.ID = ID
        this.order = order
        title = baseUrl.replace("http://", "").replace("https://", "").replace("www.", "")
        initDefaultSettings()
    }

    constructor(baseUrl: String, ID: Int, adBlockSettings: MutableList<AdblockConfig>) : this(
        id = ID.toLong(),
        name = baseUrl,
        url = baseUrl
    ) {
        this.baseUrl = baseUrl
        this.ID = ID
        this.adBlockSettings = adBlockSettings
        title = baseUrl.replace("http://", "").replace("https://", "").replace("www.", "")
        initDefaultSettings()
    }

    constructor(other: WebApp) : this(
        id = other.id,
        name = other.name,
        url = other.url
    ) {
        baseUrl = other.baseUrl
        ID = other.ID
        title = other.title
        isOverrideGlobalSettings = other.isOverrideGlobalSettings
        containerId = other.containerId
        isUseContainer = other.isUseContainer
        copySettings(other)
    }



    //This part of the copy ctor should be callable independently from actual object construction to copy values of the global web app template
    fun copySettings(other: WebApp) {
        isOpenUrlExternal = other.isOpenUrlExternal
        isAllowCookies = other.isAllowCookies
        isAllowThirdPartyCookies = other.isAllowThirdPartyCookies
        isRestorePage = other.isRestorePage
        isAllowJs = other.isAllowJs
        isActiveEntry = other.isActiveEntry
        isRequestDesktop = other.isRequestDesktop
        isClearCache = other.isClearCache
        isUseAdblock = other.isUseAdblock
        isSendSavedataRequest = other.isSendSavedataRequest
        isBlockImages = other.isBlockImages
        isAllowHttp = other.isAllowHttp
        isAllowLocationAccess = other.isAllowLocationAccess
        userAgent = other.userAgent
        isUseCustomUserAgent = other.isUseCustomUserAgent
        isAutoreload = other.isAutoreload
        timeAutoreload = other.timeAutoreload
        isForceDarkMode = other.isForceDarkMode
        isUseTimespanDarkMode = other.isUseTimespanDarkMode
        timespanDarkModeBegin = other.timespanDarkModeBegin
        timespanDarkModeEnd = other.timespanDarkModeEnd
        isIgnoreSslErrors = other.isIgnoreSslErrors
        isShowExpertSettings = other.isShowExpertSettings
        isSafeBrowsing = other.isSafeBrowsing
        isBlockThirdPartyRequests = other.isBlockThirdPartyRequests
        isDrmAllowed = other.isDrmAllowed
        isShowFullscreen = other.isShowFullscreen
        isKeepAwake = other.isKeepAwake
        isCameraPermission = other.isCameraPermission
        isMicrophonePermission = other.isMicrophonePermission
        isEnableZooming = other.isEnableZooming
        isBiometricProtection = other.isBiometricProtection
        isAllowMediaPlaybackInBackground = other.isAllowMediaPlaybackInBackground
        order = other.order
        alwaysUseFallbackContextMenu = other.alwaysUseFallbackContextMenu
        adBlockSettings = other.adBlockSettings
        iconUri = other.iconUri
        group = other.group
        customDownloadFolder = other.customDownloadFolder
        clipboardMaxItems = other.clipboardMaxItems
        _clipboardSyncEnabled = other._clipboardSyncEnabled
        floatingWindowWidth = other.floatingWindowWidth
        floatingWindowHeight = other.floatingWindowHeight
        floatingWindowOpacity = other.floatingWindowOpacity
        lastScrollPosition = other.lastScrollPosition
    }

    private fun initDefaultSettings() {
        if (baseUrl.contains("facebook.com")) {
            userAgent = Const.DESKTOP_USER_AGENT
            isUseCustomUserAgent = true
        }
    }

    /*
        This function is used for settings where the ctor needs to have a different setting because
        we want different behaviour for already existing and newly created Web Apps.
            */
    fun applySettingsForNewWebApp() {
        isOverrideGlobalSettings = false
    }

    fun markInactive(activity: Activity) {
        isActiveEntry = false
        ShortcutIconUtils.deleteShortcuts(
            listOf(ID),
            activity
        )
    }


    val alphanumericBaseUrl: String
        get() = baseUrl.replace("\\P{Alnum}".toRegex(), "").replace("https", "").replace("http", "").replace("www", "")

    fun onSwitchCookiesChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val switchThirdPCookies = mSwitch.rootView.findViewById<SwitchCompat>(R.id.switch3PCookies)
        if (isChecked) switchThirdPCookies.isEnabled = true else {
            switchThirdPCookies.isEnabled = false
            switchThirdPCookies.isChecked = false
        }
    }

    private fun disableSwitchBiometricAccessChangeListener(switchBiometricAccess: SwitchCompat) {
        switchBiometricAccess.setOnCheckedChangeListener(null)
    }

    private fun enableSwitchBiometricAccessChangeListener(switchBiometricAccess: SwitchCompat,
                                                          activity: WebAppSettingsActivity) {
        switchBiometricAccess.setOnCheckedChangeListener { switch, checked ->
            onSwitchBiometricAccessChanged(
                switch,
                checked
            )
        }
    }

    private fun setSwitchBiometricAccessSilently(newValue: Boolean,
                                                 switchBiometricAccess: SwitchCompat,
                                                 activity: WebAppSettingsActivity) {
        disableSwitchBiometricAccessChangeListener(switchBiometricAccess)
        switchBiometricAccess.isChecked = newValue
        enableSwitchBiometricAccessChangeListener(switchBiometricAccess, activity)
    }

    fun onSwitchBiometricAccessChanged(
        mSwitch: CompoundButton,
        isChecked: Boolean
    ) {
        val switchBiometricAccess =
            mSwitch.rootView.findViewById<SwitchCompat>(R.id.switchBiometricAccess)
        
        // Get activity from the view's context
        val activity = (switchBiometricAccess.context as? WebAppSettingsActivity) 
            ?: (switchBiometricAccess.context as? android.app.Activity)?.let { it as? WebAppSettingsActivity }
            ?: return

        // reset to value before user toggled, actual setting of value is done by prompt success callback
        setSwitchBiometricAccessSilently(!switchBiometricAccess.isChecked, switchBiometricAccess, activity)

        if (!switchBiometricAccess.isChecked) {
            BiometricPromptHelper(activity as FragmentActivity).showPrompt(
                {
                    setSwitchBiometricAccessSilently(true, switchBiometricAccess, activity)
                    isBiometricProtection = true
                },
                {}, activity.getString(R.string.bioprompt_enable_restriction)
            )
        }
        if (switchBiometricAccess.isChecked) {
            BiometricPromptHelper(activity as FragmentActivity).showPrompt({
                setSwitchBiometricAccessSilently(false, switchBiometricAccess, activity)
                isBiometricProtection = false
            }, {}, activity.getString(R.string.bioprompt_disable_restricition)
            )
        }
    }

    fun onSwitchJsChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val switchDesktopVersion = mSwitch.rootView.findViewById<SwitchCompat>(R.id.switchDesktopSite)
        val switchAdblock = mSwitch.rootView.findViewById<SwitchCompat>(R.id.switchAdblock)
        if (isChecked) {
            switchDesktopVersion.isEnabled = true
            switchAdblock.isEnabled = true
        } else {
            switchDesktopVersion.isChecked = false
            switchDesktopVersion.isEnabled = false
            switchAdblock.isChecked = false
            switchAdblock.isEnabled = false
        }
    }

    fun onSwitchForceDarkChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val switchLimit = mSwitch.rootView.findViewById<SwitchCompat>(R.id.switchTimeSpanDarkMode)
        val txtBegin = mSwitch.rootView.findViewById<EditText>(R.id.textDarkModeBegin)
        val txtEnd = mSwitch.rootView.findViewById<EditText>(R.id.textDarkModeEnd)
        if (isChecked) {
            switchLimit.isEnabled = true
            txtBegin.isEnabled = true
            txtEnd.isEnabled = true
        } else {
            switchLimit.isChecked = false
            switchLimit.isEnabled = false
            txtBegin.isEnabled = false
            txtEnd.isEnabled = false
        }
    }

    fun onSwitchTimeSpanDarkChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val lblBegin = mSwitch.rootView.findViewById<TextView>(R.id.lblDarkModeBegin)
        val lblEnd = mSwitch.rootView.findViewById<TextView>(R.id.lblDarkModeEnd)
        val txtBegin = mSwitch.rootView.findViewById<EditText>(R.id.textDarkModeBegin)
        val txtEnd = mSwitch.rootView.findViewById<EditText>(R.id.textDarkModeEnd)
        if (isChecked) {
            lblBegin.isEnabled = true
            lblEnd.isEnabled = true
            txtBegin.isEnabled = true
            txtEnd.isEnabled = true
        } else {
            lblBegin.isEnabled = false
            lblEnd.isEnabled = false
            txtBegin.isEnabled = false
            txtEnd.isEnabled = false
        }
    }

    fun onSwitchUserAgentChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val txt = mSwitch.rootView.findViewById<EditText>(R.id.textUserAgent)
        val switchDesktopVersion = mSwitch.rootView.findViewById<SwitchCompat>(R.id.switchDesktopSite)
        if (isChecked) {
            switchDesktopVersion.isChecked = false
            switchDesktopVersion.isEnabled = false
            txt.isEnabled = true
        } else {
            txt.isEnabled = false
            switchDesktopVersion.isEnabled = true
        }
    }

    fun onSwitchAutoreloadChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val text = mSwitch.rootView.findViewById<EditText>(R.id.textReloadInterval)
        val label = mSwitch.rootView.findViewById<TextView>(R.id.labelReloadInterval)
        text.isEnabled = isChecked
        label.isEnabled = isChecked
    }

    fun onSwitchExpertSettingsChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        val expertSettings = mSwitch.rootView.findViewById<LinearLayout>(R.id.sectionExpertSettings)
        if (isChecked) expertSettings.visibility = View.VISIBLE else expertSettings.visibility = View.GONE
    }

    public fun onSwitchSandboxChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        isUseContainer = isChecked
        containerId = if (isChecked) {
            SandboxManager.getInstance().calculateNextFreeContainerId()
        } else {
            Const.NO_CONTAINER
        }
    }

    fun onSwitchOverrideGlobalSettingsChanged(mSwitch: CompoundButton, isChecked: Boolean) {
        isOverrideGlobalSettings = isChecked
        val sectionDetailedWebAppSettings = mSwitch.rootView.findViewById<LinearLayout>(R.id.sectionWebAppDetailSettings)
        Utility.setViewAndChildrenEnabled(sectionDetailedWebAppSettings, isChecked)
    }
}