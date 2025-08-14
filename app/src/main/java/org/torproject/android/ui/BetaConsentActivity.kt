package org.torproject.android.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ustadmobile.meshrabiya.beta.BetaTestLogger
import com.ustadmobile.meshrabiya.beta.LogLevel
import org.torproject.android.R

class BetaConsentActivity : AppCompatActivity() {
    private lateinit var betaLogger: BetaTestLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beta_consent)

        betaLogger = BetaTestLogger.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.beta_consent_title)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BetaConsentFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class BetaConsentFragment : Fragment() {
    private lateinit var viewModel: BetaConsentViewModel
    private var consentDialog: ConsentDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[BetaConsentViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beta_consent, container, false)

        val statusText = view.findViewById<TextView>(R.id.tvConsentStatus)
        val enableButton = view.findViewById<Button>(R.id.btnEnableConsent)
        val changeButton = view.findViewById<Button>(R.id.btnChangeConsent)
        val exportButton = view.findViewById<Button>(R.id.btnExport)
        val clearButton = view.findViewById<Button>(R.id.btnClear)
        val statsGrid = view.findViewById<ViewGroup>(R.id.statsGrid)
        val totalEntriesText = view.findViewById<TextView>(R.id.tvTotalEntries)
        val privacyInfo = view.findViewById<ViewGroup>(R.id.privacyInfo)

        viewModel.consentGiven.observe(viewLifecycleOwner) { consent ->
            statusText.text = if (consent) getString(R.string.beta_consent_active, viewModel.loggingLevel.value) else getString(R.string.beta_consent_disabled)
            enableButton.visibility = if (consent) View.GONE else View.VISIBLE
            changeButton.visibility = if (consent) View.VISIBLE else View.GONE
            exportButton.visibility = if (consent) View.VISIBLE else View.GONE
            clearButton.visibility = if (consent) View.VISIBLE else View.GONE
            statsGrid.visibility = if (consent) View.VISIBLE else View.GONE
            totalEntriesText.visibility = if (consent) View.VISIBLE else View.GONE
            privacyInfo.visibility = if (consent) View.VISIBLE else View.GONE
        }

        viewModel.loggingStats.observe(viewLifecycleOwner) { stats ->
            view.findViewById<TextView>(R.id.tvMeshEvents).text = stats.meshEvents.toString()
            view.findViewById<TextView>(R.id.tvUserActions).text = stats.userActions.toString()
            view.findViewById<TextView>(R.id.tvNetworkConditions).text = stats.networkConditions.toString()
            view.findViewById<TextView>(R.id.tvBatteryImpacts).text = stats.batteryImpacts.toString()
            view.findViewById<TextView>(R.id.tvInstallationSteps).text = stats.installationSteps.toString()
            view.findViewById<TextView>(R.id.tvProtestMetrics).apply {
                text = stats.protestMetrics.toString()
                visibility = if (viewModel.loggingLevel.value == LoggingLevel.FULL) View.VISIBLE else View.GONE
            }
            totalEntriesText.text = getString(R.string.beta_consent_total_entries, stats.totalEntries())
        }

        enableButton.setOnClickListener {
            showConsentDialog()
        }
        changeButton.setOnClickListener {
            showConsentDialog()
        }
        exportButton.setOnClickListener {
            viewModel.exportData { stats, consent, level ->
                // TODO: Implement export logic (e.g. share intent)
            }
        }
        clearButton.setOnClickListener {
            viewModel.revokeConsent()
        }

        return view
    }

    private fun showConsentDialog() {
        if (consentDialog == null) {
            consentDialog = ConsentDialogFragment { consent, level ->
                viewModel.setConsent(consent, level)
            }
        }
        consentDialog?.show(parentFragmentManager, "ConsentDialog")
    }
} 