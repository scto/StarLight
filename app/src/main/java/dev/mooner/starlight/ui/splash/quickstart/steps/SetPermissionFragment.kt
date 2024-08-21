/*
 * SetPermissionFragment.kt created by Minki Moon(mooner1022) on 22. 2. 5. 오후 3:34
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.ui.splash.quickstart.steps

import android.Manifest.permission.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import dev.mooner.configdsl.ConfigItemBuilder
import dev.mooner.configdsl.ConfigStructure
import dev.mooner.configdsl.Icon
import dev.mooner.configdsl.adapters.ConfigAdapter
import dev.mooner.configdsl.config
import dev.mooner.configdsl.options.button
import dev.mooner.peekalert.PeekAlert
import dev.mooner.starlight.databinding.FragmentSetPermissionBinding
import dev.mooner.starlight.ui.splash.quickstart.QuickStartActivity
import dev.mooner.starlight.utils.createFailurePeek
import dev.mooner.starlight.utils.requestManageStoragePermission
import java.util.*

class SetPermissionFragment : Fragment() {

    private var _binding: FragmentSetPermissionBinding? = null
    private val binding get() = _binding!!
    private var adapter: ConfigAdapter? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        for ((permission, isGranted) in permissions) {
            if (permission == MANAGE_EXTERNAL_STORAGE) {
                if (isPermissionGranted(permission))
                    continue
                createFailurePeek("모든 권한이 승인되지 않았어요. $permission", PeekAlert.Position.Bottom).peek()
                adapter?.redraw()
                return@registerForActivityResult
            } else if (!isGranted) {
                createFailurePeek("모든 권한이 승인되지 않았어요. $permission", PeekAlert.Position.Bottom).peek()
                adapter?.redraw()
                return@registerForActivityResult
            }
        }
        adapter?.redraw()

        (requireActivity() as QuickStartActivity).setNextButtonEnabled(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetPermissionBinding.inflate(inflater, container, false)
        val activity = (activity as QuickStartActivity)

        activity.setPrevButtonEnabled(false)
        activity.setNextButtonEnabled(false)

        adapter = ConfigAdapter.Builder(activity) {
            bind(binding.recyclerView)
            structure {
                getStructure()
            }
            lifecycleOwner(this@SetPermissionFragment)
        }.build()

        return binding.root
    }

    private fun getStructure(): ConfigStructure {
        return config {
            category {
                id = "permissions"
                title = "아래 필수 권한들을 허용해주세요!"
                //textColor = activity.getColor(R.color.main_bright)
                items {
                    for (permission in REQUIRED_PERMISSIONS) {
                        if (permission == MANAGE_EXTERNAL_STORAGE)
                            continue
                        getButtonByPermission(permission)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        getButtonByPermission(MANAGE_EXTERNAL_STORAGE)
                }
            }
            category {
                id = "permission_grant"
                items {
                    button {
                        id = "grant_permissions"
                        title = "권한 허용하기"
                        icon = Icon.CHECK
                        setOnClickListener { _ ->
                            if (MANAGE_EXTERNAL_STORAGE in REQUIRED_PERMISSIONS && !isPermissionGranted(MANAGE_EXTERNAL_STORAGE))
                                requireContext().requestManageStoragePermission()
                            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
                        }
                    }
                }
            }
        }
    }

    private fun ConfigItemBuilder.getButtonByPermission(permission: String) {
        val mTitle: String
        val mDesc: String

        when(permission) {
            INTERNET -> {
                mTitle = "인터넷"
                mDesc = "인터넷에 접속할 수 있어요. (필수)"
            }
            READ_EXTERNAL_STORAGE -> {
                mTitle = "저장소 읽기"
                mDesc = "기기의 파일에 접근하여 데이터를 읽을 수 있어요. (필수)"
            }
            WRITE_EXTERNAL_STORAGE -> {
                mTitle = "저장소 쓰기"
                mDesc = "기기의 파일에 접근하여 데이터를 읽을 수 있어요. (필수)"
            }
            MANAGE_EXTERNAL_STORAGE -> {
                mTitle = "저장소 관리(MANAGE_EXTERNAL_STORAGE)"
                mDesc = "안드로이드 11 이상의 기기에서는 이 권한을 허용해야 해요. (필수)"
            }
            QUERY_ALL_PACKAGES -> {
                mTitle = "설치된 앱 확인"
                mDesc = "설치된 앱의 목록을 확인할 수 있어요. (필수)"
            }
            FOREGROUND_SERVICE -> {
                mTitle = "포그라운드 서비스"
                mDesc = "앱이 종료되지 않게 유지할 수 있어요. (필수)"
            }
            else -> {
                mTitle = "알 수 없는 권한($permission)"
                mDesc = "알 수 없는 권한이에요. 버그죠."
            }
        }
        val isGranted = isPermissionGranted(permission)
        val permissionColor = if (isGranted) COLOR_GRANTED else COLOR_REJECTED
        val permissionIcon = if (isGranted) Icon.CHECK else Icon.ERROR
        button {
            id = UUID.randomUUID().toString()
            title = mTitle
            description = mDesc
            icon = permissionIcon
            iconTintColor = permissionColor
        }
    }

    private fun isPermissionGranted(permission: String) = when(permission) {
        MANAGE_EXTERNAL_STORAGE ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
        else ->
            requireContext().checkSelfPermission(permission) == PERMISSION_GRANTED
    }

    companion object {
        val REQUIRED_PERMISSIONS = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    INTERNET,
                    FOREGROUND_SERVICE,
                    MANAGE_EXTERNAL_STORAGE,
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                arrayOf(
                    INTERNET,
                    FOREGROUND_SERVICE,
                    MANAGE_EXTERNAL_STORAGE,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                arrayOf(
                    INTERNET,
                    FOREGROUND_SERVICE,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            }
            else -> {
                arrayOf(
                    INTERNET,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            }
        }

        private val COLOR_GRANTED = Color.parseColor("#4ADE80")
        private val COLOR_REJECTED = Color.parseColor("#FF6F6F")
    }
}