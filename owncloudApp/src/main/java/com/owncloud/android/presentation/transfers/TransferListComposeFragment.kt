package com.owncloud.android.presentation.transfers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.owncloud.android.R

class TransferListComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                Column {
                    UploadGroup()
                    TransferList()
                }
            }
        }
    }
}

@Composable
fun TransferList(modifier: Modifier = Modifier){

    LazyColumn{
        item { TransferItem() }
        item { TransferItem() }
        item { TransferItem() }
    }
}


@Composable
fun TransferItem(modifier: Modifier = Modifier){
    Row (
        modifier = Modifier
            .layoutId("LisItemLayout")
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box (
            modifier = Modifier
                .width(60.dp)
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(modifier = Modifier
                .layoutId("Thumbnail")
                .width(32.dp)
                .height(32.dp),
                painter = painterResource(id = R.drawable.ic_menu_archive),
                contentDescription = "Thumbnail")
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                modifier = Modifier
                    .layoutId("upload_name"),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = colorResource(id = R.color.textColor),
                fontSize = 16.sp,
                text = stringResource(id = R.string.placeholder_filename)
            )
            Row (modifier = modifier){
                Text(
                    modifier = Modifier
                        .layoutId("upload_file_size"),
                    color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 12.sp,
                    text = stringResource(id = R.string.placeholder_filesize)
                )
                Text (
                    modifier = Modifier
                        .layoutId("upload_date"),
                    color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                    fontSize = 12.sp,
                    text = stringResource(id = R.string.placeholder_timestamp)
                )
                Text(
                    modifier = Modifier
                        .layoutId("upload_status"),
                    color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                    fontSize = 12.sp,
                    text = stringResource(id = R.string.uploads_view_upload_status_succeeded)

                )
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .layoutId("upload_progress_bar")
                    .fillMaxWidth()
            )
            Text(
                modifier = Modifier
                    .layoutId("upload_account"),
                color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
                maxLines = 1,
                fontSize = 12.sp,
                text = stringResource(id = R.string.auth_username)
            )
            SpacePathLine()
        }
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(72.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        )
        {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    modifier = Modifier
                        .width(35.dp)
                        .height(35.dp),
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Button"
                )
            }
        }
    }
}

@Composable
fun SpacePathLine (modifier: Modifier = Modifier){
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ){
        Icon(
            modifier = Modifier
                .layoutId("space_icon")
                .width(15.dp)
                .height(15.dp)
                .padding(end = 2.dp),
            painter = painterResource(id = R.drawable.ic_spaces),
            tint = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
            contentDescription = "Space Icon")
        Text(
            modifier = Modifier
                .layoutId("space_name")
                .padding(end = 8.dp),
            color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSize = 12.sp,
            text = "Space name"
        )
        Text(
            modifier = Modifier
                .layoutId("path"),
            color = colorResource(id = R.color.list_item_lastmod_and_filesize_text),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            text = "/path/to/file"
        )
    }
}

@Composable
fun EmptyList(modifier: Modifier = Modifier){
    Column(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .layoutId("list_empty_dataset_icon")
                .width(72.dp)
                .height(72.dp),
            tint = colorResource(id = R.color.grey),
            painter = painterResource(id = R.drawable.ic_uploads),
            contentDescription = "Empty dataset Icon"
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
                .layoutId("list_empty_dataset_sub_titlte")
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
fun UploadGroup (modifier: Modifier = Modifier) {
    Column{
        Row(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 5.dp)
                .fillMaxWidth()
        ){
            Text(
                modifier = Modifier
                    .layoutId("uploadListGroupName")
                    .padding(start = 16.dp),
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.color_accent),
                text = "UPLOADED"
            )
            Spacer (modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .layoutId("textViewFileCount")
                    .padding(end = 16.dp),
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.half_black),
                text = "5 FILES"
            )
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth(),
            color = colorResource(id = R.color.grey)
        )
        Row {
            Button(
                modifier = Modifier
                    .layoutId("uploadListGroupButtonClear")
                    .padding(start = 13.dp),
                onClick = {},
                shape = RoundedCornerShape(1.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.color_accent))
            ) {
                Text(
                    text = stringResource(id = R.string.action_upload_clear).toUpperCase(),
                    color = colorResource(id = R.color.white),
                )
            }
            Button(
                modifier = Modifier
                    .layoutId("uploadListGroupButtonRetry")
                    .padding(start = 8.dp),
                onClick = {},
                shape = RoundedCornerShape(1.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.color_accent))
            ) {
                Text(
                    text = stringResource(id = R.string.action_upload_retry).toUpperCase(),
                    color = colorResource(id = R.color.white),
                )
            }
        }
    }
}

@Preview
@Composable
fun TransferItemPreview(){
    Surface{
        TransferItem()
    }

}

@Preview
@Composable
fun SpacePathLinePreview(){
    Surface {
        SpacePathLine()
    }
}

@Preview
@Composable
fun EmptyListPreview(){
    Surface {
        EmptyList()
    }
}

@Preview
@Composable
fun TransferListPreview(){
    Surface {
        TransferList()
    }
}

@Preview
@Composable
fun uploadGroupPreview(){
    Surface {
        UploadGroup()
    }
}





