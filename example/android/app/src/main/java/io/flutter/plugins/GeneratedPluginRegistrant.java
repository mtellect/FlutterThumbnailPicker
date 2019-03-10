package io.flutter.plugins;

import io.flutter.plugin.common.PluginRegistry;
import com.bappstack.flutter_thumbnail_picker.FlutterThumbnailPickerPlugin;
import com.asapjay.thumbnails.ThumbnailsPlugin;
import io.flutter.plugins.videoplayer.VideoPlayerPlugin;

/**
 * Generated file. Do not edit.
 */
public final class GeneratedPluginRegistrant {
  public static void registerWith(PluginRegistry registry) {
    if (alreadyRegisteredWith(registry)) {
      return;
    }
    FlutterThumbnailPickerPlugin.registerWith(registry.registrarFor("com.bappstack.flutter_thumbnail_picker.FlutterThumbnailPickerPlugin"));
    ThumbnailsPlugin.registerWith(registry.registrarFor("com.asapjay.thumbnails.ThumbnailsPlugin"));
    VideoPlayerPlugin.registerWith(registry.registrarFor("io.flutter.plugins.videoplayer.VideoPlayerPlugin"));
  }

  private static boolean alreadyRegisteredWith(PluginRegistry registry) {
    final String key = GeneratedPluginRegistrant.class.getCanonicalName();
    if (registry.hasPlugin(key)) {
      return true;
    }
    registry.registrarFor(key);
    return false;
  }
}
