package com.owncloud.android.presentation.transfers

import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.extensions.statusToStringRes
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.workers.DownloadFileWorker
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class TransferListComposeFragment : Fragment() {

    private val transfersViewModel by viewModel<TransfersViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                Transfers()
            }
        }
    }

    @Composable
    fun Transfers() {
        var transfersState by remember { mutableStateOf(emptyList<Pair<OCTransfer, OCSpace?>>()) }
        val workInfos by transfersViewModel.workInfosListLiveData.asFlow().collectAsState(initial = emptyList())

        LaunchedEffect(transfersViewModel) {
            transfersViewModel.transfersWithSpaceStateFlow.collectLatest { transfers ->
                transfersState = transfers
            }
        }

        if (transfersState.isEmpty()) {
            EmptyList()
        } else {
            TransferList(transfersWithSpace = transfersState, workInfo = workInfos)
        }
    }

    @Composable
    fun TransferList(transfersWithSpace: List<Pair<OCTransfer, OCSpace?>>, workInfo: List<WorkInfo>) {
        val transfersGroupedByStatus = transfersWithSpace.groupBy { it.first.status }
        LazyColumn {
            transfersGroupedByStatus.forEach { (status, transfers) ->
                item {
                    UploadGroup(status = status, count = transfers.size)
                }
                transfers.sortedByDescending { it.first.transferEndTimestamp ?: it.first.id }
                    .forEach { (transfer, space) ->
                        item {
                            TransferItem(transfer = transfer, space = space, workInfo = workInfo)
                        }
                    }
            }
        }
    }

    @Composable
    fun TransferItem(transfer: OCTransfer, space: OCSpace?, workInfo: List<WorkInfo>) {
        val remoteFile = File(transfer.remotePath)
        var fileName = remoteFile.name
        if (fileName.isEmpty()) {
            fileName = File.separator
        }

        var path = ""
        remoteFile.parent?.let {
            path = if (it.endsWith("${OCFile.PATH_SEPARATOR}")) {
                it
            } else {
                "$it${OCFile.PATH_SEPARATOR}"
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .clickable(enabled = transfer.status == TransferStatus.TRANSFER_FAILED) { retryUpload(transfer) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    painter = painterResource(
                        id = MimetypeIconUtil.getFileTypeIconId(
                            MimetypeIconUtil.getBestMimeTypeByFilename(transfer.localPath),
                            fileName
                        )
                    ),
                    contentDescription = stringResource(id = R.string.content_description_thumbnail)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = colorResource(id = R.color.textColor),
                    fontSize = 16.sp,
                    text = fileName
                )
                Row {
                    Text(
                        color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        fontSize = 12.sp,
                        text = DisplayUtils.bytesToHumanReadable(transfer.fileSize, requireContext())
                    )
                    if (transfer.status != TransferStatus.TRANSFER_FAILED) {
                        transfer.transferEndTimestamp?.let {
                            val dateString = DisplayUtils.getRelativeDateTimeString(
                                requireContext(),
                                it,
                                DateUtils.SECOND_IN_MILLIS,
                                DateUtils.WEEK_IN_MILLIS,
                                0
                            )
                            Text(
                                color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                                fontSize = 12.sp,
                                text = ", $dateString"
                            )
                        }
                    }
                    if (transfer.status != TransferStatus.TRANSFER_SUCCEEDED) {
                        Text(
                            color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                            fontSize = 12.sp,
                            text = " — " + requireContext().getString(transfer.statusToStringRes())

                        )
                    }
                }

                if (transfer.status == TransferStatus.TRANSFER_IN_PROGRESS) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp),
                        color = colorResource(id = R.color.color_accent),
                        backgroundColor = colorResource(id = R.color.filelist_icon_background),
                        progress = checkProgress(transfer, workInfo) / 100f
                    )
                }

                Text(
                    color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                    maxLines = 1,
                    fontSize = 12.sp,
                    text = checkAccount(transfer)
                )
                if (space != null) {
                    SpacePathLine(space = space, parentPath = path)
                }
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            )
            {
                if (transfer.status != TransferStatus.TRANSFER_SUCCEEDED) {
                    val imageResource: Int = when (transfer.status) {
                        TransferStatus.TRANSFER_IN_PROGRESS, TransferStatus.TRANSFER_QUEUED -> {
                            R.drawable.ic_action_cancel_grey
                        }

                        TransferStatus.TRANSFER_FAILED -> {
                            R.drawable.ic_action_delete_grey
                        }

                        else -> {
                            R.drawable.ic_action_delete_grey
                        }
                    }
                    IconButton(onClick = { transfersViewModel.cancelUpload(transfer) }) {
                        Icon(
                            modifier = Modifier
                                .width(25.dp)
                                .height(25.dp),
                            painter = painterResource(id = imageResource),
                            tint = colorResource(id = R.color.half_black),
                            contentDescription = stringResource(id = R.string.content_description_cancel_delete_button)
                        )
                    }
                }
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth(),
            color = colorResource(id = R.color.filelist_icon_background),
            thickness = 1.dp
        )
    }

    @Composable
    fun SpacePathLine(space: OCSpace, parentPath: String) {
        val spaceName: String
        val spaceImage: Int

        if (space.isPersonal) {
            spaceName = stringResource(id = R.string.bottom_nav_personal)
            spaceImage = R.drawable.ic_folder
        } else {
            spaceName = space.name
            spaceImage = R.drawable.ic_spaces
        }


        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .width(15.dp)
                    .height(15.dp)
                    .padding(end = 2.dp),
                painter = painterResource(id = spaceImage),
                tint = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                contentDescription = stringResource(id = R.string.content_description_space_icon)
            )
            Text(
                modifier = Modifier
                    .padding(end = 8.dp),
                color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 12.sp,
                text = spaceName
            )
            Text(
                color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 12.sp,
                text = parentPath
            )
        }
    }

    @Composable
    fun EmptyList() {
        Column(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier
                    .width(72.dp)
                    .height(72.dp),
                tint = colorResource(id = R.color.grey),
                painter = painterResource(id = R.drawable.ic_uploads),
                contentDescription = stringResource(id = R.string.content_description_empty_dataset_icon)
            )
            Text(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                color = colorResource(id = R.color.half_black),
                fontFamily = FontFamily.SansSerif,
                fontStyle = FontStyle.Normal,
                fontSize = 16.sp,
                letterSpacing = 0.009375.sp,
                fontWeight = FontWeight.Bold,
                text = stringResource(id = R.string.upload_list_empty)
            )
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp),
                color = colorResource(id = R.color.grey),
                fontFamily = FontFamily.SansSerif,
                fontStyle = FontStyle.Normal,
                fontSize = 16.sp,
                letterSpacing = 0.03125.sp,
                text = stringResource(id = R.string.upload_list_empty_subtitle)
            )
        }

    }

    @Composable
    fun UploadGroup(status: TransferStatus, count: Int) {
        val stringResFileCount =
            if (count == 1) R.string.uploads_view_group_file_count_single else R.string.uploads_view_group_file_count
        val fileCountText: String = String.format(requireContext().getString(stringResFileCount), count)

        Column {
            Row(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 5.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp),
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.color_accent),
                    text = requireContext().getString(headerTitleStringRes(status)).uppercase()
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.half_black),
                    text = fileCountText.uppercase()
                )
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = colorResource(id = R.color.filelist_icon_background),
                thickness = 2.dp
            )
            Row {
                if (status == TransferStatus.TRANSFER_FAILED || status == TransferStatus.TRANSFER_SUCCEEDED) {
                    Button(
                        modifier = Modifier
                            .padding(start = 13.dp),
                        onClick = {
                            if (status == TransferStatus.TRANSFER_FAILED) {
                                transfersViewModel.clearFailedTransfers()
                            } else {
                                transfersViewModel.clearSuccessfulTransfers()
                            }
                        },
                        shape = RoundedCornerShape(1.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.primary_button_background_color))
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_upload_clear).uppercase(),
                            color = colorResource(id = R.color.white),
                        )
                    }
                }
                if (status == TransferStatus.TRANSFER_FAILED) {
                    Button(
                        modifier = Modifier
                            .padding(start = 8.dp),
                        onClick = { transfersViewModel.retryFailedTransfers() },
                        shape = RoundedCornerShape(1.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.color_accent))
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_upload_retry).uppercase(),
                            color = colorResource(id = R.color.white),
                        )
                    }
                }

            }
        }
        if (status == TransferStatus.TRANSFER_FAILED || status == TransferStatus.TRANSFER_SUCCEEDED) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth(),
                color = colorResource(id = R.color.filelist_icon_background),
                thickness = 1.dp
            )
        }

    }

    private fun headerTitleStringRes(status: TransferStatus): Int {
        return when (status) {
            TransferStatus.TRANSFER_IN_PROGRESS -> R.string.uploads_view_group_current_uploads
            TransferStatus.TRANSFER_FAILED -> R.string.uploads_view_group_failed_uploads
            TransferStatus.TRANSFER_SUCCEEDED -> R.string.uploads_view_group_finished_uploads
            TransferStatus.TRANSFER_QUEUED -> R.string.uploads_view_group_queued_uploads
        }
    }

    private fun checkAccount(transfer: OCTransfer): String {
        return try {
            val account = AccountUtils.getOwnCloudAccountByName(requireContext(), transfer.accountName)
            val ownCloudAccount = OwnCloudAccount(account, requireContext())
            val accountName = ownCloudAccount.displayName + " @ " +
                    DisplayUtils.convertIdn(account.name.substring(account.name.lastIndexOf("@") + 1), false)
            accountName
        } catch (e: Exception) {
            Timber.w("Couldn't get display name for account, using old style")
            transfer.accountName
        }
    }

    private fun retryUpload(transfer: OCTransfer) {
        if (transfer.lastResult == TransferResult.CREDENTIAL_ERROR) {
            val parentActivity = requireActivity() as FileActivity
            val account = AccountUtils.getOwnCloudAccountByName(requireContext(), transfer.accountName)
            parentActivity.fileOperationsHelper.checkCurrentCredentials(account)
        } else {
            val file = File(transfer.localPath)
            if (file.exists()) {
                transfersViewModel.retryUploadFromSystem(transfer.id!!)
            } else if (DocumentFile.isDocumentUri(requireContext(), Uri.parse(transfer.localPath))) {
                transfersViewModel.retryUploadFromContentUri(transfer.id!!)
            } else {
                showMessageInSnackbar(getString(R.string.local_file_not_found_toast))
            }
        }
    }

    private fun checkProgress(transfer: OCTransfer, workInfo: List<WorkInfo>): Int {
        workInfo.forEach { workInfo ->
            if (workInfo.tags.contains(transfer.id.toString())) {
                return workInfo.progress.getInt(DownloadFileWorker.WORKER_KEY_PROGRESS, -1)
            }
        }
        return 0
    }
}
