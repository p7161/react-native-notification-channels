package com.reactnativenotificationchannels

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Build
import com.facebook.react.bridge.*

class NotificationChannelsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private val notificationManager: NotificationManager = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  override fun getName(): String {
      return "NotificationChannels"
  }

  override fun getConstants(): MutableMap<String, Any> {
    val constants: MutableMap<String, Any> = HashMap()

    val visibility: MutableMap<String, Int> = HashMap()
    visibility["VISIBILITY_SECRET"] = Notification.VISIBILITY_SECRET
    visibility["VISIBILITY_PRIVATE"] = Notification.VISIBILITY_PRIVATE
    visibility["VISIBILITY_PUBLIC"] = Notification.VISIBILITY_PUBLIC
    constants["CHANNEL_VISIBILITY"] = visibility

    return constants
  }

  @ReactMethod
  fun listChannels(promise: Promise) {
    val channels = WritableNativeArray()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(channels)
      return
    }
    val listChannels: List<NotificationChannel> = notificationManager.notificationChannels
    for (channel in listChannels) {
      channels.pushString(channel.id)
    }
    promise.resolve(channels)
  }

  @ReactMethod
  fun channelBlocked(channel_id: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(false)
      return
    }
    val id = channel_id?.trim()
    if (id.isNullOrEmpty()) {
      promise.resolve(false)
      return
    }
    val channel = notificationManager.getNotificationChannel(id)
    promise.resolve(channel != null && NotificationManager.IMPORTANCE_NONE == channel.importance)
  }

  @ReactMethod
  fun channelExists(channel_id: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(false)
      return
    }
    val id = channel_id?.trim()
    if (id.isNullOrEmpty()) {
      promise.resolve(false)
      return
    }
    val channel = notificationManager.getNotificationChannel(id)
    promise.resolve(channel != null)
  }

  @ReactMethod
  fun deleteChannel(channel_id: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(true)
      return
    }
    val id = channel_id?.trim()
    if (id.isNullOrEmpty()) {
      promise.resolve(false)
      return
    }
    notificationManager.deleteNotificationChannel(id)
    promise.resolve(true)
  }

  private fun toLongArray(array: ReadableArray?): LongArray? {
    if (array == null) {
      return null
    }

    val size = array.size()
    if (size <= 0) {
      return null
    }

    val pattern = LongArray(size)
    for (i in 0 until size) {
      val value = array.getDouble(i)
      pattern[i] = if (value < 0) 0L else value.toLong()
    }

    return pattern
  }

  private fun checkOrCreateChannel(
    channel_id: String,
    channel_name: String,
    channel_description: String?,
    importance: Int,
    groupId: String?,
    lockscreenVisibility: Int?,
    bypassDnd: Boolean?,
    vibrationPattern: LongArray?
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return false
    }
    val existing = notificationManager.getNotificationChannel(channel_id)
    if (existing == null) {
      val channel = NotificationChannel(channel_id, channel_name, importance)
      channel.description = channel_description
      if (groupId != null) {
        channel.group = groupId
      }
      if (lockscreenVisibility != null) {
        channel.lockscreenVisibility = lockscreenVisibility
      }
      if (bypassDnd != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        channel.setBypassDnd(bypassDnd)
      }
      if (vibrationPattern != null) {
        channel.enableVibration(true)
        channel.vibrationPattern = vibrationPattern
      }
      notificationManager.createNotificationChannel(channel)
      return true
    }

    var touched = false
    if (channel_description != null && existing.description != channel_description) {
      existing.description = channel_description
      touched = true
    }
    if (lockscreenVisibility != null && existing.lockscreenVisibility != lockscreenVisibility) {
      existing.lockscreenVisibility = lockscreenVisibility
      touched = true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bypassDnd != null) {
      existing.setBypassDnd(bypassDnd)
      touched = true
    }
    if (vibrationPattern != null) {
      existing.enableVibration(true)
      existing.vibrationPattern = vibrationPattern
      touched = true
    }
    if (touched) {
      notificationManager.createNotificationChannel(existing)
    }
    return false
  }

  @ReactMethod
  fun createChannel(channelInfo: ReadableMap, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(true)
      return
    }

    val channelId = channelInfo.getString("channelId")?.trim()
    if (channelId.isNullOrEmpty()) {
      promise.reject("E_MISSING_ID", "channelId is required")
      return
    }
    val channelName = channelInfo.getString("channelName") ?: channelId
    val channelDescription = if (channelInfo.hasKey("channelDescription") && !channelInfo.isNull("channelDescription")) channelInfo.getString("channelDescription") else null
    //        boolean playSound = !channelInfo.hasKey("playSound") || channelInfo.getBoolean("playSound");
//        String soundName = channelInfo.hasKey("soundName") ? channelInfo.getString("soundName") : "default";
    val importance = if (channelInfo.hasKey("importance") && !channelInfo.isNull("importance")) {
      channelInfo.getInt("importance")
    } else {
      NotificationManager.IMPORTANCE_HIGH
    }
    //        boolean vibrate = channelInfo.hasKey("vibrate") && channelInfo.getBoolean("vibrate");
//        long[] vibratePattern = vibrate ? new long[] { 0, DEFAULT_VIBRATION } : null;
//        Uri soundUri = playSound ? getSoundUri(soundName) : null;
    val groupId = if (channelInfo.hasKey("groupId") && !channelInfo.isNull("groupId")) channelInfo.getString("groupId") else null
    val lockscreenVisibility = if (channelInfo.hasKey("lockscreenVisibility") && !channelInfo.isNull("lockscreenVisibility")) {
      channelInfo.getInt("lockscreenVisibility")
    } else {
      null
    }
    val bypassDnd = if (channelInfo.hasKey("bypassDnd") && !channelInfo.isNull("bypassDnd")) {
      channelInfo.getBoolean("bypassDnd")
    } else {
      null
    }
    val vibrationPattern = if (channelInfo.hasKey("vibrationPattern") && !channelInfo.isNull("vibrationPattern")) {
      toLongArray(channelInfo.getArray("vibrationPattern"))
    } else {
      null
    }
    val created = checkOrCreateChannel(
      channelId,
      channelName,
      channelDescription,
      importance,
      groupId,
      lockscreenVisibility,
      bypassDnd,
      vibrationPattern
    )

    promise.resolve(created)
  }

  @ReactMethod
  fun createChannelGroup(groupId: String?, groupName: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(true)
      return
    }
    val id = groupId?.trim()
    val name = groupName?.trim()
    if (id.isNullOrEmpty() || name.isNullOrEmpty()) {
      promise.reject("E_GROUP_ARGS", "groupId and groupName are required")
      return
    }
    notificationManager.createNotificationChannelGroup(NotificationChannelGroup(id, name))
    promise.resolve(true)
  }

}
