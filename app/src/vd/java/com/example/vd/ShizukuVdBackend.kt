package com.example.vd

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.view.Surface
import rikka.shizuku.Shizuku

/** Server side: instantiated by Shizuku in the shell process (needs a public ctor taking Context, or no-arg). */
class VdUserService(private val ctx: Context) : IVdService.Stub() {
    private val svc = VdService()
    override fun destroy() { System.exit(0) }
    override fun createDisplay(name: String, surface: Surface, w: Int, h: Int, dpi: Int) = svc.createDisplay(ctx, name, surface, w, h, dpi)
    override fun resize(id: Int, w: Int, h: Int, dpi: Int) = svc.resize(id, w, h, dpi)
    override fun release(id: Int) = svc.release(id)
    override fun launch(id: Int, component: String) = svc.launch(id, component)
    override fun touch(id: Int, action: Int, x: Float, y: Float, downTime: Long) = svc.touch(id, action, x, y, downTime)
}

/** Client side: binds the UserService once; VdBackend calls fail soft (-1) until ready. */
class ShizukuVdBackend(private val ctx: Context, private val onState: (State) -> Unit) : VdBackend {
    enum class State { NOT_RUNNING, NEED_PERMISSION, CONNECTING, READY, DIED }

    @Volatile private var remote: IVdService? = null

    private val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, VdUserService::class.java.name))
        .daemon(false).processNameSuffix("vd").version(1)

    private val conn = object : android.content.ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) { remote = IVdService.Stub.asInterface(b); onState(State.READY) }
        override fun onServiceDisconnected(n: ComponentName) { remote = null; onState(State.DIED) }
    }

    fun start() {
        if (!Shizuku.pingBinder()) { onState(State.NOT_RUNNING); return }
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onState(State.NEED_PERMISSION); Shizuku.requestPermission(1); return
        }
        onState(State.CONNECTING)
        Shizuku.bindUserService(args, conn)
    }

    override fun createDisplay(name: String, surface: Surface, w: Int, h: Int, dpi: Int) = remote?.createDisplay(name, surface, w, h, dpi) ?: -1
    override fun resize(id: Int, w: Int, h: Int, dpi: Int) { remote?.resize(id, w, h, dpi) }
    override fun release(id: Int) { remote?.release(id) }
    override fun launch(id: Int, component: String) = remote?.launch(id, component) ?: -1
    override fun touch(id: Int, action: Int, x: Float, y: Float, downTime: Long) { remote?.touch(id, action, x, y, downTime) }
}
