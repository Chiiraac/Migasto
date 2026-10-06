package com.chiiraac.migasto.ui.main

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.chiiraac.migasto.MiGastoApplication
import com.chiiraac.migasto.notifications.Notifications
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Member
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.data.repository.Reauth
import com.chiiraac.migasto.ui.calendar.CalendarScreen
import com.chiiraac.migasto.ui.components.EmptyState
import com.chiiraac.migasto.ui.components.today
import com.chiiraac.migasto.ui.groups.GroupEditorDialog
import com.chiiraac.migasto.ui.groups.GroupSettingsDialog
import com.chiiraac.migasto.ui.groups.GroupsSheet
import com.chiiraac.migasto.ui.groups.JoinGroupDialog
import com.chiiraac.migasto.ui.groups.LeaveGroupDialog
import com.chiiraac.migasto.ui.home.HomeScreen
import com.chiiraac.migasto.ui.movement.MovementDetailSheet
import com.chiiraac.migasto.ui.movement.MovementEditorSheet
import com.chiiraac.migasto.ui.settings.SettingsScreen
import com.chiiraac.migasto.ui.stats.StatsScreen
import com.chiiraac.migasto.ui.theme.AppTheme
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.launch

enum class MainTab(val label: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Rounded.Home),
    CALENDAR(R.string.nav_calendar, Icons.Rounded.CalendarMonth),
    STATS(R.string.nav_stats, Icons.AutoMirrored.Rounded.ShowChart),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

/** Acciones de la pantalla principal (separadas del ViewModel para poder previsualizar y probar). */
class MainActions(
    val selectGroup: (String) -> Unit = {},
    val createGroup: (String, GroupIcon) -> Unit = { _, _ -> },
    val joinGroup: (String, (Throwable?) -> Unit) -> Unit = { _, _ -> },
    val leaveGroup: (Group) -> Unit = {},
    val updateGroup: (Group, String, GroupIcon) -> Unit = { _, _, _ -> },
    /** (grupo, miembro, borrar también sus movimientos) — solo el creador. */
    val removeMember: (Group, Member, Boolean) -> Unit = { _, _, _ -> },
    val setJoinLocked: (Group, Boolean) -> Unit = { _, _ -> },
    val setGroupNotifications: (Group, Boolean) -> Unit = { _, _ -> },
    val saveMovement: (Long, String?, MovementDraft, Uri?, Boolean, (Boolean) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    val deleteMovement: (Movement) -> Unit = {},
    val loadPhoto: suspend (Movement) -> ByteArray? = { null },
    val setTheme: (ThemeMode) -> Unit = {},
    val setNewsEnabled: (Boolean) -> Unit = {},
    val updateName: (String) -> Unit = {},
    /** Cerrar sesión (true = aunque queden cambios sin enviar). */
    val signOut: (Boolean) -> Unit = {},
    val cancelSignOut: () -> Unit = {},
    val deleteAccount: (Reauth?, (Throwable?) -> Unit) -> Unit = { _, _ -> },
    val exportCsv: () -> Unit = {},
)

@Composable
fun MainRoute(userId: String) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val resources = LocalResources.current
    val viewModel: MainViewModel = viewModel(key = "main-$userId", factory = MainViewModel.factory(application))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Avisos de movimientos nuevos: en Android 13+ hay que pedir permiso (el sistema solo lo
    // muestra un par de veces; si se rechaza, se puede activar después en los ajustes del móvil).
    val isCloud = (application as MiGastoApplication).container.isCloud
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (isCloud && !askedNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notifications.enabled(context)
        ) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.text, *message.args.toTypedArray()))
        }
    }

    val actions = remember(viewModel) {
        MainActions(
            selectGroup = viewModel::selectGroup,
            createGroup = { name, icon -> viewModel.createGroup(name, icon) },
            joinGroup = viewModel::joinGroup,
            leaveGroup = viewModel::leaveGroup,
            updateGroup = viewModel::updateGroup,
            removeMember = viewModel::removeMember,
            setJoinLocked = viewModel::setJoinLocked,
            setGroupNotifications = viewModel::setGroupNotifications,
            saveMovement = viewModel::saveMovement,
            deleteMovement = viewModel::deleteMovement,
            loadPhoto = viewModel::loadPhoto,
            setTheme = viewModel::setThemeMode,
            setNewsEnabled = viewModel::setNewsEnabled,
            updateName = viewModel::updateName,
            signOut = viewModel::signOut,
            cancelSignOut = viewModel::cancelSignOut,
            deleteAccount = viewModel::deleteAccount,
            exportCsv = {
                scope.launch {
                    val current = viewModel.uiState.value
                    val group = current.selectedGroup ?: return@launch
                    if (current.movements.isEmpty()) {
                        snackbarHostState.showSnackbar(resources.getString(R.string.export_empty))
                    } else {
                        CsvShare.share(context, group, current.movements, current::authorName)
                    }
                }
            },
        )
    }

    MainScreen(state = state, actions = actions, snackbarHostState = snackbarHostState)
}

