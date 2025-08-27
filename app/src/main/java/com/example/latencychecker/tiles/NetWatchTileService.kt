package com.example.latencychecker.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.latencychecker.net.SpeedTestRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetWatchTileService : TileService() {
    override fun onClick() {
        qsTile.state = Tile.STATE_ACTIVE; qsTile.updateTile()
        CoroutineScope(Dispatchers.IO).launch {
            val ping = SpeedTestRunner.runPing()
            val dl = SpeedTestRunner.runDownload()
            qsTile.subtitle = "${"%.1f".format(dl)} Mbps | ${ping}ms"
            qsTile.state = Tile.STATE_INACTIVE; qsTile.updateTile()
        }
    }
}
