package fm.mrc.todolist.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fm.mrc.todolist.R

@Composable
fun LogoutButton(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onLogout,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = Icons.Default.Logout,
            contentDescription = "Logout"
        )
    }
} 