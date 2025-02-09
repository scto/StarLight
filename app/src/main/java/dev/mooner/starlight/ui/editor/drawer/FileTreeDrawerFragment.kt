package dev.mooner.starlight.ui.editor.drawer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import dev.mooner.starlight.R
import dev.mooner.starlight.databinding.FragmentFileTreeDrawerBinding
import dev.mooner.starlight.plugincore.logger.LoggerFactory
import dev.mooner.starlight.plugincore.project.Project
import dev.mooner.starlight.plugincore.utils.getStarLightDirectory
import java.io.File
import kotlin.properties.Delegates.notNull

private val logger = LoggerFactory.logger {  }

class FileTreeDrawerFragment : Fragment() {

    private var _binding: FragmentFileTreeDrawerBinding? = null
    val binding get() = _binding!!

    private var projectPath: String by notNull()
    private var projectId: String? = null
    private var mainScript: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectPath = arguments?.getString(ARG_PROJECT_PATH) ?: getStarLightDirectory().path
        projectId   = arguments?.getString(ARG_PROJECT_ID)
        mainScript  = arguments?.getString(ARG_MAIN_SCRIPT)
        logger.verbose { "File tree path: $projectPath, mainScript: $mainScript" }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileTreeDrawerBinding.inflate(inflater, container, false)

        binding.viewPager.apply {
            adapter = FileTreeViewPagerAdapter(
                fragment   = this@FileTreeDrawerFragment,
                mainScript = mainScript,
                rootPath   = projectPath,
                projectId  = projectId,
            )
            isUserInputEnabled = false
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val id = when(position) {
                        0 -> R.id.nav_file_tree
                        1 -> R.id.nav_logs
                        2 -> R.id.nav_warnings
                        else -> R.id.nav_file_tree
                    }
                    binding.topMenu.setItemSelected(
                        id = id,
                        isSelected = true,
                        dispatchAction = false
                    )
                }
            })
        }

        setOnTabSelectedListener { id ->
            val index = when(id) {
                R.id.nav_file_tree -> 0
                R.id.nav_logs ->
                    if (projectId == null) return@setOnTabSelectedListener else 1
                R.id.nav_warnings -> 2
                else -> 0
            }
            binding.viewPager.setCurrentItem(index, true)
        }

        return binding.root
    }

    fun openTabAt(index: Int) {
        binding.viewPager.setCurrentItem(index, false)
    }

    fun updateMessageCount(count: Int) {
        if (count <= 0)
            binding.topMenu.dismissBadge(R.id.nav_warnings)
        else
            binding.topMenu.showBadge(R.id.nav_warnings, count)
    }

    private fun setOnTabSelectedListener(block: (id: Int) -> Unit) =
        binding.topMenu.setOnItemSelectedListener(block)

    companion object {
        private const val ARG_PROJECT_PATH = "projectPath"
        private const val ARG_PROJECT_ID   = "projectId"
        private const val ARG_MAIN_SCRIPT  = "mainScript"

        @JvmStatic
        fun newInstance(basePath: File, project: Project?) =
            FileTreeDrawerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJECT_PATH, basePath.path)

                    if (project != null) {
                        putString(ARG_PROJECT_ID, project.info.id.toString())
                        putString(ARG_MAIN_SCRIPT, project.info.mainScript)
                    }
                }
            }
    }
}