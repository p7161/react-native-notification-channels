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
//    val channels: MutableList<String> = ArrayList()
    var channels = WritableNativeArray()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(null)
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
      promise.resolve(null)
      return
    }
    val channel = notificationManager.getNotificationChannel(channel_id)
    promise.resolve(NotificationManager.IMPORTANCE_NONE == channel.importance)
  }

  @ReactMethod
  fun channelExists(channel_id: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(null)
      return
    }
    val channel = notificationManager.getNotificationChannel(channel_id)
    promise.resolve(channel != null)
  }

  @ReactMethod
  fun deleteChannel(channel_id: String?,promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(null)
      return
    }
    notificationManager.deleteNotificationChannel(channel_id)
    promise.resolve("Channel Deleted")
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
    channel_id: String?,
    channel_name: String?,
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
    var channel = notificationManager.getNotificationChannel(channel_id)
    if (channel == null && channel_name != null && channel_description != null ||
      channel != null &&
      (channel_name != null && channel_name != channel.name ||
        channel_description != null && channel_description != channel.description)) {
      // If channel doesn't exist create a new one.
      // If channel name or description is updated then update the existing channel.
      channel = NotificationChannel(channel_id, channel_name, importance)
      channel.description = channel_description
      //            channel.enableLights(true);
//            channel.enableVibration(vibratePattern != null);
//            channel.setVibrationPattern(vibratePattern);

//            if (soundUri != null) {
//                AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .build();
//
//                channel.setSound(soundUri, audioAttributes);
//            } else {
//                channel.setSound(null, null);
//            }
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
    return false
  }

  @ReactMethod
  fun createChannel(channelInfo: ReadableMap, promise: Promise) {
    val channelId = channelInfo.getString("channelId")
    val channelName = channelInfo.getString("channelName")
    val channelDescription = if (channelInfo.hasKey("channelDescription")) channelInfo.getString("channelDescription") else ""
    //        boolean playSound = !channelInfo.hasKey("playSound") || channelInfo.getBoolean("playSound");
//        String soundName = channelInfo.hasKey("soundName") ? channelInfo.getString("soundName") : "default";
    val importance = if (channelInfo.hasKey("importance")) channelInfo.getInt("importance") else 4
    //        boolean vibrate = channelInfo.hasKey("vibrate") && channelInfo.getBoolean("vibrate");
//        long[] vibratePattern = vibrate ? new long[] { 0, DEFAULT_VIBRATION } : null;
//        Uri soundUri = playSound ? getSoundUri(soundName) : null;
    val groupId = if (channelInfo.hasKey("groupId")) channelInfo.getString("groupId") else null
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
    promise.resolve(
      checkOrCreateChannel(
        channelId,
        channelName,
        channelDescription,
        importance,
        groupId,
        lockscreenVisibility,
        bypassDnd,
        vibrationPattern
      )
    )
  }

  @ReactMethod
  fun createChannelGroup(groupId: String?, groupName: String?, promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      promise.resolve(null)
      return
    }
    notificationManager.createNotificationChannelGroup(NotificationChannelGroup(groupId, groupName))
    promise.resolve(true)
  }

}
