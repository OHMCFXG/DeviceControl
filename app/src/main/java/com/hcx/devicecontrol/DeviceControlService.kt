package com.hcx.devicecontrol

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.ControlAction
import android.service.controls.templates.StatelessTemplate
import android.util.Log
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.reactivestreams.FlowAdapters
import java.util.*
import java.util.concurrent.Flow
import java.util.function.Consumer
import kotlin.collections.HashMap


class DeviceControlService : ControlsProviderService() {

    private lateinit var updatePublisher: ReplayProcessor<Control>
    private var rootState: Boolean = false

    // 创建Shell
    private var session: RxCmdShell.Session? = null

    override fun onCreate() {
        super.onCreate()
        // 检查Root权限，没有则尝试获取
        rootState = getSharedPreferences("data", 0).getBoolean("isRoot", false)
        if (!rootState) {
            val root: Root = Root.Builder().build().blockingGet()//检查Root权限
            if (root.state == Root.State.ROOTED) {
                rootState = true
            } else Log.d("Root", "no root permission")
        } else Log.d("Root", "YAY!!!")
    }

    private val allDeviceControl = arrayListOf(
        hashMapOf(
            "title" to "扫一扫",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Scan"
        ),
        hashMapOf(
            "title" to "付款码",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Pay"
        ),
        hashMapOf(
            "title" to "收款码",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Receive"
        ),
        hashMapOf(
            "title" to "乘车码",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Transport"
        ),
        hashMapOf(
            "title" to "蚂蚁庄园",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Farm"
        ),
        hashMapOf(
            "title" to "蚂蚁森林",
            "subtitle" to "支付宝",
            "controlId" to "Alipay-Forest"
        ),


        hashMapOf(
            "title" to "扫一扫",
            "subtitle" to "微信",
            "controlId" to "Wechat-Scan"
        ),
        hashMapOf(
            "title" to "付款码",
            "subtitle" to "微信",
            "controlId" to "Wechat-Pay"
        ),
        hashMapOf(
            "title" to "收款码",
            "subtitle" to "微信",
            "controlId" to "Wechat-Receive"
        )
//
//
//        hashMapOf(
//            "title" to "付款码",
//            "subtitle" to "云闪付",
//            "controlId" to "Unipay-Pay"
//        ),
//        hashMapOf(
//            "title" to "收款码",
//            "subtitle" to "云闪付",
//            "controlId" to "Unipay-Receive"
//        ),
//        hashMapOf(
//            "title" to "乘车码",
//            "subtitle" to "云闪付",
//            "controlId" to "Unipay-Transport"
//        )
    )


    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {

        val intent = Intent()
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val controlList = mutableListOf<Control>()
        allDeviceControl.forEach { device ->
            val controlId = device["controlId"].toString()
            val iconName = controlId.toLowerCase(Locale.ROOT).replace("-", "_")
            val iconId = resources.getIdentifier(
                iconName,
                "drawable",
                this.packageName
            )

            val control = Control.StatelessBuilder(controlId, pendingIntent)
                .setTitle(device["title"].toString())
                .setSubtitle(device["subtitle"].toString())
                .setDeviceType(DeviceTypes.TYPE_UNKNOWN)
                .setCustomIcon(Icon.createWithResource(this, iconId))
                .build()
            // 控件追加
            controlList.add(control)
        }
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controlList))
    }

    private fun getDevice(p0: String): HashMap<String, String> {
        for (device in allDeviceControl) {
//            Log.d("find", device.toString())
            if (device["controlId"] == p0) {
                return device
            } else {
                continue
            }
        }
        return HashMap()
    }


    override fun createPublisherFor(p0: MutableList<String>): Flow.Publisher<Control> {
        val intent = Intent()
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 12, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        updatePublisher = ReplayProcessor.create()

        p0.forEach { name ->
            val device = getDevice(name)
            val iconName = device["controlId"].toString().toLowerCase(Locale.ROOT).replace("-", "_")
            val iconId = resources.getIdentifier(
                iconName,
                "drawable",
                this.packageName
            )
            val control = Control.StatefulBuilder(name, pendingIntent)
                .setTitle(device["title"].toString())
                .setSubtitle(device["subtitle"].toString())
                .setDeviceType(DeviceTypes.TYPE_UNKNOWN)
                .setStatus(Control.STATUS_OK)
                .setControlTemplate(StatelessTemplate(name))
                .setCustomIcon(Icon.createWithResource(this, iconId))
                .build()
            updatePublisher.onNext(control)
        }

        return FlowAdapters.toFlowPublisher(updatePublisher)
    }


    override fun performControlAction(controlId: String, p1: ControlAction, p2: Consumer<Int>) {
        Log.d("performControlAction", controlId)
        val intent = Intent()
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val device = getDevice(controlId)

        val iconName = device["controlId"].toString().toLowerCase(Locale.ROOT).replace("-", "_")
        val iconId = resources.getIdentifier(
            iconName,
            "drawable",
            this.packageName
        )

        // 判断 action 类型
        closePowerMenu() //尝试关闭电源菜单
        val msg = makeAction(controlId)

        // Control更新
        val control = Control.StatefulBuilder(controlId, pendingIntent)
            .setTitle(device["title"].toString())
            .setSubtitle(device["subtitle"].toString())
            .setDeviceType(DeviceTypes.TYPE_UNKNOWN)
            .setStatus(Control.STATUS_OK)
            .setControlTemplate(StatelessTemplate(controlId))
            .setStatusText(msg)
            .setCustomIcon(Icon.createWithResource(this, iconId))
            .build()

        updatePublisher.onNext(control)
    }


    private fun makeAction(controlId: String): String {
        Log.d("makeAction", controlId)
        var msg = ""
        val launchIntent = Intent()
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        when (controlId) {
            //支付宝
            "Alipay-Scan" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=10000007")
                startActivity(launchIntent)
            }
            "Alipay-Pay" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=20000056")
                startActivity(launchIntent)
            }
            "Alipay-Receive" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=20000123")
                startActivity(launchIntent)
            }
            "Alipay-Transport" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=200011235")
                startActivity(launchIntent)
            }
            "Alipay-Farm" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=66666674")
                startActivity(launchIntent)
            }
            "Alipay-Forest" -> {
                launchIntent.action = Intent.ACTION_VIEW
                launchIntent.data = Uri.parse("alipays://platformapi/startapp?saId=60000002")
                startActivity(launchIntent)
            }

            //微信
            "Wechat-Scan" -> {
                launchIntent.action = Intent.ACTION_MAIN
                launchIntent.addCategory("android.intent.category.LAUNCHER")
                    .setPackage("com.tencent.mm")
                    .setComponent(ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"))
                    .putExtra("LauncherUI.From.Scaner.Shortcut", true)
                startActivity(launchIntent)
            }
            "Wechat-Pay" -> {
                if (rootState) {
                    Cmd.builder("am start -n com.tencent.mm/.plugin.offline.ui.WalletOfflineCoinPurseUI")
                        .execute(session)
                } else {
                    msg = "暂不支持无root启动微信付款"
                    Log.d("root", "暂不支持无root启动微信付款")
                }
            }
            "Wechat-Receive" -> {
                if (rootState) {
                    Cmd.builder("am start -n com.tencent.mm/.plugin.collect.ui.CollectMainUI")
                        .execute(session)
                } else {
                    msg = "暂不支持无root启动微信收款"
                    Log.d("root", "暂不支持无root启动微信收款")
                }
            }

            //云闪付

            else -> Log.d("makeAction", "未知动作")
        }
        return msg
    }

    private fun closePowerMenu() {
        if (rootState) {
            session = RxCmdShell.builder().root(true).build().open().blockingGet()
            val result = Cmd.builder(
                "sendevent /dev/input/event0 1 116 1\n" +
                        "sendevent /dev/input/event0 0 0 0\n" +
                        "sendevent /dev/input/event0 1 116 0"
            ).execute(session)
            Log.d("closePowerMenu", result.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        val editor = getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        editor.putBoolean("isRoot", rootState)
        editor.apply()
    }
}