@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialTab: MainTab = MainTab.HOME,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val todayDate = today()
    var calendarDay by rememberSaveable { mutableLongStateOf(todayDate.toEpochDay()) }

    var showGroups by rememberSaveable { mutableStateOf(false) }
    var showGroupSettings by rememberSaveable { mutableStateOf(false) }
    var groupEditor by rememberSaveable { mutableStateOf<String?>(null) } // "new" | "edit"
    var showJoin by rememberSaveable { mutableStateOf(false) }
    var leaveGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    // Identifica cada apertura del editor: un guardado lento no debe cerrar otro editor nuevo.
    // Aleatorio para que no se repita entre sesiones (el ViewModel recuerda el último guardado).
    var editorToken by rememberSaveable { mutableLongStateOf(0L) }
    var editorMovementId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorDay by rememberSaveable { mutableLongStateOf(todayDate.toEpochDay()) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    val group = state.selectedGroup
    val hasGroups = state.groupsLoaded && state.groups.isNotEmpty()

    BackHandler(enabled = tab != MainTab.HOME) { tab = MainTab.HOME }

    fun openNewMovement() {
        editorMovementId = null
        editorDay = if (tab == MainTab.CALENDAR) calendarDay else todayDate.toEpochDay()
        editorToken = Random.nextLong()
        editorOpen = true
    }

    val currentEditorSaved = editorToken in state.savedEditorTokens
    LaunchedEffect(currentEditorSaved) {
        if (currentEditorSaved) editorOpen = false
    }

    Scaffold(
        topBar = {
            MainTopBar(
                group = group,
                onGroupClick = { showGroups = true },
                onSettingsClick = { showGroupSettings = true },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label), maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = AppTheme.money.navSelected,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (hasGroups && group != null && (tab == MainTab.HOME || tab == MainTab.CALENDAR)) {
                FloatingActionButton(
                    onClick = ::openNewMovement,
                    containerColor = AppTheme.money.fab,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.size(68.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_movement), modifier = Modifier.size(32.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        val layoutDirection = LocalLayoutDirection.current
        val sidePadding = Modifier.padding(
            start = padding.calculateStartPadding(layoutDirection),
            end = padding.calculateEndPadding(layoutDirection),
        )
        when {
            !state.groupsLoaded && tab != MainTab.SETTINGS -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.groups.isEmpty() && tab != MainTab.SETTINGS -> NoGroupsContent(
                modifier = Modifier.padding(padding),
                onCreate = { groupEditor = "new" },
                onJoin = { showJoin = true },
            )

            else -> AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab",
                // En tablets y en horizontal el contenido no se estira más de 720 dp.
                modifier = sidePadding
                    .fillMaxSize()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 720.dp),
            ) { current ->
                when (current) {
                    MainTab.HOME -> HomeScreen(
                        state = state,
                        onMovementClick = { detailId = it.id },
                        contentPadding = contentPadding,
                    )
                    MainTab.CALENDAR -> CalendarScreen(
                        state = state,
                        selectedDate = LocalDate.ofEpochDay(calendarDay),
                        onSelectDate = { calendarDay = it.toEpochDay() },
                        onMovementClick = { detailId = it.id },
                        contentPadding = contentPadding,
                    )
                    MainTab.STATS -> StatsScreen(state = state, contentPadding = contentPadding)
                    MainTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onUpdateName = actions.updateName,
                        onThemeChange = actions.setTheme,
                        onNewsChange = actions.setNewsEnabled,
                        onExportCsv = actions.exportCsv,
                        onSignOut = actions.signOut,
                        onCancelSignOut = actions.cancelSignOut,
                        onDeleteAccount = actions.deleteAccount,
                        contentPadding = contentPadding,
                    )
                }
            }
        }
    }

    // ----- Hojas y diálogos -----

    if (showGroups) {
        GroupsSheet(
            groups = state.groups,
            selectedId = group?.id,
            onSelect = {
                actions.selectGroup(it.id)
                showGroups = false
            },
            onCreate = {
                showGroups = false
                groupEditor = "new"
            },
            onJoin = {
                showGroups = false
                showJoin = true
            },
            onDismiss = { showGroups = false },
        )
    }

    if (showGroupSettings && group != null) {
        GroupSettingsDialog(
            group = group,
            currentUserId = state.user?.uid,
            isCloud = state.isCloud,
            movementCounts = if (group.id == state.selectedGroup?.id) {
                state.movements.groupingBy { it.createdById }.eachCount()
            } else {
                emptyMap()
            },
            onRemoveMember = { member, deleteMovements -> actions.removeMember(group, member, deleteMovements) },
            onSetJoinLocked = { locked -> actions.setJoinLocked(group, locked) },
            notificationsEnabled = if (state.isCloud) group.id !in state.mutedGroups else null,
            onSetNotifications = { enabled -> actions.setGroupNotifications(group, enabled) },
            onEdit = {
                showGroupSettings = false
                groupEditor = "edit"
            },
            onLeave = {
                showGroupSettings = false
                leaveGroupId = group.id
            },
            onDismiss = { showGroupSettings = false },
        )
    }

    when (groupEditor) {
        "new" -> GroupEditorDialog(
            initialName = if (state.groups.isEmpty()) stringResource(R.string.default_group_name) else "",
            initialIcon = GroupIcon.HOME,
            isNew = true,
            onDismiss = { groupEditor = null },
            onConfirm = { name, icon ->
                actions.createGroup(name, icon)
                groupEditor = null
            },
        )
        "edit" -> if (group != null) {
            GroupEditorDialog(
                initialName = group.name,
                initialIcon = group.icon,
                isNew = false,
                onDismiss = { groupEditor = null },
                onConfirm = { name, icon ->
                    actions.updateGroup(group, name, icon)
                    groupEditor = null
                },
            )
        }
    }

    if (showJoin) {
        JoinGroupDialog(
            isCloud = state.isCloud,
            onDismiss = { showJoin = false },
            onJoin = actions.joinGroup,
        )
    }

    val leaving = leaveGroupId?.let { id -> state.groups.firstOrNull { it.id == id } }
    if (leaving != null) {
        LeaveGroupDialog(
            group = leaving,
            isCloud = state.isCloud,
            onDismiss = { leaveGroupId = null },
            onConfirm = {
                actions.leaveGroup(leaving)
                leaveGroupId = null
            },
        )
    }

    val detail = detailId?.let { id -> state.movements.firstOrNull { it.id == id } }
    if (detail != null) {
        MovementDetailSheet(
            movement = detail,
            authorName = if (state.isCloud) state.authorName(detail) else null,
            loadPhoto = actions.loadPhoto,
            onEdit = {
                detailId = null
                editorMovementId = detail.id
                editorDay = detail.date.toEpochDay()
                editorToken = Random.nextLong()
                editorOpen = true
            },
            onDelete = {
                detailId = null
                actions.deleteMovement(detail)
            },
            onDismiss = { detailId = null },
        )
    }

    if (editorOpen && group != null) {
        val existing = editorMovementId?.let { id -> state.movements.firstOrNull { it.id == id } }
        val token = editorToken
        key(token) {
            MovementEditorSheet(
                existing = existing,
                initialDate = LocalDate.ofEpochDay(editorDay),
                savingElsewhere = token in state.savingEditorTokens,
                loadPhoto = actions.loadPhoto,
                onSave = { draft, photo, removePhoto, onDone ->
                    actions.saveMovement(token, existing?.id, draft, photo, removePhoto, onDone)
                },
                onDismiss = { if (editorToken == token) editorOpen = false },
            )
        }
    }
}

@Composable
private fun MainTopBar(group: Group?, onGroupClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(76.dp)
            .padding(horizontal = 12.dp),
    ) {
        if (group != null) {
            Surface(
                onClick = onGroupClick,
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 240.dp),
            ) {
                Row(
                    Modifier.padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_select_group),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_group_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun NoGroupsContent(modifier: Modifier, onCreate: () -> Unit, onJoin: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Rounded.Groups,
            title = stringResource(R.string.no_groups_title),
            body = stringResource(R.string.no_groups_body),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.group_create))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.group_join))
        }
    }
}
